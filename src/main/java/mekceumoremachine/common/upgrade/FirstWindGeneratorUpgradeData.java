package mekceumoremachine.common.upgrade;

import mekanism.common.tier.BaseTier;
import mekanism.common.tile.prefab.TileEntityContainerBlock;

import javax.annotation.Nonnull;

public class FirstWindGeneratorUpgradeData extends LargeMachineUpgradeData {

    public final double angle;

    public FirstWindGeneratorUpgradeData(@Nonnull BaseTier upgradeTier, @Nonnull TileEntityContainerBlock source, double angle) {
        super(upgradeTier, source);
        this.angle = angle;
    }
}
