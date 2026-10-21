package com.example.fyp;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fyp.Database.RoomDB;
import com.example.fyp.Models.Ingredients;
import com.example.fyp.Models.Inventory;
import com.example.fyp.Models.InventoryWithExpiryDates;
import com.example.fyp.Models.Meal;
import com.example.fyp.Models.Recipe;
import com.example.fyp.Models.RecipeIngredients;
import com.example.fyp.Models.RecipeWithIngredients;
import com.example.fyp.Models.ShoppingList;
import com.example.fyp.adapters.RecyclerViewAdapterGeneric;
import com.example.fyp.adapters.RecyclerViewAdapterGenericSelect;
import com.example.fyp.adapters.RecyclerViewAdapterInstruction;
import com.example.fyp.adapters.RecyclerViewAdapterInstructionSelect;
import com.example.fyp.adapters.RecyclerViewAdapterRecipes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class RecipeFragment extends Fragment implements RecyclerViewAdapterRecipes.OnRecipeItemListener, RecyclerViewAdapterGeneric.OnIngredientItemListener, RecyclerViewAdapterInstruction.OnItemListener,
        RecyclerViewAdapterInstructionSelect.OnItemListener, RecyclerViewAdapterGenericSelect.OnIngredientItemListener {

    private RecyclerView recyclerView;
    private RecyclerView recyclerView2;
    private static final String[] paths = {"By Recommended", "By Name"};
    private ImageView recipeImage;
    private EditText enterIngredient;
    private EditText enterInstruction;
    private EditText enterName;
    private RecyclerViewAdapterGeneric adapter;
    private RecyclerViewAdapterInstruction adapter2;
    private ArrayList<String> ingredientList;
    private ArrayList<Integer> count;
    private ArrayList<String> instructionList;
    // Custom recipe editor state: image and external id carried through
    // an edit so they survive when the user does not replace them.
    private Recipe editingRecipe;
    private String existingImage;
    private String existingMealId;
    private boolean imageReplaced;
    private List<Recipe> recipes;
    private RoomDB database;
    private Dialog dialog;
    private RecyclerViewAdapterRecipes recipeAdapter;
    private RecyclerView recipeRecyclerView;
    private int recipePosition;
    private RecipeWithIngredients recipeWithIngredients;
    private final List<RecipeWithIngredients> recipeWithIngredients2 = new ArrayList<>();
    private final RecipeAlgorithm ra = new RecipeAlgorithm();
    private String currentSort = "By Recommended";

    /** Single daemon worker serialising all recipe-import searches. */
    private static final java.util.concurrent.ExecutorService IMPORT_EXECUTOR =
            java.util.concurrent.Executors.newSingleThreadExecutor(
                    runnable -> {
                        Thread thread = new Thread(runnable, "meal-import");
                        thread.setDaemon(true);
                        return thread;
                    });
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
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                R.layout.simple_spinner_item_custom, paths);

        EditText searchRecipeText = requireView().findViewById(R.id.recipeSearch);
        Button addNewRecipe = requireView().findViewById(R.id.addRecipe);
        Button deleteRecipe = requireView().findViewById(R.id.delete);
        Button editRecipe = requireView().findViewById(R.id.edit);
        Button selectRecipe = requireView().findViewById(R.id.select);
        Button addRecipe = requireView().findViewById(R.id.addBrowserRecipe);

        selectRecipe.setOnClickListener(view1 -> selectRecipe());
        addNewRecipe.setOnClickListener(view1 -> addCustomRecipe());
        deleteRecipe.setOnClickListener(view1 -> deleteRecipe());
        editRecipe.setOnClickListener(view1 -> editRecipe());
        addRecipe.setOnClickListener(view1 -> searchRecipe(searchRecipeText.getText().toString()));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                currentSort = adapterView.getItemAtPosition(position).toString();
                refreshRecipeList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        initRecyclerView();
    }

    /**
     * Reloads recipes from the database and applies the currently
     * selected sort, so imports, saves, and edits never leave the list
     * silently reverting to raw table order.
     */
    private void refreshRecipeList() {
        List<Recipe> all = database.mainDao().getAllRecipes();
        filteredRecipes.clear();
        if (currentSort.equals("By Name")) {
            filteredRecipes.addAll(ra.filterRecipeByName(all));
        } else {
            expiryDates.clear();
            recipeWithIngredients2.clear();
            recipeWithIngredients2.addAll(database.mainDao().getRecipeWithIngredients());
            expiryDates.addAll(database.mainDao().getInventoriesWithExpiryDates());
            filteredRecipes.addAll(ra.filterRecipeByScore(recipeWithIngredients2, expiryDates));
        }
        recipes.clear();
        recipes.addAll(filteredRecipes);
        recipeRecyclerView.setAdapter(recipeAdapter);
    }

    private void selectRecipe() {
        if (recipePosition != -1) {
            dialog = new Dialog(this.getContext(), R.style.Theme_FYP);
            dialog.setContentView(R.layout.layout_recipe);

            List<RecipeWithIngredients> selected = database.mainDao().getRecipeWithIngredientsByName(recipes.get(recipePosition).getName());
            if (selected.isEmpty()) {
                makeToast("Recipe unavailable");
                return;
            }
            recipeWithIngredients = selected.get(0);
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
            Button addIngredients = dialog.findViewById(R.id.addMissing);
            Button cancel = dialog.findViewById(R.id.cancel);
            TextView enterName = dialog.findViewById(R.id.enterName);
            recipeImage = dialog.findViewById(R.id.recipeImage);

            enterName.setText(recipeWithIngredients.getRecipe().getName());
            String selectedImage = recipeWithIngredients.getRecipe().getImage();
            if (isRemoteUrl(selectedImage)) {
                loadIntoDialogImage(dialog, recipeImage, selectedImage);
            } else {
                recipeImage.setImageBitmap(selectedImage == null
                        ? null : BitmapFactory.decodeFile(selectedImage));
            }

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
            addIngredients.setOnClickListener(view -> addMissingIngredients(ingredientList2, count));

            dialog.show();
        } else {
            makeToast("No recipe selected");
        }
    }

    private void addMissingIngredients(List<Ingredients> ingredientList2, ArrayList<Integer> count) {
        int i = 0;
        for (Ingredients ingredient: ingredientList2) {
            String name = IngredientNames.normalize(ingredient.getName());
            List<InventoryWithExpiryDates> owned = database.mainDao().getOneInventoryItem(name);
            int ownedAmount = owned.isEmpty()
                    ? 0 : owned.get(0).getInventory().getAmount();
            int missing = count.get(i) - ownedAmount;
            if (missing > 0) {
                ShoppingList shoppingList = new ShoppingList(name, new Ingredients(name), missing);
                database.mainDao().insertShoppingList(shoppingList);
            }
            i++;
        }
        dialog.cancel();
        makeToast("Added missing ingredients to shopping list");
    }

    private void finishedCookingRecipe(List<Ingredients> ingredientList, ArrayList<Integer> count) {
        int i = 0;
        boolean validRecipe = true;
        for (Ingredients ingredient : ingredientList) {
            String name = IngredientNames.normalize(ingredient.getName());
            List<InventoryWithExpiryDates> temp = database.mainDao().getOneInventoryItem(name);
            if (temp.isEmpty() || temp.get(0).getInventory().getAmount() < count.get(i)) {
                validRecipe = false;
                break;
            }
            i++;
        }
        i = 0;
        if (validRecipe) {
            for (Ingredients ingredient : ingredientList) {
                String name = IngredientNames.normalize(ingredient.getName());
                List<InventoryWithExpiryDates> temp = database.mainDao().getOneInventoryItem(name);
                Inventory inventory = temp.get(0).getInventory();
                if (inventory.getAmount() == count.get(i)) {
                    // Last units consumed: drop the row and its expiry
                    // records together, leaving nothing stale behind.
                    database.mainDao().deleteInventoryItem(inventory);
                    database.mainDao().deleteExpiryDatesForIngredient(name);
                } else {
                    // Consume the soonest-expiring units first and delete
                    // one expiry record per consumed unit, atomically.
                    database.mainDao().consumeInventoryUnits(name, count.get(i));
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

            List<RecipeWithIngredients> editable = database.mainDao().getRecipeWithIngredientsByName(recipes.get(recipePosition).getName());
            if (editable.isEmpty()) {
                makeToast("Recipe unavailable");
                return;
            }
            recipeWithIngredients = editable.get(0);
            editingRecipe = recipes.get(recipePosition);
            existingImage = editingRecipe.getImage();
            existingMealId = editingRecipe.getMealId();
            imageReplaced = false;
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
            String currentImage = recipeWithIngredients.getRecipe().getImage();
            if (isRemoteUrl(currentImage)) {
                loadIntoDialogImage(dialog, recipeImage, currentImage);
            } else {
                recipeImage.setImageBitmap(currentImage == null
                        ? null : BitmapFactory.decodeFile(currentImage));
            }

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
            done.setOnClickListener(view -> addRecipe());
            cancel.setOnClickListener(view -> dialog.cancel());

            dialog.show();
        } else {
            makeToast("No recipe selected");
        }
    }

    private void deleteRecipe() {
        if (recipePosition != -1) {
            Recipe recipe = recipes.get(recipePosition);
            database.mainDao().deleteRecipeWithIngredients(recipe);
            String image = recipe.getImage();
            if (image != null && !image.startsWith("http://")
                    && !image.startsWith("https://")) {
                new File(image).delete();
            }
            refreshRecipeList();
            recipePosition = -1;
        } else {
            makeToast("No recipe selected");
        }
    }

    private void addCustomRecipe() {
        dialog = new Dialog(this.getContext(), R.style.Theme_FYP);
        dialog.setContentView(R.layout.layout_newrecipeitem);

        editingRecipe = null;
        existingImage = null;
        existingMealId = null;
        imageReplaced = false;
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
        done.setOnClickListener(view -> addRecipe());
        cancel.setOnClickListener(view -> dialog.cancel());

        dialog.show();
    }

    /**
     * Saves the editor contents, for both new and edited recipes.
     *
     * Validation happens before anything is written, and an edit updates
     * the stored recipe in place inside one transaction: an invalid or
     * cancelled edit can no longer leave the original recipe deleted.
     * The external meal id and the existing image are preserved whenever
     * the user did not replace them; a superseded app-private image file
     * is removed only after the database change has committed.
     */
    private void addRecipe() {
        String recipeName = enterName.getText().toString().trim();
        if (recipeName.isEmpty()) {
            makeToast("Enter a recipe name");
            return;
        }
        String imagePath = existingImage == null ? "" : existingImage;
        if (imageReplaced) {
            // Only persist a file when the user actually chose an image;
            // a missing drawable no longer crashes the save.
            String saved = savePickedImageToAppFiles();
            imagePath = saved == null ? "" : saved;
        }

        StringBuilder instructions = new StringBuilder();
        for (int i = 0; i < instructionList.size(); i++) {
            if (i != instructionList.size() - 1) {
                instructions.append(instructionList.get(i)).append("@");
            } else {
                instructions.append(instructionList.get(i));
            }

        }
        Recipe recipe = new Recipe(recipeName, instructions.toString(), imagePath);
        recipe.setMealId(existingMealId);

        List<RecipeIngredients> rows = new ArrayList<>();
        for (int i = 0; i < ingredientList.size(); i++) {
            rows.add(new RecipeIngredients(0,
                    new Ingredients(ingredientList.get(i)), count.get(i)));
        }

        String supersededImage = null;
        if (editingRecipe != null) {
            recipe.setID(editingRecipe.getID());
            database.mainDao().updateRecipeWithIngredients(recipe, rows);
            if (imageReplaced && existingImage != null
                    && !existingImage.isEmpty() && !isRemoteUrl(existingImage)) {
                supersededImage = existingImage;
            }
            editingRecipe = null;
        } else {
            database.mainDao().insertRecipeWithIngredients(recipe, rows);
        }
        if (supersededImage != null) {
            new File(supersededImage).delete();
        }
        refreshRecipeList();
        dialog.dismiss();
    }

    private static boolean isRemoteUrl(String image) {
        return image != null && (image.startsWith("http://")
                || image.startsWith("https://"));
    }

    /**
     * Copies the image currently shown in the editor (picked via the
     * gallery) into app-private storage. Returns the new file path, or
     * null when no bitmap is available or writing failed.
     */
    private String savePickedImageToAppFiles() {
        Drawable drawable = recipeImage == null ? null : recipeImage.getDrawable();
        if (!(drawable instanceof BitmapDrawable)) {
            return null;
        }
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        if (bitmap == null) {
            return null;
        }
        File dir = new File(requireContext().getExternalFilesDir(null).getAbsolutePath() + "/LessOfIt/Images/");
        if (!dir.exists() && !dir.mkdirs()) {
            return null;
        }
        File file = new File(dir, System.currentTimeMillis() + ".jpg");
        try (OutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return file.getAbsolutePath();
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
            String normalised = IngredientNames.normalize(text);
            if (ingredientList.contains(normalised)) {
                int pos = ingredientList.indexOf(normalised);
                count.set(pos, count.get(pos) + 1);
                recyclerView.setAdapter(adapter);
            } else {
                ingredientList.add(normalised);
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
                    if (result.getResultCode() == Activity.RESULT_OK
                            && result.getData() != null
                            && result.getData().getData() != null) {
                        Uri imageUri = result.getData().getData();
                        recipeImage.setImageURI(imageUri);
                        imageReplaced = true;
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
            ingredientList.remove(position);
            count.remove(position);
            recyclerView.setAdapter(adapter);
        }
    }

    @Override
    public void onRemoveInstructionButtonClick(int position) {
        instructionList.remove(instructionList.get(position));
        recyclerView2.setAdapter(adapter2);
    }

    /**
     * Imports one MealDB recipe by meal id. Duplicate imports are skipped
     * via the persisted external id, and recipe ingredients are attached
     * to the row id returned by the insert (never inferred from the last
     * row of the whole table, which raced under concurrent imports).
     */
    private boolean importMeal(String mealId) {
        if (database.mainDao().findRecipeIdByMealId(mealId) != null) {
            return false;
        }
        Meal meal = MealApi.parseMeal(MealApi.get("lookup.php?i=" + mealId));
        if (meal == null) {
            return false;
        }
        Recipe recipe = new Recipe(meal.getStrMeal(),
                MealApi.instructionsFrom(meal), MealApi.imageFrom(meal));
        recipe.setMealId(mealId);

        List<RecipeIngredients> rows = new ArrayList<>();
        for (String name : MealApi.ingredientNames(meal)) {
            // One unit per ingredient: MealDB measure strings ("200g",
            // "to taste") do not map onto discrete inventory units, and
            // inventing random counts was worse than an honest fallback.
            rows.add(new RecipeIngredients(0,
                    new Ingredients(IngredientNames.normalize(name)), 1));
        }
        // Recipe plus ingredient rows commit in one transaction against
        // the unique meal_id index: a racing import from an overlapping
        // search is rejected with -1, so ingredients can never attach to
        // a losing duplicate or the wrong recipe id.
        return database.mainDao()
                .insertRecipeWithIngredients(recipe, rows) != -1;
    }

    private void searchRecipe(String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        if (query.isEmpty()) {
            makeToast("Please enter a recipe ingredient");
            return;
        }
        final String ingredient = query.toLowerCase(Locale.ROOT).replace(" ", "_");
        // Searches queue on one shared import worker: results import
        // sequentially and overlapping searches serialise, so existence
        // checks never interleave and threads stay bounded.
        IMPORT_EXECUTOR.execute(() -> {
            List<String> mealIds = MealApi.parseMealIds(
                    MealApi.get("filter.php?i=" + ingredient));
            if (mealIds == null) {
                postToUiIfAttached(() -> makeToast(
                        "Recipe search failed. Check your connection."));
                return;
            }
            if (mealIds.isEmpty()) {
                postToUiIfAttached(() -> makeToast(
                        "No recipes found with that ingredient"));
                return;
            }
            int importedCount = 0;
            for (String mealId : mealIds) {
                if (importMeal(mealId)) {
                    importedCount++;
                }
            }
            final int count = importedCount;
            postToUiIfAttached(() -> {
                refreshRecipeList();
                if (count == 0) {
                    makeToast("Recipes already imported");
                } else {
                    makeToast("Imported " + count + " recipe(s)");
                }
            });
        });
    }

    /**
     * Posts UI work from background threads, dropping it once the
     * fragment is detached: bare requireActivity() calls from plain
     * threads threw IllegalStateException after the user navigated away,
     * and the uncaught exception terminated the app.
     */
    private void postToUiIfAttached(Runnable update) {
        if (!isAdded()) {
            return;
        }
        requireActivity().runOnUiThread(() -> {
            if (isAdded()) {
                update.run();
            }
        });
    }

    /**
     * Fetches a dialog's remote image on the shared bounded loader pool
     * (with connect/read timeouts) and applies the result only while
     * this exact dialog is still the fragment's current one; obsolete
     * results from earlier dialogs are discarded. A failed image fetch
     * shows the placeholder and never blocks the dialog.
     */
    private void loadIntoDialogImage(Dialog target, ImageView targetView,
                                     String url) {
        RemoteImageLoader.fetchAsync(url, bytes -> postToUiIfAttached(() -> {
            if (dialog != target || !target.isShowing()) {
                return;
            }
            Bitmap bitmap = bytes == null ? null
                    : BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (bitmap != null) {
                targetView.setImageBitmap(bitmap);
            } else {
                targetView.setImageResource(R.drawable.ic_launcher_foreground);
            }
        }));
    }

    Toast t;

    public void makeToast(String s) {
        if (t != null) t.cancel();
        t = Toast.makeText(requireContext().getApplicationContext(), s, Toast.LENGTH_SHORT);
        t.show();
    }
}