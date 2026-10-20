package com.example.fyp;

import java.util.Locale;

/**
 * Canonical form for ingredient names used across inventory, recipe,
 * expiry, and shopping-list lookups.
 *
 * Names arriving from TheMealDB or from free-text input can differ only
 * in capitalisation or surrounding whitespace, but they denote the same
 * ingredient. Normalisation is deliberately restricted to trimming and
 * locale-independent upper-casing: no fuzzy matching, synonym expansion,
 * or substitutions.
 */
public final class IngredientNames {

    private IngredientNames() {
    }

    /** Returns the canonical stored/matched form of an ingredient name. */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toUpperCase(Locale.ROOT);
    }
}
