package com.wismna.geoffroy.donext.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.wismna.geoffroy.donext.R;
import com.wismna.geoffroy.donext.activities.HistoryActivity;
import com.wismna.geoffroy.donext.activities.TodayActivity;
import com.wismna.geoffroy.donext.adapters.SectionsPagerAdapter;
import com.wismna.geoffroy.donext.adapters.TaskRecyclerViewAdapter;
import com.wismna.geoffroy.donext.dao.Task;
import com.wismna.geoffroy.donext.dao.TaskList;
import com.wismna.geoffroy.donext.database.TaskDataAccess;
import com.wismna.geoffroy.donext.database.TaskListDataAccess;
import com.wismna.geoffroy.donext.helpers.TaskTouchHelper;
import com.wismna.geoffroy.donext.listeners.RecyclerItemClickListener;

import org.joda.time.LocalDate;

import java.util.List;
import java.util.Objects;

/**
 * A fragment representing a list of Items.
 */
public class TasksFragment extends Fragment implements
        TaskFormDialogFragment.NewTaskListener,
        ConfirmDialogFragment.ConfirmDialogListener,
        TaskTouchHelper.TaskTouchHelperAdapter{

    public interface TaskChangedAdapter {
        void onTaskListChanged(Task task, int tabPosition);
    }

    private static final String TASK_LIST_ID = "task_list_id";
    private long taskListId = -1;
    private boolean isTodayView = false;
    private boolean isHistory = false;
    private TaskRecyclerViewAdapter taskRecyclerViewAdapter;
    private View view;
    private RecyclerView recyclerView;
    private TaskChangedAdapter mAdapter;
    private Snackbar snackbar;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public TasksFragment() {
    }

    public static TasksFragment newTaskListInstance(long taskListId) {
        TasksFragment fragment = new TasksFragment();
        Bundle args = new Bundle();
        args.putLong(TASK_LIST_ID, taskListId);
        fragment.setArguments(args);
        fragment.setRetainInstance(true);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            taskListId = getArguments().getLong(TASK_LIST_ID);
        }
        Activity parentActivity = getActivity();
        if (parentActivity instanceof HistoryActivity) isHistory = true;
        if (parentActivity instanceof TodayActivity) isTodayView = true;
        mAdapter = (MainFragment)getParentFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_tasks, container, false);
        final Context context = view.getContext();

        // Set the Recycler view
        recyclerView = view.findViewById(R.id.task_list_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        // Get all tasks
        try (TaskDataAccess taskDataAccess = new TaskDataAccess(view.getContext())) {
            taskRecyclerViewAdapter = new TaskRecyclerViewAdapter(
                    isTodayView? taskDataAccess.getTodayTasks() : taskDataAccess.getAllTasksFromList(taskListId, isHistory), isTodayView, isHistory);
        }
        recyclerView.setAdapter(taskRecyclerViewAdapter);

        if (!isHistory) {
            // Set ItemTouch helper in RecyclerView to handle swipe move on elements
            ItemTouchHelper.Callback callback = new TaskTouchHelper(this,
                    ContextCompat.getColor(context, R.color.colorPrimary),
                    ContextCompat.getColor(context, R.color.colorAccent));

            ItemTouchHelper helper = new ItemTouchHelper(callback);
            helper.attachToRecyclerView(recyclerView);
        }

        final Resources resources = getResources();
        // Implements touch listener to add click detection
        recyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(context, (view, position) -> {
                    boolean isLargeLayout = resources.getBoolean(R.bool.large_layout);
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
                    Bundle args = new Bundle();
                    args.putBoolean("today", sharedPref.getBoolean("pref_conf_today_enable", false));
                    args.putInt("button_count", isHistory ? 1 : 3);
                    args.putString("button_positive", getString(R.string.new_task_save));
                    args.putString("button_negative",
                            isHistory ? getString(R.string.task_list_ok) : getString(R.string.new_task_cancel));
                    args.putString("button_neutral", getString(R.string.new_task_delete));
                    args.putInt("position", position);

                    // Set current tab value to new task dialog
                    ViewPager viewPager = requireActivity().findViewById(R.id.container);
                    List<TaskList> taskLists;
                    Task task = taskRecyclerViewAdapter.getItem(position);
                    if (viewPager != null) {
                        taskLists = ((SectionsPagerAdapter) Objects.requireNonNull(viewPager.getAdapter())).getAllItems();
                        args.putInt("list", viewPager.getCurrentItem());
                    }
                    else {
                        try (TaskListDataAccess taskListDataAccess = new TaskListDataAccess(getActivity())) {
                            taskLists = taskListDataAccess.getTaskLists(isHistory);
                        }
                        for (TaskList taskList :
                                taskLists) {
                            if (taskList.getId() == task.getTaskListId()) {
                                args.putInt("list", taskLists.indexOf(taskList));
                                break;
                            }
                        }
                    }

                    FragmentManager manager = getParentFragmentManager();
                    TaskFormDialogFragment taskDialogFragment = TaskFormDialogFragment.newInstance(
                            task, taskLists, TasksFragment.this);
                    taskDialogFragment.setArguments(args);

                    // Open the fragment as a dialog or as full-screen depending on screen size
                    taskDialogFragment.showFragment(manager,
                            getString(isHistory ? R.string.action_view_task : R.string.action_edit_task), isLargeLayout);
                })
        );

        // Handle updating total counts in a listener to be sure that the layout is available
        recyclerView.getViewTreeObserver().addOnPreDrawListener(() -> {
            // isAdded is tested to prevent an IllegalStateException when fast switching between tabs
            if (!isAdded()) return true;
            Resources resources1 = getResources();

            // Update total cycle count
            int totalCycles = taskRecyclerViewAdapter.getCycleCount();
            TextView totalCyclesView = view.findViewById(R.id.total_task_cycles);
            if (totalCycles != 0)
                totalCyclesView.setText(resources1.getQuantityString(R.plurals.task_total_cycles, totalCycles, totalCycles));
            else totalCyclesView.setText("");

            // Update total tasks
            int totalTasks = taskRecyclerViewAdapter.getItemCount();
            View separator = view.findViewById(R.id.main_additional_info_separator);
            TextView totalTasksView = view.findViewById(R.id.total_task_count);
            View noMoreTasks = view.findViewById(R.id.no_more_tasks);
            View createTasks = view.findViewById(R.id.create_tasks);
            if (totalTasks == 0) {
                noMoreTasks.setVisibility(View.VISIBLE);
                createTasks.setVisibility(View.VISIBLE);
                totalTasksView.setVisibility(View.GONE);
                separator.setVisibility(View.GONE);
            }
            else {
                noMoreTasks.setVisibility(View.GONE);
                createTasks.setVisibility(View.GONE);
                totalTasksView.setVisibility(View.VISIBLE);
                separator.setVisibility(View.VISIBLE);
                totalTasksView.setText(resources1.getQuantityString(R.plurals.task_total, totalTasks, totalTasks));
            }

            return true;
        });

        //recyclerView.addItemDecoration(new DividerItemDecoration(getActivity()));
        return view;
    }

    @Override
    public void onPause() {
        if (snackbar != null) snackbar.dismiss();
        super.onPause();
    }

    @Override
    public void onConfirmDialogClick(DialogFragment dialog, ConfirmDialogFragment.ButtonEvent event) {
        Bundle args = dialog.getArguments();
        assert args != null;
        int itemPosition = args.getInt("ItemPosition");
        int direction = args.getInt("Direction");

        // Handle never ask again checkbox
        CheckBox neverAskAgainCheckBox = Objects.requireNonNull(dialog.getDialog()).findViewById(R.id.task_confirmation_never);
        if (neverAskAgainCheckBox.isChecked()) {

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
            SharedPreferences.Editor editor = sharedPref.edit();

            // Set system settings
            switch (direction) {
                case ItemTouchHelper.LEFT:
                    editor.putBoolean("pref_conf_done", false);
                    break;
                case ItemTouchHelper.RIGHT:
                    editor.putBoolean("pref_conf_next", false);
                    break;
                case -1:
                    editor.putBoolean("pref_conf_del", false);
                    break;
            }
            editor.apply();
        }
        if (event == ConfirmDialogFragment.ButtonEvent.YES) {
            PerformTaskAction(itemPosition, direction);
        }
        else if(event == ConfirmDialogFragment.ButtonEvent.NO) {
            taskRecyclerViewAdapter.notifyItemChanged(itemPosition);
        }
    }

    @Override
    public void onNewTaskDialogPositiveClick(DialogFragment dialog, View dialogView) {
        // Get the dialog fragment
        if (dialogView == null) return;
        long id = 0;
        Task task = ((TaskFormDialogFragment)dialog).getTask();
        if (task != null) id = task.getId();

        // Get the controls
        Spinner listSpinner = dialogView.findViewById(R.id.new_task_list);
        EditText nameText = dialogView.findViewById(R.id.new_task_name);
        EditText descText = dialogView.findViewById(R.id.new_task_description);
        SeekBar seekBar = dialogView.findViewById(R.id.new_task_priority);
        CheckBox setDueDate = dialogView.findViewById(R.id.new_task_due_date_set);
        DatePicker dueDatePicker = dialogView.findViewById(R.id.new_task_due_date);
        TaskList taskList = (TaskList) listSpinner.getSelectedItem();
        CheckBox todayList = dialogView.findViewById(R.id.new_task_today);
        boolean isToday = todayList.isChecked();
        // Add the task to the database
        try (TaskDataAccess taskDataAccess = new TaskDataAccess(view.getContext(), TaskDataAccess.MODE.WRITE)) {
            Task newTask = taskDataAccess.createOrUpdateTask(id,
                    nameText.getText().toString(),
                    descText.getText().toString(),
                    seekBar.getProgress(),
                    taskList.getId(),
                    setDueDate.isChecked() ?
                        new LocalDate(dueDatePicker.getYear(),
                                dueDatePicker.getMonth() + 1,
                                dueDatePicker.getDayOfMonth()).toString()
                        : "",
                    isToday);

            Bundle args = dialog.getArguments();
            // Should never happen because we will have to be on this tab to open the dialog
            if (taskRecyclerViewAdapter == null) return;

            int position = 0;
            // Add the task
            if (task == null) {
                // If the new task is added to another task list, update the tab
                if (mAdapter != null && taskListId != taskList.getId()) {
                    mAdapter.onTaskListChanged(newTask, listSpinner.getSelectedItemPosition());
                }
                // Otherwise add it to the current one
                else {
                    position = taskRecyclerViewAdapter.getItemCount();
                    taskRecyclerViewAdapter.add(newTask, position);
                    recyclerView.scrollToPosition(position);
                }
            }
            // Update the task
            else {
                position = args != null ? args.getInt("position") : 0;
                // Check if task list was changed
                if ((isTodayView && !isToday) || (!isTodayView && task.getTaskListId() != taskList.getId()))
                {
                    // Remove item from current tab
                    taskRecyclerViewAdapter.remove(position);

                    // Add it to the corresponding tab provided it is already instantiated
                    if (mAdapter != null) mAdapter.onTaskListChanged(newTask, listSpinner.getSelectedItemPosition());
                } else {
                    taskRecyclerViewAdapter.update(newTask, position);
                }
            }
            taskRecyclerViewAdapter.notifyItemChanged(position);
        }
    }

    @Override
    public void onNewTaskDialogNeutralClick(DialogFragment dialog) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean showDialog = sharedPref.getBoolean("pref_conf_del", true);
        Bundle args = dialog.getArguments();

        // Delete task from Adapter
        assert args != null;
        final int itemPosition = args.getInt("position");

        if (showDialog) {
            String title = getResources().getString(R.string.task_confirmation_delete_text);
            ConfirmDialogFragment confirmDialogFragment =
                    ConfirmDialogFragment.newInstance(this);
            Bundle confirmArgs = new Bundle();
            confirmArgs.putString("message", title);
            confirmArgs.putInt("button", R.string.task_confirmation_delete_button);
            confirmArgs.putInt("ItemPosition", itemPosition);
            confirmArgs.putInt("Direction", -1);
            confirmDialogFragment.setArguments(confirmArgs);
            FragmentManager fragmentManager = getParentFragmentManager();
            confirmDialogFragment.show(fragmentManager, title);
        }
        else {
            PerformTaskAction(itemPosition, -1);
        }
    }

    @Override
    public void onItemSwiped(int itemPosition, int direction) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        String title = "";
        boolean showDialog = false;
        int buttonLabel = -1;

        switch (direction)
        {
            // Mark item as Done
            case ItemTouchHelper.LEFT:
                title = getResources().getString(R.string.task_confirmation_done_text);
                showDialog = sharedPref.getBoolean("pref_conf_done", true);
                buttonLabel = R.string.task_confirmation_done_button;
                break;
            // Increase task cycle count
            case ItemTouchHelper.RIGHT:
                title = getResources().getString(R.string.task_confirmation_next_text);
                showDialog = sharedPref.getBoolean("pref_conf_next", true);
                buttonLabel = R.string.task_confirmation_next_button;
                break;
        }
        if (showDialog) {
            ConfirmDialogFragment confirmDialogFragment =
                    ConfirmDialogFragment.newInstance(this);
            Bundle args = new Bundle();
            args.putString("message", title);
            args.putInt("button", buttonLabel);
            args.putInt("ItemPosition", itemPosition);
            args.putInt("Direction", direction);
            confirmDialogFragment.setArguments(args);
            FragmentManager fragmentManager = getParentFragmentManager();
            confirmDialogFragment.show(fragmentManager, title);
        }
        else PerformTaskAction(itemPosition, direction);
    }

    /** Performs an action on a task: done, next or delete */
    private void PerformTaskAction(final int itemPosition, final int direction) {
        final long itemId = taskRecyclerViewAdapter.getItemId(itemPosition);
        final Task task = taskRecyclerViewAdapter.getItem(itemPosition);
        String action = "";
        Resources resources = getResources();

        taskRecyclerViewAdapter.remove(itemPosition);

        switch (direction)
        {
            // Mark item as Done
            case ItemTouchHelper.LEFT:
                action = resources.getString(R.string.snackabar_action_done);
                break;
            // Increase task cycle count
            case ItemTouchHelper.RIGHT:
                action = resources.getString(R.string.snackabar_action_next);
                task.setCycle(task.getCycle() + 1);
                taskRecyclerViewAdapter.add(task, taskRecyclerViewAdapter.getItemCount());
                break;
            case -1:
                FragmentManager manager = getParentFragmentManager();
                DialogFragment dialog = (DialogFragment) Objects.requireNonNull(manager).findFragmentByTag(getString(R.string.action_edit_task));
                if (dialog != null) dialog.dismiss();
                action = resources.getString(R.string.snackabar_action_deleted);
                break;
        }

        // Setup the snack bar
        View parentView = requireActivity().findViewById(R.id.main_content);
        snackbar = Snackbar.make(parentView, resources.getString(R.string.snackabar_label, action), Snackbar.LENGTH_LONG)
                .setAction(resources.getString(R.string.snackabar_button), v -> {
                    // Undo adapter changes
                    switch (direction) {
                        // Nothing special to do for done
                        case ItemTouchHelper.LEFT:
                            break;
                        // Remove the last item
                        case ItemTouchHelper.RIGHT:
                            taskRecyclerViewAdapter.remove(taskRecyclerViewAdapter.getItemCount() - 1);
                            task.setCycle(task.getCycle() - 1);
                            break;
                        // Nothing special to do for delete
                        case -1:
                            break;
                    }
                    // Reset the first item
                    taskRecyclerViewAdapter.add(task, itemPosition);
                    recyclerView.scrollToPosition(0);
                });
        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                super.onDismissed(snackbar, event);
                // When clicked on undo, do not write to DB
                if (event == DISMISS_EVENT_ACTION) return;

                // Commit the changes to DB
                try (TaskDataAccess taskDataAccess = new TaskDataAccess(view.getContext(), TaskDataAccess.MODE.WRITE)) {
                    switch (direction) {
                        // Mark item as Done
                        case ItemTouchHelper.LEFT:
                            taskDataAccess.setDone(itemId, isTodayView);
                            break;
                        // Increase task cycle count
                        case ItemTouchHelper.RIGHT:
                            taskDataAccess.increaseCycle(task, isTodayView);
                            taskRecyclerViewAdapter.notifyItemChanged(taskRecyclerViewAdapter.getItemCount() - 1);
                            break;
                        case -1:
                            // Delete the task
                            taskDataAccess.deleteTask(itemId, isTodayView);
                    }
                }
            }
        }).show();
    }
}
