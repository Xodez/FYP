package com.example.fyp.Models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "inventory",  indices = {@Index(value = {"ingredient"}, unique = true)})
public class Inventory {
    @PrimaryKey
    @NonNull
    String ID = "";

    @ColumnInfo(name = "ingredient")
    Ingredients ingredient;

    @ColumnInfo(name = "amount")
    int amount = 0;

    @ColumnInfo(name = "expiry_date")
    String expiryDate = "";

    @NonNull
    public String getID() {
        return ID;
    }

    public void setID(@NonNull String ID) {
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

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }
}
