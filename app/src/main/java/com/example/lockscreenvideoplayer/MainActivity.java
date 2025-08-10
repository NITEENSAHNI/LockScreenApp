package com.example.lockscreenvideoplayer;

import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.session.MediaSessionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView timeText;
    private TextView mediaText;
    private Button lockScreenBtn;
    private final Handler handler = new Handler();

    private static final int REQUEST_CODE_NOTIFICATION_LISTENER = 101;
    private static final int REQUEST_CODE_OVERLAY_PERMISSION = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Show over lockscreen flags for this activity itself
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );

        // Disable default keyguard
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyguardManager.requestDismissKeyguard(this, null);
        }

        setContentView(R.layout.activity_main);

        timeText = findViewById(R.id.timeText);
        mediaText = findViewById(R.id.mediaText);
        lockScreenBtn = findViewById(R.id.lockScreenBtn);

        startClockUpdater();
        checkNotificationPermissionAndStart();
        checkOverlayPermission();

        lockScreenBtn.setOnClickListener(v -> {
            if (Settings.canDrawOverlays(this)) {
                // Start your overlay service
                startService(new Intent(this, LockScreenOverlayService.class));
//                finish(); // Optional: close this activity to simulate lock
            } else {
                // Request overlay permission
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION);
            }
        });
    }

    private void checkNotificationPermissionAndStart() {
        if (!isNotificationServiceEnabled()) {
            Toast.makeText(this, "Please enable Notification Access for this app", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            startActivityForResult(intent, REQUEST_CODE_NOTIFICATION_LISTENER);
        } else {
            startMediaUpdater();
        }
    }

    private void checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            lockScreenBtn.setEnabled(false);
            Toast.makeText(this, "Overlay permission required for lock screen", Toast.LENGTH_LONG).show();
        } else {
            lockScreenBtn.setEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-check notification listener permission when user returns from settings
        if (isNotificationServiceEnabled()) {
            startMediaUpdater();
        }
        checkOverlayPermission();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            if (Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Overlay permission granted", Toast.LENGTH_SHORT).show();
                lockScreenBtn.setEnabled(true);
            } else {
                Toast.makeText(this, "Overlay permission denied", Toast.LENGTH_SHORT).show();
                lockScreenBtn.setEnabled(false);
            }
        } else if (requestCode == REQUEST_CODE_NOTIFICATION_LISTENER) {
            if (isNotificationServiceEnabled()) {
                startMediaUpdater();
            } else {
                Toast.makeText(this, "Notification access not enabled", Toast.LENGTH_SHORT).show();
            }
        }
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
                ComponentName listenerComponent = new ComponentName(this, MyNotificationListener.class);
                List<android.media.session.MediaController> controllers = mediaSessionManager.getActiveSessions(listenerComponent);
                if (!controllers.isEmpty()) {
                    android.media.session.MediaController controller = controllers.get(0);
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
                Toast.makeText(this, "Missing permission to control media.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean isNotificationServiceEnabled() {
        String pkgName = getPackageName();
        final String enabledListeners = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (enabledListeners != null) {
            final String[] names = enabledListeners.split(":");
            for (String name : names) {
                if (name.contains(pkgName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
