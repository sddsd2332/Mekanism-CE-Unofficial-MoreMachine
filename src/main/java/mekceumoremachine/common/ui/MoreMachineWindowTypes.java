package mekceumoremachine.common.ui;

import mekanism.common.config.MekanismConfig;
import mekanism.common.inventory.container.SelectedWindowData.WindowType;
import mekceumoremachine.common.MEKCeuMoreMachine;
import net.minecraft.util.ResourceLocation;

public final class MoreMachineWindowTypes {

    public static final WindowType WIRELESS_CONNECTION_CONFIG = WindowType.register(
          new ResourceLocation(MEKCeuMoreMachine.MODID, "wireless_connection_config"), "wireless_connection_config", true);
    public static final WindowType ORGANIC_FARM_OUTPUT = WindowType.register(
          new ResourceLocation(MEKCeuMoreMachine.MODID, "organic_farm_output"), "organic_farm_output", true);

    private MoreMachineWindowTypes() {
    }

    public static void init() {
        MekanismConfig.local().client.registerWindowType(WIRELESS_CONNECTION_CONFIG);
        MekanismConfig.local().client.registerWindowType(ORGANIC_FARM_OUTPUT);
    }
}
