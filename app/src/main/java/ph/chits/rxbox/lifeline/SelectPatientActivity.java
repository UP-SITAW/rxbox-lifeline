package ph.chits.rxbox.lifeline;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import ph.chits.rxbox.lifeline.model.Patient;
import ph.chits.rxbox.lifeline.rest.FhirService;
import ph.chits.rxbox.lifeline.rest.PatientQueue;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SelectPatientActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getSimpleName();

    private List<Patient> patients = new ArrayList<>();
    private List<Patient> filteredPatients = new ArrayList<>();
    PatientAdapter patientAdapter;
    private FhirService fhirService;
    private Call<PatientQueue> patientQueueCall;
    private List<Call<ph.chits.rxbox.lifeline.rest.Patient>> patientDetailsCalls;
    CountDownLatch latch;

    private ProgressBar progressBar;
    private TextView textMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_patient);
    }

    @Override
    protected void onResume() {
        super.onResume();

        RecyclerView recycler;
        recycler = findViewById(R.id.list_patients);

        patientAdapter = new PatientAdapter(filteredPatients);
        recycler.setAdapter(patientAdapter);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        progressBar = findViewById(R.id.progress);

        textMessage = findViewById(R.id.text_message);

        Button select_button = findViewById(R.id.button_select);
        select_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int selected = patientAdapter.getSelected();
                if (selected >= 0 && selected < patientAdapter.getItemCount()) {
                    next(filteredPatients.get(selected));
                }
            }
        });

        Button refreshButton = findViewById(R.id.button_refresh);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                readPatientQueue();
            }
        });

        TextInputEditText search = findViewById(R.id.search_field);
        search.addTextChangedListener(new TextWatcher() {
            private Timer timer = new Timer();
            private final long DELAY = 500; // milliseconds

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(final Editable editable) {
                timer.cancel();
                timer = new Timer();
                timer.schedule(
                        new TimerTask() {
                            @Override
                            public void run() {
                                filteredPatients.clear();
                                for (Patient p : patients) {
                                    if (p.getName().toLowerCase().contains(editable.toString().toLowerCase())) {
                                        filteredPatients.add(p);
                                    }
                                }
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        patientAdapter.clearSelected();
                                        patientAdapter.notifyDataSetChanged();
                                    }
                                });

                            }
                        },
                        DELAY
                );
            }
        });

        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String url = sharedPref.getString(getString(R.string.preference_server), null);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        fhirService = retrofit.create(FhirService.class);

        readPatientQueue();
    }

    private void readPatientQueue() {
        // cancel previous calls if any
        if (patientQueueCall != null) {
            patientQueueCall.cancel();
        }
        if (patientDetailsCalls != null) {
            for (Call<ph.chits.rxbox.lifeline.rest.Patient> patientCall : patientDetailsCalls) {
                patientCall.cancel();
            }
        }

        // clear the buffers
        patients.clear();
        filteredPatients.clear();

        // update ui
        patientAdapter.notifyDataSetChanged();
        patientAdapter.clearSelected();
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.VISIBLE);
        textMessage.setVisibility(View.GONE);

        // make the call
        patientQueueCall = fhirService.readPatientQueue();
        patientQueueCall.enqueue(new Callback<PatientQueue>() {
            @Override
            public void onResponse(Call<PatientQueue> call, Response<PatientQueue> response) {
                Log.d(TAG, response.toString());

                if (response.code() == 200) {
                    Log.d(TAG, response.toString());
                    if (response.body() != null) {
                        readPatientDetails(response.body());
                    } else {
                        textMessage.setText(R.string.patient_queue_error);
                        textMessage.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                    }
                } else {
                    textMessage.setText(R.string.patient_queue_error);
                    textMessage.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                }


            }

            @Override
            public void onFailure(Call<PatientQueue> call, Throwable t) {
                Log.d(TAG, "failure", t);
            }
        });
    }

    private void readPatientDetails(PatientQueue queue) {
        final List<String> ids = queue.getPatientIds();
        if (ids.size() <= 0) {
            textMessage.setText(R.string.patient_queue_empty);
            textMessage.setVisibility(View.VISIBLE);
            progressBar.setProgress(100);
            progressBar.setIndeterminate(false);
            progressBar.setVisibility(View.GONE);
        } else {
            progressBar.setProgress(0);
            progressBar.setIndeterminate(false);

            latch = new CountDownLatch(ids.size());

            patientDetailsCalls = new ArrayList<>(ids.size());
            for (String id : ids) {
                Call<ph.chits.rxbox.lifeline.rest.Patient> patientCall = fhirService.readPatient(id);
                patientDetailsCalls.add(patientCall);
                patientCall.enqueue(new Callback<ph.chits.rxbox.lifeline.rest.Patient>() {
                    @Override
                    public void onResponse(Call<ph.chits.rxbox.lifeline.rest.Patient> call, Response<ph.chits.rxbox.lifeline.rest.Patient> response) {
                        Log.d(TAG, "patient " + response.toString());
                        if (response.body() != null) {
                            Patient patient = new Patient(response.body().getId(),
                                    response.body().getName(),
                                    response.body().getGender(),
                                    response.body().getBirthdate()
                            );
                            patients.add(patient);
                            filteredPatients.add(patient);
                            patientAdapter.notifyItemInserted(filteredPatients.size() - 1);

                            latch.countDown();
                            int prog = Math.round(100 * (1 - (float) latch.getCount() / ids.size()));
                            progressBar.setProgress(prog);
                            Log.d(TAG, "progress " + prog);
                            if (latch.getCount() == 0) {
                                progressBar.setVisibility(View.GONE);
                            }
                        } else {
                            patients.clear();
                            filteredPatients.clear();
                            patientAdapter.notifyDataSetChanged();
                            textMessage.setText(R.string.patient_queue_error);
                            textMessage.setVisibility(View.VISIBLE);

                            for (Call<ph.chits.rxbox.lifeline.rest.Patient> patientCall : patientDetailsCalls) {
                                patientCall.cancel();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ph.chits.rxbox.lifeline.rest.Patient> call, Throwable t) {
                        patients.clear();
                        filteredPatients.clear();
                        patientAdapter.notifyDataSetChanged();
                        textMessage.setText(R.string.patient_queue_error);
                        textMessage.setVisibility(View.VISIBLE);
                    }
                });
            }
        }


    }

    private void next(Patient patient) {
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.work_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.work_patient_id), patient.getId());
        editor.putString(getString(R.string.work_patient_name), patient.getName());
        editor.putString(getString(R.string.work_patient_gender), patient.getGender());
        editor.putString(getString(R.string.work_patient_birthdate), patient.getBirthdateAsIso8601());
        editor.apply();

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
