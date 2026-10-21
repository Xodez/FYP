package com.example.fyp.Database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;

import androidx.room.Room;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.fyp.Models.InventoryWithExpiryDates;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;

/**
 * Verifies the explicit 5-to-6 migration: legacy rows survive, embedded
 * Gson ingredient values are rewritten to plain normalised names so the
 * new name-based relations still join, recipe.meal_id is added, and Room
 * opens a schema-5 database and migrates it instead of destroying data.
 */
@RunWith(AndroidJUnit4.class)
public class Migration5To6Test {

    private static final String DB_NAME = "migration-test";

    private Context context;

    /** Creates the pre-migration (schema 5) database with legacy rows. */
    private final SupportSQLiteOpenHelper.Callback legacySchema =
            new SupportSQLiteOpenHelper.Callback(5) {
                @Override
                public void onCreate(SupportSQLiteDatabase db) {
                    db.execSQL("CREATE TABLE ingredients (ID INTEGER "
                            + "PRIMARY KEY AUTOINCREMENT NOT NULL, name "
                            + "TEXT)");
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
                    db.execSQL("CREATE TABLE RecipeIngredients (ID INTEGER "
                            + "PRIMARY KEY AUTOINCREMENT NOT NULL, recipe_id "
                            + "INTEGER NOT NULL, ingredient TEXT, count "
                            + "INTEGER NOT NULL)");
                    // Schema 5: identical to Room's schema-6 DDL except
                    // the missing meal_id column (added by the migration).
                    db.execSQL("CREATE TABLE recipe (ID INTEGER PRIMARY KEY "
                            + "AUTOINCREMENT NOT NULL, name TEXT, "
                            + "instructions TEXT, image TEXT)");

                    // Data exactly as the schema-5 code stored it:
                    // ingredients as Gson dumps whose IDs drift per path.
                    db.execSQL("INSERT INTO recipe (name, instructions, "
                            + "image) VALUES ('Pie', 'bake@', 'pic')");
                    db.execSQL("INSERT INTO inventory (ingredient, amount) "
                            + "VALUES ('{\"ID\":0,\"name\":\"MILK\"}', 2)");
                    db.execSQL("INSERT INTO expiry_date "
                            + "(inventory_ingredient, expiry_date) VALUES "
                            + "('{\"ID\":0,\"name\":\"MILK\"}', '5-11-2026')");
                    db.execSQL("INSERT INTO expiry_date "
                            + "(inventory_ingredient, expiry_date) VALUES "
                            + "('{\"ID\":0,\"name\":\"MILK\"}', '6-11-2026')");
                    db.execSQL("INSERT INTO shopping_list (ID, ingredient, "
                            + "amount) VALUES ('BREAD', "
                            + "'{\"ID\":4,\"name\":\"Bread\"}', 1)");
                    db.execSQL("INSERT INTO RecipeIngredients (recipe_id, "
                            + "ingredient, count) VALUES (1, "
                            + "'{\"ID\":7,\"name\":\"Chicken Breast\"}', 2)");
                    db.execSQL("INSERT INTO ingredients (name) VALUES ('Bread')");
                }

                @Override
                public void onUpgrade(SupportSQLiteDatabase db, int oldVersion,
                                      int newVersion) {
                    // The legacy database is created at version 5 and the
                    // migration under test is applied explicitly (or by
                    // Room); no callback upgrade path is used here.
                }
            };

    private SupportSQLiteDatabase openLegacyDatabase() {
        SupportSQLiteOpenHelper.Configuration configuration =
                SupportSQLiteOpenHelper.Configuration.builder(context)
                        .name(DB_NAME)
                        .callback(legacySchema)
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

    @Test
    public void migrationRewritesIdentityAndKeepsAllRows() throws IOException {
        SupportSQLiteDatabase db = openLegacyDatabase();
        RoomDB.MIGRATION_5_6.migrate(db);

        Cursor inventory = db.query(
                "SELECT ingredient, amount FROM inventory", null);
        assertTrue(inventory.moveToFirst());
        assertEquals("MILK", inventory.getString(0));
        assertEquals(2, inventory.getInt(1));
        inventory.close();

        Cursor expiry = db.query("SELECT inventory_ingredient, expiry_date "
                + "FROM expiry_date ORDER BY expiry_date", null);
        assertEquals(2, expiry.getCount());
        assertTrue(expiry.moveToFirst());
        assertEquals("MILK", expiry.getString(0));
        assertEquals("5-11-2026", expiry.getString(1));
        expiry.close();

        Cursor list = db.query(
                "SELECT ID, ingredient, amount FROM shopping_list", null);
        assertTrue(list.moveToFirst());
        assertEquals("BREAD", list.getString(0));
        assertEquals("BREAD", list.getString(1));
        assertEquals(1, list.getInt(2));
        list.close();

        Cursor join = db.query("SELECT recipe_id, ingredient, count FROM "
                + "RecipeIngredients", null);
        assertTrue(join.moveToFirst());
        assertEquals(1, join.getInt(0));
        assertEquals("CHICKEN BREAST", join.getString(1));
        assertEquals(2, join.getInt(2));
        join.close();

        Cursor recipe = db.query("SELECT meal_id FROM recipe", null);
        assertTrue(recipe.moveToFirst());
        assertTrue(recipe.isNull(0));
        recipe.close();

        Cursor vocabulary = db.query("SELECT name FROM ingredients", null);
        assertTrue(vocabulary.moveToFirst());
        assertEquals("BREAD", vocabulary.getString(0));
        vocabulary.close();

        db.close();
    }

    @Test
    public void roomOpensLegacyDatabaseAndMigratesRelations() throws IOException {
        // Create the schema-5 database, then let Room discover version 5
        // and apply the registered migration on open.
        openLegacyDatabase().close();

        RoomDB database = Room.databaseBuilder(context, RoomDB.class, DB_NAME)
                .allowMainThreadQueries()
                .addMigrations(RoomDB.MIGRATION_5_6, RoomDB.MIGRATION_6_7)
                .build();
        try {
            List<InventoryWithExpiryDates> inventories =
                    database.mainDao().getInventoriesWithExpiryDates();
            assertEquals("legacy inventory preserved", 1, inventories.size());
            assertEquals("MILK", inventories.get(0).getInventory()
                    .getIngredient().getName());
            assertEquals("legacy expiry records still relate", 2,
                    inventories.get(0).getExpiryDates().size());

            assertEquals("legacy recipe preserved", 1,
                    database.mainDao().getRecipeWithIngredients().size());
            assertEquals(1, database.mainDao().getAllShoppingListItems().size());
        } finally {
            database.close();
        }
    }
}
