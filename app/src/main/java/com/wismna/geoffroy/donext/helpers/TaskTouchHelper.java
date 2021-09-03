package com.wismna.geoffroy.donext.helpers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.View;

import com.wismna.geoffroy.donext.R;

/**
 * Created by geoffroy on 15-12-04.
 * Helper class that handles all swipe events on a Task
 */
public class TaskTouchHelper extends ItemTouchHelper.SimpleCallback {
    public interface TaskTouchHelperAdapter {
        void onItemSwiped(int position, int direction);
    }

    private final TaskTouchHelperAdapter mAdapter;
    private final int colorRight;
    private final int colorLeft;

    public TaskTouchHelper(TaskTouchHelperAdapter adapter, int colorRight, int colorLeft){
        // No drag moves, no swipes (except for 1st element, see getSwipeDirs method)
        super(0, 0);
        this.colorRight = colorRight;
        this.colorLeft = colorLeft;
        this.mAdapter = adapter;
    }

    @Override
    public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        // Allow both directions swiping on first item, only left on the others
        if (viewHolder.getAbsoluteAdapterPosition() == 0)
            return ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
        else return super.getSwipeDirs(recyclerView, viewHolder);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        mAdapter.onItemSwiped(viewHolder.getAbsoluteAdapterPosition(), direction);
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                            float dX, float dY, int actionState, boolean isCurrentlyActive) {
        // Get RecyclerView item from the ViewHolder
        View itemView = viewHolder.itemView;

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {

            if (dX > 0) {
                Rect rect = new Rect(itemView.getLeft(), itemView.getTop(), (int) dX,
                        itemView.getBottom());
                setBackground(c, itemView, rect, dX, dY,
                        colorLeft, R.string.task_confirmation_next_button);
            } else {
                // Draw Rect with varying left side, equal to the item's right side plus negative displacement dX
                Rect rect = new Rect(itemView.getRight() + (int)dX, itemView.getTop(),
                        itemView.getRight(), itemView.getBottom());
                setBackground(c, itemView, rect, dX, dY,
                        colorRight, R.string.task_confirmation_done_button);
            }
        }
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    private void setBackground(Canvas c, View itemView,
            Rect rect, float dX, float dY, int color, int textId) {

        TextPaint textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(25 * itemView.getResources().getDisplayMetrics().density);
        textPaint.setColor(Color.WHITE);

        int heightOffset = itemView.getHeight() / 2 - (int)textPaint.getTextSize() / 2;
        float widthOffset = 30f;

        Paint background = new Paint();
        // Set your color for negative displacement
        background.setColor(color);
        // Draw Rect with varying left side, equal to the item's right side plus negative displacement dX
        c.drawRect(rect, background);

        // Draw text in the rectangle
        String text = itemView.getResources().getString(textId).toUpperCase();
        float width = textPaint.measureText(text);
        float textXCoordinate;
        if (dX > 0) textXCoordinate = rect.left + widthOffset;
        else textXCoordinate = rect.right - width - widthOffset;
        c.translate(textXCoordinate, dY + heightOffset);
        StaticLayout staticLayout = new StaticLayout(text, textPaint, (int)width, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0, false);
        staticLayout.draw(c);
    }
}
