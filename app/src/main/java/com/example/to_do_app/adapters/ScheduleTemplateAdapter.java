package com.example.to_do_app.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.to_do_app.R;
import com.example.to_do_app.activitys.Layout6Activity;
import com.example.to_do_app.model.ScheduleTemplate;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

public class ScheduleTemplateAdapter extends RecyclerView.Adapter<ScheduleTemplateAdapter.ScheduleTemplateViewHolder> {

    private final List<ScheduleTemplate> templateList;
    private final Context context;

    public ScheduleTemplateAdapter(Context context, List<ScheduleTemplate> templateList) {
        this.context = context;
        this.templateList = templateList;
    }

    @NonNull
    @Override
    public ScheduleTemplateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule_template, parent, false);
        return new ScheduleTemplateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleTemplateViewHolder holder, int position) {
        ScheduleTemplate template = templateList.get(position);
        holder.bind(template);
    }

    @Override
    public int getItemCount() {
        return templateList.size();
    }

    // Method to update the list for filtering
    public void updateList(List<ScheduleTemplate> newList) {
        templateList.clear();
        templateList.addAll(newList);
        notifyDataSetChanged();
    }

    class ScheduleTemplateViewHolder extends RecyclerView.ViewHolder {
        TextView tvScheduleTitle;
        TextView tvScheduleDescription;
        ChipGroup chipGroupTags;
        ImageView ivNext;

        public ScheduleTemplateViewHolder(@NonNull View itemView) {
            super(itemView);
            tvScheduleTitle = itemView.findViewById(R.id.tv_schedule_title);
            tvScheduleDescription = itemView.findViewById(R.id.tv_schedule_description);
            chipGroupTags = itemView.findViewById(R.id.chip_group_tags);
            ivNext = itemView.findViewById(R.id.iv_next);

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, Layout6Activity.class);
                // You could pass the ID or title of the schedule template
                // intent.putExtra("TEMPLATE_TITLE", templateList.get(getAdapterPosition()).getTitle());
                context.startActivity(intent);
            });
        }

        void bind(ScheduleTemplate template) {
            tvScheduleTitle.setText(template.getTitle());
            tvScheduleDescription.setText(template.getDescription());

            chipGroupTags.removeAllViews();
            ContextThemeWrapper chipContext = new ContextThemeWrapper(context, com.google.android.material.R.style.Widget_Material3_Chip_Suggestion);
            for (String tag : template.getTags()) {
                Chip chip = new Chip(chipContext);
                chip.setText(tag);
                chipGroupTags.addView(chip);
            }
        }
    }
}
