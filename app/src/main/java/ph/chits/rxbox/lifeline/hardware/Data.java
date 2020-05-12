package ph.chits.rxbox.lifeline.hardware;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
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

    private final String TAG = this.getClass().getSimpleName();

    private final AtomicReference<ObsQuantity<Integer>> spo2 = new AtomicReference<>(new ObsQuantity<Integer>(null, Calendar.getInstance().getTime()));
    private final AtomicReference<ObsQuantity<Integer>> pulseRate = new AtomicReference<>(new ObsQuantity<Integer>(null, Calendar.getInstance().getTime()));
    private final AtomicReference<ObsQuantity<Integer>> respirationRate = new AtomicReference<>(new ObsQuantity<Integer>(null, Calendar.getInstance().getTime()));
    private final AtomicReference<ObsQuantity<Integer>> heartRate = new AtomicReference<>(new ObsQuantity<Integer>(null, Calendar.getInstance().getTime()));
    private final AtomicReference<ObsQuantity<Float>> temperature = new AtomicReference<>(new ObsQuantity<Float>(null, Calendar.getInstance().getTime()));

    private final AtomicBoolean tempProbeConnected = new AtomicBoolean(false);
    private final AtomicBoolean pulseOxConnected = new AtomicBoolean(false);

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

}
