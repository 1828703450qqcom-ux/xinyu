package com.xinyu.app;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.xinyu.app.db.AppDatabase;
import com.xinyu.app.model.TestResult;
import com.xinyu.app.widget.TrendChartView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TestResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_result);

        String testTitle = getIntent().getStringExtra("test_title");
        int totalScore = getIntent().getIntExtra("total_score", 0);
        int maxScore = getIntent().getIntExtra("max_score", 27);
        String level = getIntent().getStringExtra("level");
        String description = getIntent().getStringExtra("description");
        String suggestion = getIntent().getStringExtra("suggestion");
        String testId = getIntent().getStringExtra("test_id");
        int[] itemScores = getIntent().getIntArrayExtra("item_scores");
        String[] questions = getIntent().getStringArrayExtra("questions");

        // Header
        ((TextView) findViewById(R.id.tv_result_title)).setText(testTitle);

        // Score
        ((TextView) findViewById(R.id.tv_score_large)).setText(String.valueOf(totalScore));
        ((TextView) findViewById(R.id.tv_score_total)).setText("/ " + maxScore + " 分");

        // Level badge
        TextView tvLevel = findViewById(R.id.tv_level);
        tvLevel.setText(level);
        int levelColor = getLevelColor(level);
        tvLevel.getBackground().setTint(levelColor);

        // Progress bar
        ProgressBar progressBar = findViewById(R.id.progress_score);
        int progress = maxScore > 0 ? (totalScore * 100 / maxScore) : 0;
        progressBar.setProgress(progress);

        // Description
        ((TextView) findViewById(R.id.tv_description)).setText(description);

        // Suggestion
        ((TextView) findViewById(R.id.tv_suggestion)).setText(suggestion);

        // Item scores
        LinearLayout itemContainer = findViewById(R.id.item_scores_container);
        if (itemScores != null && questions != null) {
            for (int i = 0; i < itemScores.length; i++) {
                View itemView = getLayoutInflater().inflate(R.layout.item_test_score, itemContainer, false);
                TextView tvQ = itemView.findViewById(R.id.tv_item_question);
                TextView tvS = itemView.findViewById(R.id.tv_item_score);
                ProgressBar pb = itemView.findViewById(R.id.progress_item);

                tvQ.setText((i + 1) + ". " + questions[i]);
                tvS.setText(itemScores[i] + " 分");
                int maxItemScore = "pss".equals(testId) || "sleep".equals(testId) ? 4 : 3;
                pb.setMax(maxItemScore);
                pb.setProgress(itemScores[i]);

                itemContainer.addView(itemView);
            }
        }

        // Load history for trend chart
        SharedPreferences prefs = getSharedPreferences("xinyu", MODE_PRIVATE);
        String username = prefs.getString("current_user", "guest");
        AppDatabase db = AppDatabase.getInstance(this);
        List<TestResult> history = db.getTestResults(username, 100);

        // Filter by test type and reverse to chronological order
        List<Float> trendScores = new ArrayList<>();
        List<String> trendDates = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd", Locale.getDefault());

        for (int i = history.size() - 1; i >= 0; i--) {
            TestResult r = history.get(i);
            if (testId.equals(r.getTestId())) {
                trendScores.add((float) r.getScore());
                trendDates.add(sdf.format(new Date(r.getCreatedAt())));
            }
        }

        TrendChartView chart = findViewById(R.id.trend_chart);
        TextView tvEmpty = findViewById(R.id.tv_history_empty);

        if (trendScores.size() >= 2) {
            chart.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);

            // Find max for this test
            int testMax = "phq9".equals(testId) ? 27 : "gad7".equals(testId) ? 21 : "pss".equals(testId) ? 40 : 20;
            chart.setData(trendScores, trendDates, testMax);
        } else {
            chart.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        }

        // Done button
        findViewById(R.id.btn_done).setOnClickListener(v -> finish());
    }

    private int getLevelColor(String level) {
        switch (level) {
            case "正常": case "良好": case "低压力": return 0xFF81C784;
            case "轻度": case "一般": return 0xFFFFB74D;
            case "中度": case "较差": case "中等压力": return 0xFFFF8A65;
            case "中重度": case "很差": case "高压力": return 0xFFEF5350;
            case "重度": return 0xFFC62828;
            default: return 0xFFFF8A9B;
        }
    }
}
