package com.example.to_do_app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.to_do_app.R;
import com.example.to_do_app.model.ScheduleItem;
import java.util.List;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder> {

    private List<ScheduleItem> scheduleList;
    private OnEditClickListener editClickListener;

    // Interface để xử lý sự kiện click Edit
    public interface OnEditClickListener {
        void onEditClick(int position, ScheduleItem item);
    }

    public ScheduleAdapter(List<ScheduleItem> scheduleList, OnEditClickListener listener) {
        this.scheduleList = scheduleList;
        this.editClickListener = listener;
    }

    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule, parent, false);
        return new ScheduleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
        ScheduleItem item = scheduleList.get(position);
        holder.tvTime.setText(item.getTime());
        holder.tvActivity.setText(item.getActivity());

        // Xử lý sự kiện click Edit
        holder.btnEdit.setOnClickListener(v -> {
            if (editClickListener != null) {
                editClickListener.onEditClick(position, item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return scheduleList.size();
    }

    // Cập nhật item sau khi edit
    public void updateItem(int position, String newTime, String newActivity) {
        if (position >= 0 && position < scheduleList.size()) {
            ScheduleItem item = scheduleList.get(position);
            item.setTime(newTime);
            item.setActivity(newActivity);
            notifyItemChanged(position);
        }
    }

    // ViewHolder
    static class ScheduleViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime;
        TextView tvActivity;
        ImageView btnEdit;

        public ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvActivity = itemView.findViewById(R.id.tvActivity);
            btnEdit = itemView.findViewById(R.id.btnEdit);
        }
    }
}