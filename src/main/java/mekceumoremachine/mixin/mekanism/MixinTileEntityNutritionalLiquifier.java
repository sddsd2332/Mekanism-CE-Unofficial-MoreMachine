package mekceumoremachine.mixin.mekanism;

import mekanism.common.capabilities.gas.BasicGasTank;
import mekanism.common.recipe.inputs.ItemStackInput;
import mekanism.common.recipe.machines.NutritionalRecipe;
import mekanism.common.recipe.outputs.GasOutput;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.machine.TileEntityNutritionalLiquifier;
import mekanism.common.tile.prefab.TileEntityBasicMachine;
import mekanism.common.upgrade.IUpgradeData;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.tile.interfaces.ITierFirstUpgrade;
import mekceumoremachine.common.upgrade.FirstNutritionalLiquifierUpgradeData;
import net.minecraft.block.state.IBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TileEntityNutritionalLiquifier.class, remap = false)
public abstract class MixinTileEntityNutritionalLiquifier extends TileEntityBasicMachine<ItemStackInput, GasOutput, NutritionalRecipe> implements ITierFirstUpgrade {

    @Shadow
    public BasicGasTank gasTank;

    public MixinTileEntityNutritionalLiquifier(String soundPath, String name, double energyStorge, double energUsage, int upgradeSlot, int baseTicksRequired) {
        super(soundPath, name, energyStorge, energUsage, upgradeSlot, baseTicksRequired);
    }

    public boolean isUpgrade = true;

    @Override
    public IBlockState getUpgradeResult(BaseTier upgradeTier) {
        return canInstallUpgrade(upgradeTier) ? MEKCeuMoreMachineBlocks.TierNutritionalLiquifier.getStateFromMeta(0) : null;
    }

    @Override
    public void prepareForUpgrade() {
        isUpgrade = false;
    }

    @Override
    public IUpgradeData getUpgradeData(BaseTier upgradeTier) {
        return canInstallUpgrade(upgradeTier)
              ? new FirstNutritionalLiquifierUpgradeData(upgradeTier, this, prevEnergy, operatingTicks, configComponent, ejectorComponent, gasTank)
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
