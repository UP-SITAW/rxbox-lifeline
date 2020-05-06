package ph.chits.rxbox.lifeline.rest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface FhirService {
    @GET("Group/patient-que")
    Call<PatientQueue> readPatientQueue();

    @GET("Patient/{id}")
    Call<Patient> readPatient(@Path("id") String id);

    @POST("Observation")
    Call<ObservationReport> createObservation(@Body ObservationQuantity obs);
}
