package com.example.fyp.Database;

import static androidx.room.OnConflictStrategy.REPLACE;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.fyp.Models.Ingredients;
import com.example.fyp.Models.Inventory;
import com.example.fyp.Models.ShoppingList;

import java.util.List;

@Dao
public interface MainDAO {
    //Ingredients
    @Insert(onConflict = REPLACE)
    void insertIngredient(Ingredients ingredients);

    @Query("SELECT * FROM ingredients")
    List<Ingredients> getAllIngredients();

    @Query("UPDATE ingredients SET name = :name WHERE ID = :id")
    void updateIngredient(String name, int id);

    @Delete
    void deleteIngredient(Ingredients ingredients);

    //Shopping list
    @Insert(onConflict = REPLACE)
    void insertShoppingList(ShoppingList shoppingList);

    @Query("SELECT * FROM shopping_list")
    List<ShoppingList> getAllShoppingListItems();

    @Query("UPDATE shopping_list SET amount = amount + :amount WHERE :id = id")
    void updateShoppingListAmount(int amount, String id);

    @Delete
    void deleteShoppingListItem(ShoppingList shoppingList);

    @Query("SELECT ID FROM shopping_list WHERE :id = ID")
    List<String> checkIfItemExists(String id);

    //Inventory
    @Insert(onConflict = REPLACE)
    void insertInventory(Inventory inventory);

    @Query("SELECT * FROM inventory")
    List<Inventory> getAllInventoryItems();

    @Delete
    void deleteInventoryItem(Inventory inventory);

    @Query("SELECT ID FROM inventory WHERE :id = ID")
    List<Integer> checkIfInventoryItemExists(String id);
}

