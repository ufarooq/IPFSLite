package threads.server;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
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
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import java.net.Inet6Address;
import java.net.InetAddress;

import threads.iri.ITangleDaemon;
import threads.iri.daemon.TangleDaemon;
import threads.iri.server.ServerConfig;

import static com.google.common.base.Preconditions.checkNotNull;

public class ServerSettingsDialog extends DialogFragment implements DialogInterface.OnClickListener {
    private static final String TAG = "ServerInfoDialog";

    private Integer portDaemon;
    private Boolean powDaemon;
    private String hostDaemon;

    public ServerSettingsDialog() {
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
    }



    public static void show(@NonNull Activity activity) {
        checkNotNull(activity);

        try {

            Bundle bundle = new Bundle();

            ServerSettingsDialog fragment = new ServerSettingsDialog();
            fragment.setArguments(bundle);
            fragment.show(activity.getFragmentManager(), null);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }


    }

    public void setLocalPow(@NonNull Boolean localPow) {
        this.powDaemon = localPow;
    }

    private void setPort(@NonNull Integer port) {
        portDaemon = port;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View view = inflater.inflate(R.layout.dialog_server_settings, null, false);


        Bundle bundle = getArguments();

        ServerConfig serverConfig = Application.getServerConfig(getContext());

        String host = serverConfig.getHost();
        setHost(host);

        Boolean localhostAddress = false;
        Boolean ipv6Address = false;

        try {
            if (!host.isEmpty()) {
                if ("localhost".equals(host) || host.equals(ITangleDaemon.getIPAddress(true)) ||
                        host.equals(ITangleDaemon.getIPAddress(false))) {
                    localhostAddress = true;
                }
            }

            InetAddress address = InetAddress.getByName(host);
            ipv6Address = address instanceof Inet6Address;
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

        Integer portDaemon = Integer.valueOf(serverConfig.getPort());
        setPort(portDaemon);

        Boolean powDaemon = serverConfig.isLocalPow();
        setLocalPow(powDaemon);


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
                    if (port >= Application.MIN_PORT &&
                            port <= Application.MAX_PORT) {
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

        port.setText(portDaemon.toString());


        Switch pow_support = view.findViewById(R.id.pow_support);
        pow_support.setChecked(powDaemon);
        Switch ipv6_support = view.findViewById(R.id.ipv6_support);
        ipv6_support.setChecked(ipv6Address);
        Switch localhost = view.findViewById(R.id.localhost);
        localhost.setChecked(localhostAddress);
        TextInputLayout host_layout = view.findViewById(R.id.host_layout);
        TextInputEditText host_text = view.findViewById(R.id.host);



        pow_support.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setLocalPow(isChecked);
            }
        });

        if (localhostAddress) {
            ipv6_support.setVisibility(View.VISIBLE);
        } else {
            ipv6_support.setVisibility(View.INVISIBLE);
        }


        ipv6_support.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                String host = ITangleDaemon.getIPAddress(!isChecked);
                setHost(host);
                host_text.setText(host);
            }
        });


        localhost.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    ipv6_support.setVisibility(View.VISIBLE);
                } else {
                    ipv6_support.setVisibility(View.INVISIBLE);
                }
            }
        });


        host_text.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        host_text.setText(host);
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
                if (host.isEmpty()) {
                    host_layout.setError(getString(R.string.host_invalid));
                } else {
                    host_layout.setError(null);
                }
            }
        });

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.tangle_server_settings)
                .setView(view)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int which) {
        switch (which) {
            case AlertDialog.BUTTON_POSITIVE:


                ServerConfig serverConfig = Application.getServerConfig(getContext());

                boolean restart = false;
                boolean reset = false;
                if (!serverConfig.getHost().equals(hostDaemon)) {
                    reset = true;
                }
                if (!serverConfig.getPort().equals(portDaemon.toString()) ||
                        serverConfig.isLocalPow() != powDaemon) {
                    reset = true;
                    restart = true;
                }

                if (reset) {
                    Application.setServerConfig(getContext(),
                            ServerConfig.createServerConfig(
                                    serverConfig.getProtocol(),
                                    hostDaemon,
                                    portDaemon.toString(),
                                    serverConfig.getCert(),
                                    powDaemon));


                }
                if (restart) {
                    ITangleDaemon tangleDaemon = TangleDaemon.getInstance();
                    if (tangleDaemon.isDaemonRunning()) {
                        // now message and restart
                        Toast.makeText(getActivity(), R.string.tangle_server_restart, Toast.LENGTH_LONG).show();


                        RestartDaemonService task = new RestartDaemonService(getActivity());
                        task.execute();
                    }
                } else {
                    if (reset) {
                        DaemonStatusService service = new DaemonStatusService(getActivity());
                        service.execute();
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
