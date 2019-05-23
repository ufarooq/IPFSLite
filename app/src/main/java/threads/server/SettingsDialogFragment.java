package threads.server;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import threads.core.Preferences;
import threads.ipfs.api.PubsubConfig;

import static androidx.core.util.Preconditions.checkNotNull;

public class SettingsDialogFragment extends DialogFragment implements View.OnTouchListener {

    static final String TAG = SettingsDialogFragment.class.getSimpleName();

    private float downX, downY, upX, upY;


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
            Preferences.evaluateException(Preferences.EXCEPTION, e);
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
            Preferences.evaluateException(Preferences.EXCEPTION, e);
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

        Switch quic_support = view.findViewById(R.id.quic_support);
        quic_support.setChecked(Preferences.isQUICEnabled(activity));
        quic_support.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Preferences.setQUICEnabled(activity, isChecked);

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

        Switch auto_relay_support = view.findViewById(R.id.auto_relay_support);
        auto_relay_support.setChecked(Preferences.isAutoRelayEnabled(activity));
        auto_relay_support.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Preferences.setAutoRelayEnabled(activity, isChecked);

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

        return new androidx.appcompat.app.AlertDialog.Builder(activity)
                .setView(view)
                .setCancelable(false)
                .create();

    }


}
