package com.example.fyp;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.fyp.Database.RoomDB;
import com.example.fyp.Models.Ingredients;
import com.example.fyp.Models.Recipe;
import com.example.fyp.Models.RecipeWithIngredients;
import com.example.fyp.Models.ShoppingList;
import com.example.fyp.adapters.RecyclerViewAdapterGeneric;
import com.example.fyp.adapters.RecyclerViewAdapterInstruction;
import com.example.fyp.adapters.RecyclerViewAdapterRecipes;
import com.example.fyp.adapters.RecyclerViewAdapterShoppingList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;

public class RecipeFragment extends Fragment implements RecyclerViewAdapterRecipes.OnRecipeItemListener, RecyclerViewAdapterGeneric.OnIngredientItemListener, RecyclerViewAdapterInstruction.OnItemListener {

    public static final int WRITE_EXTERNAL_STORAGE_CODE = 101;
    private RecyclerView recyclerView;
    private RecyclerView recyclerView2;
    private static final String[] paths = {"By recommended", "By time to make", "By Meal"};
    private ImageView recipeImage;
    private EditText enterIngredient;
    private EditText enterInstruction;
    private EditText enterName;
    private RecyclerViewAdapterGeneric adapter;
    private RecyclerViewAdapterInstruction adapter2;
    private ArrayList<String> ingredientList;
    private ArrayList<Integer> count;
    private ArrayList<String> instructionList;
    private OutputStream outputStream;
    private List<Recipe> recipes;
    private RoomDB database;
    private Dialog dialog;
    private RecyclerViewAdapterRecipes recipeAdapter;
    private RecyclerView recipeRecyclerView;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_recipe, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        database = RoomDB.getInstance(this.getContext());
        recipes = database.mainDao().getAllRecipes();
        Spinner spinner = (Spinner) view.findViewById((R.id.filterSpinner));
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_spinner_item, paths);

        Button addNewRecipe = getView().findViewById(R.id.addRecipe);
        addNewRecipe.setOnClickListener(view1 -> showDialog());

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        initRecyclerView();
    }

    private void showDialog() {
        dialog = new Dialog(this.getContext(), R.style.Theme_FYP);
        dialog.setContentView(R.layout.layout_newrecipeitem);

        ingredientList = new ArrayList<>();
        count = new ArrayList<>();
        instructionList = new ArrayList<>();

        Button uploadImage = dialog.findViewById(R.id.uploadImage);
        Button done = dialog.findViewById(R.id.done);
        Button cancel = dialog.findViewById(R.id.cancel);
        enterName = dialog.findViewById(R.id.enterName);
        enterIngredient = dialog.findViewById(R.id.enterIngredient);
        enterInstruction = dialog.findViewById(R.id.enterInstruction);
        ImageView addIngredient = dialog.findViewById(R.id.addIngredient);
        ImageView addInstruction = dialog.findViewById(R.id.addInstruction);
        recipeImage = dialog.findViewById(R.id.recipeImage);

        LinearLayoutManager layoutManager = new LinearLayoutManager(dialog.getContext(), LinearLayoutManager.VERTICAL, false);
        recyclerView = dialog.findViewById(R.id.ingredientList);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new RecyclerViewAdapterGeneric(ingredientList, count, this);
        recyclerView.setAdapter(adapter);

        LinearLayoutManager layoutManager2 = new LinearLayoutManager(dialog.getContext(), LinearLayoutManager.VERTICAL, false);
        recyclerView2 = dialog.findViewById(R.id.instructionList);
        recyclerView2.setLayoutManager(layoutManager2);
        adapter2 = new RecyclerViewAdapterInstruction(instructionList, this);
        recyclerView2.setAdapter(adapter2);

        addIngredient.setOnClickListener(view -> addIngredient());
        uploadImage.setOnClickListener(view -> openGallery());
        addInstruction.setOnClickListener(view -> addInstruction());
        done.setOnClickListener(view -> askWritePermission());
        cancel.setOnClickListener(view -> dialog.cancel());

        dialog.show();
    }

    private void addRecipe() {
//        BitmapDrawable imageToSave = (BitmapDrawable) recipeImage.getDrawable();
        Bitmap bitmap = ((BitmapDrawable) recipeImage.getDrawable()).getBitmap();

        String dir = requireContext().getExternalFilesDir(null).getAbsolutePath() +"/LessOfIt/Images/";
        File parentFiles = new File(dir);
        if(!parentFiles.exists()){
            parentFiles.mkdirs();
        };
        String recipeImageName = enterName.getText().toString() + System.currentTimeMillis() + ".jpg";
        File file = new File(dir, recipeImageName);
        try {
            outputStream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream);

        StringBuilder instructions = new StringBuilder();
        for (int i = 0; i < instructionList.size(); i++){
            if (i != instructionList.size() - 1){
                instructions.append(instructionList.get(i)).append("@");
            }else{
                instructions.append(instructionList.get(i));
            }

        }
        Recipe temp = new Recipe(enterName.getText().toString(), instructions.toString(), file.getAbsolutePath());

        database.mainDao().insertRecipe(temp);
        recipes.clear();
        recipes.addAll(database.mainDao().getAllRecipes());
        recipeRecyclerView.setAdapter(recipeAdapter);
        dialog.dismiss();
    }

    private void addInstruction() {
        String text = enterInstruction.getText().toString();
        if (text.length() == 0) {
            makeToast("Enter an item");
        } else {
            instructionList.add(text);
            recyclerView2.setAdapter(adapter2);
            enterInstruction.setText("");
        }
    }

    private void addIngredient() {
        String text = enterIngredient.getText().toString();
        if (text.length() == 0) {
            makeToast("Enter an item");
        } else {
            if (ingredientList.contains(text.toUpperCase())) {
                int pos = ingredientList.indexOf(text.toUpperCase());
                count.set(pos, count.get(pos) + 1);
                recyclerView.setAdapter(adapter);
            } else {
                ingredientList.add(text.toUpperCase());
                count.add(1);
                recyclerView.setAdapter(adapter);
                makeToast("Added: " + text);
            }

            enterIngredient.setText("");
        }
    }

    private void openGallery() {
        Intent pickImage = new Intent(Intent.ACTION_PICK);
        pickImage.setType("image/*");

        galleryActivityResultLauncher.launch(pickImage);
    }

    private final ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        Uri imageUri = data.getData();

                        recipeImage.setImageURI(imageUri);
                    } else {
                        Toast.makeText(getContext(), "Cancelled", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private void initRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this.getContext(), LinearLayoutManager.HORIZONTAL, false);
        recipeRecyclerView = requireView().findViewById(R.id.horizontalView);
        recipeRecyclerView.setLayoutManager(layoutManager);
        recipeAdapter = new RecyclerViewAdapterRecipes(recipes,this);
        recipeRecyclerView.setAdapter(recipeAdapter);
    }

    @Override
    public void onRecipeClick(int position) {

    }

    private void askWritePermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE_CODE);
        } else {
            addRecipe();
        }
    }

    @Override
    public void onIngredientItemClick(int position) {
        count.set(position, count.get(position) + 1);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onRemoveButtonClick(int position) {
        if (count.get(position) > 1) {
            count.set(position, count.get(position) - 1);
            recyclerView.setAdapter(adapter);
        } else {
            ingredientList.remove(ingredientList.get(position));
            count.remove(count.get(position));
            recyclerView.setAdapter(adapter);
        }
    }

    Toast t;

    public void makeToast(String s) {
        if (t != null) t.cancel();
        t = Toast.makeText(requireContext().getApplicationContext(), s, Toast.LENGTH_SHORT);
        t.show();
    }

    @Override
    public void onRemoveInstructionButtonClick(int position) {
        instructionList.remove(instructionList.get(position));
        recyclerView2.setAdapter(adapter2);
    }
}