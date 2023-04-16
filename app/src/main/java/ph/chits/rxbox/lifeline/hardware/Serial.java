package ph.chits.rxbox.lifeline.hardware;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;
import com.hoho.android.usbserial.driver.UsbId;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Serial implements ActionListener, AutoCloseable {
    private final String TAG = this.getClass().getSimpleName();
    private final String ACTION_USB_PERMISSION = "ph.chits.rxbox.lifeline";

    private AppCompatActivity activity;

    private UsbDeviceConnection connection;
    private UsbSerialPort port;

    private SerialIoPipe serialIoPipe;
    private Parser parser;
    private final Data data = new Data();

    private boolean connected = false;

    public Serial(AppCompatActivity activity) {
        this.activity = activity;
    }

    private final BroadcastReceiver usbPermissionReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        Log.d(TAG, "usb permission granted");
                        if (device != null) {
                            //call method to set up device communication
                            setup();
                        } else {
                            Log.d(TAG, "device null");
                        }
                        activity.unregisterReceiver(usbPermissionReceiver);
                    } else {
                        Log.d(TAG, "permission denied for device " + device);
                    }
                }
            }
        }
    };

    public void setup() {
        UsbManager manager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
        if (manager == null) {
            Log.e(TAG, "device has no usb service");
            Toast.makeText(activity.getApplicationContext(), "RxBox isn't running: No USB permission", Toast.LENGTH_LONG).show();
            return;
        }

        // Find all available drivers from attached devices.
        ProbeTable probeTable = UsbSerialProber.getDefaultProbeTable();
        probeTable.addProduct(0x2A03, UsbId.ARDUINO_MEGA_ADK_R3, CdcAcmSerialDriver.class);
        List<UsbSerialDriver> availableDrivers = new UsbSerialProber(probeTable).findAllDrivers(manager);
        //List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);

        if (availableDrivers.isEmpty()) {
            Log.d(TAG, "no available drivers");
            Toast.makeText(activity.getApplicationContext(), "RxBox isn't running: Can't find device driver", Toast.LENGTH_LONG).show();
            return;
        }

        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);
        this.connection = manager.openDevice(driver.getDevice());
        if (this.connection == null) {
            // add UsbManager.requestPermission(driver.getDevice(), ..) handling here

            PendingIntent permissionIntent = PendingIntent.getBroadcast(activity, 0, new Intent(ACTION_USB_PERMISSION), 0);
            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
            activity.registerReceiver(usbPermissionReceiver, filter);
            manager.requestPermission(driver.getDevice(), permissionIntent);
            Log.d(TAG, "connection null");
            Toast.makeText(activity.getApplicationContext(), "RxBox isn't running: Can't open USB Connection", Toast.LENGTH_LONG).show();
            return;
        }

        port = driver.getPorts().get(0); // Most devices have just one port (port 0)
        try {

            port.open(connection);
            port.setParameters(250000, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            data.setSubscriber((Data.Subscriber) activity);
            serialIoPipe = new SerialIoPipe(port);
            parser = new Parser(serialIoPipe.rx(), data, this);
            parser.start();
            serialIoPipe.start();
            resetEcgSettings();
            this.connected = true;

        } catch (IOException e) {
            Log.d(TAG, "IOException", e);
        }

        if (this.connected) {
            Toast.makeText(activity.getApplicationContext(), "RxBox is connected", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(activity.getApplicationContext(), "RxBox isn't running", Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void close() throws Exception {

        if (this.parser != null)
            this.parser.stop();

        if (this.serialIoPipe != null)
            this.serialIoPipe.stop();

        if (this.port != null)
            this.port.close();

        if (this.connection != null)
            this.connection.close();

    }

    public boolean startBP() {
        if (!this.connected) {
            Toast.makeText(activity.getApplicationContext(), "RxBox is turned off", Toast.LENGTH_LONG).show();
        } else {
            try {
                serialIoPipe.tx().write(Protocol.BP_START_MEASUREMENT);
                data.setBpIdle(false);
                return true;
            } catch (IOException e) {
                Log.d(TAG, "failed to start BP measurement");
            }
        }
        return false;
    }

    public boolean stopBP() {
        if (!this.connected) {
            Toast.makeText(activity.getApplicationContext(), "RxBox is turned off", Toast.LENGTH_LONG).show();
        } else {
            try {
                serialIoPipe.tx().write(Protocol.BP_ABORT);
                data.setBpIdle(true);
                return true;
            } catch (IOException e) {
                Log.d(TAG, "failed to abort BP measurement");
            }
        }
        return false;
    }

    public boolean resetTocoToZero() {
        if (!this.connected) {
            Toast.makeText(activity.getApplicationContext(), "RxBox is turned off", Toast.LENGTH_LONG).show();
        } else {
            try {
                serialIoPipe.tx().write(Protocol.FM_RESET_TOCO_ZERO);
            } catch (IOException e) {
                Log.d(TAG, "failed to reset Toco measurement");
            }
        }
        return false;
    }

    public Data getData() {
        return data;
    }

    public void resetEcgSettings() {
        try {
            serialIoPipe.tx().write(Arrays.toString(Protocol.SET_ECG_DEFAULTS).getBytes());
        } catch (IOException e) {
            Log.d(TAG, "failed to reset ecg settings");
        }
    }

    @Override
    public void requestBpData() {
        try {
            serialIoPipe.tx().write(Protocol.BP_REQUEST_DATA);
        } catch (IOException e) {
            Log.d(TAG, "failed to request BP");
        }
    }

    public boolean isConnected() {
        return connected;
    }

}
