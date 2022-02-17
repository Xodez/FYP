package com.example.fyp.Database;

import static androidx.room.OnConflictStrategy.REPLACE;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.fyp.Models.Ingredients;
import com.example.fyp.Models.ShoppingList;

import java.util.List;

@Dao
public interface MainDAO {
    @Insert(onConflict = REPLACE)
    void insertIngredient(Ingredients ingredients);

    @Insert(onConflict = REPLACE)
    void insertShoppingList(ShoppingList shoppingList);

    @Query("SELECT * FROM ingredients")
    List<Ingredients> getAllIngredients();

    @Query("SELECT * FROM shopping_list")
    List<ShoppingList> getAllShoppingListItems();

    @Query("UPDATE ingredients SET name = :name WHERE ID = :id")
    void updateIngredient(String name, int id);

    @Delete
    void deleteIngredient(Ingredients ingredients);
}
