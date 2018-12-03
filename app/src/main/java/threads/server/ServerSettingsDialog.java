package threads.server;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import threads.iota.daemon.IThreadsServer;

import static com.google.common.base.Preconditions.checkNotNull;

public class ServerSettingsDialog extends DialogFragment implements DialogInterface.OnClickListener {
    private static final String TAG = "ServerSettingsDialog";

    private int portDaemon;
    private String hostDaemon;

    private int orgPortDaemon;
    private String orgHostDaemon;

    private RestartServerListener restartServerListener;

    public ServerSettingsDialog() {
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
    }

    public static void show(@NonNull Activity activity) {
        checkNotNull(activity);

        ServerSettingsDialog fragment = new ServerSettingsDialog();
        fragment.show(activity.getFragmentManager(), null);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            restartServerListener = (RestartServerListener) getActivity();
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    }


    private void setPort(@NonNull Integer port) {
        portDaemon = port;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View view = inflater.inflate(R.layout.dialog_server_settings, null, false);

        SharedPreferences sharedPref = getActivity().getSharedPreferences(IThreadsServer.CONFIG, Context.MODE_PRIVATE);
        orgHostDaemon = sharedPref.getString(IThreadsServer.CONFIG_HOST, "");
        orgPortDaemon = sharedPref.getInt(IThreadsServer.CONFIG_PORT, IThreadsServer.TCP_PORT);


        setHost(orgHostDaemon);
        setPort(orgPortDaemon);


        TextInputLayout port_layout = view.findViewById(R.id.port_layout);
        port_layout.setCounterEnabled(true);
        port_layout.setCounterMaxLength(5);

        TextInputEditText port = view.findViewById(R.id.port);
        InputFilter[] filterAge = new InputFilter[1];
        filterAge[0] = new InputFilter.LengthFilter(5);
        port.setFilters(filterAge);
        port.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String portStr = s.toString();
                try {
                    Integer port = Integer.valueOf(portStr);
                    if (port >= IThreadsServer.MIN_PORT &&
                            port <= IThreadsServer.MAX_PORT) {
                        setPort(port);
                        port_layout.setError(null);
                    } else {
                        port_layout.setError(getString(R.string.port_error));
                    }
                } catch (Exception e) {
                    port_layout.setError(e.getLocalizedMessage());
                }
            }
        });

        port.setText(String.valueOf(portDaemon));


        TextInputEditText host_text = view.findViewById(R.id.host);


        host_text.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        host_text.setText(hostDaemon);
        host_text.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String host = s.toString();
                setHost(host);
            }
        });

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.daemon_server_settings)
                .setView(view)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int which) {
        switch (which) {
            case AlertDialog.BUTTON_POSITIVE:


                boolean restart = false;
                boolean reset = false;
                if (!orgHostDaemon.equals(hostDaemon)) {
                    reset = true;
                }
                if (orgPortDaemon != portDaemon) {
                    reset = true;
                    restart = true;
                }

                if (reset) {
                    IThreadsServer.setConfig(getContext(), hostDaemon, portDaemon);
                }

                if (restart) {
                    restartServerListener.restartServer();
                } else {
                    if (reset) {
                        restartServerListener.renameHost();
                    }
                }


                getDialog().dismiss();
                break;
        }
    }

    public void setHost(@NonNull String host) {
        this.hostDaemon = host;
    }


}