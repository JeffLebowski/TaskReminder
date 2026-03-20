package com.taskreminder.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TaskDao {

    @Insert
    void insert(Task task);

    @Update
    void update(Task task);

    @Delete
    void delete(Task task);

    @Query("SELECT * FROM tasks ORDER BY done ASC, created_at ASC")
    List<Task> getAllTasks();

    @Query("SELECT * FROM tasks WHERE done = 0 ORDER BY created_at ASC")
    List<Task> getOpenTasks();

    @Query("SELECT * FROM tasks WHERE id = :id")
    Task getById(int id);
}
