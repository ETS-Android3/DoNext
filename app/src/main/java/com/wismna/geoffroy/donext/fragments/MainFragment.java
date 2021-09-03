package com.wismna.geoffroy.donext.fragments;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Guideline;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.wismna.geoffroy.donext.R;
import com.wismna.geoffroy.donext.activities.HistoryActivity;
import com.wismna.geoffroy.donext.adapters.SectionsPagerAdapter;
import com.wismna.geoffroy.donext.adapters.TaskRecyclerViewAdapter;
import com.wismna.geoffroy.donext.dao.Task;
import com.wismna.geoffroy.donext.dao.TaskList;
import com.wismna.geoffroy.donext.database.TaskListDataAccess;

import java.util.List;

/**
 * Fragment that will handle the main display
 */
public class MainFragment extends Fragment implements
        TasksFragment.TaskChangedAdapter,
        TaskListsDialogFragment.TaskListsListener {

    private View mView;
    private ViewPager mViewPager;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private TabLayout tabLayout;

    public MainFragment() {
        // Required empty public constructor
    }

    public ViewPager getViewPager() {
        return mViewPager;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_main, container, false);
        Toolbar toolbar = mView.findViewById(R.id.toolbar);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        assert activity != null;
        activity.setSupportActionBar(toolbar);

        // Get preferences
        SharedPreferences sharedPref =
                PreferenceManager.getDefaultSharedPreferences(activity);

        // Check if this is the first time loading this app
        boolean first_time = sharedPref.getBoolean("first_time", true);
        // If it is, create a default task list
        if (first_time) {
            try (TaskListDataAccess taskListDataAccess = new TaskListDataAccess(activity)) {
                taskListDataAccess.createTaskList(getString(R.string.default_task_list_name), 0);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean("first_time", false);
                editor.apply();
            }
        }

        // Load task lists
        updateTaskLists(activity);

        if (!first_time) {
            // Open last opened tab
            int lastOpenedList = sharedPref.getInt("last_opened_tab", 0);
            mViewPager.setCurrentItem(lastOpenedList);
        }
        return mView;
    }

    @Override
    public void onPause() {
        super.onPause();

        // No tabs exist yet, nothing to save
        if (mViewPager == null) return;
        // Otherwise, save currently opened tab
        SharedPreferences sharedPref =
                PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("last_opened_tab", mViewPager.getCurrentItem());
        editor.apply();
    }

    @Override
    public void onTaskListChanged(Task task, int tabPosition) {
        TaskRecyclerViewAdapter destinationTaskAdapter = getSpecificTabAdapter(tabPosition);
        if (destinationTaskAdapter != null) destinationTaskAdapter.add(task, destinationTaskAdapter.getItemCount());
    }

    @Override
    public void onTaskListsDialogNegativeClick() {
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        assert activity != null;
        updateTaskLists(activity);
    }

    private void updateTaskLists(AppCompatActivity activity) {
        boolean isHistoryActivity = activity instanceof HistoryActivity;
        // Access database to retrieve Tabs
        List<TaskList> taskLists;
        try (TaskListDataAccess taskListDataAccess = new TaskListDataAccess(activity)) {
            taskLists = taskListDataAccess.getTaskLists(isHistoryActivity);

            // Create the adapter that will return a fragment for each of the three
            // primary sections of the activity.
            mSectionsPagerAdapter = new SectionsPagerAdapter(getParentFragmentManager(), taskLists);
            mSectionsPagerAdapter.notifyDataSetChanged();
        }

        // Set up the ViewPager with the sections adapter.
        mViewPager = mView.findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        if (!getResources().getBoolean(R.bool.large_layout)) {

            tabLayout = mView.findViewById(R.id.tabs);
            // Hide the tabs if there is only one task list
            tabLayout.setVisibility(taskLists.size() == 1 && !isHistoryActivity ? View.GONE : View.VISIBLE);
            tabLayout.setupWithViewPager(mViewPager);

            // Handles scroll detection (only available for SDK version >=23)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                toggleTabLayoutArrows(tabLayout.getScrollX());
                //tabLayout.setScrollIndicators(TabLayout.SCROLL_INDICATOR_LEFT | TabLayout.SCROLL_INDICATOR_RIGHT);
                tabLayout.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> toggleTabLayoutArrows(scrollX));
            }
        }
        else {
            // Move guideline to hide task list
            Guideline guideline = mView.findViewById(R.id.center_guideline);
            ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) guideline.getLayoutParams();
            lp.guidePercent = taskLists.size() == 1 && !isHistoryActivity ? 0 : (float) 0.2;
            guideline.setLayoutParams(lp);

            ListView listView = mView.findViewById(R.id.list);
            // Hide the list if there is only one task list
            listView.setAdapter(new ArrayAdapter<>(activity, R.layout.list_tasklist_item, taskLists));
            //listView.setSelection(lastOpenedList);
            listView.setOnItemClickListener((parent, view, position, id) -> mViewPager.setCurrentItem(position));
        }
    }

    private TaskRecyclerViewAdapter getSpecificTabAdapter(int position) {
        TasksFragment taskFragment = (TasksFragment) mSectionsPagerAdapter.getRegisteredFragment(position);
        if (taskFragment == null) return null;
        View view = taskFragment.getView();
        if (view == null) return null;
        RecyclerView recyclerView = view.findViewById(R.id.task_list_view);
        if (recyclerView == null) return null;
        return (TaskRecyclerViewAdapter) recyclerView.getAdapter();
    }

    /** Toggles scrolling arrows visibility */
    private void toggleTabLayoutArrows(int scrollX){
        // Hide left arrow when scrolled to the left
        View leftArrow = mView.findViewById(R.id.left_arrow);
        if (leftArrow != null) {
            if (scrollX <= 1) leftArrow.setVisibility(View.INVISIBLE);
            else leftArrow.setVisibility(View.VISIBLE);
        }
        // Hide right arrow when scrolled to the right
        View rightArrow = mView.findViewById(R.id.right_arrow);
        if (rightArrow != null) {
            Point size = new Point();
            Activity activity = getActivity();
            assert activity != null;
            activity.getWindowManager().getDefaultDisplay().getSize(size);
            if (scrollX == tabLayout.getChildAt(0).getMeasuredWidth() - tabLayout.getMeasuredWidth())
                rightArrow.setVisibility(View.INVISIBLE);
            else rightArrow.setVisibility(View.VISIBLE);
        }
    }
}
