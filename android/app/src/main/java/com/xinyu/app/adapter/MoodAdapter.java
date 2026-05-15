package com.xinyu.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.xinyu.app.R;
import com.xinyu.app.model.MoodRecord;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MoodAdapter extends RecyclerView.Adapter<MoodAdapter.ViewHolder> {

    private List<MoodRecord> records = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("MM/dd HH:mm", Locale.CHINA);

    public void setData(List<MoodRecord> data) {
        this.records = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mood_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MoodRecord record = records.get(position);
        holder.tvEmoji.setText(record.getEmoji() != null ? record.getEmoji() : "😐");
        holder.tvLabel.setText(record.getMoodLabel());
        String note = record.getNote();
        holder.tvNote.setText(note != null && !note.isEmpty() ? note : "");
        holder.tvNote.setVisibility(note != null && !note.isEmpty() ? View.VISIBLE : View.GONE);
        holder.tvTime.setText(timeFormat.format(new Date(record.getCreatedAt())));
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmoji, tvLabel, tvNote, tvTime;

        ViewHolder(View itemView) {
            super(itemView);
            tvEmoji = itemView.findViewById(R.id.tv_mood_emoji);
            tvLabel = itemView.findViewById(R.id.tv_mood_title);
            tvNote = itemView.findViewById(R.id.tv_mood_note);
            tvTime = itemView.findViewById(R.id.tv_mood_time);
        }
    }
}
