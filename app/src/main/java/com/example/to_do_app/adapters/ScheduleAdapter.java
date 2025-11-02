package com.example.to_do_app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.to_do_app.R;
import com.example.to_do_app.model.ScheduleItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Improved ScheduleAdapter:
 * - Null-safety for the list (avoids NPEs)
 * - Uses getBindingAdapterPosition() to get a safe adapter position
 * - Exposes updateList(...) to replace whole list (used when loading from Firebase)
 * - Exposes add/remove convenience methods
 * - Keeps onEdit click callback via OnEditClickListener
 * - Binds startTime/endTime if available, falls back to time field
 *
 * NOTE:
 * - Make sure the layout file referenced here (R.layout.item_schedule) contains the views:
 *     - TextView with id @+id/tvTime (or change id usages here to match your XML)
 *     - TextView with id @+id/tvActivity
 *     - ImageView with id @+id/btnEdit
 * - If you use the "item_time_row.xml" layout you shared earlier, either rename that file to item_schedule.xml
 *   or change inflate(R.layout.item_schedule, ...) to inflate(R.layout.item_time_row, ...).
 */
public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder> {

    private List<ScheduleItem> scheduleList;
    private OnEditClickListener editClickListener;

    // Interface để xử lý sự kiện click Edit
    public interface OnEditClickListener {
        void onEditClick(int position, ScheduleItem item);
    }

    public ScheduleAdapter(List<ScheduleItem> scheduleList, OnEditClickListener listener) {
        // ensure we always have a non-null list
        this.scheduleList = (scheduleList == null) ? new ArrayList<>() : scheduleList;
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

        // Prefer explicit startTime / endTime if available, otherwise use the generic 'time' field
        String timeText;
        if (item.getStartTime() != null && item.getEndTime() != null) {
            timeText = item.getStartTime() + " - " + item.getEndTime();
        } else if (item.getTime() != null && !item.getTime().isEmpty()) {
            timeText = item.getTime();
        } else if (item.getStartTime() != null) {
            timeText = item.getStartTime();
        } else {
            timeText = "";
        }

        holder.tvTime.setText(timeText);
        holder.tvActivity.setText(item.getActivity() == null ? "" : item.getActivity());

        // Xử lý sự kiện click Edit: use binding adapter position to avoid stale positions
        holder.btnEdit.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            if (editClickListener != null) {
                editClickListener.onEditClick(pos, scheduleList.get(pos));
            }
        });

        // Optional: click whole item to edit as well (uncomment if desired)
        // holder.itemView.setOnClickListener(v -> {
        //     int pos = holder.getBindingAdapterPosition();
        //     if (pos == RecyclerView.NO_POSITION) return;
        //     if (editClickListener != null) {
        //         editClickListener.onEditClick(pos, scheduleList.get(pos));
        //     }
        // });
    }

    @Override
    public int getItemCount() {
        return scheduleList.size();
    }

    // Cập nhật item sau khi edit (keeps existing behavior)
    public void updateItem(int position, String newTime, String newActivity) {
        if (position >= 0 && position < scheduleList.size()) {
            ScheduleItem item = scheduleList.get(position);
            // try to split newTime into start/end if contains '-'
            if (newTime != null) {
                String t = newTime.trim();
                if (t.contains("-")) {
                    String[] parts = t.split("-");
                    item.setStartTime(parts[0].trim());
                    if (parts.length > 1) item.setEndTime(parts[1].trim());
                } else {
                    item.setTime(newTime);
                }
            }
            if (newActivity != null) item.setActivity(newActivity);
            notifyItemChanged(position);
        }
    }

    // Replace the whole list (useful after Firebase load)
    public void updateList(List<ScheduleItem> newList) {
        this.scheduleList = (newList == null) ? new ArrayList<>() : new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    // Convenience add/remove
    public void addItem(ScheduleItem item) {
        if (item == null) return;
        scheduleList.add(item);
        notifyItemInserted(scheduleList.size() - 1);
    }

    public void removeItem(int position) {
        if (position < 0 || position >= scheduleList.size()) return;
        scheduleList.remove(position);
        notifyItemRemoved(position);
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