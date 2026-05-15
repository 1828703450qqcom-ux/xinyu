package com.xinyu.app;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.xinyu.app.db.AppDatabase;
import com.xinyu.app.model.Note;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NoteEditActivity extends AppCompatActivity {

    private EditText etTitle, etContent;
    private TextView tvDate;
    private AppDatabase db;
    private String username;
    private long existingNoteId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_edit);

        db = AppDatabase.getInstance(this);
        username = getSharedPreferences("xinyu", 0).getString("current_user", "guest");

        initViews();
        loadNote();
    }

    private void initViews() {
        etTitle = findViewById(R.id.et_note_title);
        etContent = findViewById(R.id.et_note_content);
        tvDate = findViewById(R.id.tv_date);

        tvDate.setText(new SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault()).format(new Date()));

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        findViewById(R.id.btn_save).setOnClickListener(v -> saveNote());
    }

    private void loadNote() {
        existingNoteId = getIntent().getLongExtra("note_id", -1);
        String title = getIntent().getStringExtra("note_title");
        String content = getIntent().getStringExtra("note_content");

        if (title != null) etTitle.setText(title);
        if (content != null) etContent.setText(content);

        if (existingNoteId > 0) {
            // Editing existing note - title already loaded
        }
    }

    private void saveNote() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (title.isEmpty() && content.isEmpty()) {
            Toast.makeText(this, "标题和内容不能都为空", Toast.LENGTH_SHORT).show();
            return;
        }
        String filterErr = com.xinyu.app.util.ContentFilter.check(title);
        if (filterErr == null) filterErr = com.xinyu.app.util.ContentFilter.check(content);
        if (filterErr != null) {
            Toast.makeText(this, filterErr, Toast.LENGTH_SHORT).show();
            return;
        }

        Note note;
        if (existingNoteId > 0) {
            note = new Note(username, title, content);
            note.setId(existingNoteId);
        } else {
            note = new Note(username, title, content);
        }

        db.saveNote(note);
        Toast.makeText(this, "笔记已保存 📝", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }
}
