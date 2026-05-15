package com.xinyu.app;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AIChatActivity extends AppCompatActivity {

    private EditText etInput;
    private RecyclerView chatList;
    private TextView tvTyping;
    private ChatAdapter chatAdapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;

    private static final String API_KEY = "tp-s1jk693d4nknf8t97g317idgf0ys6k9bk32i8s0v7j3abuzq";
    private static final String API_URL = "https://token-plan-sgp.xiaomimimo.com/anthropic/v1/messages";
    private static final String MODEL = "mimo-v2.5";
    private static final String SESSIONS_KEY = "ai_chat_sessions";

    private long currentSessionId;
    private boolean isViewingHistory = false;
    private static final String SYSTEM_PROMPT = "你是一个专业的心理健康助手，名叫\"心屿\"。你擅长倾听和共情，能够帮助用户缓解情绪困扰。你的回答应该：\n" +
            "1. 温暖、有同理心，让用户感到被理解\n" +
            "2. 提供实用的建议和技巧\n" +
            "3. 如果发现严重心理问题，建议寻求专业帮助\n" +
            "4. 不做医学诊断，保持支持性态度\n" +
            "5. 回答简洁清晰，适合手机阅读\n" +
            "6. 使用中文回答";

    private final List<String[]> messages = new ArrayList<>(); // {role, content}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        prefs = getSharedPreferences("xinyu", MODE_PRIVATE);
        currentSessionId = System.currentTimeMillis();
        initViews();
        setupChat();
        loadChatHistory();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!messages.isEmpty()) {
            saveSession();
        }
    }

    private void initViews() {
        etInput = findViewById(R.id.et_input);
        chatList = findViewById(R.id.chat_list);
        tvTyping = findViewById(R.id.tv_typing);

        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { finish(); }
        });

        findViewById(R.id.btn_send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { sendMessage(); }
        });

        findViewById(R.id.btn_history).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { showHistoryDialog(); }
        });

        findViewById(R.id.btn_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(AIChatActivity.this)
                        .setTitle("清空对话")
                        .setMessage("确定要清空所有对话记录吗？")
                        .setPositiveButton("清空", (dialog, which) -> {
                            messages.clear();
                            chatAdapter.clear();
                            // Clear all sessions
                            prefs.edit().remove(SESSIONS_KEY).apply();
                            prefs.edit().remove("ai_chat_history").apply();
                            currentSessionId = System.currentTimeMillis();
                            isViewingHistory = false;
                            addAIMessage("你好！我是心屿AI心理助手 🌸\n\n有什么想聊的吗？");
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        });
    }

    private void setupChat() {
        chatAdapter = new ChatAdapter();
        chatList.setLayoutManager(new LinearLayoutManager(this));
        chatList.setAdapter(chatAdapter);
    }

    private void sendMessage() {
        String text = etInput.getText().toString().trim();
        if (text.isEmpty()) return;

        // If viewing history, start a new conversation first
        if (isViewingHistory) {
            if (!messages.isEmpty()) {
                saveSession();
            }
            messages.clear();
            chatAdapter.clear();
            currentSessionId = System.currentTimeMillis();
            isViewingHistory = false;
        }

        etInput.setText("");
        addUserMessage(text);
        showTyping(true);
        callAI(text);
    }

    private void addUserMessage(String text) {
        messages.add(new String[]{"user", text});
        chatAdapter.addMessage("user", text);
        isViewingHistory = false;
        saveSession();
        scrollToBottom();
    }

    private void addAIMessage(String text) {
        messages.add(new String[]{"assistant", text});
        chatAdapter.addMessage("assistant", text);
        saveSession();
        scrollToBottom();
    }

    private void scrollToBottom() {
        chatList.post(new Runnable() {
            @Override
            public void run() {
                if (chatAdapter.getItemCount() > 0) {
                    chatList.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
                }
            }
        });
    }

    private void loadChatHistory() {
        // Load the latest session
        String sessionsJson = prefs.getString(SESSIONS_KEY, "[]");
        try {
            JSONArray sessions = new JSONArray(sessionsJson);
            if (sessions.length() > 0) {
                // Load the most recent session
                JSONObject latest = sessions.getJSONObject(sessions.length() - 1);
                currentSessionId = latest.getLong("id");
                JSONArray msgs = latest.getJSONArray("messages");
                for (int i = 0; i < msgs.length(); i++) {
                    JSONObject obj = msgs.getJSONObject(i);
                    String role = obj.getString("role");
                    String content = obj.getString("content");
                    messages.add(new String[]{role, content});
                    chatAdapter.addMessageDirect(role, content);
                }
                if (messages.isEmpty()) {
                    addAIMessage("你好！我是心屿AI心理助手 🌸\n\n有什么想聊的吗？你可以告诉我你今天的心情、遇到的困扰，或者任何想倾诉的事情。我会认真倾听并尽力帮助你。");
                } else {
                    scrollToBottom();
                }
            } else {
                // Also migrate old format if exists
                String oldJson = prefs.getString("ai_chat_history", "[]");
                JSONArray oldArr = new JSONArray(oldJson);
                if (oldArr.length() > 0) {
                    // Migrate to sessions format
                    JSONObject session = new JSONObject();
                    session.put("id", currentSessionId);
                    session.put("time", currentSessionId);
                    session.put("messages", oldArr);
                    JSONArray newSessions = new JSONArray();
                    newSessions.put(session);
                    prefs.edit().putString(SESSIONS_KEY, newSessions.toString()).apply();
                    prefs.edit().remove("ai_chat_history").apply();

                    for (int i = 0; i < oldArr.length(); i++) {
                        JSONObject obj = oldArr.getJSONObject(i);
                        messages.add(new String[]{obj.getString("role"), obj.getString("content")});
                        chatAdapter.addMessageDirect(obj.getString("role"), obj.getString("content"));
                    }
                    scrollToBottom();
                } else {
                    addAIMessage("你好！我是心屿AI心理助手 🌸\n\n有什么想聊的吗？你可以告诉我你今天的心情、遇到的困扰，或者任何想倾诉的事情。我会认真倾听并尽力帮助你。");
                }
            }
        } catch (Exception e) {
            addAIMessage("你好！我是心屿AI心理助手 🌸\n\n有什么想聊的吗？");
        }
    }

    private void saveSession() {
        try {
            String sessionsJson = prefs.getString(SESSIONS_KEY, "[]");
            JSONArray sessions = new JSONArray(sessionsJson);

            // Build messages array for this session
            JSONArray msgsArr = new JSONArray();
            for (String[] msg : messages) {
                JSONObject obj = new JSONObject();
                obj.put("role", msg[0]);
                obj.put("content", msg[1]);
                msgsArr.put(obj);
            }

            // Find and update existing session or create new one
            boolean found = false;
            for (int i = 0; i < sessions.length(); i++) {
                JSONObject s = sessions.getJSONObject(i);
                if (s.getLong("id") == currentSessionId) {
                    s.put("messages", msgsArr);
                    s.put("time", System.currentTimeMillis());
                    sessions.put(i, s);
                    found = true;
                    break;
                }
            }

            if (!found) {
                JSONObject session = new JSONObject();
                session.put("id", currentSessionId);
                session.put("time", System.currentTimeMillis());
                session.put("messages", msgsArr);
                sessions.put(session);
            }

            prefs.edit().putString(SESSIONS_KEY, sessions.toString()).apply();
        } catch (Exception e) {
            // Ignore
        }
    }

    private void startNewConversation() {
        // Save current session first
        if (!messages.isEmpty()) {
            saveSession();
        }
        // Clear and start fresh
        messages.clear();
        chatAdapter.clear();
        currentSessionId = System.currentTimeMillis();
        isViewingHistory = false;
        addAIMessage("你好！我是心屿AI心理助手 🌸\n\n有什么想聊的吗？");
    }

    private void loadSession(long sessionId) {
        try {
            String sessionsJson = prefs.getString(SESSIONS_KEY, "[]");
            JSONArray sessions = new JSONArray(sessionsJson);
            for (int i = 0; i < sessions.length(); i++) {
                JSONObject s = sessions.getJSONObject(i);
                if (s.getLong("id") == sessionId) {
                    messages.clear();
                    chatAdapter.clear();
                    JSONArray msgs = s.getJSONArray("messages");
                    for (int j = 0; j < msgs.length(); j++) {
                        JSONObject obj = msgs.getJSONObject(j);
                        String role = obj.getString("role");
                        String content = obj.getString("content");
                        messages.add(new String[]{role, content});
                        chatAdapter.addMessageDirect(role, content);
                    }
                    chatAdapter.notifyDataSetChanged();
                    isViewingHistory = true;
                    scrollToBottom();
                    break;
                }
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    private void showHistoryDialog() {
        try {
            String sessionsJson = prefs.getString(SESSIONS_KEY, "[]");
            JSONArray sessions = new JSONArray(sessionsJson);

            // Also include old format if exists
            String oldJson = prefs.getString("ai_chat_history", "[]");
            JSONArray oldArr = new JSONArray(oldJson);
            if (oldArr.length() > 0) {
                JSONObject session = new JSONObject();
                session.put("id", 0L);
                session.put("time", 0L);
                session.put("messages", oldArr);
                JSONArray merged = new JSONArray();
                merged.put(session);
                for (int i = 0; i < sessions.length(); i++) {
                    merged.put(sessions.getJSONObject(i));
                }
                sessions = merged;
            }

            final AlertDialog[] dialogHolder = new AlertDialog[1];

            // Root container with warm background
            LinearLayout root = new LinearLayout(this);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setBackgroundResource(R.drawable.bg_history_dialog);
            root.setPadding(0, 0, 0, 16);

            // Header area — cute icon + title
            LinearLayout headerArea = new LinearLayout(this);
            headerArea.setOrientation(LinearLayout.VERTICAL);
            headerArea.setGravity(Gravity.CENTER_HORIZONTAL);
            headerArea.setPadding(40, 36, 40, 20);

            TextView headerIcon = new TextView(this);
            headerIcon.setText("🌸");
            headerIcon.setTextSize(36);
            headerIcon.setGravity(Gravity.CENTER);
            headerArea.addView(headerIcon);

            TextView headerTitle = new TextView(this);
            headerTitle.setText("我们的对话回忆");
            headerTitle.setTextSize(18);
            headerTitle.setTextColor(0xFF4A3728);
            headerTitle.setTypeface(null, Typeface.BOLD);
            headerTitle.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            titleLp.topMargin = 8;
            headerArea.addView(headerTitle, titleLp);

            TextView headerSub = new TextView(this);
            headerSub.setText("每一段对话，心屿都记在心里 💌");
            headerSub.setTextSize(13);
            headerSub.setTextColor(0xFFB39DDB);
            headerSub.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            subLp.topMargin = 4;
            headerArea.addView(headerSub, subLp);

            View divider = new View(this);
            divider.setBackgroundColor(0x15FFB6C1);
            LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1);
            divLp.setMargins(40, 0, 40, 0);
            headerArea.addView(divider, divLp);

            root.addView(headerArea);

            if (sessions.length() == 0) {
                // Empty state
                LinearLayout emptyLayout = new LinearLayout(this);
                emptyLayout.setOrientation(LinearLayout.VERTICAL);
                emptyLayout.setGravity(Gravity.CENTER);
                emptyLayout.setPadding(40, 60, 40, 60);

                TextView emptyIcon = new TextView(this);
                emptyIcon.setText("🐾");
                emptyIcon.setTextSize(40);
                emptyIcon.setGravity(Gravity.CENTER);
                emptyLayout.addView(emptyIcon);

                TextView emptyText = new TextView(this);
                emptyText.setText("还没有对话记录哦~\n和心屿聊聊天吧！");
                emptyText.setTextSize(14);
                emptyText.setTextColor(0xFFBDBDBD);
                emptyText.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams emptyTextLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                emptyTextLp.topMargin = 12;
                emptyLayout.addView(emptyText, emptyTextLp);

                root.addView(emptyLayout);
            } else {
                // Scrollable session list
                LinearLayout listContainer = new LinearLayout(this);
                listContainer.setOrientation(LinearLayout.VERTICAL);
                listContainer.setPadding(28, 12, 28, 0);

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.getDefault());
                String lastDate = "";

                for (int i = sessions.length() - 1; i >= 0; i--) {
                    JSONObject s = sessions.getJSONObject(i);
                    long time = s.getLong("time");
                    JSONArray msgs = s.getJSONArray("messages");

                    // Date separator
                    String currentDate = time > 0 ? sdf.format(new Date(time)).substring(0, 9) : "";
                    if (!currentDate.isEmpty() && !currentDate.equals(lastDate)) {
                        lastDate = currentDate;
                        TextView dateSep = new TextView(this);
                        dateSep.setText("  📅 " + currentDate);
                        dateSep.setTextSize(12);
                        dateSep.setTextColor(0xFFCCBBBB);
                        dateSep.setTypeface(null, Typeface.BOLD);
                        LinearLayout.LayoutParams dateLp = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        dateLp.topMargin = 12;
                        dateLp.bottomMargin = 6;
                        listContainer.addView(dateSep, dateLp);
                    }

                    // Get first user message as preview
                    String preview = "和心屿的悄悄话~";
                    for (int j = 0; j < msgs.length(); j++) {
                        JSONObject m = msgs.getJSONObject(j);
                        if ("user".equals(m.getString("role"))) {
                            String txt = m.getString("content");
                            preview = txt.length() > 25 ? txt.substring(0, 25) + "..." : txt;
                            break;
                        }
                    }

                    int msgCount = msgs.length();
                    String timeStr = time > 0 ?
                            new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(time)) : "";

                    // Card container
                    LinearLayout card = new LinearLayout(this);
                    card.setOrientation(LinearLayout.HORIZONTAL);
                    card.setGravity(Gravity.CENTER_VERTICAL);
                    card.setBackgroundResource(R.drawable.bg_history_card);
                    card.setPadding(24, 20, 20, 20);
                    LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    cardLp.bottomMargin = 10;
                    card.setClickable(true);
                    card.setFocusable(true);

                    // Chat bubble icon
                    TextView icon = new TextView(this);
                    icon.setText(msgCount > 4 ? "💬" : "🗨️");
                    icon.setTextSize(22);
                    card.addView(icon);

                    // Text content
                    LinearLayout textArea = new LinearLayout(this);
                    textArea.setOrientation(LinearLayout.VERTICAL);
                    LinearLayout.LayoutParams textAreaLp = new LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                    textAreaLp.leftMargin = 14;
                    textArea.setLayoutParams(textAreaLp);

                    TextView previewText = new TextView(this);
                    previewText.setText(preview);
                    previewText.setTextSize(15);
                    previewText.setTextColor(0xFF4A3728);
                    previewText.setTypeface(null, Typeface.BOLD);
                    previewText.setMaxLines(1);
                    textArea.addView(previewText);

                    LinearLayout metaRow = new LinearLayout(this);
                    metaRow.setOrientation(LinearLayout.HORIZONTAL);
                    LinearLayout.LayoutParams metaRowLp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    metaRowLp.topMargin = 4;
                    metaRow.setLayoutParams(metaRowLp);

                    TextView timeText = new TextView(this);
                    timeText.setText(timeStr);
                    timeText.setTextSize(12);
                    timeText.setTextColor(0xFFCCBBBB);
                    metaRow.addView(timeText);

                    TextView countText = new TextView(this);
                    countText.setText("  ·  " + msgCount + "条消息");
                    countText.setTextSize(12);
                    countText.setTextColor(0xFFCCBBBB);
                    metaRow.addView(countText);

                    textArea.addView(metaRow);
                    card.addView(textArea);

                    // Arrow
                    TextView arrow = new TextView(this);
                    arrow.setText("›");
                    arrow.setTextSize(20);
                    arrow.setTextColor(0xFFCCBBBB);
                    card.addView(arrow);

                    listContainer.addView(card, cardLp);

                    final long sid = s.getLong("id");
                    card.setOnClickListener(v -> {
                        loadSession(sid);
                        if (dialogHolder[0] != null) dialogHolder[0].dismiss();
                    });
                }

                ScrollView scroll = new ScrollView(this);
                scroll.addView(listContainer);
                scroll.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
                root.addView(scroll, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
            }

            // Bottom button — new conversation
            LinearLayout btnArea = new LinearLayout(this);
            btnArea.setGravity(Gravity.CENTER);
            btnArea.setPadding(28, 16, 28, 0);

            TextView newBtn = new TextView(this);
            newBtn.setText("  ✨ 开始新对话");
            newBtn.setTextSize(15);
            newBtn.setTextColor(Color.WHITE);
            newBtn.setTypeface(null, Typeface.BOLD);
            newBtn.setPadding(48, 22, 48, 22);
            newBtn.setBackgroundResource(R.drawable.bg_history_new_btn);
            newBtn.setGravity(Gravity.CENTER);
            btnArea.addView(newBtn);

            root.addView(btnArea);

            dialogHolder[0] = new AlertDialog.Builder(this)
                    .setView(root)
                    .create();

            if (dialogHolder[0].getWindow() != null) {
                dialogHolder[0].getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            newBtn.setOnClickListener(v -> {
                startNewConversation();
                if (dialogHolder[0] != null) dialogHolder[0].dismiss();
            });

            dialogHolder[0].show();
        } catch (Exception e) {
            Toast.makeText(this, "加载历史失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void showTyping(boolean show) {
        tvTyping.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) scrollToBottom();
    }

    private void callAI(String userMessage) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String gender = getSharedPreferences("xinyu", MODE_PRIVATE)
                            .getString("current_gender", "male");

                    JSONObject body = new JSONObject();
                    body.put("message", userMessage);
                    body.put("gender", gender);

                    URL url = new URL("http://121.41.211.25/api/chat");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(60000);

                    OutputStream os = conn.getOutputStream();
                    os.write(body.toString().getBytes("UTF-8"));
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    BufferedReader reader;
                    if (responseCode == 200) {
                        reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    } else {
                        reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    }

                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();
                    conn.disconnect();

                    JSONObject response = new JSONObject(sb.toString());
                    String reply = response.optString("reply", "AI助手暂时不可用");

                    final String finalReply = reply;
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            showTyping(false);
                            addAIMessage(finalReply);
                        }
                    });
                } catch (Exception e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            showTyping(false);
                            addAIMessage("网络连接失败，请检查网络后重试。\n\n你也可以先记录心情，等网络恢复后再聊。");
                        }
                    });
                }
            }
        });
    }

    // Chat Adapter
    private class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
        private final List<String[]> chatMessages = new ArrayList<>();

        void addMessage(String role, String content) {
            chatMessages.add(new String[]{role, content});
            notifyDataSetChanged();
        }

        void addMessageDirect(String role, String content) {
            chatMessages.add(new String[]{role, content});
        }

        void removeMessage(int position) {
            if (position >= 0 && position < chatMessages.size()) {
                chatMessages.remove(position);
                // Also remove from messages list
                if (position < messages.size()) {
                    messages.remove(position);
                }
                notifyDataSetChanged();
                saveSession();
            }
        }

        void clear() {
            chatMessages.clear();
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return "user".equals(chatMessages.get(position)[0]) ? 1 : 0;
        }

        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_message, parent, false);
            return new ChatViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            String[] msg = chatMessages.get(position);
            boolean isUser = "user".equals(msg[0]);

            holder.layoutUser.setVisibility(isUser ? View.VISIBLE : View.GONE);
            holder.layoutAI.setVisibility(isUser ? View.GONE : View.VISIBLE);

            if (isUser) {
                holder.tvUserMessage.setText(msg[1]);
            } else {
                holder.tvAiMessage.setText(msg[1]);
                // Set daily avatar based on day of month
                int day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
                int[] avatars = {R.drawable.ic_cat_spring, R.drawable.ic_cat_star, R.drawable.ic_cat_rain, R.drawable.ic_cat_leaf};
                holder.ivAiAvatar.setImageResource(avatars[day % avatars.length]);
            }

            // Long press to delete
            View bubble = isUser ? holder.layoutUser : holder.layoutAI;
            bubble.setOnLongClickListener(v -> {
                new AlertDialog.Builder(AIChatActivity.this)
                        .setTitle("删除消息")
                        .setMessage("确定要删除这条消息吗？")
                        .setPositiveButton("删除", (dialog, which) -> {
                            removeMessage(holder.getAdapterPosition());
                        })
                        .setNegativeButton("取消", null)
                        .show();
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return chatMessages.size();
        }

        class ChatViewHolder extends RecyclerView.ViewHolder {
            LinearLayout layoutUser, layoutAI;
            TextView tvUserMessage, tvAiMessage;
            ImageView ivAiAvatar;

            ChatViewHolder(@NonNull View itemView) {
                super(itemView);
                layoutUser = itemView.findViewById(R.id.layout_user);
                layoutAI = itemView.findViewById(R.id.layout_ai);
                tvUserMessage = itemView.findViewById(R.id.tv_user_message);
                tvAiMessage = itemView.findViewById(R.id.tv_ai_message);
                ivAiAvatar = itemView.findViewById(R.id.iv_ai_avatar);
            }
        }
    }
}
