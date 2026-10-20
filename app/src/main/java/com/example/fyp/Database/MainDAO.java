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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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

    @Query("SELECT * FROM shopping_list WHERE ID = :id LIMIT 1")
    ShoppingList getShoppingListItem(String id);

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

    /**
     * Inventory entries for one ingredient, looked up by its normalised
     * name (ingredient columns store plain names since schema 6).
     */
    @Transaction
    @Query("SELECT * FROM inventory WHERE ingredient = :ingredientName")
    List<InventoryWithExpiryDates> getOneInventoryItem(String ingredientName);

    @Query("UPDATE inventory SET amount = amount + :amount WHERE :ingredientName = ingredient")
    void updateInventoryListAmount(int amount, String ingredientName);

    @Delete
    void deleteInventoryItem(Inventory inventory);

    @Query("SELECT ingredient FROM inventory WHERE :ingredientName = ingredient")
    List<Ingredients> checkIfInventoryItemExists(String ingredientName);

    /** Deletes an inventory row and all of its expiry records together. */
    @Query("DELETE FROM expiry_date WHERE inventory_ingredient = :ingredientName")
    void deleteExpiryDatesForIngredient(String ingredientName);

    //Expiry Date
    @Insert(onConflict = REPLACE)
    void insertExpiryDate(ExpiryDate expiryDate);

    @Query("SELECT * FROM expiry_date")
    List<ExpiryDate> getAllExpiryDates();

    /**
     * All expiry records of one ingredient (normalised name).
     */
    @Query("SELECT * FROM expiry_date WHERE inventory_ingredient = :ingredientName")
    List<ExpiryDate> getExpiryDatesForIngredient(String ingredientName);

    /**
     * The soonest-expiring unit of one ingredient, or null when the
     * ingredient has no expiry records with parsable dates. Scoped by
     * ingredient so removing one unit can never delete another
     * ingredient's same-dated record. Dates are stored as d-M-yyyy text,
     * so the earliest is selected in Java rather than by SQL ordering.
     */
    @Transaction
    default ExpiryDate getEarliestExpiryDate(String ingredientName) {
        ExpiryDate earliest = null;
        LocalDate earliestDate = null;
        for (ExpiryDate candidate : getExpiryDatesForIngredient(ingredientName)) {
            LocalDate parsed = parseExpiry(candidate.getExpiryDate());
            if (parsed != null && (earliestDate == null || parsed.isBefore(earliestDate))) {
                earliestDate = parsed;
                earliest = candidate;
            }
        }
        return earliest;
    }

    static LocalDate parseExpiry(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return LocalDate.parse(raw, DateTimeFormatter.ofPattern("d-M-yyyy"));
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    @Delete
    void deleteExpiryDate(ExpiryDate expiryDate);

    @Query("DELETE FROM expiry_date WHERE ID = :id")
    void deleteExpiryDateById(int id);

    /**
     * Removes one owned unit of an ingredient: decrements the amount and
     * deletes exactly one expiry record (the soonest), atomically. When no
     * expiry record exists, only the amount changes.
     */
    @Transaction
    default void removeOneInventoryUnit(String ingredientName) {
        ExpiryDate earliest = getEarliestExpiryDate(ingredientName);
        if (earliest != null) {
            deleteExpiryDateById(earliest.getID());
        }
        updateInventoryListAmount(-1, ingredientName);
    }

    /**
     * Consumes count units of an ingredient for a completed recipe:
     * deletes up to count of its soonest expiry records and decrements the
     * amount, atomically. Never leaves expiry rows behind for consumed
     * units and never touches records of other ingredients.
     */
    @Transaction
    default void consumeInventoryUnits(String ingredientName, int count) {
        for (int i = 0; i < count; i++) {
            ExpiryDate earliest = getEarliestExpiryDate(ingredientName);
            if (earliest == null) {
                break;
            }
            deleteExpiryDateById(earliest.getID());
        }
        updateInventoryListAmount(-count, ingredientName);
    }

    //Recipe
    @Insert(onConflict = REPLACE)
    long insertRecipe(Recipe recipe);

    @Query("SELECT * FROM RECIPE")
    List<Recipe> getAllRecipes();

    @Transaction
    default void deleteRecipeWithIngredients(Recipe recipe) {
        deleteRecipeIngredientsOfRecipe(recipe.getID());
        deleteRecipe(recipe);
    }

    @Delete
    void deleteRecipe(Recipe recipe);

    @Query("DELETE FROM RecipeIngredients WHERE recipe_id = :recipeId")
    void deleteRecipeIngredientsOfRecipe(int recipeId);

    @Query("SELECT ID FROM recipe WHERE meal_id = :mealId LIMIT 1")
    Integer findRecipeIdByMealId(String mealId);

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
