package ph.chits.rxbox.lifeline.hardware;

public interface DataListener {
    void setHeartRate(int heartRate);

    void setRespirationRate(int respirationRate);

    void setTemperature(float temperature);

    void setTempProbeConnected(boolean connected);

    void setPulseRate(int pulseRate);

    void setSpo2(int spo2);

    void setPulseOxConnected(boolean connected);

    void setBpCuffPressure(int pressure);

    void setBpError(int error);

    void setBpResult(int systolic, int diastolic, int map);

    void setBpIdle(boolean idle);
}
