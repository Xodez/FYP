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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.example.fyp.adapters.RecyclerViewAdapterShoppingList;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;

public class ShoppingListFragment extends Fragment implements RecyclerViewAdapterShoppingList.OnShoppingItemListener {

    ArrayList<String> items;
    ArrayList<Integer> count;
    RecyclerView recyclerView;

    EditText input;
    ImageView enter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_shopping_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        createItems();
        input = view.findViewById(R.id.input);
        enter = view.findViewById(R.id.add);
    }

    private void createItems(){
        items = new ArrayList<>();
        items.add("Apple");
        items.add("Fish");
        items.add("Salami");
        items.add("Peanuts");
        items.add("Chicken");
        items.add("Steak");
        items.add("Orange");

        count = new ArrayList<>();
        count.add(1);
        count.add(2);
        count.add(4);
        count.add(1);
        count.add(3);
        count.add(6);
        count.add(23);

        initRecyclerView();
    }


    private void initRecyclerView(){
        LinearLayoutManager layoutManager = new LinearLayoutManager(this.getContext(), LinearLayoutManager.VERTICAL, false);
        recyclerView = getView().findViewById(R.id.verticalView);
        recyclerView.setLayoutManager(layoutManager);
        RecyclerViewAdapterShoppingList adapter = new RecyclerViewAdapterShoppingList(items, count, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onShoppingItemCLick(int position) {
        count.set(position, (count.get(position) + 1));
        initRecyclerView();
    }

    Toast t;

    public void makeToast(String s){
        if (t != null) t.cancel();
        t = Toast.makeText(getContext().getApplicationContext(), s, Toast.LENGTH_SHORT);
        t.show();
    }
}