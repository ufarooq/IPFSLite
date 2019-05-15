package threads.server;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import threads.core.Preferences;

import static androidx.core.util.Preconditions.checkNotNull;

public class SettingsDialogFragment extends DialogFragment {

    static final String TAG = SettingsDialogFragment.class.getSimpleName();

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Activity activity = getActivity();
        checkNotNull(activity);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);


        LayoutInflater inflater = activity.getLayoutInflater();


        @SuppressWarnings("all")
        View view = inflater.inflate(R.layout.settings_view, null);


        Switch quic_support = view.findViewById(R.id.quic_support);
        quic_support.setChecked(Preferences.isQUICEnabled(activity));
        quic_support.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Preferences.setQUICEnabled(activity, isChecked);
            Preferences.setConfigChanged(activity, true);

            Toast.makeText(getContext(),
                    R.string.daemon_restart_config_changed,
                    Toast.LENGTH_LONG).show();


        });


        Switch pubsub_support = view.findViewById(R.id.pubsub_support);
        pubsub_support.setChecked(Preferences.isPubsubEnabled(activity));
        pubsub_support.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Preferences.setPubsubEnabled(activity, isChecked);
            Preferences.setConfigChanged(activity, true);

            Toast.makeText(getContext(),
                    R.string.daemon_restart_config_changed,
                    Toast.LENGTH_LONG).show();


        });


        Switch auto_relay_support = view.findViewById(R.id.auto_relay_support);
        auto_relay_support.setChecked(Preferences.isAutoRelayEnabled(activity));
        auto_relay_support.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Preferences.setAutoRelayEnabled(activity, isChecked);
            Preferences.setConfigChanged(activity, true);

            Toast.makeText(getContext(),
                    R.string.daemon_restart_config_changed,
                    Toast.LENGTH_LONG).show();


        });


        Switch connect_relay_support = view.findViewById(R.id.connect_relay_support);
        connect_relay_support.setChecked(Preferences.isAutoConnectRelay(activity));
        connect_relay_support.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Preferences.setAutoConnectRelay(activity, isChecked);
            Toast.makeText(getContext(),
                    R.string.daemon_restart_config_changed,
                    Toast.LENGTH_LONG).show();

        });

        TextView connection_timeout_text = view.findViewById(R.id.connection_timeout_text);
        SeekBar connection_timeout = view.findViewById(R.id.connection_timeout);


        int offset = 10;
        connection_timeout.setMax(170);
        int timeout = (Preferences.getConnectionTimeout(activity) / 1000);
        connection_timeout_text.setText(getString(R.string.connection_timeout,
                String.valueOf(timeout)));
        connection_timeout.setProgress(timeout - offset);
        connection_timeout.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                int newProgress = progress + offset;
                int newValue = newProgress * 1000;
                Preferences.setConnectionTimeout(activity, newValue);
                connection_timeout_text.setText(
                        getString(R.string.connection_timeout,
                                String.valueOf(newProgress)));

            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // ignore, not used
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                // ignore, not used
            }
        });
        builder.setView(view);

        return new androidx.appcompat.app.AlertDialog.Builder(activity)
                .setView(view)
                .setCancelable(false)
                .create();

    }

}
