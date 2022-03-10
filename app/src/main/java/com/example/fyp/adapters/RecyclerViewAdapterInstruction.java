package com.example.fyp.adapters;

import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fyp.R;

import java.util.ArrayList;

public class RecyclerViewAdapterInstruction extends RecyclerView.Adapter<RecyclerViewAdapterInstruction.ViewHolder> {

    ArrayList<String> instructions;
    OnItemListener onItemListener;

    public RecyclerViewAdapterInstruction(ArrayList<String> instructions, OnItemListener onItemListener){
        this.instructions = instructions;
        this.onItemListener = onItemListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_instructionitem, parent, false);
        return new RecyclerViewAdapterInstruction.ViewHolder(view, onItemListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String numberString = (position + 1) + ":";
        holder.number.setText(numberString);
        holder.instruction.setText(instructions.get(position));
        holder.remove.setOnClickListener(view -> onItemListener.onRemoveInstructionButtonClick(holder.getAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return instructions.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        TextView instruction;
        TextView number;
        ImageView remove;
        OnItemListener onItemListener;

        public ViewHolder(@NonNull View itemView, OnItemListener onItemListener) {
            super(itemView);
            instruction = itemView.findViewById(R.id.instruction);
            number = itemView.findViewById(R.id.number);
            remove = itemView.findViewById(R.id.remove);
            this.onItemListener = onItemListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {

        }
    }

    public interface OnItemListener{
        void onRemoveInstructionButtonClick(int position);
    }
}
