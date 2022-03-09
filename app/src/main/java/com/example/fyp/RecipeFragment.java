package com.example.fyp;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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

import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.fyp.adapters.RecyclerViewAdapterRecipes;

import java.util.ArrayList;

public class RecipeFragment extends Fragment implements RecyclerViewAdapterRecipes.OnRecipeItemListener {

    public static final int WRITE_EXTERNAL_STORAGE_CODE = 101;
    private static final int REQ = 1;
    private ArrayList<Integer> nImages;
    private ArrayList<String> nNames;
    private Context mContext;
    private Spinner spinner;
    private static final String[] paths = {"By recommended", "By time to make", "By Meal"};
    private Button addNewRecipe;
    ActivityResultLauncher<String> getImage;
    Uri receivedImage;
    ImageView recipeImage;

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

        addNewRecipe = getView().findViewById(R.id.addRecipe);
        addNewRecipe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog();
            }
        });

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        getImages();
    }

    private void showDialog() {
        Dialog dialog = new Dialog(this.getContext(), android.R.style.Theme_DeviceDefault);
        dialog.setContentView(R.layout.layout_newrecipeitem);

        Button uploadImage = dialog.findViewById(R.id.uploadImage);
        recipeImage = dialog.findViewById(R.id.recipeImage);


        uploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGallery();
            }
        });
        dialog.show();
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
                    if (result.getResultCode() == Activity.RESULT_OK){
                        Intent data = result.getData();
                        Uri imageUri = data.getData();

                        recipeImage.setImageURI(imageUri);
                    }else{
                        Toast.makeText(getContext(), "Cancelled", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

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

    private void askWritePermission() {
        if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(getActivity(), new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE_CODE);
        }else{
            getContext();
        }
    }
}