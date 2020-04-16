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

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
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
                            setupSerial();
                        }
                        activity.unregisterReceiver(usbPermissionReceiver);
                    } else {
                        Log.d(TAG, "permission denied for device " + device);
                    }
                }
            }
        }
    };

    private void setupSerial() {
        UsbManager manager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
        if (manager == null) {
            Log.e(TAG, "device has no usb service");
            return;
        }
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            return;
        }

        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            // add UsbManager.requestPermission(driver.getDevice(), ..) handling here
            return;
        }

        port = driver.getPorts().get(0); // Most devices have just one port (port 0)
        try {
            port.open(connection);
            port.setParameters(Protocol.Serial.BAUDRATE, Protocol.Serial.DATABITS, Protocol.Serial.STOP_BITS, Protocol.Serial.PARITY);

            serialIoPipe = new SerialIoPipe(port);
            parser = new Parser(serialIoPipe.rx(), data);
            parser.start();
            serialIoPipe.start();
        } catch (IOException e) {
            Log.d(TAG, "IOException", e);
        }
    }

    public void setupDriver() {
        UsbManager manager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
        if (manager == null) {
            Log.d(TAG, "no usb service");
            return;
        }
        // Find all available drivers from attached devices.
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);

        if (!availableDrivers.isEmpty()) {
            // Open a connection to the first available driver.
            UsbSerialDriver driver = availableDrivers.get(0);

            PendingIntent permissionIntent = PendingIntent.getBroadcast(activity, 0, new Intent(ACTION_USB_PERMISSION), 0);
            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
            activity.registerReceiver(usbPermissionReceiver, filter);
            manager.requestPermission(driver.getDevice(), permissionIntent);
        } else {
            Log.d(TAG, "no drivers available for any of the devices or there are no devices");
        }
    }

    public Data getData() {
        return data;
    }
}
