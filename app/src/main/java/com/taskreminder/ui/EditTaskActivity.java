package com.taskreminder.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.taskreminder.R;
import com.taskreminder.data.AppDatabase;
import com.taskreminder.data.Task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditTaskActivity extends AppCompatActivity {

    private static final String EXTRA_TASK_ID = "task_id";
    private static final int NO_TASK = -1;

    private TextInputEditText titleInput;
    private TextInputEditText notesInput;

    private int taskId = NO_TASK;
    private Task existingTask = null;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void startForCreate(Context context) {
        context.startActivity(new Intent(context, EditTaskActivity.class));
    }

    public static void startForEdit(Context context, int taskId) {
        Intent intent = new Intent(context, EditTaskActivity.class);
        intent.putExtra(EXTRA_TASK_ID, taskId);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_task);

        setSupportActionBar(findViewById(R.id.editToolbar));

        titleInput = findViewById(R.id.titleInput);
        notesInput = findViewById(R.id.notesInput);
        Button saveButton = findViewById(R.id.saveButton);

        taskId = getIntent().getIntExtra(EXTRA_TASK_ID, NO_TASK);

        if (taskId != NO_TASK) {
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Edit Task");
            executor.execute(() -> {
                existingTask = AppDatabase.getInstance(this).taskDao().getById(taskId);
                if (existingTask != null) {
                    runOnUiThread(() -> {
                        titleInput.setText(existingTask.title);
                        notesInput.setText(existingTask.notes);
                    });
                }
            });
        } else {
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("New Task");
        }

        saveButton.setOnClickListener(v -> save());
    }

    private void save() {
        String title = titleInput.getText() != null ? titleInput.getText().toString().trim() : "";
        String notes = notesInput.getText() != null ? notesInput.getText().toString().trim() : "";

        if (TextUtils.isEmpty(title)) {
            titleInput.setError("Title is required");
            return;
        }

        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            if (taskId == NO_TASK) {
                // Create
                Task task = new Task(title, notes);
                db.taskDao().insert(task);
            } else {
                // Update
                if (existingTask != null) {
                    existingTask.title = title;
                    existingTask.notes = notes;
                    db.taskDao().update(existingTask);
                }
            }
            runOnUiThread(this::finish);
        });
    }
}
