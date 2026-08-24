package com.system.optimizer.utility;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Simple elegant text view for splash
        TextView tv = new TextView(this);
        tv.setText("🔒 Private World\nSecure & Fast Streaming");
        tv.setTextSize(22);
        tv.setTextColor(android.graphics.Color.WHITE);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setBackgroundColor(android.graphics.Color.parseColor("#0F051D"));
        setContentView(tv);

        // Fade-in animation
        Animation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(1500);
        tv.startAnimation(fadeIn);

        // 2 seconds ke baad MainActivity par move karega
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }, 2000);
    }
}
