package com.example.fyp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fyp.Models.ExpiryDate;
import com.example.fyp.Models.Inventory;
import com.example.fyp.Models.InventoryWithExpiryDates;
import com.example.fyp.R;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecyclerViewAdapterInventory extends RecyclerView.Adapter<RecyclerViewAdapterInventory.ViewHolder> {

    List<InventoryWithExpiryDates> inventoryList = new ArrayList<>();
    OnInventoryItemListener onInventoryItemListener;

    public RecyclerViewAdapterInventory(List<InventoryWithExpiryDates> inventoryList, OnInventoryItemListener onInventoryItemListener) {
        this.inventoryList = inventoryList;
        this.onInventoryItemListener = onInventoryItemListener;
    }

    @NonNull
    @Override
    public RecyclerViewAdapterInventory.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_inventoryitem, parent, false);
        return new RecyclerViewAdapterInventory.ViewHolder(view, onInventoryItemListener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewAdapterInventory.ViewHolder holder, int position) {
        ArrayList<LocalDate> expiryDates = new ArrayList<>();
        for (ExpiryDate date: inventoryList.get(position).expiryDates){
            expiryDates.add(LocalDate.parse(date.getExpiryDate(), DateTimeFormatter.ofPattern("d-M-yyyy")));
        }
        Collections.sort(expiryDates);
        holder.expiryDate.setText(expiryDates.get(0).format(DateTimeFormatter.ofPattern("d-M-yyyy")));

        holder.count.setText(String.valueOf(inventoryList.get(position).getInventory().getAmount()));
        String preUpper = inventoryList.get(position).getInventory().getIngredient().getName();
        String postUpper = preUpper.substring(0, 1).toUpperCase() + preUpper.substring(1).toLowerCase();
        holder.name.setText(postUpper);
        holder.remove.setOnClickListener(view -> onInventoryItemListener.onRemoveButtonClick(holder.getAdapterPosition(), expiryDates.get(0).format(DateTimeFormatter.ofPattern("d-M-yyyy"))));
    }

    @Override
    public int getItemCount() {
        return inventoryList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView count;
        TextView name;
        TextView expiryDate;
        ImageView remove;
        OnInventoryItemListener onInventoryItemListener;

        public ViewHolder(@NonNull View itemView, OnInventoryItemListener onInventoryItemListener) {
            super(itemView);
            expiryDate = itemView.findViewById(R.id.text3);
            count = itemView.findViewById(R.id.text2);
            name = itemView.findViewById(R.id.text1);
            remove = itemView.findViewById(R.id.remove);
            this.onInventoryItemListener = onInventoryItemListener;
        }
    }

    public interface OnInventoryItemListener {
        void onRemoveButtonClick(int position, String expiryDate);
    }
}
