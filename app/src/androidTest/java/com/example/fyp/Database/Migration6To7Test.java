package com.example.fyp.Database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;

import androidx.room.Room;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.fyp.Models.Recipe;
import com.example.fyp.Models.RecipeIngredients;
import com.example.fyp.Models.RecipeWithIngredients;
import com.example.fyp.Models.Ingredients;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;

/**
 * Verifies the schema 6-to-7 migration: the unique recipe.meal_id index
 * is created, pre-existing duplicate imports are reconciled with the
 * documented policy (earliest row survives; a partial survivor inherits
 * the duplicate's ingredient rows; otherwise duplicates' rows are
 * deleted), custom recipes with NULL meal ids are untouched, and Room
 * accepts the migrated schema and enforces the constraint afterwards.
 */
@RunWith(AndroidJUnit4.class)
public class Migration6To7Test {

    private static final String DB_NAME = "migration-6-7-test";

    private Context context;

    /** Schema 6: meal_id exists (added in 5-6) but is NOT uniquely indexed. */
    private final SupportSQLiteOpenHelper.Callback schema6 =
            new SupportSQLiteOpenHelper.Callback(6) {
                @Override
                public void onCreate(SupportSQLiteDatabase db) {
                    db.execSQL("CREATE TABLE ingredients (ID INTEGER "
                            + "PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT)");
                    db.execSQL("CREATE UNIQUE INDEX index_ingredients_name "
                            + "ON ingredients (name)");
                    db.execSQL("CREATE TABLE inventory (ID INTEGER PRIMARY "
                            + "KEY AUTOINCREMENT NOT NULL, ingredient TEXT, "
                            + "amount INTEGER NOT NULL)");
                    db.execSQL("CREATE UNIQUE INDEX index_inventory_ingredient "
                            + "ON inventory (ingredient)");
                    db.execSQL("CREATE TABLE expiry_date (ID INTEGER PRIMARY "
                            + "KEY AUTOINCREMENT NOT NULL, "
                            + "inventory_ingredient TEXT, expiry_date TEXT)");
                    db.execSQL("CREATE TABLE shopping_list (ID TEXT PRIMARY "
                            + "KEY NOT NULL, ingredient TEXT, amount INTEGER "
                            + "NOT NULL)");
                    db.execSQL("CREATE UNIQUE INDEX index_shopping_list_ingredient "
                            + "ON shopping_list (ingredient)");
                    db.execSQL("CREATE TABLE recipe (ID INTEGER PRIMARY KEY "
                            + "AUTOINCREMENT NOT NULL, name TEXT, "
                            + "instructions TEXT, image TEXT, meal_id TEXT)");
                    db.execSQL("CREATE TABLE RecipeIngredients (ID INTEGER "
                            + "PRIMARY KEY AUTOINCREMENT NOT NULL, recipe_id "
                            + "INTEGER NOT NULL, ingredient TEXT, count "
                            + "INTEGER NOT NULL)");

                    // A custom recipe (NULL meal id) that must survive.
                    db.execSQL("INSERT INTO recipe (ID, name, instructions, "
                            + "image, meal_id) VALUES (1, 'Family Stew', "
                            + "'simmer@', '', NULL)");
                    db.execSQL("INSERT INTO RecipeIngredients (recipe_id, "
                            + "ingredient, count) VALUES (1, 'CARROT', 2)");

                    // Duplicate imports of meal 52772: survivor (ID 2) has
                    // its own rows; the duplicate (ID 5) has one extra row.
                    db.execSQL("INSERT INTO recipe (ID, name, instructions, "
                            + "image, meal_id) VALUES (2, 'Soup', 'boil@', "
                            + "'a', '52772')");
                    db.execSQL("INSERT INTO RecipeIngredients (recipe_id, "
                            + "ingredient, count) VALUES (2, 'TOMATO', 1)");
                    db.execSQL("INSERT INTO RecipeIngredients (recipe_id, "
                            + "ingredient, count) VALUES (2, 'SALT', 1)");
                    db.execSQL("INSERT INTO recipe (ID, name, instructions, "
                            + "image, meal_id) VALUES (5, 'Soup', 'boil@', "
                            + "'a', '52772')");
                    db.execSQL("INSERT INTO RecipeIngredients (recipe_id, "
                            + "ingredient, count) VALUES (5, 'PEPPER', 1)");

                    // Duplicate imports of meal 52773: the older survivor
                    // (ID 3) is a PARTIAL import without rows; the
                    // duplicate (ID 6) holds the only ingredient rows,
                    // which must be re-attached, not deleted.
                    db.execSQL("INSERT INTO recipe (ID, name, instructions, "
                            + "image, meal_id) VALUES (3, 'Pie', 'bake@', "
                            + "'b', '52773')");
                    db.execSQL("INSERT INTO recipe (ID, name, instructions, "
                            + "image, meal_id) VALUES (6, 'Pie', 'bake@', "
                            + "'b', '52773')");
                    db.execSQL("INSERT INTO RecipeIngredients (recipe_id, "
                            + "ingredient, count) VALUES (6, 'APPLE', 3)");
                }

                @Override
                public void onUpgrade(SupportSQLiteDatabase db, int oldVersion,
                                      int newVersion) {
                    // Created at version 6; the migration under test is
                    // applied explicitly or by Room.
                }
            };

    private SupportSQLiteDatabase openSchema6() {
        SupportSQLiteOpenHelper.Configuration configuration =
                SupportSQLiteOpenHelper.Configuration.builder(context)
                        .name(DB_NAME)
                        .callback(schema6)
                        .build();
        return new FrameworkSQLiteOpenHelperFactory()
                .create(configuration).getWritableDatabase();
    }

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase(DB_NAME);
    }

    @After
    public void tearDown() {
        context.deleteDatabase(DB_NAME);
    }

    private int countRows(SupportSQLiteDatabase db, String sql) {
        Cursor cursor = db.query(sql, null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    @Test
    public void migrationReconcilesDuplicatesAndCreatesUniqueIndex()
            throws IOException {
        SupportSQLiteDatabase db = openSchema6();
        RoomDB.MIGRATION_6_7.migrate(db);

        // 5 seeded recipes minus 2 duplicates = 3 (IDs 1, 2, 3).
        assertEquals("duplicates removed", 3,
                countRows(db, "SELECT COUNT(*) FROM recipe"));
        assertEquals("custom recipe untouched", 1, countRows(db,
                "SELECT COUNT(*) FROM recipe WHERE meal_id IS NULL "
                        + "AND name = 'Family Stew'"));
        assertEquals("earliest import per meal survives", 2, countRows(db,
                "SELECT COUNT(*) FROM recipe WHERE ID IN (2, 3)"));

        // Soup (survivor with its own rows): duplicate's row deleted.
        assertEquals("full survivor keeps its rows only", 2, countRows(db,
                "SELECT COUNT(*) FROM RecipeIngredients WHERE recipe_id = 2"));
        assertEquals("partial survivor inherits duplicate's rows", 1,
                countRows(db,
                        "SELECT COUNT(*) FROM RecipeIngredients "
                                + "WHERE recipe_id = 3"));
        // No orphaned relations anywhere.
        assertEquals("no orphan join rows", 0, countRows(db,
                "SELECT COUNT(*) FROM RecipeIngredients WHERE recipe_id "
                        + "NOT IN (SELECT ID FROM recipe)"));

        Cursor index = db.query("SELECT name FROM sqlite_master WHERE type "
                + "= 'index' AND name = 'index_recipe_meal_id'", null);
        assertTrue("unique index created", index.moveToFirst());
        index.close();

        // Constraint now enforced at the database level; NULL ids stay free.
        assertThrows(SQLiteConstraintException.class, () -> db.execSQL(
                "INSERT INTO recipe (name, instructions, image, meal_id) "
                        + "VALUES ('Late', 'x@', '', '52772')"));
        db.execSQL("INSERT INTO recipe (name, instructions, image, meal_id) "
                + "VALUES ('Custom A', 'x@', '', NULL)");
        db.execSQL("INSERT INTO recipe (name, instructions, image, meal_id) "
                + "VALUES ('Custom B', 'x@', '', NULL)");
        assertEquals("NULL meal ids never collide", 5,
                countRows(db, "SELECT COUNT(*) FROM recipe"));
        db.close();
    }

    @Test
    public void roomOpensMigratedDatabaseAndRejectsDuplicateInserts()
            throws IOException {
        openSchema6().close(); // leave the database at version 6

        RoomDB database = Room.databaseBuilder(context, RoomDB.class, DB_NAME)
                .allowMainThreadQueries()
                .addMigrations(RoomDB.MIGRATION_5_6, RoomDB.MIGRATION_6_7)
                .build();
        try {
            List<RecipeWithIngredients> all =
                    database.mainDao().getRecipeWithIngredients();
            assertEquals("migrated recipes preserved", 3, all.size());

            Recipe duplicate = new Recipe("Soup", "boil@", "a");
            duplicate.setMealId("52772");
            long rejected = database.mainDao().insertRecipeWithIngredients(
                    duplicate, java.util.Collections.singletonList(
                            new RecipeIngredients(0, new Ingredients("ONION"), 1)));
            assertEquals("room path rejects duplicate meal", -1, rejected);

            assertNotNull("meal lookup works after migration",
                    database.mainDao().findRecipeIdByMealId("52773"));
            assertEquals("rejected import added no row", 3,
                    database.mainDao().getAllRecipes().size());
        } finally {
            database.close();
        }
    }
}
