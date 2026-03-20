package com.taskreminder.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks")
public class Task {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "notes")
    public String notes;

    @ColumnInfo(name = "done")
    public boolean done;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    public Task(String title, String notes) {
        this.title = title;
        this.notes = notes;
        this.done = false;
        this.createdAt = System.currentTimeMillis();
    }
}
