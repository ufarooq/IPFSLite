package threads.ipfs;

import androidx.annotation.NonNull;

public class ConnMgrConfig {
    @NonNull
    private TypeEnum Type = TypeEnum.basic;
    private int LowWater = 600;
    private int HighWater = 900;
    @NonNull
    private String GracePeriod = "20s";

    private ConnMgrConfig() {
    }

    public static ConnMgrConfig create() {
        return new ConnMgrConfig();
    }

    @NonNull
    public TypeEnum getType() {
        return Type;
    }

    public void setType(@NonNull TypeEnum type) {
        this.Type = type;
    }

    public int getLowWater() {
        return LowWater;
    }

    public void setLowWater(int lowWater) {
        LowWater = lowWater;
    }

    public int getHighWater() {
        return HighWater;
    }

    public void setHighWater(int highWater) {
        HighWater = highWater;
    }

    @NonNull
    public String getGracePeriod() {
        return GracePeriod;
    }

    public void setGracePeriod(@NonNull String gracePeriod) {
        GracePeriod = gracePeriod;
    }

    @Override
    @NonNull
    public String toString() {
        return "ConnMgrConfig{" +
                "Type=" + Type +
                ", LowWater=" + LowWater +
                ", HighWater=" + HighWater +
                ", GracePeriod='" + GracePeriod + '\'' +
                '}';
    }

    public enum TypeEnum {
        none, basic
    }

}
