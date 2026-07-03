package mekceumoremachine.mixin.mekanism;

import mekanism.common.capabilities.gas.BasicGasTank;
import mekanism.common.block.states.BlockStateMachine;
import mekanism.common.recipe.inputs.ItemStackInput;
import mekanism.common.recipe.machines.DissolutionRecipe;
import mekanism.common.recipe.outputs.GasOutput;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.machine.TileEntityChemicalDissolutionChamber;
import mekanism.common.tile.prefab.TileEntityBasicMachine;
import mekanism.common.upgrade.IUpgradeData;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.tile.interfaces.ITierFirstUpgrade;
import mekceumoremachine.common.upgrade.FirstChemicalDissolutionChamberUpgradeData;
import net.minecraft.block.state.IBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = TileEntityChemicalDissolutionChamber.class, remap = false)
public abstract class MixinTileEntityChemicalDissolutionChamber extends TileEntityBasicMachine<ItemStackInput, GasOutput, DissolutionRecipe> implements ITierFirstUpgrade {

    @Shadow
    public BasicGasTank injectTank;

    @Shadow
    public BasicGasTank outputTank;

    public MixinTileEntityChemicalDissolutionChamber(String soundPath, BlockStateMachine.MachineType type, int upgradeSlot, int baseTicksRequired) {
        super(soundPath, type, upgradeSlot, baseTicksRequired);
    }

    @Unique
    public boolean isUpgrade = true;


    @Override
    public IBlockState getUpgradeResult(BaseTier upgradeTier) {
        return canInstallUpgrade(upgradeTier) ? MEKCeuMoreMachineBlocks.TierChemicalDissolutionChamber.getStateFromMeta(0) : null;
    }

    @Override
    public void prepareForUpgrade() {
        isUpgrade = false;
    }

    @Override
    public IUpgradeData getUpgradeData(BaseTier upgradeTier) {
        return canInstallUpgrade(upgradeTier)
              ? new FirstChemicalDissolutionChamberUpgradeData(upgradeTier, this, prevEnergy, operatingTicks, configComponent,
                    ejectorComponent, injectTank, outputTank)
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
