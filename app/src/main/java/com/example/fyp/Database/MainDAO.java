package com.example.fyp.Database;

import static androidx.room.OnConflictStrategy.REPLACE;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.fyp.Models.Ingredients;

import java.util.List;

@Dao
public interface MainDAO {
    @Insert(onConflict = REPLACE)
    void insert(Ingredients ingredients);

    @Query("SELECT * FROM ingredients")
    List<Ingredients> getAll();

    @Query("UPDATE ingredients SET name = :name WHERE ID = :id")
    void update(String name, int id);

    @Delete
    void delete(Ingredients ingredients);
}
