package com.example.lockscreenvideoplayer;

import android.app.Notification;
import android.content.Intent;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class MyNotificationListener extends NotificationListenerService {

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();
        if (notification != null && notification.extras != null) {
            CharSequence title = notification.extras.getCharSequence(Notification.EXTRA_TITLE);
            CharSequence artist = notification.extras.getCharSequence(Notification.EXTRA_TEXT);
            // Broadcast this info to MainActivity
            Intent intent = new Intent("com.example.lockscreenvideoplayer.MEDIA_INFO_UPDATE");
            intent.putExtra("title", title);
            intent.putExtra("artist", artist);
            sendBroadcast(intent);
        }
    }
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Handle notification removed
    }
}