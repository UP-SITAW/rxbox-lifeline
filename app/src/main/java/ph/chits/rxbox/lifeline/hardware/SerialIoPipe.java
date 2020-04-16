package ph.chits.rxbox.lifeline.hardware;

import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;

public class SerialIoPipe implements Runnable {
    private final String TAG = this.getClass().getSimpleName();

    private final int READ_TIMEOUT_MILLIS = 100;
    private final int WRITE_TIMEOUT_MILLIS = 100;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(8 * 1024); // affects how much to transfer from the usb endpoint. Max is 16k (driver limit)

    private PipedInputStream rxInput, txInput;
    private PipedOutputStream rxOutput, txOutput;

    private UsbSerialPort port;

    private enum State {
        STOPPED,
        RUNNING,
        STOPPING
    }

    private State state = State.STOPPED;

    public SerialIoPipe(UsbSerialPort port) {
        this.port = port;
        try {
            rxInput = new PipedInputStream(512 * 1024); // 512k
            txInput = new PipedInputStream(1024); // 1k
            rxOutput = new PipedOutputStream(rxInput);
            txOutput = new PipedOutputStream(txInput);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public PipedInputStream rx() {
        return rxInput;
    }

    public PipedOutputStream tx() {
        return txOutput;
    }

    private synchronized State getState() {
        return state;
    }

    private synchronized void setState(State state) {
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
            //throw new IllegalStateException("Already running.");
            Log.d(TAG, "already running");
            return;
        }
        setState(State.RUNNING);
        Log.d(TAG, "running");

        try {
            while (true) {
                if (getState() != State.RUNNING) {
                    Log.d(TAG, "stopping");
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
        int len = port.read(readBuffer.array(), READ_TIMEOUT_MILLIS);
        if (len > 0) {
            rxOutput.write(readBuffer.array(), 0, len); // writing to the pipe stream is blocking
            readBuffer.clear();
            //Log.d(TAG, "read " + len + ", pipe " + rxInput.available());
        }

        int len2 = Math.min(txInput.available(), 32);
        if (len2 > 0) {
            byte[] toWrite = new byte[len2];
            len2 = txInput.read(toWrite, 0, len2);
            port.write(toWrite, WRITE_TIMEOUT_MILLIS);
        }

        if (len + len2 <= 0) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Log.d(TAG, "sleep interrupted", e);
            }
        }
    }
}
