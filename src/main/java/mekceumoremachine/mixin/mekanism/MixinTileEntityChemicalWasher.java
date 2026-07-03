package mekceumoremachine.mixin.mekanism;

import mekanism.common.capabilities.fluid.BasicFluidTank;
import mekanism.common.capabilities.gas.BasicGasTank;
import mekanism.common.block.states.BlockStateMachine;
import mekanism.common.recipe.inputs.GasAndFluidInput;
import mekanism.common.recipe.machines.WasherRecipe;
import mekanism.common.recipe.outputs.GasOutput;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.machine.TileEntityChemicalWasher;
import mekanism.common.tile.prefab.TileEntityBasicMachine;
import mekanism.common.upgrade.IUpgradeData;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.tile.interfaces.ITierFirstUpgrade;
import mekceumoremachine.common.upgrade.FirstChemicalWasherUpgradeData;
import net.minecraft.block.state.IBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TileEntityChemicalWasher.class, remap = false)
public abstract class MixinTileEntityChemicalWasher extends TileEntityBasicMachine<GasAndFluidInput, GasOutput, WasherRecipe> implements ITierFirstUpgrade {
    @Shadow
    public double clientEnergyUsed;

    @Shadow
    public BasicFluidTank fluidTank;

    @Shadow
    public BasicGasTank inputTank;

    @Shadow
    public BasicGasTank outputTank;

    public MixinTileEntityChemicalWasher(String soundPath, BlockStateMachine.MachineType type, int upgradeSlot, int baseTicksRequired) {
        super(soundPath, type, upgradeSlot, baseTicksRequired);
    }

    public boolean isUpgrade = true;

    @Override
    public IBlockState getUpgradeResult(BaseTier upgradeTier) {
        return canInstallUpgrade(upgradeTier) ? MEKCeuMoreMachineBlocks.TierChemicalWasher.getDefaultState() : null;
    }

    @Override
    public void prepareForUpgrade() {
        isUpgrade = false;
    }

    @Override
    public IUpgradeData getUpgradeData(BaseTier upgradeTier) {
        return canInstallUpgrade(upgradeTier)
              ? new FirstChemicalWasherUpgradeData(upgradeTier, this, clientEnergyUsed, prevEnergy, configComponent, ejectorComponent,
                    fluidTank, inputTank, outputTank)
              : null;
    }

    /**
     * @author sddsd2332
     * @reason 取消在升级的时候进行辐射判断
     */
    @Override
    public boolean shouldDumpRadiation() {
        return isUpgrade;
    }
}
