package com.example.fyp;

import static android.content.ContentValues.TAG;

import android.app.DatePickerDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.fyp.Database.RoomDB;
import com.example.fyp.Models.ExpiryDate;
import com.example.fyp.Models.Ingredients;
import com.example.fyp.Models.Inventory;
import com.example.fyp.Models.ShoppingList;
import com.example.fyp.adapters.RecyclerViewAdapterShoppingList;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ShoppingListFragment extends Fragment implements RecyclerViewAdapterShoppingList.OnShoppingItemListener {

    private List<ShoppingList> shoppingList = new ArrayList<>();
    private RecyclerView recyclerView;
    private RecyclerViewAdapterShoppingList adapter;
    private RoomDB database;
    private String dateFormatted;
    private List<String> ingredients;
    private ArrayAdapter<String> stringAdapter;
    private AutoCompleteTextView input;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_shopping_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        database = RoomDB.getInstance(this.getContext());
        shoppingList = database.mainDao().getAllShoppingListItems();
        ingredients = new ArrayList<>();
        for (String i : database.mainDao().getAllIngredientNames()) {
            ingredients.add(i.toLowerCase());
        }

        input = view.findViewById(R.id.input);
        ImageView enter = view.findViewById(R.id.add);
        ImageView remove = view.findViewById(R.id.remove);

        initRecyclerView();

        enter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = input.getText().toString();
                if (text.length() == 0) {
                    makeToast("Enter an item");
                } else {
                    addItem(text);
                    input.setText("");
                }
            }
        });
    }

    public void refresh() {
        shoppingList.clear();
        shoppingList.addAll(database.mainDao().getAllShoppingListItems());
        recyclerView.setAdapter(adapter);
    }

    private void initRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this.getContext(), LinearLayoutManager.VERTICAL, false);
        recyclerView = requireView().findViewById(R.id.verticalView);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new RecyclerViewAdapterShoppingList(shoppingList, this);
        recyclerView.setAdapter(adapter);
        stringAdapter = new ArrayAdapter<String>(this.getContext(), android.R.layout.simple_list_item_1, ingredients);
        input.setAdapter(stringAdapter);
    }

    @Override
    public void onShoppingItemClick(int position) {
        database.mainDao().updateShoppingListAmount(1, shoppingList.get(position).getIngredient().getName());
        shoppingList.clear();
        shoppingList.addAll(database.mainDao().getAllShoppingListItems());
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onRemoveButtonClick(int position) {
        final Calendar cal = Calendar.getInstance();
        int mDate = cal.get(Calendar.DATE);
        int mMonth = cal.get(Calendar.MONTH);
        int mYear = cal.get(Calendar.YEAR);
        DatePickerDialog datePickerDialog = new DatePickerDialog(this.getContext(), android.R.style.Theme_DeviceDefault_Dialog, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int date) {
                dateFormatted = date + "-" + (month + 1) + "-" + year;
                ExpiryDate expiryDate = new ExpiryDate(dateFormatted, shoppingList.get(position).getIngredient());
                if (database.mainDao().checkIfInventoryItemExists(shoppingList.get(position).getIngredient()).size() > 0) {
                    database.mainDao().updateInventoryListAmount(1, shoppingList.get(position).getIngredient());
                    database.mainDao().insertExpiryDate(expiryDate);
                } else {
                    Inventory temp = new Inventory(shoppingList.get(position).getIngredient(), 1);
                    database.mainDao().insertInventory(temp);
                    database.mainDao().insertExpiryDate(expiryDate);
                }

                if (shoppingList.get(position).getAmount() > 1) {
                    database.mainDao().updateShoppingListAmount(-1, shoppingList.get(position).getIngredient().getName());
                } else {
                    database.mainDao().deleteShoppingListItem(shoppingList.get(position));
                }
                shoppingList.clear();
                shoppingList.addAll(database.mainDao().getAllShoppingListItems());
                recyclerView.setAdapter(adapter);
            }
        }, mYear, mMonth, mDate);

        datePickerDialog.show();

    }

    public void addItem(String item) {
        if (database.mainDao().checkIfItemExists(item.toUpperCase()).size() > 0) {
            database.mainDao().updateShoppingListAmount(1, item.toUpperCase());
            shoppingList.clear();
            shoppingList.addAll(database.mainDao().getAllShoppingListItems());
            recyclerView.setAdapter(adapter);
        } else {
            Log.d(TAG, "addItem: " + database.mainDao().checkIfItemExists(item));
            Ingredients temp = new Ingredients(item.toUpperCase());
            database.mainDao().insertIngredient(temp);
            ShoppingList temp2 = new ShoppingList(temp.getName(), temp, 1);
            database.mainDao().insertShoppingList(temp2);
            shoppingList.clear();
            shoppingList.addAll(database.mainDao().getAllShoppingListItems());
            recyclerView.setAdapter(adapter);
            stringAdapter.clear();
            ingredients.clear();
            for (String i : database.mainDao().getAllIngredientNames()) {
                ingredients.add(i.toLowerCase());
            }
            stringAdapter.addAll(ingredients);
            stringAdapter.notifyDataSetChanged();
            makeToast("Added: " + item);
        }
    }

    Toast t;

    public void makeToast(String s) {
        if (t != null) t.cancel();
        t = Toast.makeText(requireContext().getApplicationContext(), s, Toast.LENGTH_SHORT);
        t.show();
    }
}