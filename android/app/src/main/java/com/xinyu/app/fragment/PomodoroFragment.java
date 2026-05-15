package com.xinyu.app.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xinyu.app.R;
import com.xinyu.app.model.Pet;
import com.xinyu.app.util.PetManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PomodoroFragment extends Fragment {

    private TextView tvTimer, tvTimerLabel, tvTimerSession;
    private TextView btnStart, btnReset, btnSkip;
    private TextView tvTotalPomodoros, tvTotalMinutes, tvPetExp;
    private RecyclerView recordList;

    private CountDownTimer timer;
    private boolean isRunning = false;
    private boolean isBreak = false;
    private long timeLeftMillis = 25 * 60 * 1000;
    private int sessionCount = 1;
    private int completedPomodoros = 0;
    private int totalMinutes = 0;

    private static final long POMODORO_TIME = 25 * 60 * 1000;
    private static final long SHORT_BREAK = 5 * 60 * 1000;
    private static final long LONG_BREAK = 15 * 60 * 1000;

    private SharedPreferences prefs;
    private List<String[]> records = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pomodoro, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = requireContext().getSharedPreferences("xinyu", 0);
        initViews(view);
        loadStats();
    }

    private void initViews(View view) {
        tvTimer = view.findViewById(R.id.tv_timer);
        tvTimerLabel = view.findViewById(R.id.tv_timer_label);
        tvTimerSession = view.findViewById(R.id.tv_timer_session);
        btnStart = view.findViewById(R.id.btn_start);
        btnReset = view.findViewById(R.id.btn_reset);
        btnSkip = view.findViewById(R.id.btn_skip);
        tvTotalPomodoros = view.findViewById(R.id.tv_total_pomodoros);
        tvTotalMinutes = view.findViewById(R.id.tv_total_minutes);
        tvPetExp = view.findViewById(R.id.tv_pet_exp);
        recordList = view.findViewById(R.id.record_list);

        recordList.setLayoutManager(new LinearLayoutManager(requireContext()));

        btnStart.setOnClickListener(v -> toggleTimer());
        btnReset.setOnClickListener(v -> resetTimer());
        btnSkip.setOnClickListener(v -> skipToNext());

        updateTimerDisplay();
    }

    private void toggleTimer() {
        if (isRunning) {
            pauseTimer();
        } else {
            startTimer();
        }
    }

    private void startTimer() {
        isRunning = true;
        btnStart.setText("⏸");

        timer = new CountDownTimer(timeLeftMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftMillis = millisUntilFinished;
                updateTimerDisplay();
            }

            @Override
            public void onFinish() {
                isRunning = false;
                if (!isBreak) {
                    // Pomodoro completed
                    completedPomodoros++;
                    totalMinutes += 25;
                    addRecord("🍅 完成第 " + completedPomodoros + " 个番茄", "25分钟");

                    // Give pet exp
                    PetManager pm = PetManager.getInstance(requireContext());
                    Pet pet = pm.getPet();
                    pet.study(25);
                    pm.savePet(pet);

                    Toast.makeText(requireContext(), "🍅 太棒了！完成一个番茄！+25经验", Toast.LENGTH_SHORT).show();

                    // Start break
                    isBreak = true;
                    if (completedPomodoros % 4 == 0) {
                        timeLeftMillis = LONG_BREAK;
                        tvTimerLabel.setText("☕ 长休息");
                    } else {
                        timeLeftMillis = SHORT_BREAK;
                        tvTimerLabel.setText("🌿 短休息");
                    }
                } else {
                    // Break completed
                    isBreak = false;
                    sessionCount++;
                    timeLeftMillis = POMODORO_TIME;
                    tvTimerLabel.setText("🍅 专注中");
                    Toast.makeText(requireContext(), "休息结束，开始下一轮！", Toast.LENGTH_SHORT).show();
                }
                updateTimerDisplay();
                updateStats();
                btnStart.setText("▶");
            }
        }.start();
    }

    private void pauseTimer() {
        if (timer != null) timer.cancel();
        isRunning = false;
        btnStart.setText("▶");
    }

    private void resetTimer() {
        if (timer != null) timer.cancel();
        isRunning = false;
        isBreak = false;
        timeLeftMillis = POMODORO_TIME;
        sessionCount = 1;
        tvTimerLabel.setText("🍅 专注中");
        btnStart.setText("▶");
        updateTimerDisplay();
    }

    private void skipToNext() {
        if (timer != null) timer.cancel();
        isRunning = false;

        if (!isBreak) {
            isBreak = true;
            timeLeftMillis = SHORT_BREAK;
            tvTimerLabel.setText("🌿 短休息");
        } else {
            isBreak = false;
            sessionCount++;
            timeLeftMillis = POMODORO_TIME;
            tvTimerLabel.setText("🍅 专注中");
        }
        btnStart.setText("▶");
        updateTimerDisplay();
    }

    private void updateTimerDisplay() {
        int minutes = (int) (timeLeftMillis / 1000) / 60;
        int seconds = (int) (timeLeftMillis / 1000) % 60;
        tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
        tvTimerSession.setText("第 " + sessionCount + " 轮");
    }

    private void updateStats() {
        tvTotalPomodoros.setText(String.valueOf(completedPomodoros));
        tvTotalMinutes.setText(String.valueOf(totalMinutes));
        tvPetExp.setText("+" + (completedPomodoros * 25));
    }

    private void loadStats() {
        // Check if today's date matches saved date
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String savedDate = prefs.getString("pomodoro_date", "");
        if (!today.equals(savedDate)) {
            // New day - archive yesterday's stats and reset
            String yesterdayKey = "pomodoro_" + savedDate;
            int yesterdayCount = prefs.getInt("pomodoro_count_today", 0);
            int yesterdayMins = prefs.getInt("pomodoro_minutes_today", 0);
            if (yesterdayCount > 0 && !savedDate.isEmpty()) {
                prefs.edit()
                    .putInt(yesterdayKey + "_count", yesterdayCount)
                    .putInt(yesterdayKey + "_mins", yesterdayMins)
                    .apply();
            }
            // Reset today
            prefs.edit()
                .putString("pomodoro_date", today)
                .putInt("pomodoro_count_today", 0)
                .putInt("pomodoro_minutes_today", 0)
                .apply();
        }
        completedPomodoros = prefs.getInt("pomodoro_count_today", 0);
        totalMinutes = prefs.getInt("pomodoro_minutes_today", 0);
        updateStats();
        updateWeeklyStats();
    }

    private void updateWeeklyStats() {
        int weekCount = 0;
        int weekMins = 0;
        SimpleDateFormat dayFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < 7; i++) {
            String key = "pomodoro_" + dayFmt.format(cal.getTime());
            weekCount += prefs.getInt(key + "_count", 0);
            weekMins += prefs.getInt(key + "_mins", 0);
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }
        // Also add today's un-archived count
        weekCount += completedPomodoros;
        weekMins += totalMinutes;
        tvTotalPomodoros.setText(completedPomodoros + " / " + weekCount);
        tvTotalMinutes.setText(totalMinutes + " / " + weekMins);
        tvPetExp.setText("本周 " + weekCount + " 个");
    }

    private void addRecord(String title, String duration) {
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        records.add(0, new String[]{title, duration, time});

        // Save to prefs
        prefs.edit()
                .putInt("pomodoro_count_today", completedPomodoros)
                .putInt("pomodoro_minutes_today", totalMinutes)
                .apply();

        // Simple adapter
        recordList.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(android.R.layout.simple_list_item_2, parent, false);
                return new RecyclerView.ViewHolder(v) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                String[] record = records.get(position);
                ((TextView) holder.itemView.findViewById(android.R.id.text1)).setText(record[0]);
                ((TextView) holder.itemView.findViewById(android.R.id.text2)).setText(record[1] + " · " + record[2]);
            }

            @Override
            public int getItemCount() {
                return records.size();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (timer != null) timer.cancel();
    }
}
