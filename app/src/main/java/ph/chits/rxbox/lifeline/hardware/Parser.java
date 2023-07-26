package ph.chits.rxbox.lifeline.hardware;

import android.util.Log;

import java.io.IOException;
import java.io.PipedInputStream;
import java.nio.BufferOverflowException;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.concurrent.Executors;

import ph.chits.rxbox.lifeline.hardware.parser.ProtocolID;

public class Parser implements Runnable {
    private enum State {
        STOPPED,
        RUNNING,
        STOPPING
    }

    private boolean reading = false;

    private final String TAG = getClass().getSimpleName();

    private State state = State.STOPPED;
    private PipedInputStream rx;
    private IntBuffer packet = IntBuffer.allocate(10);
    private IntBuffer bp = IntBuffer.allocate(45);

    private DataListener listener;
    private ActionListener actionListener;

    public Parser(PipedInputStream rx, DataListener listener, ActionListener actionListener) {
        this.rx = rx;
        this.listener = listener;
        this.actionListener = actionListener;
    }

    public synchronized State getState() {
        return state;
    }

    public synchronized void setState(State state) {
        this.state = state;
    }

    public void stop() {
        setState(State.STOPPING);
    }

    public void start() {
        Executors.newSingleThreadExecutor().execute(this);
    }

    @Override
    public void run() {
        if (getState() != State.STOPPED) {
            Log.d(TAG, "already running");
            return;
        }
        setState(State.RUNNING);
        Log.d(TAG, "running");

        try {
            while (true) {
                if (getState() != State.RUNNING) {
                    cleanUp();
                    break;
                }
                step();
            }
        } catch (Exception e) {
            Log.w(TAG, "stopping due to: " + e.getMessage(), e);
        } finally {
            setState(State.STOPPED);
            Log.d(TAG, "stopped");

        }
    }

    private void step() throws IOException {
        int r = rx.read();
        if (r < 0) { // reached end of stream
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Log.d(TAG, "sleep interrupted");
            }
            return;
        }

        try {
            if (Protocol.isIdentifier(r)) {
                if (this.reading) {
                    parseData();
                }
                ProtocolID protocolID = ProtocolID.get(r);
                packet.clear().limit(protocolID != null ? protocolID.getLength() : 0);
                packet.put(r);
                this.reading = true;
                //Log.d(TAG, "packet id: "+ String.format("0x%02X", packet.limit() > 0 ? packet.get(0) : 0));
            } else {
                packet.put(r);
            }
        } catch (BufferOverflowException e) { //drop the byte r if buffer overflows
            if (packet.limit() > 0) {
                //Log.d(TAG, "packet buffer ovf. packet: " + String.format("0x%02X", packet.get(0)) + ". limit: " + packet.limit());
            } else {
                //Log.d(TAG, "packet unidentified: " + String.format("0x%02X", r));
            }
        }

        if ((!packet.hasRemaining()) && (packet.limit() > 0)) {
            parseData();
        }
    }

    private void parseData() {
        Protocol.restoreBitSeven(packet.array());
        switch (ProtocolID.get(packet.get(0))) {
            case ECG_WAVEFORM_I_II_V1_RESP:
            case ECG_WAVEFORM_I_II_V1:
                break;
            case ECG_HEART_RATE_RESPIRATION_RATE:
                //Log.d(TAG, "recv ecg hr rr");
                listener.setHeartRate((packet.get(3) << 8) | (packet.get(2)));
                listener.setRespirationRate((packet.get(5) << 8) | (packet.get(4)));
                break;
            case ECG_LEAD_CONNECTIONS_INFO_1:
                break;
            case ECG_BOARD_RESET:
                break;
            case ECG_TEMPERATURE_AND_PROBE:
                listener.setTemperature((float) ((packet.get(4) << 8) | packet.get(3)) / 10f);
                listener.setTempProbeConnected((packet.get(2) & 0x01) == 0x00);
                break;
            case ECG_WAVEFORM_V2_TO_V6:
            case ECG_LEAD_CONNECTIONS_INFO_2:
                break;
            case PULSE_OXIMETER:
                listener.setPulseOxConnected((packet.get(4) & 0x10) == 0);
                listener.setPulseRate(((packet.get(4) & 0x40) << 1) | (packet.get(5)));
                listener.setSpo2(packet.get(6) & 0x7F);
                break;
            case BP_END_CUFF_TX:
                //request data here
                Log.d(TAG, "requesting BP data");
                actionListener.requestBpData();
                break;
            case BP_CUFF_TX_2:
                try {
                    int cuff_pressure = intFromBuffer(bp, 1, 3);
                    listener.setBpCuffPressure(cuff_pressure);
                } catch (NumberFormatException | IndexOutOfBoundsException e) {
                    Log.d(TAG, "failed to read bp cuff pressure", e);
                }
                break;
            case BP_PART_1:
                bp.clear();
            case BP_STATUS_2:
            case BP_STATUS_3:
            case BP_STATUS_4:
            case BP_STATUS_5:
                try {
                    bp.put(Arrays.copyOfRange(packet.array(), 2, 9), 0, 7);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case BP_STATUS_6:
                try {
                    bp.put(Arrays.copyOfRange(packet.array(), 2, 9), 0, 7);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //bp array complete
                int m[] = bp.array();
                StringBuilder sb = new StringBuilder();
                for (int i : m) {
                    sb.append((char) i);
                }
                Log.d(TAG, "bp string: " + sb.toString());
                String status2 = "" + (char) bp.get(2);
                String operation_mode = "" + (char) bp.get(5);
                int error = -1;
                try {
                    error = intFromBuffer(bp, 21, 2);
                    int p_systole = intFromBuffer(bp, 16, 3);
                    int p_diastole = intFromBuffer(bp, 19, 3);
                    int p_map = intFromBuffer(bp, 22, 3);
                    //int bp_hr = intFromBuffer(bp, 27, 3);
                    listener.setBpResult(p_systole, p_diastole, p_map);
                } catch (NumberFormatException | IndexOutOfBoundsException e) {
                    Log.d(TAG, "failed to parse bp data", e);
                    listener.setBpError(error);
                }
                break;
            case FM: {
                int fetalHeartRate = packet.get(2) & 255;
                int tocometerPressure = packet.get(4) & 255;
                int temp = packet.get(6) & 255;
                boolean markPressed = (temp & 16) == 16;
                listener.setFetalHeartRate(fetalHeartRate);
                listener.setTocometerPressure(tocometerPressure);
                listener.setMarkPressed(markPressed);
                listener.setFetalMonitorUpdate(fetalHeartRate, tocometerPressure, markPressed);
                break;
            }
            default:
                Log.d(TAG, "Unsupported Protocol: " + packet.get(0));
                break;
        }
        this.reading = false;
    }

    private void cleanUp() {
        Log.d(TAG, "stopping");
        try {
            this.packet.clear();
            this.bp.clear();
            this.packet = null;
            this.bp = null;
        } catch (Exception e) {
            throw new RuntimeException("Error while stopping parser", e);
        }
        Log.d(TAG, "stopped");
    }

    private String strFromBuffer(IntBuffer buffer, int position, int length) {
        int[] m = Arrays.copyOfRange(buffer.array(), position, position + length);
        StringBuilder sb = new StringBuilder();
        for (int i : m) {
            sb.append((char) i);
        }
        return sb.toString();
    }

    private int intFromBuffer(IntBuffer buffer, int offset, int length) throws NumberFormatException {
        return Integer.parseInt(strFromBuffer(buffer, offset, length));
    }

}
