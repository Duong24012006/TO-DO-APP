// java
// File: app/src/main/java/com/example/to_do_app/adapters/ScheduleTemplateAdapter.java
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

import java.util.ArrayList;
import java.util.List;

public class ScheduleTemplateAdapter extends RecyclerView.Adapter<ScheduleTemplateAdapter.ScheduleTemplateViewHolder> {

    private final List<ScheduleTemplate> templateList;
    private final Context context;

    public ScheduleTemplateAdapter(Context context, List<ScheduleTemplate> templateList) {
        this.context = context;
        this.templateList = (templateList == null) ? new ArrayList<>() : templateList;
    }

    @NonNull
    @Override
    public ScheduleTemplateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule_template, parent, false);
        return new ScheduleTemplateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleTemplateViewHolder holder, int position) {
        // Guard against inconsistent state: don't call get if position out of range
        if (position < 0 || position >= templateList.size()) return;
        ScheduleTemplate template = templateList.get(position);
        holder.bind(template);
    }

    @Override
    public int getItemCount() {
        return (templateList == null) ? 0 : templateList.size();
    }

    public void updateList(List<ScheduleTemplate> newList) {
        templateList.clear();
        if (newList != null) templateList.addAll(newList);
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

            ivNext.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION || pos < 0 || pos >= templateList.size()) return;
                ScheduleTemplate template = templateList.get(pos);

                Intent intent = new Intent(context, Layout6Activity.class);
                intent.putExtra("EXTRA_TEMPLATE_TITLE", template.getTitle());
                intent.putExtra("EXTRA_TEMPLATE_DESCRIPTION", template.getDescription());
                intent.putStringArrayListExtra("EXTRA_TEMPLATE_TAGS",
                        new ArrayList<>(template.getTags() == null ? new ArrayList<>() : template.getTags()));
                if (!(context instanceof android.app.Activity)) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                context.startActivity(intent);
            });

            itemView.setOnClickListener(v -> ivNext.performClick());
        }

        void bind(ScheduleTemplate template) {
            tvScheduleTitle.setText(template.getTitle() == null ? "" : template.getTitle());
            tvScheduleDescription.setText(template.getDescription() == null ? "" : template.getDescription());

            chipGroupTags.removeAllViews();
            ContextThemeWrapper chipContext = new ContextThemeWrapper(context, R.style.MyChipStyle);
            List<String> tags = template.getTags();
            if (tags == null) tags = new ArrayList<>();
            for (String tag : tags) {
                Chip chip = new Chip(chipContext);
                chip.setText(tag);
                chip.setClickable(false);
                chip.setCheckable(false);
                chipGroupTags.addView(chip);
            }
        }
    }
}
