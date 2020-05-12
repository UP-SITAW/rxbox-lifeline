package ph.chits.rxbox.lifeline;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.appbar.MaterialToolbar;

import ph.chits.rxbox.lifeline.dialog.SetServer;

public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.server:
                        openServerDialog();
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        Button button = findViewById(R.id.button_start);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                next();
            }
        });
    }

    void next() {
        Intent intent = new Intent(this, SelectPatientActivity.class);
        startActivity(intent);
    }

    void openServerDialog() {
        SetServer setServer = new SetServer();
        setServer.show(getSupportFragmentManager(), "setServer");
    }
}
