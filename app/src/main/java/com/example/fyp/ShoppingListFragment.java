package com.example.fyp;

import static android.content.ContentValues.TAG;

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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.fyp.Database.RoomDB;
import com.example.fyp.Models.Ingredients;
import com.example.fyp.Models.ShoppingList;
import com.example.fyp.adapters.RecyclerViewAdapterShoppingList;

import java.util.ArrayList;
import java.util.List;

public class ShoppingListFragment extends Fragment implements RecyclerViewAdapterShoppingList.OnShoppingItemListener {

    List<ShoppingList> shoppingList = new ArrayList<>();
    RecyclerView recyclerView;
    RecyclerViewAdapterShoppingList adapter;
    LinearLayoutManager layoutManager;
    RoomDB database;

    EditText input;
    ImageView enter;
    ImageView remove;

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

        createItems();
        input = view.findViewById(R.id.input);
        enter = view.findViewById(R.id.add);
        remove = view.findViewById(R.id.remove);

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

    private void createItems() {

        initRecyclerView();
    }


    private void initRecyclerView() {
        layoutManager = new LinearLayoutManager(this.getContext(), LinearLayoutManager.VERTICAL, false);
        recyclerView = getView().findViewById(R.id.verticalView);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new RecyclerViewAdapterShoppingList(shoppingList, this);
        recyclerView.setAdapter(adapter);
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
        removeItem(position);
    }

    public void addItem(String item) {
        if (database.mainDao().checkIfItemExists(item.toUpperCase()).size() > 0){
            makeToast("Item already in list");
        }
        else {
            Log.d(TAG, "addItem: " + database.mainDao().checkIfItemExists(item));
            Ingredients temp = new Ingredients(item.toUpperCase());
            database.mainDao().insertIngredient(temp);
            ShoppingList temp2 = new ShoppingList(temp.getName(), temp, 1);
            database.mainDao().insertShoppingList(temp2);
            shoppingList.clear();
            shoppingList.addAll(database.mainDao().getAllShoppingListItems());
            recyclerView.setAdapter(adapter);
            makeToast("Added: " + item);
        }
    }

    public void removeItem(int remove) {
        if (shoppingList.get(remove).getAmount() > 1) {
            database.mainDao().updateShoppingListAmount(-1, shoppingList.get(remove).getIngredient().getName());
        } else {
            database.mainDao().deleteShoppingListItem(shoppingList.get(remove));
        }
        shoppingList.clear();
        shoppingList.addAll(database.mainDao().getAllShoppingListItems());
        recyclerView.setAdapter(adapter);
    }

    Toast t;

    public void makeToast(String s) {
        if (t != null) t.cancel();
        t = Toast.makeText(getContext().getApplicationContext(), s, Toast.LENGTH_SHORT);
        t.show();
    }
}