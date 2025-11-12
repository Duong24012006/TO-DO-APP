package com.example.to_do_app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.to_do_app.R;
import com.example.to_do_app.model.ScheduleItem;

import java.util.ArrayList;
import java.util.List;

public class ScheduleItemAdapter extends RecyclerView.Adapter<ScheduleItemAdapter.VH> {

    public interface OnItemClickListener {
        void onItemClick(int position, ScheduleItem item);
        void onEditClick(int position, ScheduleItem item);
    }

    private final Context ctx;
    private List<ScheduleItem> items;
    private final OnItemClickListener listener;
    private final boolean showEditButton; // Field to control edit button visibility

    // Constructor for Layout6Activity (defaults to showing edit button)
    public ScheduleItemAdapter(Context ctx, List<ScheduleItem> items, OnItemClickListener listener) {
        this(ctx, items, listener, true);
    }

    // Overloaded constructor for HomeFragment (allows hiding the edit button)
    public ScheduleItemAdapter(Context ctx, List<ScheduleItem> items, OnItemClickListener listener, boolean showEditButton) {
        this.ctx = ctx;
        this.items = (items == null) ? new ArrayList<>() : items;
        this.listener = listener;
        this.showEditButton = showEditButton;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_time_row, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ScheduleItem item = items.get(position);

        // Bind start/end/activity to the views
        holder.tvStart.setText(item.getStartTime() != null ? item.getStartTime() : "");
        holder.tvEnd.setText(item.getEndTime() != null ? item.getEndTime() : "");
        holder.tvActivity.setText(item.getActivity() != null ? item.getActivity() : "");

        holder.cardContent.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(holder.getBindingAdapterPosition(), item);
        });

        // Conditionally set visibility and click listener for the edit icon
        if (showEditButton) {
            holder.ivEdit.setVisibility(View.VISIBLE);
            holder.ivEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEditClick(holder.getBindingAdapterPosition(), item);
            });
        } else {
            holder.ivEdit.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateList(List<ScheduleItem> newList) {
        this.items = (newList == null) ? new ArrayList<>() : new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvStart, tvEnd, tvActivity;
        ImageView ivEdit;
        CardView cardContent;

        VH(@NonNull View itemView) {
            super(itemView);
            tvStart = itemView.findViewById(R.id.tv_start_time);
            tvEnd = itemView.findViewById(R.id.tv_end_time);
            tvActivity = itemView.findViewById(R.id.tv_activity_preview);
            ivEdit = itemView.findViewById(R.id.iv_edit);
            cardContent = itemView.findViewById(R.id.card_content);
        }
    }
}
