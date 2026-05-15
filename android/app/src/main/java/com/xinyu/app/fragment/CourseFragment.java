package com.xinyu.app.fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.xinyu.app.R;
import com.xinyu.app.util.Reporter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CourseFragment extends Fragment {

    private LinearLayout weekdayTabs;
    private LinearLayout scheduleContainer;
    private LinearLayout emptyState;
    private TextView btnAddCourse;
    private TextView btnImportCourse;
    private SharedPreferences prefs;
    private int selectedDay = 0; // 0=周一
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final String[] weekdays = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
    private final int[] courseColors = {
            0xFF81D4FA, 0xFFA5D6A7, 0xFFF48FB1, 0xFFCE93D8,
            0xFFFFE082, 0xFF80CBC4, 0xFFFFAB91
    };

    private final ActivityResultLauncher<Intent> filePickerLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) importCourseFile(uri);
            }
        });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_course, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = requireContext().getSharedPreferences("xinyu", 0);

        weekdayTabs = view.findViewById(R.id.weekday_tabs);
        scheduleContainer = view.findViewById(R.id.schedule_container);
        emptyState = view.findViewById(R.id.empty_state);
        btnAddCourse = view.findViewById(R.id.btn_add_course);
        btnImportCourse = view.findViewById(R.id.btn_import_course);

        // Auto-select today's weekday (Mon=1 -> index 0, Sun=7 -> index 6)
        int dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        selectedDay = (dayOfWeek + 5) % 7; // Mon=0, Tue=1, ... Sun=6

        btnAddCourse.setOnClickListener(v -> showAddCourseDialog());
        btnImportCourse.setOnClickListener(v -> pickCourseFile());
        setupWeekdayTabs();
        loadSchedule();
    }

    private void pickCourseFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimeTypes = {
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        };
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(Intent.createChooser(intent, "选择课表文件"));
    }

    private void importCourseFile(Uri uri) {
        Toast.makeText(getContext(), "正在解析文件...", Toast.LENGTH_SHORT).show();
        executor.execute(() -> {
            try {
                // 将Uri复制到临时文件
                InputStream is = getContext().getContentResolver().openInputStream(uri);
                String fileName = "course_import.tmp";
                // 尝试获取文件名
                String[] projection = {android.provider.OpenableColumns.DISPLAY_NAME};
                android.database.Cursor cursor = getContext().getContentResolver().query(uri, projection, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    fileName = cursor.getString(0);
                    cursor.close();
                }
                File tmpFile = new File(getContext().getCacheDir(), fileName);
                FileOutputStream fos = new FileOutputStream(tmpFile);
                byte[] buf = new byte[4096];
                int len;
                while ((len = is.read(buf)) > 0) fos.write(buf, 0, len);
                is.close();
                fos.close();

                // 上传到服务器解析
                JSONObject result = Reporter.importCourseFile(tmpFile);
                tmpFile.delete();

                mainHandler.post(() -> {
                    try {
                        if (result.has("error")) {
                            Toast.makeText(getContext(), "解析失败: " + result.optString("error"), Toast.LENGTH_LONG).show();
                            return;
                        }
                        JSONArray courses = result.optJSONArray("courses");
                        if (courses == null || courses.length() == 0) {
                            Toast.makeText(getContext(), "未识别到课程信息", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        showImportPreview(courses);
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "解析出错", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> Toast.makeText(getContext(), "文件读取失败", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showImportPreview(JSONArray courses) {
        String[] dayNames = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_import_preview, null);
        RecyclerView recycler = dialogView.findViewById(R.id.recycler_import_courses);
        TextView tvCount = dialogView.findViewById(R.id.tv_import_count);

        List<JSONObject> courseList = new ArrayList<>();
        List<Boolean> checkedList = new ArrayList<>();
        for (int i = 0; i < courses.length(); i++) {
            courseList.add(courses.optJSONObject(i));
            checkedList.add(true);
        }

        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        recycler.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new RecyclerView.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_import_course, parent, false)) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                JSONObject c = courseList.get(position);
                CheckBox cb = holder.itemView.findViewById(R.id.cb_import);
                TextView tvName = holder.itemView.findViewById(R.id.tv_course_name);
                TextView tvInfo = holder.itemView.findViewById(R.id.tv_course_info);

                tvName.setText(c.optString("name", "未知课程"));
                String info = "";
                int wd = c.optInt("weekday", -1);
                if (wd >= 0 && wd < dayNames.length) info += dayNames[wd] + "  ";
                String time = c.optString("time", "");
                if (!time.isEmpty()) info += time + "  ";
                String room = c.optString("room", "");
                if (!room.isEmpty()) info += room;
                tvInfo.setText(info.isEmpty() ? "未知时间" : info);

                cb.setChecked(checkedList.get(position));
                cb.setOnCheckedChangeListener((b, checked) -> checkedList.set(position, checked));
            }

            @Override
            public int getItemCount() { return courseList.size(); }
        });

        tvCount.setText("共 " + courses.length() + " 门课程，勾选要导入的");

        new AlertDialog.Builder(getContext())
            .setTitle("导入课表")
            .setView(dialogView)
            .setPositiveButton("导入", (d, w) -> {
                int imported = 0;
                for (int i = 0; i < courseList.size(); i++) {
                    if (checkedList.get(i)) {
                        JSONObject c = courseList.get(i);
                        int wd = c.optInt("weekday", selectedDay);
                        if (wd < 0 || wd > 6) wd = selectedDay;
                        addCourseSilent(wd, c.optString("name", ""), c.optString("room", ""), c.optString("time", ""));
                        imported++;
                    }
                }
                loadSchedule();
                Toast.makeText(getContext(), "成功导入 " + imported + " 门课程", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void addCourseSilent(int day, String name, String room, String time) {
        try {
            String json = prefs.getString("courses_" + day, "[]");
            JSONArray courses = new JSONArray(json);
            JSONObject course = new JSONObject();
            course.put("name", name);
            course.put("room", room);
            course.put("time", time);
            courses.put(course);
            prefs.edit().putString("courses_" + day, courses.toString()).apply();
        } catch (Exception e) {
            // ignore
        }
    }

    private void setupWeekdayTabs() {
        weekdayTabs.removeAllViews();
        for (int i = 0; i < weekdays.length; i++) {
            TextView tab = new TextView(requireContext());
            tab.setText(weekdays[i]);
            tab.setTextSize(14);
            tab.setPadding(dpToPx(14), dpToPx(8), dpToPx(14), dpToPx(8));

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dpToPx(20));
            if (i == selectedDay) {
                bg.setColor(getResources().getColor(R.color.primary, null));
                tab.setTextColor(Color.WHITE);
            } else {
                bg.setColor(Color.TRANSPARENT);
                bg.setStroke(1, getResources().getColor(R.color.divider, null));
                tab.setTextColor(getResources().getColor(R.color.text_secondary, null));
            }
            tab.setBackground(bg);

            final int day = i;
            tab.setOnClickListener(v -> {
                selectedDay = day;
                setupWeekdayTabs();
                loadSchedule();
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(dpToPx(6));
            tab.setLayoutParams(params);

            weekdayTabs.addView(tab);
        }
    }

    private void loadSchedule() {
        scheduleContainer.removeAllViews();
        String json = prefs.getString("courses_" + selectedDay, "[]");

        try {
            JSONArray courses = new JSONArray(json);
            if (courses.length() == 0) {
                emptyState.setVisibility(View.VISIBLE);
                scheduleContainer.setVisibility(View.GONE);
                return;
            }

            emptyState.setVisibility(View.GONE);
            scheduleContainer.setVisibility(View.VISIBLE);

            for (int i = 0; i < courses.length(); i++) {
                JSONObject course = courses.getJSONObject(i);
                View itemView = LayoutInflater.from(requireContext())
                        .inflate(R.layout.item_course, scheduleContainer, false);

                TextView tvName = itemView.findViewById(R.id.tv_course_name);
                TextView tvRoom = itemView.findViewById(R.id.tv_course_room);
                TextView tvTime = itemView.findViewById(R.id.tv_course_time);
                View colorBar = itemView.findViewById(R.id.course_color_bar);
                TextView btnDelete = itemView.findViewById(R.id.btn_delete_course);

                tvName.setText(course.getString("name"));
                tvRoom.setText(course.getString("room"));
                tvTime.setText(course.getString("time"));

                GradientDrawable barBg = new GradientDrawable();
                barBg.setColor(courseColors[i % courseColors.length]);
                colorBar.setBackground(barBg);

                MaterialCardView card = (MaterialCardView) itemView;
                GradientDrawable cardBg = new GradientDrawable();
                cardBg.setColor(getResources().getColor(R.color.surface, null));
                cardBg.setCornerRadius(dpToPx(12));
                card.setCardBackgroundColor(courseColors[i % courseColors.length] + 0x15);

                final int index = i;
                btnDelete.setOnClickListener(v -> {
                    deleteCourse(selectedDay, index);
                });

                scheduleContainer.addView(itemView);
            }
        } catch (Exception e) {
            emptyState.setVisibility(View.VISIBLE);
            scheduleContainer.setVisibility(View.GONE);
        }
    }

    private void showAddCourseDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_course, null);

        EditText etName = dialogView.findViewById(R.id.et_course_name);
        EditText etRoom = dialogView.findViewById(R.id.et_course_room);
        EditText etTime = dialogView.findViewById(R.id.et_course_time);
        Spinner spinnerDay = dialogView.findViewById(R.id.spinner_weekday);

        ArrayAdapter<String> dayAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, weekdays);
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDay.setAdapter(dayAdapter);
        spinnerDay.setSelection(selectedDay);

        new AlertDialog.Builder(requireContext())
                .setTitle("添加课程")
                .setView(dialogView)
                .setPositiveButton("添加", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String room = etRoom.getText().toString().trim();
                    String time = etTime.getText().toString().trim();
                    int day = spinnerDay.getSelectedItemPosition();

                    if (name.isEmpty()) {
                        Toast.makeText(requireContext(), "请输入课程名称", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    addCourse(day, name, room, time);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void addCourse(int day, String name, String room, String time) {
        try {
            String json = prefs.getString("courses_" + day, "[]");
            JSONArray courses = new JSONArray(json);
            JSONObject course = new JSONObject();
            course.put("name", name);
            course.put("room", room);
            course.put("time", time);
            courses.put(course);
            prefs.edit().putString("courses_" + day, courses.toString()).apply();

            if (day == selectedDay) loadSchedule();
            Toast.makeText(requireContext(), "课程已添加", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "添加失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteCourse(int day, int index) {
        try {
            String json = prefs.getString("courses_" + day, "[]");
            JSONArray courses = new JSONArray(json);
            JSONArray newCourses = new JSONArray();
            for (int i = 0; i < courses.length(); i++) {
                if (i != index) newCourses.put(courses.getJSONObject(i));
            }
            prefs.edit().putString("courses_" + day, newCourses.toString()).apply();
            loadSchedule();
            Toast.makeText(requireContext(), "课程已删除", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "删除失败", Toast.LENGTH_SHORT).show();
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
