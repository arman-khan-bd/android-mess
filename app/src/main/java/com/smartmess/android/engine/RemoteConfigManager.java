package com.smartmess.android.engine;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.smartmess.android.data.remote.ApiClient;
import com.smartmess.android.data.remote.ApiService;
import com.smartmess.android.data.remote.dto.RemoteConfigResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RemoteConfigManager {

    private static final String TAG = "RemoteConfigManager";

    public interface ConfigCallback {
        void onConfigLoaded(RemoteConfigResponse config);
        void onError(String error);
    }

    public static void checkRemoteConfig(final Activity activity, final ConfigCallback callback) {
        ApiService apiService = ApiClient.getApiService(activity);
        apiService.getRemoteConfig().enqueue(new Callback<RemoteConfigResponse>() {
            @Override
            public void onResponse(Call<RemoteConfigResponse> call, Response<RemoteConfigResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    final RemoteConfigResponse config = response.body();

                    if (!activity.isFinishing()) {
                        handleConfigRules(activity, config);
                    }

                    if (callback != null) {
                        callback.onConfigLoaded(config);
                    }
                } else {
                    if (callback != null) callback.onError("Failed to fetch remote config: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<RemoteConfigResponse> call, Throwable t) {
                Log.w(TAG, "Offline or remote config unreachable: " + t.getMessage());
                if (callback != null) callback.onError(t.getMessage());
            }
        });
    }

    private static void handleConfigRules(final Activity activity, final RemoteConfigResponse config) {
        // 1. Maintenance Mode
        if (config.isMaintenanceMode()) {
            new AlertDialog.Builder(activity)
                    .setTitle("নির্ধারিত রক্ষণাবেক্ষণ")
                    .setMessage(config.getMaintenanceMessage() != null ? config.getMaintenanceMessage() : "SmartMess servers are currently undergoing maintenance.")
                    .setCancelable(false)
                    .setPositiveButton("অ্যাপ বন্ধ করুন", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            activity.finishAffinity();
                        }
                    })
                    .show();
            return;
        }

        // 2. Force Update
        int currentVersionCode = 1;
        try {
            PackageInfo pInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            currentVersionCode = pInfo.versionCode;
        } catch (Exception ignored) {}

        if (config.isForceUpdate() && currentVersionCode < config.getMinVersionCode()) {
            new AlertDialog.Builder(activity)
                    .setTitle("নতুন আপডেট প্রয়োজন")
                    .setMessage("স্মার্ট মেস অ্যাপ ব্যবহার চালিয়ে যেতে নতুন আপডেট প্রয়োজন। দয়া করে সর্বশেষ সংস্করণটি ডাউনলোড করুন।")
                    .setCancelable(false)
                    .setPositiveButton("আপডেট ডাউনলোড করুন", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String url = config.getUpdateUrl() != null ? config.getUpdateUrl() : "https://mess.e-bd.shop/download";
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            activity.startActivity(browserIntent);
                            activity.finishAffinity();
                        }
                    })
                    .show();
            return;
        }

        // 3. Broadcast Announcement
        if (config.getBroadcastAnnouncement() != null && !config.getBroadcastAnnouncement().trim().isEmpty()) {
            Toast.makeText(activity, "📢 " + config.getBroadcastAnnouncement(), Toast.LENGTH_LONG).show();
        }
    }
}
