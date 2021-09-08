package com.wismna.geoffroy.donext.adapters;

import android.graphics.Color;
import android.graphics.Typeface;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.wismna.geoffroy.donext.R;
import com.wismna.geoffroy.donext.dao.Task;

import org.joda.time.LocalDate;

import java.util.List;


/**
 * {@link RecyclerView.Adapter} that can display a {@link Task}.
 */
public class TaskRecyclerViewAdapter extends RecyclerView.Adapter<TaskRecyclerViewAdapter.StandardViewHolder> {

    private List<Task> mValues;
    private boolean mIsToday;
    private boolean mIsHistory;

    public TaskRecyclerViewAdapter(List<Task> items, boolean isToday, boolean isHistory) {
        mValues = items;
        mIsToday = isToday;
        mIsHistory = isHistory;
    }

    @NonNull
    @Override
    public StandardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(viewType, parent, false);
        return new StandardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final StandardViewHolder holder, int position) {
        // Set basic information
        holder.mItem = mValues.get(position);
        holder.mIdView.setText(String.valueOf(holder.mItem.getId()));
        holder.mCycleView.setText(String.valueOf(holder.mItem.getCycle()));
        holder.mTitleView.setText(holder.mItem.getName());
        holder.mDescriptionView.setText(holder.mItem.getDescription());
        // Set task rendering
        if (position > 0) {
            holder.mTitleView.setTypeface(Typeface.DEFAULT);
            holder.mTitleView.setTextColor(Color.BLACK);
        }

        // Set priority
        switch (holder.mItem.getPriority())
        {
            case 0:
                holder.mIconView.setImageResource(R.drawable.ic_low_priority_lightgray_24dp);
                break;
            case 2:
                holder.mIconView.setImageResource(R.drawable.ic_priority_high_red_24dp);
                break;
            default:
                holder.mIconView.setImageDrawable(null);
                break;
        }

        // Additional information will not be displayed in Today view
        if (mIsToday) return;
        // Set alarm if past due date
        LocalDate dueDate = holder.mItem.getDueDate();
        if (holder.mItem.getDone() == 1)
            holder.mIconView.setImageResource(R.drawable.ic_check_ligth);
        else if (holder.mItem.getDeleted() == 1)
            holder.mIconView.setImageResource(R.drawable.ic_close_light);
        else if(dueDate != null && dueDate.isBefore(LocalDate.now()))
            holder.mIconView.setImageResource(R.drawable.ic_access_alarm);

    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getId();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 && !mIsHistory) return R.layout.fragment_task_first;
        return R.layout.fragment_task_detailed;
    }

    public void add(Task item, int position) {
        mValues.add(position, item);
        notifyItemInserted(position);
    }

    public void remove(int position) {
        mValues.remove(position);
        notifyItemRemoved(position);
    }

    public void update(Task item, int position) {
        mValues.set(position, item);
        notifyItemChanged(position);
    }

    public int getCycleCount() {
        int count = 0;
        for (Task task: mValues) {
            count += task.getCycle();
        }
        return count;
    }

    public void setItems(List<Task> tasks) {
        this.mValues = tasks;
        notifyItemRangeInserted(0, tasks.size() - 1);
    }

    public Task getItem(int position) {
        return mValues.get(position);
    }

    static class StandardViewHolder extends RecyclerView.ViewHolder {
        final View mView;
        final TextView mIdView;
        final ImageView mIconView;
        final TextView mCycleView;
        final TextView mTitleView;
        final TextView mDescriptionView;
        Task mItem;

        StandardViewHolder(View view) {
            super(view);
            mView = view;

            mIdView = view.findViewById(R.id.task_id);
            mIconView = view.findViewById(R.id.task_icon);
            mCycleView = view.findViewById(R.id.task_cycle);
            mTitleView = view.findViewById(R.id.task_name);
            mDescriptionView = view.findViewById(R.id.task_description);
        }

        @NonNull
        @Override
        public String toString() {
            return super.toString() + " '" + mTitleView.getText() + "'";
        }
    }
}
