package com.taskreminder.data;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import com.taskreminder.receiver.AlarmReceiver;

import java.util.Calendar;

public class AlarmScheduler {

    public static final String PREFS_NAME = "task_reminder_prefs";
    public static final String KEY_ALARM_HOUR = "alarm_hour";
    public static final String KEY_ALARM_MINUTE = "alarm_minute";
    public static final String KEY_ALARM_ENABLED = "alarm_enabled";

    private static final int ALARM_REQUEST_CODE = 1001;

    public static void schedule(Context context, int hour, int minute) {
        // Save settings
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putInt(KEY_ALARM_HOUR, hour)
                .putInt(KEY_ALARM_MINUTE, minute)
                .putBoolean(KEY_ALARM_ENABLED, true)
                .apply();

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = buildPendingIntent(context);

        // Build the trigger time for today (or tomorrow if already passed)
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // Fall back to inexact repeating if exact alarms are not permitted
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
            );
        } else {
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
            );
        }
    }

    public static void cancel(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_ALARM_ENABLED, false).apply();

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(buildPendingIntent(context));
    }

    public static void rescheduleFromPrefs(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(KEY_ALARM_ENABLED, false);
        if (enabled) {
            int hour = prefs.getInt(KEY_ALARM_HOUR, 8);
            int minute = prefs.getInt(KEY_ALARM_MINUTE, 0);
            schedule(context, hour, minute);
        }
    }

    private static PendingIntent buildPendingIntent(Context context) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        return PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
