package mekceumoremachine.mixin.mekanism;

import mekanism.common.capabilities.gas.BasicGasTank;
import mekanism.common.block.states.BlockStateMachine;
import mekanism.common.recipe.inputs.GasInput;
import mekanism.common.recipe.machines.IsotopicRecipe;
import mekanism.common.recipe.outputs.GasOutput;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.machine.TileEntityIsotopicCentrifuge;
import mekanism.common.tile.prefab.TileEntityBasicMachine;
import mekanism.common.upgrade.IUpgradeData;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.tile.interfaces.ITierFirstUpgrade;
import mekceumoremachine.common.upgrade.FirstIsotopicCentrifugeUpgradeData;
import net.minecraft.block.state.IBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TileEntityIsotopicCentrifuge.class, remap = false)
public abstract class MixinTileEntityIsotopicCentrifuge extends TileEntityBasicMachine<GasInput, GasOutput, IsotopicRecipe> implements ITierFirstUpgrade {

    @Shadow
    public BasicGasTank inputTank;

    @Shadow
    public BasicGasTank outputTank;

    @Shadow
    public double clientEnergyUsed;

    public MixinTileEntityIsotopicCentrifuge(String soundPath, BlockStateMachine.MachineType type, int upgradeSlot, int baseTicksRequired) {
        super(soundPath, type, upgradeSlot, baseTicksRequired);
    }


    public boolean isUpgrade = true;

    @Override
    public IBlockState getUpgradeResult(BaseTier upgradeTier) {
        return canInstallUpgrade(upgradeTier) ? MEKCeuMoreMachineBlocks.TierIsotopicCentrifuge.getDefaultState() : null;
    }

    @Override
    public void prepareForUpgrade() {
        isUpgrade = false;
    }

    @Override
    public IUpgradeData getUpgradeData(BaseTier upgradeTier) {
        return canInstallUpgrade(upgradeTier)
              ? new FirstIsotopicCentrifugeUpgradeData(upgradeTier, this, clientEnergyUsed, prevEnergy, operatingTicks, configComponent,
                    ejectorComponent, inputTank, outputTank)
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
