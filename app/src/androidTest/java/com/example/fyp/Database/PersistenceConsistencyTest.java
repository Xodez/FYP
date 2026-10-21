package com.example.fyp.Database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.fyp.Models.ExpiryDate;
import com.example.fyp.Models.Ingredients;
import com.example.fyp.Models.Inventory;
import com.example.fyp.Models.InventoryWithExpiryDates;
import com.example.fyp.Models.Recipe;
import com.example.fyp.Models.RecipeIngredients;
import com.example.fyp.Models.RecipeWithIngredients;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * On-device regression tests for the schema-6 persistence repairs:
 * name-based ingredient identity, expiry records that move with the
 * units they belong to, transactional consume/remove operations, and
 * recipe deletion cleanup. Runs against in-memory Room databases; no
 * network or emulator-only UI interaction required.
 */
@RunWith(AndroidJUnit4.class)
public class PersistenceConsistencyTest {

    private RoomDB db;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, RoomDB.class)
                .allowMainThreadQueries()
                .build();
    }

    @After
    public void tearDown() {
        db.close();
    }

    private void addInventory(String name, int amount, String... expiryDates) {
        Ingredients ingredient = new Ingredients(name);
        db.mainDao().insertInventory(new Inventory(ingredient, amount));
        for (String date : expiryDates) {
            db.mainDao().insertExpiryDate(new ExpiryDate(date, ingredient));
        }
    }

    private InventoryWithExpiryDates entry(String name) {
        List<InventoryWithExpiryDates> found = db.mainDao().getOneInventoryItem(name);
        assertEquals("one row for " + name, 1, found.size());
        return found.get(0);
    }

    // ── Identity: relations and lookups work through the converters ────

    @Test
    public void inventoryJoinsItsExpiryRecordsByIngredientName() {
        addInventory("MILK", 2, "3-11-2026", "9-11-2026");

        InventoryWithExpiryDates milk = entry("MILK");
        assertEquals(2, milk.expiryDates.size());
        assertEquals(2, milk.getInventory().getAmount());
    }

    @Test
    public void recipeIngredientFromApiFindsUpperCasedInventory() {
        // Imported recipes historically stored "Tomato" while inventory
        // stored "TOMATO"; after normalisation both share one identity.
        addInventory("TOMATO", 3, "5-11-2026");

        Ingredients fromApi = new Ingredients("Tomato");
        List<InventoryWithExpiryDates> found = db.mainDao()
                .getOneInventoryItem(com.example.fyp.IngredientNames
                        .normalize(fromApi.getName()));

        assertEquals(1, found.size());
        assertEquals(3, found.get(0).getInventory().getAmount());
    }

    // ── 4A: unit removal targets the correct ingredient's records ──────

    @Test
    public void removeOneUnitDeletesOnlyItsOwnSoonestExpiryRecord() {
        // Two ingredients expiring on the SAME date: the old date-only
        // lookup could delete the other ingredient's record.
        addInventory("A", 2, "10-11-2026", "12-11-2026");
        addInventory("B", 1, "10-11-2026");

        db.mainDao().removeOneInventoryUnit("A");

        InventoryWithExpiryDates a = entry("A");
        assertEquals(1, a.getInventory().getAmount());
        assertEquals("soonest unit of A consumed", 1, a.expiryDates.size());
        assertEquals("12-11-2026", a.expiryDates.get(0).getExpiryDate());

        assertEquals("B untouched", 1, entry("B").expiryDates.size());
    }

    @Test
    public void removeOneUnitWithoutExpiryRecordsOnlyDecrementsAmount() {
        addInventory("SALT", 3);

        db.mainDao().removeOneInventoryUnit("SALT");

        assertEquals(2, entry("SALT").getInventory().getAmount());
    }

    @Test
    public void removingLastUnitClearsItemAndAllExpiryRecords() {
        addInventory("EGG", 1, "1-11-2026");

        InventoryWithExpiryDates egg = entry("EGG");
        db.mainDao().deleteInventoryItem(egg.getInventory());
        db.mainDao().deleteExpiryDatesForIngredient("EGG");

        assertTrue(db.mainDao().getOneInventoryItem("EGG").isEmpty());
        assertTrue(db.mainDao().getExpiryDatesForIngredient("EGG").isEmpty());
    }

    // ── 4B: completing a recipe keeps quantity and expiry consistent ───

    @Test
    public void consumeUnitsRemovesSoonestRecordsPerConsumedUnit() {
        addInventory("FLOUR", 5, "2-11-2026", "4-11-2026",
                "6-11-2026", "8-11-2026", "9-11-2026");

        db.mainDao().consumeInventoryUnits("FLOUR", 2);

        InventoryWithExpiryDates flour = entry("FLOUR");
        assertEquals(3, flour.getInventory().getAmount());
        assertEquals("no stale expiry rows for consumed units", 3,
                flour.expiryDates.size());
        // Remaining units are the latest-dated ones.
        assertEquals("6-11-2026", flour.expiryDates.get(0).getExpiryDate());
    }

    @Test
    public void consumeUnitsMoreRecordsThanAvailableLeavesNoNegativeRows() {
        // Legacy data can have fewer expiry rows than the amount.
        addInventory("RICE", 4, "2-11-2026");

        db.mainDao().consumeInventoryUnits("RICE", 3);

        InventoryWithExpiryDates rice = entry("RICE");
        assertEquals(1, rice.getInventory().getAmount());
        assertTrue(rice.expiryDates.isEmpty());
    }

    // ── 4E / 5C: recipe deletion cleans join rows; insert returns id ───

    @Test
    public void insertRecipeReturnsIdForIngredientAssociation() {
        Recipe recipe = new Recipe("Soup", "boil@", "");
        long id = db.mainDao().insertRecipe(recipe);

        assertTrue(id > 0);
        db.mainDao().insertRecipeIngredient(
                new RecipeIngredients((int) id, new Ingredients("TOMATO"), 2));

        List<RecipeWithIngredients> all = db.mainDao().getRecipeWithIngredients();
        assertEquals(1, all.size());
        assertEquals("Soup", all.get(0).getRecipe().getName());
        assertEquals(1, all.get(0).getRecipeIngredients().size());
    }

    @Test
    public void deletingRecipeRemovesItsIngredientRowsOnly() {
        Recipe sharedIngredientUser = new Recipe("Soup", "boil@", "");
        Recipe other = new Recipe("Sauce", "mix@", "");
        db.mainDao().insertIngredient(new Ingredients("TOMATO"));
        long soupId = db.mainDao().insertRecipe(sharedIngredientUser);
        long sauceId = db.mainDao().insertRecipe(other);
        db.mainDao().insertRecipeIngredient(
                new RecipeIngredients((int) soupId, new Ingredients("TOMATO"), 1));
        RecipeIngredients sauceRow = new RecipeIngredients(
                (int) sauceId, new Ingredients("TOMATO"), 1);
        db.mainDao().insertRecipeIngredient(sauceRow);

        // Reload from the database: @Insert(long) does not write the
        // generated key back into the passed entity, and production code
        // always deletes recipes it loaded from the table.
        Recipe persistedSauce = null;
        for (Recipe candidate : db.mainDao().getAllRecipes()) {
            if (candidate.getName().equals("Sauce")) {
                persistedSauce = candidate;
            }
        }
        assertNotNull(persistedSauce);
        db.mainDao().deleteRecipeWithIngredients(persistedSauce);

        assertTrue(db.mainDao().getRecipeWithIngredientsByName("Sauce").isEmpty());
        List<RecipeWithIngredients> remaining =
                db.mainDao().getRecipeWithIngredients();
        assertEquals(1, remaining.size());
        assertEquals("soup join row untouched", 1,
                remaining.get(0).getRecipeIngredients().size());
        assertEquals("shared vocabulary ingredient not deleted", 1,
                db.mainDao().getAllIngredients().size());
    }

    // ── 5D: meal_id deduplication support ──────────────────────────────

    @Test
    public void importedRecipeIdLookupSupportsDeduplication() {
        Recipe meal = new Recipe("Pie", "bake@", "");
        meal.setMealId("52772");
        db.mainDao().insertRecipe(meal);

        assertNotNull(db.mainDao().findRecipeIdByMealId("52772"));
        assertNull(db.mainDao().findRecipeIdByMealId("99999"));
    }

    // ── §6: unique meal_id makes import deduplication atomic ───────────

    @Test
    public void secondImportOfSameMealIdIsRejectedWithoutDuplicates() {
        Recipe first = new Recipe("Soup", "boil@", "img1");
        first.setMealId("52772");
        long id = db.mainDao().insertRecipeWithIngredients(first,
                Arrays.asList(
                        new RecipeIngredients(0, new Ingredients("TOMATO"), 1),
                        new RecipeIngredients(0, new Ingredients("SALT"), 1)));
        assertTrue(id > 0);

        Recipe again = new Recipe("Soup", "boil@", "img1");
        again.setMealId("52772");
        long rejected = db.mainDao().insertRecipeWithIngredients(again,
                Collections.singletonList(
                        new RecipeIngredients(0, new Ingredients("BELL PEPPER"), 1)));

        assertEquals("IGNORE conflict reports -1", -1, rejected);
        assertEquals(1, db.mainDao().getAllRecipes().size());
        List<RecipeIngredients> surviving = db.mainDao()
                .getRecipeWithIngredientsByName("Soup").get(0)
                .getRecipeIngredients();
        assertEquals("survivor keeps exactly its own rows", 2, surviving.size());
        for (RecipeIngredients row : surviving) {
            assertFalse("rejected import attached nothing",
                    row.getIngredients().getName().equals("BELL PEPPER"));
        }
    }

    @Test
    public void concurrentImportsOfOneMealKeepExactlyOneRecipe() throws Exception {
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(2);
        final AtomicInteger accepted = new AtomicInteger();
        for (int attempt = 0; attempt < 2; attempt++) {
            new Thread(() -> {
                Recipe recipe = new Recipe("Pie", "bake@", "img");
                recipe.setMealId("52999");
                try {
                    start.await();
                    long id = db.mainDao().insertRecipeWithIngredients(recipe,
                            Collections.singletonList(new RecipeIngredients(
                                    0, new Ingredients("APPLE"), 1)));
                    if (id != -1) {
                        accepted.incrementAndGet();
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }
        start.countDown();
        assertTrue("imports did not finish", done.await(10, TimeUnit.SECONDS));

        assertEquals("unique index admits exactly one import", 1, accepted.get());
        assertEquals(1, db.mainDao().getAllRecipes().size());
        assertEquals("survivor keeps its own ingredient row", 1, db.mainDao()
                .getRecipeWithIngredientsByName("Pie").get(0)
                .getRecipeIngredients().size());
    }

    @Test
    public void differentMealsAndCustomNullIdsImportFreely() {
        Recipe mealA = new Recipe("A", "x@", "");
        mealA.setMealId("111");
        Recipe mealB = new Recipe("B", "y@", "");
        mealB.setMealId("222");
        assertTrue(db.mainDao().insertRecipeWithIngredients(mealA,
                Collections.singletonList(new RecipeIngredients(
                        0, new Ingredients("RICE"), 1))) > 0);
        assertTrue(db.mainDao().insertRecipeWithIngredients(mealB,
                Collections.emptyList()) > 0);

        // Custom recipes have no external id at all; NULL meal ids must
        // never collide with the unique index.
        for (int i = 0; i < 3; i++) {
            Recipe custom = new Recipe("Family Dish " + i, "step@", "");
            assertTrue(db.mainDao().insertRecipeWithIngredients(custom,
                    Collections.emptyList()) > 0);
        }

        assertEquals(5, db.mainDao().getAllRecipes().size());
    }
}