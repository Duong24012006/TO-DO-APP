package com.example.to_do_app.adapters;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.to_do_app.R;
import com.example.to_do_app.fragment.ProfileFragment;

import java.util.List;

/**
 * Adapter for history RecyclerView in ProfileFragment.
 *
 * Layout for each item uses res/layout/profile_history_item.xml which must define:
 * - TextView id @+id/tv_item_title
 * - TextView id @+id/tv_item_subtitle
 * - (optional) ImageButton ids for actions, here we'll add three small buttons in code using existing layout:
 *     - apply button (we reuse iv_chevron as apply trigger)
 *     - edit/delete buttons can be added to the item layout if desired; for now we provide a popup menu on item click.
 *
 * This adapter exposes a listener interface to the fragment to handle apply/edit/delete actions.
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {

    public interface OnItemActionListener {
        void onApplyClicked(int position);
        void onEditClicked(int position);
        void onDeleteClicked(int position);
    }

    private final List<ProfileFragment.HistoryItem> items;
    private final OnItemActionListener listener;

    public HistoryAdapter(List<ProfileFragment.HistoryItem> items, OnItemActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.profile_history_item, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ProfileFragment.HistoryItem it = items.get(position);
        holder.tvTitle.setText(it.title);
        holder.tvSubtitle.setText(it.subtitle);

        // Tap whole item to open actions (Apply/Edit/Delete)
        holder.itemView.setOnClickListener(v -> {
            // simple dialog with 3 choices
            android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(v.getContext());
            b.setTitle(it.title);
            String[] opts = new String[]{"Áp dụng", "Chỉnh sửa", "Xóa"};
            b.setItems(opts, (dialog, which) -> {
                if (which == 0) {
                    if (listener != null) listener.onApplyClicked(position);
                } else if (which == 1) {
                    if (listener != null) listener.onEditClicked(position);
                } else if (which == 2) {
                    if (listener != null) listener.onDeleteClicked(position);
                }
            });
            b.show();
        });

        // Chevron can also be a quick apply
        holder.ivChevron.setOnClickListener(v -> {
            if (listener != null) listener.onApplyClicked(position);
        });
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvSubtitle;
        View ivChevron;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_item_title);
            tvSubtitle = itemView.findViewById(R.id.tv_item_subtitle);
            ivChevron = itemView.findViewById(R.id.iv_chevron);
        }
    }
}