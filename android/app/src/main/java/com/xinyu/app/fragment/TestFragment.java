package com.xinyu.app.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.xinyu.app.R;
import com.xinyu.app.TestDetailActivity;
import com.xinyu.app.adapter.TestAdapter;
import com.xinyu.app.db.AppDatabase;
import com.xinyu.app.model.TestResult;

import java.util.List;

public class TestFragment extends Fragment {

    private MaterialCardView cardPhq9, cardGad7, cardPss, cardSleep;
    private RecyclerView testHistoryList;
    private TestAdapter testAdapter;
    private SharedPreferences prefs;
    private AppDatabase db;
    private String username;

    private final String[] phq9Questions = {
        "做事时提不起劲或没有兴趣",
        "感到心情低落、沮丧或绝望",
        "入睡困难、睡不安稳或睡眠过多",
        "感觉疲倦或没有活力",
        "食欲不振或吃太多",
        "觉得自己很糟或让自己或家人失望",
        "注意力难以集中",
        "动作或说话速度变得缓慢，或坐立不安",
        "有不如死掉或伤害自己的念头"
    };

    private final String[] gad7Questions = {
        "感到紧张、焦虑或急切",
        "不能停止或控制担忧",
        "对各种各样的事情担忧过多",
        "很难放松下来",
        "由于不安而无法静坐",
        "变得容易烦恼或急躁",
        "感到似乎将有可怕的事情发生"
    };

    private final String[] pssQuestions = {
        "因为意想不到的事情而感到心烦意乱",
        "感到无法控制生活中的重要事情",
        "感到紧张和压力大",
        "成功地处理了恼人的生活麻烦",
        "感到有效地应对了生活中重要的变化",
        "对自己处理个人问题的能力感到自信",
        "觉得事情按照你的意愿进行",
        "发现不能处理所有自己必须做的事情",
        "能够控制生活中的烦恼",
        "觉得困难堆积太多而无法克服"
    };

    private final String[] sleepQuestions = {
        "入睡困难（超过30分钟）",
        "夜间容易醒来",
        "早醒后难以再次入睡",
        "白天感到疲倦或精力不足",
        "睡眠质量让你不满意"
    };

    private final String[] options03 = {"完全没有", "好几天", "一半以上的天数", "几乎每天"};
    private final String[] options04 = {"从不", "偶尔", "有时", "经常", "总是"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_test, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = requireContext().getSharedPreferences("xinyu", 0);
        db = AppDatabase.getInstance(requireContext());
        username = prefs.getString("current_user", "guest");

        cardPhq9 = view.findViewById(R.id.card_phq9);
        cardGad7 = view.findViewById(R.id.card_gad7);
        cardPss = view.findViewById(R.id.card_pss);
        cardSleep = view.findViewById(R.id.card_sleep);
        testHistoryList = view.findViewById(R.id.test_history_list);

        view.findViewById(R.id.btn_report).setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), com.xinyu.app.ReportActivity.class));
        });

        cardPhq9.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { openTest("phq9", "PHQ-9 抑郁量表", phq9Questions, 0, 3); }
        });
        cardGad7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { openTest("gad7", "GAD-7 焦虑量表", gad7Questions, 0, 3); }
        });
        cardPss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { openTest("pss", "PSS 压力量表", pssQuestions, 0, 4); }
        });
        cardSleep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { openTest("sleep", "睡眠质量评估", sleepQuestions, 0, 4); }
        });

        testAdapter = new TestAdapter();
        testHistoryList.setLayoutManager(new LinearLayoutManager(requireContext()));
        testHistoryList.setAdapter(testAdapter);
        refresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void openTest(String testId, String testTitle, String[] questions, int minScore, int maxScore) {
        Intent intent = new Intent(requireContext(), TestDetailActivity.class);
        intent.putExtra("test_id", testId);
        intent.putExtra("test_title", testTitle);
        intent.putExtra("questions", questions);
        intent.putExtra("min_score", minScore);
        intent.putExtra("max_score", maxScore);

        // Test descriptions
        String desc;
        String suitable;
        String timeEstimate;
        switch (testId) {
            case "phq9":
                desc = "PHQ-9 是国际通用的抑郁筛查量表，用于评估过去两周内的抑郁症状严重程度。";
                suitable = "适合：感到情绪低落、兴趣减退、疲劳的人群";
                timeEstimate = "预计用时：2-3 分钟";
                break;
            case "gad7":
                desc = "GAD-7 是广泛性焦虑障碍量表，用于评估过去两周内的焦虑水平。";
                suitable = "适合：经常感到紧张、担忧、难以放松的人群";
                timeEstimate = "预计用时：1-2 分钟";
                break;
            case "pss":
                desc = "PSS-10 是压力量表，用于评估过去一个月内的生活压力感受。";
                suitable = "适合：感到压力大、生活节奏快、难以应对的人群";
                timeEstimate = "预计用时：2-3 分钟";
                break;
            default:
                desc = "睡眠质量评估用于了解你的睡眠状况和潜在问题。";
                suitable = "适合：有入睡困难、易醒、白天疲倦等睡眠问题的人群";
                timeEstimate = "预计用时：1 分钟";
                break;
        }
        intent.putExtra("description", desc);
        intent.putExtra("suitable", suitable);
        intent.putExtra("time_estimate", timeEstimate);
        startActivity(intent);
    }

    public void refresh() {
        if (getContext() == null || prefs == null) return;
        List<TestResult> results = db.getTestResults(username, 100);
        testAdapter.setData(results);
    }
}
