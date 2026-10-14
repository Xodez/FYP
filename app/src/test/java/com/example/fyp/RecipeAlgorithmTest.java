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
 * These tests encode the INTENDED behaviour of the ingredient matching and
 * ranking pipeline (case-insensitive matching, expiry measured in real
 * calendar days, makeable recipes ranked ahead of recipes requiring
 * unavailable ingredients, sooner expiry prioritised, quantity sufficiency
 * respected, degenerate inputs handled safely) - not the way the current
 * implementation happens to behave.
 *
 * Several tests therefore fail against the existing algorithm and document
 * verified production defects:
 *  - ingredient matching is case-sensitive and whitespace-sensitive
 *    (RecipeAlgorithm uses String.equals on names), so TheMealDB recipes
 *    never match uppercased inventory entries;
 *  - days-to-expiry is derived from Period.between(...).getDays(), which is
 *    the day-of-month component, not the calendar day count, so an item
 *    expiring "one month from now" is scored as expiring today;
 *  - the comparator truncates float score differences to int, collapsing
 *    fractional scores into ties ordered by input position.
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
}
