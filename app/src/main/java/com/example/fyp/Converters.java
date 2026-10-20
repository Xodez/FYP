package com.example.fyp;

import androidx.room.TypeConverter;

import com.example.fyp.Models.Ingredients;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class Converters {

    /**
     * Ingredients are persisted in embedded columns as their normalised
     * name only. Room relations and lookups join on these columns, so
     * identity no longer depends on accidentally identical Gson strings
     * carrying a mutable auto-generated ID.
     */
    @TypeConverter
    public String fromIngredientToString(Ingredients ingredients) {
        if (ingredients == null) {
            return null;
        }
        return IngredientNames.normalize(ingredients.getName());
    }

    @TypeConverter
    public Ingredients fromStringToIngredient(String stringIngredient) {
        String name = nameFromStored(stringIngredient);
        if (name == null) {
            return null;
        }
        return new Ingredients(name);
    }

    /**
     * Extracts the ingredient name from a persisted value, accepting both
     * the current format (a plain normalised name) and the legacy format
     * (a Gson-serialised Ingredients object such as {"ID":3,"name":"MILK"}).
     * Used by the converters and the 5-to-6 schema migration.
     */
    public static String nameFromStored(String stored) {
        if (stored == null) {
            return null;
        }
        String trimmed = stored.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if (trimmed.startsWith("{")) {
            try {
                JsonObject object = JsonParser.parseString(trimmed).getAsJsonObject();
                if (object.has("name") && !object.get("name").isJsonNull()) {
                    return IngredientNames.normalize(object.get("name").getAsString());
                }
            } catch (JsonParseException | IllegalStateException ignored) {
                // Corrupt legacy value: fall back to the raw text below.
            }
        }
        return IngredientNames.normalize(trimmed);
    }

    @TypeConverter
    public static ArrayList<String> fromString(String value) {
        Type listType = new TypeToken<ArrayList<String>>() {}.getType();
        return new Gson().fromJson(value, listType);
    }

    @TypeConverter
    public static String fromArrayList(ArrayList<String> list) {
        Gson gson = new Gson();
        String json = gson.toJson(list);
        return json;
    }
}
