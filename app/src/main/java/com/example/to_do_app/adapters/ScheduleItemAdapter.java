package com.example.to_do_app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.to_do_app.R;
import com.example.to_do_app.model.ScheduleItem;

import java.util.List;

public class ScheduleItemAdapter extends RecyclerView.Adapter<ScheduleItemAdapter.ViewHolder> {

    private final List<ScheduleItem> itemList;
    private final Context context;
    private final OnItemClickListener listener;

    // Callback interface
    public interface OnItemClickListener {
        void onItemClick(int position, ScheduleItem item);
    }

    public ScheduleItemAdapter(Context context, List<ScheduleItem> itemList, OnItemClickListener listener) {
        this.context = context;
        this.itemList = itemList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule_template, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScheduleItem item = itemList.get(position);
        holder.tvStartTime.setText(item.getStartTime());
        holder.tvEndTime.setText(item.getEndTime());
        holder.tvActivityPreview.setText(item.getActivity());

        holder.ivEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(position, item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    // Cập nhật danh sách dữ liệu
    public void updateList(List<ScheduleItem> newList) {
        itemList.clear();           // Xóa dữ liệu cũ
        itemList.addAll(newList);   // Thêm dữ liệu mới
        notifyDataSetChanged();     // Cập nhật RecyclerView
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStartTime, tvEndTime, tvActivityPreview;
        ImageView ivEdit;

        ViewHolder(View itemView) {
            super(itemView);
            tvStartTime = itemView.findViewById(R.id.tv_start_time);
            tvEndTime = itemView.findViewById(R.id.tv_end_time);
            tvActivityPreview = itemView.findViewById(R.id.tv_activity_preview);
            ivEdit = itemView.findViewById(R.id.iv_edit);
        }
    }
}
