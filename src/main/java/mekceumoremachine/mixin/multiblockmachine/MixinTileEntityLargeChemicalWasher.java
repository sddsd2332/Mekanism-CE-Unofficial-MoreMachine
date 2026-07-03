package mekceumoremachine.mixin.multiblockmachine;

import mekanism.common.base.IUpgradeableTile;
import mekanism.common.block.states.BlockStateMachine.MachineType;
import mekanism.common.capabilities.fluid.BasicFluidTank;
import mekanism.common.capabilities.gas.BasicGasTank;
import mekanism.common.recipe.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.common.recipe.inputs.GasAndFluidInput;
import mekanism.common.recipe.machines.WasherRecipe;
import mekanism.common.recipe.outputs.GasOutput;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.prefab.TileEntityBasicMachine;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.multiblockmachine.common.tile.machine.TileEntityLargeChemicalWasher;
import mekceumoremachine.common.upgrade.LargeChemicalWasherUpgradeData;
import mekceumoremachine.common.upgrade.LargeMachineUpgradeDataApplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(value = TileEntityLargeChemicalWasher.class, remap = false)
public abstract class MixinTileEntityLargeChemicalWasher extends TileEntityBasicMachine<GasAndFluidInput, GasOutput, WasherRecipe> implements IUpgradeableTile {

    @Shadow
    public BasicFluidTank fluidTank;

    @Shadow
    public BasicGasTank inputTank;

    @Shadow
    public BasicGasTank outputTank;

    @Shadow
    public double clientEnergyUsed;

    public MixinTileEntityLargeChemicalWasher(String soundPath, MachineType type, int upgradeSlot, int baseTicksRequired, List<RecipeError> trackedErrorTypes) {
        super(soundPath, type, upgradeSlot, baseTicksRequired, trackedErrorTypes);
    }

    @Shadow
    public abstract void onPlace();

    @Override
    public boolean parseUpgradeData(IUpgradeData upgradeData) {
        if (!(upgradeData instanceof LargeChemicalWasherUpgradeData data) || data.getUpgradeTier() != BaseTier.ULTIMATE) {
            return false;
        }
        onPlace();
        LargeMachineUpgradeDataApplier.applyCommon(this, data, upgradeComponent, securityComponent);
        clientEnergyUsed = data.clientEnergyUsed;
        prevEnergy = data.prevEnergy;
        fluidTank.setFluid(data.fluid == null ? null : data.fluid.copy());
        inputTank.setGas(data.inputGas == null ? null : data.inputGas.copy());
        outputTank.setGas(data.outputGas == null ? null : data.outputGas.copy());
        LargeMachineUpgradeDataApplier.finish(this, upgradeComponent);
        return true;
    }
}
