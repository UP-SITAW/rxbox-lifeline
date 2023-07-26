package ph.chits.rxbox.lifeline;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.TextureView;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import ph.chits.rxbox.lifeline.model.SensorData;

public class CTGTrace extends TextureView implements Runnable {

    // static fields
    private static final String TAG = "CTGTrace";
    private static final float LINE_STROKE = 1.2f;
    private static final float LINE_STROKE_THICK = 1.0f;
    private static final float LINE_STROKE_THIN = 0.3f;
    private static final double TEXT_SIZE_FHR = 12.0d;
    private static final double TEXT_SIZE_TOCO_KPA = 12.0d;
    private static final float TEXT_SIZE_TOCO_MMHG = 13.0f;
    private static final int CATEGORY_MASK = 0xffff0000;
    private static final int MARK_COLOR = -16776961;
    private static final int MAX_SAMPLES = 4800;

    // drawing paint objects
    private Paint paintFhrText;
    private Paint paintGridMajor;
    private Paint paintGridMinor;
    private Paint paintLine;
    private Paint paintMark;
    private Paint paintTextBg;
    private Paint paintTocoKpaText;
    private Paint paintTocoMmhgText;

    // drawing configurations fields
    private int height;
    private int width;
    private float pixelsPerSample;
    private float minorHorizontalInterval;
    private float fhrMinorVerticalInterval;
    private float fhrOffsetFromTop;
    private float fhrMarginFromTop;
    private float tocoMinorVerticalInterval;
    private float tocoOffsetFromTop;
    private float tocoMarginFromTop;
    private float screenDensity;

    // data source
    private SensorData<Integer> fetalHeartRate;
    private SensorData<Integer> pressure;
    private SensorData<Boolean> mark;

    // thread fields
    private boolean running = false;
    private Thread thread;
    private Object mutex = new Object();

    public CTGTrace(@NonNull Context context, SensorData<Integer> fhr, SensorData<Integer> pressure, SensorData<Boolean> mark) {
        super(context);
        this.fetalHeartRate = fhr;
        this.pressure = pressure;
        this.mark = mark;
        this.paintLine = new Paint(1);
        this.paintGridMajor = new Paint(1);
        this.paintGridMinor = new Paint(1);
        this.paintFhrText = new Paint(1);
        this.paintTocoMmhgText = new Paint(1);
        this.paintTocoKpaText = new Paint(1);
        this.paintTextBg = new Paint(1);
        this.paintMark = new Paint(this.paintLine);
        this.paintMark.setColor(MARK_COLOR);
        this.paintLine.setStyle(Paint.Style.STROKE);
        this.paintLine.setStrokeWidth(LINE_STROKE);
        this.paintLine.setColor(View.MEASURED_STATE_MASK);
        this.paintGridMajor.setStyle(Paint.Style.STROKE);
        this.paintGridMajor.setStrokeWidth(LINE_STROKE_THICK);
        this.paintGridMajor.setColor(CATEGORY_MASK);
        this.paintGridMinor.setStyle(Paint.Style.STROKE);
        this.paintGridMinor.setStrokeWidth(LINE_STROKE_THIN);
        this.paintGridMinor.setColor(CATEGORY_MASK);
        this.paintFhrText.setColor(CATEGORY_MASK);
        this.paintFhrText.setTextSize(12.0f);
        this.paintTocoMmhgText.setColor(CATEGORY_MASK);
        this.paintTocoMmhgText.setTextSize(TEXT_SIZE_TOCO_MMHG);
        this.paintTocoKpaText.setColor(CATEGORY_MASK);
        this.paintTocoKpaText.setTextSize(12.0f);
        this.paintTextBg.setStyle(Paint.Style.FILL_AND_STROKE);
        this.paintTextBg.setStrokeWidth(1.5f);
        this.paintTextBg.setColor(-1);
    }

    private void setDefaultSettings(@NonNull Canvas canvas, float screenDensity) {
        this.height = canvas.getHeight();
        this.width = canvas.getWidth();
        this.pixelsPerSample = (float) width / MAX_SAMPLES;
        this.minorHorizontalInterval = pixelsPerSample * 120.0f;

        // Note: fhir means Fetal Heart Rate
        float temp1 = (height * 70.0f) / 115.0f; // f2
        this.fhrMinorVerticalInterval = temp1 / 21.0f;
        this.fhrOffsetFromTop = temp1;
        this.fhrMarginFromTop = 0.0f;

        // Note: Tocometer
        float temp2 = (height * 40.0f) / 115.0f; // f3
        this.tocoMinorVerticalInterval = temp2 / 20.0f;

        float temp3 = temp1 + ((height * 5.0f) / 115.0f); // f4
        this.tocoOffsetFromTop = temp2 + temp3;
        this.tocoMarginFromTop = temp3;

        float temp4 = LINE_STROKE * screenDensity; // f5
        this.paintLine.setStrokeWidth(temp4);

        this.paintMark = new Paint(this.paintLine);
        this.paintMark.setStrokeWidth(temp4);
        this.paintMark.setColor(MARK_COLOR);

        this.paintGridMajor.setStrokeWidth(LINE_STROKE_THICK * screenDensity);
        this.paintGridMinor.setStrokeWidth(LINE_STROKE_THIN * screenDensity);

        float temp5 = 12.0f * screenDensity; // f6
        this.paintFhrText.setTextSize(temp5);
        this.paintTocoMmhgText.setTextSize(TEXT_SIZE_TOCO_MMHG * screenDensity);
        this.paintTocoKpaText.setTextSize(temp5);
        this.paintTextBg.setStrokeWidth(screenDensity * 1.5f);
    }

    /**
     * Draw the CT Graph with current data
     *
     * @param screenDensity
     */
    private void drawChart(float screenDensity) {
        Canvas canvas = lockCanvas();
        setDefaultSettings(canvas, screenDensity);
        canvas.drawRGB(255, 255, 255);
        drawBackground(canvas);
        drawLine(canvas);
        unlockCanvasAndPost(canvas);
    }

    /**
     * Draw CT Graph data points and line
     * @param canvas
     */
    private void drawLine(Canvas canvas) {
        ArrayList<Integer> fhrData = new ArrayList<>(this.fetalHeartRate.getBulk(-MAX_SAMPLES));
        ArrayList<Integer> tocoData = new ArrayList<>(this.pressure.getBulk(-MAX_SAMPLES));
        ArrayList<Boolean> markData = new ArrayList<>(this.mark.getBulk(-MAX_SAMPLES));
        drawMarkLine(canvas, markData);
        drawFHRLine(canvas, fhrData);
        drawTocoLine(canvas, tocoData);
    }

    private void drawMarkLine(Canvas canvas, List<Boolean> markData) {
        int sampleDataLimit = Math.round(this.width / this.pixelsPerSample);
        float xPos = (sampleDataLimit - markData.size()) * this.pixelsPerSample;
        int yPosTop = 0;
        for (Boolean mark : markData) {
            if (mark) {
                canvas.drawLine(xPos, yPosTop, xPos, this.tocoOffsetFromTop, this.paintMark);
            }
            xPos += this.pixelsPerSample;
        }
    }

    private void drawFHRLine(Canvas canvas, List<Integer> data) {
        int sampleDataLimit = Math.round(this.width / this.pixelsPerSample);
        int pixelPerSampleData = Math.round(this.fhrOffsetFromTop / 210.0f);
        float xPos = (sampleDataLimit - data.size()) * this.pixelsPerSample;
        float yPos = this.fhrOffsetFromTop;
        for (Integer value : data) {
            float currentXPos = xPos + this.pixelsPerSample;
            float currentYPos = yPos;
            if (value >= 30) {
                Integer adjustedValue = value - 30;
                currentYPos = this.fhrOffsetFromTop - (adjustedValue * pixelPerSampleData);
                canvas.drawLine(xPos, yPos, currentXPos, currentYPos, this.paintLine);
            }
            xPos = currentXPos;
            yPos = currentYPos;
        }
    }

    private void drawTocoLine(Canvas canvas, List<Integer> data) {
        int sampleDataLimit = Math.round(this.width / this.pixelsPerSample);
        float pixelPerSampleData = (this.tocoOffsetFromTop - this.tocoMarginFromTop) / 100.0f;
        float xPos = (sampleDataLimit - data.size()) * this.pixelsPerSample;
        float yPos = this.tocoOffsetFromTop;
        for (Integer value : data) {
            float currentXPos = xPos + this.pixelsPerSample;
            float currentYPos = this.tocoOffsetFromTop - (value * pixelPerSampleData);
            canvas.drawLine(xPos, yPos, currentXPos, currentYPos, this.paintLine);
            xPos = currentXPos;
            yPos = currentYPos;
        }
    }

    /**
     * Draw CT Graph background outline
     * NOTE: THIS IS A EXTRACTED CODE FROM LEGACY APP. It doesn't make sense to me but if you can re-write this in much
     * readable format I appreciate it.
     * TODO: Need lots of refactoring
     *
     * @param canvas
     */
    private void drawBackground(Canvas canvas) {

        // draw vertical lines
        for (int i = 0; i < 22; i++) { // what is 22????
            float yAxis = this.fhrOffsetFromTop - (i * this.fhrMinorVerticalInterval);
            if (i % 3 == 0) {
                canvas.drawLine(0.0f, yAxis, this.width, yAxis, this.paintGridMajor);
            } else {
                canvas.drawLine(0.0f, yAxis, this.width, yAxis, this.paintGridMinor);
            }
        }

        int i;
        int i2;
        int i3 = this.width;
        int i4 = 0;

        float f = 0.0f;
        float i6 = 0; // what is i6?
        float i7 = 0; // what is i7?

        // for FHR part
        while (!(f > this.width)) {
            if (i6 == 0) {
                i = 1;
                i2 = 3;
                if (i7 == 0) {
                    canvas.drawLine(f, this.fhrMarginFromTop, f, this.fhrOffsetFromTop, this.paintGridMajor);
                } else {
                    canvas.drawLine(f, this.fhrMarginFromTop + this.fhrMinorVerticalInterval, f, this.fhrOffsetFromTop, this.paintGridMinor);
                }
                if (i7 == i) {
                    Rect rect = new Rect();
                    this.paintFhrText.getTextBounds("FHR", 0, i2, rect);
                    canvas.drawText("FHR", f - (rect.width() / 2.0f), this.fhrMarginFromTop + (this.fhrMinorVerticalInterval / 2.0f) + (rect.height() / 2.0f), this.paintFhrText);
                } else {
                    String str = "240";
                    if (i7 == i2) {
                        int i8 = 7;
                        while (i < i8) {
                            Rect rect = new Rect();
                            String valueOf = String.valueOf((i * 30) + 30);
                            this.paintFhrText.getTextBounds(valueOf, 0, valueOf.length(), rect);
                            float height = (this.fhrOffsetFromTop - ((i * 3) * this.fhrMinorVerticalInterval)) + (rect.height() / 2.0f);
                            float width = f - (rect.width() / 2.0f);
                            canvas.drawRect(width, height - rect.height(), width + rect.width(), height, this.paintTextBg);
                            canvas.drawText(valueOf, width, height, this.paintFhrText);
                            i++;
                        }
                        String str2 = str;
                        Rect rect = new Rect();
                        this.paintFhrText.getTextBounds(str2, 0, i2, rect);
                        canvas.drawText(str2, f - (rect.width() / 2.0f), this.fhrMarginFromTop + (this.fhrMinorVerticalInterval / 2.0f) + (rect.height() / 2.0f), this.paintFhrText);
                        this.paintFhrText.getTextBounds("30", 0, 2, rect);
                        float height2 = (this.fhrOffsetFromTop - (this.fhrMinorVerticalInterval / 2.0f)) + (rect.height() / 2.0f);
                        float width2 = f - (rect.width() / 2.0f);
                        canvas.drawRect(width2, height2 - rect.height(), width2 + rect.width(), height2, this.paintTextBg);
                        canvas.drawText("30", width2, height2, this.paintFhrText);
                    } else if (i7 == 5) {
                        Rect rect = new Rect();
                        this.paintFhrText.getTextBounds("240", 0, i2, rect);
                        canvas.drawText("bpm", f - (rect.width() / 2.0f), this.fhrMarginFromTop + (this.fhrMinorVerticalInterval / 2.0f) + (rect.height() / 2.0f), this.paintFhrText);
                    }
                }
            } else if (i7 == 0) {
                canvas.drawLine(f, this.fhrMarginFromTop, f, this.fhrOffsetFromTop, this.paintGridMajor);
            } else {
                canvas.drawLine(f, this.fhrMarginFromTop, f, this.fhrOffsetFromTop, this.paintGridMinor);
            }
            i7++;
            if (i7 > 5) { // what is hard code 5?
                i6++;
                if (i6 > 2) { // what is hard code 2?
                    i6 = 0;
                }
                i7 = 0;
            }
            f += this.minorHorizontalInterval;
        }

        // for toco horizontal lines
        for (int i9 = 0; i9 < 21; i9++) {
            float yAxis = this.tocoOffsetFromTop - (i9 * this.tocoMinorVerticalInterval);
            if (i9 % 5 == 0) {
                canvas.drawLine(0.0f, yAxis, this.width, yAxis, this.paintGridMajor);
            } else {
                canvas.drawLine(0.0f, yAxis, this.width, yAxis, this.paintGridMinor);
            }
        }

        // for toco vertical lines
        int i11 = 0;
        int i12 = 0;
        float f14 = 0.0f;
        while (f14 <= this.width) {
            if (i11 != 0) {
                if (i11 == 1) {
                    if (i12 == 0) {
                        canvas.drawLine(f14, this.tocoMarginFromTop, f14, this.tocoOffsetFromTop, this.paintGridMajor);
                    } else if (i12 < 5) {
                        canvas.drawLine(f14, this.tocoMarginFromTop, f14, this.tocoOffsetFromTop - (this.tocoMinorVerticalInterval * 2.0f), this.paintGridMinor);
                    } else {
                        canvas.drawLine(f14, this.tocoMarginFromTop, f14, this.tocoOffsetFromTop, this.paintGridMinor);
                        float y = this.tocoOffsetFromTop - this.tocoMinorVerticalInterval;
                        canvas.drawLine(f14, y, f14 + (this.minorHorizontalInterval * 4.0f), y, this.paintGridMinor);
                    }
                    if (i12 == 1) {
                        Rect rect = new Rect();
                        this.paintTocoMmhgText.getTextBounds("UA", i4, 2, rect);
                        canvas.drawText("UA", f14 - (rect.width() / 2.0f), (this.tocoOffsetFromTop - this.tocoMinorVerticalInterval) + (rect.height() / 2.0f), this.paintTocoMmhgText);
                    } else if (i12 == 2) {
                        int i13 = 1;
                        while (i13 < 4) {
                            Rect rect = new Rect();
                            String valueOf2 = String.valueOf(i13 * 25);
                            this.paintTocoMmhgText.getTextBounds(valueOf2, i4, valueOf2.length(), rect);
                            float height3 = (this.tocoOffsetFromTop - ((i13 * 5) * this.tocoMinorVerticalInterval)) + (rect.height() / 2.0f);
                            float width3 = f14 - (rect.width() / 2.0f);
                            canvas.drawRect(width3, height3 - rect.height(), width3 + rect.width(), height3, this.paintTextBg);
                            canvas.drawText(valueOf2, width3, height3, this.paintTocoMmhgText);
                            i13++;
                        }
                        Rect rect9 = new Rect();
                        this.paintTocoMmhgText.getTextBounds("0", i4, 1, rect9);
                        canvas.drawText("0", f14 - (rect9.width() / 2.0f), (this.tocoOffsetFromTop - this.tocoMinorVerticalInterval) + (rect9.height() / 2.0f), this.paintTocoMmhgText);
                        this.paintTocoMmhgText.getTextBounds("100", i4, 3, rect9);
                        float height4 = this.tocoMarginFromTop + this.tocoMinorVerticalInterval + (rect9.height() / 2.0f);
                        float width4 = f14 - (rect9.width() / 2.0f);
                        canvas.drawRect(width4, height4 - rect9.height(), width4 + rect9.width(), height4, this.paintTextBg);
                        canvas.drawText("100", width4, height4, this.paintFhrText);
                    } else if (i12 == 3) {
                        Rect rect = new Rect();
                        this.paintTocoMmhgText.getTextBounds("m", i4, 1, rect);
                        this.paintTocoMmhgText.getTextBounds("H", i4, 1, rect);
                        canvas.drawText("mmHg", f14 - (rect.width() / 2.0f), (this.tocoOffsetFromTop - this.tocoMinorVerticalInterval) + (rect.height() / 2.0f), this.paintTocoMmhgText);
                    }
                } else if (i11 == 2) {
                    if (i12 == 0) {
                        canvas.drawLine(f14, this.tocoMarginFromTop, f14, this.tocoOffsetFromTop, this.paintGridMajor);
                    } else if (i12 > 3) {
                        canvas.drawLine(f14, this.tocoMarginFromTop, f14, this.tocoOffsetFromTop - (this.tocoMinorVerticalInterval * 2.0f), this.paintGridMinor);
                    } else {
                        canvas.drawLine(f14, this.tocoMarginFromTop, f14, this.tocoOffsetFromTop, this.paintGridMinor);
                    }
                    if (i12 == 4) {
                        int i15 = 1;
                        while (i15 < 7) {
                            Rect rect11 = new Rect();
                            String valueOf3 = String.valueOf(i15 * 2);
                            this.paintTocoKpaText.getTextBounds(valueOf3, i4, valueOf3.length(), rect11);
                            float height5 = (this.tocoOffsetFromTop - ((i15 * 3) * this.tocoMinorVerticalInterval)) + (rect11.height() / 2.0f);
                            float width5 = f14 - (rect11.width() / 2.0f);
                            canvas.drawRect(width5, height5 - rect11.height(), width5 + rect11.width(), height5, this.paintTextBg);
                            canvas.drawText(valueOf3, width5, height5, this.paintTocoKpaText);
                            i15++;
                            i4 = 0;
                        }
                        Rect rect2 = new Rect();
                        this.paintTocoKpaText.getTextBounds("0", 0, 1, rect2);
                        canvas.drawText("0", f14 - (rect2.width() / 2.0f), (this.tocoOffsetFromTop - this.tocoMinorVerticalInterval) + (rect2.height() / 2.0f), this.paintTocoKpaText);
                    } else if (i12 == 5) {
                        Rect rect = new Rect();
                        this.paintTocoKpaText.getTextBounds("kPa", 0, 3, rect);
                        canvas.drawText("kPa", f14 - (rect.width() / 2.0f), (this.tocoOffsetFromTop - this.tocoMinorVerticalInterval) + (rect.height() / 2.0f), this.paintTocoKpaText);
                    }
                }
            } else if (i12 == 0) {
                canvas.drawLine(f14, this.tocoMarginFromTop, f14, this.tocoOffsetFromTop, this.paintGridMajor);
                float startY = this.tocoOffsetFromTop - this.tocoMinorVerticalInterval;
                float stopX = f14 + (this.minorHorizontalInterval * 6.0f);
                canvas.drawLine(f14, startY, stopX, startY, this.paintGridMinor);
            } else {
                canvas.drawLine(f14, this.tocoMarginFromTop, f14, this.tocoOffsetFromTop, this.paintGridMinor);
            }
            i12++;
            if (i12 > 5) {
                i11++;
                if (i11 > 2) {
                    i11 = 0;
                }
                i12 = 0;
            }
            f14 = f14 + this.minorHorizontalInterval;
        }

    }

    @Override
    public void run() {

        this.thread = Thread.currentThread();
        this.running = true;
        long currentTimeMillis = System.currentTimeMillis();

        Log.d(TAG, "Started CTG Trace");
        while (this.running) {
            if ((System.currentTimeMillis() - currentTimeMillis) >= 999) {
                synchronized (this.mutex) {
                    drawChart(this.screenDensity);
                }
                currentTimeMillis = System.currentTimeMillis();
            } else {
                try {
                    Thread.sleep(250L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            }
        }
        Log.d(TAG, "CTG Trace is stopped");

    }

    public void stop() {
        synchronized (this.mutex) {
            this.running = false;
            if (this.thread != null) {
                this.thread.interrupt();
            }
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void setScreenDensity(float screenDensity) {
        this.screenDensity = screenDensity;
    }

}
