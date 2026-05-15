package com.xinyu.app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xinyu.app.model.SupportMessage;
import com.xinyu.app.util.Reporter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SupportChatActivity extends AppCompatActivity {

    private int matchId;
    private String partnerName;
    private RecyclerView msgList;
    private EditText etInput;
    private TextView tvTitle;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private List<SupportMessage> messages = new ArrayList<>();
    private ChatAdapter adapter;
    private String myDeviceId;
    private long lastMessageId = 0; // 跟踪最新消息ID，避免重复

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support_chat);

        matchId = getIntent().getIntExtra("match_id", 0);
        partnerName = getIntent().getStringExtra("partner_name");
        myDeviceId = Reporter.getDeviceId(this);

        tvTitle = findViewById(R.id.tv_title);
        tvTitle.setText(partnerName);
        msgList = findViewById(R.id.msg_list);
        etInput = findViewById(R.id.et_input);

        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        msgList.setLayoutManager(lm);
        adapter = new ChatAdapter();
        msgList.setAdapter(adapter);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_send).setOnClickListener(v -> sendMessage());
        findViewById(R.id.btn_report).setOnClickListener(v -> reportUser());

        loadMessages();

        // 1秒轮询，实现即时聊天
        mainHandler.postDelayed(refreshRunnable, 1000);
    }

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            loadMessages();
            mainHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacks(refreshRunnable);
    }

    private void loadMessages() {
        executor.execute(() -> {
            JSONObject result = Reporter.supportGetMessages(matchId);
            mainHandler.post(() -> {
                try {
                    JSONArray msgs = result.optJSONArray("messages");
                    if (msgs != null && msgs.length() > 0) {
                        // 检查是否有新消息
                        JSONObject lastMsg = msgs.getJSONObject(msgs.length() - 1);
                        long newLastId = lastMsg.optInt("id", 0);
                        boolean hasNew = newLastId > lastMessageId;
                        lastMessageId = newLastId;

                        messages.clear();
                        for (int i = 0; i < msgs.length(); i++) {
                            JSONObject m = msgs.getJSONObject(i);
                            SupportMessage msg = new SupportMessage();
                            msg.setId(m.optInt("id"));
                            msg.setMatchId(m.optInt("match_id"));
                            msg.setSenderDeviceId(m.optString("sender_device_id"));
                            msg.setContent(m.optString("content"));
                            msg.setRead(m.optInt("is_read", 0) == 1);
                            msg.setCreatedAt(m.optLong("created_at"));
                            messages.add(msg);
                        }
                        adapter.notifyDataSetChanged();
                        // 标记对方消息为已读
                        executor.execute(() -> Reporter.markMessagesRead(SupportChatActivity.this, matchId));
                        if (hasNew || messages.size() == msgs.length()) {
                            msgList.scrollToPosition(messages.size() - 1);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
    }

    private void sendMessage() {
        String content = etInput.getText().toString().trim();
        if (content.isEmpty()) return;
        String filterErr = com.xinyu.app.util.ContentFilter.check(content);
        if (filterErr != null) {
            Toast.makeText(this, filterErr, Toast.LENGTH_SHORT).show();
            return;
        }
        etInput.setText("");

        // 乐观更新：立即显示发出的消息
        SupportMessage localMsg = new SupportMessage();
        localMsg.setId((int)(System.currentTimeMillis()));
        localMsg.setMatchId(matchId);
        localMsg.setSenderDeviceId(myDeviceId);
        localMsg.setContent(content);
        localMsg.setCreatedAt(System.currentTimeMillis() / 1000);
        messages.add(localMsg);
        adapter.notifyItemInserted(messages.size() - 1);
        msgList.scrollToPosition(messages.size() - 1);

        executor.execute(() -> {
            JSONObject result = Reporter.supportSendMessage(this, matchId, content);
            mainHandler.post(() -> {
                if (result.has("error")) {
                    Toast.makeText(this, "发送失败", Toast.LENGTH_SHORT).show();
                    // 移除乐观添加的消息
                    messages.remove(messages.size() - 1);
                    adapter.notifyItemRemoved(messages.size());
                } else {
                    // 发送成功，用服务器数据刷新（获取真实ID）
                    loadMessages();
                }
            });
        });
    }

    private void reportUser() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("举报用户")
                .setMessage("确定要举报 " + partnerName + " 吗？举报后将解除匹配关系。")
                .setPositiveButton("举报", (d, w) -> {
                    executor.execute(() -> {
                        JSONObject result = Reporter.supportReport(this, partnerName, "用户举报");
                        mainHandler.post(() -> {
                            Toast.makeText(this, "已举报", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }

    class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.VH> {
        @Override
        public int getItemViewType(int position) {
            SupportMessage msg = messages.get(position);
            String senderId = msg.getSenderDeviceId();
            if (senderId == null || myDeviceId == null) return 0;
            return senderId.equals(myDeviceId) ? 1 : 0;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_support_message, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            SupportMessage msg = messages.get(position);
            String senderId = msg.getSenderDeviceId();
            boolean isSelf = senderId != null && senderId.equals(myDeviceId);

            holder.layoutOther.setVisibility(isSelf ? View.GONE : View.VISIBLE);
            holder.layoutSelf.setVisibility(isSelf ? View.VISIBLE : View.GONE);

            if (!isSelf) {
                holder.tvOtherName.setText(partnerName);
                holder.tvOtherMsg.setText(msg.getContent());
                holder.tvReadStatus.setVisibility(View.GONE);
            } else {
                holder.tvSelfMsg.setText(msg.getContent());
                holder.tvReadStatus.setVisibility(View.VISIBLE);
                holder.tvReadStatus.setText(msg.isRead() ? "已读" : "未读");
            }
        }

        @Override
        public int getItemCount() { return messages.size(); }

        class VH extends RecyclerView.ViewHolder {
            View layoutOther, layoutSelf;
            TextView tvOtherName, tvOtherMsg, tvSelfMsg, tvReadStatus;
            VH(View v) {
                super(v);
                layoutOther = v.findViewById(R.id.layout_other);
                layoutSelf = v.findViewById(R.id.layout_self);
                tvOtherName = v.findViewById(R.id.tv_other_name);
                tvOtherMsg = v.findViewById(R.id.tv_other_msg);
                tvSelfMsg = v.findViewById(R.id.tv_self_msg);
                tvReadStatus = v.findViewById(R.id.tv_read_status);
            }
        }
    }
}
