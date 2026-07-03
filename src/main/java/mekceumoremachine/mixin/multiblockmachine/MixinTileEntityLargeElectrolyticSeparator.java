package mekceumoremachine.mixin.multiblockmachine;

import mekanism.common.base.IUpgradeableTile;
import mekanism.common.capabilities.fluid.BasicFluidTank;
import mekanism.common.capabilities.gas.BasicGasTank;
import mekanism.common.recipe.inputs.FluidInput;
import mekanism.common.recipe.machines.SeparatorRecipe;
import mekanism.common.recipe.outputs.ChemicalPairOutput;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.TileEntityGasTank.GasMode;
import mekanism.common.tile.prefab.TileEntityBasicMachine;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.multiblockmachine.common.tile.machine.TileEntityLargeElectrolyticSeparator;
import mekceumoremachine.common.upgrade.LargeElectrolyticSeparatorUpgradeData;
import mekceumoremachine.common.upgrade.LargeMachineUpgradeDataApplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TileEntityLargeElectrolyticSeparator.class, remap = false)
public abstract class MixinTileEntityLargeElectrolyticSeparator extends TileEntityBasicMachine<FluidInput, ChemicalPairOutput, SeparatorRecipe> implements IUpgradeableTile {

    @Shadow
    public BasicFluidTank fluidTank;

    @Shadow
    public BasicGasTank leftTank;

    @Shadow
    public BasicGasTank rightTank;

    @Shadow
    public GasMode dumpLeft;

    @Shadow
    public GasMode dumpRight;

    @Shadow
    public double clientEnergyUsed;

    public MixinTileEntityLargeElectrolyticSeparator(String soundPath, String name, double energyStorage, double energyUsage, int upgradeSlot, int baseTicksRequired) {
        super(soundPath, name, energyStorage, energyUsage, upgradeSlot, baseTicksRequired);
    }

    @Shadow
    public abstract void onPlace();

    @Override
    public boolean parseUpgradeData(IUpgradeData upgradeData) {
        if (!(upgradeData instanceof LargeElectrolyticSeparatorUpgradeData data) || data.getUpgradeTier() != BaseTier.ULTIMATE) {
            return false;
        }
        onPlace();
        LargeMachineUpgradeDataApplier.applyCommon(this, data, upgradeComponent, securityComponent);
        clientEnergyUsed = data.clientEnergyUsed;
        prevEnergy = data.prevEnergy;
        fluidTank.setFluid(data.fluid == null ? null : data.fluid.copy());
        leftTank.setGas(data.leftGas == null ? null : data.leftGas.copy());
        rightTank.setGas(data.rightGas == null ? null : data.rightGas.copy());
        dumpLeft = data.dumpLeft;
        dumpRight = data.dumpRight;
        LargeMachineUpgradeDataApplier.finish(this, upgradeComponent);
        return true;
    }
}
