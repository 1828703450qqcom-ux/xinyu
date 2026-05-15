package com.xinyu.app;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.xinyu.app.db.AppDatabase;
import com.xinyu.app.model.MoodRecord;
import com.xinyu.app.model.TestResult;
import com.xinyu.app.widget.MoodTrendView;
import com.xinyu.app.widget.TrendChartView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DataReportActivity extends AppCompatActivity {

    private String username;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_report);

        username = getSharedPreferences("xinyu", MODE_PRIVATE).getString("current_user", "guest");
        db = AppDatabase.getInstance(this);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        loadMoodTrend();
        loadAssessmentTrends();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMoodTrend();
        loadAssessmentTrends();
    }

    private void loadMoodTrend() {
        List<MoodRecord> moods = db.getMoods(username, 30);
        MoodTrendView moodTrend = findViewById(R.id.mood_trend);
        TextView tvMoodCount = findViewById(R.id.tv_mood_count);

        tvMoodCount.setText("共记录 " + moods.size() + " 次心情");

        if (moods.size() < 2) {
            return;
        }

        // Reverse to chronological order
        List<MoodRecord> sorted = new ArrayList<>(moods);
        Collections.reverse(sorted);

        // Take last 14 entries max for display
        int start = Math.max(0, sorted.size() - 14);
        List<MoodRecord> display = sorted.subList(start, sorted.size());

        List<Integer> values = new ArrayList<>();
        List<String> dates = new ArrayList<>();
        List<String> emojis = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd", Locale.getDefault());

        for (MoodRecord m : display) {
            values.add(m.getMoodValue());
            dates.add(sdf.format(new Date(m.getCreatedAt())));
            emojis.add(m.getEmoji() != null ? m.getEmoji() : "");
        }

        moodTrend.setData(values, dates, emojis);
    }

    private void loadAssessmentTrends() {
        List<TestResult> results = db.getTestResults(username, 100);

        // Group by test type
        List<TestResult> phq9List = new ArrayList<>();
        List<TestResult> gad7List = new ArrayList<>();
        List<TestResult> pssList = new ArrayList<>();
        List<TestResult> sleepList = new ArrayList<>();

        for (TestResult r : results) {
            switch (r.getTestId()) {
                case "phq9": phq9List.add(r); break;
                case "gad7": gad7List.add(r); break;
                case "pss": pssList.add(r); break;
                case "sleep": sleepList.add(r); break;
            }
        }

        // Reverse each to chronological order
        Collections.reverse(phq9List);
        Collections.reverse(gad7List);
        Collections.reverse(pssList);
        Collections.reverse(sleepList);

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd", Locale.getDefault());

        // PHQ-9
        TrendChartView trendPhq9 = findViewById(R.id.trend_phq9);
        setTrendData(trendPhq9, phq9List, 27, sdf);

        // GAD-7
        TrendChartView trendGad7 = findViewById(R.id.trend_gad7);
        setTrendData(trendGad7, gad7List, 21, sdf);

        // PSS
        TrendChartView trendPss = findViewById(R.id.trend_pss);
        setTrendData(trendPss, pssList, 40, sdf);

        // Sleep
        TrendChartView trendSleep = findViewById(R.id.trend_sleep);
        setTrendData(trendSleep, sleepList, 20, sdf);
    }

    private void setTrendData(TrendChartView view, List<TestResult> results, int maxScore, SimpleDateFormat sdf) {
        if (results.isEmpty()) return;

        // Take last 10 entries max
        int start = Math.max(0, results.size() - 10);
        List<TestResult> display = results.subList(start, results.size());

        List<Float> scores = new ArrayList<>();
        List<String> dates = new ArrayList<>();
        for (TestResult r : display) {
            scores.add((float) r.getScore());
            dates.add(sdf.format(new Date(r.getCreatedAt())));
        }
        view.setData(scores, dates, maxScore);
    }
}
