package com.xinyu.app.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.xinyu.app.R;
import com.xinyu.app.adapter.MoodAdapter;
import com.xinyu.app.db.AppDatabase;
import com.xinyu.app.model.MoodRecord;
import com.xinyu.app.model.Pet;
import com.xinyu.app.util.PetManager;
import com.xinyu.app.util.Reporter;
import com.xinyu.app.widget.MoodChartView;
import com.xinyu.app.widget.MoodMixerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MoodFragment extends Fragment {

    private MoodMixerView moodMixer;
    private TextInputEditText moodNoteInput;
    private MaterialButton btnConfirmMood;
    private TextView tvRecordCount, tvCheckinDays, tvHappyRate;
    private RecyclerView moodHistoryList;
    private TextView tvFoodRecommend;
    private View btnFood1, btnFood2, btnFood3, btnFood4, btnFood5, btnFood6;
    private LinearLayout foodMemoryContainer;
    private TextView tvFoodMemoryEmpty;
    private MoodChartView moodChart;

    private SharedPreferences prefs;
    private AppDatabase db;
    private String username;

    private String currentMoodLabel = "平静";
    private String currentEmoji = "😌";

    private MoodAdapter moodAdapter;

    // 7 mood definitions matching the dial
    private final String[] moodLabels = {"惊讶", "受鼓舞", "平静", "厌恶", "害怕", "其他", "开心"};
    private final String[] moodEmojis = {"😲", "💪", "😌", "😖", "😨", "😶", "😊"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mood, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        initData();
        setupMoodDial();
        setupConfirmButton();
        setupMoodHistory();
        refresh();
    }

    private void initViews(View view) {
        moodMixer = view.findViewById(R.id.mood_mixer);
        moodNoteInput = view.findViewById(R.id.mood_note_input);
        btnConfirmMood = view.findViewById(R.id.btn_confirm_mood);
        tvRecordCount = view.findViewById(R.id.tv_record_count);
        tvCheckinDays = view.findViewById(R.id.tv_checkin_days);
        tvHappyRate = view.findViewById(R.id.tv_happy_rate);
        moodHistoryList = view.findViewById(R.id.mood_history_list);
        tvFoodRecommend = view.findViewById(R.id.tv_food_recommend);
        btnFood1 = view.findViewById(R.id.btn_food_1);
        btnFood2 = view.findViewById(R.id.btn_food_2);
        btnFood3 = view.findViewById(R.id.btn_food_3);
        btnFood4 = view.findViewById(R.id.btn_food_4);
        btnFood5 = view.findViewById(R.id.btn_food_5);
        btnFood6 = view.findViewById(R.id.btn_food_6);
        foodMemoryContainer = view.findViewById(R.id.food_memory_container);
        tvFoodMemoryEmpty = view.findViewById(R.id.tv_food_memory_empty);
        moodChart = view.findViewById(R.id.mood_chart);
        setupFoodButtons();
        loadFoodMemories();
    }

    private void initData() {
        prefs = requireContext().getSharedPreferences("xinyu", 0);
        db = AppDatabase.getInstance(requireContext());
        username = prefs.getString("current_user", "guest");

        // Set initial mood from dial default (value 4 = 平静 in 7-mood system is index 2)
        currentMoodLabel = moodLabels[2];
        currentEmoji = moodEmojis[2];
    }

    private void setupMoodDial() {
        moodMixer.setOnMoodMixListener(new MoodMixerView.OnMoodMixListener() {
            @Override
            public void onMoodMixed(String moodLabel, String moodEmoji, int[] values) {
                currentMoodLabel = moodLabel;
                currentEmoji = moodEmoji;
                btnConfirmMood.setText("记录此刻心情 · " + currentEmoji);
            }
        });
    }

    private void updateFoodRecommendation(int moodIndex) {
        String[][] recommendations = {
                {"😲", "来点刺激的！", "🌮 刺身/烧烤"},
                {"💪", "补充能量！", "🥗 健身餐/沙拉"},
                {"😌", "享受当下~", "🍵 奶茶/甜品"},
                {"😖", "吃点好的治愈一下", "🍜 暖胃面食"},
                {"😨", "别怕，吃顿好的", "🍲 火锅暖暖胃"},
                {"😶", "随心选~", "🍱 丰盛便当"},
                {"😊", "开心加倍！", "🍰 蛋糕/甜品庆祝"}
        };
        if (moodIndex >= 0 && moodIndex < recommendations.length) {
            String[] rec = recommendations[moodIndex];
            String mood = moodLabels[moodIndex];
            String lastFood = getLastFoodForMood(mood);
            if (lastFood != null) {
                tvFoodRecommend.setText("上次你" + mood + "时吃了 " + lastFood + "\n今天试试：" + rec[2] + "～");
            } else {
                tvFoodRecommend.setText("根据你的心情推荐：" + rec[2] + "～");
            }
        }
    }

    private void setupConfirmButton() {
        btnConfirmMood.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveMood();
            }
        });
    }

    // Food data: each mood maps to a recommendation
    private final String[][] foodRecommendations = {
            {"😲", "来点刺激的！", "🌮 刺身/烧烤", "mishop://"},
            {"💪", "补充能量！", "🥗 健身餐/沙拉", "mishop://"},
            {"😌", "享受当下~", "🍵 奶茶/甜品", "mishop://"},
            {"😖", "吃点好的治愈一下", "🍜 暖胃面食", "mishop://"},
            {"😨", "别怕，吃顿好的", "🍲 火锅暖暖胃", "mishop://"},
            {"😶", "随心选~", "🍱 丰盛便当", "mishop://"},
            {"😊", "开心加倍！", "🍰 蛋糕/甜品庆祝", "mishop://"}
    };

    // Food items for each category
    private final String[][] foodItems = {
            {"🍰 蛋糕", "🧇 华夫饼", "🧁 马卡龙", "🍮 布丁", "🍩 甜甜圈", "🥞 舒芙蕾"},
            {"🍜 拉面", "🍝 意面", "🥟 饺子", "🍛 咖喱饭", "🍝 炒河粉", "🫕 关东煮"},
            {"🥗 减脂沙拉", "🥦 低卡便当", "🥑 牛油果吐司", "🫛 豆腐沙拉", "🥒 蔬果汁", "🥜 坚果拼盘"},
            {"🍲 麻辣火锅", "🥘 砂锅粥", "🫕 酸菜鱼", "🍜 螺蛳粉", "🥡 冒菜", "🍢 串串香"},
            {"🧋 珍珠奶茶", "🍵 抹茶拿铁", "☕ 生椰咖啡", "🫧 气泡水", "🍹 杨枝甘露", "🥛 芋泥波波"},
            {"🍱 日式便当", "🍙 饭团", "🥘 黄焖鸡", "🍚 红烧排骨饭", "🍛 咖喱鸡排饭", "🍜 兰州拌面"}
    };

    private void setupFoodButtons() {
        String[] foodNames = {"🍰 甜品", "🍜 面食", "🥗 轻食", "🍲 火锅", "🍵 奶茶", "🍱 便当"};
        View[] buttons = {btnFood1, btnFood2, btnFood3, btnFood4, btnFood5, btnFood6};

        for (int i = 0; i < buttons.length; i++) {
            final int idx = i;
            final String name = foodNames[i];
            buttons[i].setOnClickListener(v -> showFoodDialog(name, foodItems[idx]));
        }
    }

    private void showFoodDialog(String title, String[] items) {
        new AlertDialog.Builder(requireContext())
                .setTitle(title + " 推荐")
                .setItems(items, (dialog, which) -> {
                    String food = items[which];
                    Toast.makeText(requireContext(), "今天就吃 " + food + " 吧！", Toast.LENGTH_SHORT).show();
                    tvFoodRecommend.setText("为你推荐：" + food + "～");
                    // Save food memory
                    saveFoodMemory(currentMoodLabel, currentEmoji, food);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void saveFoodMemory(String mood, String emoji, String food) {
        try {
            String json = prefs.getString("food_memories", "[]");
            JSONArray arr = new JSONArray(json);
            JSONObject obj = new JSONObject();
            obj.put("mood", mood);
            obj.put("emoji", emoji);
            obj.put("food", food);
            obj.put("time", new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(new Date()));
            // Add to front, keep max 30
            JSONArray newArr = new JSONArray();
            newArr.put(obj);
            for (int i = 0; i < arr.length() && i < 29; i++) {
                newArr.put(arr.getJSONObject(i));
            }
            prefs.edit().putString("food_memories", newArr.toString()).apply();
            loadFoodMemories();
        } catch (Exception e) {
            // ignore
        }
    }

    private String getLastFoodForMood(String mood) {
        try {
            String json = prefs.getString("food_memories", "[]");
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                if (obj.getString("mood").equals(mood)) {
                    return obj.getString("emoji") + " " + obj.getString("food") + " (" + obj.getString("time") + ")";
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    private void loadFoodMemories() {
        try {
            String json = prefs.getString("food_memories", "[]");
            JSONArray arr = new JSONArray(json);

            // Clear old items (keep the empty hint)
            while (foodMemoryContainer.getChildCount() > 1) {
                foodMemoryContainer.removeViewAt(1);
            }

            if (arr.length() == 0) {
                tvFoodMemoryEmpty.setVisibility(View.VISIBLE);
                return;
            }

            tvFoodMemoryEmpty.setVisibility(View.GONE);
            int showCount = Math.min(arr.length(), 5);
            for (int i = 0; i < showCount; i++) {
                JSONObject obj = arr.getJSONObject(i);
                TextView item = new TextView(requireContext());
                item.setText(obj.getString("emoji") + " " + obj.getString("mood") + " → " + obj.getString("food") + "  " + obj.getString("time"));
                item.setTextSize(12);
                item.setTextColor(getResources().getColor(R.color.text_secondary, null));
                item.setPadding(0, dpToPx(4), 0, dpToPx(4));
                foodMemoryContainer.addView(item);
            }
        } catch (Exception e) {
            tvFoodMemoryEmpty.setVisibility(View.VISIBLE);
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void saveMood() {
        String note = "";
        if (moodNoteInput.getText() != null) {
            note = moodNoteInput.getText().toString().trim();
        }

        // Calculate mood value from mixer (0-100 → 1-5)
        int[] vals = moodMixer.getValues();
        int avg = 0;
        for (int v : vals) avg += v;
        avg /= vals.length;
        int moodValue = Math.max(1, Math.min(5, avg / 20 + 1));
        MoodRecord record = new MoodRecord(username, moodValue, currentMoodLabel, currentEmoji, note);
        db.saveMood(record);
        Reporter.report(requireContext(), "mood_record", currentMoodLabel);

        // Give pet exp for recording mood
        PetManager pm = PetManager.getInstance(requireContext());
        Pet pet = pm.getPet();
        pet.addExp(15);
        pm.savePet(pet);

        // Cute encouraging message
        String[] encouragements = {
                "已记录！" + currentEmoji + " 今天辛苦啦~",
                "收到！" + currentEmoji + " 你的心情我记住啦~",
                "记好啦！" + currentEmoji + " 要天天开心哦~",
                "已保存！" + currentEmoji + " 每一天都值得记录~",
                "棒棒的！" + currentEmoji + " 记录心情是好习惯~"
        };
        String msg = encouragements[(int)(Math.random() * encouragements.length)];
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();

        // Confetti animation
        showConfetti();

        moodNoteInput.setText("");
        refresh();
    }

    private void showConfetti() {
        if (getView() == null) return;
        FrameLayout container = (FrameLayout) getView().getParent();
        if (container == null) return;

        String[] confettiEmojis = {"🌸", "✨", "💖", "🎀", "⭐", "🌷", "💫", "🦋"};
        for (int i = 0; i < 12; i++) {
            final TextView confetti = new TextView(requireContext());
            confetti.setText(confettiEmojis[i % confettiEmojis.length]);
            confetti.setTextSize(16 + (int)(Math.random() * 12));

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT);
            params.leftMargin = (int)(Math.random() * container.getWidth());
            params.topMargin = -20;
            confetti.setLayoutParams(params);
            confetti.setAlpha(0.9f);
            container.addView(confetti);

            final float startX = params.leftMargin;
            final float endY = container.getHeight() + 50;
            final float drift = (float)(Math.random() - 0.5) * 200;
            final long duration = 1500 + (long)(Math.random() * 1000);

            confetti.animate()
                    .translationY(endY)
                    .translationX(drift)
                    .rotation((float)(Math.random() * 360))
                    .alpha(0f)
                    .setDuration(duration)
                    .setStartDelay(i * 80)
                    .withEndAction(() -> container.removeView(confetti))
                    .start();
        }
    }

    private void setupMoodHistory() {
        moodAdapter = new MoodAdapter();
        moodHistoryList.setLayoutManager(new LinearLayoutManager(requireContext()));
        moodHistoryList.setAdapter(moodAdapter);
    }

    public void refresh() {
        if (getContext() == null || prefs == null) return;

        // Load stats
        List<MoodRecord> allMoods = db.getMoods(username, 1000);
        int totalRecords = allMoods.size();
        tvRecordCount.setText(String.valueOf(totalRecords));

        // Checkin count
        int checkinCount = db.getCheckinCount(username);
        tvCheckinDays.setText(String.valueOf(checkinCount));

        // Happy rate (value >= 6 is 开心, value >= 2 is 受鼓舞)
        int happyCount = 0;
        for (MoodRecord mood : allMoods) {
            if (mood.getMoodValue() >= 6 || mood.getMoodValue() == 2) {
                happyCount++;
            }
        }
        int happyRate = totalRecords > 0 ? (happyCount * 100 / totalRecords) : 0;
        tvHappyRate.setText(happyRate + "%");

        // Load history
        List<MoodRecord> recentMoods = db.getMoods(username, 50);
        moodAdapter.setData(recentMoods);

        // Update chart - last 7 days
        updateChart(recentMoods);
    }

    private void updateChart(List<MoodRecord> moods) {
        // Group moods by day for last 7 days
        java.util.Map<String, Float> dailyMoods = new java.util.LinkedHashMap<>();
        java.text.SimpleDateFormat dayFmt = new java.text.SimpleDateFormat("MM/dd", Locale.CHINA);
        java.text.SimpleDateFormat fullFmt = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);

        // Initialize last 7 days
        java.util.Calendar cal = java.util.Calendar.getInstance();
        for (int i = 6; i >= 0; i--) {
            java.util.Calendar c = java.util.Calendar.getInstance();
            c.add(java.util.Calendar.DAY_OF_YEAR, -i);
            String key = fullFmt.format(c.getTime());
            dailyMoods.put(key, -1f); // -1 means no data
        }

        // Fill with actual mood data
        for (MoodRecord m : moods) {
            try {
                java.util.Date date = new java.util.Date(m.getCreatedAt() * 1000);
                String key = fullFmt.format(date);
                if (dailyMoods.containsKey(key)) {
                    float val = m.getMoodValue();
                    // Convert mood value (0-6) to 0-100 scale
                    float scaled = val * 100f / 6f;
                    dailyMoods.put(key, scaled);
                }
            } catch (Exception e) {
                // skip
            }
        }

        // Build chart data
        java.util.List<Float> values = new java.util.ArrayList<>();
        java.util.List<String> labels = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, Float> entry : dailyMoods.entrySet()) {
            if (entry.getValue() >= 0) {
                values.add(entry.getValue());
            } else {
                values.add(0f); // no data = 0
            }
            // Format label as MM/dd
            try {
                java.util.Date d = fullFmt.parse(entry.getKey());
                labels.add(dayFmt.format(d));
            } catch (Exception e) {
                labels.add(entry.getKey().substring(5));
            }
        }

        moodChart.setData(values, labels);
    }
}
