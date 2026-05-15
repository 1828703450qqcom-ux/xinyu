package com.xinyu.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.xinyu.app.R;
import com.xinyu.app.model.Note;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.ViewHolder> {

    public interface OnNoteClickListener {
        void onNoteClick(Note note);
        void onNoteDelete(Note note);
    }

    private List<Note> notes = new ArrayList<>();
    private OnNoteClickListener listener;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.CHINA);

    public void setOnNoteClickListener(OnNoteClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<Note> data) {
        this.notes = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Note note = notes.get(position);
        holder.tvTitle.setText(note.getTitle());
        String content = note.getContent();
        holder.tvPreview.setText(content != null && !content.isEmpty() ? content : "空白笔记");
        holder.tvTime.setText(timeFormat.format(new Date(note.getUpdatedAt())));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onNoteClick(note);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onNoteDelete(note);
        });
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvPreview, tvTime;
        ImageButton btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_note_title);
            tvPreview = itemView.findViewById(R.id.tv_note_preview);
            tvTime = itemView.findViewById(R.id.tv_note_time);
            btnDelete = itemView.findViewById(R.id.btn_note_delete);
        }
    }
}
