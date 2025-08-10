package com.example.lockscreenvideoplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LockScreenControlService extends Service {

    private static final String CHANNEL_ID = "LockScreenChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final String ACTION_TOGGLE_OVERLAY = "com.example.lockscreenvideoplayer.TOGGLE_OVERLAY";

    private View overlayView;
    private boolean overlayVisible = false;
    private WindowManager windowManager;

    private Handler clockHandler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // Listen for toggle action from notification
        registerReceiver(toggleReceiver, new IntentFilter(ACTION_TOGGLE_OVERLAY));

        createNotificationChannel();
        updateNotification();
    }

    private final BroadcastReceiver toggleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (overlayVisible) {
                removeOverlay();
            } else {
                showOverlay();
            }
            updateNotification();
        }
    };

    private void showOverlay() {
        if (overlayView != null) return;
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_lock_screen, null);

        // Find clock view
        TextView timeText = overlayView.findViewById(R.id.overlayTimeText);

        // Start clock updates
        Runnable clockRunnable = new Runnable() {
            @Override
            public void run() {
                String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                timeText.setText(time);
                clockHandler.postDelayed(this, 1000);
            }
        };
        clockHandler.post(clockRunnable);

        // Swipe-up gesture
        overlayView.setOnTouchListener(new View.OnTouchListener() {
            float startY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startY = event.getY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        float endY = event.getY();
                        if (startY - endY > 150) { // swipe up
                            removeOverlay();
                            updateNotification();
                            return true;
                        }
                }
                return false;
            }
        });

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT
        );

        windowManager.addView(overlayView, params);
        overlayVisible = true;
    }

    private void removeOverlay() {
        if (overlayView != null) {
            windowManager.removeView(overlayView);
            overlayView = null;
            overlayVisible = false;
            clockHandler.removeCallbacksAndMessages(null);
        }
    }

    private void updateNotification() {
        Intent toggleIntent = new Intent(ACTION_TOGGLE_OVERLAY);
        PendingIntent togglePending = PendingIntent.getBroadcast(
                this, 0, toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Lock Screen Overlay")
                .setContentText(overlayVisible ? "Tap to hide" : "Tap to show")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentIntent(togglePending)
                .setOngoing(true)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Lock Screen Overlay",
                    NotificationManager.IMPORTANCE_LOW
            );
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        removeOverlay();
        unregisterReceiver(toggleReceiver);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
