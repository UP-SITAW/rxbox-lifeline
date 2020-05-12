package ph.chits.rxbox.lifeline.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import ph.chits.rxbox.lifeline.R;

public class SetServer extends DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        builder.setView(inflater.inflate(R.layout.dialog_set_server, null))
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        try {
                            TextInputEditText editText = getDialog().findViewById(R.id.edit_url);
                            String url = editText.getText().toString();
                            Context context = getActivity();
                            SharedPreferences sharedPref = context.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putString(getString(R.string.preference_server), url);
                            editor.apply();
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }

                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        SetServer.this.getDialog().cancel();
                    }
                });
        return builder.create();
    }

    @Override
    public void onResume() {
        super.onResume();

        AlertDialog alertDialog = (AlertDialog) getDialog();
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

        Context context = getActivity();
        SharedPreferences sharedPref = context.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String url = sharedPref.getString(getString(R.string.preference_server), null);
        Dialog d = SetServer.this.getDialog();
        TextInputEditText editText = d.findViewById(R.id.edit_url);
        editText.setText(url);
        editText.selectAll();

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String url = editable.toString();
                boolean is_valid = URLUtil.isValidUrl(url);
                AlertDialog alertDialog = (AlertDialog) getDialog();
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(is_valid);
                TextInputLayout textInputLayout = getDialog().findViewById(R.id.text_input_layout);
                if (!is_valid) {

                    textInputLayout.setError("Must be a valid URL");
                    textInputLayout.setErrorEnabled(true);
                } else {
                    textInputLayout.setErrorEnabled(false);
                }
            }
        });
    }
}
