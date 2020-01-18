package threads.ipfs;

import androidx.annotation.NonNull;

import static androidx.core.util.Preconditions.checkNotNull;

public class PubSubConfig {

    private RouterEnum Router;
    private boolean DisableSigning = false;
    private boolean StrictSignatureVerification = false;

    private PubSubConfig() {
        Router = RouterEnum.floodsub;
    }

    public static PubSubConfig create() {
        return new PubSubConfig();
    }

    @Override
    @NonNull
    public String toString() {
        return "PubSubConfig{" +
                "Router=" + Router +
                ", DisableSigning=" + DisableSigning +
                ", StrictSignatureVerification=" + StrictSignatureVerification +
                '}';
    }

    @NonNull
    public RouterEnum getRouter() {
        if (Router == null) {
            return RouterEnum.floodsub;
        }
        return Router;
    }

    public void setRouter(@NonNull RouterEnum router) {
        checkNotNull(router);
        this.Router = router;
    }

    public boolean isDisableSigning() {
        return DisableSigning;
    }

    public void setDisableSigning(boolean disableSigning) {
        DisableSigning = disableSigning;
    }

    public boolean isStrictSignatureVerification() {
        return StrictSignatureVerification;
    }

    public void setStrictSignatureVerification(boolean strictSignatureVerification) {
        StrictSignatureVerification = strictSignatureVerification;
    }

    public enum RouterEnum {
        gossipsub("gossipsub"), floodsub("");
        @NonNull
        private final String param;

        RouterEnum(@NonNull String param) {
            checkNotNull(param);
            this.param = param;
        }

        public static RouterEnum toRouter(@NonNull String router) {
            checkNotNull(router);
            if (router.isEmpty()) {
                return RouterEnum.floodsub;
            }
            return RouterEnum.valueOf(router);
        }

        public String param() {
            return param;
        }
    }

}
