package com.example.to_do_app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.to_do_app.R;
import com.example.to_do_app.model.TimeSlot;
import java.util.List;

public class TimeAdapter extends RecyclerView.Adapter<TimeAdapter.TimeViewHolder> {

    private List<TimeSlot> list;
    private OnEditClickListener editListener;

    public interface OnEditClickListener {
        void onEditClick(int position, TimeSlot slot);
    }

    public TimeAdapter(List<TimeSlot> list, OnEditClickListener listener) {
        this.list = list;
        this.editListener = listener;
    }

    @NonNull
    @Override
    public TimeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_time_row, parent, false);
        return new TimeViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TimeViewHolder holder, int position) {
        TimeSlot slot = list.get(position);
        holder.tvStart.setText(slot.getStartTime());
        holder.tvEnd.setText(slot.getEndTime());
        holder.tvActivity.setText(slot.getActivity());

        holder.ivEdit.setOnClickListener(v -> {
            if (editListener != null) editListener.onEditClick(position, slot);
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    public void updateList(List<TimeSlot> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    static class TimeViewHolder extends RecyclerView.ViewHolder {
        TextView tvStart, tvEnd, tvActivity;
        ImageView ivEdit;
        TimeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStart = itemView.findViewById(R.id.tv_start_time);
            tvEnd = itemView.findViewById(R.id.tv_end_time);
            tvActivity = itemView.findViewById(R.id.tv_activity_preview);
            ivEdit = itemView.findViewById(R.id.iv_edit);
        }
    }
}