package com.taskreminder.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.taskreminder.ui.TaskPopupActivity;

/**
 * Listens for ACTION_USER_PRESENT (screen unlocked) and launches the task
 * popup if the alarm fired while the screen was locked.
 *
 * Registered dynamically in TaskReminderApp so it is active whenever the
 * app process is alive, and also statically in the manifest so Android can
 * start the process if needed.
 */
public class UnlockReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_USER_PRESENT.equals(intent.getAction())) return;

        SharedPreferences prefs = context.getSharedPreferences(
                AlarmReceiver.PREFS_NAME, Context.MODE_PRIVATE);
        boolean pending = prefs.getBoolean(AlarmReceiver.KEY_POPUP_PENDING, false);

        if (pending) {
            // Clear the flag so we only show once
            prefs.edit().putBoolean(AlarmReceiver.KEY_POPUP_PENDING, false).apply();

            Intent popupIntent = new Intent(context, TaskPopupActivity.class);
            popupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(popupIntent);
        }
    }
}
