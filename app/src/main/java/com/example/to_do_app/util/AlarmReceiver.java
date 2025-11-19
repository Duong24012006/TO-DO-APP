package com.example.to_do_app.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.example.to_do_app.R;

public class AlarmReceiver extends BroadcastReceiver {

    public static final String EXTRA_NOTIFICATION_ID = "notification_id";
    public static final String EXTRA_ACTIVITY_NAME = "activity_name";
    public static final String EXTRA_ACTIVITY_TIME = "activity_time";

    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        String activityName = intent.getStringExtra(EXTRA_ACTIVITY_NAME);
        String activityTime = intent.getStringExtra(EXTRA_ACTIVITY_TIME);
        int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0);

        String channelId = "schedule_notifications";
        CharSequence channelName = "Schedule Notifications";

        // Create notification channel for Android Oreo and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_add) // Make sure you have this drawable
                .setContentTitle("Công việc sắp tới: " + activityName)
                .setContentText("Thời gian: " + activityTime)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        // Show the notification
        notificationManager.notify(notificationId, builder.build());
    }
}
