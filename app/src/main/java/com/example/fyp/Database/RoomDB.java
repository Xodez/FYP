package com.example.fyp.Database;

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.room.Database;

import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.fyp.Converters;
import com.example.fyp.IngredientNames;
import com.example.fyp.Models.ExpiryDate;
import com.example.fyp.Models.Ingredients;
import com.example.fyp.Models.Inventory;
import com.example.fyp.Models.InventoryWithExpiryDates;
import com.example.fyp.Models.Recipe;
import com.example.fyp.Models.RecipeIngredients;
import com.example.fyp.Models.ShoppingList;

@Database(entities = {ShoppingList.class, Ingredients.class, Inventory.class, ExpiryDate.class, Recipe.class, RecipeIngredients.class}, version = 6, exportSchema = false)
@TypeConverters(Converters.class)
public abstract class RoomDB extends RoomDatabase {
    private static RoomDB database;
    private static String DATABASE_NAME = "RecipeApp";

    /**
     * Migrates schema 5 to 6 without losing user data:
     *  - adds the recipe.meal_id column used to deduplicate API imports;
     *  - rewrites ingredient columns that previously stored Gson dumps of
     *    the Ingredients object (whose ID could drift between tables) to
     *    the plain normalised ingredient name, keeping inventory, expiry,
     *    shopping-list, and recipe-ingredient rows joinable.
     *
     * If a rewritten value would collide with an existing unique index
     * entry, the row is left unchanged rather than deleted; in practice
     * legacy writes already upper-cased names, so collisions are an edge
     * case and must never destroy inventory quantities.
     */
    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE recipe ADD COLUMN meal_id TEXT");
            rewriteIngredientColumn(database, "inventory", "ingredient", "ID");
            rewriteIngredientColumn(database, "expiry_date",
                    "inventory_ingredient", "ID");
            rewriteIngredientColumn(database, "shopping_list", "ingredient", "ID");
            rewriteIngredientColumn(database, "RecipeIngredients",
                    "ingredient", "ID");
            normalizeIngredientTableNames(database);
        }
    };

    private static void rewriteIngredientColumn(SupportSQLiteDatabase database,
                                                String table, String column,
                                                String idColumn) {
        Cursor cursor = database.query(
                "SELECT " + idColumn + ", " + column + " FROM " + table);
        while (cursor.moveToNext()) {
            String id = cursor.getString(0);
            String name = Converters.nameFromStored(cursor.getString(1));
            if (name == null) {
                continue;
            }
            try {
                database.execSQL(
                        "UPDATE " + table + " SET " + column + " = ? WHERE "
                                + idColumn + " = ?",
                        new Object[]{name, id});
            } catch (android.database.SQLException ignored) {
                // Unique-index collision: keep the original row value.
            }
        }
        cursor.close();
    }

    private static void normalizeIngredientTableNames(
            SupportSQLiteDatabase database) {
        Cursor cursor = database.query("SELECT ID, name FROM ingredients");
        while (cursor.moveToNext()) {
            int id = cursor.getInt(0);
            String name = IngredientNames.normalize(cursor.getString(1));
            try {
                database.execSQL(
                        "UPDATE ingredients SET name = ? WHERE ID = ?",
                        new Object[]{name, id});
            } catch (android.database.SQLException ignored) {
                // Collision with an existing normalised name: leave as is.
            }
        }
        cursor.close();
    }

    public synchronized static RoomDB getInstance(Context context){
        if (database == null){
            database = Room.databaseBuilder(context.getApplicationContext(), RoomDB.class, DATABASE_NAME)
                    .allowMainThreadQueries()
                    .addMigrations(MIGRATION_5_6)
                    .build();
        }
        return database;
    }

    public abstract MainDAO mainDao();
}
