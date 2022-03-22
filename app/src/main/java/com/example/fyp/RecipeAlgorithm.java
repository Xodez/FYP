package com.example.fyp;

import android.util.Log;

import com.example.fyp.Database.RoomDB;
import com.example.fyp.Models.ExpiryDate;
import com.example.fyp.Models.Ingredients;
import com.example.fyp.Models.InventoryWithExpiryDates;
import com.example.fyp.Models.Recipe;
import com.example.fyp.Models.RecipeIngredients;
import com.example.fyp.Models.RecipeWithIngredients;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RecipeAlgorithm {

    private RoomDB database;

    public List<Recipe> filterRecipeByScore(List<RecipeWithIngredients> recipes, List<InventoryWithExpiryDates> exp) {
        ArrayList<LocalDate> tempExpiryDates = new ArrayList<>();
        HashMap<String, String[]> valuedIngredients = new HashMap<>();
        LinkedHashMap<Recipe, Integer> recipesWithScores = new LinkedHashMap<>();

        // Giving value to ingredients
        for (InventoryWithExpiryDates item : exp) {
            for (int i = 0; i < item.expiryDates.size(); i++) {
                tempExpiryDates.add(LocalDate.parse(item.expiryDates.get(i).getExpiryDate(), DateTimeFormatter.ofPattern("d-M-yyyy")));
            }
            Collections.sort(tempExpiryDates);
            valuedIngredients.put(item.getInventory().getIngredient().getName(), new String[]{String.valueOf(item.getInventory().getAmount()), String.valueOf(Period.between(LocalDate.now(), tempExpiryDates.get(0)).getDays())});
            tempExpiryDates.clear();
        }

        // Giving recipes scores
        Boolean valid = false;
        for (RecipeWithIngredients recipe : recipes) {
            for (RecipeIngredients ingredients : recipe.getRecipeIngredients()) {
                for (String ingredientName : valuedIngredients.keySet()) {
                    valid = false;
                    if (ingredientName.equals(ingredients.getIngredients().getName()) && Integer.parseInt(Objects.requireNonNull(valuedIngredients.get(ingredientName))[0]) >= ingredients.getCount()) {
                        valid = true;
                        if (!recipesWithScores.containsKey(recipe.getRecipe())){
                            recipesWithScores.put(recipe.getRecipe(), Integer.parseInt(Objects.requireNonNull(valuedIngredients.get(ingredientName))[1]));
                        }
                        else {
                            recipesWithScores.put(recipe.getRecipe(), recipesWithScores.get(recipe.getRecipe()) + Integer.parseInt(Objects.requireNonNull(valuedIngredients.get(ingredientName))[1]));
                        }
                        break;
                    }
                }
                if (valid){
                    continue;
                }
                recipesWithScores.put(recipe.getRecipe(), 99999999);
                break;
            }
        }

        // Sorting recipes
        List<Map.Entry<Recipe, Integer>> sortedRecipes = new ArrayList<>(recipesWithScores.entrySet());
        sortedRecipes.sort(new Comparator<Map.Entry<Recipe, Integer>>() {
            @Override
            public int compare(Map.Entry<Recipe, Integer> one, Map.Entry<Recipe, Integer> two) {
                return one.getValue() - two.getValue();
            }

            @Override
            public boolean equals(Object o) {
                return false;
            }
        });

        List<Recipe> recipesToReturn = new ArrayList<>();
        for (Map.Entry<Recipe, Integer> entry: sortedRecipes) {
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
