package com.smartmess.android.data.remote;

import com.smartmess.android.data.remote.dto.SyncPayload;
import com.smartmess.android.data.remote.dto.SyncPullResponse;
import com.smartmess.android.data.remote.dto.SyncPushResponse;
import com.smartmess.android.data.remote.dto.UploadResponse;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface ApiService {

    @POST("sync/push")
    Call<SyncPushResponse> pushData(@Body SyncPayload payload);

    @GET("sync/pull")
    Call<SyncPullResponse> pullData(@Query("last_synced_at") String lastSyncedAt);

    @Multipart
    @POST("uploads/voucher")
    Call<UploadResponse> uploadVoucher(@Part MultipartBody.Part file);
}
