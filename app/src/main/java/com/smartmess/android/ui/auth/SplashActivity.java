package com.smartmess.android.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

import com.smartmess.android.R;
import com.smartmess.android.ui.dashboard.DashboardActivity;
import com.smartmess.android.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isFinishing()) return;
            try {
                // Auto-detect if app crashed previously and hasn't been reported
                android.content.SharedPreferences crashPrefs = com.smartmess.android.engine.CrashTelemetryHandler.getCrashPrefs(this);
                if (crashPrefs.getBoolean(com.smartmess.android.engine.CrashTelemetryHandler.KEY_HAS_CRASH, false)
                        && !crashPrefs.getBoolean(com.smartmess.android.engine.CrashTelemetryHandler.KEY_CRASH_SENT, false)) {
                    Intent crashIntent = new Intent(SplashActivity.this, com.smartmess.android.ui.telemetry.CrashReportActivity.class);
                    crashIntent.putExtra(com.smartmess.android.ui.telemetry.CrashReportActivity.EXTRA_AUTO_SHOW_DIALOG, true);
                    startActivity(crashIntent);
                    finish();
                    return;
                }

                SessionManager sessionManager = new SessionManager(this);
                if (sessionManager.isLoggedIn()) {
                    startActivity(new Intent(SplashActivity.this, com.smartmess.android.ui.MainActivity.class));
                } else {
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                }
            } catch (Throwable t) {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }
            finish();
        }, 1000);
    }
}
