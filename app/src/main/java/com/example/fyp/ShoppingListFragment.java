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
        // Capture the item's stable identity (its ID is the ingredient
        // name) instead of relying on a position that may no longer be
        // valid when the date dialog completes.
        final String itemId = shoppingList.get(position).getID();
        final Calendar cal = Calendar.getInstance();
        int mDate = cal.get(Calendar.DATE);
        int mMonth = cal.get(Calendar.MONTH);
        int mYear = cal.get(Calendar.YEAR);
        DatePickerDialog datePickerDialog = new DatePickerDialog(this.getContext(), android.R.style.Theme_DeviceDefault_Dialog, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int date) {
                ShoppingList item = database.mainDao().getShoppingListItem(itemId);
                if (item == null) {
                    // Item disappeared while the dialog was open.
                    refresh();
                    return;
                }
                String name = IngredientNames.normalize(item.getIngredient().getName());
                dateFormatted = date + "-" + (month + 1) + "-" + year;
                ExpiryDate expiryDate = new ExpiryDate(dateFormatted, new Ingredients(name));
                // Move the purchased unit into inventory with its expiry
                // record, then decrement or remove the shopping entry.
                if (database.mainDao().checkIfInventoryItemExists(name).size() > 0) {
                    database.mainDao().updateInventoryListAmount(1, name);
                    database.mainDao().insertExpiryDate(expiryDate);
                } else {
                    database.mainDao().insertInventory(new Inventory(new Ingredients(name), 1));
                    database.mainDao().insertExpiryDate(expiryDate);
                }

                if (item.getAmount() > 1) {
                    database.mainDao().updateShoppingListAmount(-1, item.getID());
                } else {
                    database.mainDao().deleteShoppingListItem(item);
                }
                refresh();
            }
        }, mYear, mMonth, mDate);

        datePickerDialog.show();

    }

    public void addItem(String item) {
        String name = IngredientNames.normalize(item);
        if (name.isEmpty()) {
            makeToast("Enter an item");
            return;
        }
        if (database.mainDao().checkIfItemExists(name).size() > 0) {
            database.mainDao().updateShoppingListAmount(1, name);
            shoppingList.clear();
            shoppingList.addAll(database.mainDao().getAllShoppingListItems());
            recyclerView.setAdapter(adapter);
        } else {
            Ingredients temp = new Ingredients(name);
            database.mainDao().insertIngredient(temp);
            ShoppingList temp2 = new ShoppingList(name, new Ingredients(name), 1);
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