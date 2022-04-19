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

public class RecyclerViewAdapterGenericSelect extends RecyclerView.Adapter<RecyclerViewAdapterGenericSelect.ViewHolder> {

    ArrayList<String> ingredientList;
    ArrayList<Integer> count;
    OnIngredientItemListener onIngredientItemListener;

    public RecyclerViewAdapterGenericSelect(ArrayList<String> ingredientList, ArrayList<Integer> count, OnIngredientItemListener onIngredientItemListener){
        this.ingredientList = ingredientList;
        this.count = count;
        this.onIngredientItemListener = onIngredientItemListener;
    }

    @NonNull
    @Override
    public RecyclerViewAdapterGenericSelect.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_selectitem, parent, false);
        return new RecyclerViewAdapterGenericSelect.ViewHolder(view, onIngredientItemListener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewAdapterGenericSelect.ViewHolder holder, int position) {
        holder.count.setText(String.valueOf(count.get(position)));
        String preUpper = ingredientList.get(position);
        String postUpper = preUpper.substring(0, 1).toUpperCase() + preUpper.substring(1).toLowerCase();
        holder.name.setText(postUpper);
    }

    @Override
    public int getItemCount() {
        return ingredientList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        TextView count;
        TextView name;
        OnIngredientItemListener onIngredientItemListener;

        public ViewHolder(@NonNull View itemView, OnIngredientItemListener onIngredientItemListener) {
            super(itemView);
            count = itemView.findViewById(R.id.text2);
            name = itemView.findViewById(R.id.text1);
            this.onIngredientItemListener = onIngredientItemListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {

        }
    }
    public interface OnIngredientItemListener{

    }
}
