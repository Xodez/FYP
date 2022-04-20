package com.example.fyp;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fyp.Database.RoomDB;
import com.example.fyp.Models.Ingredients;
import com.example.fyp.Models.InventoryWithExpiryDates;
import com.example.fyp.Models.Meal;
import com.example.fyp.Models.Recipe;
import com.example.fyp.Models.RecipeIngredients;
import com.example.fyp.Models.RecipeWithIngredients;
import com.example.fyp.adapters.RecyclerViewAdapterGeneric;
import com.example.fyp.adapters.RecyclerViewAdapterGenericSelect;
import com.example.fyp.adapters.RecyclerViewAdapterInstruction;
import com.example.fyp.adapters.RecyclerViewAdapterInstructionSelect;
import com.example.fyp.adapters.RecyclerViewAdapterRecipes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RecipeFragment extends Fragment implements RecyclerViewAdapterRecipes.OnRecipeItemListener, RecyclerViewAdapterGeneric.OnIngredientItemListener, RecyclerViewAdapterInstruction.OnItemListener,
        RecyclerViewAdapterInstructionSelect.OnItemListener, RecyclerViewAdapterGenericSelect.OnIngredientItemListener {

    public static final int WRITE_EXTERNAL_STORAGE_CODE = 101;
    private RecyclerView recyclerView;
    private RecyclerView recyclerView2;
    private static final String[] paths = {"By Recommended", "By Name", "By Meal"};
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
    private int recipePosition;
    private RecipeWithIngredients recipeWithIngredients;
    private final List<RecipeWithIngredients> recipeWithIngredients2 = new ArrayList<>();
    private final RecipeAlgorithm ra = new RecipeAlgorithm();
    private final List<Recipe> filteredRecipes = new ArrayList<>();
    private final List<InventoryWithExpiryDates> expiryDates = new ArrayList<>();

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
        Button addNewRecipe = requireView().findViewById(R.id.addRecipe);
        Button deleteRecipe = requireView().findViewById(R.id.delete);
        Button editRecipe = requireView().findViewById(R.id.edit);
        Button selectRecipe = requireView().findViewById(R.id.select);
        Button addRecipe = requireView().findViewById(R.id.testAddRecipe);

        selectRecipe.setOnClickListener(view1 -> selectRecipe());
        addNewRecipe.setOnClickListener(view1 -> showDialog());
        deleteRecipe.setOnClickListener(view1 -> deleteRecipe());
        editRecipe.setOnClickListener(view1 -> editRecipe());
        addRecipe.setOnClickListener(view1 -> getRecipe());
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                String selectedItem = adapterView.getItemAtPosition(position).toString();
                if (selectedItem.equals("By Name")) {
                    filteredRecipes.addAll(ra.filterRecipeByName(recipes));
                    recipes.clear();
                    recipes.addAll(filteredRecipes);
                    filteredRecipes.clear();
                    recipeRecyclerView.setAdapter(recipeAdapter);
                } else if (selectedItem.equals("By Recommended")) {
                    expiryDates.clear();
                    recipeWithIngredients2.clear();
                    recipeWithIngredients2.addAll(database.mainDao().getRecipeWithIngredients());
                    expiryDates.addAll(database.mainDao().getInventoriesWithExpiryDates());
                    filteredRecipes.addAll(ra.filterRecipeByScore(recipeWithIngredients2, expiryDates));
                    recipes.clear();
                    recipes.addAll(filteredRecipes);
                    filteredRecipes.clear();
                    recipeRecyclerView.setAdapter(recipeAdapter);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        initRecyclerView();
    }


    private void selectRecipe() {
        if (recipePosition != -1) {
            dialog = new Dialog(this.getContext(), R.style.Theme_FYP);
            dialog.setContentView(R.layout.layout_recipe);

            recipeWithIngredients = database.mainDao().getRecipeWithIngredientsByName(recipes.get(recipePosition).getName()).get(0);
            ingredientList = new ArrayList<>();
            List<Ingredients> ingredientList2 = new ArrayList<>();
            count = new ArrayList<>();
            instructionList = new ArrayList<>();

            Collections.addAll(instructionList, recipeWithIngredients.getRecipe().getInstructions().split("@"));

            for (int i = 0; i < recipeWithIngredients.getRecipeIngredients().size(); i++) {
                ingredientList.add(recipeWithIngredients.getRecipeIngredients().get(i).getIngredients().getName());
                ingredientList2.add(recipeWithIngredients.getRecipeIngredients().get(i).getIngredients());
                count.add(recipeWithIngredients.getRecipeIngredients().get(i).getCount());
            }

            Button done = dialog.findViewById(R.id.done);
            Button cancel = dialog.findViewById(R.id.cancel);
            TextView enterName = dialog.findViewById(R.id.enterName);
            recipeImage = dialog.findViewById(R.id.recipeImage);

            enterName.setText(recipeWithIngredients.getRecipe().getName());
            recipeImage.setImageBitmap(BitmapFactory.decodeFile(recipeWithIngredients.getRecipe().getImage()));

            LinearLayoutManager layoutManager = new LinearLayoutManager(dialog.getContext(), LinearLayoutManager.VERTICAL, false);
            recyclerView = dialog.findViewById(R.id.ingredientList);
            recyclerView.setLayoutManager(layoutManager);
            RecyclerViewAdapterGenericSelect adapter = new RecyclerViewAdapterGenericSelect(ingredientList, count, this);
            recyclerView.setAdapter(adapter);

            LinearLayoutManager layoutManager2 = new LinearLayoutManager(dialog.getContext(), LinearLayoutManager.VERTICAL, false);
            recyclerView2 = dialog.findViewById(R.id.instructionList);
            recyclerView2.setLayoutManager(layoutManager2);
            RecyclerViewAdapterInstructionSelect adapter2 = new RecyclerViewAdapterInstructionSelect(instructionList, this);
            recyclerView2.setAdapter(adapter2);

            cancel.setOnClickListener(view -> dialog.cancel());
            done.setOnClickListener(view -> finishedCookingRecipe(ingredientList2, count));

            dialog.show();
        } else {
            makeToast("No recipe selected");
        }
    }

    private void finishedCookingRecipe(List<Ingredients> ingredientList, ArrayList<Integer> count) {
        int i = 0;
        boolean validRecipe = true;
        for (Ingredients ingredient : ingredientList) {
            List<InventoryWithExpiryDates> temp = database.mainDao().getOneInventoryItem(ingredient);
            if (temp.size() == 0 || temp.get(0).getInventory().getAmount() < count.get(i)) {
                validRecipe = false;
                break;
            }
        }
        if (validRecipe) {
            for (Ingredients ingredient : ingredientList) {
                List<InventoryWithExpiryDates> temp = database.mainDao().getOneInventoryItem(ingredient);
                if (temp.get(0).getInventory().getAmount() == count.get(i)) {
                    database.mainDao().deleteInventoryItem(temp.get(0).getInventory());
                } else {
                    database.mainDao().updateInventoryListAmount(-count.get(i), ingredient);
                }
                i++;
            }
            dialog.dismiss();
            makeToast("Finished Recipe!");
        } else {
            makeToast("Recipe does not have the required ingredients");
        }
    }

    private void editRecipe() {
        if (recipePosition != -1) {
            dialog = new Dialog(this.getContext(), R.style.Theme_FYP);
            dialog.setContentView(R.layout.layout_newrecipeitem);

            recipeWithIngredients = database.mainDao().getRecipeWithIngredientsByName(recipes.get(recipePosition).getName()).get(0);
            ingredientList = new ArrayList<>();
            count = new ArrayList<>();
            instructionList = new ArrayList<>();

            Collections.addAll(instructionList, recipeWithIngredients.getRecipe().getInstructions().split("@"));

            for (int i = 0; i < recipeWithIngredients.getRecipeIngredients().size(); i++) {
                ingredientList.add(recipeWithIngredients.getRecipeIngredients().get(i).getIngredients().getName());
                count.add(recipeWithIngredients.getRecipeIngredients().get(i).getCount());
            }

            Button uploadImage = dialog.findViewById(R.id.uploadImage);
            Button done = dialog.findViewById(R.id.done);
            Button cancel = dialog.findViewById(R.id.cancel);
            enterName = dialog.findViewById(R.id.enterName);
            enterIngredient = dialog.findViewById(R.id.enterIngredient);
            enterInstruction = dialog.findViewById(R.id.enterInstruction);
            ImageView addIngredient = dialog.findViewById(R.id.addIngredient);
            ImageView addInstruction = dialog.findViewById(R.id.addInstruction);
            recipeImage = dialog.findViewById(R.id.recipeImage);

            enterName.setText(recipeWithIngredients.getRecipe().getName());
            recipeImage.setImageBitmap(BitmapFactory.decodeFile(recipeWithIngredients.getRecipe().getImage()));

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
            done.setOnClickListener(view -> {
                deleteRecipe();
                askWritePermission();
            });
            cancel.setOnClickListener(view -> dialog.cancel());

            dialog.show();
        } else {
            makeToast("No recipe selected");
        }
    }

    private void deleteRecipe() {
        if (recipePosition != -1) {
            database.mainDao().deleteRecipe(recipes.get(recipePosition));
            File tempFile = new File(recipes.get(recipePosition).getImage());
            Boolean imageDeleted = tempFile.delete();
            recipes.clear();
            recipes.addAll(database.mainDao().getAllRecipes());
            recipeRecyclerView.setAdapter(recipeAdapter);
            recipePosition = -1;
        } else {
            makeToast("No recipe selected");
        }
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
        Bitmap bitmap = ((BitmapDrawable) recipeImage.getDrawable()).getBitmap();

        String dir = requireContext().getExternalFilesDir(null).getAbsolutePath() + "/LessOfIt/Images/";
        File parentFiles = new File(dir);
        if (!parentFiles.exists()) {
            parentFiles.mkdirs();
        }
        ;
        String recipeImageName = enterName.getText().toString() + System.currentTimeMillis() + ".jpg";
        File file = new File(dir, recipeImageName);
        try {
            outputStream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream);

        StringBuilder instructions = new StringBuilder();
        for (int i = 0; i < instructionList.size(); i++) {
            if (i != instructionList.size() - 1) {
                instructions.append(instructionList.get(i)).append("@");
            } else {
                instructions.append(instructionList.get(i));
            }

        }
        Recipe temp = new Recipe(enterName.getText().toString(), instructions.toString(), file.getAbsolutePath());

        database.mainDao().insertRecipe(temp);
        int recipeID = database.mainDao().getAllRecipes().get(database.mainDao().getAllRecipes().size() - 1).getID();
        for (int i = 0; i < ingredientList.size(); i++) {
            Ingredients ingredient = new Ingredients(ingredientList.get(i));
            database.mainDao().insertIngredient(ingredient);

            RecipeIngredients recipeIngredients = new RecipeIngredients(recipeID, ingredient, count.get(i));
            database.mainDao().insertRecipeIngredient(recipeIngredients);
        }
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
        recipeAdapter = new RecyclerViewAdapterRecipes(recipes, this, getActivity());
        recipeRecyclerView.setAdapter(recipeAdapter);
    }

    @Override
    public void onRecipeClick(int position) {
        recipePosition = position;
        makeToast("Selected " + recipes.get(position).getName());
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

    @Override
    public void onRemoveInstructionButtonClick(int position) {
        instructionList.remove(instructionList.get(position));
        recyclerView2.setAdapter(adapter2);
    }

    public void getRecipe() {
        new Thread(() -> {
            //doInBackground
            BufferedReader reader;
            String line;
            StringBuffer responseContent = new StringBuffer();
            try {
                URL url = new URL("https://www.themealdb.com/api/json/v1/1/random.php");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int status = connection.getResponseCode();

                if (status > 299) {
                    reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    while ((line = reader.readLine()) != null) {
                        responseContent.append(line);
                    }
                    reader.close();
                } else {
                    reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    while ((line = reader.readLine()) != null) {
                        responseContent.append(line);
                    }
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            requireActivity().runOnUiThread(() -> {
                Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
                JsonObject json = JsonParser.parseString(responseContent.toString()).getAsJsonObject();

                Meal meal = gson.fromJson(json.get("meals").getAsJsonArray().get(0).toString(), Meal.class);

                String parsedInstructions = meal.getStrInstructions().replaceAll("\n", "").replaceAll("\r", "").replaceAll("\\.", "@");
                Recipe recipe = new Recipe(meal.getStrMeal(), parsedInstructions, meal.getStrMealThumb());
                database.mainDao().insertRecipe(recipe);
                int recipeID = database.mainDao().getAllRecipes().get(database.mainDao().getAllRecipes().size() - 1).getID();
                for (String item : meal.getAllIngredients()) {
                    if (item.equals("")){
                        break;
                    }
                    Ingredients ingredient = new Ingredients(item);
                    database.mainDao().insertIngredient(ingredient);

                    RecipeIngredients recipeIngredients = new RecipeIngredients(recipeID, ingredient, (int) (Math.random() * (4 - 1)) +1);
                    database.mainDao().insertRecipeIngredient(recipeIngredients);
                }
                recipes.clear();
                recipes.addAll(database.mainDao().getAllRecipes());
                recipeRecyclerView.setAdapter(recipeAdapter);
            });
        }).start();

    }

    Toast t;

    public void makeToast(String s) {
        if (t != null) t.cancel();
        t = Toast.makeText(requireContext().getApplicationContext(), s, Toast.LENGTH_SHORT);
        t.show();
    }
}