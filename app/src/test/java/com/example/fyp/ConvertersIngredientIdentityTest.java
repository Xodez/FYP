package com.example.fyp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.example.fyp.Models.Ingredients;

import org.junit.Test;

/**
 * Ingredient persistence identity rules introduced with schema 6:
 * embedded ingredient columns store plain normalised names, and legacy
 * Gson-object values remain readable so existing inventories keep
 * matching without user action.
 */
public class ConvertersIngredientIdentityTest {

    private final Converters converters = new Converters();

    @Test
    public void ingredientIsStoredAsPlainNormalisedName() {
        Ingredients ingredient = new Ingredients("  tomato ");
        ingredient.setID(42); // a drifted row id must not leak into storage

        assertEquals("TOMATO", converters.fromIngredientToString(ingredient));
    }

    @Test
    public void plainNameReadsBackAsCanonicalIngredient() {
        Ingredients ingredient = converters.fromStringToIngredient("MILK");

        assertEquals("MILK", ingredient.getName());
        // Deserialised ingredients must not carry a table ID used for joins.
        assertEquals(0, ingredient.getID());
    }

    @Test
    public void legacyGsonObjectValueStillYieldsItsName() {
        Ingredients ingredient = converters.fromStringToIngredient(
                "{\"ID\":17,\"name\":\"Chicken Breast\"}");

        assertEquals("CHICKEN BREAST", ingredient.getName());
    }

    @Test
    public void legacyValueWithoutIdFieldIsStillReadable() {
        assertEquals("RICE", converters.fromStringToIngredient(
                "{\"name\":\"rice\"}").getName());
    }

    @Test
    public void corruptLegacyValueFallsBackToRawText() {
        assertEquals("{BROKEN", Converters.nameFromStored(" {BROKEN "));
    }

    @Test
    public void nullValuesRemainNull() {
        assertNull(converters.fromIngredientToString(null));
        assertNull(converters.fromStringToIngredient(null));
        assertNull(Converters.nameFromStored(null));
    }

    @Test
    public void nameFromStoredNormalisesEveryShape() {
        assertEquals("TOFU", Converters.nameFromStored("tofu"));
        assertEquals("TOFU", Converters.nameFromStored(
                "{\"ID\":0,\"name\":\" Tofu \"}"));
    }
}
