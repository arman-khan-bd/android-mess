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
