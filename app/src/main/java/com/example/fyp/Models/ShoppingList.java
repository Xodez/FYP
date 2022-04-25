package com.example.fyp.Models;


import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "shopping_list",  indices = {@Index(value = {"ingredient"}, unique = true)})
public class ShoppingList implements Serializable {
    @PrimaryKey
    @NonNull
    String ID = "";

    @ColumnInfo(name = "ingredient")
    Ingredients ingredient;

    @ColumnInfo(name = "amount")
    int amount = 0;

    public ShoppingList(@NonNull String ID, Ingredients ingredient, int amount) {
        this.ID = ID;
        this.ingredient = ingredient;
        this.amount = amount;
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public Ingredients getIngredient() {
        return ingredient;
    }

    public void setIngredient(Ingredients ingredient) {
        this.ingredient = ingredient;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}
