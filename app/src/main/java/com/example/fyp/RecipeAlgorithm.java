package com.example.fyp;

import android.util.Log;

import com.example.fyp.Database.RoomDB;
import com.example.fyp.Models.ExpiryDate;
import com.example.fyp.Models.Recipe;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RecipeAlgorithm {

    private RoomDB database;

    public List<Recipe> filterRecipeByScore(List<Recipe> recipes, List<ExpiryDate> expiryDates){
        int dateDiff;

        for (int i = 0; i < recipes.size(); i++) {
            dateDiff = Period.between(LocalDate.now(), LocalDate.parse(expiryDates.get(0).getExpiryDate(), DateTimeFormatter.ofPattern("d-M-yyyy"))).getDays();
            Log.d("datediff", "filterRecipeByScore: " + dateDiff);
        }
        return null;
    }

    public List<Recipe> filterRecipeByName(List<Recipe> recipes){
        List<Recipe> sortedRecipes = new ArrayList<>(recipes);
        sortedRecipes.sort(Comparator.comparing(Recipe::getName));
        return sortedRecipes;
    }
}
