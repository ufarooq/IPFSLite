package threads.server;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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

import threads.core.Preferences;
import threads.ipfs.api.PubsubConfig;
import threads.ipfs.api.RoutingConfig;

import static androidx.core.util.Preconditions.checkNotNull;

public class SettingsDialogFragment extends DialogFragment implements View.OnTouchListener {

    static final String TAG = SettingsDialogFragment.class.getSimpleName();

    private float downX, downY, upX, upY;
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
    public boolean onTouch(View v, MotionEvent event) {

        int min_distance = 200;
        if (event.getOrientation() == 0) {
            switch (event.getAction()) { // Check vertical and horizontal touches
                case MotionEvent.ACTION_DOWN: {
                    downX = event.getX();
                    downY = event.getY();
                    return true;
                }
                case MotionEvent.ACTION_UP: {
                    upX = event.getX();
                    upY = event.getY();

                    float deltaX = downX - upX;
                    float deltaY = downY - upY;

                    //HORIZONTAL SCROLL
                    if (Math.abs(deltaX) > Math.abs(deltaY)) {
                        if (Math.abs(deltaX) > min_distance) {
                            // left or right
                            if (deltaX < 0) {
                                this.onLeftToRightSwipe();
                                return true;
                            }
                            if (deltaX > 0) {
                                this.onRightToLeftSwipe();
                                return true;
                            }
                        } else {
                            //not long enough swipe...
                            return false;
                        }
                    }
                    return false;
                }
            }
        }
        return false;
    }

    public void onLeftToRightSwipe() {
        try {
            final View decorView = getDialog().getWindow().getDecorView();

            ObjectAnimator scaleDown = ObjectAnimator.ofFloat(decorView,
                    View.TRANSLATION_X, decorView.getWidth());
            scaleDown.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    dismiss();
                }

                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
            scaleDown.setDuration(500);
            scaleDown.start();
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    public void onRightToLeftSwipe() {
        try {
            final View decorView = getDialog().getWindow().getDecorView();

            ObjectAnimator scaleDown = ObjectAnimator.ofFloat(decorView,
                    View.TRANSLATION_X, -decorView.getWidth());

            scaleDown.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    dismiss();
                }

                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
            scaleDown.setDuration(500);
            scaleDown.start();
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }


    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {


        Activity activity = getActivity();
        checkNotNull(activity);


        LayoutInflater inflater = activity.getLayoutInflater();


        @SuppressWarnings("all")
        View view = inflater.inflate(R.layout.settings_view, null);
        //view.setOnTouchListener(this); // TODO not yet activated

        Switch dht_support = view.findViewById(R.id.dht_support);
        dht_support.setChecked(Preferences.getRoutingType(activity) == RoutingConfig.TypeEnum.dht);
        dht_support.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Preferences.setRoutingType(activity, RoutingConfig.TypeEnum.dht);
            } else {
                Preferences.setRoutingType(activity, RoutingConfig.TypeEnum.dhtclient);
            }
            Toast.makeText(getContext(),
                    R.string.daemon_restart_config_changed,
                    Toast.LENGTH_LONG).show();


        });

        Switch mdns_support = view.findViewById(R.id.mdns_support);
        mdns_support.setChecked(Preferences.isMdnsEnabled(activity));
        mdns_support.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Preferences.setMdnsEnabled(activity, isChecked);

            Toast.makeText(getContext(),
                    R.string.daemon_restart_config_changed,
                    Toast.LENGTH_LONG).show();


        });

        Switch reprovider_enabled = view.findViewById(R.id.reprovider_enabled);
        reprovider_enabled.setChecked(Preferences.getReproviderInterval(activity).equals("12h"));
        reprovider_enabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Preferences.setReproviderInterval(activity, "12h");
            } else {
                Preferences.setReproviderInterval(activity, "0"); // disable
            }

            Toast.makeText(getContext(),
                    R.string.daemon_restart_config_changed,
                    Toast.LENGTH_LONG).show();


        });


        Switch quic_support = view.findViewById(R.id.quic_support);
        quic_support.setChecked(Preferences.isQUICEnabled(activity));
        quic_support.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Preferences.setQUICEnabled(activity, isChecked);

            Toast.makeText(getContext(),
                    R.string.daemon_restart_config_changed,
                    Toast.LENGTH_LONG).show();


        });


        Switch tls_prefer = view.findViewById(R.id.tls_prefer);
        tls_prefer.setChecked(Preferences.isPreferTLS(activity));
        tls_prefer.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Preferences.setPreferTLS(activity, isChecked);

            Toast.makeText(getContext(),
                    R.string.daemon_restart_config_changed,
                    Toast.LENGTH_LONG).show();


        });


        Switch pubsub_support = view.findViewById(R.id.pubsub_support);
        pubsub_support.setChecked(Preferences.isPubsubEnabled(activity));
        pubsub_support.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Preferences.setPubsubEnabled(activity, isChecked);

            Toast.makeText(getContext(),
                    R.string.daemon_restart_config_changed,
                    Toast.LENGTH_LONG).show();


        });

        Switch pubsub_router = view.findViewById(R.id.pubsub_router);
        pubsub_router.setChecked(Preferences.getPubsubRouter(activity)
                == PubsubConfig.RouterEnum.gossipsub);
        pubsub_router.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Preferences.setPubsubRouter(activity, PubsubConfig.RouterEnum.gossipsub);
            } else {
                Preferences.setPubsubRouter(activity, PubsubConfig.RouterEnum.floodsub);
            }

            Toast.makeText(getContext(),
                    R.string.daemon_restart_config_changed,
                    Toast.LENGTH_LONG).show();


        });


        Switch logs_mode_enable = view.findViewById(R.id.logs_mode_enable);
        logs_mode_enable.setChecked(Preferences.isDebugMode(activity));
        logs_mode_enable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Preferences.setDebugMode(activity, isChecked);

            Toast.makeText(getContext(),
                    R.string.daemon_restart_config_changed,
                    Toast.LENGTH_LONG).show();


        });

        Switch auto_relay_support = view.findViewById(R.id.auto_relay_support);
        auto_relay_support.setChecked(Preferences.isAutoRelayEnabled(activity));
        auto_relay_support.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Preferences.setAutoRelayEnabled(activity, isChecked);

            Toast.makeText(getContext(),
                    R.string.daemon_restart_config_changed,
                    Toast.LENGTH_LONG).show();


        });

        Switch auto_nat_service_enabled = view.findViewById(R.id.auto_nat_service_enabled);
        auto_nat_service_enabled.setChecked(Preferences.isAutoNATServiceEnabled(activity));
        auto_nat_service_enabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Preferences.setAutoNATServiceEnabled(activity, isChecked);

            Toast.makeText(getContext(),
                    R.string.daemon_restart_config_changed,
                    Toast.LENGTH_LONG).show();


        });

        Switch relay_hop_enabled = view.findViewById(R.id.relay_hop_enabled);
        relay_hop_enabled.setChecked(Preferences.isRelayHopEnabled(activity));
        relay_hop_enabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Preferences.setRelayHopEnabled(activity, isChecked);

            Toast.makeText(getContext(),
                    R.string.daemon_restart_config_changed,
                    Toast.LENGTH_LONG).show();


        });


        Spinner pin_gateways = view.findViewById(R.id.pin_gateways);
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


        TextView pin_service_time_text = view.findViewById(R.id.pin_service_time_text);
        SeekBar pin_service_time = view.findViewById(R.id.pin_service_time);


        pin_service_time.setMax(12);
        int time = 0;
        int pinServiceTime = Service.getPinServiceTime(activity);
        if (pinServiceTime > 0) {
            time = (pinServiceTime);
        }
        pin_service_time_text.setText(getString(R.string.pin_service_time,
                String.valueOf(time)));
        pin_service_time.setProgress(time);
        pin_service_time.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {


                Service.setPinServiceTime(activity, progress);
                pin_service_time_text.setText(
                        getString(R.string.pin_service_time,
                                String.valueOf(progress)));

            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // ignore, not used
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                // ignore, not used
            }
        });


        Switch support_peer_discovery = view.findViewById(R.id.support_peer_discovery);
        support_peer_discovery.setChecked(Service.isSupportPeerDiscovery(activity));
        support_peer_discovery.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Service.setSupportPeerDiscovery(activity, isChecked);
        });


        Switch send_notifciations_enabled = view.findViewById(R.id.send_notifications_enabled);
        send_notifciations_enabled.setChecked(Service.isSendNotificationsEnabled(activity));
        send_notifciations_enabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Service.setSendNotificationsEnabled(activity, isChecked);
        });

        Switch receive_notifciations_enabled = view.findViewById(R.id.receive_notifications_enabled);
        receive_notifciations_enabled.setChecked(Service.isReceiveNotificationsEnabled(activity));
        receive_notifciations_enabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Service.setReceiveNotificationsEnabled(activity, isChecked);
        });

        TextView connection_timeout_text = view.findViewById(R.id.connection_timeout_text);
        SeekBar connection_timeout = view.findViewById(R.id.connection_timeout);


        connection_timeout.setMax(180);
        int timeout = 0;
        int connectionTimeout = Preferences.getConnectionTimeout(activity);
        if (connectionTimeout > 0) {
            timeout = (connectionTimeout / 1000);
        }
        connection_timeout_text.setText(getString(R.string.connection_timeout,
                String.valueOf(timeout)));
        connection_timeout.setProgress(timeout);
        connection_timeout.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {


                int newValue = progress * 1000;
                Preferences.setConnectionTimeout(activity, newValue);
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
        support_automatic_download.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Service.setAutoDownload(activity, isChecked);
        });


        return new androidx.appcompat.app.AlertDialog.Builder(activity)
                .setView(view)
                .setCancelable(false)
                .create();

    }


}
