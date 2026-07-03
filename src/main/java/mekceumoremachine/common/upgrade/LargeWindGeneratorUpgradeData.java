package mekceumoremachine.common.upgrade;

import mekanism.common.tier.BaseTier;
import mekceumoremachine.common.tile.generator.TileEntityTierWindGenerator;

import javax.annotation.Nonnull;

public class LargeWindGeneratorUpgradeData extends LargeMachineUpgradeData {

    public final double angle;

    public LargeWindGeneratorUpgradeData(@Nonnull BaseTier upgradeTier, @Nonnull TileEntityTierWindGenerator source) {
        super(upgradeTier, source);
        angle = source.getAngle();
    }
}
