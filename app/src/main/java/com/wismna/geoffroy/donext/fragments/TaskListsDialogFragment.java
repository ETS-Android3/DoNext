package com.wismna.geoffroy.donext.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.wismna.geoffroy.donext.R;
import com.wismna.geoffroy.donext.adapters.TaskListRecyclerViewAdapter;
import com.wismna.geoffroy.donext.dao.TaskList;
import com.wismna.geoffroy.donext.database.TaskListDataAccess;
import com.wismna.geoffroy.donext.helpers.TaskListTouchHelper;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;

/**
 * A fragment representing a list of Items.
 */
public class TaskListsDialogFragment extends DynamicDialogFragment implements
        TaskListRecyclerViewAdapter.TaskListRecyclerViewAdapterListener,
        ConfirmDialogFragment.ConfirmDialogListener {
    private TaskListRecyclerViewAdapter taskListRecyclerViewAdapter;
    private TaskListDataAccess taskListDataAccess;
    private ItemTouchHelper mItemTouchHelper;
    private TaskListsListener mListener;

    /** The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    interface TaskListsListener {
        void onTaskListsDialogNegativeClick();
    }

    public static TaskListsDialogFragment newInstance(TaskListsListener taskListListener) {
        TaskListsDialogFragment fragment = new TaskListsDialogFragment();
        fragment.mListener = taskListListener;
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public TaskListsDialogFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mButtonCount = 1;
        mNegativeButtonString = getString(R.string.task_list_ok);
        mContentLayoutId = R.layout.content_tasklists;

        taskListDataAccess = new TaskListDataAccess(getContext(), TaskListDataAccess.MODE.WRITE);
        new GetTaskListsTask(this).execute(taskListDataAccess);
    }

    @Override
    public void onStart() {
        super.onStart();
        Button createTaskListButton = findViewById(R.id.new_task_list_button);
        createTaskListButton.setOnClickListener(v -> {
            EditText editText = findViewById(R.id.new_task_list_name);
            String text = editText.getText().toString();
            if (text.matches("")) {
                editText.setError(getResources().getString(R.string.task_list_new_list_error));
                return;
            }
            int position = taskListRecyclerViewAdapter.getItemCount();

            TaskList taskList = taskListDataAccess.createTaskList(text, position);
            taskListRecyclerViewAdapter.add(taskList, position);

            editText.setText("");
            toggleVisibleCreateNewTaskListLayout();
        });
    }

    @Override
    protected void onPositiveButtonClick(View view) {
        // Not implemented
    }

    @Override
    protected void onNeutralButtonClick(View view) {
        // Not implemented
    }

    @Override
    protected void onNegativeButtonClick() {
        dismiss();
        // TODO: add an argument to refresh only if something changed
        mListener.onTaskListsDialogNegativeClick();
    }

    @Override
    public void onPause() {
        clearFocus();
        super.onPause();
        taskListDataAccess.close();
    }

    @Override
    public void onResume() {
        super.onResume();
        clearFocus();
        taskListDataAccess.open(TaskListDataAccess.MODE.WRITE);
    }

    private void toggleVisibleCreateNewTaskListLayout() {
        LinearLayout layout = findViewById(R.id.new_task_list_layout);
        int taskListCount = taskListRecyclerViewAdapter.getItemCount();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        String maxTaskListsString = sharedPref.getString("pref_conf_max_lists", "5");
        int maxTaskLists = Integer.parseInt(maxTaskListsString);
        if (taskListCount >= maxTaskLists) layout.setVisibility(View.GONE);
        else layout.setVisibility(View.VISIBLE);
        clearFocus();
    }

    @Override
    public void onEditTextLoseFocus(TaskList taskList) {
        taskListDataAccess.updateName(taskList.getId(), taskList.getName());
    }

    @Override
    public void onClickDeleteButton(int position, long id) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());

        if(sharedPref.getBoolean("pref_conf_tasklist_del", true)) {
            String title = getResources().getString(R.string.task_list_confirmation_delete);
            ConfirmDialogFragment confirmDialogFragment =
                    ConfirmDialogFragment.newInstance(this);
            Bundle args = new Bundle();
            args.putString("message", title);
            args.putInt("button", R.string.task_confirmation_delete_button);
            args.putInt("ItemPosition", position);
            args.putLong("ItemId", id);
            confirmDialogFragment.setArguments(args);
            confirmDialogFragment.show(getParentFragmentManager(), title);
        }
        else deleteTaskList(position, id);
    }

    @Override
    public void onConfirmDialogClick(DialogFragment dialog, ConfirmDialogFragment.ButtonEvent event) {
        // Handle never ask again checkbox
        CheckBox neverAskAgainCheckBox = dialog.getDialog().findViewById(R.id.task_confirmation_never);
        if (neverAskAgainCheckBox.isChecked()) {

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
            SharedPreferences.Editor editor = sharedPref.edit();

            editor.putBoolean("pref_conf_tasklist_del", false);
            editor.apply();
        }

        if (event == ConfirmDialogFragment.ButtonEvent.NO) return;

        Bundle args = dialog.getArguments();
        assert args != null;
        deleteTaskList(args.getInt("ItemPosition"), args.getLong("ItemId"));
    }

    @Override
    public void onItemMove(long fromTaskId, long toTaskId, int fromPosition, int toPosition) {
        taskListDataAccess.updateOrder(fromTaskId, toPosition);
        taskListDataAccess.updateOrder(toTaskId, fromPosition);
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        mItemTouchHelper.startDrag(viewHolder);
    }

    private void deleteTaskList(int position, long id)
    {
        taskListRecyclerViewAdapter.remove(position);
        taskListDataAccess.deleteTaskList(id);
        toggleVisibleCreateNewTaskListLayout();
    }

    private static class GetTaskListsTask extends AsyncTask<TaskListDataAccess, Void, List<TaskList>> {
        private final WeakReference<TaskListsDialogFragment> fragmentReference;

        GetTaskListsTask(TaskListsDialogFragment context) {
            fragmentReference = new WeakReference<>(context);
        }

        @Override
        protected List<TaskList> doInBackground(TaskListDataAccess... params) {
            TaskListDataAccess taskListDataAccess = params[0];
            return taskListDataAccess.getTaskLists(false);
        }

        @Override
        protected void onPostExecute(List<TaskList> taskLists) {
            super.onPostExecute(taskLists);
            TaskListsDialogFragment fragment = fragmentReference.get();
            if (fragment == null) return;
            fragment.taskListRecyclerViewAdapter =
                    new TaskListRecyclerViewAdapter(taskLists, fragment);

            // Set the adapter
            Context context = fragment.getContext();
            RecyclerView recyclerView = fragment.findViewById(R.id.task_lists_view);
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            recyclerView.setAdapter(fragment.taskListRecyclerViewAdapter);

            // Set the Touch Helper
            ItemTouchHelper.Callback callback = new TaskListTouchHelper(fragment.taskListRecyclerViewAdapter);
            fragment.mItemTouchHelper = new ItemTouchHelper(callback);
            fragment.mItemTouchHelper.attachToRecyclerView(recyclerView);

            fragment.toggleVisibleCreateNewTaskListLayout();
        }
    }
}
