package mekceumoremachine.common.tile.interfaces;

import mekanism.common.base.IUpgradeableTile;
import mekanism.common.tier.BaseTier;
import mekanism.common.tier.ITier;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.upgrade.TierUpgradeData;

public interface ITierMachine<T extends ITier> extends IUpgradeableTile {

    T getTier();

    @Override
    default boolean canInstallUpgrade(BaseTier upgradeTier) {
        return upgradeTier != null && upgradeTier != BaseTier.CREATIVE && getTier().getBaseTier().ordinal() + 1 == upgradeTier.ordinal();
    }

    @Override
    default IUpgradeData getUpgradeData(BaseTier upgradeTier) {
        return canInstallUpgrade(upgradeTier) ? new TierUpgradeData(upgradeTier) : null;
    }

    @Override
    default boolean parseUpgradeData(IUpgradeData upgradeData) {
        return upgradeData instanceof TierUpgradeData data && applyTierUpgrade(data.getUpgradeTier());
    }

    default boolean applyTierUpgrade(BaseTier upgradeTier) {
        return false;
    }

}
