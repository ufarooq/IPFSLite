package threads.server;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.common.collect.Lists;

import java.util.List;

import threads.ipfs.api.Profile;

import static com.google.common.base.Preconditions.checkNotNull;

public class SettingsDialog extends DialogFragment {
    private static final String TAG = SettingsDialog.class.getSimpleName();

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Activity activity = getActivity();
        checkNotNull(activity);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);


        LayoutInflater inflater = activity.getLayoutInflater();


        @SuppressWarnings("all")
        View view = inflater.inflate(R.layout.settings_view, null);

        Spinner spinner_profile = view.findViewById(R.id.spinner_profile);
        List<Profile> profiles = Lists.newArrayList(Profile.values());
        ArrayAdapter<Profile> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, profiles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_profile.setAdapter(adapter);
        spinner_profile.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                try {
                    Profile profile = (Profile) parent.getItemAtPosition(position);
                    if (profile != Application.getProfile(getContext())) {
                        Application.setProfile(getContext(), profile);
                        DaemonService.configHasChanged = true;
                        if (DaemonService.isIpfsRunning()) {
                            Toast.makeText(getContext(),
                                    R.string.daemon_restart_config_changed,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.getLocalizedMessage());
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        int pos = profiles.indexOf(Application.getProfile(getContext()));
        spinner_profile.setSelection(pos);
        builder.setView(view);

        return new android.support.v7.app.AlertDialog.Builder(getActivity())
                .setTitle(R.string.settings)
                .setMessage(R.string.settings_message)
                .setView(view)
                .setCancelable(false)
                .create();

    }

}
