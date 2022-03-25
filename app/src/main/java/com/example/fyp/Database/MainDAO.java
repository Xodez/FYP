package com.example.fyp.Database;

import static androidx.room.OnConflictStrategy.REPLACE;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.fyp.Models.ExpiryDate;
import com.example.fyp.Models.Ingredients;
import com.example.fyp.Models.Inventory;
import com.example.fyp.Models.InventoryWithExpiryDates;
import com.example.fyp.Models.Recipe;
import com.example.fyp.Models.RecipeIngredients;
import com.example.fyp.Models.RecipeWithIngredients;
import com.example.fyp.Models.ShoppingList;

import java.util.List;

@Dao
public interface MainDAO {
    //Ingredients
    @Insert(onConflict = REPLACE)
    void insertIngredient(Ingredients ingredients);

    @Query("SELECT * FROM ingredients")
    List<Ingredients> getAllIngredients();

    @Query("SELECT name FROM ingredients")
    List<String> getAllIngredientNames();

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

    @Transaction
    @Query("SELECT * FROM inventory")
    List<InventoryWithExpiryDates> getInventoriesWithExpiryDates();

    @Query("UPDATE inventory SET amount = amount + :amount WHERE :ingredient = ingredient")
    void updateInventoryListAmount(int amount, Ingredients ingredient);

    @Delete
    void deleteInventoryItem(Inventory inventory);

    @Query("SELECT ingredient FROM inventory WHERE :ingredient = ingredient")
    List<Ingredients> checkIfInventoryItemExists(Ingredients ingredient);

    //Expiry Date
    @Insert(onConflict = REPLACE)
    void insertExpiryDate(ExpiryDate expiryDate);

    @Query("SELECT * FROM expiry_date")
    List<ExpiryDate> getAllExpiryDates();

    @Query("SELECT * FROM expiry_date WHERE :expiryDate = expiry_date LIMIT 1")
    List<ExpiryDate> getOneExpiryDate(String expiryDate);

    @Delete
    void deleteExpiryDate(ExpiryDate expiryDate);

    //Recipe
    @Insert(onConflict = REPLACE)
    void insertRecipe(Recipe recipe);

    @Query("SELECT * FROM RECIPE")
    List<Recipe> getAllRecipes();

    @Delete
    void deleteRecipe(Recipe recipe);

    @Transaction
    @Query("SELECT * FROM recipe")
    List<RecipeWithIngredients> getRecipeWithIngredients();

    @Transaction
    @Query("SELECT * FROM recipe WHERE name = :name")
    List<RecipeWithIngredients> getRecipeWithIngredientsByName(String name);

    //Recipe Ingredients
    @Insert(onConflict = REPLACE)
    void insertRecipeIngredient(RecipeIngredients recipeIngredients);
}

