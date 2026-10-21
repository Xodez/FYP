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

import java.util.ArrayList;
import java.util.List;

import com.example.fyp.Converters;
import com.example.fyp.IngredientNames;
import com.example.fyp.Models.ExpiryDate;
import com.example.fyp.Models.Ingredients;
import com.example.fyp.Models.Inventory;
import com.example.fyp.Models.InventoryWithExpiryDates;
import com.example.fyp.Models.Recipe;
import com.example.fyp.Models.RecipeIngredients;
import com.example.fyp.Models.ShoppingList;

@Database(entities = {ShoppingList.class, Ingredients.class, Inventory.class, ExpiryDate.class, Recipe.class, RecipeIngredients.class}, version = 7, exportSchema = false)
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

    /**
     * Migrates schema 6 to 7: adds a unique index on recipe.meal_id so
     * racing or repeated imports can never create two recipes for the
     * same external meal. Custom recipes keep a NULL meal_id, which a
     * SQLite unique index treats as distinct, so local recipes remain
     * unlimited.
     *
     * Pre-existing duplicates are reconciled explicitly rather than
     * blindly dropped: for each duplicated meal_id the earliest import
     * (lowest row id) survives. If that survivor has no ingredient rows
     * (a partial historical import) the duplicate's rows are re-attached
     * to it before the duplicate recipe is removed; otherwise the
     * duplicate's rows are deleted so no orphaned relations survive.
     * Recipes with a NULL meal_id and every other table are untouched.
     */
    static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            List<String> mealIds = new ArrayList<>();
            List<Integer> survivorIds = new ArrayList<>();
            Cursor grouped = database.query(
                    "SELECT meal_id, MIN(ID) FROM recipe "
                            + "WHERE meal_id IS NOT NULL "
                            + "GROUP BY meal_id HAVING COUNT(*) > 1");
            while (grouped.moveToNext()) {
                mealIds.add(grouped.getString(0));
                survivorIds.add(grouped.getInt(1));
            }
            grouped.close();

            for (int g = 0; g < mealIds.size(); g++) {
                String mealId = mealIds.get(g);
                int survivor = survivorIds.get(g);
                List<Integer> duplicateIds = new ArrayList<>();
                Cursor duplicates = database.query(
                        "SELECT ID FROM recipe WHERE meal_id = ? AND ID <> ?",
                        new Object[]{mealId, survivor});
                while (duplicates.moveToNext()) {
                    duplicateIds.add(duplicates.getInt(0));
                }
                duplicates.close();

                for (int duplicateId : duplicateIds) {
                    if (joinRowCount(database, survivor) == 0
                            && joinRowCount(database, duplicateId) > 0) {
                        database.execSQL(
                                "UPDATE RecipeIngredients SET recipe_id = ? "
                                        + "WHERE recipe_id = ?",
                                new Object[]{survivor, duplicateId});
                    } else {
                        database.execSQL(
                                "DELETE FROM RecipeIngredients "
                                        + "WHERE recipe_id = ?",
                                new Object[]{duplicateId});
                    }
                    database.execSQL("DELETE FROM recipe WHERE ID = ?",
                            new Object[]{duplicateId});
                }
            }

            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS "
                    + "index_recipe_meal_id ON recipe (meal_id)");
        }
    };

    private static int joinRowCount(SupportSQLiteDatabase database,
                                    int recipeId) {
        Cursor cursor = database.query(
                "SELECT COUNT(*) FROM RecipeIngredients WHERE recipe_id = ?",
                new Object[]{recipeId});
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

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
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
                    .build();
        }
        return database;
    }

    public abstract MainDAO mainDao();
}
