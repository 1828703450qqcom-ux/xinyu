package com.xinyu.app;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.tabs.TabLayout;
import com.xinyu.app.SquareFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TreeHoleActivity extends AppCompatActivity {

    private EditText etContent;
    private View btnBury;
    private View btnHistory;
    private View btnBack;
    private View buryContainer;
    private View editorContainer;
    private View successContainer;
    private View fragmentContainer;
    private TabLayout tabLayout;
    private SquareFragment squareFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tree_hole);

        etContent = findViewById(R.id.et_content);
        btnBury = findViewById(R.id.btn_bury);
        btnHistory = findViewById(R.id.btn_history);
        btnBack = findViewById(R.id.btn_back);
        buryContainer = findViewById(R.id.bury_container);
        editorContainer = findViewById(R.id.editor_container);
        successContainer = findViewById(R.id.success_container);
        fragmentContainer = findViewById(R.id.fragment_container);
        tabLayout = findViewById(R.id.tab_layout);

        btnBack.setOnClickListener(v -> finish());
        btnBury.setOnClickListener(v -> buryConfession());
        btnHistory.setOnClickListener(v -> showHistory());

        // Setup tabs
        tabLayout.addTab(tabLayout.newTab().setText("🕳️ 树洞"));
        tabLayout.addTab(tabLayout.newTab().setText("🌐 广场"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    // Tree hole tab
                    editorContainer.setVisibility(View.VISIBLE);
                    fragmentContainer.setVisibility(View.GONE);
                    btnHistory.setVisibility(View.VISIBLE);
                } else {
                    // Square tab
                    editorContainer.setVisibility(View.GONE);
                    fragmentContainer.setVisibility(View.VISIBLE);
                    btnHistory.setVisibility(View.GONE);
                    showSquareFragment();
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Default to tree hole tab
        editorContainer.setVisibility(View.VISIBLE);
        fragmentContainer.setVisibility(View.GONE);
    }

    private void showSquareFragment() {
        if (squareFragment == null) {
            squareFragment = new SquareFragment();
        }
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, squareFragment)
            .commit();
    }

    private void buryConfession() {
        String text = etContent.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "写点什么再埋掉吧~", Toast.LENGTH_SHORT).show();
            return;
        }

        saveConfession(text);

        editorContainer.setVisibility(View.GONE);
        successContainer.setVisibility(View.VISIBLE);

        View holeEmoji = findViewById(R.id.success_hole);
        View thankText = findViewById(R.id.success_text);
        View doneBtn = findViewById(R.id.success_done);

        holeEmoji.setScaleX(0.3f);
        holeEmoji.setScaleY(0.3f);
        holeEmoji.setAlpha(0f);
        thankText.setAlpha(0f);
        doneBtn.setAlpha(0f);

        AnimatorSet holeSet = new AnimatorSet();
        holeSet.playTogether(
            ObjectAnimator.ofFloat(holeEmoji, "scaleX", 0.3f, 1.2f),
            ObjectAnimator.ofFloat(holeEmoji, "scaleY", 0.3f, 1.2f),
            ObjectAnimator.ofFloat(holeEmoji, "alpha", 0f, 1f)
        );
        holeSet.setDuration(600);
        holeSet.setInterpolator(new OvershootInterpolator(1.5f));
        holeSet.start();

        thankText.animate().alpha(1f).setStartDelay(500).setDuration(400).start();
        doneBtn.animate().alpha(1f).setStartDelay(800).setDuration(400).start();

        doneBtn.setOnClickListener(v -> {
            etContent.setText("");
            successContainer.setVisibility(View.GONE);
            editorContainer.setVisibility(View.VISIBLE);
        });
    }

    private void saveConfession(String text) {
        try {
            String key = "tree_hole_list";
            String existing = getSharedPreferences("xinyu", MODE_PRIVATE).getString(key, "[]");
            JSONArray arr = new JSONArray(existing);
            JSONObject obj = new JSONObject();
            obj.put("text", text);
            obj.put("time", System.currentTimeMillis());
            arr.put(obj);
            getSharedPreferences("xinyu", MODE_PRIVATE).edit().putString(key, arr.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void showHistory() {
        List<String[]> entries = loadHistory();
        if (entries.isEmpty()) {
            Toast.makeText(this, "树洞里空空的~", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_tree_hole_history, null);
        LinearLayout listContainer = dialogView.findViewById(R.id.history_list_container);
        TextView tvEmpty = dialogView.findViewById(R.id.tv_empty);
        tvEmpty.setVisibility(View.GONE);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        for (int i = entries.size() - 1; i >= 0; i--) {
            String[] entry = entries.get(i);
            View itemView = getLayoutInflater().inflate(R.layout.item_tree_hole_history, listContainer, false);
            TextView tvContent = itemView.findViewById(R.id.tv_hole_content);
            TextView tvTime = itemView.findViewById(R.id.tv_hole_time);
            tvContent.setText(entry[0]);
            tvTime.setText(sdf.format(new Date(Long.parseLong(entry[1]))));
            listContainer.addView(itemView);
        }

        new AlertDialog.Builder(this)
                .setTitle("📖 树洞历史")
                .setView(dialogView)
                .setPositiveButton("关闭", null)
                .setNeutralButton("清空", (d, w) -> {
                    getSharedPreferences("xinyu", MODE_PRIVATE).edit()
                            .remove("tree_hole_list").apply();
                    Toast.makeText(this, "已清空树洞", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private List<String[]> loadHistory() {
        List<String[]> list = new ArrayList<>();
        String json = getSharedPreferences("xinyu", MODE_PRIVATE).getString("tree_hole_list", "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(new String[]{obj.getString("text"), String.valueOf(obj.getLong("time"))});
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_out_bottom);
    }
}
