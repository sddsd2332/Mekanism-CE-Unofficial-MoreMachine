package mekceumoremachine.common.tile.interfaces;

import mekanism.common.base.IUpgradeableTile;
import mekanism.common.tier.BaseTier;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.upgrade.TierUpgradeData;
import mekanism.common.util.UpgradeUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;

public interface ITierFirstUpgrade extends IUpgradeableTile {

    @Override
    default boolean canInstallUpgrade(BaseTier upgradeTier) {
        return upgradeTier == BaseTier.BASIC;
    }

    @Override
    default IUpgradeData getUpgradeData(BaseTier upgradeTier) {
        if (!canInstallUpgrade(upgradeTier)) {
            return null;
        }
        return new TierUpgradeData(upgradeTier);
    }

    @Override
    default boolean parseUpgradeData(IUpgradeData upgradeData) {
        return upgradeData instanceof TierUpgradeData data && data.getUpgradeTier() == BaseTier.BASIC && applyFirstTierUpgrade();
    }

    default boolean applyFirstTierUpgrade() {
        if (!(this instanceof TileEntity tile)) {
            return false;
        }
        IUpgradeData upgradeData = getUpgradeData(BaseTier.BASIC);
        IBlockState upgradeResult = getUpgradeResult(BaseTier.BASIC);
        return upgradeData != null && upgradeResult != null && UpgradeUtils.replaceTileForUpgrade(tile, upgradeResult, upgradeData);
    }

}
