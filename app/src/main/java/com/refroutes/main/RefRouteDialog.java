package com.refroutes.main;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.refroutes.R;

/**
 * Dialog for renaming or
 */
public class RefRouteDialog extends AppCompatDialogFragment {

    private String refRouteName;
    private String refRouteDescription;
    private String refRouteFileName;
    private Integer listIndex = -1;
    private int dialogMode;

    private EditText refRouteNameEditText;
    private EditText refRouteDescriptionEditText;
    private EditText refRouteFileNameEditText;
    private RefRouteDialogListener dialogListener;
    private String dialogDescription = "Title not set";

    public void setDialogDescription (String dialogDescription) {
        this.dialogDescription = dialogDescription;
    }

    public void setRefRouteName (String refRouteName) {
        this.refRouteName = refRouteName;
    }

    public void setRefRouteDescription (String refRouteDescription) {
        this.refRouteDescription = refRouteDescription;
    }

    public void setRefRouteFileName (String refRouteFileName) {
        this.refRouteFileName = refRouteFileName;
    }

    public void setDialogMode (int dialogMode) {
        this.dialogMode = dialogMode;
    }

    public void setListIndex (Integer listIndex) {
        this.listIndex = listIndex;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.refroute_dialog, null);

        builder.setView(view)
                .setNegativeButton("Abbruch", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Cancel, nothing to do
                        dialogListener.refRouteDialogCancel();
                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // OK, Werte verarbeiten
                        String refRouteName = refRouteNameEditText.getText().toString();
                        String refRouteDescription = refRouteDescriptionEditText.getText().toString();
                        String refRouteFileName = refRouteFileNameEditText.getText().toString();
                        dialogListener.refRouteDialogOk(refRouteName, refRouteDescription, refRouteFileName, listIndex, dialogMode);
                    }
                });

        refRouteNameEditText = view.findViewById(R.id.addRefRouteName);
        refRouteDescriptionEditText = view.findViewById(R.id.addRefRouteDescription);
        refRouteFileNameEditText = view.findViewById(R.id.addRefRouteFileName);

        if (this.refRouteName != null && this.refRouteName.length() > 0) {
            refRouteNameEditText.setText(this.refRouteName);
        }
        if (this.refRouteDescription != null && this.refRouteDescription.length() > 0) {
            refRouteDescriptionEditText.setText(this.refRouteDescription);
        }
        if (this.refRouteFileName != null && this.refRouteFileName.length() > 0) {
            refRouteFileNameEditText.setText(this.refRouteFileName);
        }
        return builder.create();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            dialogListener = (RefRouteDialogListener)context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + "Exception in NewRefRouteDialo not implemented yet");
        }
    }

    /**
     * Interface to implement in calling class
     */
    public interface RefRouteDialogListener {
        /**
         * Delivers the values of RefRouteName and RefRouteDescription
         * @param refRouteName
         * @param refRouteDescription
         * @param refRouteFileName
         * @param listIndex
         * @param dialogMode
         */
        void refRouteDialogOk(String refRouteName, String refRouteDescription, String refRouteFileName, Integer listIndex, int dialogMode);
        void refRouteDialogCancel();
    }

}
