package com.example.fyp.Database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;

import com.example.fyp.Converters;
import com.example.fyp.Models.Ingredients;
import com.example.fyp.Models.ShoppingList;

@Database(entities = {ShoppingList.class, Ingredients.class}, version = 2, exportSchema = false)
@TypeConverters(Converters.class)
public abstract class RoomDB extends RoomDatabase {
    private static RoomDB database;
    private static String DATABASE_NAME = "RecipeApp";

    public synchronized static RoomDB getInstance(Context context){
        if (database == null){
            database = Room.databaseBuilder(context.getApplicationContext(), RoomDB.class, DATABASE_NAME).allowMainThreadQueries().fallbackToDestructiveMigration().build();
        }
        return database;
    }

    public abstract MainDAO mainDao();
}
