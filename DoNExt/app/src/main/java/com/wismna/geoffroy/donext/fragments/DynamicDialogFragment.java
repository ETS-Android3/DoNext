package com.wismna.geoffroy.donext.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import com.wismna.geoffroy.donext.R;

/**
 * Created by wismna on 2017-03-21.
 * Sub-class this class to create a dynamic fragment that will act as a Dialog in large layouts and
 *  a full screen fragment in smaller layouts.
 */

public abstract class DynamicDialogFragment extends DialogFragment {
    boolean mHasNeutralButton = false;
    int mContentLayoutId = 0;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // This part is only needed on small layouts (large layouts use onCreateDialog)
        if (!getShowsDialog()) {
            View view = inflater.inflate(R.layout.fragment_dynamic_dialog, container, false);
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            activity.setSupportActionBar(setToolbarTitle(view));

            ActionBar actionBar = activity.getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setHomeButtonEnabled(true);
                actionBar.setHomeAsUpIndicator(android.R.drawable.ic_menu_close_clear_cancel);
            }
            setHasOptionsMenu(true);
            insertContentView(view, inflater);
            return view;
        }
        // This basically returns null
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Inflate and set the layout for the dialog
        LayoutInflater inflater = getActivity().getLayoutInflater();
        // As it is a Dialog, the root ViewGroup can be null without issues
        @SuppressLint("InflateParams") final View view = inflater.inflate(R.layout.fragment_dynamic_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        Bundle args = getArguments();
        // Set the dialog buttons
        builder.setView(view)
                // Add action buttons
                .setPositiveButton(args.getString("button_positive"), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        onPositiveButtonClick(view);
                    }
                })
                .setNegativeButton(args.getString("button_negative"), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Send the negative button event back to the host activity
                        // Canceled creation, nothing to do
                        //dialog.cancel();
                        onNegativeButtonClick();
                    }
                });
        if (mHasNeutralButton) {
            builder.setNeutralButton(args.getString("button_neutral"), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    onNeutralButtonClick(view);
                }
            });
        }
        setToolbarTitle(view);
        insertContentView(view, inflater);
        return builder.create();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        //super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        getActivity().getMenuInflater().inflate(R.menu.menu_dynamic_fragment, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        Bundle args = getArguments();
        // Show the neutral button if needed
        if (!mHasNeutralButton) {
            menu.removeItem(R.id.menu_neutral_button);
        }
        else {
            menu.findItem(R.id.menu_neutral_button).setTitle(args.getString("button_neutral"));
        }
        menu.findItem(R.id.menu_positive_button).setTitle(args.getString("button_positive"));
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Determine which menu item was clicked
        int id = item.getItemId();
        View view = getView();

        // Hide the keyboard if present
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }
        if (id == R.id.menu_positive_button) {
            // Handle positive button click here
            onPositiveButtonClick(view);
            return true;
        }
        else if (id == R.id.menu_neutral_button) {
            // Handle neutral button click here
            onNeutralButtonClick(view);
            return true;
        }
        else if (id == android.R.id.home) {
            // Handle negative button click here
            onNegativeButtonClick();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        Dialog dialog = getDialog();
        // Stop the dialog from being dismissed on rotation, due to a bug with the compatibility library
        // https://code.google.com/p/android/issues/detail?id=17423
        if (dialog != null && getRetainInstance()) {
            dialog.setDismissMessage(null);
        }
        super.onDestroyView();
    }

    /** Helper function to get a View, without having to worry about the fact that is a Dialog or not*/
    protected View findViewById(int id) {
        if (getShowsDialog()) return getDialog().findViewById(id);
        return getView().findViewById(id);
    }

    /** Sets the title of the Fragment from the Tag */
    private Toolbar setToolbarTitle(View view) {
        Toolbar toolbar = view.findViewById(R.id.dialog_toolbar);
        toolbar.setTitle(getTag());
        return toolbar;
    }

    /** Inserts the actual contents in the content Frame Layout */
    private void insertContentView(View view, LayoutInflater inflater) {
        // Ensure that the content view is set
        if (mContentLayoutId == 0) return;
        // Insert the content view
        FrameLayout content = view.findViewById(R.id.dynamic_fragment_content);
        content.addView(inflater.inflate(mContentLayoutId, (ViewGroup) view.getParent()));
    }

    protected abstract void onPositiveButtonClick(View view);

    protected abstract void onNeutralButtonClick(View view);

    protected abstract void onNegativeButtonClick();
}
