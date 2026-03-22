package com.taskreminder.receiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.core.app.NotificationCompat;

import com.taskreminder.R;
import com.taskreminder.data.AppDatabase;
import com.taskreminder.data.Task;
import com.taskreminder.ui.TaskPopupActivity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlarmReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "task_reminder_channel";
    private static final int NOTIFICATION_ID = 42;

    // Shared prefs key — UnlockReceiver reads this to know whether to show popup
    public static final String PREFS_NAME = "alarm_state";
    public static final String KEY_POPUP_PENDING = "popup_pending";

    @Override
    public void onReceive(Context context, Intent intent) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            List<Task> openTasks = AppDatabase.getInstance(context).taskDao().getOpenTasks();
            if (!openTasks.isEmpty()) {
                // Mark that a popup should show on next unlock
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                prefs.edit().putBoolean(KEY_POPUP_PENDING, true).apply();

                showNotification(context, openTasks.size());

                // Also try to launch popup immediately (works if phone is already unlocked)
                Intent popupIntent = new Intent(context, TaskPopupActivity.class);
                popupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(popupIntent);
            }
        });
    }

    private void showNotification(Context context, int taskCount) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Ensure channel exists
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Daily Task Reminder",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Reminds you of your open tasks every day");
        channel.enableVibration(true);
        nm.createNotificationChannel(channel);

        Intent popupIntent = new Intent(context, TaskPopupActivity.class);
        popupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(
                context, 0, popupIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = taskCount == 1
                ? "You have 1 open task"
                : "You have " + taskCount + " open tasks";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText("Tap to review your task list")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pi);

        nm.notify(NOTIFICATION_ID, builder.build());
    }
}
