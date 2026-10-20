package com.example.fyp.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fyp.Models.Recipe;
import com.example.fyp.R;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class RecyclerViewAdapterRecipes extends RecyclerView.Adapter<RecyclerViewAdapterRecipes.ViewHolder> {

    private List<Recipe> recipes = new ArrayList<>();
    OnRecipeItemListener onRecipeItemListener;
    Activity activity;

    public RecyclerViewAdapterRecipes(List<Recipe> recipes, OnRecipeItemListener onRecipeItemListener, Activity activity) {
        this.recipes = recipes;
        this.onRecipeItemListener = onRecipeItemListener;
        this.activity = activity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_recipeitem, parent, false);
        return new ViewHolder(view, onRecipeItemListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if(recipes.get(position).getImage().startsWith("http://") || recipes.get(position).getImage().startsWith("https://")){
            new Thread(() -> {
                InputStream in;
                Drawable d = null;
                try {
                    in = (InputStream) new URL(recipes.get(position).getImage()).getContent();
                    d = Drawable.createFromStream(in, "src image");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Drawable finalD = d;
                activity.runOnUiThread(() -> {
                    if (finalD != null) {
                        holder.image.setImageDrawable(finalD);
                    }
                    else{
                        holder.image.setImageResource(R.drawable.ic_launcher_foreground);
                    }
                });
            }).start();
        }
        else {
            holder.image.setImageBitmap(BitmapFactory.decodeFile(recipes.get(position).getImage()));
        }
        holder.name.setText(recipes.get(position).getName());

    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        ImageView image;
        TextView name;
        OnRecipeItemListener onRecipeItemListener;

        public ViewHolder(@NonNull View itemView, OnRecipeItemListener onRecipeItemListener) {
            super(itemView);
            image = itemView.findViewById(R.id.image_view);
            name = itemView.findViewById(R.id.name);
            this.onRecipeItemListener = onRecipeItemListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            onRecipeItemListener.onRecipeClick(getAdapterPosition());
        }
    }

    public interface OnRecipeItemListener{
        void onRecipeClick(int position);
    }
}
