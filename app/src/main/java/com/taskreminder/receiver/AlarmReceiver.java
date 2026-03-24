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
import com.taskreminder.data.AlarmScheduler;
import com.taskreminder.data.AppDatabase;
import com.taskreminder.data.Task;
import com.taskreminder.ui.EditTaskActivity;
import com.taskreminder.ui.TaskPopupActivity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlarmReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "task_reminder_channel";
    private static final int NOTIFICATION_ID = 42;

    public static final String PREFS_NAME = "alarm_state";
    public static final String KEY_POPUP_PENDING = "popup_pending";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Immediately re-schedule tomorrow's alarm (setAlarmClock is one-shot)
        AlarmScheduler.scheduleNext(context);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            List<Task> openTasks = AppDatabase.getInstance(context).taskDao().getOpenTasks();
            if (!openTasks.isEmpty()) {
                // Mark popup as pending for UnlockReceiver
                SharedPreferences prefs = context.getSharedPreferences(
                        PREFS_NAME, Context.MODE_PRIVATE);
                prefs.edit().putBoolean(KEY_POPUP_PENDING, true).apply();

                showNotification(context, openTasks.size());

                // Try to launch popup immediately if screen is already unlocked
                Intent popupIntent = new Intent(context, TaskPopupActivity.class);
                popupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                     Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(popupIntent);
            }
        });
    }

    private void showNotification(Context context, int taskCount) {
        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Daily Task Reminder",
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Erinnert dich täglich an offene Tasks");
        channel.enableVibration(true);
        nm.createNotificationChannel(channel);

        // Main tap → open popup
        Intent popupIntent = new Intent(context, TaskPopupActivity.class);
        popupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent popupPi = PendingIntent.getActivity(
                context, 0, popupIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Action button → add new task directly over lock screen
        Intent addIntent = new Intent(context, EditTaskActivity.class);
        addIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent addPi = PendingIntent.getActivity(
                context, 1, addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = taskCount == 1
                ? "Du hast 1 offenen Task"
                : "Du hast " + taskCount + " offene Tasks";

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText("Tippe um deine Tasks zu sehen")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(popupPi)
                .addAction(android.R.drawable.ic_input_add,
                           "➕ Task hinzufügen", addPi);

        nm.notify(NOTIFICATION_ID, builder.build());
    }
}
