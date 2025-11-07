package com.example.to_do_app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.to_do_app.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple RecyclerView adapter to show activity item: time, title, location, note.
 */
public class HomeActivitiesAdapter extends RecyclerView.Adapter<HomeActivitiesAdapter.VH> {

    public static class Item {
        public final String time;
        public final String title;
        public final String location;
        public final String note;
        public Item(String time, String title, String location, String note) {
            this.time = time; this.title = title; this.location = location; this.note = note;
        }
    }

    private final List<Item> items = new ArrayList<>();

    public HomeActivitiesAdapter(List<Item> initial) {
        if (initial != null) items.addAll(initial);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home_activity, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Item it = items.get(position);
        holder.tvTime.setText(it.time != null ? it.time : "");
        holder.tvTitle.setText(it.title != null ? it.title : "");
        holder.tvLocation.setText(it.location != null && !it.location.isEmpty() ? "Địa điểm: " + it.location : "");
        holder.tvNote.setText(it.note != null && !it.note.isEmpty() ? "Ghi chú: " + it.note : "");
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setItems(List<Item> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTime, tvTitle, tvLocation, tvNote;
        VH(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.item_time);
            tvTitle = itemView.findViewById(R.id.item_title);
            tvLocation = itemView.findViewById(R.id.item_location);
            tvNote = itemView.findViewById(R.id.item_note);
        }
    }
}