package com.taskreminder.data;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.taskreminder.receiver.AlarmReceiver;

import java.util.Calendar;

public class AlarmScheduler {

    private static final String TAG = "AlarmScheduler";

    public static final String PREFS_NAME = "task_reminder_prefs";
    public static final String KEY_ALARM_HOUR = "alarm_hour";
    public static final String KEY_ALARM_MINUTE = "alarm_minute";
    public static final String KEY_ALARM_ENABLED = "alarm_enabled";

    private static final int ALARM_REQUEST_CODE = 1001;

    public static String schedule(Context context, int hour, int minute) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putInt(KEY_ALARM_HOUR, hour)
                .putInt(KEY_ALARM_MINUTE, minute)
                .putBoolean(KEY_ALARM_ENABLED, true)
                .apply();

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Build trigger time (today or tomorrow if already passed)
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        long triggerAt = calendar.getTimeInMillis();
        long minutesUntil = (triggerAt - System.currentTimeMillis()) / 1000 / 60;

        PendingIntent alarmPi = buildPendingIntent(context);

        // setAlarmClock() has the highest priority of all alarm types.
        // Android guarantees it fires on time even in Doze mode, and shows
        // a clock icon in the status bar so the user knows an alarm is set.
        // The second PendingIntent (showIntent) is what Android opens if the
        // user taps the clock icon in the status bar — we reuse the same popup.
        AlarmManager.AlarmClockInfo alarmClockInfo =
                new AlarmManager.AlarmClockInfo(triggerAt, alarmPi);
        alarmManager.setAlarmClock(alarmClockInfo, alarmPi);

        String msg = String.format("⏰ Alarm gesetzt für %02d:%02d (in %d Min.)", hour, minute, minutesUntil);
        Log.d(TAG, msg);
        return msg;
    }

    /**
     * Called after each firing to schedule the next day's alarm.
     * AlarmClock alarms are one-shot, so we must re-schedule every time.
     */
    public static void scheduleNext(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(KEY_ALARM_ENABLED, false);
        if (!enabled) return;

        int hour = prefs.getInt(KEY_ALARM_HOUR, 8);
        int minute = prefs.getInt(KEY_ALARM_MINUTE, 0);

        // Always schedule for tomorrow (this is called right after today's alarm fired)
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DAY_OF_YEAR, 1);

        long triggerAt = calendar.getTimeInMillis();
        PendingIntent alarmPi = buildPendingIntent(context);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        AlarmManager.AlarmClockInfo alarmClockInfo =
                new AlarmManager.AlarmClockInfo(triggerAt, alarmPi);
        alarmManager.setAlarmClock(alarmClockInfo, alarmPi);

        Log.d(TAG, "Next alarm scheduled for tomorrow at " + hour + ":" +
                String.format("%02d", minute));
    }

    public static void cancel(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_ALARM_ENABLED, false).apply();
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(buildPendingIntent(context));
        Log.d(TAG, "Alarm cancelled");
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

    public static String getStatus(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(KEY_ALARM_ENABLED, false);
        if (!enabled) return "Alarm nicht gesetzt — tippe auf das Uhr-Symbol.";

        int hour = prefs.getInt(KEY_ALARM_HOUR, 8);
        int minute = prefs.getInt(KEY_ALARM_MINUTE, 0);

        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, hour);
        next.set(Calendar.MINUTE, minute);
        next.set(Calendar.SECOND, 0);
        if (next.getTimeInMillis() <= System.currentTimeMillis()) {
            next.add(Calendar.DAY_OF_YEAR, 1);
        }
        long minutesUntil = (next.getTimeInMillis() - System.currentTimeMillis()) / 1000 / 60;

        return String.format("Alarm: %02d:%02d | In: %d Min. | Modus: ✅ AlarmClock (höchste Priorität)",
                hour, minute, minutesUntil);
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
