package com.example.fyp;

import com.example.fyp.Models.Meal;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * TheMealDB access layer.
 *
 * Parsing helpers are deliberately pure functions over response bodies so
 * every malformed/unexpected API shape can be unit-tested offline; a null
 * return or null element signals an invalid response and an empty list
 * signals a valid response with no results.
 *
 * Quantity limitation: the free MealDB API mixes measure strings ("200g",
 * "2 cups", "to taste") that do not map onto this app's discrete unit
 * inventory, so imported recipes require exactly one unit per ingredient
 * instead of inventing counts or discarding information silently.
 */
public final class MealApi {

    private static final String BASE_URL =
            "https://www.themealdb.com/api/json/v1/1/";

    private MealApi() {
    }

    /**
     * Fetches a URL, returning the response body for 2xx responses or null
     * for connection failures, timeouts, non-success statuses, or empty
     * bodies. Never throws.
     */
    public static String get(String path) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(BASE_URL + path);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            int status = connection.getResponseCode();
            if (status < 200 || status > 299) {
                return null;
            }
            StringBuilder body = new StringBuilder();
            InputStream in = connection.getInputStream();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }
            } finally {
                in.close();
            }
            return body.length() == 0 ? null : body.toString();
        } catch (IOException e) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Meal ids from a filter.php ingredient search response. Returns null
     * for an invalid/unparseable response and an empty list when the API
     * answered {"meals": null} (no matches).
     */
    public static List<String> parseMealIds(String body) {
        JsonObject root = parseObject(body);
        if (root == null || !root.has("meals")) {
            return null;
        }
        JsonElement meals = root.get("meals");
        if (meals.isJsonNull()) {
            return new ArrayList<>();
        }
        if (!meals.isJsonArray()) {
            return null;
        }
        List<String> ids = new ArrayList<>();
        for (JsonElement meal : meals.getAsJsonArray()) {
            if (!meal.isJsonObject()) {
                continue;
            }
            JsonObject object = meal.getAsJsonObject();
            if (object.has("idMeal") && !object.get("idMeal").isJsonNull()) {
                ids.add(object.get("idMeal").getAsString());
            }
        }
        return ids;
    }

    /**
     * Full meal from a lookup.php response, or null when the response is
     * invalid, empty, or missing the meal name/instructions needed to
     * store a usable recipe.
     */
    public static Meal parseMeal(String body) {
        JsonObject root = parseObject(body);
        if (root == null || !root.has("meals") || root.get("meals").isJsonNull()
                || !root.get("meals").isJsonArray()
                || root.get("meals").getAsJsonArray().size() == 0) {
            return null;
        }
        JsonElement first = root.get("meals").getAsJsonArray().get(0);
        if (!first.isJsonObject()) {
            return null;
        }
        try {
            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation().create();
            Meal meal = gson.fromJson(first.toString(), Meal.class);
            if (meal == null || meal.getStrMeal() == null
                    || meal.getStrMeal().trim().isEmpty()) {
                return null;
            }
            return meal;
        } catch (JsonParseException | IllegalStateException e) {
            return null;
        }
    }

    /**
     * Ingredient names of a meal. Empty or missing slots are skipped
     * without truncating the scan of the remaining fields.
     */
    public static List<String> ingredientNames(Meal meal) {
        List<String> names = new ArrayList<>();
        for (String name : meal.getAllIngredients()) {
            if (name == null || name.trim().isEmpty()) {
                continue;
            }
            names.add(name.trim());
        }
        return names;
    }

    /**
     * Instructions as the "@"-joined step list the app persists. Null-safe;
     * follows the existing sentence-splitting convention.
     */
    public static String instructionsFrom(Meal meal) {
        String raw = meal.getStrInstructions();
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("\n", "").replaceAll("\r", "")
                .replaceAll("\\.", "@");
    }

    /** Thumbnail URL, empty string when absent (images are optional). */
    public static String imageFrom(Meal meal) {
        return meal.getStrMealThumb() == null ? "" : meal.getStrMealThumb();
    }

    private static JsonObject parseObject(String body) {
        if (body == null || body.trim().isEmpty()) {
            return null;
        }
        try {
            JsonElement element = JsonParser.parseString(body);
            if (element.isJsonObject()) {
                return element.getAsJsonObject();
            }
        } catch (JsonParseException | IllegalStateException ignored) {
            // Fall through: treated as an invalid response.
        }
        return null;
    }
}
