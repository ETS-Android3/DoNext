package com.wismna.geoffroy.donext.helpers;

import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

/**
 * Created by geoffroy on 15-12-30.
 * Helper class that handles all drags events on a TaskList
 */
public class TaskListTouchHelper extends ItemTouchHelper.SimpleCallback  {

    public interface TaskListTouchHelperAdapter {
        boolean onItemMove(int fromPosition, int toPosition);
    }

    private final TaskListTouchHelperAdapter mAdapter;

    public TaskListTouchHelper(TaskListTouchHelperAdapter adapter) {
        super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
        mAdapter = adapter;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return mAdapter.onItemMove(viewHolder.getAbsoluteAdapterPosition(), target.getAbsoluteAdapterPosition());
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        // No swipe moves
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE)
        {
            viewHolder.itemView.setBackgroundColor(Color.LTGRAY);
        }

        super.onSelectedChanged(viewHolder, actionState);
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);

        viewHolder.itemView.setAlpha(1.0f);
        viewHolder.itemView.setBackgroundColor(0);
    }
}
