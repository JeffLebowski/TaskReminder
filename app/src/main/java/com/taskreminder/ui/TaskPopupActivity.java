package com.taskreminder.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.taskreminder.R;
import com.taskreminder.data.AppDatabase;
import com.taskreminder.data.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskPopupActivity extends AppCompatActivity implements TaskAdapter.TaskListener {

    private TaskAdapter adapter;
    private TextView headerText;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_popup);

        headerText = findViewById(R.id.headerText);
        RecyclerView recyclerView = findViewById(R.id.popupRecyclerView);
        adapter = new TaskAdapter(new ArrayList<>(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        findViewById(R.id.dismissButton).setOnClickListener(v -> finish());

        loadOpenTasks();
    }

    private void loadOpenTasks() {
        executor.execute(() -> {
            List<Task> tasks = AppDatabase.getInstance(this).taskDao().getOpenTasks();
            runOnUiThread(() -> {
                adapter.setTasks(tasks);
                int count = tasks.size();
                if (count == 0) {
                    headerText.setText("🎉  All done! No open tasks.");
                } else if (count == 1) {
                    headerText.setText("You have 1 open task");
                } else {
                    headerText.setText("You have " + count + " open tasks");
                }
            });
        });
    }

    // ── TaskAdapter.TaskListener callbacks ────────────────────────────────────

    @Override
    public void onTaskChecked(Task task, boolean done) {
        executor.execute(() -> {
            task.done = done;
            AppDatabase.getInstance(this).taskDao().update(task);
            runOnUiThread(this::loadOpenTasks);
        });
    }

    @Override
    public void onTaskEdit(Task task) {
        // Open full editor and come back
        EditTaskActivity.startForEdit(this, task.id);
    }

    @Override
    public void onTaskDelete(Task task) {
        // No confirmation dialog in the popup — just delete
        executor.execute(() -> {
            AppDatabase.getInstance(this).taskDao().delete(task);
            runOnUiThread(this::loadOpenTasks);
        });
    }
}
