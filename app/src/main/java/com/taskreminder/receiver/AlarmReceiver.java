package com.taskreminder.receiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

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

    @Override
    public void onReceive(Context context, Intent intent) {
        // Query open tasks on a background thread
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            List<Task> openTasks = AppDatabase.getInstance(context).taskDao().getOpenTasks();
            if (!openTasks.isEmpty()) {
                showNotification(context, openTasks.size());
                launchPopup(context);
            }
        });
    }

    private void showNotification(Context context, int taskCount) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create channel (no-op on subsequent calls)
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Daily Task Reminder",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Reminds you of your open tasks every day");
        nm.createNotificationChannel(channel);

        // Tap intent → open popup
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
                .setAutoCancel(true)
                .setContentIntent(pi);

        nm.notify(NOTIFICATION_ID, builder.build());
    }

    private void launchPopup(Context context) {
        Intent popupIntent = new Intent(context, TaskPopupActivity.class);
        popupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(popupIntent);
    }
}
