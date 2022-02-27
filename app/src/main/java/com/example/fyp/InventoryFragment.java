package com.example.fyp;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.example.fyp.Database.RoomDB;
import com.example.fyp.Models.ExpiryDate;
import com.example.fyp.Models.Ingredients;
import com.example.fyp.Models.Inventory;
import com.example.fyp.Models.InventoryWithExpiryDates;
import com.example.fyp.adapters.RecyclerViewAdapterInventory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class InventoryFragment extends Fragment implements RecyclerViewAdapterInventory.OnInventoryItemListener {

    List<InventoryWithExpiryDates> inventories = new ArrayList<>();
    RecyclerView recyclerView;
    RecyclerViewAdapterInventory adapter;
    LinearLayoutManager layoutManager;
    RoomDB database;
    Button input;
    int mDate, mMonth, mYear;
    String dateFormatted;

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
        input = view.findViewById(R.id.input);

        input.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog();
            }
        });

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
    public void onRemoveButtonClick(int position) {
        if (inventories.get(position).getInventory().getAmount() > 1) {
            database.mainDao().updateInventoryListAmount(-1, inventories.get(position).getInventory().getIngredient());
            database.mainDao().deleteExpiryDate(database.mainDao().getExpiryDatesOrdered(inventories.get(position).getInventory().getIngredient()).get(0));
        } else {
            database.mainDao().deleteInventoryItem(inventories.get(position).getInventory());
        }
        inventories.clear();
        inventories.addAll(database.mainDao().getInventoriesWithExpiryDates());
        recyclerView.setAdapter(adapter);
    }

    public void showDialog() {
        Dialog dialog = new Dialog(this.getContext());
        dialog.setContentView(R.layout.layout_newinventoryitem);

        Button closeDialogue = dialog.findViewById(R.id.buttonOk);
        EditText ingredientName = dialog.findViewById(R.id.ingredientName);
        TextView dateText = dialog.findViewById(R.id.date);
        ImageView calendar = dialog.findViewById(R.id.calendar);
        NumberPicker numberPicker = dialog.findViewById(R.id.numberPicker);
        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(10);
        final Calendar cal = Calendar.getInstance();
        mDate = cal.get(Calendar.DATE);
        mMonth = cal.get(Calendar.MONTH);
        mYear = cal.get(Calendar.YEAR);

        closeDialogue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Ingredients ingredient = new Ingredients(ingredientName.getText().toString().toUpperCase());
                Inventory inventory = new Inventory(ingredient, numberPicker.getValue());
                ExpiryDate expiryDate = new ExpiryDate(dateText.getText().toString(), ingredient);
                for (int i = 0; i < numberPicker.getValue(); i++){
                    database.mainDao().insertExpiryDate(expiryDate);
                }
                if (database.mainDao().checkIfInventoryItemExists(ingredient).size() > 0){
                    database.mainDao().updateInventoryListAmount(numberPicker.getValue(), ingredient);
                }
                else{
                    database.mainDao().insertInventory(inventory);
                }
                inventories.clear();
                inventories.addAll(database.mainDao().getInventoriesWithExpiryDates());
                recyclerView.setAdapter(adapter);
                dialog.dismiss();
            }
        });

        calendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatePickerDialog datePickerDialog = new DatePickerDialog(dialog.getContext(), android.R.style.Theme_DeviceDefault_Dialog, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int month, int date) {
                        dateFormatted = date + "-" + month + "-" + year;
                        dateText.setText(dateFormatted);
                    }
                }, mYear, mMonth, mDate);
                datePickerDialog.show();
            }
        });
        dialog.show();
    }
}