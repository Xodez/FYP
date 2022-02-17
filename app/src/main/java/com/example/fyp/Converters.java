package com.example.fyp;

import androidx.room.TypeConverter;

import com.example.fyp.Models.Ingredients;
import com.google.gson.Gson;

public class Converters {
    @TypeConverter
    public String fromIngredientToString(Ingredients ingredients){
        return new Gson().toJson(ingredients);
    }

    @TypeConverter
    public Ingredients fromStringToIngredient(String stringIngredient){
        return new Gson().fromJson(stringIngredient, Ingredients.class);
    }
}
