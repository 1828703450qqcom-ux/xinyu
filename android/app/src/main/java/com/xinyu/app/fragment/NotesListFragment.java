package com.xinyu.app.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xinyu.app.NoteEditActivity;
import com.xinyu.app.R;
import com.xinyu.app.adapter.NoteAdapter;
import com.xinyu.app.db.AppDatabase;
import com.xinyu.app.model.Note;

import java.util.List;

public class NotesListFragment extends Fragment {

    private RecyclerView noteList;
    private TextView btnAddNote, tvNoteCount;
    private LinearLayout emptyState;
    private NoteAdapter noteAdapter;

    private SharedPreferences prefs;
    private AppDatabase db;
    private String username;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notes_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = requireContext().getSharedPreferences("xinyu", 0);
        db = AppDatabase.getInstance(requireContext());
        username = prefs.getString("current_user", "guest");

        noteList = view.findViewById(R.id.note_list);
        btnAddNote = view.findViewById(R.id.btn_add_note);
        tvNoteCount = view.findViewById(R.id.tv_note_count);
        emptyState = view.findViewById(R.id.empty_state);

        noteAdapter = new NoteAdapter();
        noteAdapter.setOnNoteClickListener(new NoteAdapter.OnNoteClickListener() {
            @Override
            public void onNoteClick(Note note) {
                Intent intent = new Intent(requireContext(), NoteEditActivity.class);
                intent.putExtra("note_id", note.getId());
                intent.putExtra("note_title", note.getTitle());
                intent.putExtra("note_content", note.getContent());
                startActivity(intent);
                requireActivity().overridePendingTransition(R.anim.slide_in_bottom, 0);
            }

            @Override
            public void onNoteDelete(Note note) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("删除笔记")
                        .setMessage("确定要删除这篇笔记吗？")
                        .setPositiveButton("删除", (dialog, which) -> {
                            db.deleteNote(note.getId());
                            Toast.makeText(requireContext(), "笔记已删除", Toast.LENGTH_SHORT).show();
                            refresh();
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        });

        noteList.setLayoutManager(new LinearLayoutManager(requireContext()));
        noteList.setAdapter(noteAdapter);

        btnAddNote.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), NoteEditActivity.class);
            startActivity(intent);
            requireActivity().overridePendingTransition(R.anim.slide_in_bottom, 0);
        });

        refresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    public void refresh() {
        if (getContext() == null || prefs == null) return;

        List<Note> notes = db.getNotes(username);
        noteAdapter.setData(notes);

        int count = notes.size();
        tvNoteCount.setText("共 " + count + " 篇笔记");

        if (count == 0) {
            emptyState.setVisibility(View.VISIBLE);
            noteList.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            noteList.setVisibility(View.VISIBLE);
        }
    }
}
