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

public class Serial {
    private final String TAG = this.getClass().getSimpleName();
    private final String ACTION_USB_PERMISSION = "ph.chits.rxbox.lifeline";

    private AppCompatActivity activity;
    private UsbSerialPort port;

    private SerialIoPipe serialIoPipe;
    private Parser parser;
    private final Data data = new Data();

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
            return;
        }

        // Find all available drivers from attached devices.
        ProbeTable probeTable = UsbSerialProber.getDefaultProbeTable();
        probeTable.addProduct(0x2A03, UsbId.ARDUINO_MEGA_ADK_R3, CdcAcmSerialDriver.class);
        List<UsbSerialDriver> availableDrivers = new UsbSerialProber(probeTable).findAllDrivers(manager);
        //List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);

        if (availableDrivers.isEmpty()) {
            Log.d(TAG, "no available drivers");
            return;
        }

        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            // add UsbManager.requestPermission(driver.getDevice(), ..) handling here

            PendingIntent permissionIntent = PendingIntent.getBroadcast(activity, 0, new Intent(ACTION_USB_PERMISSION), 0);
            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
            activity.registerReceiver(usbPermissionReceiver, filter);
            manager.requestPermission(driver.getDevice(), permissionIntent);
            Log.d(TAG, "connection null");
            return;
        }

        port = driver.getPorts().get(0); // Most devices have just one port (port 0)
        try {
            port.open(connection);
            port.setParameters(250000, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

            serialIoPipe = new SerialIoPipe(port);
            parser = new Parser(serialIoPipe.rx(), data);
            parser.start();
            serialIoPipe.start();

            resetEcgSettings();
        } catch (IOException e) {
            Log.d(TAG, "IOException", e);
        }
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
}
