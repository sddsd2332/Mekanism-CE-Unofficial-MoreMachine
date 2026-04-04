package mekceumoremachine.common.config;

import javax.annotation.Nullable;

public class MoreMachineConfig {

    private static MoreMachineConfig LOCAL = new MoreMachineConfig();
    private static MoreMachineConfig SERVER = null;

    public static MoreMachineConfig current() {
        return SERVER != null ? SERVER : LOCAL;
    }

    public static MoreMachineConfig local() {
        return LOCAL;
    }

    public static void setSyncedConfig(@Nullable MoreMachineConfig newConfig) {
        if (newConfig != null) {
            newConfig.client = LOCAL.client;
        }
        SERVER = newConfig;
    }

    public MekCEUMoreMachineConfig config = new MekCEUMoreMachineConfig();
    public MoreMachineClientConfig client = new MoreMachineClientConfig();
}
