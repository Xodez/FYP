package com.example.fyp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fyp.R;

import java.util.ArrayList;
import java.util.Dictionary;

public class RecyclerViewAdapterGeneric extends RecyclerView.Adapter<RecyclerViewAdapterGeneric.ViewHolder> {

    ArrayList<String> ingredientList;
    ArrayList<Integer> count;
    OnIngredientItemListener onIngredientItemListener;

    public RecyclerViewAdapterGeneric(ArrayList<String> ingredientList, ArrayList<Integer> count, OnIngredientItemListener onIngredientItemListener){
        this.ingredientList = ingredientList;
        this.count = count;
        this.onIngredientItemListener = onIngredientItemListener;
    }

    @NonNull
    @Override
    public RecyclerViewAdapterGeneric.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_shoppinglistitem, parent, false);
        return new RecyclerViewAdapterGeneric.ViewHolder(view, onIngredientItemListener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewAdapterGeneric.ViewHolder holder, int position) {
        holder.count.setText(String.valueOf(count.get(position)));
        String preUpper = ingredientList.get(position);
        String postUpper = preUpper.substring(0, 1).toUpperCase() + preUpper.substring(1).toLowerCase();
        holder.name.setText(postUpper);
        holder.remove.setOnClickListener(view -> onIngredientItemListener.onRemoveButtonClick(holder.getAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return ingredientList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        TextView count;
        TextView name;
        ImageView remove;
        OnIngredientItemListener onIngredientItemListener;

        public ViewHolder(@NonNull View itemView, OnIngredientItemListener onIngredientItemListener) {
            super(itemView);
            count = itemView.findViewById(R.id.text2);
            name = itemView.findViewById(R.id.text1);
            remove = itemView.findViewById(R.id.remove);
            this.onIngredientItemListener = onIngredientItemListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            onIngredientItemListener.onIngredientItemClick(getAdapterPosition());
        }
    }
    public interface OnIngredientItemListener{
        void onIngredientItemClick(int position);
        void onRemoveButtonClick(int position);
    }
}
