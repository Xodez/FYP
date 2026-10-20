package com.example.fyp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.fyp.Models.Meal;

import org.junit.Test;

import java.util.List;

/**
 * Offline regression tests for MealDB response handling. No network: the
 * parsing contract is exercised with recorded response shapes, including
 * the malformed bodies that previously crashed the UI thread.
 */
public class MealApiTest {

    // ── search (filter.php) responses ───────────────────────────────────

    @Test
    public void validSearchResponseReturnsAllMealIds() {
        String body = "{\"meals\":[{\"idMeal\":\"52772\",\"strMeal\":\"X\","
                + "\"strMealThumb\":\"https://y/z.jpg\"},"
                + "{\"idMeal\":\"52778\",\"strMeal\":\"Y\"}]}";

        assertEquals(java.util.Arrays.asList("52772", "52778"),
                MealApi.parseMealIds(body));
    }

    @Test
    public void noMatchResponseIsAnEmptyListNotAnError() {
        assertEquals(java.util.Collections.emptyList(),
                MealApi.parseMealIds("{\"meals\": null}"));
    }

    @Test
    public void emptyOrMalformedSearchBodyIsNullInvalid() {
        assertNull(MealApi.parseMealIds(null));
        assertNull(MealApi.parseMealIds(""));
        assertNull(MealApi.parseMealIds("<html>502 Bad Gateway</html>"));
        assertNull(MealApi.parseMealIds("[1,2,3]"));
        assertNull(MealApi.parseMealIds("{\"meals\": \"oops\"}"));
    }

    // ── lookup (lookup.php) responses ───────────────────────────────────

    private static final String LOOKUP_OK = "{\"meals\":[{\"strMeal\":"
            + "\"Chicken Pot Pie\",\"strInstructions\":\"Step one. Step two.\","
            + "\"strMealThumb\":\"https://thumb/1.jpg\","
            + "\"strIngredient1\":\"Chicken\",\"strIngredient2\":null,"
            + "\"strIngredient3\":\"\",\"strIngredient4\":\" \","
            + "\"strIngredient5\":\"Peas\",\"strIngredient6\":\"Water\","
            + "\"strIngredient7\":null,\"strIngredient8\":null,"
            + "\"strIngredient9\":null,\"strIngredient10\":null,"
            + "\"strIngredient11\":null,\"strIngredient12\":null,"
            + "\"strIngredient13\":null,\"strIngredient14\":null,"
            + "\"strIngredient15\":null,\"strIngredient16\":null,"
            + "\"strIngredient17\":null,\"strIngredient18\":null,"
            + "\"strIngredient19\":null,\"strIngredient20\":null}]}";

    @Test
    public void validLookupMealParses() {
        Meal meal = MealApi.parseMeal(LOOKUP_OK);

        assertEquals("Chicken Pot Pie", meal.getStrMeal());
    }

    @Test
    public void emptyIngredientsDoNotTruncateTheIngredientScan() {
        Meal meal = MealApi.parseMeal(LOOKUP_OK);

        // Chicken at slot 1; nulls/blank slots 2-4; Peas and Water at 5-6.
        // The old parser stopped at the first empty slot (losing Peas
        // and Water) and threw NPEs on null slots.
        assertEquals(java.util.Arrays.asList("Chicken", "Peas", "Water"),
                MealApi.ingredientNames(meal));
    }

    @Test
    public void missingMealsArrayAndGarbageYieldNullMeal() {
        assertNull(MealApi.parseMeal("{\"meals\": null}"));
        assertNull(MealApi.parseMeal("{\"meals\": []}"));
        assertNull(MealApi.parseMeal(""));
        assertNull(MealApi.parseMeal("not json at all"));
    }

    @Test
    public void mealWithoutUsableNameIsRejectedRatherThanStored() {
        assertNull(MealApi.parseMeal(
                "{\"meals\":[{\"strMeal\":\"  \",\"strInstructions\":\"x\"}]}"));
    }

    @Test
    public void instructionsAreSentenceSplitAndNullSafe() {
        Meal meal = MealApi.parseMeal(LOOKUP_OK);

        assertEquals("Step one@ Step two@", MealApi.instructionsFrom(meal));

        Meal blank = MealApi.parseMeal(
                "{\"meals\":[{\"strMeal\":\"No Steps\"}]}");
        assertEquals("", MealApi.instructionsFrom(blank));
        assertEquals("", MealApi.imageFrom(blank));
    }

    @Test
    public void ingredientNamesAreTrimmedOfSurroundingWhitespace() {
        Meal meal = MealApi.parseMeal("{\"meals\":[{\"strMeal\":\"Soup\","
                + "\"strIngredient1\":\"  Lentils \",\"strIngredient2\":null,"
                + "\"strIngredient3\":\"Tomato\"}]}");
        List<String> names = MealApi.ingredientNames(meal);

        assertEquals(java.util.Arrays.asList("Lentils", "Tomato"), names);
    }

    @Test
    public void sparseMealWithOnlyLateIngredientStillImports() {
        Meal meal = MealApi.parseMeal("{\"meals\":[{\"strMeal\":\"Rare\","
                + "\"strIngredient1\":\"\",\"strIngredient2\":\"\","
                + "\"strIngredient15\":\"Saffron\"}]}");

        assertTrue(MealApi.ingredientNames(meal).contains("Saffron"));
    }
}
