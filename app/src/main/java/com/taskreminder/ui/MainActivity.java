package com.taskreminder.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

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

        updateAlarmStatus();
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

    // ── TaskAdapter.TaskListener callbacks ────────────────────────────────────

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
