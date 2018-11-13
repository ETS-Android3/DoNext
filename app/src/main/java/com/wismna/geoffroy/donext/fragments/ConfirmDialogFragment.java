package com.wismna.geoffroy.donext.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;

import com.wismna.geoffroy.donext.R;

import java.util.Objects;

public class ConfirmDialogFragment extends DialogFragment {
    interface ConfirmDialogListener {
        void onConfirmDialogClick(DialogFragment dialog, ButtonEvent event);
    }

    enum ButtonEvent{
        YES,
        NO
    }
    private ConfirmDialogListener confirmDialogListener;

    public static ConfirmDialogFragment newInstance(ConfirmDialogListener confirmDialogListener) {
        ConfirmDialogFragment fragment = new ConfirmDialogFragment();
        fragment.confirmDialogListener = confirmDialogListener;
        fragment.setRetainInstance(true);
        return fragment;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);

        // Allows refreshing the first item of the adapter
        confirmDialogListener.onConfirmDialogClick(this, ButtonEvent.NO);
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(Objects.requireNonNull(getActivity()));

        Bundle args = getArguments();
        LayoutInflater inflater = getActivity().getLayoutInflater();
        // No need for a parent in a Dialog Fragment
        View view = inflater.inflate(R.layout.fragment_task_confirmation, null);
        assert args != null;
        builder.setView(view).setMessage(args.getString("message"))
            .setPositiveButton(args.getInt("button"), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    confirmDialogListener.onConfirmDialogClick(ConfirmDialogFragment.this, ButtonEvent.YES);
                }
            })
            .setNegativeButton(R.string.task_confirmation_no_button, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog
                    ConfirmDialogFragment.this.getDialog().cancel();
                }
            })
            .setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    return keyCode != KeyEvent.KEYCODE_BACK;
                }
            });
        // Create the AlertDialog object and return it
        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
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
}
