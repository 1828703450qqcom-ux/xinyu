package com.xinyu.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.tabs.TabLayout;
import com.xinyu.app.db.AppDatabase;
import com.xinyu.app.model.User;
import com.xinyu.app.util.Reporter;

import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private EditText etUsername, etPassword, etNickname;
    private View tilNickname;
    private LinearLayout genderLayout;
    private com.google.android.material.button.MaterialButton btnSubmit;
    private View btnSkip, cardBoy, cardGirl;
    private boolean isLoginMode = false; // 默认注册模式
    private String selectedGender = "male";
    private AppDatabase db;

    private View loginScrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        loginScrollView = findViewById(R.id.login_scroll_view);

        // 已登录 → 验证session是否有效（可能被网页端踢下线）
        String user = getSharedPreferences("xinyu", MODE_PRIVATE).getString("current_user", null);
        if (user != null && !user.isEmpty()) {
            // 隐藏登录表单，只显示splash背景
            loginScrollView.setVisibility(View.INVISIBLE);
            new Thread(() -> {
                JSONObject result = Reporter.validateSession(this);
                boolean valid = result.optBoolean("valid", false);
                runOnUiThread(() -> {
                    if (valid) {
                        overridePendingTransition(0, 0);
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                        overridePendingTransition(0, 0);
                    } else {
                        // 被踢下线，清除本地登录状态，显示登录表单
                        getSharedPreferences("xinyu", MODE_PRIVATE).edit().clear().apply();
                        loginScrollView.setVisibility(View.VISIBLE);
                        Toast.makeText(this, "账号已在其他设备登录，请重新登录", Toast.LENGTH_LONG).show();
                    }
                });
            }).start();
            return;
        }
        db = AppDatabase.getInstance(this);
        initViews();
    }

    private void initViews() {
        tabLayout = findViewById(R.id.tab_layout);
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        etNickname = findViewById(R.id.et_nickname);
        tilNickname = findViewById(R.id.til_nickname);
        genderLayout = findViewById(R.id.layout_gender);
        btnSubmit = (com.google.android.material.button.MaterialButton) findViewById(R.id.btn_login);
        btnSkip = findViewById(R.id.btn_skip);
        cardBoy = findViewById(R.id.card_boy);
        cardGirl = findViewById(R.id.card_girl);

        btnSubmit.setOnClickListener(v -> handleSubmit());
        btnSkip.setVisibility(View.GONE);
        cardBoy.setOnClickListener(v -> selectGender("male"));
        cardGirl.setOnClickListener(v -> selectGender("female"));
        findViewById(R.id.btn_forgot_pwd).setOnClickListener(v -> showForgotPasswordDialog());

        // 用Java代码创建Tab，确保一定显示
        tabLayout.removeAllTabs();
        tabLayout.addTab(tabLayout.newTab().setText("登录"));
        tabLayout.addTab(tabLayout.newTab().setText("注册"));

        // 先设置监听器
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                isLoginMode = tab.getPosition() == 0;
                updateUI();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // 默认选中注册Tab（位置1）
        tabLayout.selectTab(tabLayout.getTabAt(1));
    }

    private void updateUI() {
        if (isLoginMode) {
            tilNickname.setVisibility(View.GONE);
            genderLayout.setVisibility(View.GONE);
            btnSubmit.setText("登录");
        } else {
            tilNickname.setVisibility(View.VISIBLE);
            genderLayout.setVisibility(View.VISIBLE);
            btnSubmit.setText("注册");
        }
    }

    private void selectGender(String gender) {
        selectedGender = gender;
        cardBoy.setSelected(gender.equals("male"));
        cardGirl.setSelected(gender.equals("female"));
    }

    private void handleSubmit() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "请填写用户名和密码", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmit.setEnabled(false);
        new Thread(() -> {
            JSONObject result;
            if (isLoginMode) {
                result = Reporter.serverLogin(username, password);
            } else {
                if (password.length() < 4) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "密码至少4位", Toast.LENGTH_SHORT).show();
                        btnSubmit.setEnabled(true);
                    });
                    return;
                }
                String nickname = etNickname.getText().toString().trim();
                result = Reporter.serverRegister(username, password, nickname, selectedGender);
            }

            runOnUiThread(() -> {
                btnSubmit.setEnabled(true);
                try {
                    if (result.has("error")) {
                        Toast.makeText(this, result.getString("error"), Toast.LENGTH_SHORT).show();
                    } else {
                        String nickname = result.optString("nickname", username);
                        String gender = result.optString("gender", "male");
                        String avatar = result.optString("avatar", "");
                        String sessionToken = result.optString("session_token", "");
                        saveUserLocal(username, nickname, gender, avatar, sessionToken);
                        Reporter.heartbeat(this);
                        goToMain();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "网络错误，请检查网络", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void saveUserLocal(String username, String nickname, String gender, String avatar, String sessionToken) {
        getSharedPreferences("xinyu", MODE_PRIVATE).edit()
            .putString("current_user", username)
            .putString("current_nickname", nickname)
            .putString("current_gender", gender)
            .putString("current_avatar", avatar)
            .putString("session_token", sessionToken)
            .apply();
        // 同步写入本地数据库（离线可用）
        User user = new User(username, "", nickname, gender);
        user.setAvatar(avatar);
        if (db.getUser(username) == null) {
            db.registerUser(user);
        }
    }

    private void goToMain() {
        overridePendingTransition(0, 0);
        startActivity(new Intent(this, MainActivity.class));
        finish();
        overridePendingTransition(0, 0);
    }

    private void showForgotPasswordDialog() {
        String username = etUsername.getText().toString().trim();
        if (username.isEmpty()) {
            Toast.makeText(this, "请先输入用户名", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            JSONObject result = Reporter.getSecurityQuestion(username);
            runOnUiThread(() -> {
                try {
                    if (result.has("error")) {
                        Toast.makeText(this, result.getString("error"), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String question = result.getString("question");
                    showSecurityAnswerDialog(username, question);
                } catch (Exception e) {
                    Toast.makeText(this, "网络错误", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void showSecurityAnswerDialog(String username, String question) {
        android.widget.EditText etAnswer = new android.widget.EditText(this);
        etAnswer.setHint("请输入答案");
        etAnswer.setPadding(48, 32, 48, 32);

        new android.app.AlertDialog.Builder(this)
            .setTitle("安全问题: " + question)
            .setView(etAnswer)
            .setPositiveButton("验证", (d, w) -> {
                String answer = etAnswer.getText().toString().trim();
                if (answer.isEmpty()) {
                    Toast.makeText(this, "请输入答案", Toast.LENGTH_SHORT).show();
                    return;
                }
                new Thread(() -> {
                    JSONObject result = Reporter.verifySecurityAnswer(username, answer);
                    runOnUiThread(() -> {
                        try {
                            if (result.has("error")) {
                                Toast.makeText(this, result.getString("error"), Toast.LENGTH_SHORT).show();
                                return;
                            }
                            String resetToken = result.getString("reset_token");
                            showResetPasswordDialog(username, resetToken);
                        } catch (Exception e) {
                            Toast.makeText(this, "网络错误", Toast.LENGTH_SHORT).show();
                        }
                    });
                }).start();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void showResetPasswordDialog(String username, String resetToken) {
        android.widget.EditText etNewPwd = new android.widget.EditText(this);
        etNewPwd.setHint("输入新密码（至少4位）");
        etNewPwd.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
        etNewPwd.setPadding(48, 32, 48, 32);

        new android.app.AlertDialog.Builder(this)
            .setTitle("重置密码")
            .setView(etNewPwd)
            .setPositiveButton("确定", (d, w) -> {
                String newPwd = etNewPwd.getText().toString().trim();
                if (newPwd.length() < 4) {
                    Toast.makeText(this, "密码至少4位", Toast.LENGTH_SHORT).show();
                    return;
                }
                new Thread(() -> {
                    JSONObject result = Reporter.resetPassword(username, resetToken, newPwd);
                    runOnUiThread(() -> {
                        try {
                            if (result.has("error")) {
                                Toast.makeText(this, result.getString("error"), Toast.LENGTH_SHORT).show();
                                return;
                            }
                            Toast.makeText(this, "密码重置成功，请重新登录", Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(this, "网络错误", Toast.LENGTH_SHORT).show();
                        }
                    });
                }).start();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    @Override
    public void onBackPressed() {
        // 禁止返回跳过登录
        Toast.makeText(this, "请先注册或登录", Toast.LENGTH_SHORT).show();
    }
}
