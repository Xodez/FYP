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

public class RecyclerViewAdapterInstructionSelect extends RecyclerView.Adapter<RecyclerViewAdapterInstructionSelect.ViewHolder> {

    ArrayList<String> instructions;
    OnItemListener onItemListener;

    public RecyclerViewAdapterInstructionSelect(ArrayList<String> instructions, OnItemListener onItemListener){
        this.instructions = instructions;
        this.onItemListener = onItemListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_instructionitem2, parent, false);
        return new RecyclerViewAdapterInstructionSelect.ViewHolder(view, onItemListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String numberString = (position + 1) + ":";
        holder.number.setText(numberString);
        holder.instruction.setText(instructions.get(position));
    }

    @Override
    public int getItemCount() {
        return instructions.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        TextView instruction;
        TextView number;
        OnItemListener onItemListener;

        public ViewHolder(@NonNull View itemView, OnItemListener onItemListener) {
            super(itemView);
            instruction = itemView.findViewById(R.id.instruction);
            number = itemView.findViewById(R.id.number);
            this.onItemListener = onItemListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {

        }
    }

    public interface OnItemListener{
    }
}
