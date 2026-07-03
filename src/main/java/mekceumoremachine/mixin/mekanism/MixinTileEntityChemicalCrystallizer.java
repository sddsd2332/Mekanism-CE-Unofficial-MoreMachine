package mekceumoremachine.mixin.mekanism;

import mekanism.common.block.states.BlockStateMachine;
import mekanism.common.capabilities.gas.BasicGasTank;
import mekanism.common.recipe.inputs.GasInput;
import mekanism.common.recipe.machines.CrystallizerRecipe;
import mekanism.common.recipe.outputs.ItemStackOutput;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.machine.TileEntityChemicalCrystallizer;
import mekanism.common.tile.prefab.TileEntityBasicMachine;
import mekanism.common.upgrade.IUpgradeData;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.tile.interfaces.ITierFirstUpgrade;
import mekceumoremachine.common.upgrade.FirstChemicalCrystallizerUpgradeData;
import net.minecraft.block.state.IBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TileEntityChemicalCrystallizer.class, remap = false)
public abstract class MixinTileEntityChemicalCrystallizer extends TileEntityBasicMachine<GasInput, ItemStackOutput, CrystallizerRecipe>
      implements ITierFirstUpgrade {

    @Shadow
    public BasicGasTank inputTank;

    public MixinTileEntityChemicalCrystallizer(String soundPath, BlockStateMachine.MachineType type, int upgradeSlot, int baseTicksRequired) {
        super(soundPath, type, upgradeSlot, baseTicksRequired);
    }

    public boolean isUpgrade = true;

    @Override
    public IBlockState getUpgradeResult(BaseTier upgradeTier) {
        return canInstallUpgrade(upgradeTier) ? MEKCeuMoreMachineBlocks.TierChemicalCrystallizer.getStateFromMeta(0) : null;
    }

    @Override
    public void prepareForUpgrade() {
        isUpgrade = false;
    }

    @Override
    public IUpgradeData getUpgradeData(BaseTier upgradeTier) {
        return canInstallUpgrade(upgradeTier)
              ? new FirstChemicalCrystallizerUpgradeData(upgradeTier, this, prevEnergy, operatingTicks, configComponent, ejectorComponent, inputTank)
              : null;
    }

    @Override
    public boolean shouldDumpRadiation() {
        return isUpgrade;
    }
}
