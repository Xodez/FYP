package com.example.fyp.adapters;

import static android.content.ContentValues.TAG;

import android.util.Log;
import android.view.LayoutInflater;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fyp.Models.Ingredients;
import com.example.fyp.Models.ShoppingList;
import com.example.fyp.R;

import java.util.ArrayList;
import java.util.List;

public class RecyclerViewAdapterShoppingList extends RecyclerView.Adapter<RecyclerViewAdapterShoppingList.ViewHolder> {

    List<ShoppingList> shoppingLists = new ArrayList<>();
    OnShoppingItemListener onShoppingItemListener;

    public RecyclerViewAdapterShoppingList(List<ShoppingList> shoppingLists, OnShoppingItemListener onShoppingItemListener) {
        this.shoppingLists = shoppingLists;
        this.onShoppingItemListener = onShoppingItemListener;
    }

    @NonNull
    @Override
    public RecyclerViewAdapterShoppingList.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_shoppinglistitem, parent, false);
        return new RecyclerViewAdapterShoppingList.ViewHolder(view, onShoppingItemListener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewAdapterShoppingList.ViewHolder holder, int position) {
        holder.count.setText(String.valueOf(shoppingLists.get(position).getAmount()));
        holder.name.setText(shoppingLists.get(position).getIngredient().getName());
        holder.remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onShoppingItemListener.onRemoveButtonClick(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return shoppingLists.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        TextView count;
        TextView name;
        ImageView remove;
        OnShoppingItemListener onShoppingItemListener;

        public ViewHolder(@NonNull View itemView, OnShoppingItemListener onShoppingItemListener) {
            super(itemView);
            count = itemView.findViewById(R.id.text2);
            name = itemView.findViewById(R.id.text1);
            remove = itemView.findViewById(R.id.remove);
            this.onShoppingItemListener = onShoppingItemListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
        onShoppingItemListener.onShoppingItemClick(getAdapterPosition());
        }
    }

    public interface OnShoppingItemListener{
        void onShoppingItemClick(int position);
        void onRemoveButtonClick(int position);
    }
}
