package mekceumoremachine.common.registries;

import mekanism.api.gas.Gas;
import mekanism.api.gas.GasRegistry;

public class MEKCeuMoreMachineFluids {

    public static final Gas UU_MATTER = new Gas("uu_matter", 0xFF530570);

    public static void register() {
        GasRegistry.register(UU_MATTER);
    }


}
