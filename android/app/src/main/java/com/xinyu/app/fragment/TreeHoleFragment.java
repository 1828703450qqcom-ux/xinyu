package com.xinyu.app.fragment;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.xinyu.app.R;
import com.xinyu.app.SquareFragment;
import com.xinyu.app.db.AppDatabase;
import com.xinyu.app.fragment.SupportFragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TreeHoleFragment extends Fragment {

    private EditText etContent;
    private View btnBury;
    private View editorContainer;
    private View fragmentContainer;
    private View successContainer;
    private View successDone;
    private TabLayout tabLayout;
    private SquareFragment squareFragment;
    private SupportFragment supportFragment;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_treehole, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etContent = view.findViewById(R.id.et_content);
        btnBury = view.findViewById(R.id.btn_bury);
        View btnHistory = view.findViewById(R.id.btn_history);
        editorContainer = view.findViewById(R.id.editor_container);
        fragmentContainer = view.findViewById(R.id.fragment_container);
        successContainer = view.findViewById(R.id.success_container);
        successDone = view.findViewById(R.id.success_done);
        tabLayout = view.findViewById(R.id.tab_layout);

        btnBury.setOnClickListener(v -> buryConfession());
        successDone.setOnClickListener(v -> showEditor());
        btnHistory.setOnClickListener(v -> showHistory());

        tabLayout.addTab(tabLayout.newTab().setText("🕳️ 树洞"));
        tabLayout.addTab(tabLayout.newTab().setText("🌐 广播"));
        tabLayout.addTab(tabLayout.newTab().setText("🤝 互助"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                hideAllFragments();
                if (tab.getPosition() == 0) {
                    editorContainer.setVisibility(View.VISIBLE);
                    fragmentContainer.setVisibility(View.GONE);
                } else {
                    editorContainer.setVisibility(View.GONE);
                    fragmentContainer.setVisibility(View.VISIBLE);
                    if (tab.getPosition() == 1) {
                        showSquareFragment();
                    } else {
                        showSupportFragment();
                    }
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void showSquareFragment() {
        if (squareFragment == null) {
            // 尝试从FragmentManager恢复
            squareFragment = (SquareFragment) getChildFragmentManager().findFragmentByTag("square");
        }
        if (squareFragment == null) {
            squareFragment = new SquareFragment();
            getChildFragmentManager().beginTransaction()
                .add(R.id.fragment_container, squareFragment, "square")
                .commitAllowingStateLoss();
        } else {
            getChildFragmentManager().beginTransaction().show(squareFragment).commitAllowingStateLoss();
        }
    }

    private void showSupportFragment() {
        if (supportFragment == null) {
            // 尝试从FragmentManager恢复
            supportFragment = (SupportFragment) getChildFragmentManager().findFragmentByTag("support");
        }
        if (supportFragment == null) {
            supportFragment = new SupportFragment();
            getChildFragmentManager().beginTransaction()
                .add(R.id.fragment_container, supportFragment, "support")
                .commitAllowingStateLoss();
        } else {
            getChildFragmentManager().beginTransaction().show(supportFragment).commitAllowingStateLoss();
        }
    }

    private void hideAllFragments() {
        // 从FragmentManager查找，确保不遗漏
        SquareFragment sf = (SquareFragment) getChildFragmentManager().findFragmentByTag("square");
        SupportFragment spf = (SupportFragment) getChildFragmentManager().findFragmentByTag("support");
        androidx.fragment.app.FragmentTransaction t = getChildFragmentManager().beginTransaction();
        if (sf != null) t.hide(sf);
        if (spf != null) t.hide(spf);
        t.commitAllowingStateLoss();
    }

    private void buryConfession() {
        String content = etContent.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(getContext(), "写点什么再埋掉~", Toast.LENGTH_SHORT).show();
            return;
        }
        String filterErr = com.xinyu.app.util.ContentFilter.check(content);
        if (filterErr != null) {
            Toast.makeText(getContext(), filterErr, Toast.LENGTH_SHORT).show();
            return;
        }

        saveConfession(content);
        etContent.setText("");
        showSuccess();
    }

    private void saveConfession(String text) {
        try {
            String key = "tree_hole_list";
            String existing = requireContext().getSharedPreferences("xinyu", 0).getString(key, "[]");
            JSONArray arr = new JSONArray(existing);
            JSONObject obj = new JSONObject();
            obj.put("text", text);
            obj.put("time", System.currentTimeMillis());
            arr.put(obj);
            requireContext().getSharedPreferences("xinyu", 0).edit().putString(key, arr.toString()).apply();
        } catch (Exception e) {
            // ignore
        }
    }

    private void showSuccess() {
        editorContainer.setVisibility(View.GONE);
        fragmentContainer.setVisibility(View.GONE);
        successContainer.setVisibility(View.VISIBLE);

        successContainer.setAlpha(0f);
        successContainer.setScaleX(0.8f);
        successContainer.setScaleY(0.8f);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
            ObjectAnimator.ofFloat(successContainer, "alpha", 0f, 1f),
            ObjectAnimator.ofFloat(successContainer, "scaleX", 0.8f, 1f),
            ObjectAnimator.ofFloat(successContainer, "scaleY", 0.8f, 1f)
        );
        set.setDuration(500);
        set.setInterpolator(new OvershootInterpolator());
        set.start();
    }

    private void showEditor() {
        successContainer.setVisibility(View.GONE);
        editorContainer.setVisibility(View.VISIBLE);
        fragmentContainer.setVisibility(View.GONE);
        tabLayout.selectTab(tabLayout.getTabAt(0));
    }

    public void showHistory() {
        List<String[]> entries = loadHistory();
        if (entries.isEmpty()) {
            Toast.makeText(getContext(), "树洞里空空的~", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_tree_hole_history, null);
        LinearLayout listContainer = dialogView.findViewById(R.id.history_list_container);
        TextView tvEmpty = dialogView.findViewById(R.id.tv_empty);
        tvEmpty.setVisibility(View.GONE);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        for (int i = entries.size() - 1; i >= 0; i--) {
            String[] entry = entries.get(i);
            View itemView = LayoutInflater.from(getContext()).inflate(R.layout.item_tree_hole_history, listContainer, false);
            TextView tvContent = itemView.findViewById(R.id.tv_hole_content);
            TextView tvTime = itemView.findViewById(R.id.tv_hole_time);
            tvContent.setText(entry[0]);
            tvTime.setText(sdf.format(new Date(Long.parseLong(entry[1]))));
            listContainer.addView(itemView);
        }

        new AlertDialog.Builder(requireContext())
            .setTitle("📖 树洞历史")
            .setView(dialogView)
            .setPositiveButton("关闭", null)
            .setNeutralButton("清空", (d, w) -> {
                requireContext().getSharedPreferences("xinyu", 0).edit()
                    .remove("tree_hole_list").apply();
                Toast.makeText(getContext(), "已清空树洞", Toast.LENGTH_SHORT).show();
            })
            .show();
    }

    private List<String[]> loadHistory() {
        List<String[]> list = new ArrayList<>();
        String json = requireContext().getSharedPreferences("xinyu", 0).getString("tree_hole_list", "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(new String[]{obj.getString("text"), String.valueOf(obj.getLong("time"))});
            }
        } catch (Exception e) { /* ignore */ }
        return list;
    }

    public void refresh() {
        if (squareFragment != null) {
            // Square fragment handles its own refresh in onResume
        }
    }
}
