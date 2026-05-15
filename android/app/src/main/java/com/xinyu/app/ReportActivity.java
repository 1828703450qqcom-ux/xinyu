package com.xinyu.app;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.xinyu.app.db.AppDatabase;
import com.xinyu.app.model.TestResult;
import com.xinyu.app.widget.BarChartView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportActivity extends AppCompatActivity {

    private String username;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        username = getSharedPreferences("xinyu", MODE_PRIVATE).getString("current_user", "guest");
        db = AppDatabase.getInstance(this);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        loadReport();
    }

    private void loadReport() {
        List<TestResult> results = db.getTestResults(username, 200);

        // Group by testId, keep latest
        Map<String, TestResult> latest = new HashMap<>();
        for (TestResult r : results) {
            if (!latest.containsKey(r.getTestId())) {
                latest.put(r.getTestId(), r);
            }
        }

        // PHQ-9
        TestResult phq9 = latest.get("phq9");
        updateCard(phq9, 27,
            R.id.tv_phq9_level, R.id.pb_phq9, R.id.tv_phq9_score, R.id.tv_phq9_desc, "#7986CB");

        // GAD-7
        TestResult gad7 = latest.get("gad7");
        updateCard(gad7, 21,
            R.id.tv_gad7_level, R.id.pb_gad7, R.id.tv_gad7_score, R.id.tv_gad7_desc, "#FFB74D");

        // PSS
        TestResult pss = latest.get("pss");
        updateCard(pss, 40,
            R.id.tv_pss_level, R.id.pb_pss, R.id.tv_pss_score, R.id.tv_pss_desc, "#EF5350");

        // Sleep
        TestResult sleep = latest.get("sleep");
        updateCard(sleep, 20,
            R.id.tv_sleep_level, R.id.pb_sleep, R.id.tv_sleep_score, R.id.tv_sleep_desc, "#AB47BC");

        // Bar chart
        BarChartView barChart = findViewById(R.id.bar_chart);
        List<Float> barValues = new ArrayList<>();
        List<String> barLabels = new ArrayList<>();
        List<Integer> barColors = new ArrayList<>();

        barValues.add(phq9 != null ? (float) phq9.getScore() : 0f);
        barLabels.add("抑郁");
        barColors.add(Color.parseColor("#7986CB"));

        barValues.add(gad7 != null ? (float) gad7.getScore() : 0f);
        barLabels.add("焦虑");
        barColors.add(Color.parseColor("#FFB74D"));

        barValues.add(pss != null ? (float) pss.getScore() : 0f);
        barLabels.add("压力");
        barColors.add(Color.parseColor("#EF5350"));

        barValues.add(sleep != null ? (float) sleep.getScore() : 0f);
        barLabels.add("睡眠");
        barColors.add(Color.parseColor("#AB47BC"));

        barChart.setData(barValues, barLabels, barColors, 40f);

        // Overall status
        TextView tvOverallEmoji = findViewById(R.id.tv_overall_emoji);
        TextView tvOverallStatus = findViewById(R.id.tv_overall_status);
        TextView tvOverallDesc = findViewById(R.id.tv_overall_desc);

        int completedCount = latest.size();
        if (completedCount == 0) {
            tvOverallEmoji.setText("🌈");
            tvOverallStatus.setText("还没有做过测评");
            tvOverallDesc.setText("完成下面的量表后，这里会显示综合报告");
        } else {
            // Calculate overall risk level
            int riskScore = 0;
            if (phq9 != null) riskScore += phq9.getScore();
            if (gad7 != null) riskScore += gad7.getScore();
            if (pss != null) riskScore += pss.getScore() / 2;
            if (sleep != null) riskScore += sleep.getScore();

            if (riskScore < 15) {
                tvOverallEmoji.setText("😊");
                tvOverallStatus.setText("状态良好");
                tvOverallDesc.setText("各项指标均在正常范围内，继续保持！");
            } else if (riskScore < 30) {
                tvOverallEmoji.setText("🙂");
                tvOverallStatus.setText("基本正常");
                tvOverallDesc.setText("大部分指标正常，注意保持健康的生活习惯");
            } else if (riskScore < 50) {
                tvOverallEmoji.setText("😐");
                tvOverallStatus.setText("需要关注");
                tvOverallDesc.setText("部分指标偏高，建议适当调整作息和心态");
            } else {
                tvOverallEmoji.setText("😟");
                tvOverallStatus.setText("建议寻求帮助");
                tvOverallDesc.setText("多项指标较高，建议咨询专业人士获取帮助");
            }
        }
    }

    private void updateCard(TestResult result, int maxScore,
                           int levelId, int progressId, int scoreId, int descId, String colorHex) {
        TextView tvLevel = findViewById(levelId);
        ProgressBar pb = findViewById(progressId);
        TextView tvScore = findViewById(scoreId);
        TextView tvDesc = findViewById(descId);

        if (result != null) {
            tvLevel.setText(result.getLevel());
            pb.setProgress(result.getScore());
            tvScore.setText("得分: " + result.getScore() + "/" + maxScore);
            String desc = result.getDescription();
            if (desc != null && desc.length() > 80) {
                desc = desc.substring(0, 80) + "...";
            }
            tvDesc.setText(desc != null ? desc : "");
        } else {
            tvLevel.setText("未测评");
            pb.setProgress(0);
            tvScore.setText("得分: --/" + maxScore);
            tvDesc.setText("暂无测评记录");
        }
    }
}
