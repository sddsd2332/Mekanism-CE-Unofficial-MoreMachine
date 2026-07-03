package mekceumoremachine.mixin.mekanism;

import mekanism.common.capabilities.gas.BasicGasTank;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.TileEntityRadioactiveWasteBarrel;
import mekanism.common.tile.prefab.TileEntityBasicBlock;
import mekanism.common.upgrade.IUpgradeData;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.tile.interfaces.ITierFirstUpgrade;
import mekceumoremachine.common.upgrade.FirstRadioactiveWasteBarrelUpgradeData;
import net.minecraft.block.state.IBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TileEntityRadioactiveWasteBarrel.class, remap = false)
public class MixinTileEntityRadioactiveWasteBarrel extends TileEntityBasicBlock implements ITierFirstUpgrade {

    @Shadow
    public boolean isActive;
    @Shadow
    public boolean clientActive;
    @Shadow
    public BasicGasTank gasTank;
    public boolean isUpgrade = true;

    @Override
    public IBlockState getUpgradeResult(BaseTier upgradeTier) {
        return canInstallUpgrade(upgradeTier) ? MEKCeuMoreMachineBlocks.TierRadioactiveWasteBarrel.getDefaultState() : null;
    }

    @Override
    public void prepareForUpgrade() {
        isUpgrade = false;
    }

    @Override
    public IUpgradeData getUpgradeData(BaseTier upgradeTier) {
        return canInstallUpgrade(upgradeTier)
              ? new FirstRadioactiveWasteBarrelUpgradeData(upgradeTier, (TileEntityRadioactiveWasteBarrel) (Object) this, gasTank)
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
