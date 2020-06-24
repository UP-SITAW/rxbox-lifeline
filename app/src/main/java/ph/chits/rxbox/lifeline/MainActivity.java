package ph.chits.rxbox.lifeline;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import ph.chits.rxbox.lifeline.hardware.Serial;
import ph.chits.rxbox.lifeline.model.Patient;
import ph.chits.rxbox.lifeline.rest.FhirService;
import ph.chits.rxbox.lifeline.rest.ObservationQuantity;
import ph.chits.rxbox.lifeline.rest.ObservationReport;
import ph.chits.rxbox.lifeline.util.StringUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    private final String TAG = getClass().getSimpleName();

    private Patient patient = new Patient();
    private FhirService fhirService;

    private Serial serial = new Serial(this);

    private ScheduledFuture updatingFuture, sendingFuture;

    private Runnable updateMonitor = new Runnable() {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //Log.d(TAG, "updating monitor");

                    TextView viewTemp = findViewById(R.id.tile_temp).findViewById(R.id.value);
                    final Float temp = serial.getData().getTemperature();
                    if (!serial.getData().isTemperatureRecent()) {
                        viewTemp.setText(R.string.monitor_error_data_timeout);
                    } else if (temp != null && serial.getData().isTempProbeConnected()) {
                        viewTemp.setText(String.format(Locale.US, "%.1f", temp));
                    } else {
                        viewTemp.setText("--");
                    }

                    TextView viewHr = findViewById(R.id.tile_hr).findViewById(R.id.value);
                    final Integer hr = serial.getData().getHeartRate();
                    if (!serial.getData().isHeartRateRecent()) {
                        viewHr.setText(R.string.monitor_error_data_timeout);
                    } else if (hr != null && (hr > 0) && (hr < 255)) {
                        viewHr.setText(String.format(Locale.US, "%d", hr));
                    } else {
                        viewHr.setText("0");
                    }

                    TextView viewSpo2 = findViewById(R.id.tile_spo2).findViewById(R.id.value);
                    final Integer spo2 = serial.getData().getSpo2();
                    if (!serial.getData().isSpo2Recent()) {
                        viewSpo2.setText(R.string.monitor_error_data_timeout);
                    } else if (spo2 != null && serial.getData().isPulseOxConnected() && (spo2 <= 100)) {
                        viewSpo2.setText(String.format(Locale.US, "%d", spo2));
                    } else {
                        viewSpo2.setText("--");
                    }

                    TextView viewPr = findViewById(R.id.tile_pr).findViewById(R.id.value);
                    final Integer pr = serial.getData().getPulseRate();
                    if (!serial.getData().isSpo2Recent()) {
                        viewPr.setText(R.string.monitor_error_data_timeout);
                    } else if (pr != null && serial.getData().isPulseOxConnected() && (pr <= 250)) {
                        viewPr.setText(String.format(Locale.US, "%d", pr));
                    } else {
                        viewPr.setText("--");
                    }

                    TextView viewRr = findViewById(R.id.tile_rr).findViewById(R.id.value);
                    final Integer rr = serial.getData().getRespirationRate();
                    if (!serial.getData().isRespirationRateRecent()) {
                        viewRr.setText(R.string.monitor_error_data_timeout);
                    } else if (rr != null && (rr > 0) && (rr < 255)) {
                        viewRr.setText(String.format(Locale.US, "%d", rr));
                    } else {
                        viewRr.setText("0");
                    }
                }
            });
        }
    };

    private Runnable sendObservations = new Runnable() {
        @Override
        public void run() {
            try {
                Log.d(TAG, "sending");
                int spo2 = serial.getData().getSpo2();
                if (spo2 > 100) spo2 = 0;
                fhirService.createObservation(new ObservationQuantity(
                        "Observation",
                        String.format(Locale.US, "%11f", Math.random() * 10e6),
                        "final",
                        new ObservationQuantity.Coding("59407-7", "loinc.org"),
                        String.format(Locale.US, "Patient/%s", patient.getId()),
                        StringUtils.formatISO(Calendar.getInstance().getTime()),
                        new ObservationQuantity.ValueQuantity((float) spo2, "%", "http://unitsofmeasure.org", "%")
                )).enqueue(new Callback<ObservationReport>() {
                    @Override
                    public void onResponse(Call<ObservationReport> call, Response<ObservationReport> response) {
                        Log.d(TAG, "created spo2 observation " + response.message());
                    }

                    @Override
                    public void onFailure(Call<ObservationReport> call, Throwable t) {
                        Log.d(TAG, "failed creating spo2 obs", t);
                    }
                });

                int rr = serial.getData().getRespirationRate();
                if (rr > 100) rr = 0;
                fhirService.createObservation(new ObservationQuantity(
                        "Observation",
                        String.format(Locale.US, "%11f", Math.random() * 10e6),
                        "final",
                        new ObservationQuantity.Coding("76270-8", "loinc.org"),
                        String.format(Locale.US, "Patient/%s", patient.getId()),
                        StringUtils.formatISO(Calendar.getInstance().getTime()),
                        new ObservationQuantity.ValueQuantity((float) rr, "{Breaths}/min", "http://unitsofmeasure.org", "Breaths / min")
                )).enqueue(new Callback<ObservationReport>() {
                    @Override
                    public void onResponse(Call<ObservationReport> call, Response<ObservationReport> response) {
                        Log.d(TAG, "created rr observation " + response.message());
                    }

                    @Override
                    public void onFailure(Call<ObservationReport> call, Throwable t) {
                        Log.d(TAG, "failed creating rr obs", t);
                    }
                });

                int hr = serial.getData().getHeartRate();
                if (hr > 250) hr = 0;
                fhirService.createObservation(new ObservationQuantity(
                        "Observation",
                        String.format(Locale.US, "%11f", Math.random() * 10e6),
                        "final",
                        new ObservationQuantity.Coding("76282-3", "loinc.org"),
                        String.format(Locale.US, "Patient/%s", patient.getId()),
                        StringUtils.formatISO(Calendar.getInstance().getTime()),
                        new ObservationQuantity.ValueQuantity((float) hr, "{Beats}/min", "http://unitsofmeasure.org", "Beats / minute")
                )).enqueue(new Callback<ObservationReport>() {
                    @Override
                    public void onResponse(Call<ObservationReport> call, Response<ObservationReport> response) {
                        Log.d(TAG, "created hr observation " + response.message());
                    }

                    @Override
                    public void onFailure(Call<ObservationReport> call, Throwable t) {
                        Log.d(TAG, "failed creating hr obs", t);
                    }
                });

                int pr = serial.getData().getPulseRate();
                if (pr > 250) pr = 0;
                fhirService.createObservation(new ObservationQuantity(
                        "Observation",
                        String.format(Locale.US, "%11f", Math.random() * 10e6),
                        "final",
                        new ObservationQuantity.Coding("8889-8", "loinc.org"),
                        String.format(Locale.US, "Patient/%s", patient.getId()),
                        StringUtils.formatISO(Calendar.getInstance().getTime()),
                        new ObservationQuantity.ValueQuantity((float) pr, "{Beats}/min", "http://unitsofmeasure.org", "Beats / minute")
                )).enqueue(new Callback<ObservationReport>() {
                    @Override
                    public void onResponse(Call<ObservationReport> call, Response<ObservationReport> response) {
                        Log.d(TAG, "created hr observation " + response.message());
                    }

                    @Override
                    public void onFailure(Call<ObservationReport> call, Throwable t) {
                        Log.d(TAG, "failed creating hr obs", t);
                    }
                });

                Log.d(TAG, "now=" + StringUtils.formatISO(Calendar.getInstance().getTime()));
                float temp = serial.getData().getTemperature();
                if (!serial.getData().isTempProbeConnected()) temp = 0;
                fhirService.createObservation(new ObservationQuantity(
                        "Observation",
                        String.format(Locale.US, "%11f", Math.random() * 10e6),
                        "final",
                        new ObservationQuantity.Coding("8310-5", "loinc.org"),
                        String.format(Locale.US, "Patient/%s", patient.getId()),
                        StringUtils.formatISO(Calendar.getInstance().getTime()),
                        new ObservationQuantity.ValueQuantity(temp, "Cel", "http://unitsofmeasure.org", "°C")
                )).enqueue(new Callback<ObservationReport>() {
                    @Override
                    public void onResponse(Call<ObservationReport> call, Response<ObservationReport> response) {
                        Log.d(TAG, "created temp observation " + response.message());
                    }

                    @Override
                    public void onFailure(Call<ObservationReport> call, Throwable t) {
                        Log.d(TAG, "failed creating temp obs", t);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String url = sharedPref.getString(getString(R.string.preference_server), null);

        SharedPreferences workPref = this.getSharedPreferences(getString(R.string.work_file_key), Context.MODE_PRIVATE);
        patient = new Patient(
                workPref.getString(getString(R.string.work_patient_id), null),
                workPref.getString(getString(R.string.work_patient_name), null),
                workPref.getString(getString(R.string.work_patient_gender), null),
                StringUtils.parseISO(workPref.getString(getString(R.string.work_patient_birthdate), null))
        );

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setTitle(patient.getName().toUpperCase());

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SelectPatientActivity.class);
                navigateUpTo(intent);
            }
        });

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        fhirService = retrofit.create(FhirService.class);
    }

    @Override
    protected void onResume() {
        super.onResume();

        initializeTiles();
        serial.setup();

        cancelFuture();
        updatingFuture = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(updateMonitor, 2, 1, TimeUnit.SECONDS);
        sendingFuture = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(sendObservations, 2, 1, TimeUnit.SECONDS);
        //runOnUiThread(sendObservations);
    }

    @Override
    protected void onStop() {
        cancelFuture();

        super.onStop();
    }

    private void cancelFuture() {
        if (updatingFuture != null) updatingFuture.cancel(true);
        if (sendingFuture != null) sendingFuture.cancel(true);
    }

    private void initializeTiles() {
        initializeTile(
                findViewById(R.id.tile_hr),
                ResourcesCompat.getColor(getResources(), R.color.neonEcg, null),
                "HR",
                "BPM");
        initializeTile(
                findViewById(R.id.tile_spo2),
                ResourcesCompat.getColor(getResources(), R.color.neonSpo2, null),
                "SPO2",
                "%");
        initializeTile(
                findViewById(R.id.tile_pr),
                ResourcesCompat.getColor(getResources(), R.color.neonSpo2, null),
                "PR",
                "BPM");
        initializeTile(
                findViewById(R.id.tile_rr),
                ResourcesCompat.getColor(getResources(), R.color.neonRespiration, null),
                "RESP",
                "RPM");
        initializeTile(
                findViewById(R.id.tile_temp),
                ResourcesCompat.getColor(getResources(), R.color.neonOthers, null),
                "Temp",
                "°C");
        initializeTileBp();
    }

    private void initializeTile(View tile, int color, String title, String subtitle) {
        TextView titleView = tile.findViewById(R.id.title);
        titleView.setText(title);
        titleView.setTextColor(color);
        TextView subtitleView = tile.findViewById(R.id.subtitle);
        subtitleView.setText(subtitle);
        subtitleView.setTextColor(color);
        TextView valueView = tile.findViewById(R.id.value);
        valueView.setText("--");
        valueView.setTextColor(color);
    }

    private void initializeTileBp() {
        final int color = ResourcesCompat.getColor(getResources(), R.color.neonOthers, null);
        View tile = findViewById(R.id.tile_bp);
        TextView titleView = tile.findViewById(R.id.title);
        titleView.setText("NIBP");
        titleView.setTextColor(color);
        TextView subtitleView = tile.findViewById(R.id.subtitle);
        subtitleView.setText("mmHg");
        subtitleView.setTextColor(color);
        TextView valueView = tile.findViewById(R.id.value);
        valueView.setText("--/--");
        valueView.setTextColor(color);
    }
}
