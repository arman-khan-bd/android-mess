package com.smartmess.android.data.remote;

import com.smartmess.android.data.remote.dto.CreateTicketRequest;
import com.smartmess.android.data.remote.dto.RemoteConfigResponse;
import com.smartmess.android.data.remote.dto.SendTicketMessageRequest;
import com.smartmess.android.data.remote.dto.SyncPayload;
import com.smartmess.android.data.remote.dto.SyncPullResponse;
import com.smartmess.android.data.remote.dto.SyncPushResponse;
import com.smartmess.android.data.remote.dto.TelemetryReportRequest;
import com.smartmess.android.data.remote.dto.TelemetryReportResponse;
import com.smartmess.android.data.remote.dto.TicketDetailResponse;
import com.smartmess.android.data.remote.dto.TicketListResponse;
import com.smartmess.android.data.remote.dto.UploadResponse;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // Offline-First Synchronization
    @POST("sync/push")
    Call<SyncPushResponse> pushData(@Body SyncPayload payload);

    @GET("sync/pull")
    Call<SyncPullResponse> pullData(@Query("last_synced_at") String lastSyncedAt);

    // Media & Voucher Image Upload
    @Multipart
    @POST("uploads/voucher")
    Call<UploadResponse> uploadVoucher(@Part MultipartBody.Part file);

    // Remote Configuration & Force Update Check
    @GET("app/config")
    Call<RemoteConfigResponse> getRemoteConfig();

    // Diagnostics & Crash Telemetry
    @POST("telemetry/report")
    Call<TelemetryReportResponse> sendTelemetryReport(@Body TelemetryReportRequest report);

    // Two-Way Support Desk
    @GET("support/tickets")
    Call<TicketListResponse> getTickets();

    @POST("support/tickets")
    Call<TicketDetailResponse> createTicket(@Body CreateTicketRequest request);

    @GET("support/tickets/{id}/messages")
    Call<TicketDetailResponse> getTicketMessages(@Path("id") long ticketId);

    @POST("support/tickets/{id}/messages")
    Call<TicketDetailResponse> sendTicketMessage(@Path("id") long ticketId, @Body SendTicketMessageRequest request);

    // SaaS Plan Status & Instant Checkout
    @GET("plans")
    Call<com.smartmess.android.data.remote.dto.PlansResponse> getPlans();

    @GET("plans/status")
    Call<com.smartmess.android.data.remote.dto.PlanStatusResponse> getPlanStatus();

    @POST("plans/checkout")
    Call<com.smartmess.android.data.remote.dto.CheckoutResponse> checkoutPlan(@Body com.smartmess.android.data.remote.dto.CheckoutRequest request);

    // Mess Automation Rules & Target Meal Budget
    @POST("mess/settings")
    Call<com.smartmess.android.data.remote.dto.MessSettingsResponse> updateMessSettings(@Body java.util.Map<String, Object> body);
}
