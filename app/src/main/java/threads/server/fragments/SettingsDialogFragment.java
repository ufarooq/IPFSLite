package threads.server.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.List;

import threads.ipfs.IPFS;
import threads.ipfs.PubSubConfig;
import threads.ipfs.RoutingConfig;
import threads.server.R;
import threads.server.jobs.JobServicePublisher;
import threads.server.services.Service;
import threads.server.utils.Preferences;

import static androidx.core.util.Preconditions.checkNotNull;

public class SettingsDialogFragment extends DialogFragment {

    public static final String TAG = SettingsDialogFragment.class.getSimpleName();


    private Context mContext;

    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }


    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {


        Activity activity = getActivity();
        checkNotNull(activity);


        LayoutInflater inflater = activity.getLayoutInflater();


        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.settings_view, null);

        Switch dht_support = view.findViewById(R.id.dht_support);
        dht_support.setChecked(IPFS.getRoutingType(activity) == RoutingConfig.TypeEnum.dht);
        dht_support.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                IPFS.setRoutingType(activity, RoutingConfig.TypeEnum.dht);
            } else {
                IPFS.setRoutingType(activity, RoutingConfig.TypeEnum.dhtclient);
            }
            Toast.makeText(getContext(),
                    R.string.daemon_restart_config_changed,
                    Toast.LENGTH_LONG).show();


        });

        Switch mdns_support = view.findViewById(R.id.mdns_support);
        mdns_support.setChecked(IPFS.isMDNSEnabled(activity));
        mdns_support.setOnCheckedChangeListener((buttonView, isChecked) -> {
            IPFS.setMDNSEnabled(activity, isChecked);

            Toast.makeText(getContext(),
                    R.string.daemon_restart_config_changed,
                    Toast.LENGTH_LONG).show();


        });


        Switch quic_support = view.findViewById(R.id.quic_support);
        quic_support.setChecked(IPFS.isQUICEnabled(activity));
        quic_support.setOnCheckedChangeListener((buttonView, isChecked) -> {
            IPFS.setQUICEnabled(activity, isChecked);

            Toast.makeText(getContext(),
                    R.string.daemon_restart_config_changed,
                    Toast.LENGTH_LONG).show();


        });


        Switch tls_prefer = view.findViewById(R.id.tls_prefer);
        tls_prefer.setChecked(IPFS.isPreferTLS(activity));
        tls_prefer.setOnCheckedChangeListener((buttonView, isChecked) -> {
            IPFS.setPreferTLS(activity, isChecked);

            Toast.makeText(getContext(),
                    R.string.daemon_restart_config_changed,
                    Toast.LENGTH_LONG).show();


        });


        Switch pubsub_support = view.findViewById(R.id.pubsub_support);
        pubsub_support.setChecked(IPFS.isPubSubEnabled(activity));
        pubsub_support.setOnCheckedChangeListener((buttonView, isChecked) -> {
            IPFS.setPubSubEnabled(activity, isChecked);

            Toast.makeText(getContext(),
                    R.string.daemon_restart_config_changed,
                    Toast.LENGTH_LONG).show();


        });

        Switch pubsub_router = view.findViewById(R.id.pubsub_router);
        pubsub_router.setChecked(IPFS.getPubSubRouter(activity)
                == PubSubConfig.RouterEnum.gossipsub);
        pubsub_router.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                IPFS.setPubSubRouter(activity, PubSubConfig.RouterEnum.gossipsub);
            } else {
                IPFS.setPubSubRouter(activity, PubSubConfig.RouterEnum.floodsub);
            }

            Toast.makeText(getContext(),
                    R.string.daemon_restart_config_changed,
                    Toast.LENGTH_LONG).show();


        });


        Switch auto_relay_support = view.findViewById(R.id.auto_relay_support);
        auto_relay_support.setChecked(IPFS.isAutoRelayEnabled(activity));
        auto_relay_support.setOnCheckedChangeListener((buttonView, isChecked) -> {
            IPFS.setAutoRelayEnabled(activity, isChecked);

            Toast.makeText(getContext(),
                    R.string.daemon_restart_config_changed,
                    Toast.LENGTH_LONG).show();


        });

        Switch auto_nat_service_enabled = view.findViewById(R.id.auto_nat_service_enabled);
        auto_nat_service_enabled.setChecked(IPFS.isAutoNATServiceEnabled(activity));
        auto_nat_service_enabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            IPFS.setAutoNATServiceEnabled(activity, isChecked);

            Toast.makeText(getContext(),
                    R.string.daemon_restart_config_changed,
                    Toast.LENGTH_LONG).show();


        });

        Switch relay_hop_enabled = view.findViewById(R.id.relay_hop_enabled);
        relay_hop_enabled.setChecked(IPFS.isRelayHopEnabled(activity));
        relay_hop_enabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            IPFS.setRelayHopEnabled(activity, isChecked);

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

        int pos = list.indexOf(Service.getGateway(mContext));
        pin_gateways.setSelection(pos);
        pin_gateways.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long l) {
                Service.setGateway(parent.getContext(), parent.getItemAtPosition(pos).toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        TextView publisher_service_time_text = view.findViewById(R.id.publisher_service_time_text);
        SeekBar publisher_service_time = view.findViewById(R.id.publisher_service_time);


        publisher_service_time.setMax(12);
        int time = 0;
        int pinServiceTime = Service.getPublishServiceTime(activity);
        if (pinServiceTime > 0) {
            time = (pinServiceTime);
        }
        publisher_service_time_text.setText(getString(R.string.publisher_service_time,
                String.valueOf(time)));
        publisher_service_time.setProgress(time);
        publisher_service_time.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {


                Service.setPublisherServiceTime(mContext, progress);
                publisher_service_time_text.setText(
                        getString(R.string.publisher_service_time,
                                String.valueOf(progress)));

                JobServicePublisher.publish(mContext);

            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // ignore, not used
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                // ignore, not used
            }
        });


        Switch enable_random_swarm_port = view.findViewById(R.id.enable_random_swarm_port);
        enable_random_swarm_port.setChecked(IPFS.isRandomSwarmPort(activity));
        enable_random_swarm_port.setOnCheckedChangeListener((buttonView, isChecked) -> {
            IPFS.setRandomSwarmPort(activity, isChecked);

            Toast.makeText(getContext(),
                    R.string.daemon_restart_config_changed,
                    Toast.LENGTH_LONG).show();
        });

        Switch support_peer_discovery = view.findViewById(R.id.support_peer_discovery);
        support_peer_discovery.setChecked(Service.isSupportPeerDiscovery(activity));
        support_peer_discovery.setOnCheckedChangeListener((buttonView, isChecked) ->
                Service.setSupportPeerDiscovery(activity, isChecked)
        );


        Switch send_notifications_enabled = view.findViewById(R.id.send_notifications_enabled);
        send_notifications_enabled.setChecked(Service.isSendNotificationsEnabled(activity));
        send_notifications_enabled.setOnCheckedChangeListener((buttonView, isChecked) ->
                Service.setSendNotificationsEnabled(activity, isChecked)
        );

        Switch receive_notifications_enabled = view.findViewById(R.id.receive_notifications_enabled);
        receive_notifications_enabled.setChecked(Service.isReceiveNotificationsEnabled(activity));
        receive_notifications_enabled.setOnCheckedChangeListener((buttonView, isChecked) ->
                Service.setReceiveNotificationsEnabled(activity, isChecked)
        );

        TextView connection_timeout_text = view.findViewById(R.id.connection_timeout_text);
        SeekBar connection_timeout = view.findViewById(R.id.connection_timeout);


        connection_timeout.setMax(180);

        int connectionTimeout = Preferences.getConnectionTimeout(activity);

        connection_timeout_text.setText(getString(R.string.connection_timeout,
                String.valueOf(connectionTimeout)));
        connection_timeout.setProgress(connectionTimeout);
        connection_timeout.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                Preferences.setConnectionTimeout(activity, progress);
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

        Switch support_automatic_download = view.findViewById(R.id.support_automatic_download);
        support_automatic_download.setChecked(Service.isAutoDownload(activity));
        support_automatic_download.setOnCheckedChangeListener((buttonView, isChecked) ->
                Service.setAutoDownload(activity, isChecked)
        );


        return new androidx.appcompat.app.AlertDialog.Builder(activity)
                .setView(view)
                .setCancelable(false)
                .create();

    }


}