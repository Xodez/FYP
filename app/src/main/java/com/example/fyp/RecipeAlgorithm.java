package com.example.fyp;

import com.example.fyp.Models.ExpiryDate;
import com.example.fyp.Models.InventoryWithExpiryDates;
import com.example.fyp.Models.Recipe;
import com.example.fyp.Models.RecipeIngredients;
import com.example.fyp.Models.RecipeWithIngredients;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic recipe ranking against the user's inventory.
 *
 * Ranking rules (lower score = higher priority):
 *  - A recipe is makeable only when every required ingredient is owned in
 *    sufficient quantity. Makeable recipes are scored as the sum over
 *    ingredients of (urgencyDays / ingredientCount / ownedAmount), so
 *    recipes using soon-expiring items and exact stock are prioritised.
 *  - Non-makeable recipes receive Float.POSITIVE_INFINITY and are still
 *    returned, after every makeable recipe, as shopping-required
 *    alternatives.
 *  - Ingredient names are matched after IngredientNames normalisation
 *    (trim + locale-independent upper case).
 *  - urgencyDays is the total calendar days until the earliest valid
 *    expiry date. Expired items clamp to 0 (maximum urgency) so past
 *    dates cannot produce negative or extreme scores; the score is a
 *    waste-reduction heuristic, not a food-safety assessment.
 *  - Ingredients with no parsable expiry date use
 *    UNKNOWN_EXPIRY_URGENCY_DAYS so a recipe relying on malformed or
 *    missing data cannot outrank an otherwise comparable recommendation.
 *  - Recipes without any ingredient requirements cannot be evaluated;
 *    they are excluded from the ranked results rather than promoted.
 *  - Ties keep input order (stable sort). No secondary tie-breaker is
 *    specified by the product, so none is invented here.
 */
public class RecipeAlgorithm {

    private static final DateTimeFormatter EXPIRY_FORMAT =
            DateTimeFormatter.ofPattern("d-M-yyyy");

    /**
     * Urgency weight for inventory items with missing or malformed expiry
     * data. It is far above realistic expiry horizons, which places such
     * items last among makeable recipes without fabricating a date.
     */
    static final float UNKNOWN_EXPIRY_URGENCY_DAYS = 100_000f;

    /** Owned quantity plus the expiry urgency of one inventory item. */
    static class Stock {
        final int amount;
        final float urgencyDays;

        Stock(int amount, float urgencyDays) {
            this.amount = amount;
            this.urgencyDays = urgencyDays;
        }
    }

    /** Derives amount and expiry urgency for one inventory entry. */
    static Stock stockOf(InventoryWithExpiryDates item) {
        int amount = item.getInventory() == null
                ? 0 : item.getInventory().getAmount();
        LocalDate soonest = null;
        List<ExpiryDate> expiryDates = item.expiryDates;
        if (expiryDates != null) {
            for (ExpiryDate expiryDate : expiryDates) {
                if (expiryDate == null || expiryDate.getExpiryDate() == null) {
                    continue;
                }
                try {
                    LocalDate parsed =
                            LocalDate.parse(expiryDate.getExpiryDate(), EXPIRY_FORMAT);
                    if (soonest == null || parsed.isBefore(soonest)) {
                        soonest = parsed;
                    }
                } catch (DateTimeParseException ignored) {
                    // Malformed persisted dates are skipped; if no valid
                    // date remains the item's urgency is "unknown".
                }
            }
        }
        float urgency;
        if (soonest == null) {
            urgency = UNKNOWN_EXPIRY_URGENCY_DAYS;
        } else {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), soonest);
            urgency = days < 0 ? 0f : (float) days;
        }
        return new Stock(amount, urgency);
    }

    public List<Recipe> filterRecipeByScore(List<RecipeWithIngredients> recipes,
                                            List<InventoryWithExpiryDates> exp) {
        HashMap<String, Stock> valuedIngredients = new HashMap<>();
        for (InventoryWithExpiryDates item : exp) {
            if (item == null || item.getInventory() == null
                    || item.getInventory().getIngredient() == null) {
                continue;
            }
            String name = IngredientNames.normalize(
                    item.getInventory().getIngredient().getName());
            valuedIngredients.put(name, stockOf(item));
        }

        LinkedHashMap<Recipe, Float> recipesWithScores = new LinkedHashMap<>();
        for (RecipeWithIngredients recipe : recipes) {
            List<RecipeIngredients> needed = recipe.getRecipeIngredients();
            if (needed == null || needed.isEmpty()) {
                // Requirements cannot be evaluated: exclude, never promote.
                continue;
            }
            float score = 0f;
            boolean makeable = true;
            for (RecipeIngredients ingredient : needed) {
                Stock stock = valuedIngredients.get(
                        IngredientNames.normalize(ingredient.getIngredients().getName()));
                if (stock == null || stock.amount <= 0
                        || stock.amount < ingredient.getCount()) {
                    makeable = false;
                    break;
                }
                score += stock.urgencyDays / needed.size() / stock.amount;
            }
            recipesWithScores.put(recipe.getRecipe(), makeable ? score : Float.POSITIVE_INFINITY);
        }

        List<Map.Entry<Recipe, Float>> sortedRecipes =
                new ArrayList<>(recipesWithScores.entrySet());
        sortedRecipes.sort(new Comparator<Map.Entry<Recipe, Float>>() {
            @Override
            public int compare(Map.Entry<Recipe, Float> one,
                               Map.Entry<Recipe, Float> two) {
                return Float.compare(one.getValue(), two.getValue());
            }
        });

        List<Recipe> recipesToReturn = new ArrayList<>();
        for (Map.Entry<Recipe, Float> entry : sortedRecipes) {
            recipesToReturn.add(entry.getKey());
        }
        return recipesToReturn;
    }

    public List<Recipe> filterRecipeByName(List<Recipe> recipes) {
        List<Recipe> sortedRecipes = new ArrayList<>(recipes);
        sortedRecipes.sort(Comparator.comparing(Recipe::getName));
        return sortedRecipes;
    }
}
