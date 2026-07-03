package mekceumoremachine.common.tile.interfaces;

import mekanism.common.base.IUpgradeableTile;
import mekanism.common.tier.BaseTier;
import mekanism.common.tier.ITier;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.upgrade.TierUpgradeData;

public interface INeedRepeatTierUpgrade<T extends ITier> extends IUpgradeableTile {

    T getNowTier();

    @Override
    default boolean canInstallUpgrade(BaseTier upgradeTier) {
        return upgradeTier != null && upgradeTier != BaseTier.CREATIVE && getNowTier().getBaseTier().ordinal() + 1 == upgradeTier.ordinal();
    }

    @Override
    default IUpgradeData getUpgradeData(BaseTier upgradeTier) {
        return canInstallUpgrade(upgradeTier) ? new TierUpgradeData(upgradeTier) : null;
    }

    @Override
    default boolean parseUpgradeData(IUpgradeData upgradeData) {
        return upgradeData instanceof TierUpgradeData data && applyRepeatedTierUpgrade(data.getUpgradeTier());
    }

    default boolean applyRepeatedTierUpgrade(BaseTier upgradeTier) {
        return false;
    }
}
