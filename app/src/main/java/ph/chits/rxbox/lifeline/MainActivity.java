package ph.chits.rxbox.lifeline;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import ph.chits.rxbox.lifeline.hardware.Serial;

public class MainActivity extends AppCompatActivity {
    Serial serial = new Serial(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        serial.setupDriver();
    }
}
