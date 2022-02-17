package com.example.fyp.Models;


import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "shopping_list",  indices = {@Index(value = {"ingredient"}, unique = true)})
public class ShoppingList implements Serializable {
    @PrimaryKey(autoGenerate = true)
    int ID = 0;

    @ColumnInfo(name = "ingredient")
    Ingredients ingredient;

    @ColumnInfo(name = "amount")
    int amount = 0;

    public ShoppingList(Ingredients ingredient, int amount) {
        this.ingredient = ingredient;
        this.amount = amount;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
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
