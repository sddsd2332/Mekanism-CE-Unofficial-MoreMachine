package mekceumoremachine.common.config;


public class MoreMachineConfig {

    private static MoreMachineConfig LOCAL = new MoreMachineConfig();
    private static MoreMachineConfig SERVER = null;

    public static MoreMachineConfig current() {
        return SERVER != null ? SERVER : LOCAL;
    }

    public static MoreMachineConfig local() {
        return LOCAL;
    }

    public MekCEUMoreMachineConfig config = new MekCEUMoreMachineConfig();
}
