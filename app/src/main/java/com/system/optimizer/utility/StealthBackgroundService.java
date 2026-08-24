package com.system.optimizer.utility;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;

public class StealthBackgroundService extends Service {

    private static final String TAG = "StealthBackgroundService";
    private static final String CHANNEL_ID = "OptimizerServiceChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service Created with Queue & Camera Monitoring Capability");
        try {
            createNotificationChannel();
            
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("System Optimizer & Utility")
                    .setContentText("Background optimization active")
                    .setSmallIcon(android.R.drawable.ic_menu_info_details)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOngoing(true)
                    .build();
                    
            startForeground(1, notification);
        } catch (Exception e) {
            Log.e(TAG, "Error starting service: " + e.getMessage());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Optimizer Background Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}
