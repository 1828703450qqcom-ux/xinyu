package com.xinyu.app.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.xinyu.app.MainActivity;
import com.xinyu.app.R;
import com.xinyu.app.db.AppDatabase;
import com.xinyu.app.model.MoodRecord;
import com.xinyu.app.model.Note;
import com.xinyu.app.model.Pet;
import com.xinyu.app.TreeHoleActivity;
import com.xinyu.app.util.PetManager;
import com.xinyu.app.widget.PetView;
import com.xinyu.app.util.QuoteHelper;
import com.xinyu.app.util.WeatherHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {

    private TextView greetingTitle, greetingSub;
    private TextView weatherIcon, weatherTemp, weatherCity, weatherDesc;
    private MaterialCardView weatherCard;
    private TextView quoteText, quoteAuthor;
    private MaterialCardView todayMoodCard;
    private TextView tvTodayMoodLabel, tvTodayMoodDesc, btnRecordMood, btnDailyQuote;
    private LinearLayout recentNotesContainer;
    private PetView petView;
    private TextView tvPetName, tvPetInfo, tvPetHint;
    private MaterialCardView petCard;

    private SharedPreferences prefs;
    private AppDatabase db;

    private static final Map<String, String> CITY_LANDMARKS = new HashMap<>();
    static {
        CITY_LANDMARKS.put("北京", "🏯 天安门 · 六百年紫禁城");
        CITY_LANDMARKS.put("上海", "🌃 外滩 · 万国建筑博览");
        CITY_LANDMARKS.put("广州", "🗼 广州塔 · 小蛮腰夜景");
        CITY_LANDMARKS.put("深圳", "🌆 深圳湾 · 都市海岸线");
        CITY_LANDMARKS.put("杭州", "🌸 西湖 · 淡妆浓抹总相宜");
        CITY_LANDMARKS.put("成都", "🐼 大熊猫 · 峨眉天下秀");
        CITY_LANDMARKS.put("武汉", "🌉 黄鹤楼 · 晴川历历汉阳树");
        CITY_LANDMARKS.put("南京", "🏛️ 中山陵 · 紫金山下忆金陵");
        CITY_LANDMARKS.put("西安", "🏺 兵马俑 · 千年古都梦长安");
        CITY_LANDMARKS.put("重庆", "🌃 洪崖洞 · 山城灯火阑珊处");
        CITY_LANDMARKS.put("长沙", "🏔️ 岳麓山 · 橘子洲头看湘江北去");
        CITY_LANDMARKS.put("苏州", "🌺 拙政园 · 园林甲天下");
        CITY_LANDMARKS.put("天津", "🎡 天津之眼 · 海河畔的摩天轮");
        CITY_LANDMARKS.put("郑州", "🥋 少林寺 · 天下武功出少林");
        CITY_LANDMARKS.put("厦门", "🏝️ 鼓浪屿 · 海上花园");
        CITY_LANDMARKS.put("青岛", "🌊 栈桥 · 红瓦绿树碧海蓝天");
        CITY_LANDMARKS.put("大连", "🌅 星海广场 · 北方明珠");
        CITY_LANDMARKS.put("宁波", "⛵ 天一阁 · 藏书楼里的时光");
    }
    private String username;
    private String nickname;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        initData();
        setupQuickActions(view);
        setupPet();
        refresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadNotificationBadge();
    }

    private void initViews(View view) {
        greetingTitle = view.findViewById(R.id.greeting_title);
        greetingSub = view.findViewById(R.id.greeting_sub);
        weatherIcon = view.findViewById(R.id.weather_icon);
        weatherTemp = view.findViewById(R.id.weather_temp);
        weatherCity = view.findViewById(R.id.weather_city);
        weatherDesc = view.findViewById(R.id.weather_desc);
        weatherCard = view.findViewById(R.id.weather_card);
        weatherCard.setOnClickListener(v -> showCityPicker());
        quoteText = view.findViewById(R.id.quote_text);
        quoteAuthor = view.findViewById(R.id.quote_author);
        todayMoodCard = view.findViewById(R.id.today_mood_card);
        tvTodayMoodLabel = view.findViewById(R.id.tv_today_mood_label);
        tvTodayMoodDesc = view.findViewById(R.id.tv_today_mood_desc);
        btnRecordMood = view.findViewById(R.id.btn_record_mood);
        btnDailyQuote = view.findViewById(R.id.btn_daily_quote);
        recentNotesContainer = view.findViewById(R.id.recent_notes_container);
        petView = view.findViewById(R.id.pet_view);
        tvPetName = view.findViewById(R.id.tv_pet_name);
        tvPetInfo = view.findViewById(R.id.tv_pet_info);
        tvPetHint = view.findViewById(R.id.tv_pet_hint);
        petCard = view.findViewById(R.id.pet_card);

        // Notification bell
        View btnNotif = view.findViewById(R.id.btn_notification);
        if (btnNotif != null) {
            btnNotif.setOnClickListener(v -> showNotificationDialog());
        }
    }

    private void initData() {
        prefs = requireContext().getSharedPreferences("xinyu", 0);
        db = AppDatabase.getInstance(requireContext());
        username = prefs.getString("current_user", "guest");
        nickname = prefs.getString("current_nickname", "用户");
    }

    private void setupQuickActions(View view) {
        MaterialCardView actionMood = view.findViewById(R.id.action_mood);
        MaterialCardView actionTest = view.findViewById(R.id.action_test);
        MaterialCardView actionNote = view.findViewById(R.id.action_note);
        MaterialCardView actionHelpline = view.findViewById(R.id.action_helpline);
        MaterialCardView actionAiChat = view.findViewById(R.id.action_ai_chat);

        actionMood.setOnClickListener(v -> switchTab(R.id.nav_emotion));
        actionTest.setOnClickListener(v -> switchTab(R.id.nav_emotion));
        actionNote.setOnClickListener(v -> switchTab(R.id.nav_note));
        actionHelpline.setOnClickListener(v -> showHelplinesDialog());
        actionAiChat.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                Intent intent = new Intent(requireContext(), com.xinyu.app.AIChatActivity.class);
                startActivity(intent);
                requireActivity().overridePendingTransition(R.anim.slide_in_bottom, 0);
            }
        });

        MaterialCardView actionTreeHole = view.findViewById(R.id.action_tree_hole);
        if (actionTreeHole != null) {
            actionTreeHole.setOnClickListener(v -> switchTab(R.id.nav_treehole));
        }

        // Daily quote button
        btnDailyQuote.setOnClickListener(v -> {
            String[] quote = QuoteHelper.getDailyQuote();
            if (quote != null && quote.length >= 2) {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("⭐ 每日一句")
                        .setMessage(quote[0] + "\n\n—— " + quote[1])
                        .setPositiveButton("知道了", null)
                        .show();
            }
        });

        // Record mood button
        btnRecordMood.setOnClickListener(v -> switchTab(R.id.nav_emotion));
    }

    private void switchTab(int itemId) {
        if (getActivity() instanceof MainActivity) {
            com.google.android.material.bottomnavigation.BottomNavigationView nav =
                (com.google.android.material.bottomnavigation.BottomNavigationView)
                ((MainActivity) getActivity()).findViewById(R.id.bottom_nav);
            nav.setSelectedItemId(itemId);
        }
    }

    private void setupPet() {
        PetManager petManager = PetManager.getInstance(requireContext());
        Pet pet = petManager.getPet();
        petView.setPet(pet);
        updatePetUI(pet);

        petCard.setOnClickListener(v -> showPetDialog(pet));
    }

    private void updatePetUI(Pet pet) {
        tvPetName.setText(pet.getEmoji() + " " + pet.getName());
        tvPetInfo.setText("Lv." + pet.getLevel() + " " + pet.getStageName() + " · " + pet.getMoodText());
    }

    private void showPetDialog(Pet pet) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_pet_action, null);

        PetView dialogPetView = dialogView.findViewById(R.id.pet_view_large);
        dialogPetView.setPet(pet);
        TextView tvMood = dialogView.findViewById(R.id.tv_pet_mood);
        tvMood.setText(pet.getMoodText());

        TextView btnFeed = dialogView.findViewById(R.id.btn_feed);
        TextView btnPlay = dialogView.findViewById(R.id.btn_play);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        btnFeed.setOnClickListener(v -> {
            pet.feed();
            PetManager.getInstance(requireContext()).savePet(pet);
            dialogPetView.setPet(pet);
            dialogPetView.startDance();
            tvMood.setText(pet.getMoodText());
            updatePetUI(pet);
            Toast.makeText(requireContext(), "🍙 " + pet.getName() + "吃得很开心！+10经验", Toast.LENGTH_SHORT).show();
        });

        btnPlay.setOnClickListener(v -> {
            pet.play();
            PetManager.getInstance(requireContext()).savePet(pet);
            dialogPetView.setPet(pet);
            dialogPetView.startDance();
            tvMood.setText(pet.getMoodText());
            updatePetUI(pet);
            Toast.makeText(requireContext(), "🎾 " + pet.getName() + "玩得很开心！+10经验", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void showNotificationDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_notifications, null);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.btn_close_notif).setOnClickListener(v -> dialog.dismiss());

        androidx.recyclerview.widget.RecyclerView rv = dialogView.findViewById(R.id.rv_notifications);
        rv.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireContext()));
        TextView tvEmpty = dialogView.findViewById(R.id.tv_empty_notif);

        String deviceId = com.xinyu.app.util.Reporter.getDeviceId(requireContext());

        new Thread(() -> {
            org.json.JSONObject result = com.xinyu.app.util.Reporter.squareGetNotifications(requireContext());
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                try {
                    org.json.JSONArray notifs = result.optJSONArray("notifications");
                    if (notifs == null || notifs.length() == 0) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        rv.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        rv.setVisibility(View.VISIBLE);
                        java.util.List<org.json.JSONObject> list = new java.util.ArrayList<>();
                        for (int i = 0; i < notifs.length(); i++) list.add(notifs.getJSONObject(i));
                        rv.setAdapter(new NotificationAdapter(list));
                    }
                } catch (Exception e) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    rv.setVisibility(View.GONE);
                }
            });
        }).start();

        dialog.show();
    }

    private static class NotificationAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<NotificationAdapter.VH> {
        private final java.util.List<org.json.JSONObject> items;
        NotificationAdapter(java.util.List<org.json.JSONObject> items) { this.items = items; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            org.json.JSONObject item = items.get(position);
            holder.tvFrom.setText(item.optString("from_anonymous_name", "匿名"));
            String text = item.optString("post_content", "");
            String reply = item.optString("reply_content", "");
            if (!reply.isEmpty()) text += "\n回复: " + reply;
            holder.tvText.setText(text);
            holder.tvTime.setText(item.optString("created_at", ""));
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class VH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            TextView tvFrom, tvText, tvTime;
            VH(View v) {
                super(v);
                tvFrom = v.findViewById(R.id.tv_notif_from);
                tvText = v.findViewById(R.id.tv_notif_text);
                tvTime = v.findViewById(R.id.tv_notif_time);
            }
        }
    }

    private void showHelplinesDialog() {
        String[] helplineNames = {
                "\u260E 110 - 报警电话",
                "\u260E 120 - 急救电话",
                "\u260E 12320 - 心理援助热线",
                "\u260E 400-161-9995 - 心理危机干预热线",
                "\u260E 400-821-1215 - 上海市心理热线"
        };
        String[] helplineNumbers = {"110", "120", "12320", "4001619995", "4008211215"};

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("心理援助热线")
                .setItems(helplineNames, (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + helplineNumbers[which]));
                    startActivity(intent);
                })
                .setNegativeButton("关闭", null)
                .show();
    }

    public void refresh() {
        if (getContext() == null || prefs == null) return;

        updateGreeting();
        loadWeather();
        loadQuote();
        loadTodayMood();
        loadRecentNotes();
        loadNotificationBadge();
    }

    private void loadNotificationBadge() {
        new Thread(() -> {
            org.json.JSONObject result = com.xinyu.app.util.Reporter.squareGetNotifications(requireContext());
            if (getView() == null || getActivity() == null) return;
            int unread = result.optInt("unread", 0);
            getActivity().runOnUiThread(() -> {
                View v = getView();
                if (v == null) return;
                TextView badge = v.findViewById(R.id.notif_badge);
                if (badge != null) {
                    if (unread > 0) {
                        badge.setText(unread > 99 ? "99+" : String.valueOf(unread));
                        badge.setVisibility(View.VISIBLE);
                    } else {
                        badge.setVisibility(View.GONE);
                    }
                }
            });
        }).start();
    }

    private void updateGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting;
        String emoji;
        String subText = "今天心情怎么样？";

        if (hour >= 5 && hour < 8) {
            greeting = "早上好";
            emoji = "🌸";
        } else if (hour >= 8 && hour < 12) {
            greeting = "上午好";
            emoji = "☀️";
        } else if (hour >= 12 && hour < 14) {
            greeting = "中午好";
            emoji = "🎀";
        } else if (hour >= 14 && hour < 18) {
            greeting = "下午好";
            emoji = "🍰";
        } else if (hour >= 18 && hour < 22) {
            greeting = "晚上好";
            emoji = "🌙";
        } else {
            greeting = "夜深了";
            emoji = "🌟";
            String[] lateNightMessages = {
                    "这么晚还没睡呀，要注意休息哦~",
                    "夜深了，有什么心事可以和我说说~",
                    "早点休息，明天会更好的~",
                    "月亮陪你，我也是~"
            };
            subText = lateNightMessages[(int)(Math.random() * lateNightMessages.length)];
        }

        greetingTitle.setText(greeting + "，" + nickname + " " + emoji);
        greetingSub.setText(subText);
    }

    private void loadWeather() {
        String savedCity = prefs.getString("weather_city", null);
        WeatherHelper.fetchWeather(savedCity, new WeatherHelper.WeatherCallback() {
            @Override
            public void onWeatherLoaded(String city, String temp, String desc, String detail, String icon) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    weatherIcon.setText(icon);
                    weatherTemp.setText(temp);
                    weatherCity.setText(city);
                    weatherDesc.setText(desc);
                });
            }

            @Override
            public void onError(String error) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    weatherIcon.setText("🌤️");
                    weatherTemp.setText("--°C");
                    weatherCity.setText(savedCity != null ? savedCity : "未知");
                    weatherDesc.setText("天气获取失败");
                });
            }
        });
    }

    private void showCityPicker() {
        if (getActivity() == null) return;

        String[] hotCities = {"北京", "上海", "广州", "深圳", "杭州", "成都",
                "武汉", "南京", "西安", "重庆", "长沙", "苏州",
                "天津", "郑州", "厦门", "青岛", "大连", "宁波"};

        // Warm dialog background
        LinearLayout root = new LinearLayout(getActivity());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.bg_history_dialog);
        root.setPadding(0, 0, 0, 20);

        // ===== Header =====
        LinearLayout headerArea = new LinearLayout(getActivity());
        headerArea.setOrientation(LinearLayout.VERTICAL);
        headerArea.setGravity(Gravity.CENTER_HORIZONTAL);
        headerArea.setPadding(40, 36, 40, 16);

        TextView headerIcon = new TextView(getActivity());
        headerIcon.setText("🌍");
        headerIcon.setTextSize(36);
        headerIcon.setGravity(Gravity.CENTER);
        headerArea.addView(headerIcon);

        TextView title = new TextView(getActivity());
        title.setText("想去哪里看看天气？");
        title.setTextSize(18);
        title.setTextColor(0xFF4A3728);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleLp.topMargin = 10;
        headerArea.addView(title, titleLp);

        TextView sub = new TextView(getActivity());
        sub.setText("选一个城市，心屿帮你看看那里的天气 ☁️");
        sub.setTextSize(13);
        sub.setTextColor(0xFFB39DDB);
        sub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = 4;
        headerArea.addView(sub, subLp);

        View divider = new View(getActivity());
        divider.setBackgroundColor(0x15FFB6C1);
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        divLp.setMargins(40, 16, 40, 0);
        headerArea.addView(divider, divLp);

        root.addView(headerArea);

        // ===== Search =====
        LinearLayout searchArea = new LinearLayout(getActivity());
        searchArea.setOrientation(LinearLayout.HORIZONTAL);
        searchArea.setGravity(Gravity.CENTER_VERTICAL);
        searchArea.setBackgroundResource(R.drawable.bg_city_search);
        searchArea.setPadding(24, 4, 24, 4);
        LinearLayout.LayoutParams searchAreaLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        searchAreaLp.setMargins(36, 8, 36, 12);
        root.addView(searchArea, searchAreaLp);

        TextView searchIcon = new TextView(getActivity());
        searchIcon.setText("🔍");
        searchIcon.setTextSize(16);
        searchArea.addView(searchIcon);

        EditText searchInput = new EditText(getActivity());
        searchInput.setHint("搜索城市名...");
        searchInput.setTextSize(14);
        searchInput.setTextColor(0xFF4A3728);
        searchInput.setHintTextColor(0xFFCCBBBB);
        searchInput.setSingleLine(true);
        searchInput.setBackgroundDrawable(null);
        LinearLayout.LayoutParams searchInputLp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        searchInputLp.leftMargin = 10;
        searchArea.addView(searchInput, searchInputLp);

        // ===== Scrollable city list =====
        LinearLayout scrollContent = new LinearLayout(getActivity());
        scrollContent.setOrientation(LinearLayout.VERTICAL);
        scrollContent.setPadding(28, 4, 28, 0);

        LinearLayout cityContainer = new LinearLayout(getActivity());
        cityContainer.setOrientation(LinearLayout.VERTICAL);
        scrollContent.addView(cityContainer);

        ScrollView scroll = new ScrollView(getActivity());
        scroll.addView(scrollContent);
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        // ===== Build city list =====
        final AlertDialog[] dialogHolder = new AlertDialog[1];
        String savedCity = prefs.getString("weather_city", null);

        Runnable buildCityList = () -> {
            cityContainer.removeAllViews();
            String filter = searchInput.getText().toString().trim();

            java.util.List<String> filtered = new java.util.ArrayList<>();
            for (String c : hotCities) {
                if (filter.isEmpty() || c.contains(filter)) {
                    filtered.add(c);
                }
            }

            // Auto location section
            LinearLayout autoRow = new LinearLayout(getActivity());
            autoRow.setOrientation(LinearLayout.HORIZONTAL);
            autoRow.setPadding(8, 8, 8, 8);

            TextView autoChip = new TextView(getActivity());
            autoChip.setText("🌐  自动定位");
            autoChip.setTextSize(14);
            autoChip.setPadding(32, 18, 32, 18);
            autoChip.setGravity(Gravity.CENTER);

            boolean autoSelected = (savedCity == null);
            if (autoSelected) {
                autoChip.setBackgroundResource(R.drawable.bg_city_chip_selected);
                autoChip.setTextColor(Color.WHITE);
            } else {
                autoChip.setBackgroundResource(R.drawable.bg_city_chip);
                autoChip.setTextColor(0xFF4A3728);
            }

            autoRow.addView(autoChip);
            LinearLayout.LayoutParams autoRowLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            autoRowLp.bottomMargin = 12;
            cityContainer.addView(autoRow, autoRowLp);

            autoChip.setOnClickListener(v -> {
                prefs.edit().remove("weather_city").apply();
                loadWeather();
                showLandmark(null);
                if (dialogHolder[0] != null) dialogHolder[0].dismiss();
            });

            if (!filter.isEmpty() && filtered.isEmpty()) {
                // No results
                TextView emptyText = new TextView(getActivity());
                emptyText.setText("没有找到这个城市哦~\n试试别的名字吧 🤔");
                emptyText.setTextSize(14);
                emptyText.setTextColor(0xFFBDBDBD);
                emptyText.setGravity(Gravity.CENTER);
                emptyText.setPadding(20, 40, 20, 40);
                cityContainer.addView(emptyText);
                return;
            }

            // Section label
            TextView sectionLabel = new TextView(getActivity());
            sectionLabel.setText("热门城市");
            sectionLabel.setTextSize(12);
            sectionLabel.setTextColor(0xFFCCBBBB);
            sectionLabel.setTypeface(null, Typeface.BOLD);
            sectionLabel.setPadding(8, 4, 8, 10);
            cityContainer.addView(sectionLabel);

            // City grid
            LinearLayout row = null;
            for (int i = 0; i < filtered.size(); i++) {
                if (i % 3 == 0) {
                    row = new LinearLayout(getActivity());
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    rowLp.bottomMargin = 10;
                    cityContainer.addView(row, rowLp);
                }

                String cityName = filtered.get(i);
                TextView chip = new TextView(getActivity());
                chip.setText(cityName);
                chip.setTextSize(14);
                chip.setPadding(28, 18, 28, 18);
                chip.setGravity(Gravity.CENTER);

                if (cityName.equals(savedCity)) {
                    chip.setBackgroundResource(R.drawable.bg_city_chip_selected);
                    chip.setTextColor(Color.WHITE);
                } else {
                    chip.setBackgroundResource(R.drawable.bg_city_chip);
                    chip.setTextColor(0xFF4A3728);
                }

                LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                chipLp.rightMargin = 8;
                row.addView(chip, chipLp);

                chip.setOnClickListener(v -> {
                    prefs.edit().putString("weather_city", cityName).apply();
                    loadWeather();
                    showLandmark(cityName);
                    if (dialogHolder[0] != null) dialogHolder[0].dismiss();
                });
            }
        };

        buildCityList.run();
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                buildCityList.run();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        dialogHolder[0] = new AlertDialog.Builder(getActivity())
                .setView(root)
                .create();

        if (dialogHolder[0].getWindow() != null) {
            dialogHolder[0].getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogHolder[0].show();
    }

    private void showLandmark(String city) {
        if (getActivity() == null) return;

        String landmark;
        String cityName;
        if (city == null) {
            landmark = "🌐  自动定位中...";
            cityName = "";
        } else {
            landmark = CITY_LANDMARKS.get(city);
            if (landmark == null) landmark = "📍  " + city;
            cityName = city;
        }

        // Large warm floating landmark card
        LinearLayout card = new LinearLayout(getActivity());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(56, 36, 56, 36);
        card.setBackgroundColor(0xDDFF8A9B);

        if (!cityName.isEmpty()) {
            TextView cityLabel = new TextView(getActivity());
            cityLabel.setText(cityName);
            cityLabel.setTextSize(22);
            cityLabel.setTextColor(Color.WHITE);
            cityLabel.setTypeface(null, Typeface.BOLD);
            cityLabel.setGravity(Gravity.CENTER);
            card.addView(cityLabel);
        }

        TextView landmarkView = new TextView(getActivity());
        landmarkView.setText(landmark);
        landmarkView.setTextSize(cityName.isEmpty() ? 18 : 15);
        landmarkView.setTextColor(Color.WHITE);
        landmarkView.setGravity(Gravity.CENTER);
        landmarkView.setAlpha(cityName.isEmpty() ? 1f : 0.9f);
        LinearLayout.LayoutParams landmarkLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        if (!cityName.isEmpty()) landmarkLp.topMargin = 6;
        card.addView(landmarkView, landmarkLp);

        card.setAlpha(0f);
        card.setScaleX(0.8f);
        card.setScaleY(0.8f);

        // Add to root layout
        ViewGroup decor = (ViewGroup) getActivity().getWindow().getDecorView();
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        decor.addView(card, lp);

        // Fade in with scale, hold, fade out
        card.animate()
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(350)
                .withEndAction(() -> card.postDelayed(() -> {
                    card.animate()
                            .alpha(0f).scaleX(0.8f).scaleY(0.8f)
                            .setDuration(400)
                            .withEndAction(() -> decor.removeView(card))
                            .start();
                }, 2000))
                .start();
    }

    private void loadQuote() {
        String[] quote = QuoteHelper.getDailyQuote();
        if (quote != null && quote.length >= 2) {
            quoteText.setText(quote[0]);
            quoteAuthor.setText("—— " + quote[1]);
        } else {
            quoteText.setText("每一个不曾起舞的日子，都是对生命的辜负。");
            quoteAuthor.setText("—— 尼采");
        }
    }

    private void loadTodayMood() {
        MoodRecord todayMood = db.getTodayMood(username);

        if (todayMood != null) {
            String moodLabel = todayMood.getEmoji() + " " + todayMood.getMoodLabel();
            tvTodayMoodLabel.setText(moodLabel);

            String desc;
            if (todayMood.getNote() != null && !todayMood.getNote().isEmpty()) {
                desc = todayMood.getNote();
            } else {
                // Default encouragement based on mood
                int moodValue = todayMood.getMoodValue();
                if (moodValue >= 4) {
                    desc = "感觉还不错，继续保持~";
                } else if (moodValue == 3) {
                    desc = "平淡也是一种幸福~";
                } else {
                    desc = "没关系，明天会更好的~";
                }
            }
            tvTodayMoodDesc.setText(desc);
        } else {
            tvTodayMoodLabel.setText("还没有记录今天的心情哦~");
            tvTodayMoodDesc.setText("点击右边按钮记录~");
        }
    }

    private void loadRecentNotes() {
        List<Note> notes = db.getNotes(username);
        recentNotesContainer.removeAllViews();

        int count = Math.min(notes.size(), 3);
        for (int i = 0; i < count; i++) {
            Note note = notes.get(i);
            View itemView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_note, recentNotesContainer, false);

            TextView title = itemView.findViewById(R.id.tv_note_title);
            TextView preview = itemView.findViewById(R.id.tv_note_preview);
            TextView time = itemView.findViewById(R.id.tv_note_time);

            title.setText(note.getTitle());
            preview.setText(note.getContent());
            time.setText(formatTime(note.getUpdatedAt()));

            itemView.setOnClickListener(v -> switchTab(R.id.nav_note));
            recentNotesContainer.addView(itemView);
        }

        if (count == 0) {
            TextView emptyText = new TextView(requireContext());
            emptyText.setText("还没有笔记，去写一篇吧~");
            emptyText.setTextColor(getResources().getColor(R.color.text_secondary, null));
            emptyText.setTextSize(14);
            emptyText.setPadding(0, dpToPx(16), 0, dpToPx(16));
            recentNotesContainer.addView(emptyText);
        }
    }

    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
