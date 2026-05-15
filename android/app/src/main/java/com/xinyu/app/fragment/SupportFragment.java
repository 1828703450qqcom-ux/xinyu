package com.xinyu.app.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;
import com.xinyu.app.R;
import com.xinyu.app.SupportChatActivity;
import com.xinyu.app.model.SupportMatch;
import com.xinyu.app.model.SupportProfile;
import com.xinyu.app.util.Reporter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SupportFragment extends Fragment {

    private TabLayout tabLayout;
    private FrameLayout container;
    private View discoverView, matchesView;
    private RecyclerView recyclerUsers, recyclerMatches;
    private LinearLayout emptyDiscover, emptyMatches;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private List<SupportProfile> userList = new ArrayList<>();
    private List<SupportMatch> matchList = new ArrayList<>();
    private UserAdapter userAdapter;
    private MatchAdapter matchAdapter;
    private boolean profileReady = false;

    private static final String[] MOOD_OPTIONS = {"焦虑", "压力大", "低落", "孤独", "迷茫", "想聊聊"};
    private static final String[] INTEREST_OPTIONS = {"学习", "运动", "音乐", "游戏", "阅读", "电影", "旅行", "美食"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_support, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tabLayout = view.findViewById(R.id.tab_layout);
        container = view.findViewById(R.id.container);

        discoverView = LayoutInflater.from(getContext()).inflate(R.layout.fragment_support_discover, container, false);
        matchesView = LayoutInflater.from(getContext()).inflate(R.layout.fragment_support_matches, container, false);
        container.addView(discoverView);
        container.addView(matchesView);
        matchesView.setVisibility(View.GONE);

        recyclerUsers = discoverView.findViewById(R.id.recycler_users);
        emptyDiscover = discoverView.findViewById(R.id.empty_view);
        EditText etSearch = discoverView.findViewById(R.id.et_search);
        View btnSearch = discoverView.findViewById(R.id.btn_search);
        View btnEditTags = discoverView.findViewById(R.id.btn_edit_tags);
        btnEditTags.setOnClickListener(v -> openTagEditor());
        recyclerUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        userAdapter = new UserAdapter();
        recyclerUsers.setAdapter(userAdapter);

        recyclerMatches = matchesView.findViewById(R.id.recycler_matches);
        emptyMatches = matchesView.findViewById(R.id.empty_view);
        recyclerMatches.setLayoutManager(new LinearLayoutManager(getContext()));
        matchAdapter = new MatchAdapter();
        recyclerMatches.setAdapter(matchAdapter);

        tabLayout.addTab(tabLayout.newTab().setText("发现"));
        tabLayout.addTab(tabLayout.newTab().setText("我的匹配"));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    discoverView.setVisibility(View.VISIBLE);
                    matchesView.setVisibility(View.GONE);
                    if (profileReady) loadDiscover();
                } else {
                    discoverView.setVisibility(View.GONE);
                    matchesView.setVisibility(View.VISIBLE);
                    loadMatches();
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // 搜索按钮
        btnSearch.setOnClickListener(v -> {
            String keyword = etSearch.getText().toString().trim();
            if (keyword.isEmpty()) {
                Toast.makeText(getContext(), "请输入用户名", Toast.LENGTH_SHORT).show();
                return;
            }
            searchUsers(keyword);
        });

        // 键盘回车也能搜索
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                String keyword = etSearch.getText().toString().trim();
                if (!keyword.isEmpty()) {
                    searchUsers(keyword);
                }
                return true;
            }
            return false;
        });

        // 先确保自己有名片，然后加载发现页
        ensureProfile();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (profileReady && tabLayout.getSelectedTabPosition() == 0) {
            loadDiscover();
        } else if (tabLayout.getSelectedTabPosition() == 1) {
            loadMatches();
        }
    }

    /** 确保当前用户有互助名片，没有则自动创建，标签为空则弹出设置 */
    private void ensureProfile() {
        executor.execute(() -> {
            JSONObject result = Reporter.supportGetProfile(requireContext());
            boolean needSetup = true;
            if (!result.has("error")) {
                // 有名片，检查标签是否为空
                org.json.JSONArray mt = result.optJSONArray("mood_tags");
                org.json.JSONArray it = result.optJSONArray("interest_tags");
                needSetup = (mt == null || mt.length() == 0) && (it == null || it.length() == 0);
            }
            if (needSetup) {
                // 没有名片或标签为空，先创建基础名片
                Reporter.supportCreateProfile(requireContext(), "[]", "[]", "");
            }
            profileReady = true;
            final boolean showSetup = needSetup;
            mainHandler.post(() -> {
                loadDiscover();
                if (showSetup) showTagSetupDialog(new ArrayList<>(), new ArrayList<>());
            });
        });
    }

    /** 从服务器加载当前标签并打开编辑对话框 */
    private void openTagEditor() {
        executor.execute(() -> {
            JSONObject result = Reporter.supportGetProfile(requireContext());
            java.util.List<String> curMoods = new ArrayList<>();
            java.util.List<String> curInterests = new ArrayList<>();
            if (!result.has("error")) {
                org.json.JSONArray mt = result.optJSONArray("mood_tags");
                org.json.JSONArray it = result.optJSONArray("interest_tags");
                if (mt != null) { for (int i = 0; i < mt.length(); i++) curMoods.add(mt.optString(i)); }
                if (it != null) { for (int i = 0; i < it.length(); i++) curInterests.add(it.optString(i)); }
            }
            final java.util.List<String> fm = curMoods;
            final java.util.List<String> fi = curInterests;
            mainHandler.post(() -> showTagSetupDialog(fm, fi));
        });
    }

    /** 弹出标签设置对话框，支持回显已选标签 */
    private void showTagSetupDialog(java.util.List<String> selectedMoods, java.util.List<String> selectedInterests) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_tag_setup, null);
        builder.setView(dialogView);
        android.app.AlertDialog dialog = builder.create();
        dialog.setCancelable(true);

        com.google.android.material.chip.ChipGroup chipGroupMood = dialogView.findViewById(R.id.chip_group_mood);
        com.google.android.material.chip.ChipGroup chipGroupInterest = dialogView.findViewById(R.id.chip_group_interest);
        View btnConfirm = dialogView.findViewById(R.id.btn_confirm);

        // 填充情绪标签
        for (String mood : MOOD_OPTIONS) {
            com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(requireContext());
            chip.setText(mood);
            chip.setCheckable(true);
            chip.setChipBackgroundColorResource(R.color.cute_pink);
            chip.setTextColor(getResources().getColor(R.color.text_primary));
            chip.setChecked(selectedMoods.contains(mood));
            chipGroupMood.addView(chip);
        }

        // 填充兴趣标签
        for (String interest : INTEREST_OPTIONS) {
            com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(requireContext());
            chip.setText(interest);
            chip.setCheckable(true);
            chip.setChipBackgroundColorResource(R.color.cute_lavender);
            chip.setTextColor(getResources().getColor(R.color.text_primary));
            chip.setChecked(selectedInterests.contains(interest));
            chipGroupInterest.addView(chip);
        }

        btnConfirm.setOnClickListener(v -> {
            java.util.List<String> newMoods = new ArrayList<>();
            for (int i = 0; i < chipGroupMood.getChildCount(); i++) {
                com.google.android.material.chip.Chip c = (com.google.android.material.chip.Chip) chipGroupMood.getChildAt(i);
                if (c.isChecked()) newMoods.add(c.getText().toString());
            }
            java.util.List<String> newInterests = new ArrayList<>();
            for (int i = 0; i < chipGroupInterest.getChildCount(); i++) {
                com.google.android.material.chip.Chip c = (com.google.android.material.chip.Chip) chipGroupInterest.getChildAt(i);
                if (c.isChecked()) newInterests.add(c.getText().toString());
            }

            if (newMoods.isEmpty() && newInterests.isEmpty()) {
                Toast.makeText(getContext(), "请至少选择一个标签", Toast.LENGTH_SHORT).show();
                return;
            }

            org.json.JSONArray moodArr = new org.json.JSONArray();
            for (String m : newMoods) moodArr.put(m);
            org.json.JSONArray interestArr = new org.json.JSONArray();
            for (String s : newInterests) interestArr.put(s);

            executor.execute(() -> {
                Reporter.supportCreateProfile(requireContext(), moodArr.toString(), interestArr.toString(), "");
                mainHandler.post(() -> {
                    dialog.dismiss();
                    Toast.makeText(getContext(), "标签已更新", Toast.LENGTH_SHORT).show();
                    loadDiscover();
                });
            });
        });

        dialog.show();
    }

    private void loadDiscover() {
        executor.execute(() -> {
            JSONObject result = Reporter.supportDiscover(requireContext());
            mainHandler.post(() -> {
                try {
                    JSONArray users = result.optJSONArray("users");
                    userList.clear();
                    if (users != null) {
                        for (int i = 0; i < users.length(); i++) {
                            JSONObject u = users.getJSONObject(i);
                            SupportProfile p = new SupportProfile();
                            p.setDeviceId(u.optString("device_id", ""));
                            String realName = u.optString("real_name", "");
                            p.setAnonymousName(realName.isEmpty() ? u.optString("anonymous_name", "匿名用户") : realName);
                            p.setBio(u.optString("bio", ""));
                            JSONArray mt = u.optJSONArray("mood_tags");
                            List<String> ml = new ArrayList<>();
                            if (mt != null) { for (int j = 0; j < mt.length(); j++) { ml.add(mt.getString(j)); } }
                            p.setMoodTags(ml);
                            JSONArray it = u.optJSONArray("interest_tags");
                            List<String> il = new ArrayList<>();
                            if (it != null) { for (int j = 0; j < it.length(); j++) { il.add(it.getString(j)); } }
                            p.setInterestTags(il);
                            userList.add(p);
                        }
                    }
                    userAdapter.notifyDataSetChanged();
                    emptyDiscover.setVisibility(userList.isEmpty() ? View.VISIBLE : View.GONE);
                    recyclerUsers.setVisibility(userList.isEmpty() ? View.GONE : View.VISIBLE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
    }

    private void searchUsers(String keyword) {
        Toast.makeText(getContext(), "搜索: " + keyword, Toast.LENGTH_SHORT).show();
        executor.execute(() -> {
            JSONObject result = Reporter.supportSearch(requireContext(), keyword);
            mainHandler.post(() -> {
                try {
                    if (result.has("error")) {
                        Toast.makeText(getContext(), "搜索出错: " + result.optString("error"), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    JSONArray users = result.optJSONArray("users");
                    userList.clear();
                    if (users != null) {
                        for (int i = 0; i < users.length(); i++) {
                            JSONObject u = users.getJSONObject(i);
                            SupportProfile p = new SupportProfile();
                            p.setDeviceId(u.optString("device_id", ""));
                            String realName = u.optString("real_name", "");
                            p.setAnonymousName(realName.isEmpty() ? u.optString("anonymous_name", "匿名用户") : realName);
                            p.setBio(u.optString("bio", ""));
                            JSONArray mt = u.optJSONArray("mood_tags");
                            List<String> ml = new ArrayList<>();
                            if (mt != null) { for (int j = 0; j < mt.length(); j++) { ml.add(mt.getString(j)); } }
                            p.setMoodTags(ml);
                            JSONArray it = u.optJSONArray("interest_tags");
                            List<String> il = new ArrayList<>();
                            if (it != null) { for (int j = 0; j < it.length(); j++) { il.add(it.getString(j)); } }
                            p.setInterestTags(il);
                            userList.add(p);
                        }
                    }
                    userAdapter.notifyDataSetChanged();
                    emptyDiscover.setVisibility(userList.isEmpty() ? View.VISIBLE : View.GONE);
                    recyclerUsers.setVisibility(userList.isEmpty() ? View.GONE : View.VISIBLE);
                    if (userList.isEmpty()) {
                        Toast.makeText(getContext(), "未找到该用户", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "解析出错", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void loadMatches() {
        executor.execute(() -> {
            JSONObject result = Reporter.supportGetMatches(requireContext());
            mainHandler.post(() -> {
                try {
                    JSONArray matches = result.optJSONArray("matches");
                    matchList.clear();
                    if (matches != null) {
                        for (int i = 0; i < matches.length(); i++) {
                            JSONObject m = matches.getJSONObject(i);
                            SupportMatch match = new SupportMatch();
                            match.setMatchId(m.optInt("match_id"));
                            match.setPartnerName(m.optString("partner_name"));
                            match.setPartnerDeviceId(m.optString("partner_device_id"));
                            match.setLastMessage(m.optString("last_message"));
                            match.setLastTime(m.optString("last_time"));
                            match.setCreatedAt(m.optString("created_at"));
                            matchList.add(match);
                        }
                    }
                    matchAdapter.notifyDataSetChanged();
                    emptyMatches.setVisibility(matchList.isEmpty() ? View.VISIBLE : View.GONE);
                    recyclerMatches.setVisibility(matchList.isEmpty() ? View.GONE : View.VISIBLE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
    }

    // 用户卡片适配器
    class UserAdapter extends RecyclerView.Adapter<UserAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_support_user, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            SupportProfile p = userList.get(position);
            holder.tvName.setText(p.getAnonymousName() != null ? p.getAnonymousName() : "匿名用户");
            String bio = p.getBio();
            holder.tvBio.setText(bio == null || bio.isEmpty() ? "这个人很神秘~" : bio);

            holder.chipGroupMood.removeAllViews();
            if (p.getMoodTags() != null) {
                for (String mood : p.getMoodTags()) {
                    Chip chip = new Chip(requireContext());
                    chip.setText(mood);
                    chip.setChipBackgroundColorResource(R.color.cute_pink);
                    chip.setTextColor(getResources().getColor(R.color.text_primary));
                    chip.setClickable(false);
                    holder.chipGroupMood.addView(chip);
                }
            }

            holder.chipGroupInterest.removeAllViews();
            if (p.getInterestTags() != null) {
                for (String interest : p.getInterestTags()) {
                    Chip chip = new Chip(requireContext());
                    chip.setText(interest);
                    chip.setChipBackgroundColorResource(R.color.cute_lavender);
                    chip.setTextColor(getResources().getColor(R.color.text_primary));
                    chip.setClickable(false);
                    holder.chipGroupInterest.addView(chip);
                }
            }

            // 防止匹配自己
            String myDeviceId = Reporter.getDeviceId(requireContext());
            if (myDeviceId != null && myDeviceId.equals(p.getDeviceId())) {
                holder.btnConnect.setVisibility(View.GONE);
            } else {
                holder.btnConnect.setVisibility(View.VISIBLE);
                holder.btnConnect.setOnClickListener(v -> {
                    executor.execute(() -> {
                        JSONObject result = Reporter.supportMatch(requireContext(), p.getDeviceId());
                        mainHandler.post(() -> {
                            if (result.has("error")) {
                                Toast.makeText(getContext(), result.optString("error"), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), "匹配成功！去我的匹配看看", Toast.LENGTH_SHORT).show();
                                loadDiscover();
                            }
                        });
                    });
                });
            }
        }

        @Override
        public int getItemCount() { return userList.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvBio;
            ChipGroup chipGroupMood, chipGroupInterest;
            View btnConnect;
            VH(View v) {
                super(v);
                tvName = v.findViewById(R.id.tv_name);
                tvBio = v.findViewById(R.id.tv_bio);
                chipGroupMood = v.findViewById(R.id.chip_group_mood);
                chipGroupInterest = v.findViewById(R.id.chip_group_interest);
                btnConnect = v.findViewById(R.id.btn_connect);
            }
        }
    }

    // 匹配列表适配器
    class MatchAdapter extends RecyclerView.Adapter<MatchAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_support_match, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            SupportMatch m = matchList.get(position);
            holder.tvPartnerName.setText(m.getPartnerName() != null ? m.getPartnerName() : "用户");
            String lastMsg = m.getLastMessage();
            holder.tvLastMsg.setText(lastMsg == null || lastMsg.isEmpty() ? "开始聊天吧~" : lastMsg);
            holder.tvTime.setText(formatTime(m.getLastTime()));

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), SupportChatActivity.class);
                intent.putExtra("match_id", m.getMatchId());
                intent.putExtra("partner_name", m.getPartnerName());
                startActivity(intent);
            });

            // 长按删除匹配
            holder.itemView.setOnLongClickListener(v -> {
                new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("解除匹配")
                    .setMessage("确定要和 " + m.getPartnerName() + " 解除匹配吗？聊天记录也会删除。")
                    .setPositiveButton("删除", (d, w) -> {
                        executor.execute(() -> {
                            JSONObject result = Reporter.supportUnmatch(requireContext(), m.getMatchId());
                            mainHandler.post(() -> {
                                if (result.has("error")) {
                                    Toast.makeText(getContext(), "删除失败", Toast.LENGTH_SHORT).show();
                                } else {
                                    matchList.remove(position);
                                    notifyItemRemoved(position);
                                    Toast.makeText(getContext(), "已解除匹配", Toast.LENGTH_SHORT).show();
                                    if (matchList.isEmpty()) {
                                        emptyMatches.setVisibility(View.VISIBLE);
                                        recyclerMatches.setVisibility(View.GONE);
                                    }
                                }
                            });
                        });
                    })
                    .setNegativeButton("取消", null)
                    .show();
                return true;
            });
        }

        @Override
        public int getItemCount() { return matchList.size(); }

        private String formatTime(String timeStr) {
            try {
                long ts = Long.parseLong(timeStr);
                long now = System.currentTimeMillis() / 1000;
                long diff = now - ts;
                if (diff < 60) return "刚刚";
                if (diff < 3600) return (diff / 60) + "分钟前";
                if (diff < 86400) return (diff / 3600) + "小时前";
                return (diff / 86400) + "天前";
            } catch (Exception e) {
                return "";
            }
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvPartnerName, tvLastMsg, tvTime;
            VH(View v) {
                super(v);
                tvPartnerName = v.findViewById(R.id.tv_partner_name);
                tvLastMsg = v.findViewById(R.id.tv_last_msg);
                tvTime = v.findViewById(R.id.tv_time);
            }
        }
    }
}
