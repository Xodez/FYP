package com.example.fyp;

import android.content.Context;
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
import android.widget.Spinner;

import com.example.fyp.adapters.RecyclerViewAdapterRecipes;

import java.util.ArrayList;

public class RecipeFragment extends Fragment implements RecyclerViewAdapterRecipes.OnRecipeItemListener {

    private ArrayList<Integer> nImages;
    private ArrayList<String> nNames;
    private Context mContext;
    private Spinner spinner;
    private static final String[] paths = {"By recommended", "By time to make", "By Meal"};

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_recipe, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        spinner = (Spinner) view.findViewById((R.id.filterSpinner));
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_spinner_item, paths);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        getImages();
    }

    private void getImages() {
        nImages = new ArrayList<>();
        nNames = new ArrayList<>();
        nImages.add(R.drawable.burger);
        nNames.add("Burger");
        nImages.add(R.drawable.spaghetti);
        nNames.add("spaghetti");
        nImages.add(R.drawable.lasagne);
        nNames.add("lasagne");

        initRecyclerView();
    }

    private void initRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this.getContext(), LinearLayoutManager.HORIZONTAL, false);
        RecyclerView recyclerView = getView().findViewById(R.id.horizontalView);
        recyclerView.setLayoutManager(layoutManager);
        RecyclerViewAdapterRecipes adapter = new RecyclerViewAdapterRecipes(nNames, nImages, this.getContext(), this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onRecipeClick(int position) {

    }
}