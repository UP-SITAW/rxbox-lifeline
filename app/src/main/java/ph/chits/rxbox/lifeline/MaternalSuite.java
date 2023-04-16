package ph.chits.rxbox.lifeline;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import ph.chits.rxbox.lifeline.hardware.BpState;
import ph.chits.rxbox.lifeline.hardware.Data;
import ph.chits.rxbox.lifeline.hardware.Serial;
import ph.chits.rxbox.lifeline.model.Patient;
import ph.chits.rxbox.lifeline.rest.FhirService;
import ph.chits.rxbox.lifeline.rest.ObservationBp;
import ph.chits.rxbox.lifeline.rest.ObservationQuantity;
import ph.chits.rxbox.lifeline.rest.ObservationReport;
import ph.chits.rxbox.lifeline.util.StringUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MaternalSuite extends AppCompatActivity implements Data.Subscriber {

    private final static String TAG = MaternalSuite.class.getSimpleName();
    private boolean standaloneMode = false;
    private FhirService fhirService = null;
    private Serial serial = new Serial(this);
    private Patient patient = null;
    private static BpState bpState = BpState.IDLE;
    private Integer bpRequestId = null;

    private Future uiUpdateThread;
    private Future serverUpdateThread;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.maternal_suite);

        android.os.Bundle extras = getIntent().getExtras();
        if (extras != null) {
            standaloneMode = extras.getBoolean("standalone", false);
        }

        if (!standaloneMode) {
            SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            String url = sharedPref.getString(getString(R.string.preference_server), null);

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(url)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            fhirService = retrofit.create(FhirService.class);

            SharedPreferences workPref = this.getSharedPreferences(getString(R.string.work_file_key), Context.MODE_PRIVATE);
            patient = new Patient(
                    workPref.getString(getString(R.string.work_patient_id), null),
                    workPref.getString(getString(R.string.work_patient_name), null),
                    workPref.getString(getString(R.string.work_patient_gender), null),
                    StringUtils.parseISO(workPref.getString(getString(R.string.work_patient_birthdate), null))
            );
        } else {
            patient = new Patient(
                    "-100",
                    "STANDALONE MODE",
                    "", null
            );
        }

        MaterialToolbar toolbar = findViewById(R.id.toco_top_bar);
        toolbar.setTitle(patient.getName().toUpperCase());
        View fetalButton = toolbar.findViewById(R.id.fetalmonitor);
        fetalButton.setEnabled(false);
        fetalButton.setVisibility(View.INVISIBLE);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MaternalSuite.this, SelectPatientActivity.class);
                navigateUpTo(intent);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        initializeTiles();
        serial.setup();
        startFuture();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopFuture();

        try {
            this.serial.close();
        } catch (Exception e) {
            Log.e(TAG, "Cant close RxBox connection", e);
        }

    }

    private void initializeTiles() {

        initializeTile(
                findViewById(R.id.tile_fetal_heart_rate),
                ResourcesCompat.getColor(getResources(), R.color.neonEcg, null),
                "Fetal",
                "Heart Rate BPM");

        TextView view = findViewById(R.id.tile_fetal_heart_rate).findViewById(R.id.value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, (view.getTextSize() - 100));

        initializeTile(
                findViewById(R.id.tile_fetal_o2_sat),
                ResourcesCompat.getColor(getResources(), R.color.neonSpo2, null),
                "SPO2",
                "%");
        initializeTile(
                findViewById(R.id.tile_fetal_pulse_rate),
                ResourcesCompat.getColor(getResources(), R.color.neonRespiration, null),
                "PR",
                "BPM");

        initializeTileBp();
        initializeToco();

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
        View tile = findViewById(R.id.tile_fetal_bp);
        TextView titleView = tile.findViewById(R.id.title);
        titleView.setText("NIBP");
        titleView.setTextColor(color);
        TextView subtitleView = tile.findViewById(R.id.subtitle);
        subtitleView.setText("mmHg");
        subtitleView.setTextColor(color);
        TextView valueView = tile.findViewById(R.id.value);
        valueView.setText("--/--");
        valueView.setTextColor(color);
        TextView mapView = tile.findViewById(R.id.map);
        mapView.setText(null);
        mapView.setTextColor(color);
        final Button button = findViewById(R.id.start_stop);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bpState == MaternalSuite.bpState.IDLE) {
                    if(serial.startBP()) {
                        bpState = MaternalSuite.bpState.MEASURING;
                        button.setText("STOP");
                    }
                } else {
                    Log.d(TAG, "stopping bp from UI click");
                    if (serial.stopBP()) {
                        bpState =MaternalSuite.bpState.IDLE;
                        button.setText("BP NOW");
                    }
                }
            }
        });
    }

    private void initializeToco() {
        final int color = ResourcesCompat.getColor(getResources(), R.color.neonOthers, null);
        View tile = findViewById(R.id.tile_tocometer);
        TextView titleView = tile.findViewById(R.id.toco_title);
        titleView.setTextColor(color);
        TextView valueView = tile.findViewById(R.id.toco_value);
        valueView.setText("--");
        valueView.setTextColor(color);
        Button button = findViewById(R.id.toco_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                serial.resetTocoToZero();
            }
        });
    }

    @Override
    public void bpResult(Data.Bp bp, Date date) {
        Log.d(TAG, "starting to send bp");
        try {
            if (!standaloneMode) {
                Log.d(TAG, "sending bp");
                fhirService.createObservation(new ObservationBp(
                        "Observation",
                        String.format(Locale.US, "%11f", Math.random() * 10e6),
                        "final",
                        new ObservationBp.Coding("85354-9", "loinc.org"),
                        String.format(Locale.US, "Patient/%s", patient.getId()),
                        StringUtils.formatISO(date),
                        bp.getSystolic(),
                        bp.getDiastolic(),
                        bp.getMap()
                )).enqueue(new Callback<ObservationReport>() {
                    @Override
                    public void onResponse(Call<ObservationReport> call, Response<ObservationReport> response) {
                        Log.d(TAG, "created bp observation " + response.message());

                        try {
                            fhirService.acknowledgeRequestBp(bpRequestId, "1").enqueue(new Callback<String>() {
                                @Override
                                public void onResponse(Call<String> call, Response<String> response) {
                                    Log.d(TAG, "ack bp");
                                }

                                @Override
                                public void onFailure(Call<String> call, Throwable t) {
                                    Log.d(TAG, "error ack bp");
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        bpRequestId = null;
                    }

                    @Override
                    public void onFailure(Call<ObservationReport> call, Throwable t) {
                        Log.d(TAG, "failed creating bp obs", t);
                        bpRequestId = null;
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            bpRequestId = null;
        }
    }

    private void startFuture() {
        this.uiUpdateThread = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(monitorUpdate, 2, 1, TimeUnit.SECONDS);

        if (!standaloneMode) {
            this.serverUpdateThread = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(serverUpdate, 2, 5, TimeUnit.SECONDS);
        }

    }

    private void stopFuture() {

        if (this.uiUpdateThread != null) {
            this.uiUpdateThread.cancel(true);
        }

        if (this.serverUpdateThread != null) {
            this.serverUpdateThread.cancel(true);
        }

    }

    private Runnable monitorUpdate = () -> {
        runOnUiThread(() -> {

            Data data = serial.getData();

            // Fetal Heart Rate
            TextView viewFhr = findViewById(R.id.tile_fetal_heart_rate).findViewById(R.id.value);
            final Integer fhr = data.getFetalHeartRate();
            if (!data.isFetalHeartRateRecent()) {
                viewFhr.setText("E1");
            } else if (fhr != null && fhr > 0 && fhr <= 255) {
                viewFhr.setText(String.format(Locale.US, "%d", fhr));
            } else {
                viewFhr.setText("--");
            }

            // PULSE RATE
            TextView viewPr = findViewById(R.id.tile_fetal_pulse_rate).findViewById(R.id.value);
            final Integer pr = data.getPulseRate();
            if (!data.isSpo2Recent()) {
                viewPr.setText(R.string.monitor_error_data_timeout);
            } else if (pr != null && data.isPulseOxConnected() && (pr <= 250)) {
                viewPr.setText(String.format(Locale.US, "%d", pr));
            } else {
                viewPr.setText("--");
            }

            // SPO2
            TextView viewSpo2 = findViewById(R.id.tile_fetal_o2_sat).findViewById(R.id.value);
            final Integer spo2 = data.getSpo2();
            if (!data.isSpo2Recent()) {
                viewSpo2.setText(R.string.monitor_error_data_timeout);
            } else if (spo2 != null && data.isPulseOxConnected() && (spo2 <= 100)) {
                viewSpo2.setText(String.format(Locale.US, "%d", spo2));
            } else {
                viewSpo2.setText("--");
            }

            // Tocometer
            TextView viewTocometer = findViewById(R.id.tile_tocometer).findViewById(R.id.toco_value);
            final Integer toco = data.getTocometerPressure();
            if (!data.isTocometerPressureRecent()) {
                viewTocometer.setText("E1");
            } else if (toco != null && toco > -1 && toco <= 255) {
                viewTocometer.setText(String.format(Locale.US, "%d", toco));
            } else {
                viewTocometer.setText("--");
            }

            View tile = findViewById(R.id.tile_fetal_bp);
            TextView valueView = tile.findViewById(R.id.value);
            final Data.Bp bp = data.getBloodPressure();
            String bp_string = "--/--";
            String map_string = "";
            if (bp.getSystolic() != null && bp.getDiastolic() != null && bp.getMap() != null) {
                bp_string = String.format(Locale.US, "%d/%d", bp.getSystolic(), bp.getDiastolic());
                map_string = String.format(Locale.US, "(%d)", bp.getMap());
            }
            valueView.setText(bp_string);
            TextView mapView = tile.findViewById(R.id.map);
            mapView.setText(map_string);

        });
    };

    private Runnable serverUpdate = () -> {

        Data data = this.serial.getData();

        // SPO2
        int spo2 = data.getSpo2();
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

        // Pulse Rate
        int pr = data.getPulseRate();
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

        // fetal heart rate
        int fhr = data.getFetalHeartRate();
        if (fhr > 250) fhr = 0;
        fhirService.createObservation(new ObservationQuantity(
                "Observation",
                String.format(Locale.US, "%11f", Math.random() * 10e6),
                "final",
                new ObservationQuantity.Coding("55283-6", "loinc.org"),
                String.format(Locale.US, "Patient/%s", patient.getId()),
                StringUtils.formatISO(Calendar.getInstance().getTime()),
                new ObservationQuantity.ValueQuantity((float) fhr, "{Beats}/min", "http://unitsofmeasure.org", "Beats / minute")
        )).enqueue(new Callback<ObservationReport>() {
            @Override
            public void onResponse(Call<ObservationReport> call, Response<ObservationReport> response) {
                Log.d(TAG, "created fhr observation " + response.message());
            }

            @Override
            public void onFailure(Call<ObservationReport> call, Throwable t) {
                Log.d(TAG, "failed creating fhr obs", t);
            }
        });

        // Uterine Contraction
        int contraction = data.getTocometerPressure();
        if (contraction > 100) contraction = 0;
        fhirService.createObservation(new ObservationQuantity(
                "Observation",
                String.format(Locale.US, "%11f", Math.random() * 10e6),
                "final",
                new ObservationQuantity.Coding("72196-3", "loinc.org"),
                String.format(Locale.US, "Patient/%s", patient.getId()),
                StringUtils.formatISO(Calendar.getInstance().getTime()),
                new ObservationQuantity.ValueQuantity((float) contraction, "mm[Hg]", "http://unitsofmeasure.org", "mm[Hg]")
        )).enqueue(new Callback<ObservationReport>() {
            @Override
            public void onResponse(Call<ObservationReport> call, Response<ObservationReport> response) {
                Log.d(TAG, "created contraction observation " + response.message());
            }

            @Override
            public void onFailure(Call<ObservationReport> call, Throwable t) {
                Log.d(TAG, "failed creating contraction obs", t);
            }
        });

    };

}
