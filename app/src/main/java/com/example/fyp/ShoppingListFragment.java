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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.fyp.Database.RoomDB;
import com.example.fyp.Models.Ingredient;
import com.example.fyp.adapters.RecyclerViewAdapterShoppingList;

import java.util.ArrayList;
import java.util.List;

public class ShoppingListFragment extends Fragment implements RecyclerViewAdapterShoppingList.OnShoppingItemListener {

    List<Ingredient> ingredients = new ArrayList<>();
    ArrayList<Integer> count;
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
        ingredients = database.mainDao().getAll();

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
                    makeToast("Added: " + text);
                }
            }
        });
    }

    private void createItems() {
        count = new ArrayList<>();

        initRecyclerView();
    }


    private void initRecyclerView() {
        layoutManager = new LinearLayoutManager(this.getContext(), LinearLayoutManager.VERTICAL, false);
        recyclerView = getView().findViewById(R.id.verticalView);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new RecyclerViewAdapterShoppingList(ingredients, count, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onShoppingItemClick(int position) {
        count.set(position, (count.get(position) + 1));
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onRemoveButtonClick(int position) {
        removeItem(position);
    }

    public void addItem(String item) {
//        if (ingredients.contains(item)){
//            makeToast("Item already in list");
//        } TODO
        database.mainDao().insert(new Ingredient(item));
        ingredients = database.mainDao().getAll();
        count.add(1);
        recyclerView.setAdapter(adapter);

    }

    public void removeItem(int remove) {
        if (count.get(remove) > 1) {
            count.set(remove, (count.get(remove) - 1));
            recyclerView.setAdapter(adapter);
        } else {
            database.mainDao().delete(ingredients.get(remove));
            ingredients.remove(remove);
            count.remove(remove);
            recyclerView.setAdapter(adapter);
        }
    }

    Toast t;

    public void makeToast(String s) {
        if (t != null) t.cancel();
        t = Toast.makeText(getContext().getApplicationContext(), s, Toast.LENGTH_SHORT);
        t.show();
    }
}