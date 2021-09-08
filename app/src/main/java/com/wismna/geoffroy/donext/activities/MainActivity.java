package com.wismna.geoffroy.donext.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.widget.ViewPager2;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.Crashes;
import com.wismna.geoffroy.donext.R;
import com.wismna.geoffroy.donext.adapters.SectionsPagerAdapter;
import com.wismna.geoffroy.donext.fragments.AboutDialogFragment;
import com.wismna.geoffroy.donext.fragments.MainFragment;
import com.wismna.geoffroy.donext.fragments.TaskFormDialogFragment;
import com.wismna.geoffroy.donext.fragments.TaskListsDialogFragment;
import com.wismna.geoffroy.donext.fragments.TasksFragment;

/**
 * Main Activity class
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // App center integration
        AppCenter.start(getApplication(), "a5aeb8b9-0730-419f-b30b-f23b972f82f3",
                Analytics.class, Crashes.class);

        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        // Handles today list
        MenuItem todayListItem = menu.findItem(R.id.action_todayList);
        if (todayListItem == null) return false;
        todayListItem.setVisible(sharedPref.getBoolean("pref_conf_today_enable", false));

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_changeLayout) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = sharedPref.edit();
            String layoutTypeString = sharedPref.getString("pref_conf_task_layout", "1");
            int layoutType = Integer.parseInt(layoutTypeString);
            editor.putString("pref_conf_task_layout", String.valueOf(layoutType % 2 + 1));
            editor.apply();

            // Update the ViewPagerAdapter to refresh all tabs
            /*ViewPager2 viewPager = getMainFragmentViewPager();
            if (viewPager != null)
            {
                viewPager.getAdapter().notifyDataSetChanged();
            }*/
            return true;
        }
        return false;
    }

    /** Called when the user clicks on the Today List button */
    public void showTodayList(MenuItem item) {
        Intent intent = new Intent(this, TodayActivity.class);
        startActivity(intent);
    }

    /** Called when the user clicks the Edit Lists button  */
    public void openTaskLists(MenuItem menuItem) {
        FragmentManager fragmentManager = getSupportFragmentManager();

        // Create the fragment
        TaskListsDialogFragment taskListFragment = TaskListsDialogFragment.newInstance(
                (MainFragment)fragmentManager.findFragmentById(R.id.fragment_main));

        taskListFragment.showFragment(fragmentManager,
                getString(R.string.task_list_edit), getResources().getBoolean(R.bool.large_layout));
    }

    /** Called when the user clicks the History button*/
    public void openHistory(MenuItem item) {
        Intent intent = new Intent(this, HistoryActivity.class);
        startActivity(intent);
    }

    /** Called when the user clicks the Settings button  */
    public void openSettings(MenuItem menuItem) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    /** Called when the user clicks the About button  */
    public void openAbout(MenuItem menuItem) {
        FragmentManager fragmentManager = getSupportFragmentManager();

        // Create the fragment
        AboutDialogFragment taskListFragment = new AboutDialogFragment();
        taskListFragment.showFragment(fragmentManager,
                getString(R.string.action_about), getResources().getBoolean(R.bool.large_layout));
    }

    /** Called when user clicks on the New Task floating button */
    public void onNewTaskClick(View view) {
        ViewPager2 viewPager = getMainFragmentViewPager();
        if (viewPager == null) return;
        int currentTabPosition = viewPager.getCurrentItem();
        SectionsPagerAdapter pagerAdapter = (SectionsPagerAdapter) viewPager.getAdapter();
        assert pagerAdapter != null;
        TaskFormDialogFragment taskDialogFragment = TaskFormDialogFragment.newInstance(null,
                pagerAdapter.getAllItems(),
                (TasksFragment) pagerAdapter.createFragment(currentTabPosition));

        // Set some configuration values for the tab
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        Bundle args = new Bundle();
        args.putInt("list", currentTabPosition);
        args.putBoolean("today", sharedPref.getBoolean("pref_conf_today_enable", false));
        args.putInt("button_count", 2);
        args.putString("button_positive", getString(R.string.new_task_save));
        args.putString("button_negative", getString(R.string.new_task_cancel));
        args.putString("button_neutral", getString(R.string.new_task_delete));
        taskDialogFragment.setArguments(args);

        FragmentManager fragmentManager = getSupportFragmentManager();

        taskDialogFragment.showFragment(fragmentManager,
                getString(R.string.action_new_task), getResources().getBoolean(R.bool.large_layout));
    }

    private ViewPager2 getMainFragmentViewPager(){
        FragmentManager manager = getSupportFragmentManager();
        MainFragment fragment = (MainFragment)manager.findFragmentById(R.id.fragment_main);
        return fragment != null ? fragment.getViewPager() : null;
    }
}
