package com.xinyu.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MailboxActivity extends AppCompatActivity {

    private LinearLayout layoutList, layoutWrite, layoutOpen;
    private LinearLayout layoutEmpty;
    private RecyclerView rvLetters;
    private EditText etLetter;
    private TextView tvReceiveDate, btnOpenLetter, tvOpenDate;
    private LetterAdapter adapter;
    private long selectedReceiveTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mailbox);

        initViews();
        loadLetters();
    }

    private void initViews() {
        layoutList = findViewById(R.id.layout_list);
        layoutWrite = findViewById(R.id.layout_write);
        layoutOpen = findViewById(R.id.layout_open);
        layoutEmpty = findViewById(R.id.layout_empty);
        rvLetters = findViewById(R.id.rv_letters);
        etLetter = findViewById(R.id.et_letter);
        tvReceiveDate = findViewById(R.id.tv_receive_date);
        tvOpenDate = findViewById(R.id.tv_open_date);
        btnOpenLetter = findViewById(R.id.btn_open_letter);

        findViewById(R.id.btn_back).setOnClickListener(v -> {
            if (layoutWrite.getVisibility() == View.VISIBLE) {
                showList();
            } else {
                finish();
            }
        });

        findViewById(R.id.btn_write).setOnClickListener(v -> showWrite());

        findViewById(R.id.btn_cancel).setOnClickListener(v -> showList());

        findViewById(R.id.btn_send_letter).setOnClickListener(v -> sendLetter());

        // Default receive date: 1 month later
        setSelectedDate(1);

        // Click on receive date to change
        tvReceiveDate.setOnClickListener(v -> showDatePicker());

        btnOpenLetter.setOnClickListener(v -> {
            // Already handled in adapter
        });

        rvLetters.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LetterAdapter();
        rvLetters.setAdapter(adapter);
    }

    private void setSelectedDate(int monthsLater) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, monthsLater);
        selectedReceiveTime = cal.getTimeInMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月d日", Locale.getDefault());
        tvReceiveDate.setText(sdf.format(new Date(selectedReceiveTime)) + " 收到");
    }

    private void showDatePicker() {
        String[] options = {"1个月后", "3个月后", "6个月后", "1年后", "自定义"};
        new AlertDialog.Builder(this)
                .setTitle("选择收到信的日期")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: setSelectedDate(1); break;
                        case 1: setSelectedDate(3); break;
                        case 2: setSelectedDate(6); break;
                        case 3: setSelectedDate(12); break;
                        case 4: showCustomDatePicker(); break;
                    }
                })
                .show();
    }

    private void showCustomDatePicker() {
        // Simple input dialog for days
        EditText input = new EditText(this);
        input.setHint("输入天数");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        new AlertDialog.Builder(this)
                .setTitle("自定义天数后收到")
                .setView(input)
                .setPositiveButton("确定", (d, w) -> {
                    try {
                        int days = Integer.parseInt(input.getText().toString());
                        if (days > 0 && days <= 3650) {
                            Calendar cal = Calendar.getInstance();
                            cal.add(Calendar.DAY_OF_YEAR, days);
                            selectedReceiveTime = cal.getTimeInMillis();
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月d日", Locale.getDefault());
                            tvReceiveDate.setText(sdf.format(new Date(selectedReceiveTime)) + " 收到");
                        } else {
                            Toast.makeText(this, "请输入1-3650天", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "请输入数字", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showList() {
        layoutList.setVisibility(View.VISIBLE);
        layoutWrite.setVisibility(View.GONE);
        layoutOpen.setVisibility(View.GONE);
        etLetter.setText("");
    }

    private void showWrite() {
        layoutList.setVisibility(View.GONE);
        layoutWrite.setVisibility(View.VISIBLE);
        layoutOpen.setVisibility(View.GONE);
        etLetter.requestFocus();
    }

    private void showOpen(long createTime, String content) {
        layoutList.setVisibility(View.GONE);
        layoutWrite.setVisibility(View.GONE);
        layoutOpen.setVisibility(View.VISIBLE);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月d日", Locale.getDefault());
        tvOpenDate.setText(sdf.format(new Date(createTime)) + " 写给未来");

        btnOpenLetter.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("💌 来自过去的信")
                    .setMessage("这是你在 " + sdf.format(new Date(createTime)) + " 写给未来的自己")
                    .setPositiveButton("阅读", (d, w) -> {
                        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_read_letter, null);
                        TextView tvDate = dialogView.findViewById(R.id.tv_read_date);
                        TextView tvContent = dialogView.findViewById(R.id.tv_read_content);
                        tvDate.setText(sdf.format(new Date(createTime)) + " 写给未来");
                        tvContent.setText(content);

                        new AlertDialog.Builder(this)
                                .setView(dialogView)
                                .setPositiveButton(" closes", null)
                                .show();
                    })
                    .setNegativeButton("关闭", null)
                    .show();
        });
    }

    private void sendLetter() {
        String text = etLetter.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "写点什么再寄出吧~", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String json = getSharedPreferences("xinyu", MODE_PRIVATE).getString("mailbox_letters", "[]");
            JSONArray arr = new JSONArray(json);

            JSONObject letter = new JSONObject();
            letter.put("content", text);
            letter.put("create_time", System.currentTimeMillis());
            letter.put("receive_time", selectedReceiveTime);
            letter.put("opened", false);
            arr.put(letter);

            getSharedPreferences("xinyu", MODE_PRIVATE).edit()
                    .putString("mailbox_letters", arr.toString()).apply();

            Toast.makeText(this, "💌 信已寄出，等待未来的你拆开~", Toast.LENGTH_SHORT).show();
            showList();
            loadLetters();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void loadLetters() {
        List<LetterItem> letters = new ArrayList<>();
        String json = getSharedPreferences("xinyu", MODE_PRIVATE).getString("mailbox_letters", "[]");

        try {
            JSONArray arr = new JSONArray(json);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            long now = System.currentTimeMillis();

            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                long receiveTime = obj.getLong("receive_time");
                long createTime = obj.getLong("create_time");
                boolean opened = obj.getBoolean("opened");

                // Auto-open if past receive date
                if (!opened && now >= receiveTime) {
                    obj.put("opened", true);
                    arr.put(i, obj);
                    opened = true;
                }

                String status;
                if (opened) {
                    status = "已收到";
                } else {
                    long diff = receiveTime - now;
                    long days = diff / (1000 * 60 * 60 * 24);
                    if (days <= 0) status = "可拆开";
                    else if (days < 30) status = "还有" + days + "天";
                    else {
                        long months = days / 30;
                        status = "还有" + months + "个月";
                    }
                }

                letters.add(new LetterItem(
                        obj.getString("content"),
                        createTime,
                        receiveTime,
                        opened
                ));
            }

            // Save updated state
            getSharedPreferences("xinyu", MODE_PRIVATE).edit()
                    .putString("mailbox_letters", arr.toString()).apply();

        } catch (JSONException e) {
            e.printStackTrace();
        }

        adapter.setData(letters);
        layoutEmpty.setVisibility(letters.isEmpty() ? View.VISIBLE : View.GONE);
        rvLetters.setVisibility(letters.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_out_bottom);
    }

    // Data class
    static class LetterItem {
        String content;
        long createTime;
        long receiveTime;
        boolean opened;

        LetterItem(String content, long createTime, long receiveTime, boolean opened) {
            this.content = content;
            this.createTime = createTime;
            this.receiveTime = receiveTime;
            this.opened = opened;
        }
    }

    // Adapter
    class LetterAdapter extends RecyclerView.Adapter<LetterAdapter.VH> {
        private final List<LetterItem> data = new ArrayList<>();

        void setData(List<LetterItem> items) {
            data.clear();
            data.addAll(items);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_letter, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            LetterItem item = data.get(position);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

            holder.tvPreview.setText(item.content.length() > 50 ? item.content.substring(0, 50) + "..." : item.content);
            holder.tvDate.setText(sdf.format(new Date(item.createTime)) + " 寄出");

            long now = System.currentTimeMillis();
            if (item.opened || now >= item.receiveTime) {
                holder.tvStatus.setText("已收到");
                holder.tvStatus.setBackgroundResource(R.drawable.bg_level_tag);
                holder.itemView.setOnClickListener(v -> showOpen(item.createTime, item.content));
            } else {
                long days = (item.receiveTime - now) / (1000 * 60 * 60 * 24);
                if (days < 30) {
                    holder.tvStatus.setText("还有" + days + "天");
                } else {
                    long months = days / 30;
                    holder.tvStatus.setText("还有" + months + "个月");
                }
                holder.tvStatus.setBackgroundColor(getResources().getColor(R.color.primary, null));
                holder.itemView.setOnClickListener(v ->
                        Toast.makeText(MailboxActivity.this, "还没到拆开的时候哦~ 🤫", Toast.LENGTH_SHORT).show());
            }
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvPreview, tvDate, tvStatus;
            VH(View v) {
                super(v);
                tvPreview = v.findViewById(R.id.tv_letter_preview);
                tvDate = v.findViewById(R.id.tv_letter_date);
                tvStatus = v.findViewById(R.id.tv_letter_status);
            }
        }
    }
}
