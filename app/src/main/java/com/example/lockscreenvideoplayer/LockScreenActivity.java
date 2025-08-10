package com.example.lockscreenvideoplayer;

import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LockScreenActivity extends AppCompatActivity {

    private TextView timeText;
    private Handler clockHandler = new Handler();
    private float startY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Show activity over lock screen
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        setContentView(R.layout.activity_lock_screen);

        // Apply immersive mode
        enterImmersiveMode();

        timeText = findViewById(R.id.timeText);
        startClock();

        // Exit button
        findViewById(R.id.exitLockScreenBtn).setOnClickListener(v -> finish());

        // Swipe-up gesture anywhere on the main layout
        findViewById(R.id.lockScreenRoot).setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startY = event.getY();
                    return true;
                case MotionEvent.ACTION_UP:
                    float endY = event.getY();
                    if (startY - endY > 150) { // Swipe up threshold
                        finish();
                        return true;
                    }
            }
            return false;
        });

        // Reapply immersive mode if system UI changes
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(
                visibility -> enterImmersiveMode()
        );
    }

    private void startClock() {
        clockHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Show hours and minutes; change to "HH:mm:ss" if you want seconds
                String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                timeText.setText(time);
                clockHandler.postDelayed(this, 1000);
            }
        }, 0);
    }

    private void enterImmersiveMode() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        enterImmersiveMode();
    }

    @Override
    protected void onDestroy() {
        clockHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
