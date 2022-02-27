package com.example.fyp;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.fyp.Database.RoomDB;
import com.example.fyp.Models.InventoryWithExpiryDates;
import com.example.fyp.adapters.RecyclerViewAdapterInventory;

import java.util.ArrayList;
import java.util.List;

public class InventoryFragment extends Fragment implements RecyclerViewAdapterInventory.OnInventoryItemListener {

    List<InventoryWithExpiryDates> inventories = new ArrayList<>();
    RecyclerView recyclerView;
    RecyclerViewAdapterInventory adapter;
    LinearLayoutManager layoutManager;
    RoomDB database;

    ImageView remove;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_inventory, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        database = RoomDB.getInstance(this.getContext());
        inventories = database.mainDao().getInventoriesWithExpiryDates();
        initRecyclerView();
    }

    private void initRecyclerView() {
        layoutManager = new LinearLayoutManager(this.getContext(), LinearLayoutManager.VERTICAL, false);
        recyclerView = requireView().findViewById(R.id.verticalView);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new RecyclerViewAdapterInventory(inventories, this);
        recyclerView.setAdapter(adapter);
    }

    public void refresh() {
        inventories.clear();
        inventories.addAll(database.mainDao().getInventoriesWithExpiryDates());
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onInventoryItemClick(int position) {

    }

    @Override
    public void onRemoveButtonClick(int position) {
//        if (inventories.get(position).getAmount() > 1) {
//            database.mainDao().updateInventoryListAmount(-1, shoppingList.get(position).getIngredient().getName());
//        } else {
//            database.mainDao().deleteShoppingListItem(shoppingList.get(position));
//        }
//        shoppingList.clear();
//        shoppingList.addAll(database.mainDao().getAllShoppingListItems());
//        recyclerView.setAdapter(adapter);
    }

}