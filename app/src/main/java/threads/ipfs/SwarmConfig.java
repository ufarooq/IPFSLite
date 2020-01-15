package threads.ipfs;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import static androidx.core.util.Preconditions.checkNotNull;

public class SwarmConfig {
    @NonNull
    private final ConnMgrConfig ConnMgr = ConnMgrConfig.create();

    private boolean DisableBandwidthMetrics = false;
    private boolean DisableNatPortMap = false;
    private boolean DisableRelay = false;
    private boolean EnableRelayHop = false;
    private boolean EnableAutoNATService = false;
    private boolean EnableAutoRelay = false;

    @NonNull
    private List<String> AddrFilters = new ArrayList<>();

    private SwarmConfig() {
    }

    public static SwarmConfig create() {
        return new SwarmConfig();
    }

    public boolean isEnableAutoNATService() {
        return EnableAutoNATService;
    }

    public void setEnableAutoNATService(boolean enableAutoNATService) {
        EnableAutoNATService = enableAutoNATService;
    }

    public boolean isEnableAutoRelay() {
        return EnableAutoRelay;
    }

    public void setEnableAutoRelay(boolean enableAutoRelay) {
        EnableAutoRelay = enableAutoRelay;
    }

    @NonNull
    public List<String> getAddrFilters() {
        return new ArrayList<>(AddrFilters);
    }

    public void addAddrFilterEntries(List<String> entries) {
        if (entries == null) {
            clearAddrFilter();
        } else {
            addAddrFilter(entries.toArray(new String[0]));
        }
    }

    public void addAddrFilter(@NonNull String... entries) {
        checkNotNull(entries);
        for (String entry : entries) {
            if (!AddrFilters.contains(entry)) {
                AddrFilters.add(entry);
            }
        }
    }

    public void clearAddrFilter() {
        AddrFilters.clear();
    }

    public boolean isDisableBandwidthMetrics() {
        return DisableBandwidthMetrics;
    }

    public void setDisableBandwidthMetrics(boolean disableBandwidthMetrics) {
        DisableBandwidthMetrics = disableBandwidthMetrics;
    }

    @Override
    @NonNull
    public String toString() {
        return "SwarmConfig{" +
                "ConnMgr=" + ConnMgr +
                ", DisableBandwidthMetrics=" + DisableBandwidthMetrics +
                ", DisableNatPortMap=" + DisableNatPortMap +
                ", DisableRelay=" + DisableRelay +
                ", EnableRelayHop=" + EnableRelayHop +
                ", EnableAutoNATService=" + EnableAutoNATService +
                ", EnableAutoRelay=" + EnableAutoRelay +
                ", AddrFilters=" + AddrFilters +
                '}';
    }

    public boolean isDisableNatPortMap() {
        return DisableNatPortMap;
    }


    public void setDisableNatPortMap(boolean disableNatPortMap) {
        DisableNatPortMap = disableNatPortMap;
    }

    public boolean isDisableRelay() {
        return DisableRelay;
    }

    public void setDisableRelay(boolean disableRelay) {
        DisableRelay = disableRelay;
    }

    public boolean isEnableRelayHop() {
        return EnableRelayHop;
    }

    public void setEnableRelayHop(boolean enableRelayHop) {
        EnableRelayHop = enableRelayHop;
    }

    @NonNull
    public ConnMgrConfig getConnMgr() {
        return ConnMgr;
    }
}
