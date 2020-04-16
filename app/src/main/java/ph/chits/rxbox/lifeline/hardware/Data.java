package ph.chits.rxbox.lifeline.hardware;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import ph.chits.rxbox.lifeline.util.AtomicPseudoFloat;

public class Data implements DataListener {
    private final String TAG = this.getClass().getSimpleName();

    private final AtomicInteger spo2 = new AtomicInteger();
    private final AtomicInteger respirationRate = new AtomicInteger();
    private final AtomicInteger heartRate = new AtomicInteger();
    private final AtomicPseudoFloat temperature = new AtomicPseudoFloat();
    private final AtomicBoolean tempProbeConnected = new AtomicBoolean(false);
    private final AtomicBoolean pulseOxConnected = new AtomicBoolean(false);

    @Override
    public void setHeartRate(int heartRate) {
        this.heartRate.set(heartRate);
    }

    @Override
    public void setRespirationRate(int respirationRate) {
        this.respirationRate.set(respirationRate);
    }

    @Override
    public void setTemperature(float temperature) {
        this.temperature.setFloat(temperature);
    }

    @Override
    public void setTempProbeConnected(boolean connected) {
        this.tempProbeConnected.set(connected);
    }

    @Override
    public void setPulseRate(int pulseRate) {
    }

    @Override
    public void setSpo2(int spo2) {
        this.spo2.set(spo2);
        //Log.d(TAG, "spo2: " + spo2);
    }

    @Override
    public void setPulseOxConnected(boolean connected) {
        this.pulseOxConnected.set(connected);
    }

    public int getHeartRate() {
        return heartRate.get();
    }

    public int getRespirationRate() {
        return respirationRate.get();
    }

    public float getTemperature() {
        return temperature.getFloat();
    }

    public int getSpo2() {
        return spo2.get();
    }

    public boolean isTempProbeConnected() {
        return tempProbeConnected.get();
    }

    public boolean isPulseOxConnected() {
        return pulseOxConnected.get();
    }
}
