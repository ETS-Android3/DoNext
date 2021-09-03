package com.wismna.geoffroy.donext.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.wismna.geoffroy.donext.dao.TaskList;
import com.wismna.geoffroy.donext.fragments.TasksFragment;

import java.util.List;

/**
 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapter extends SmartFragmentStatePagerAdapter {

    private final List<TaskList> taskLists;

    public SectionsPagerAdapter(FragmentManager fm, List<TaskList> taskLists) {
        super(fm);
        this.taskLists = taskLists;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        // getItem is called to instantiate the fragment for the given page.
        // Return a PlaceholderFragment (defined as a static inner class below).
        TaskList taskList = taskLists.get(position);
        return TasksFragment.newTaskListInstance(taskList.getId());
    }

    @Override
    public int getCount() {
        if (taskLists != null) {
            // Show the task lists
            return taskLists.size();
        }
        return 0;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (taskLists == null) return "N/A";
        return taskLists.get(position).getName();
    }

    public List<TaskList> getAllItems(){
        return taskLists;
    }
}