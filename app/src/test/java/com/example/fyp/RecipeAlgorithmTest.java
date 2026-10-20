package com.example.fyp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.fyp.Models.ExpiryDate;
import com.example.fyp.Models.Ingredients;
import com.example.fyp.Models.Inventory;
import com.example.fyp.Models.InventoryWithExpiryDates;
import com.example.fyp.Models.Recipe;
import com.example.fyp.Models.RecipeIngredients;
import com.example.fyp.Models.RecipeWithIngredients;

import org.junit.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Regression tests for RecipeAlgorithm recommendation behaviour.
 *
 * These tests encode the intended behaviour of the ingredient matching and
 * ranking pipeline (case-insensitive matching, expiry measured in real
 * calendar days, makeable recipes ranked ahead of recipes requiring
 * unavailable ingredients, sooner expiry prioritised, quantity sufficiency
 * respected, degenerate inputs handled safely).
 *
 * Five of these tests previously failed and documented verified production
 * defects (case-sensitive matching, day-of-month expiry arithmetic via
 * Period.getDays(), and int-truncated comparator scores). They now serve
 * as guards against regressions of those defects.
 */
public class RecipeAlgorithmTest {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("d-M-yyyy");

    private final RecipeAlgorithm algorithm = new RecipeAlgorithm();

    // ── fixtures ────────────────────────────────────────────────────────

    private static String inDays(int days) {
        return LocalDate.now().plusDays(days).format(DATE_FORMAT);
    }

    private static String inMonths(int months) {
        return LocalDate.now().plusMonths(months).format(DATE_FORMAT);
    }

    /** Inventory entry for one ingredient with a single expiry date. */
    private static InventoryWithExpiryDates pantry(String name, int amount,
                                                   String expiryDate) {
        Ingredients ingredient = new Ingredients(name);
        Inventory inventory = new Inventory(ingredient, amount);
        InventoryWithExpiryDates entry = new InventoryWithExpiryDates();
        entry.setInventory(inventory);
        entry.setExpiryDates(new ArrayList<>(
                Collections.singletonList(new ExpiryDate(expiryDate, ingredient))));
        return entry;
    }

    /** Inventory entry with explicit (possibly invalid) expiry dates. */
    private static InventoryWithExpiryDates pantryWithDates(String name,
                                                            int amount,
                                                            String... expiryDates) {
        Ingredients ingredient = new Ingredients(name);
        Inventory inventory = new Inventory(ingredient, amount);
        InventoryWithExpiryDates entry = new InventoryWithExpiryDates();
        entry.setInventory(inventory);
        List<ExpiryDate> dates = new ArrayList<>();
        for (String date : expiryDates) {
            dates.add(new ExpiryDate(date, ingredient));
        }
        entry.setExpiryDates(dates);
        return entry;
    }

    private static RecipeIngredients required(String name, int count) {
        return new RecipeIngredients(0, new Ingredients(name), count);
    }

    private static RecipeWithIngredients recipe(String name,
                                                RecipeIngredients... ingredients) {
        Recipe recipe = new Recipe(name, "mix@", "");
        RecipeWithIngredients withIngredients = new RecipeWithIngredients();
        withIngredients.setRecipe(recipe);
        withIngredients.setRecipeIngredients(
                new ArrayList<>(Arrays.asList(ingredients)));
        return withIngredients;
    }

    private static int indexOfRecipe(List<Recipe> result, String name) {
        for (int i = 0; i < result.size(); i++) {
            if (result.get(i).getName().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    private static void assertRankedAhead(List<Recipe> result, String earlier,
                                          String later) {
        int earlierIndex = indexOfRecipe(result, earlier);
        int laterIndex = indexOfRecipe(result, later);
        assertTrue(earlier + " must be returned as a recommendation, got " + result,
                earlierIndex >= 0);
        assertTrue(later + " must be returned as a recommendation, got " + result,
                laterIndex >= 0);
        assertTrue(
                "Expected \"" + earlier + "\" ranked ahead of \"" + later
                        + "\" but result order was " + namesOf(result),
                earlierIndex < laterIndex);
    }

    private static List<String> namesOf(List<Recipe> result) {
        List<String> names = new ArrayList<>();
        for (Recipe recipe : result) {
            names.add(recipe.getName());
        }
        return names;
    }

    // ── A. ingredient matching ──────────────────────────────────────────

    /**
     * Intended: an inventory name and a recipe ingredient name that differ
     * only in capitalisation are the same ingredient. TheMealDB returns
     * names like "Tomato" while locally added inventory is uppercased.
     */
    @Test
    public void matchingIgnoresIngredientNameCapitalisation() {
        List<InventoryWithExpiryDates> exp = Arrays.asList(
                pantry("TOMATO", 2, inDays(3)),
                pantry("FLOUR", 2, inDays(25)));
        // Uses the MealDB-style spelling "Tomato".
        RecipeWithIngredients soup =
                recipe("Tomato Soup", required("Tomato", 2));
        // Uses the exact stored spelling.
        RecipeWithIngredients bread =
                recipe("Bread", required("FLOUR", 2));

        List<Recipe> result = algorithm.filterRecipeByScore(
                Arrays.asList(soup, bread), exp);

        // Both are makeable; soup's tomato expires sooner, so it must lead.
        assertRankedAhead(result, "Tomato Soup", "Bread");
    }

    /**
     * Intended: surrounding whitespace in an ingredient name does not break
     * matching (names flow in from external APIs and free-text input).
     */
    @Test
    public void matchingIgnoresSurroundingWhitespace() {
        List<InventoryWithExpiryDates> exp = Arrays.asList(
                pantry("SPINACH", 2, inDays(4)),
                pantry("RICE", 2, inDays(25)));
        RecipeWithIngredients salad =
                recipe("Spinach Salad", required(" spinach ", 2));
        RecipeWithIngredients risotto =
                recipe("Rice Bowl", required("RICE", 2));

        List<Recipe> result = algorithm.filterRecipeByScore(
                Arrays.asList(salad, risotto), exp);

        assertRankedAhead(result, "Spinach Salad", "Rice Bowl");
    }

    // ── B. expiry-date calculation ──────────────────────────────────────

    /**
     * Intended: an ingredient expiring tomorrow outweighs one expiring
     * roughly a month from now. The current implementation reads the
     * day-of-month component of the period, so the month-away ingredient
     * wrongly scores as expiring today.
     */
    @Test
    public void expiringTomorrowRanksAheadOfExpiringNextMonth() {
        List<InventoryWithExpiryDates> exp = Arrays.asList(
                pantry("MILK", 1, inDays(1)),
                pantry("FLOUR", 1, inMonths(1)));
        RecipeWithIngredients pancakes =
                recipe("Pancakes", required("MILK", 1));
        RecipeWithIngredients loaf =
                recipe("Loaf", required("FLOUR", 1));

        List<Recipe> result = algorithm.filterRecipeByScore(
                Arrays.asList(pancakes, loaf), exp);

        assertRankedAhead(result, "Pancakes", "Loaf");
    }

    /**
     * Intended: dates spanning a year boundary still count real days.
     * Expiring in 12 months must rank behind expiring in 3 days.
     */
    @Test
    public void expiringInDaysRanksAheadOfExpiringNextYear() {
        List<InventoryWithExpiryDates> exp = Arrays.asList(
                pantry("MILK", 1, inDays(3)),
                pantry("RICE", 1, inMonths(12)));
        RecipeWithIngredients pudding =
                recipe("Rice Pudding", required("MILK", 1));
        RecipeWithIngredients porridge =
                recipe("Porridge", required("RICE", 1));

        List<Recipe> result = algorithm.filterRecipeByScore(
                Arrays.asList(pudding, porridge), exp);

        assertRankedAhead(result, "Rice Pudding", "Porridge");
    }

    /**
     * Intended: within-range day differences order recipes by urgency.
     * This also covers expiry dates landing across a month boundary,
     * since both dates are real calendar dates from today's perspective.
     */
    @Test
    public void expiringInTwoDaysRanksAheadOfExpiringInNineDays() {
        List<InventoryWithExpiryDates> exp = Arrays.asList(
                pantry("MILK", 1, inDays(2)),
                pantry("FLOUR", 1, inDays(9)));
        RecipeWithIngredients crepes =
                recipe("Crepes", required("MILK", 1));
        RecipeWithIngredients pasta =
                recipe("Pasta", required("FLOUR", 1));

        List<Recipe> result = algorithm.filterRecipeByScore(
                Arrays.asList(crepes, pasta), exp);

        assertRankedAhead(result, "Crepes", "Pasta");
    }

    // ── C. ranking and ordering ─────────────────────────────────────────

    /**
     * Intended: a fully makeable recipe ranks ahead of a recipe that
     * requires an ingredient absent from the inventory.
     */
    @Test
    public void makeableRecipeRanksAheadOfRecipeWithMissingIngredient() {
        List<InventoryWithExpiryDates> exp =
                Collections.singletonList(pantry("TOMATO", 2, inDays(5)));
        RecipeWithIngredients soup =
                recipe("Tomato Soup", required("TOMATO", 2));
        RecipeWithIngredients yoghurtDish =
                recipe("Yoghurt Parfait", required("YOGURT", 1));

        List<Recipe> result = algorithm.filterRecipeByScore(
                Arrays.asList(soup, yoghurtDish), exp);

        assertEquals("every recipe must still be returned", 2, result.size());
        assertRankedAhead(result, "Tomato Soup", "Yoghurt Parfait");
    }

    /**
     * Intended: equal urgency and equal structure give the same ranking
     * regardless of the order recipes arrive in; a recipe using an item
     * expiring in 2 days must lead one using an item expiring in 20 days
     * in both input orders.
     */
    @Test
    public void rankingIsIndependentOfInputOrder() {
        List<InventoryWithExpiryDates> exp = Arrays.asList(
                pantry("MILK", 1, inDays(2)),
                pantry("FLOUR", 1, inDays(20)));
        RecipeWithIngredients crepes =
                recipe("Crepes", required("MILK", 1));
        RecipeWithIngredients pasta =
                recipe("Pasta", required("FLOUR", 1));

        List<Recipe> forward = algorithm.filterRecipeByScore(
                Arrays.asList(crepes, pasta), exp);
        List<Recipe> reversed = algorithm.filterRecipeByScore(
                Arrays.asList(pasta, crepes), exp);

        assertRankedAhead(forward, "Crepes", "Pasta");
        assertRankedAhead(reversed, "Crepes", "Pasta");
    }

    /**
     * Intended: scores keep enough precision to order recipes whose raw
     * scores differ but both round to the same integer. Here both recipes
     * are fully makeable; the "Crepes" ingredients expire in 2 days vs 9
     * days for "Pasta", so Crepes must lead even when passed second.
     * The current comparator truncates (float)(a - b) to int, making the
     * two recipes tie and preserving the input order instead.
     */
    @Test
    public void fractionalScoreDifferencesAreNotTruncatedIntoTies() {
        List<InventoryWithExpiryDates> exp = Arrays.asList(
                pantry("MILK", 10, inDays(2)),
                pantry("SUGAR", 10, inDays(2)),
                pantry("FLOUR", 10, inDays(9)),
                pantry("RICE", 10, inDays(9)));
        RecipeWithIngredients crepes = recipe("Crepes",
                required("MILK", 1), required("SUGAR", 1));
        RecipeWithIngredients pasta = recipe("Pasta",
                required("FLOUR", 1), required("RICE", 1));

        // Pasta supplied first; truncation keeps that order, urgency must not.
        List<Recipe> result = algorithm.filterRecipeByScore(
                Arrays.asList(pasta, crepes), exp);

        assertRankedAhead(result, "Crepes", "Pasta");
    }

    // ── D. quantity handling ────────────────────────────────────────────

    /**
     * Intended: owning exactly the required quantity makes a recipe
     * makeable and ranks it ahead of a recipe with an absent ingredient.
     */
    @Test
    public void exactAvailableQuantityCountsAsMakeable() {
        List<InventoryWithExpiryDates> exp =
                Collections.singletonList(pantry("TOMATO", 2, inDays(5)));
        RecipeWithIngredients soup =
                recipe("Tomato Soup", required("TOMATO", 2));
        RecipeWithIngredients yoghurtDish =
                recipe("Yoghurt Parfait", required("YOGURT", 1));

        List<Recipe> result = algorithm.filterRecipeByScore(
                Arrays.asList(soup, yoghurtDish), exp);

        assertRankedAhead(result, "Tomato Soup", "Yoghurt Parfait");
    }

    /**
     * Intended: owning more than the required quantity also counts as
     * makeable.
     */
    @Test
    public void surplusAvailableQuantityCountsAsMakeable() {
        List<InventoryWithExpiryDates> exp =
                Collections.singletonList(pantry("MILK", 10, inDays(6)));
        RecipeWithIngredients cereal =
                recipe("Cereal", required("MILK", 1));
        RecipeWithIngredients yoghurtDish =
                recipe("Yoghurt Parfait", required("YOGURT", 1));

        List<Recipe> result = algorithm.filterRecipeByScore(
                Arrays.asList(cereal, yoghurtDish), exp);

        assertRankedAhead(result, "Cereal", "Yoghurt Parfait");
    }

    /**
     * Intended: a recipe whose required quantity exceeds what is owned is
     * not fully makeable, so it must rank behind a recipe that is.
     */
    @Test
    public void insufficientQuantityIsNotTreatedAsMakeable() {
        List<InventoryWithExpiryDates> exp = Arrays.asList(
                pantry("TOMATO", 1, inDays(2)),
                pantry("FLOUR", 2, inDays(25)));
        RecipeWithIngredients soup =
                recipe("Tomato Soup", required("TOMATO", 3));
        RecipeWithIngredients bread =
                recipe("Bread", required("FLOUR", 2));

        List<Recipe> result = algorithm.filterRecipeByScore(
                Arrays.asList(soup, bread), exp);

        assertRankedAhead(result, "Bread", "Tomato Soup");
    }

    // ── E. empty and edge-case inputs ───────────────────────────────────

    /** Intended: an empty recipe collection yields an empty result. */
    @Test
    public void emptyRecipeListYieldsEmptyResult() {
        List<Recipe> result = algorithm.filterRecipeByScore(
                Collections.emptyList(),
                Collections.singletonList(pantry("MILK", 1, inDays(2))));

        assertEquals(Collections.emptyList(), result);
    }

    /**
     * Intended: with an empty inventory every recipe is still returned
     * (as a shopping-required alternative) and nothing throws.
     */
    @Test
    public void emptyInventoryStillReturnsAllRecipes() {
        RecipeWithIngredients soup =
                recipe("Tomato Soup", required("TOMATO", 1));
        RecipeWithIngredients bread =
                recipe("Bread", required("FLOUR", 1));

        List<Recipe> result = algorithm.filterRecipeByScore(
                Arrays.asList(soup, bread), Collections.emptyList());

        assertEquals("every recipe must still be returned", 2, result.size());
    }

    /**
     * Intended: a recipe with no ingredient requirements must not surface
     * as the top recommendation ahead of a real makeable recipe (the
     * algorithm must not leak validity state between successive recipes).
     */
    @Test
    public void recipeWithoutIngredientsIsNotTopRecommendation() {
        List<InventoryWithExpiryDates> exp =
                Collections.singletonList(pantry("MILK", 1, inDays(2)));
        RecipeWithIngredients emptyMenu = recipe("Empty Menu");
        RecipeWithIngredients crepes =
                recipe("Crepes", required("MILK", 1));

        List<Recipe> result = algorithm.filterRecipeByScore(
                Arrays.asList(emptyMenu, crepes), exp);

        assertTrue("a real makeable recipe must be ranked first, got "
                        + namesOf(result),
                !result.isEmpty() && result.get(0).getName().equals("Crepes"));
    }

    /**
     * Chosen policy: a recipe without ingredient requirements is excluded
     * from ranked recommendations rather than promoted or leaking state.
     */
    @Test
    public void recipeWithoutIngredientsIsExcludedFromResults() {
        List<InventoryWithExpiryDates> exp =
                Collections.singletonList(pantry("MILK", 1, inDays(2)));
        RecipeWithIngredients emptyMenu = recipe("Empty Menu");
        RecipeWithIngredients crepes =
                recipe("Crepes", required("MILK", 1));

        List<Recipe> result = algorithm.filterRecipeByScore(
                Arrays.asList(emptyMenu, crepes), exp);

        assertEquals(Collections.singletonList("Crepes"), namesOf(result));
    }

    /**
     * Chosen policy: an inventory item with no usable expiry information
     * keeps its identity and quantity but its urgency counts as unknown,
     * so recipes relying on it must not outrank otherwise comparable
     * recipes with valid expiry data.
     */
    @Test
    public void unknownExpiryRanksBehindComparableKnownExpiry() {
        List<InventoryWithExpiryDates> exp = Arrays.asList(
                pantryWithDates("TOFU", 1),                    // no rows
                pantryWithDates("RICE", 1, "32-13-2026"),      // malformed
                pantry("POTATO", 1, inDays(60)));              // valid, far
        RecipeWithIngredients tofuDish =
                recipe("Tofu Stir Fry", required("TOFU", 1));
        RecipeWithIngredients riceDish =
                recipe("Rice Pilaf", required("RICE", 1));
        RecipeWithIngredients potatoDish =
                recipe("Potato Gratin", required("POTATO", 1));

        List<Recipe> result = algorithm.filterRecipeByScore(
                Arrays.asList(tofuDish, riceDish, potatoDish), exp);

        assertEquals("all three recipes remain makeable and returned",
                3, result.size());
        // The recipe with valid (even if distant) expiry data must lead.
        assertEquals("Potato Gratin", result.get(0).getName());
        // Unknown-urgency recipes rank behind it, never ahead.
        int tofuIndex = indexOfRecipe(result, "Tofu Stir Fry");
        int riceIndex = indexOfRecipe(result, "Rice Pilaf");
        assertTrue("Potato Gratin", tofuIndex > 0);
        assertTrue("Potato Gratin", riceIndex > 0);
    }

    /** Chosen policy: malformed expiry data never crashes ranking. */
    @Test
    public void malformedAndNullExpiryEntriesDoNotThrow() {
        List<InventoryWithExpiryDates> exp = Arrays.asList(
                pantryWithDates("BEANS", 2, "garbage", null, "1-1-2999"),
                pantry("LENTILS", 2, inDays(4)));
        RecipeWithIngredients chilli =
                recipe("Chilli", required("BEANS", 1), required("LENTILS", 1));

        List<Recipe> result = algorithm.filterRecipeByScore(
                Collections.singletonList(chilli), exp);

        assertEquals(Collections.singletonList("Chilli"), namesOf(result));
    }

    /**
     * Chosen policy: already-expired items count as maximum urgency (0
     * days) but never as negative extreme scores; the ranking is a
     * waste-reduction heuristic, not a food-safety judgement.
     */
    @Test
    public void expiredIngredientRanksWithMaximumUrgencyNotExtremeScore() {
        List<InventoryWithExpiryDates> exp = Arrays.asList(
                pantry("YOGURT", 1, inDays(-5)),   // expired 5 days ago
                pantry("FLOUR", 1, inDays(20)));
        RecipeWithIngredients pancakes =
                recipe("Pancakes", required("YOGURT", 1));
        RecipeWithIngredients bread =
                recipe("Bread", required("FLOUR", 1));

        List<Recipe> result = algorithm.filterRecipeByScore(
                Arrays.asList(bread, pancakes), exp);

        // Expired-yogurt recipe is most urgent; it must not sink or throw,
        // and both recipes remain available.
        assertRankedAhead(result, "Pancakes", "Bread");
    }

    /**
     * Chosen policy: an inventory row whose amount is zero or negative is
     * out of stock; a recipe requiring none of it still cannot score as a
     * makeable top recommendation through division-by-zero behaviour.
     */
    @Test
    public void zeroAmountInventoryCountsAsMissing() {
        List<InventoryWithExpiryDates> exp =
                Collections.singletonList(pantry("SALT", 0, inDays(10)));
        RecipeWithIngredients soup = recipe("Soup", required("SALT", 1));
        RecipeWithIngredients bread =
                recipe("Bread", required("FLOUR", 1));
        List<InventoryWithExpiryDates> withFlour = Arrays.asList(
                pantry("SALT", 0, inDays(10)),
                pantry("FLOUR", 2, inDays(20)));

        List<Recipe> result = algorithm.filterRecipeByScore(
                Arrays.asList(soup, bread), withFlour);

        assertEquals("Bread", result.get(0).getName());
    }

    /** IngredientNames normalisation rules used by matching. */
    @Test
    public void normalisationTrimsAndUpperCasesWithoutLocaleSurprises() {
        assertEquals("TOMATO", IngredientNames.normalize(" tomato "));
        assertEquals("MILK", IngredientNames.normalize("Milk"));
        assertEquals("SPINACH", IngredientNames.normalize("SPINACH"));
        assertEquals("", IngredientNames.normalize(null));
        // Locale-independent: Turkish locale must not break the I case.
        assertEquals("ONIONS", IngredientNames.normalize("onions"));
    }
}
