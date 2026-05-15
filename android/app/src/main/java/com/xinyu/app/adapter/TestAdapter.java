package com.xinyu.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.xinyu.app.R;
import com.xinyu.app.model.TestResult;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TestAdapter extends RecyclerView.Adapter<TestAdapter.ViewHolder> {

    private List<TestResult> results = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.CHINA);

    public void setData(List<TestResult> data) {
        this.results = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_test, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TestResult result = results.get(position);
        holder.tvTitle.setText(result.getTestTitle());
        holder.tvDesc.setText("得分: " + result.getScore() + "分 | " + result.getLevel());
        holder.tvTime.setText(timeFormat.format(new Date(result.getCreatedAt())));
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDesc, tvTime;

        ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_test_title);
            tvDesc = itemView.findViewById(R.id.tv_test_desc);
            tvTime = itemView.findViewById(R.id.tv_test_time);
        }
    }
}
