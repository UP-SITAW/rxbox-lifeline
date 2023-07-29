package ph.chits.rxbox.lifeline.hardware;

import android.util.Log;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Data implements DataListener {
    public static class ObsQuantity<T> {
        T value;
        Date measured;

        public ObsQuantity(T value, Date measured) {
            this.value = value;
            this.measured = measured;
        }

        public T getValue() {
            return value;
        }

        public void setValue(T value) {
            this.value = value;
        }

        public Date getMeasured() {
            return measured;
        }

        public void setMeasured(Date measured) {
            this.measured = measured;
        }

        public boolean isRecent() {
            Date now = Calendar.getInstance().getTime();
            long diff = Math.abs(now.getTime() - measured.getTime());
            return diff < 10 * 1000;
        }
    }

    public static class Bp {
        Integer systolic, diastolic, map;

        public Bp(Integer systolic, Integer diastolic, Integer map) {
            this.systolic = systolic;
            this.diastolic = diastolic;
            this.map = map;
        }

        public Integer getSystolic() {
            return systolic;
        }

        public Integer getDiastolic() {
            return diastolic;
        }

        public Integer getMap() {
            return map;
        }
    }

    public static interface Subscriber {
        void bpResult(Bp bp, Date date);
        void fetalMonitorUpdate(Integer fhr, Integer tocoPressure, Boolean mark);
    }

    private final String TAG = this.getClass().getSimpleName();

    private final AtomicReference<ObsQuantity<Integer>> spo2 = new AtomicReference<>(new ObsQuantity<Integer>(null, Calendar.getInstance().getTime()));
    private final AtomicReference<ObsQuantity<Integer>> pulseRate = new AtomicReference<>(new ObsQuantity<Integer>(null, Calendar.getInstance().getTime()));
    private final AtomicReference<ObsQuantity<Integer>> respirationRate = new AtomicReference<>(new ObsQuantity<Integer>(null, Calendar.getInstance().getTime()));
    private final AtomicReference<ObsQuantity<Integer>> heartRate = new AtomicReference<>(new ObsQuantity<Integer>(null, Calendar.getInstance().getTime()));
    private final AtomicReference<ObsQuantity<Float>> temperature = new AtomicReference<>(new ObsQuantity<Float>(null, Calendar.getInstance().getTime()));

    private final AtomicReference<ObsQuantity<Integer>> fetalHeartRate = new AtomicReference<>(new ObsQuantity<Integer>(null, Calendar.getInstance().getTime()));
    private final AtomicReference<ObsQuantity<Integer>> tocometerPressure = new AtomicReference<>(new ObsQuantity<Integer>(null, Calendar.getInstance().getTime()));
    private final AtomicReference<ObsQuantity<Boolean>> markPressed = new AtomicReference<>(new ObsQuantity<Boolean>(null, Calendar.getInstance().getTime()));

    private final AtomicReference<ObsQuantity<Bp>> bp = new AtomicReference<>(new ObsQuantity<Bp>(new Bp(null, null, null), Calendar.getInstance().getTime()));
    private final AtomicInteger cuffPressure = new AtomicInteger(0);
    private final AtomicInteger bpError = new AtomicInteger(-1);
    private final AtomicBoolean bpIdle = new AtomicBoolean(true);

    private final AtomicBoolean tempProbeConnected = new AtomicBoolean(false);
    private final AtomicBoolean pulseOxConnected = new AtomicBoolean(false);

    private Subscriber subscriber;

    public Subscriber getSubscriber() {
        return subscriber;
    }

    public void setSubscriber(Subscriber subscriber) {
        this.subscriber = subscriber;
    }

    @Override
    public void setHeartRate(int heartRate) {
        this.heartRate.set(new ObsQuantity<Integer>(heartRate, Calendar.getInstance().getTime()));
        //Log.d(TAG, "set hr " + heartRate);
    }

    @Override
    public void setRespirationRate(int respirationRate) {
        this.respirationRate.set(new ObsQuantity<Integer>(respirationRate, Calendar.getInstance().getTime()));
    }

    @Override
    public void setTemperature(float temperature) {
        this.temperature.set(new ObsQuantity<Float>(temperature, Calendar.getInstance().getTime()));
    }

    @Override
    public void setTempProbeConnected(boolean connected) {
        this.tempProbeConnected.set(connected);
    }

    @Override
    public void setPulseRate(int pulseRate) {
        this.pulseRate.set(new ObsQuantity<Integer>(pulseRate, Calendar.getInstance().getTime()));
    }

    @Override
    public void setSpo2(int spo2) {
        this.spo2.set(new ObsQuantity<>(spo2, Calendar.getInstance().getTime()));
    }

    @Override
    public void setPulseOxConnected(boolean connected) {
        this.pulseOxConnected.set(connected);
    }

    @Override
    public void setBpCuffPressure(int pressure) {
        cuffPressure.set(pressure);
    }

    @Override
    public void setBpError(int error) {
        this.bpError.set(error);
        this.bpIdle.set(true);
    }

    @Override
    public void setBpResult(int systolic, int diastolic, int map) {
        Bp bp = new Bp(systolic, diastolic, map);
        Date measured = Calendar.getInstance().getTime();
        this.bp.set(new ObsQuantity<Bp>(bp, measured));
        this.bpIdle.set(true);

        if (subscriber != null) {
            Log.d(TAG, "invoking bp result");
            subscriber.bpResult(bp, measured);
        }
    }

    @Override
    public void setBpIdle(boolean idle) {
        this.bpIdle.set(idle);
    }

    @Override
    public void setFetalHeartRate(int fhr) {
        this.fetalHeartRate.set(new ObsQuantity<>(fhr, Calendar.getInstance().getTime()));
    }

    @Override
    public void setTocometerPressure(int tmPressure) {
        this.tocometerPressure.set(new ObsQuantity<>(tmPressure, Calendar.getInstance().getTime()));
    }

    @Override
    public void setMarkPressed(boolean pressed) {
        this.markPressed.set(new ObsQuantity<>(pressed, Calendar.getInstance().getTime()));
    }

    public Integer getFetalHeartRate() {
        return fetalHeartRate.get().getValue();
    }

    public Integer getTocometerPressure() {
        return tocometerPressure.get().getValue();
    }

    public Boolean getMarkPressed() {
        return markPressed.get().getValue();
    }

    public Integer getHeartRate() {
        return heartRate.get().getValue();
    }

    public Integer getRespirationRate() {
        return respirationRate.get().getValue();
    }

    public Float getTemperature() {
        return temperature.get().getValue();
    }

    public Integer getSpo2() {
        return spo2.get().getValue();
    }

    public Integer getPulseRate() {
        return pulseRate.get().getValue();
    }

    public boolean isTempProbeConnected() {
        return tempProbeConnected.get();
    }

    public boolean isPulseOxConnected() {
        return pulseOxConnected.get();
    }

    public boolean isSpo2Recent() {
        return spo2.get().isRecent();
    }

    public boolean isTemperatureRecent() {
        return temperature.get().isRecent();
    }

    public boolean isHeartRateRecent() {
        return heartRate.get().isRecent();
    }

    public boolean isRespirationRateRecent() {
        return respirationRate.get().isRecent();
    }

    public boolean isFetalHeartRateRecent() {
        return this.fetalHeartRate.get().isRecent();
    }

    public boolean isTocometerPressureRecent() {
        return this.tocometerPressure.get().isRecent();
    }

    public boolean isMarkPressedRecent() {
        return this.markPressed.get().isRecent();
    }

    public Bp getBloodPressure() {
        return bp.get().getValue();
    }

    public int getBpError() {
        return bpError.get();
    }

    public int getBpCuffPressure() {
        return cuffPressure.get();
    }

    public boolean isBpIdle() {
        return bpIdle.get();
    }

    @Override
    public void setFetalMonitorUpdate(Integer fhr, Integer tmPressure, Boolean pressed) {
        if (subscriber != null) {
            subscriber.fetalMonitorUpdate(fhr, tmPressure, pressed);
        }
    }
}
