package threads.server;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import com.google.common.collect.Lists;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import threads.core.Preferences;
import threads.ipfs.api.Profile;

import static com.google.common.base.Preconditions.checkNotNull;

public class SettingsDialogFragment extends DialogFragment {
    public static final String TAG = SettingsDialogFragment.class.getSimpleName();

    @Override
    @NonNull
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
                    if (profile != Preferences.getProfile(getContext())) {
                        Preferences.setProfile(getContext(), profile);
                        Application.setConfigChanged(getContext(), true);

                        if (DaemonService.DAEMON_RUNNING.get()) {
                            Toast.makeText(getContext(),
                                    R.string.daemon_restart_config_changed,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        int pos = profiles.indexOf(Preferences.getProfile(getContext()));
        spinner_profile.setSelection(pos);


        Switch quic_support = view.findViewById(R.id.quic_support);
        quic_support.setChecked(Preferences.isQUICEnabled(getContext()));
        quic_support.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Preferences.setQUICEnabled(getContext(), isChecked);
            Application.setConfigChanged(getContext(), true);
            if (DaemonService.DAEMON_RUNNING.get()) {
                Toast.makeText(getContext(),
                        R.string.daemon_restart_config_changed,
                        Toast.LENGTH_LONG).show();
            }

        });


        Switch pubsub_support = view.findViewById(R.id.pubsub_support);
        pubsub_support.setChecked(Preferences.isPubsubEnabled(getContext()));
        pubsub_support.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Preferences.setPubsubEnabled(getContext(), isChecked);
            Application.setConfigChanged(getContext(), true);
            if (DaemonService.DAEMON_RUNNING.get()) {
                Toast.makeText(getContext(),
                        R.string.daemon_restart_config_changed,
                        Toast.LENGTH_LONG).show();
            }

        });


        Switch auto_relay_support = view.findViewById(R.id.auto_relay_support);
        auto_relay_support.setChecked(Preferences.isAutoRelayEnabled(getContext()));
        auto_relay_support.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Preferences.setAutoRelayEnabled(getContext(), isChecked);
            Application.setConfigChanged(getContext(), true);
            if (DaemonService.DAEMON_RUNNING.get()) {
                Toast.makeText(getContext(),
                        R.string.daemon_restart_config_changed,
                        Toast.LENGTH_LONG).show();
            }

        });

        builder.setView(view);

        return new androidx.appcompat.app.AlertDialog.Builder(getActivity())
                .setTitle(R.string.settings)
                .setMessage(R.string.settings_message)
                .setView(view)
                .setCancelable(false)
                .create();

    }

}
