package mekceumoremachine.common.config;

import mekanism.common.config.BaseConfig;
import mekanism.common.config.options.BooleanOption;
import mekanism.common.config.options.DoubleOption;
import mekanism.common.config.options.IntOption;
import mekanism.common.config.options.StringListOption;
import mekanism.common.tier.InductionCellTier;
import mekanism.common.tier.InductionProviderTier;

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

    public DoubleOption BasicWirelessChargingMaxEnergy = new DoubleOption(this, "BasicWirelessChargingMaxEnergy", InductionCellTier.BASIC.getBaseMaxEnergy(), "Maximum stored electricity of the basic level of wireless transmission station and wireless power supply station");
    public DoubleOption AdvancedWirelessChargingMaxEnergy = new DoubleOption(this, "AdvancedWirelessChargingMaxEnergy", InductionCellTier.ADVANCED.getBaseMaxEnergy(), "Maximum stored electricity of advanced levels of wireless transmission stations and wireless power supply stations");
    public DoubleOption EliteWirelessChargingMAXEnergy = new DoubleOption(this, "EliteWirelessChargingMAXEnergy", InductionCellTier.ELITE.getBaseMaxEnergy(), "Maximum stored electricity of elite level wireless transmission stations and wireless power supply stations");
    public DoubleOption UltimateWirelessChargingMaxEnergy = new DoubleOption(this, "UltimateWirelessChargingMaxEnergy", InductionCellTier.ULTIMATE.getBaseMaxEnergy(), "Maximum stored electricity at the ultimate level of wireless transmission stations and wireless power supply stations");

    public DoubleOption BasicWirelessChargingOutput = new DoubleOption(this, "BasicWirelessChargingOutput", InductionProviderTier.BASIC.getBaseOutput(), "Maximum transmission power of the basic level of wireless transmission stations and wireless power supply stations");
    public DoubleOption AdvancedWirelessChargingOutput = new DoubleOption(this, "AdvancedWirelessChargingOutput", InductionProviderTier.ADVANCED.getBaseOutput(), "Maximum transmission power of advanced levels of wireless transmission stations and wireless power supply stations");
    public DoubleOption EliteWirelessChargingOutput = new DoubleOption(this, "EliteWirelessChargingOutput", InductionProviderTier.ELITE.getBaseOutput(), "Maximum transmission power of elite level wireless transmission stations and wireless power supply stations");
    public DoubleOption UltimateWirelessChargingOutput = new DoubleOption(this, "UltimateWirelessChargingOutput", InductionProviderTier.ULTIMATE.getBaseOutput(), "Maximum transmission power of the ultimate level of wireless transmission stations and wireless power supply stations");

    public IntOption BasicWirelessChargingLink = new IntOption(this, "BasicWirelessChargingLink", 300, "Maximum number of connections for the basic wireless power station", 1, Integer.MAX_VALUE);
    public IntOption AdvancedWirelessChargingLink = new IntOption(this, "AdvancedWirelessChargingLink", 600, "Maximum number of connections for the basic wireless power station", 1, Integer.MAX_VALUE);
    public IntOption EliteWirelessChargingLink = new IntOption(this, "EliteWirelessChargingLink", 700, "Maximum number of connections for the basic wireless power station", 1, Integer.MAX_VALUE);
    public IntOption UltimateWirelessChargingLink = new IntOption(this, "UltimateWirelessChargingLink", 900, "Maximum number of connections for the basic wireless power station", 1, Integer.MAX_VALUE);

    public StringListOption OreGenerationBlacklist = new StringListOption(this, "OreGenerationBlacklist", new String[]{}, "Types of minerals that the Void Mineral Generator cannot mine,For example: 'oreIron;");
    public BooleanOption OreGenerationBlacklistReversal = new BooleanOption(this, "OreGenerationBlacklistReversal", false, "Whether the blacklist of the Void Mineral Generator is reversed. If it is true, the Void Mineral Generator can only mine the types of minerals in the blacklist.");


    public IntOption VoidMineralGeneratorTick = new IntOption(this, "VoidMineralGeneratorTick", 200, "How many ticks does the Void Mineral Generator need to complete one operation?", 200, Integer.MAX_VALUE);
    public DoubleOption VoidMineralGeneratorEnergyUsage = new DoubleOption(this, "VoidMineralGeneratorEnergyUsage", 2500D, "Energy usage of the Void Mineral Generator");
    public DoubleOption VoidMineralGeneratorEnergyStorge = new DoubleOption(this, "VoidMineralGeneratorEnergyStorge", 50000000D, "Energy Storge of Void Mineral Generator");

    @Override
    public String getCategory() {
        return "moremachine";
    }
}
