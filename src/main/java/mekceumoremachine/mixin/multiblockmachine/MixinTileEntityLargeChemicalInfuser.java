package mekceumoremachine.mixin.multiblockmachine;

import mekanism.common.base.IUpgradeableTile;
import mekanism.common.block.states.BlockStateMachine.MachineType;
import mekanism.common.capabilities.gas.BasicGasTank;
import mekanism.common.recipe.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.common.recipe.inputs.ChemicalPairInput;
import mekanism.common.recipe.machines.ChemicalInfuserRecipe;
import mekanism.common.recipe.outputs.GasOutput;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.prefab.TileEntityBasicMachine;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.multiblockmachine.common.tile.machine.TileEntityLargeChemicalInfuser;
import mekceumoremachine.common.upgrade.LargeChemicalInfuserUpgradeData;
import mekceumoremachine.common.upgrade.LargeMachineUpgradeDataApplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(value = TileEntityLargeChemicalInfuser.class, remap = false)
public abstract class MixinTileEntityLargeChemicalInfuser extends TileEntityBasicMachine<ChemicalPairInput, GasOutput, ChemicalInfuserRecipe> implements IUpgradeableTile {

    @Shadow
    public BasicGasTank leftTank;

    @Shadow
    public BasicGasTank rightTank;

    @Shadow
    public BasicGasTank centerTank;

    @Shadow
    public double clientEnergyUsed;

    public MixinTileEntityLargeChemicalInfuser(String soundPath, MachineType type, int upgradeSlot, int baseTicksRequired, List<RecipeError> trackedErrorTypes) {
        super(soundPath, type, upgradeSlot, baseTicksRequired, trackedErrorTypes);
    }

    @Shadow
    public abstract void onPlace();

    @Override
    public boolean parseUpgradeData(IUpgradeData upgradeData) {
        if (!(upgradeData instanceof LargeChemicalInfuserUpgradeData data) || data.getUpgradeTier() != BaseTier.ULTIMATE) {
            return false;
        }
        onPlace();
        LargeMachineUpgradeDataApplier.applyCommon(this, data, upgradeComponent, securityComponent);
        clientEnergyUsed = data.clientEnergyUsed;
        prevEnergy = data.prevEnergy;
        leftTank.setGas(data.leftGas == null ? null : data.leftGas.copy());
        rightTank.setGas(data.rightGas == null ? null : data.rightGas.copy());
        centerTank.setGas(data.centerGas == null ? null : data.centerGas.copy());
        LargeMachineUpgradeDataApplier.finish(this, upgradeComponent);
        return true;
    }
}
