package com.example.fyp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;



import com.example.fyp.Database.MainDAO;
import com.example.fyp.Database.RoomDB;
import com.example.fyp.Models.Ingredients;
import com.example.fyp.Models.Recipe;
import com.example.fyp.Models.RecipeIngredients;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * End-to-end instrumented regression tests for the repaired core user
 * flows: inventory add/remove with expiry-record consistency, cooking a
 * custom recipe (the historic null-image crash path), the missing-
 * ingredients -> shopping list -> purchase round trip, and editing an
 * imported recipe whose image is a remote URL (the historic
 * decodeFile/BitmapDrawable NPE path).
 */
@RunWith(AndroidJUnit4.class)
public class CoreUserFlowsEspressoTest {

    private ActivityScenario<MainActivity> scenario;

    @Before
    public void clearDatabase() {
        RoomDB.getInstance(ApplicationProvider.getApplicationContext())
                .clearAllTables();
    }

    @After
    public void closeApp() {
        if (scenario != null) {
            scenario.close();
            scenario = null;
        }
    }

    private void launch() {
        scenario = ActivityScenario.launch(MainActivity.class);
        pause(1200); // initial ranking pass from the spinner callback
    }

    // ── helpers ─────────────────────────────────────────────────────────

    private interface DbQuery {
        void run(MainDAO dao, AtomicInteger result);
    }

    private int query(DbQuery q) {
        AtomicInteger out = new AtomicInteger(-999);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                () -> q.run(RoomDB.getInstance(
                        ApplicationProvider.getApplicationContext()).mainDao(), out));
        return out.get();
    }

    private void selectTab(String label) {
        onView(withText(label)).perform(click());
        // ViewPager2 creates the page fragment after the click returns;
        // let the page and its list content settle first.
        pause(1200);
    }

    /** Adds an inventory item through the dialog; date = today. */
    private void addInventoryItem(String name) {
        selectTab("INVENTORY");
        onView(visible(withId(R.id.input))).perform(click());
        onView(withId(R.id.ingredientName))
                .perform(typeText(name), closeSoftKeyboard());
        onView(withId(R.id.calendar)).perform(click());
        onView(withText("OK")).perform(click());        // date picker
        onView(withId(R.id.buttonOk)).perform(click()); // inventory dialog
        pause(500);
    }

    /** Creates a custom recipe, one ingredient at a time, no image. */
    private void createCustomRecipe(String name, String... ingredients) {
        selectTab("RECIPES");
        onView(visible(withId(R.id.addRecipe))).perform(click());
        onView(withId(R.id.enterName))
                .perform(typeText(name), closeSoftKeyboard());
        for (String ingredient : ingredients) {
            onView(withId(R.id.enterIngredient))
                    .perform(typeText(ingredient), closeSoftKeyboard());
            onView(withId(R.id.addIngredient)).perform(click());
        }
        onView(withId(R.id.done)).perform(scrollTo(), click());
        pause(800);
    }

    /**
     * The remove icon that belongs to the list row whose ingredient label
     * (text1) equals the given display name; avoids ambiguity when several
     * rows are visible.
     */
    private static Matcher removeButtonOfRow(final String display) {
        return new TypeSafeMatcher<View>() {
            @Override
            protected boolean matchesSafely(View view) {
                if (view.getId() != R.id.remove
                        || !(view.getParent() instanceof ViewGroup)) {
                    return false;
                }
                ViewGroup row = (ViewGroup) view.getParent();
                for (int i = 0; i < row.getChildCount(); i++) {
                    View child = row.getChildAt(i);
                    if (child.getId() == R.id.text1 && child instanceof TextView
                            && display.contentEquals(((TextView) child).getText())) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("remove button of row " + display);
            }
        };
    }

    // ── Flow: inventory + cooking consistency ───────────────────────────

    @Test
    public void cookingCustomRecipeConsumesInventoryAndExpiryRecords() {
        launch();
        addInventoryItem("tomato");
        createCustomRecipe("TestSoup", "tomato");

        // Makeable with urgency 0 -> first card.
        onView(allOf(withId(R.id.name), withText("TestSoup")))
                .perform(click());
        onView(withId(R.id.select)).perform(click());
        onView(withId(R.id.done)).perform(scrollTo(), click()); // cook
        pause(800);

        Assert.assertEquals("tomato row removed", 0,
                query((dao, out) -> out.set(dao.getInventoriesWithExpiryDates().size())));
        Assert.assertEquals("no stale expiry rows after cooking", 0,
                query((dao, out) -> out.set(dao.getAllExpiryDates().size())));
    }

    @Test
    public void removingLastInventoryUnitDeletesOnlyItsOwnExpiryRecord() {
        launch();
        addInventoryItem("tomato");
        addInventoryItem("milk"); // same expiry date string on purpose

        selectTab("INVENTORY");
        onView(removeButtonOfRow("Tomato")).perform(scrollTo(), click());
        pause(700);

        Assert.assertEquals("milk inventory row remains", 1,
                query((dao, out) -> out.set(dao.getInventoriesWithExpiryDates().size())));
        Assert.assertEquals("milk expiry record intact", 1,
                query((dao, out) -> out.set(dao.getAllExpiryDates().size())));
        Assert.assertEquals("tomato expiry gone", 0,
                query((dao, out) ->
                        out.set(dao.getExpiryDatesForIngredient("TOMATO").size())));
    }


    /**
     * An invalid edit (blank name) must be rejected BEFORE anything is
     * written: the historic flow deleted the original recipe first, so a
     * failed validation lost the recipe. Covers acceptance cases 2, 3, 5.
     */
    @Test
    public void invalidEditLeavesOriginalRecipeIntact() {
        launch();
        createCustomRecipe("KeepMe", "tomato", "basil");

        onView(allOf(withId(R.id.name), withText("KeepMe"))).perform(click());
        onView(withId(R.id.edit)).perform(click());
        onView(withId(R.id.enterName)).perform(replaceText("   "),
                closeSoftKeyboard());
        onView(withId(R.id.done)).perform(scrollTo(), click());
        pause(600);

        // Validation rejected the save: the editor stays open...
        onView(withId(R.id.enterName)).check(matches(isDisplayed()));
        // ...and the stored recipe with its two ingredient rows is intact.
        Assert.assertEquals("original recipe still stored", 1,
                query((dao, out) -> out.set(
                        dao.getRecipeWithIngredientsByName("KeepMe").size())));
        Assert.assertEquals("ingredient rows unchanged", 2,
                query((dao, out) -> out.set(
                        dao.getRecipeWithIngredientsByName("KeepMe").get(0)
                                .getRecipeIngredients().size())));

        // Cancelling after the failed validation changes nothing either.
        onView(withId(R.id.cancel)).perform(scrollTo(), click());
        pause(600);
        Assert.assertEquals("still stored after cancel", 1,
                query((dao, out) -> out.set(
                        dao.getRecipeWithIngredientsByName("KeepMe").size())));
        Assert.assertEquals("nothing saved under a blank name", 0,
                query((dao, out) -> out.set(dao.getAllRecipes().size() == 1 ? 0 : 1)));
    }

    /**
     * Cancelling an edit after typing new values must leave the stored
     * recipe, its image, and its external meal id untouched (acceptance
     * case 4).
     */
    @Test
    public void cancellingEditKeepsStoredRecipeUntouched() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            MainDAO dao = RoomDB.getInstance(
                    ApplicationProvider.getApplicationContext()).mainDao();
            Recipe tart = new Recipe("Tart", "fill@ bake@",
                    "https://example.invalid/t.jpg");
            tart.setMealId("52999");
            long id = dao.insertRecipe(tart);
            dao.insertRecipeIngredient(new RecipeIngredients(
                    (int) id, new Ingredients("APPLE"), 2));
        });
        launch();

        selectTab("RECIPES");
        onView(allOf(withId(R.id.name), withText("Tart"))).perform(click());
        onView(withId(R.id.edit)).perform(click());
        onView(withId(R.id.enterName)).perform(replaceText("Renamed"),
                closeSoftKeyboard());
        onView(withId(R.id.cancel)).perform(scrollTo(), click());
        pause(600);

        Assert.assertEquals("original still stored under old name", 1,
                query((dao, out) -> out.set(
                        dao.getRecipeWithIngredientsByName("Tart").size())));
        Assert.assertEquals("nothing saved under the typed new name", 0,
                query((dao, out) -> out.set(
                        dao.getRecipeWithIngredientsByName("Renamed").size())));
        Assert.assertEquals("meal id and URL image intact", 1,
                query((dao, out) -> {
                    java.util.List<Recipe> recipes = dao.getAllRecipes();
                    out.set(recipes.size() == 1
                            && "52999".equals(recipes.get(0).getMealId())
                            && recipes.get(0).getImage().startsWith("https://")
                            ? 1 : 0);
                }));
    }


    /**
     * A recipe whose remote image is unreachable must still open its
     * dialog: images are optional and the shared loader reports failure
     * as null (port 9 on loopback refuses instantly; no live network).
     */
    @Test
    public void unreachableRemoteImageStillOpensRecipeDialog() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            MainDAO dao = RoomDB.getInstance(
                    ApplicationProvider.getApplicationContext()).mainDao();
            Recipe soup = new Recipe("NoNet Soup", "boil@",
                    "http://127.0.0.1:9/soup.jpg");
            long id = dao.insertRecipe(soup);
            dao.insertRecipeIngredient(new RecipeIngredients(
                    (int) id, new Ingredients("CARROT"), 1));
        });
        launch();

        selectTab("RECIPES");
        onView(allOf(withId(R.id.name), withText("NoNet Soup"))).perform(click());
        onView(withId(R.id.select)).perform(click());
        onView(withId(R.id.ingredientList)).check(matches(isDisplayed()));
    }

    /**
     * Destroying the view while a remote image request is still in
     * flight must not crash: the guarded post drops callbacks once the
     * fragment detaches. An unroutable loopback-range address keeps the
     * connect pending until its timeout, so the screen is closed mid
     * request; a surviving re-launch proves no uncaught exception
     * escaped (which would have killed the process).
     */
    @Test
    public void closingScreenDuringPendingImageLoadDoesNotCrash() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            MainDAO dao = RoomDB.getInstance(
                    ApplicationProvider.getApplicationContext()).mainDao();
            Recipe slow = new Recipe("Slow Pic", "simmer@",
                    "http://10.255.255.1/slow.jpg");
            long id = dao.insertRecipe(slow);
            dao.insertRecipeIngredient(new RecipeIngredients(
                    (int) id, new Ingredients("CELERY"), 1));
        });
        launch();

        selectTab("RECIPES");
        onView(allOf(withId(R.id.name), withText("Slow Pic"))).perform(click());
        onView(withId(R.id.select)).perform(click());
        pause(300); // image connect is now pending
        scenario.close();
        scenario = null;

        launch();
        selectTab("RECIPES");
        onView(visible(withId(R.id.addRecipe))).check(matches(isDisplayed()));
    }

    // ── Flow: missing ingredients -> shopping list -> purchase ──────────

    @Test
    public void missingIngredientsTravelToInventoryViaShoppingList() {
        launch();
        createCustomRecipe("Squid Dish", "octopus", "octopus"); // required 2

        onView(allOf(withId(R.id.name), withText("Squid Dish")))
                .perform(click());
        onView(withId(R.id.select)).perform(click());
        onView(withId(R.id.addMissing)).perform(scrollTo(), click());
        pause(800);

        Assert.assertEquals("one shopping entry", 1,
                query((dao, out) -> out.set(dao.getAllShoppingListItems().size())));
        Assert.assertEquals("shortfall of 2 units recorded", 2,
                query((dao, out) -> out.set(dao.getAllShoppingListItems().isEmpty()
                        ? -1 : dao.getAllShoppingListItems().get(0).getAmount())));

        selectTab("SHOPPING LIST");
        onView(visible(withId(R.id.remove))).perform(scrollTo(), click());
        onView(withText("OK")).perform(click()); // purchased, expires today
        pause(800);

        Assert.assertEquals("1 unit moved into inventory", 1,
                query((dao, out) -> out.set(dao.getOneInventoryItem("OCTOPUS").isEmpty()
                        ? -1 : dao.getOneInventoryItem("OCTOPUS").get(0)
                        .getInventory().getAmount())));
        Assert.assertEquals("expiry record created for purchase", 1,
                query((dao, out) ->
                        out.set(dao.getExpiryDatesForIngredient("OCTOPUS").size())));
        Assert.assertEquals("shopping entry decremented to 1", 1,
                query((dao, out) -> out.set(dao.getAllShoppingListItems().isEmpty()
                        ? -1 : dao.getAllShoppingListItems().get(0).getAmount())));
    }

    // ── Flow: edit imported (URL-image) recipe without crashing ─────────

    @Test
    public void editingImportedRecipePreservesImageAndRelations() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            MainDAO dao = RoomDB.getInstance(
                    ApplicationProvider.getApplicationContext()).mainDao();
            Recipe pie = new Recipe("Pie", "bake one. two@",
                    "https://www.themealdb.com/images/media/meals/x.jpg");
            pie.setMealId("52772");
            long id = dao.insertRecipe(pie);
            dao.insertRecipeIngredient(new RecipeIngredients(
                    (int) id, new Ingredients("TOMATO"), 1));
        });
        launch(); // load the DAO-inserted recipe through the normal flow

        selectTab("RECIPES");
        onView(allOf(withId(R.id.name), withText("Pie")))
                .perform(click());
        onView(withId(R.id.edit)).perform(click());
        onView(withId(R.id.done)).perform(scrollTo(), click()); // save edit
        pause(900);

        Assert.assertEquals("edit leaves exactly one Pie recipe", 1,
                query((dao, out) ->
                        out.set(dao.getRecipeWithIngredientsByName("Pie").size())));
        Assert.assertEquals("ingredient relation intact", 1,
                query((dao, out) -> out.set(dao.getRecipeWithIngredientsByName("Pie")
                        .isEmpty() ? -1 : dao.getRecipeWithIngredientsByName("Pie")
                        .get(0).getRecipeIngredients().size())));
        Assert.assertEquals("meal id and URL image preserved", 1,
                query((dao, out) -> {
                    List<Recipe> recipes = dao.getAllRecipes();
                    out.set(recipes.size() == 1
                            && "52772".equals(recipes.get(0).getMealId())
                            && recipes.get(0).getImage().startsWith("https://")
                            ? 1 : 0);
                }));
    }

    /**
     * ViewPager2 keeps adjacent fragment views attached, so main-screen
     * ids can match several times; isDisplayed() keeps only the current
     * tab's copy.
     */
    private static Matcher<View> visible(Matcher<View> inner) {
        return org.hamcrest.CoreMatchers.allOf(inner, isDisplayed());
    }

    private static void pause(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
