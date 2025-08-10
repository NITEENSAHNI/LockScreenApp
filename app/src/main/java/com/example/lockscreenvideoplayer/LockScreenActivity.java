package com.example.lockscreenvideoplayer;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LockScreenActivity extends AppCompatActivity {

    private TextView timeText;
    private TextView mediaText;
    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Show on lock screen & keep screen on
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        // Disable default keyguard if API >= O
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyguardManager.requestDismissKeyguard(this, null);
        }

        setContentView(R.layout.activity_lock_screen);

        timeText = findViewById(R.id.timeText);
        mediaText = findViewById(R.id.mediaText);
        Button exitBtn = findViewById(R.id.exitLockScreenBtn);

        exitBtn.setOnClickListener(v -> {
            finish(); // close overlay
        });

        startClockUpdater();
        startMediaUpdater();
    }

    private void startClockUpdater() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                timeText.setText(time);
                handler.postDelayed(this, 1000);
            }
        }, 0);
    }

    private void startMediaUpdater() {
        MediaSessionManager mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        if (mediaSessionManager != null) {
            try {
                List<MediaController> controllers = mediaSessionManager.getActiveSessions(null);
                if (!controllers.isEmpty()) {
                    MediaController controller = controllers.get(0);
                    if (controller.getMetadata() != null) {
                        String title = controller.getMetadata().getDescription().getTitle() != null
                                ? controller.getMetadata().getDescription().getTitle().toString()
                                : "No title";
                        mediaText.setText(title);
                        return;
                    }
                }
                mediaText.setText("No active media");
            } catch (SecurityException e) {
                mediaText.setText("No permission to control media");
            }
        }
    }
}
