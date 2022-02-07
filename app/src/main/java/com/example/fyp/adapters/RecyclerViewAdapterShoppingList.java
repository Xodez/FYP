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

public class RecyclerViewAdapterShoppingList extends RecyclerView.Adapter<RecyclerViewAdapterShoppingList.ViewHolder> {

    private ArrayList<String> nNames = new ArrayList<>();
    private ArrayList<Integer> nCount = new ArrayList<>();
    OnShoppingItemListener onShoppingItemListener;

    public RecyclerViewAdapterShoppingList(ArrayList<String> nNames, ArrayList<Integer> nCount, OnShoppingItemListener onShoppingItemListener) {
        this.nNames = nNames;
        this.nCount = nCount;
        this.onShoppingItemListener = onShoppingItemListener;
    }

    @NonNull
    @Override
    public RecyclerViewAdapterShoppingList.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new RecyclerViewAdapterShoppingList.ViewHolder(view, onShoppingItemListener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewAdapterShoppingList.ViewHolder holder, int position) {
        holder.count.setText(String.valueOf(nCount.get(position)));
        holder.name.setText(nNames.get(position));

    }

    @Override
    public int getItemCount() {
        return nNames.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        TextView count;
        TextView name;
        OnShoppingItemListener onShoppingItemListener;

        public ViewHolder(@NonNull View itemView, OnShoppingItemListener onShoppingItemListener) {
            super(itemView);
            count = itemView.findViewById(android.R.id.text2);
            name = itemView.findViewById(android.R.id.text1);
            this.onShoppingItemListener = onShoppingItemListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
        onShoppingItemListener.onShoppingItemCLick(getAdapterPosition());
        }
    }

    public interface OnShoppingItemListener{
        void onShoppingItemCLick(int position);
    }
}
