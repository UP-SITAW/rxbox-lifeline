package ph.chits.rxbox.lifeline;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.appbar.MaterialToolbar;

import ph.chits.rxbox.lifeline.dialog.SetServer;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

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
                    case R.id.standalone:
                        startStandaloneMode();
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
                try {
                    SharedPreferences sharedPref = StartActivity.this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                    String url = sharedPref.getString(getString(R.string.preference_server), null);
                    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(url)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                    next();
                } catch (Exception e) {
                    Toast.makeText(StartActivity.this, "Please set the Server URL", Toast.LENGTH_LONG).show();
                }
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

    void startStandaloneMode() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("standalone", true);
        startActivity(intent);
    }
}
