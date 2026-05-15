package com.xinyu.app.fragment;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.xinyu.app.LoginActivity;
import com.xinyu.app.MailboxActivity;
import com.xinyu.app.R;
import com.xinyu.app.db.AppDatabase;
import com.xinyu.app.model.Pet;
import com.xinyu.app.util.PetManager;
import com.xinyu.app.util.Reporter;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private TextView tvAvatar, tvNickname, tvUsername;
    private MaterialCardView cardProfileHeader;
    private LinearLayout menuTheme, menuEmergency, menuHelplines, menuCheckin, menuLogin, menuBluetooth, menuMailbox, menuDataReport, menuSecurityQuestion;
    private SwitchMaterial switchDarkMode;
    private TextView tvCheckinStreak, tvLoginText;

    private SharedPreferences prefs;
    private AppDatabase db;
    private String username;
    private String nickname;
    private String currentAvatar;

    private static final int PERMISSION_REQUEST_BLUETOOTH = 1001;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        initData();
        setupMenus();
        refresh();
    }

    private void initViews(View view) {
        tvAvatar = view.findViewById(R.id.tv_avatar);
        tvNickname = view.findViewById(R.id.tv_nickname);
        tvUsername = view.findViewById(R.id.tv_username);
        cardProfileHeader = view.findViewById(R.id.card_profile_header);
        menuTheme = view.findViewById(R.id.menu_theme);
        menuEmergency = view.findViewById(R.id.menu_emergency);
        menuHelplines = view.findViewById(R.id.menu_helplines);
        menuCheckin = view.findViewById(R.id.menu_checkin);
        menuLogin = view.findViewById(R.id.menu_login);
        menuBluetooth = view.findViewById(R.id.menu_bluetooth);
        menuMailbox = view.findViewById(R.id.menu_mailbox);
        menuDataReport = view.findViewById(R.id.menu_data_report);
        menuSecurityQuestion = view.findViewById(R.id.menu_security_question);
        switchDarkMode = view.findViewById(R.id.switch_dark_mode);
        tvCheckinStreak = view.findViewById(R.id.tv_checkin_streak);
        tvLoginText = view.findViewById(R.id.tv_login_text);
    }

    private void initData() {
        prefs = requireContext().getSharedPreferences("xinyu", 0);
        db = AppDatabase.getInstance(requireContext());
        username = prefs.getString("current_user", "guest");
        nickname = prefs.getString("current_nickname", "用户");
        currentAvatar = prefs.getString("current_avatar", "");
    }

    private void setupMenus() {
        // Edit profile
        cardProfileHeader.setOnClickListener(v -> showEditProfileDialog());

        // Theme toggle - save preference first, then let activity handle the mode change
        boolean isDark = prefs.getBoolean("dark_mode", false);
        switchDarkMode.setChecked(isDark);
        switchDarkMode.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean("dark_mode", checked).apply();
            // 立即更新窗口背景，防止切换时闪烁
            int bgColor = checked ? 0xFF1A1A2E : 0xFFF0F0;
            requireActivity().getWindow().getDecorView().setBackgroundColor(bgColor);
            if (checked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });
        menuTheme.setOnClickListener(v -> switchDarkMode.setChecked(!switchDarkMode.isChecked()));

        // Emergency contacts
        menuEmergency.setOnClickListener(v -> showEmergencyContactsDialog());

        // Helplines
        menuHelplines.setOnClickListener(v -> showHelplinesDialog());

        // Daily checkin
        menuCheckin.setOnClickListener(v -> performCheckin());

        // Menu items
        menuLogin.setOnClickListener(v -> showLogoutConfirmDialog());

        // Bluetooth
        menuBluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(requireContext(), com.xinyu.app.BleActivity.class);
                startActivity(intent);
                requireActivity().overridePendingTransition(R.anim.slide_in_bottom, 0);
            }
        });

        // Healing Mailbox
        menuMailbox.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), MailboxActivity.class);
            startActivity(intent);
            requireActivity().overridePendingTransition(R.anim.slide_in_bottom, 0);
        });

        // Data Report
        menuDataReport.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), com.xinyu.app.DataReportActivity.class);
            startActivity(intent);
            requireActivity().overridePendingTransition(R.anim.slide_in_bottom, 0);
        });

        // Security Question
        menuSecurityQuestion.setOnClickListener(v -> showSetSecurityQuestionDialog());
    }

    private void showSetSecurityQuestionDialog() {
        if ("guest".equals(username)) {
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        EditText etQuestion = new EditText(requireContext());
        etQuestion.setHint("例如：我的小学名字是？");
        etQuestion.setPadding(48, 32, 48, 32);

        EditText etAnswer = new EditText(requireContext());
        etAnswer.setHint("答案");
        etAnswer.setPadding(48, 32, 48, 32);

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 16, 48, 0);
        layout.addView(etQuestion);
        layout.addView(etAnswer);

        new AlertDialog.Builder(requireContext())
            .setTitle("设置安全问题")
            .setMessage("用于找回密码，请牢记答案")
            .setView(layout)
            .setPositiveButton("保存", (d, w) -> {
                String question = etQuestion.getText().toString().trim();
                String answer = etAnswer.getText().toString().trim();
                if (question.isEmpty() || answer.isEmpty()) {
                    Toast.makeText(requireContext(), "请填写完整", Toast.LENGTH_SHORT).show();
                    return;
                }
                new Thread(() -> {
                    JSONObject result = Reporter.setSecurityQuestion(username, question, answer);
                    requireActivity().runOnUiThread(() -> {
                        try {
                            if (result.has("error")) {
                                Toast.makeText(requireContext(), result.getString("error"), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(requireContext(), "安全问题已设置", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(requireContext(), "网络错误", Toast.LENGTH_SHORT).show();
                        }
                    });
                }).start();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void performCheckin() {
        if (db.hasCheckinToday(username)) {
            Toast.makeText(requireContext(), "今天已经打卡了！", Toast.LENGTH_SHORT).show();
            return;
        }

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        db.addCheckin(username, today);
        Reporter.report(requireContext(), "checkin", today);

        // Give pet exp for checkin
        PetManager pm = PetManager.getInstance(requireContext());
        Pet pet = pm.getPet();
        pet.feed();
        pm.savePet(pet);

        int streak = db.getCheckinCount(username);

        // Checkin celebration
        String[] celebrations = {
                "打卡成功！今天也要元气满满哦~ 🌸",
                "太棒了！又坚持了一天~ ✨",
                "打卡完成！你是最棒的~ 💖",
                "连续打卡，越来越厉害了~ 🎀",
                "今天也来啦！好开心~ 🍰"
        };
        String msg = celebrations[(int)(Math.random() * celebrations.length)];

        // 7-day achievement badge
        if (streak == 7) {
            msg = "🎉 解锁成就：坚持一周！你太棒了~ 🏅";
        } else if (streak == 14) {
            msg = "🎉 解锁成就：两周达人！超级厉害~ 🏆";
        } else if (streak == 30) {
            msg = "🎉 解锁成就：一个月坚持！你是传奇~ 👑";
        }

        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();

        // Confetti
        showCheckinConfetti();
        refresh();
    }

    private void showCheckinConfetti() {
        if (getView() == null) return;
        android.widget.FrameLayout container = getView().findViewById(android.R.id.content) != null ?
                (android.widget.FrameLayout) getView().getRootView().findViewById(android.R.id.content) : null;
        if (container == null) return;

        String[] emojis = {"🎉", "⭐", "✨", "🌸", "💖", "🏅", "🎀", "💫"};
        for (int i = 0; i < 15; i++) {
            final TextView confetti = new TextView(requireContext());
            confetti.setText(emojis[i % emojis.length]);
            confetti.setTextSize(18 + (int)(Math.random() * 14));

            android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT);
            params.leftMargin = (int)(Math.random() * container.getWidth());
            params.topMargin = -30;
            confetti.setLayoutParams(params);
            confetti.setAlpha(0.9f);
            container.addView(confetti);

            final float drift = (float)(Math.random() - 0.5) * 200;
            final long duration = 1800 + (long)(Math.random() * 1200);

            confetti.animate()
                    .translationY(container.getHeight() + 50)
                    .translationX(drift)
                    .rotation((float)(Math.random() * 360))
                    .alpha(0f)
                    .setDuration(duration)
                    .setStartDelay(i * 60)
                    .withEndAction(() -> container.removeView(confetti))
                    .start();
        }
    }

    private void showEditProfileDialog() {
        // Emoji avatars
        String[] avatars = {"🐱", "🐶", "🐰", "🐻", "🦊", "🐼", "🐮", "🐷", "🐵", "🦁", "🐯", "🐸"};
        final String[] selectedAvatar = {currentAvatar.isEmpty() ? (nickname.isEmpty() ? "😊" : String.valueOf(nickname.charAt(0))) : currentAvatar};

        // Build dialog view
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dpToPx(24), dpToPx(16), dpToPx(24), dpToPx(8));

        // Avatar preview
        TextView tvPreview = new TextView(requireContext());
        tvPreview.setText(selectedAvatar[0]);
        tvPreview.setTextSize(48);
        tvPreview.setGravity(android.view.Gravity.CENTER);
        tvPreview.setPadding(0, dpToPx(8), 0, dpToPx(16));
        root.addView(tvPreview);

        // Emoji grid label
        TextView labelAvatar = new TextView(requireContext());
        labelAvatar.setText("选择头像");
        labelAvatar.setTextColor(getResources().getColor(R.color.text_primary));
        labelAvatar.setPadding(0, 0, 0, dpToPx(8));
        root.addView(labelAvatar);

        // Emoji grid (4 columns)
        android.widget.GridLayout grid = new android.widget.GridLayout(requireContext());
        grid.setColumnCount(4);
        grid.setRowCount(3);
        grid.setAlignmentMode(android.widget.GridLayout.ALIGN_BOUNDS);

        TextView[] emojiViews = new TextView[avatars.length];
        for (int i = 0; i < avatars.length; i++) {
            final int index = i;
            TextView tv = new TextView(requireContext());
            tv.setText(avatars[i]);
            tv.setTextSize(28);
            tv.setGravity(android.view.Gravity.CENTER);
            tv.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));

            android.widget.GridLayout.LayoutParams params = new android.widget.GridLayout.LayoutParams();
            params.width = 0;
            params.height = android.widget.GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f);
            tv.setLayoutParams(params);

            // Highlight if selected
            if (avatars[i].equals(selectedAvatar[0])) {
                tv.setBackgroundResource(R.drawable.circle_avatar_bg);
                tv.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
            }

            tv.setOnClickListener(v -> {
                selectedAvatar[0] = avatars[index];
                tvPreview.setText(avatars[index]);
                // Reset all highlights
                for (int j = 0; j < emojiViews.length; j++) {
                    emojiViews[j].setBackgroundResource(0);
                    emojiViews[j].setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
                }
                // Highlight selected
                tv.setBackgroundResource(R.drawable.circle_avatar_bg);
                tv.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
            });

            emojiViews[i] = tv;
            grid.addView(tv);
        }
        root.addView(grid);

        // Nickname input
        TextView labelNick = new TextView(requireContext());
        labelNick.setText("修改昵称");
        labelNick.setTextColor(getResources().getColor(R.color.text_primary));
        labelNick.setPadding(0, dpToPx(16), 0, dpToPx(8));
        root.addView(labelNick);

        EditText etNickname = new EditText(requireContext());
        etNickname.setText(nickname);
        etNickname.setHint("输入新昵称");
        etNickname.setSelection(nickname.length());
        root.addView(etNickname);

        // Username (read-only)
        TextView tvUsernameHint = new TextView(requireContext());
        tvUsernameHint.setText("用户名: " + username + " (不可修改)");
        tvUsernameHint.setTextColor(getResources().getColor(R.color.text_secondary));
        tvUsernameHint.setTextSize(12);
        tvUsernameHint.setPadding(0, dpToPx(12), 0, 0);
        root.addView(tvUsernameHint);

        new AlertDialog.Builder(requireContext())
                .setTitle("编辑资料")
                .setView(root)
                .setPositiveButton("保存", (dialog, which) -> {
                    String newNickname = etNickname.getText().toString().trim();
                    if (newNickname.isEmpty()) newNickname = username;

                    // Save to DB
                    db.updateUserProfile(username, newNickname, selectedAvatar[0]);

                    // Save to SharedPreferences
                    prefs.edit()
                            .putString("current_nickname", newNickname)
                            .putString("current_avatar", selectedAvatar[0])
                            .apply();

                    refresh();
                    Toast.makeText(requireContext(), "资料已更新", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showEmergencyContactsDialog() {
        List<Map<String, String>> contacts = db.getContacts(username);

        String[] items = new String[contacts.size() + 1];
        for (int i = 0; i < contacts.size(); i++) {
            Map<String, String> contact = contacts.get(i);
            items[i] = contact.get("name") + " - " + contact.get("phone") + " (" + contact.get("relation") + ")";
        }
        items[contacts.size()] = "+ 添加联系人";

        new AlertDialog.Builder(requireContext())
                .setTitle("紧急联系人")
                .setItems(items, (dialog, which) -> {
                    if (which < contacts.size()) {
                        // Click on existing contact
                        Map<String, String> contact = contacts.get(which);
                        showContactActionDialog(contact);
                    } else {
                        // Add new contact
                        showAddContactDialog();
                    }
                })
                .setNegativeButton("关闭", null)
                .show();
    }

    private void showContactActionDialog(Map<String, String> contact) {
        String[] options = {"拨打电话", "删除联系人"};
        new AlertDialog.Builder(requireContext())
                .setTitle(contact.get("name"))
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Call
                        Intent intent = new Intent(Intent.ACTION_DIAL);
                        intent.setData(Uri.parse("tel:" + contact.get("phone")));
                        startActivity(intent);
                    } else {
                        // Delete
                        db.deleteContact(Long.parseLong(contact.get("id")));
                        Toast.makeText(requireContext(), "联系人已删除", Toast.LENGTH_SHORT).show();
                        refresh();
                    }
                })
                .show();
    }

    private void showAddContactDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(android.R.layout.simple_list_item_2, null);

        // Create a simple dialog with 3 EditText fields
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(24), dpToPx(8), dpToPx(24), dpToPx(8));

        TextView labelName = new TextView(requireContext());
        labelName.setText("姓名");
        layout.addView(labelName);

        EditText etName = new EditText(requireContext());
        etName.setHint("输入姓名");
        layout.addView(etName);

        TextView labelPhone = new TextView(requireContext());
        labelPhone.setText("电话");
        layout.addView(labelPhone);

        EditText etPhone = new EditText(requireContext());
        etPhone.setHint("输入电话号码");
        etPhone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        layout.addView(etPhone);

        TextView labelRelation = new TextView(requireContext());
        labelRelation.setText("关系");
        layout.addView(labelRelation);

        EditText etRelation = new EditText(requireContext());
        etRelation.setHint("如：家人、朋友、医生");
        layout.addView(etRelation);

        new AlertDialog.Builder(requireContext())
                .setTitle("添加紧急联系人")
                .setView(layout)
                .setPositiveButton("添加", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();
                    String relation = etRelation.getText().toString().trim();

                    if (name.isEmpty() || phone.isEmpty()) {
                        Toast.makeText(requireContext(), "姓名和电话不能为空",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    db.addContact(username, name, phone, relation);
                    Toast.makeText(requireContext(), "联系人已添加", Toast.LENGTH_SHORT).show();
                    refresh();
                })
                .setNegativeButton("取消", null)
                .show();
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

        new AlertDialog.Builder(requireContext())
                .setTitle("心理援助热线")
                .setItems(helplineNames, (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + helplineNumbers[which]));
                    startActivity(intent);
                })
                .setNegativeButton("关闭", null)
                .show();
    }

    private void showLogoutConfirmDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("退出登录")
                .setMessage("确定要退出登录吗？")
                .setPositiveButton("退出", (dialog, which) -> {
                    prefs.edit().clear().apply();
                    Intent intent = new Intent(requireContext(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(requireContext(),
                            Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT
                        },
                        PERMISSION_REQUEST_BLUETOOTH);
            }
        } else {
            // Android 11 and below
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.BLUETOOTH},
                        PERMISSION_REQUEST_BLUETOOTH);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_BLUETOOTH) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "蓝牙权限已授予", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "蓝牙权限被拒绝", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void refresh() {
        if (getContext() == null || prefs == null) return;

        // Update user info
        nickname = prefs.getString("current_nickname", "用户");
        username = prefs.getString("current_user", "guest");
        currentAvatar = prefs.getString("current_avatar", "");

        tvNickname.setText(nickname);
        tvUsername.setText("ID: " + username);

        // Avatar - use custom avatar or first character of nickname
        if (!currentAvatar.isEmpty()) {
            tvAvatar.setText(currentAvatar);
        } else if (!nickname.isEmpty()) {
            tvAvatar.setText(String.valueOf(nickname.charAt(0)));
        }

        // Checkin streak
        int checkinCount = db.getCheckinCount(username);
        boolean hasCheckedIn = db.hasCheckinToday(username);
        tvCheckinStreak.setText("连续 " + checkinCount + " 天" +
                (hasCheckedIn ? " (今日已打卡)" : ""));
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
