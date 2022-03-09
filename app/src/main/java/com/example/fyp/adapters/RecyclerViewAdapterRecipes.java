package com.example.fyp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.fyp.R;

import java.util.ArrayList;

public class RecyclerViewAdapterRecipes extends RecyclerView.Adapter<RecyclerViewAdapterRecipes.ViewHolder> {

    private ArrayList<String> nNames = new ArrayList<>();
    private ArrayList<Integer> nImages = new ArrayList<>();
    private Context mContext;
    OnRecipeItemListener onRecipeItemListener;

    public RecyclerViewAdapterRecipes(ArrayList<String> nNames, ArrayList<Integer> nImages, Context mContext, OnRecipeItemListener onRecipeItemListener) {
        this.nNames = nNames;
        this.nImages = nImages;
        this.mContext = mContext;
        this.onRecipeItemListener = onRecipeItemListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_recipeitem, parent, false);
        return new ViewHolder(view, onRecipeItemListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Glide.with(mContext).asBitmap().load(nImages.get(position)).into(holder.image);
        holder.name.setText(nNames.get(position));

    }

    @Override
    public int getItemCount() {
        return nNames.size();
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
