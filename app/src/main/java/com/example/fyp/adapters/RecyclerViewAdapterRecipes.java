package com.example.fyp.adapters;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fyp.Models.Recipe;
import com.example.fyp.R;
import com.example.fyp.RemoteImageLoader;

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
        String image = recipes.get(position).getImage();
        if (image != null && (image.startsWith("http://") || image.startsWith("https://"))) {
            // Tag the view with the url it is waiting for; when the holder
            // is recycled for another recipe the tag changes and the
            // obsolete result is dropped instead of showing the wrong
            // picture. Fetching happens on the shared bounded loader pool
            // with explicit connect/read timeouts (no raw per-bind thread).
            holder.image.setTag(image);
            RemoteImageLoader.fetchAsync(image, bytes ->
                    postToUi(() -> applyLoadedImage(holder, image, bytes)));
        } else {
            holder.image.setTag(null);
            holder.image.setImageBitmap(image == null
                    ? null : BitmapFactory.decodeFile(image));
        }
        holder.name.setText(recipes.get(position).getName());
    }

    private void applyLoadedImage(ViewHolder holder, String url, byte[] bytes) {
        if (!url.equals(holder.image.getTag())) {
            return; // holder recycled for a different recipe
        }
        Bitmap bitmap = bytes == null ? null
                : BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        if (bitmap != null) {
            holder.image.setImageBitmap(bitmap);
        } else {
            holder.image.setImageResource(R.drawable.ic_launcher_foreground);
        }
    }

    /**
     * Applies UI work only while the hosting activity can still take it;
     * results arriving after teardown are discarded without crashing.
     */
    private void postToUi(Runnable update) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        activity.runOnUiThread(() -> {
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                update.run();
            }
        });
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
