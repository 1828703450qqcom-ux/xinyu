package com.xinyu.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.xinyu.app.db.AppDatabase;
import com.xinyu.app.model.TestResult;

public class TestDetailActivity extends AppCompatActivity {

    private TextView tvTitle, tvProgress, btnBack;
    private ProgressBar progressBar;
    private LinearLayout questionsContainer;
    private String username;
    private AppDatabase db;

    private String testId;
    private String testTitle;
    private String[] questions;
    private String[] options;
    private int minScore;
    private int maxScore;
    private RadioGroup[] radioGroups;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_detail);

        SharedPreferences prefs = getSharedPreferences("xinyu", MODE_PRIVATE);
        username = prefs.getString("current_user", "guest");
        db = AppDatabase.getInstance(this);

        testId = getIntent().getStringExtra("test_id");
        testTitle = getIntent().getStringExtra("test_title");
        questions = getIntent().getStringArrayExtra("questions");
        minScore = getIntent().getIntExtra("min_score", 0);
        maxScore = getIntent().getIntExtra("max_score", 3);

        if (maxScore == 3) {
            options = new String[]{"完全没有", "好几天", "一半以上的天数", "几乎每天"};
        } else {
            options = new String[]{"从不", "偶尔", "有时", "经常", "总是"};
        }

        initViews();
        showDescriptionDialog();
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tv_title);
        tvProgress = findViewById(R.id.tv_progress);
        progressBar = findViewById(R.id.progress_bar);
        questionsContainer = findViewById(R.id.questions_container);
        btnBack = findViewById(R.id.btn_back);

        tvTitle.setText(testTitle);
        tvProgress.setText("进度: 0/" + questions.length);
        progressBar.setMax(questions.length);

        btnBack.setOnClickListener(v -> finish());
        findViewById(R.id.btn_submit).setOnClickListener(v -> submitTest());
    }

    private void showDescriptionDialog() {
        String desc = getIntent().getStringExtra("description");
        String suitable = getIntent().getStringExtra("suitable");
        String timeEstimate = getIntent().getStringExtra("time_estimate");

        if (desc == null) desc = "专业心理量表测评";
        if (suitable == null) suitable = "";
        if (timeEstimate == null) timeEstimate = "";

        String message = desc + "\n\n" + suitable + "\n" + timeEstimate
                + "\n\n⚠️ 本测评仅供自我了解参考，不构成医疗诊断。如有严重不适请咨询专业医生。";

        new AlertDialog.Builder(this)
                .setTitle(testTitle)
                .setMessage(message)
                .setPositiveButton("开始测评", (dialog, which) -> {
                    questionsContainer.setVisibility(View.VISIBLE);
                    findViewById(R.id.btn_submit).setVisibility(View.VISIBLE);
                })
                .setNegativeButton("返回", (dialog, which) -> finish())
                .setCancelable(false)
                .show();

        // Hide questions until user clicks start
        questionsContainer.setVisibility(View.INVISIBLE);
        findViewById(R.id.btn_submit).setVisibility(View.INVISIBLE);

        buildQuestions();
    }

    private void buildQuestions() {
        radioGroups = new RadioGroup[questions.length];

        for (int i = 0; i < questions.length; i++) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.color.surface);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.bottomMargin = dpToPx(12);
            card.setLayoutParams(cardParams);
            card.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

            // Question number
            TextView tvNumber = new TextView(this);
            tvNumber.setText("第 " + (i + 1) + " 题 / 共 " + questions.length + " 题");
            tvNumber.setTextColor(getResources().getColor(R.color.primary, null));
            tvNumber.setTextSize(12);
            card.addView(tvNumber);

            // Question text
            TextView tvQuestion = new TextView(this);
            tvQuestion.setText(questions[i]);
            tvQuestion.setTextColor(getResources().getColor(R.color.text_primary, null));
            tvQuestion.setTextSize(16);
            LinearLayout.LayoutParams qParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            qParams.topMargin = dpToPx(8);
            qParams.bottomMargin = dpToPx(12);
            tvQuestion.setLayoutParams(qParams);
            card.addView(tvQuestion);

            // RadioGroup
            RadioGroup rg = new RadioGroup(this);
            rg.setOrientation(RadioGroup.VERTICAL);
            radioGroups[i] = rg;

            for (int j = 0; j < options.length; j++) {
                RadioButton rb = new RadioButton(this);
                rb.setText(options[j]);
                rb.setTextColor(getResources().getColor(R.color.text_primary, null));
                rb.setTextSize(14);
                rb.setPadding(dpToPx(8), dpToPx(8), 0, dpToPx(8));
                rb.setId(View.generateViewId());

                LinearLayout.LayoutParams rbParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                rb.setLayoutParams(rbParams);
                rg.addView(rb);
            }

            rg.setOnCheckedChangeListener((group, checkedId) -> updateProgress());
            card.addView(rg);
            questionsContainer.addView(card);
        }
    }

    private void updateProgress() {
        int answered = 0;
        for (RadioGroup rg : radioGroups) {
            if (rg.getCheckedRadioButtonId() != -1) answered++;
        }
        tvProgress.setText("进度: " + answered + "/" + questions.length);
        progressBar.setProgress(answered);
    }

    private void submitTest() {
        for (int i = 0; i < radioGroups.length; i++) {
            if (radioGroups[i].getCheckedRadioButtonId() == -1) {
                Toast.makeText(this, "请完成第 " + (i + 1) + " 题", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Calculate score
        int totalScore = 0;
        int[] itemScores = new int[questions.length];
        for (int i = 0; i < radioGroups.length; i++) {
            int checkedId = radioGroups[i].getCheckedRadioButtonId();
            RadioButton rb = findViewById(checkedId);
            int score = getSelectedScore(rb);
            itemScores[i] = score;
            totalScore += score;
        }

        int maxPossible = questions.length * maxScore;
        String level, description, suggestion;

        if ("phq9".equals(testId)) {
            if (totalScore <= 4) {
                level = "正常";
                description = "您的情绪状态良好，抑郁症状在正常范围内。";
                suggestion = "继续保持积极乐观的心态，规律作息和适量运动是保持好心情的秘诀。";
            } else if (totalScore <= 9) {
                level = "轻度";
                description = "存在轻度抑郁倾向，建议关注自身情绪变化。";
                suggestion = "建议：① 每天散步30分钟 ② 保持规律作息 ③ 多与朋友交流 ④ 尝试写心情日记记录感受";
            } else if (totalScore <= 14) {
                level = "中度";
                description = "抑郁症状较为明显，建议寻求专业帮助。";
                suggestion = "建议：① 尽快预约心理咨询 ② 告诉信任的人你的感受 ③ 保持基本的生活规律 ④ 避免独处太久";
            } else if (totalScore <= 19) {
                level = "中重度";
                description = "抑郁症状较严重，强烈建议寻求专业帮助。";
                suggestion = "强烈建议：① 立即联系心理咨询师 ② 告知家人或朋友 ③ 全国24小时心理援助热线：400-161-9995";
            } else {
                level = "重度";
                description = "抑郁症状非常严重，请立即寻求专业帮助。";
                suggestion = "请立即行动：① 拨打心理援助热线 ② 联系家人陪同就医 ③ 全国24小时心理援助热线：400-161-9995 ④ 北京心理危机研究与干预中心：010-82951332";
            }
        } else if ("gad7".equals(testId)) {
            if (totalScore <= 4) {
                level = "正常";
                description = "您的焦虑水平在正常范围内。";
                suggestion = "继续保持良好的心态，适当运动和放松有助于维持心理健康。";
            } else if (totalScore <= 9) {
                level = "轻度";
                description = "存在轻度焦虑，建议学习放松技巧。";
                suggestion = "建议：① 尝试4-7-8呼吸法 ② 减少咖啡因摄入 ③ 每天10分钟冥想 ④ 规律运动释放压力";
            } else if (totalScore <= 14) {
                level = "中度";
                description = "焦虑症状较为明显，建议寻求专业帮助。";
                suggestion = "建议：① 预约心理咨询 ② 学习渐进式肌肉放松 ③ 减少过度思考 ④ 建立规律的作息";
            } else {
                level = "重度";
                description = "焦虑症状严重，请尽快寻求专业帮助。";
                suggestion = "请尽快行动：① 联系心理医生 ② 全国24小时心理援助热线：400-161-9995 ③ 避免自我封闭 ④ 告知信任的人";
            }
        } else if ("pss".equals(testId)) {
            if (totalScore <= 13) {
                level = "低压力";
                description = "您的压力水平较低，应对能力良好。";
                suggestion = "继续保持良好的压力管理方式，适当运动和社交是很好的减压方式。";
            } else if (totalScore <= 26) {
                level = "中等压力";
                description = "存在中等程度的压力，建议学习压力管理技巧。";
                suggestion = "建议：① 学习时间管理 ② 设定合理的目标和期望 ③ 每天留出放松时间 ④ 与朋友倾诉烦恼";
            } else {
                level = "高压力";
                description = "压力水平较高，需要采取措施缓解。";
                suggestion = "建议：① 优先处理最重要的事情 ② 学会说\"不\" ③ 寻求家人朋友支持 ④ 必要时咨询心理医生";
            }
        } else {
            if (totalScore <= 5) {
                level = "良好";
                description = "您的睡眠质量良好。";
                suggestion = "继续保持规律的作息习惯，睡前避免使用手机。";
            } else if (totalScore <= 10) {
                level = "一般";
                description = "睡眠质量一般，有一些小问题。";
                suggestion = "建议：① 固定作息时间 ② 睡前1小时不看手机 ③ 保持卧室黑暗安静 ④ 避免午睡超过30分钟";
            } else if (totalScore <= 15) {
                level = "较差";
                description = "睡眠质量较差，影响日常生活。";
                suggestion = "建议：① 建立睡前仪式（如泡脚、听轻音乐） ② 避免睡前饮酒 ③ 白天适当运动 ④ 必要时咨询医生";
            } else {
                level = "很差";
                description = "睡眠质量很差，严重影响生活。";
                suggestion = "建议尽快咨询医生或睡眠专家，排除睡眠障碍。同时保持规律作息，避免咖啡因。";
            }
        }

        // Save result
        TestResult result = new TestResult(username, testId, testTitle, totalScore, level, description);
        db.saveTestResult(result);

        // Sync to server (async)
        final String syncUser = username;
        final String syncTestId = testId;
        final String syncTitle = testTitle;
        final int syncScore = totalScore;
        final String syncLevel = level;
        final String syncDesc = description;
        final long syncTime = result.getCreatedAt();
        new Thread(() -> {
            try {
                com.xinyu.app.util.Reporter.syncAssessment(
                    syncUser, syncTestId, syncTitle, syncScore, syncLevel, syncDesc, syncTime);
            } catch (Exception ignored) {}
        }).start();

        // Launch full-screen result activity
        Intent intent = new Intent(this, TestResultActivity.class);
        intent.putExtra("test_title", testTitle);
        intent.putExtra("total_score", totalScore);
        intent.putExtra("max_score", maxPossible);
        intent.putExtra("level", level);
        intent.putExtra("description", description);
        intent.putExtra("suggestion", suggestion);
        intent.putExtra("test_id", testId);
        intent.putExtra("item_scores", itemScores);
        intent.putExtra("questions", questions);
        startActivity(intent);
        finish();
    }

    private int getSelectedScore(RadioButton rb) {
        String text = rb.getText().toString();
        // Map option text to score
        for (int i = 0; i < options.length; i++) {
            if (text.equals(options[i])) return i;
        }
        return 0;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
