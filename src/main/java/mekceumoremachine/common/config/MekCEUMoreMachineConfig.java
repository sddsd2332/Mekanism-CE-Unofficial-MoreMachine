package mekceumoremachine.common.config;

import mekanism.common.config.BaseConfig;
import mekanism.common.config.options.BooleanOption;
import mekanism.common.config.options.DoubleOption;
import mekanism.common.config.options.IntOption;
import mekanism.common.config.options.StringListOption;

public class MekCEUMoreMachineConfig extends BaseConfig {

    public DoubleOption ReplicatorItemStackEnergyUsage = new DoubleOption(this, "ReplicatorItemStackEnergyUsage", 200D, "Energy usage of the Item Replicator");
    public DoubleOption ReplicatorItemStackEnergyStorge = new DoubleOption(this, "ReplicatorItemStackEnergyStorge", 80000D, "Energy Storge of the Item Replicator");

    public DoubleOption ReplicatorGasesEnergyUsage = new DoubleOption(this, "ReplicatorGasesEnergyUsage", 200D, "Energy usage of the Gases Replicator");
    public DoubleOption ReplicatorGasesEnergyStorge = new DoubleOption(this, "ReplicatorGasesEnergyStorge", 80000D, "Energy Storge of the Gases Replicator");

    public DoubleOption ReplicatorFluidStackEnergyUsage = new DoubleOption(this, "ReplicatorFluidStackEnergyUsage", 200D, "Energy usage of the Fluid Replicator");
    public DoubleOption ReplicatorFluidStackEnergyStorge = new DoubleOption(this, "ReplicatorFluidStackEnergyStorge", 80000D, "Energy Storge of the Fluid Replicator");

    public BooleanOption enableEuWirelessRecharge = new BooleanOption(this, "enableICWirelessRecharge", true, "Allows machines of the EU series to be charged");

    public BooleanOption enableRFWirelessRecharge = new BooleanOption(this, "enableRFWirelessRecharge", true, "Allows machines of the RF series to be charged");

    public BooleanOption enableFEWirelessRecharge = new BooleanOption(this, "enableFEWirelessRecharge", true, "Allows machines of the FE series to be charged");

    public BooleanOption enableTeslaWirelessRecharge = new BooleanOption(this, "enableTeslaWirelessRecharge", true, "Allows machines of the Tesla series to be charged");

    public BooleanOption enableAutoClearErrorMachine = new BooleanOption(this, "enableAutoClearErrorMachine", true, "When the wireless power station detects the coordinates of an incorrect block, it will store them in a map and continuously check whether the block exists at those coordinates. If it does not exist, the error will be cleared.");

    public IntOption AutoClearErrorMachineSecond = new IntOption(this, "AutoClearErrorMachineSecond", 60, "How many seconds to clear an error", 1, Integer.MAX_VALUE / 20);

    public StringListOption BlacklistMachine = new StringListOption(this, "BlacklistMachine", new String[]{
            "nc.tile.TileBin",
            "com.supermartijn642.trashcans",
            "com.rwtema.extrautils2.tile.TileTrashCanEnergy",
            "mctmods.immersivetechnology.common.blocks.metal.tileentities.TileEntityTrashEnergy",
            "cassiokf.industrialrenewal.tileentity.TileEntityTrash",
            "sonar.fluxnetworks.common.tileentity.TileFluxPlug"
    }, "Wireless Charging Station Blacklist,The machines in here do not charge.");

    @Override
    public String getCategory() {
        return "moremachine";
    }
}
