package mekceumoremachine.mixin.mekanism;

import mekanism.common.block.states.BlockStateMachine;
import mekanism.common.capabilities.gas.BasicGasTank;
import mekanism.common.recipe.inputs.ItemStackInput;
import mekanism.common.recipe.machines.OxidationRecipe;
import mekanism.common.recipe.outputs.GasOutput;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.machine.TileEntityChemicalOxidizer;
import mekanism.common.tile.prefab.TileEntityBasicMachine;
import mekanism.common.upgrade.IUpgradeData;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.tile.interfaces.ITierFirstUpgrade;
import mekceumoremachine.common.upgrade.FirstChemicalOxidizerUpgradeData;
import net.minecraft.block.state.IBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = TileEntityChemicalOxidizer.class, remap = false)
public abstract class MixinTileEntityChemicalOxidizer extends TileEntityBasicMachine<ItemStackInput, GasOutput, OxidationRecipe>
      implements ITierFirstUpgrade {

    @Shadow
    public BasicGasTank gasTank;

    @Unique
    private boolean mekceuMoreMachine$isUpgrade = true;

    public MixinTileEntityChemicalOxidizer(String soundPath, BlockStateMachine.MachineType type, int upgradeSlot, int baseTicksRequired) {
        super(soundPath, type, upgradeSlot, baseTicksRequired);
    }

    @Override
    public IBlockState getUpgradeResult(BaseTier upgradeTier) {
        return canInstallUpgrade(upgradeTier) ? MEKCeuMoreMachineBlocks.TierChemicalOxidizer.getStateFromMeta(0) : null;
    }

    @Override
    public void prepareForUpgrade() {
        mekceuMoreMachine$isUpgrade = false;
    }

    @Override
    public IUpgradeData getUpgradeData(BaseTier upgradeTier) {
        return canInstallUpgrade(upgradeTier)
              ? new FirstChemicalOxidizerUpgradeData(upgradeTier, this, prevEnergy, operatingTicks, configComponent, ejectorComponent, gasTank)
              : null;
    }

    @Override
    public boolean shouldDumpRadiation() {
        return mekceuMoreMachine$isUpgrade;
    }
}
