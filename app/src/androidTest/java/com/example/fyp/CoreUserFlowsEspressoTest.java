package com.example.fyp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.scrollTo;
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
