package threads.ipfs;

public class ExperimentalConfig {
    private boolean QUIC = false;
    private boolean StrategicProviding = false;
    private boolean PreferTLS = false;

    private ExperimentalConfig() {
    }

    public static ExperimentalConfig create() {
        return new ExperimentalConfig();
    }

    public boolean isStrategicProviding() {
        return StrategicProviding;
    }

    public void setStrategicProviding(boolean strategicProviding) {
        StrategicProviding = strategicProviding;
    }

    public boolean isPreferTLS() {
        return PreferTLS;
    }

    public void setPreferTLS(boolean preferTLS) {
        PreferTLS = preferTLS;
    }

    @Override
    public String toString() {
        return "ExperimentalConfig{" +
                "QUIC=" + QUIC +
                ", StrategicProviding=" + StrategicProviding +
                ", PreferTLS=" + PreferTLS +
                '}';
    }

    public boolean isQUIC() {
        return QUIC;
    }

    public void setQUIC(boolean QUIC) {
        this.QUIC = QUIC;
    }
}
