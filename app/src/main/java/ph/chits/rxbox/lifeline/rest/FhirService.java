package ph.chits.rxbox.lifeline.rest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface FhirService {
    @GET("Group/patient-queue")
    Call<PatientQueue> readPatientQueue();

    @GET("Patient/{id}")
    Call<Patient> readPatient(@Path("id") String id);

    @POST("Observation")
    Call<ObservationReport> createObservation(@Body ObservationQuantity obs);

    @POST("Observation")
    Call<ObservationReport> createObservation(@Body ObservationBp obs);

    @GET("DeviceRequest")
    Call<CustomDr> readMonitoringSettings(@Query("patient") String patient);

    @FormUrlEncoded
    @POST("sendrequestBP")
    Call<String> acknowledgeRequestBp(@Field("requestid") int requestId, @Field("bpvalue") String bpValue);
}
