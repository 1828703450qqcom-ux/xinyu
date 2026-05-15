package com.xinyu.app.fragment;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.xinyu.app.R;
import com.xinyu.app.db.AppDatabase;
import com.xinyu.app.model.Note;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NoteFragment extends Fragment {

    private FrameLayout fragmentContainer;
    private TextView tabNotes, tabPomodoro, tabCourse, btnExport;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private NotesListFragment notesListFragment;
    private PomodoroFragment pomodoroFragment;
    private CourseFragment courseFragment;
    private Fragment activeFragment;

    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_note, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = requireContext().getSharedPreferences("xinyu", 0);

        fragmentContainer = view.findViewById(R.id.fragment_container);
        tabNotes = view.findViewById(R.id.tab_notes);
        tabPomodoro = view.findViewById(R.id.tab_pomodoro);
        tabCourse = view.findViewById(R.id.tab_course);
        btnExport = view.findViewById(R.id.btn_export);

        btnExport.setOnClickListener(v -> exportData());

        // Create fragments if first time
        if (savedInstanceState == null) {
            notesListFragment = new NotesListFragment();
            pomodoroFragment = new PomodoroFragment();
            courseFragment = new CourseFragment();

            getChildFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, courseFragment, "course").hide(courseFragment)
                    .add(R.id.fragment_container, pomodoroFragment, "pomodoro").hide(pomodoroFragment)
                    .add(R.id.fragment_container, notesListFragment, "notes")
                    .commit();
            activeFragment = notesListFragment;
        } else {
            notesListFragment = (NotesListFragment) getChildFragmentManager().findFragmentByTag("notes");
            pomodoroFragment = (PomodoroFragment) getChildFragmentManager().findFragmentByTag("pomodoro");
            courseFragment = (CourseFragment) getChildFragmentManager().findFragmentByTag("course");
        }

        tabNotes.setOnClickListener(v -> switchTab(0));
        tabPomodoro.setOnClickListener(v -> switchTab(1));
        tabCourse.setOnClickListener(v -> switchTab(2));
    }

    private void switchTab(int index) {
        Fragment target;
        switch (index) {
            case 1: target = pomodoroFragment; break;
            case 2: target = courseFragment; break;
            default: target = notesListFragment; break;
        }

        if (target == activeFragment) return;

        getChildFragmentManager().beginTransaction()
                .hide(activeFragment)
                .show(target)
                .commit();
        activeFragment = target;

        updateTabStyle(index);
    }

    private void updateTabStyle(int selected) {
        TextView[] tabs = {tabNotes, tabPomodoro, tabCourse};
        String[] labels = {"📝 笔记", "🍅 番茄钟", "📚 课表"};

        for (int i = 0; i < tabs.length; i++) {
            if (i == selected) {
                tabs[i].setTextColor(getResources().getColor(R.color.primary, null));
                tabs[i].setTypeface(null, Typeface.BOLD);
            } else {
                tabs[i].setTextColor(getResources().getColor(R.color.tab_inactive, null));
                tabs[i].setTypeface(null, Typeface.NORMAL);
            }
        }
    }

    private void exportData() {
        btnExport.setEnabled(false);
        btnExport.setText("⏳ 导出中...");

        executor.execute(() -> {
            try {
                String username = prefs.getString("current_user", "guest");
                AppDatabase db = AppDatabase.getInstance(requireContext());

                JSONObject root = new JSONObject();
                root.put("exported_at", System.currentTimeMillis());
                root.put("username", username);

                // Export notes
                List<Note> notes = db.getNotes(username);
                JSONArray notesArr = new JSONArray();
                for (Note n : notes) {
                    JSONObject noteObj = new JSONObject();
                    noteObj.put("id", n.getId());
                    noteObj.put("title", n.getTitle());
                    noteObj.put("content", n.getContent());
                    noteObj.put("created_at", n.getCreatedAt());
                    noteObj.put("updated_at", n.getUpdatedAt());
                    notesArr.put(noteObj);
                }
                root.put("notes", notesArr);

                // Export courses
                JSONArray coursesArr = new JSONArray();
                String[] days = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
                for (int d = 0; d < 7; d++) {
                    String json = prefs.getString("courses_" + d, "[]");
                    JSONArray dayCourses = new JSONArray(json);
                    for (int i = 0; i < dayCourses.length(); i++) {
                        JSONObject c = dayCourses.getJSONObject(i);
                        c.put("weekday", days[d]);
                        coursesArr.put(c);
                    }
                }
                root.put("courses", coursesArr);

                // Export mood stats
                root.put("pomodoro_today", prefs.getInt("pomodoro_count_today", 0));

                String json = root.toString(2);
                String filename = "xinyu_backup_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".json";

                boolean saved = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Downloads.DISPLAY_NAME, filename);
                    values.put(MediaStore.Downloads.MIME_TYPE, "application/json");
                    values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                    Uri uri = requireContext().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (uri != null) {
                        OutputStream os = requireContext().getContentResolver().openOutputStream(uri);
                        os.write(json.getBytes(StandardCharsets.UTF_8));
                        os.close();
                        saved = true;
                    }
                } else {
                    File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    File file = new File(dir, filename);
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(json.getBytes(StandardCharsets.UTF_8));
                    fos.close();
                    saved = true;
                }

                final boolean success = saved;
                final int noteCount = notes.size();
                final int courseCount = coursesArr.length();
                requireActivity().runOnUiThread(() -> {
                    btnExport.setEnabled(true);
                    btnExport.setText("📤 导出");
                    if (success) {
                        Toast.makeText(getContext(), "已导出 " + noteCount + " 篇笔记、" + courseCount + " 门课程到下载目录", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getContext(), "导出失败", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    btnExport.setEnabled(true);
                    btnExport.setText("📤 导出");
                    Toast.makeText(getContext(), "导出出错: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
