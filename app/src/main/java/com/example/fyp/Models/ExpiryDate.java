package com.example.fyp.Models;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Relation;

import java.io.Serializable;
import java.util.List;

@Entity(tableName = "expiry_date")
public class ExpiryDate implements Serializable {
    @PrimaryKey(autoGenerate = true)
    int ID = 0;

    @ColumnInfo(name = "inventory_ingredient")
    Ingredients ingredients;

    @ColumnInfo(name = "expiry_date")
    String expiryDate;

    public ExpiryDate(String expiryDate, Ingredients ingredients) {
        this.expiryDate = expiryDate;
        this.ingredients = ingredients;
    }

    public Ingredients getIngredients() {
        return ingredients;
    }

    public void setIngredients(Ingredients ingredients) {
        this.ingredients = ingredients;
    }

    public int getID() {
        return ID;
    }

    public void setID(int eID) {
        this.ID = eID;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }
}

