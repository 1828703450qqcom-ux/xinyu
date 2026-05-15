package com.xinyu.app.fragment;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.xinyu.app.R;

public class EmotionFragment extends Fragment {

    private FrameLayout fragmentContainer;
    private TextView tabMood, tabTest;

    private MoodFragment moodFragment;
    private TestFragment testFragment;
    private Fragment activeFragment;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_emotion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fragmentContainer = view.findViewById(R.id.fragment_container);
        tabMood = view.findViewById(R.id.tab_mood);
        tabTest = view.findViewById(R.id.tab_test);

        tabMood.setOnClickListener(v -> switchTab(0));
        tabTest.setOnClickListener(v -> switchTab(1));

        if (savedInstanceState == null) {
            moodFragment = new MoodFragment();
            testFragment = new TestFragment();
            getChildFragmentManager().beginTransaction()
                .add(R.id.fragment_container, testFragment, "test").hide(testFragment)
                .add(R.id.fragment_container, moodFragment, "mood")
                .commit();
            activeFragment = moodFragment;
        } else {
            moodFragment = (MoodFragment) getChildFragmentManager().findFragmentByTag("mood");
            testFragment = (TestFragment) getChildFragmentManager().findFragmentByTag("test");
            activeFragment = moodFragment;
        }
    }

    private void switchTab(int index) {
        Fragment target = index == 0 ? moodFragment : testFragment;
        if (target == activeFragment) return;

        getChildFragmentManager().beginTransaction()
            .hide(activeFragment).show(target).commit();
        activeFragment = target;

        tabMood.setTextColor(Color.parseColor("#B0A89E"));
        tabMood.setTypeface(Typeface.DEFAULT);
        tabMood.setBackgroundResource(0);
        tabTest.setTextColor(Color.parseColor("#B0A89E"));
        tabTest.setTypeface(Typeface.DEFAULT);
        tabTest.setBackgroundResource(0);

        if (index == 0) {
            tabMood.setTextColor(getResources().getColor(R.color.primary, null));
            tabMood.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            tabMood.setBackgroundResource(R.drawable.bg_tab_selected);
        } else {
            tabTest.setTextColor(getResources().getColor(R.color.primary, null));
            tabTest.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            tabTest.setBackgroundResource(R.drawable.bg_tab_selected);
        }
    }

    public void refresh() {
        if (activeFragment instanceof MoodFragment) {
            ((MoodFragment) activeFragment).refresh();
        }
    }
}
