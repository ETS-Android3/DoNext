package com.wismna.geoffroy.donext.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import com.wismna.geoffroy.donext.R;

import java.util.Objects;

/**
 * Created by wismna on 2017-03-21.
 * Sub-class this class to create a dynamic fragment that will act as a Dialog in large layouts and
 *  a full screen fragment in smaller layouts.
 */

public abstract class DynamicDialogFragment extends DialogFragment {
    int mButtonCount = 2;
    String mPositiveButtonString = "";
    String mNeutralButtonString = "";
    String mNegativeButtonString = "";
    int mContentLayoutId = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // This part is only needed on small layouts (large layouts use onCreateDialog)
        if (!getShowsDialog()) {
            View view = inflater.inflate(R.layout.fragment_dynamic_dialog, container, false);
            AppCompatActivity activity = (AppCompatActivity) requireActivity();
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
        Activity activity = requireActivity();
        LayoutInflater inflater = activity.getLayoutInflater();
        // As it is a Dialog, the root ViewGroup can be null without issues
        final View view = inflater.inflate(R.layout.fragment_dynamic_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        // Add action buttons
        builder.setView(view)
                .setNegativeButton(mNegativeButtonString, (dialog, id) -> {
                    // Send the negative button event back to the host activity
                    // Canceled creation, nothing to do
                    onNegativeButtonClick();
                });
        if (mButtonCount >= 2) {
            builder.setPositiveButton(mPositiveButtonString, (dialog, id) -> onPositiveButtonClick(view));
        }
        if (mButtonCount == 3) {
            builder.setNeutralButton(mNeutralButtonString, (dialog, which) -> onNeutralButtonClick(view));
        }
        setToolbarTitle(view);
        insertContentView(view, inflater);
        return builder.create();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, @NonNull MenuInflater inflater) {
        menu.clear();
        requireActivity().getMenuInflater().inflate(R.menu.menu_dynamic_fragment, menu);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        /*switch (mButtonCount) {
            case 1:
                menu.removeItem(R.id.menu_positive_button);
                menu.removeItem(R.id.menu_neutral_button);
                break;
            case 2:
                menu.removeItem(R.id.menu_neutral_button);
                menu.findItem(R.id.menu_positive_button).setTitle(args.getString("button_positive"));
                break;
            case 3:
                menu.findItem(R.id.menu_neutral_button).setTitle(args.getString("button_neutral"));
                menu.findItem(R.id.menu_positive_button).setTitle(args.getString("button_positive"));
                break;
        }*/

        // Hide buttons depending on count
        switch (mButtonCount) {
            case 1: menu.removeItem(R.id.menu_positive_button);
            case 2: menu.removeItem(R.id.menu_neutral_button);
        }

        // Set titles on existing buttons
        switch (mButtonCount) {
            case 3: menu.findItem(R.id.menu_neutral_button).setTitle(mNeutralButtonString);
            case 2: menu.findItem(R.id.menu_positive_button).setTitle(mPositiveButtonString);
        }
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Determine which menu item was clicked
        int id = item.getItemId();
        View view = getView();

        // Hide the keyboard if present
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
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

    public void showFragment(FragmentManager fragmentManager, String title, boolean isLargeLayout)
    {
        if (isLargeLayout)
            show(fragmentManager, title);
        else {
            // The device is smaller, so show the fragment fullscreen
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            // For a little polish, specify a transition animation
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            // To make it fullscreen, use the 'content' root view as the container
            // for the fragment, which is always the root view for the activity
            transaction.add(android.R.id.content, this, title)
                    .addToBackStack(null).commit();
        }
    }

    /** Helper function to get a View, without having to worry about the fact that is a Dialog or not*/
    protected <T extends View> T findViewById(int id) {
        if (getShowsDialog()) return requireDialog().findViewById(id);
        return requireView().findViewById(id);
    }


    /** Helper method to clear focus by giving it to the parent layout */
    protected void clearFocus() {
        View view = getView();
        if (view != null) {
            view.requestFocus();

            // Hide keyboard
            InputMethodManager inputMethodManager = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            Objects.requireNonNull(inputMethodManager).hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
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
