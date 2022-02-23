package com.example.fyp;

import androidx.room.TypeConverter;

import com.example.fyp.Models.Ingredients;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class Converters {
    @TypeConverter
    public String fromIngredientToString(Ingredients ingredients){
        return new Gson().toJson(ingredients);
    }

    @TypeConverter
    public Ingredients fromStringToIngredient(String stringIngredient){
        return new Gson().fromJson(stringIngredient, Ingredients.class);
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
