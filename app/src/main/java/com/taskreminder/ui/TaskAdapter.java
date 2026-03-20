package com.taskreminder.ui;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.taskreminder.R;
import com.taskreminder.data.Task;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    public interface TaskListener {
        void onTaskChecked(Task task, boolean done);
        void onTaskEdit(Task task);
        void onTaskDelete(Task task);
    }

    private List<Task> tasks;
    private final TaskListener listener;

    public TaskAdapter(List<Task> tasks, TaskListener listener) {
        this.tasks = tasks;
        this.listener = listener;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);

        holder.checkBox.setOnCheckedChangeListener(null); // avoid spurious callbacks
        holder.checkBox.setChecked(task.done);
        holder.titleText.setText(task.title);

        // Strike-through when done
        if (task.done) {
            holder.titleText.setPaintFlags(
                    holder.titleText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.titleText.setAlpha(0.45f);
        } else {
            holder.titleText.setPaintFlags(
                    holder.titleText.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            holder.titleText.setAlpha(1f);
        }

        // Notes
        if (task.notes != null && !task.notes.isEmpty()) {
            holder.notesText.setVisibility(View.VISIBLE);
            holder.notesText.setText(task.notes);
        } else {
            holder.notesText.setVisibility(View.GONE);
        }

        holder.checkBox.setOnCheckedChangeListener((btn, checked) ->
                listener.onTaskChecked(task, checked));

        holder.editButton.setOnClickListener(v -> listener.onTaskEdit(task));
        holder.deleteButton.setOnClickListener(v -> listener.onTaskDelete(task));
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        TextView titleText;
        TextView notesText;
        ImageButton editButton;
        ImageButton deleteButton;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkBox);
            titleText = itemView.findViewById(R.id.titleText);
            notesText = itemView.findViewById(R.id.notesText);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}
