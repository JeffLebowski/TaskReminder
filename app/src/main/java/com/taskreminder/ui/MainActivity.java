package com.taskreminder.ui;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.taskreminder.R;
import com.taskreminder.data.AlarmScheduler;
import com.taskreminder.data.AppDatabase;
import com.taskreminder.data.Task;
import com.taskreminder.receiver.AlarmReceiver;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements TaskAdapter.TaskListener {

    private TaskAdapter adapter;
    private TextView emptyView;
    private TextView alarmStatusText;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setSupportActionBar(findViewById(R.id.toolbar));

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.emptyView);
        alarmStatusText = findViewById(R.id.alarmStatusText);

        adapter = new TaskAdapter(new ArrayList<>(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> EditTaskActivity.startForCreate(this));

        // Test button: fire the alarm logic right now
        Button testBtn = findViewById(R.id.testButton);
        testBtn.setOnClickListener(v -> runAlarmNow());

        createNotificationChannel();
        checkPermissions();
        updateAlarmStatus();
    }

    /** Fires the exact same code the alarm receiver runs — useful for testing. */
    private void runAlarmNow() {
        executor.execute(() -> {
            List<Task> openTasks = AppDatabase.getInstance(this).taskDao().getOpenTasks();
            runOnUiThread(() -> {
                if (openTasks.isEmpty()) {
                    Toast.makeText(this,
                        "No open tasks — add a task first, then test again.",
                        Toast.LENGTH_LONG).show();
                    return;
                }

                // 1. Show notification
                showTestNotification(openTasks.size());

                // 2. Set popup_pending flag (same as AlarmReceiver does)
                SharedPreferences prefs = getSharedPreferences(
                        AlarmReceiver.PREFS_NAME, MODE_PRIVATE);
                prefs.edit().putBoolean(AlarmReceiver.KEY_POPUP_PENDING, true).apply();

                Toast.makeText(this,
                    "✅ Test fired!\n• Notification sent\n• Lock screen, then unlock to see popup",
                    Toast.LENGTH_LONG).show();
            });
        });
    }

    private void showTestNotification(int count) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        Intent popupIntent = new Intent(this, TaskPopupActivity.class);
        popupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(
                this, 0, popupIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = count == 1 ? "You have 1 open task"
                                  : "You have " + count + " open tasks";

        androidx.core.app.NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, AlarmReceiver.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText("Tap to review your task list")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pi);

        nm.notify(99, builder.build());
    }

    private void createNotificationChannel() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(
                AlarmReceiver.CHANNEL_ID,
                "Daily Task Reminder",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Reminds you of your open tasks every day");
        channel.enableVibration(true);
        channel.setShowBadge(true);
        nm.createNotificationChannel(channel);
    }

    /** Warn the user if any critical permission is missing. */
    private void checkPermissions() {
        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        // Exact alarm permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (!am.canScheduleExactAlarms()) {
                new MaterialAlertDialogBuilder(this)
                    .setTitle("Allow exact alarms")
                    .setMessage("Task Reminder needs permission to fire at an exact time. " +
                                "Please enable 'Alarms & reminders' for this app.")
                    .setPositiveButton("Open settings", (d, w) -> {
                        Intent i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        startActivity(i);
                    })
                    .setNegativeButton("Later", null)
                    .show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTasks();
        updateAlarmStatus();
    }

    private void loadTasks() {
        executor.execute(() -> {
            List<Task> tasks = AppDatabase.getInstance(this).taskDao().getAllTasks();
            runOnUiThread(() -> {
                adapter.setTasks(tasks);
                emptyView.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    private void updateAlarmStatus() {
        SharedPreferences prefs = getSharedPreferences(AlarmScheduler.PREFS_NAME, MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(AlarmScheduler.KEY_ALARM_ENABLED, false);
        if (enabled) {
            int hour = prefs.getInt(AlarmScheduler.KEY_ALARM_HOUR, 8);
            int minute = prefs.getInt(AlarmScheduler.KEY_ALARM_MINUTE, 0);
            alarmStatusText.setText(String.format(Locale.getDefault(),
                    "⏰  Daily reminder at %02d:%02d", hour, minute));
        } else {
            alarmStatusText.setText("⏰  Daily reminder: off  —  tap the clock icon to enable");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_set_alarm) {
            showTimePicker();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showTimePicker() {
        SharedPreferences prefs = getSharedPreferences(AlarmScheduler.PREFS_NAME, MODE_PRIVATE);
        int savedHour = prefs.getInt(AlarmScheduler.KEY_ALARM_HOUR, 8);
        int savedMinute = prefs.getInt(AlarmScheduler.KEY_ALARM_MINUTE, 0);

        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(savedHour)
                .setMinute(savedMinute)
                .setTitleText("Set daily reminder time")
                .build();

        picker.addOnPositiveButtonClickListener(v -> {
            AlarmScheduler.schedule(this, picker.getHour(), picker.getMinute());
            updateAlarmStatus();
        });

        picker.show(getSupportFragmentManager(), "time_picker");
    }

    @Override
    public void onTaskChecked(Task task, boolean done) {
        executor.execute(() -> {
            task.done = done;
            AppDatabase.getInstance(this).taskDao().update(task);
            runOnUiThread(this::loadTasks);
        });
    }

    @Override
    public void onTaskEdit(Task task) {
        EditTaskActivity.startForEdit(this, task.id);
    }

    @Override
    public void onTaskDelete(Task task) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete task?")
                .setMessage("\"" + task.title + "\" will be permanently removed.")
                .setPositiveButton("Delete", (d, w) -> executor.execute(() -> {
                    AppDatabase.getInstance(this).taskDao().delete(task);
                    runOnUiThread(this::loadTasks);
                }))
                .setNegativeButton("Cancel", null)
                .show();
    }
}
