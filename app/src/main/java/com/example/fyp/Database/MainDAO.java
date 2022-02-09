package com.example.fyp.Database;

import static androidx.room.OnConflictStrategy.REPLACE;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.fyp.Models.Ingredient;

import java.util.ArrayList;
import java.util.List;

@Dao
public interface MainDAO {
    @Insert(onConflict = REPLACE)
    void insert(Ingredient ingredient);

    @Query("SELECT * FROM ingredients")
    ArrayList<Ingredient> getAll();

    @Query("UPDATE ingredients SET name = :name, WHERE ID = :id")
    void update(int id, String name);

    @Delete
    void delete(Ingredient ingredient);
}
