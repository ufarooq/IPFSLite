package threads.server.fragments;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.List;

import threads.ipfs.IPFS;
import threads.ipfs.RoutingConfig;
import threads.server.InitApplication;
import threads.server.R;
import threads.server.services.LiteService;

public class SettingsDialogFragment extends DialogFragment {

    public static final String TAG = SettingsDialogFragment.class.getSimpleName();


    private Context mContext;
    private FragmentActivity mActivity;

    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
        mActivity = null;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
        mActivity = getActivity();
    }


    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {


        LayoutInflater inflater = mActivity.getLayoutInflater();


        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.settings_view, null);

        Switch dht_support = view.findViewById(R.id.dht_support);
        dht_support.setChecked(IPFS.getRoutingType(mContext) == RoutingConfig.TypeEnum.dht);
        dht_support.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                IPFS.setRoutingType(mContext, RoutingConfig.TypeEnum.dht);
            } else {
                IPFS.setRoutingType(mContext, RoutingConfig.TypeEnum.dhtclient);
            }
            Toast.makeText(getContext(),
                    R.string.daemon_restart_config_changed,
                    Toast.LENGTH_LONG).show();


        });


        Switch quic_support = view.findViewById(R.id.quic_support);
        quic_support.setChecked(IPFS.isQUICEnabled(mContext));
        quic_support.setOnCheckedChangeListener((buttonView, isChecked) -> {
            IPFS.setQUICEnabled(mContext, isChecked);

            Toast.makeText(getContext(),
                    R.string.daemon_restart_config_changed,
                    Toast.LENGTH_LONG).show();


        });


        Switch tls_prefer = view.findViewById(R.id.tls_prefer);
        tls_prefer.setChecked(IPFS.isPreferTLS(mContext));
        tls_prefer.setOnCheckedChangeListener((buttonView, isChecked) -> {
            IPFS.setPreferTLS(mContext, isChecked);

            Toast.makeText(getContext(),
                    R.string.daemon_restart_config_changed,
                    Toast.LENGTH_LONG).show();


        });


        Switch auto_relay_support = view.findViewById(R.id.auto_relay_support);
        auto_relay_support.setChecked(IPFS.isAutoRelayEnabled(mContext));
        auto_relay_support.setOnCheckedChangeListener((buttonView, isChecked) -> {
            IPFS.setAutoRelayEnabled(mContext, isChecked);

            Toast.makeText(getContext(),
                    R.string.daemon_restart_config_changed,
                    Toast.LENGTH_LONG).show();


        });

        Switch auto_nat_service_enabled = view.findViewById(R.id.auto_nat_service_enabled);
        auto_nat_service_enabled.setChecked(IPFS.isAutoNATServiceEnabled(mContext));
        auto_nat_service_enabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            IPFS.setAutoNATServiceEnabled(mContext, isChecked);

            Toast.makeText(getContext(),
                    R.string.daemon_restart_config_changed,
                    Toast.LENGTH_LONG).show();


        });

        Switch relay_hop_enabled = view.findViewById(R.id.relay_hop_enabled);
        relay_hop_enabled.setChecked(IPFS.isRelayHopEnabled(mContext));
        relay_hop_enabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            IPFS.setRelayHopEnabled(mContext, isChecked);

            Toast.makeText(getContext(),
                    R.string.daemon_restart_config_changed,
                    Toast.LENGTH_LONG).show();


        });


        Spinner pin_gateways = view.findViewById(R.id.publisher_gateways);
        List<String> list = new ArrayList<>();
        list.add("https://ipfs.io");
        list.add("https://cloudflare-ipfs.com");
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(mContext,
                android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        pin_gateways.setAdapter(dataAdapter);

        int pos = list.indexOf(LiteService.getGateway(mContext));
        pin_gateways.setSelection(pos);
        pin_gateways.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long l) {
                LiteService.setGateway(parent.getContext(), parent.getItemAtPosition(pos).toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        TextView publisher_service_time_text = view.findViewById(R.id.publisher_service_time_text);
        SeekBar publisher_service_time = view.findViewById(R.id.publisher_service_time);


        publisher_service_time.setMax(12);
        int time = 0;
        int pinServiceTime = LiteService.getPublishServiceTime(mContext);
        if (pinServiceTime > 0) {
            time = (pinServiceTime);
        }
        publisher_service_time_text.setText(getString(R.string.publisher_service_time,
                String.valueOf(time)));
        publisher_service_time.setProgress(time);
        publisher_service_time.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {


                LiteService.setPublisherServiceTime(mContext, progress);
                publisher_service_time_text.setText(
                        getString(R.string.publisher_service_time,
                                String.valueOf(progress)));
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // ignore, not used
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                // ignore, not used
            }
        });


        Switch enable_random_swarm_port = view.findViewById(R.id.enable_random_swarm_port);
        enable_random_swarm_port.setChecked(IPFS.isRandomSwarmPort(mContext));
        enable_random_swarm_port.setOnCheckedChangeListener((buttonView, isChecked) -> {
            IPFS.setRandomSwarmPort(mContext, isChecked);

            Toast.makeText(getContext(),
                    R.string.daemon_restart_config_changed,
                    Toast.LENGTH_LONG).show();
        });

        Switch send_notifications_enabled = view.findViewById(R.id.send_notifications_enabled);
        send_notifications_enabled.setChecked(LiteService.isSendNotificationsEnabled(mContext));
        send_notifications_enabled.setOnCheckedChangeListener((buttonView, isChecked) ->
                LiteService.setSendNotificationsEnabled(mContext, isChecked)
        );

        Switch receive_notifications_enabled = view.findViewById(R.id.receive_notifications_enabled);
        receive_notifications_enabled.setChecked(LiteService.isReceiveNotificationsEnabled(mContext));
        receive_notifications_enabled.setOnCheckedChangeListener((buttonView, isChecked) ->
                LiteService.setReceiveNotificationsEnabled(mContext, isChecked)
        );

        TextView connection_timeout_text = view.findViewById(R.id.connection_timeout_text);
        SeekBar connection_timeout = view.findViewById(R.id.connection_timeout);


        connection_timeout.setMax(120);

        int connectionTimeout = InitApplication.getConnectionTimeout(mContext);

        connection_timeout_text.setText(getString(R.string.connection_timeout,
                String.valueOf(connectionTimeout)));
        connection_timeout.setProgress(connectionTimeout);
        connection_timeout.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                InitApplication.setConnectionTimeout(mContext, progress);
                connection_timeout_text.setText(
                        getString(R.string.connection_timeout,
                                String.valueOf(progress)));

            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // ignore, not used
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                // ignore, not used
            }
        });

        TextView download_timeout_text = view.findViewById(R.id.download_timeout_text);
        SeekBar download_timeout = view.findViewById(R.id.download_timeout);


        download_timeout.setMax(600);

        int downloadTimeout = InitApplication.getDownloadTimeout(mContext);

        download_timeout_text.setText(getString(R.string.download_timeout,
                String.valueOf(downloadTimeout)));
        download_timeout.setProgress(downloadTimeout);
        download_timeout.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                InitApplication.setDownloadTimeout(mContext, progress);
                download_timeout_text.setText(
                        getString(R.string.download_timeout,
                                String.valueOf(progress)));

            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // ignore, not used
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                // ignore, not used
            }
        });

        Switch support_automatic_download = view.findViewById(R.id.support_automatic_download);
        support_automatic_download.setChecked(LiteService.isAutoDownload(mContext));
        support_automatic_download.setOnCheckedChangeListener((buttonView, isChecked) ->
                LiteService.setAutoDownload(mContext, isChecked)
        );

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setView(view)
                .setCancelable(false)
                .create();
        Dialog dialog = builder.create();
        Window window = dialog.getWindow();
        if (window != null) {
            window.getAttributes().windowAnimations = R.style.DialogRightAnimation;
            window.getAttributes().gravity = Gravity.CENTER;
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        return dialog;

    }


}
