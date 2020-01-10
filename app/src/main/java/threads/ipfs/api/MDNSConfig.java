package threads.ipfs.api;

import androidx.annotation.NonNull;

public class MDNSConfig {
    private boolean Enabled = true;
    private int Interval = 10;

    private MDNSConfig() {
    }

    public static MDNSConfig create() {
        return new MDNSConfig();
    }

    public boolean isEnabled() {
        return Enabled;
    }

    public void setEnabled(boolean enabled) {
        Enabled = enabled;
    }

    public int getInterval() {
        return Interval;
    }

    public void setInterval(int interval) {
        Interval = interval;
    }

    @Override
    @NonNull
    public String toString() {
        return "MDNSConfig{" +
                "Enabled=" + Enabled +
                ", Interval=" + Interval +
                '}';
    }
}
