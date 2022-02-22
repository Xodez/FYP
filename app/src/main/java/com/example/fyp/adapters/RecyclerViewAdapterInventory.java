package com.example.fyp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fyp.Models.Inventory;
import com.example.fyp.Models.ShoppingList;
import com.example.fyp.R;

import java.util.ArrayList;
import java.util.List;

public class RecyclerViewAdapterInventory extends RecyclerView.Adapter<RecyclerViewAdapterInventory.ViewHolder> {

    List<Inventory> inventoryList = new ArrayList<>();
    RecyclerViewAdapterInventory.OnInventoryItemListener onInventoryItemListener;

    public RecyclerViewAdapterInventory(List<Inventory> inventoryList, OnInventoryItemListener onInventoryItemListener){
        this.inventoryList = inventoryList;
        this.onInventoryItemListener = onInventoryItemListener;
    }

    @NonNull
    @Override
    public RecyclerViewAdapterInventory.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_shoppinglistitem, parent, false);
        return new RecyclerViewAdapterInventory.ViewHolder(view, onInventoryItemListener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewAdapterInventory.ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        TextView count;
        TextView name;
        ImageView remove;
        RecyclerViewAdapterInventory.OnInventoryItemListener onInventoryItemListener;

        public ViewHolder(@NonNull View itemView, RecyclerViewAdapterInventory.OnInventoryItemListener onInventoryItemListener) {
            super(itemView);
            count = itemView.findViewById(R.id.text2);
            name = itemView.findViewById(R.id.text1);
            remove = itemView.findViewById(R.id.remove);
            this.onInventoryItemListener = onInventoryItemListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            onInventoryItemListener.onInventoryItemClick(getAdapterPosition());
        }
    }

    public interface OnInventoryItemListener{
        void onInventoryItemClick(int position);
        void onRemoveButtonClick(int position);
    }
}
