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

    private enum State {
        RESET,
        READING_PATIENT_QUEUE,
        READING_PATIENT_QUEUE_NOT_OK,
        READING_PATIENT_QUEUE_INVALID_BODY,
        READING_PATIENT_QUEUE_ERROR,
        EMPTY_PATIENT_QUEUE,
        READING_PATIENT_DETAILS,
        READING_PATIENT_DETAILS_NOT_OK,
        READING_PATIENT_DETAILS_INVALID_BODY,
        READING_PATIENT_DETAILS_ERROR,
        READING_PATIENT_DETAILS_SUCCESS
    }

    private List<Patient> patients = new ArrayList<>();
    private List<Patient> filteredPatients = new ArrayList<>();
    PatientAdapter patientAdapter;
    private FhirService fhirService;
    private Call<PatientQueue> patientQueueCall;
    private List<Call<ph.chits.rxbox.lifeline.rest.Patient>> patientDetailsCalls;
    CountDownLatch latch;
    List<String> patientIds;

    private ProgressBar progressBar;
    private TextView textMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_patient);

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
    }

    @Override
    protected void onResume() {
        super.onResume();

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

        updateUi(State.RESET, State.READING_PATIENT_QUEUE);

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
                        updateUi(State.READING_PATIENT_QUEUE_INVALID_BODY);
                    }
                } else {
                    updateUi(State.READING_PATIENT_QUEUE_NOT_OK);
                }
            }

            @Override
            public void onFailure(Call<PatientQueue> call, Throwable t) {
                //updateUi(State.READING_PATIENT_QUEUE_ERROR);
                Log.d(TAG, "failure", t);
            }
        });
    }

    private void readPatientDetails(PatientQueue queue) {
        patientIds = queue.getPatientIds();
        if (patientIds.size() <= 0) {
            updateUi(State.EMPTY_PATIENT_QUEUE);
        } else {
            latch = new CountDownLatch(patientIds.size());
            updateUi(State.READING_PATIENT_DETAILS);

            patientDetailsCalls = new ArrayList<>(patientIds.size());
            for (String id : patientIds) {
                Call<ph.chits.rxbox.lifeline.rest.Patient> patientCall = fhirService.readPatient(id);
                patientDetailsCalls.add(patientCall);
                patientCall.enqueue(new Callback<ph.chits.rxbox.lifeline.rest.Patient>() {
                    @Override
                    public void onResponse(Call<ph.chits.rxbox.lifeline.rest.Patient> call, Response<ph.chits.rxbox.lifeline.rest.Patient> response) {
                        if (response.code() == 200) {
                            if (response.body() != null) {
                                Patient patient = new Patient(response.body().getId(),
                                        response.body().getName(),
                                        response.body().getGender(),
                                        response.body().getBirthdate()
                                );
                                patients.add(patient);
                                //filteredPatients.add(patient);
                                //patientAdapter.notifyItemInserted(filteredPatients.size() - 1);

                                latch.countDown();
                                updateUi(State.READING_PATIENT_DETAILS);
                            } else {
                                updateUi(State.READING_PATIENT_DETAILS_INVALID_BODY);

                                for (Call<ph.chits.rxbox.lifeline.rest.Patient> patientCall : patientDetailsCalls) {
                                    patientCall.cancel();
                                }
                            }
                        } else {
                            updateUi(State.READING_PATIENT_DETAILS_NOT_OK);
                        }
                    }

                    @Override
                    public void onFailure(Call<ph.chits.rxbox.lifeline.rest.Patient> call, Throwable t) {
                        //updateUi(State.READING_PATIENT_DETAILS_ERROR);
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

    private void updateUi(State... states) {
        for (State s : states) {
            updateUi(s);
        }
    }

    private void updateUi(State state) {
        switch (state) {
            case RESET:
                patients.clear();
                filteredPatients.clear();
                patientAdapter.notifyDataSetChanged();
                patientAdapter.clearSelected();
                progressBar.setIndeterminate(true);
                break;
            case READING_PATIENT_QUEUE:
                progressBar.setVisibility(View.VISIBLE);
                textMessage.setVisibility(View.GONE);
                break;
            case READING_PATIENT_DETAILS_INVALID_BODY:
            case READING_PATIENT_DETAILS_NOT_OK:
            case READING_PATIENT_DETAILS_ERROR:
                textMessage.setText(R.string.patient_queue_error);
                textMessage.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                break;
            case EMPTY_PATIENT_QUEUE:
                textMessage.setText(R.string.patient_queue_empty);
                textMessage.setVisibility(View.VISIBLE);
                progressBar.setProgress(100);
                progressBar.setIndeterminate(false);
                progressBar.setVisibility(View.GONE);
                break;
            case READING_PATIENT_DETAILS:
                progressBar.setIndeterminate(false);
                int progress = Math.round(100 * (1 - (float) latch.getCount() / patientIds.size()));
                progressBar.setProgress(progress);
                Log.d(TAG, "progress " + progress);
                if (latch.getCount() == 0) {
                    progressBar.setVisibility(View.GONE);
                    updateUi(State.READING_PATIENT_DETAILS_SUCCESS);
                }
                break;
            case READING_PATIENT_QUEUE_INVALID_BODY:
            case READING_PATIENT_QUEUE_NOT_OK:
            case READING_PATIENT_QUEUE_ERROR:
                patients.clear();
                filteredPatients.clear();
                patientAdapter.notifyDataSetChanged();
                textMessage.setText(R.string.patient_queue_error);
                textMessage.setVisibility(View.VISIBLE);
                break;
            case READING_PATIENT_DETAILS_SUCCESS:
                filteredPatients.addAll(patients);
                patientAdapter.notifyDataSetChanged();
        }
    }
}
