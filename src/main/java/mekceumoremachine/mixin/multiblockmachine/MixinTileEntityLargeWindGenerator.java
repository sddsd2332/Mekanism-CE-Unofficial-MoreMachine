package mekceumoremachine.mixin.multiblockmachine;

import mekanism.common.base.IUpgradeableTile;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.component.TileComponentUpgrade;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.generators.common.tile.TileEntityGenerator;
import mekanism.multiblockmachine.common.tile.generator.TileEntityLargeWindGenerator;
import mekceumoremachine.common.upgrade.LargeMachineUpgradeDataApplier;
import mekceumoremachine.common.upgrade.LargeWindGeneratorUpgradeData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TileEntityLargeWindGenerator.class, remap = false)
public abstract class MixinTileEntityLargeWindGenerator extends TileEntityGenerator implements IUpgradeableTile {

    @Shadow
    public TileComponentUpgrade upgradeComponent;

    public MixinTileEntityLargeWindGenerator(String soundPath, String name, double maxEnergy, double out) {
        super(soundPath, name, maxEnergy, out);
    }

    @Shadow
    public abstract void onPlace();

    @Shadow
    public abstract void setAngle(double angle);

    @Override
    public boolean parseUpgradeData(IUpgradeData upgradeData) {
        if (!(upgradeData instanceof LargeWindGeneratorUpgradeData data) || data.getUpgradeTier() != BaseTier.ULTIMATE) {
            return false;
        }
        onPlace();
        LargeMachineUpgradeDataApplier.applyCommon(this, data, upgradeComponent, securityComponent);
        setAngle(data.angle);
        LargeMachineUpgradeDataApplier.finish(this, upgradeComponent);
        return true;
    }
}
