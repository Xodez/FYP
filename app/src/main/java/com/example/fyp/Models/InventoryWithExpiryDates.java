package com.example.fyp.Models;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

public class InventoryWithExpiryDates {
    @Embedded
    public Inventory inventory;
    @Relation(
            parentColumn = "ingredient",
            entityColumn = "inventory_ingredient"
    )
    public List<ExpiryDate> expiryDates;

    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public List<ExpiryDate> getExpiryDates() {
        return expiryDates;
    }

    public void setExpiryDates(List<ExpiryDate> expiryDates) {
        this.expiryDates = expiryDates;
    }
}
