package com.taskreminder.ui;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
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

        createNotificationChannel();
        checkPermissions();
        updateAlarmStatus();
    }

    private void createNotificationChannel() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(
                AlarmReceiver.CHANNEL_ID,
                "Daily Task Reminder",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Erinnert dich täglich an offene Tasks");
        channel.enableVibration(true);
        channel.setShowBadge(true);
        nm.createNotificationChannel(channel);
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (!am.canScheduleExactAlarms()) {
                new MaterialAlertDialogBuilder(this)
                    .setTitle("⚠️ Berechtigung für genaue Alarme")
                    .setMessage("Ohne diese Berechtigung kann die tägliche Erinnerung verzögert " +
                                "oder gar nicht ausgelöst werden.\n\n" +
                                "Bitte 'Einstellungen öffnen' tippen und 'Alarme & Erinnerungen' " +
                                "für Task Reminder aktivieren.")
                    .setPositiveButton("Einstellungen öffnen", (d, w) -> {
                        Intent i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        startActivity(i);
                    })
                    .setNegativeButton("Später", null)
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
        alarmStatusText.setText("⏰  " + AlarmScheduler.getStatus(this));
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
                .setTitleText("Tägliche Erinnerungszeit setzen")
                .build();

        picker.addOnPositiveButtonClickListener(v -> {
            String result = AlarmScheduler.schedule(this, picker.getHour(), picker.getMinute());
            updateAlarmStatus();
            Toast.makeText(this, result, Toast.LENGTH_LONG).show();
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
                .setTitle("Task löschen?")
                .setMessage("\"" + task.title + "\" wird dauerhaft entfernt.")
                .setPositiveButton("Löschen", (d, w) -> executor.execute(() -> {
                    AppDatabase.getInstance(this).taskDao().delete(task);
                    runOnUiThread(this::loadTasks);
                }))
                .setNegativeButton("Abbrechen", null)
                .show();
    }
}
