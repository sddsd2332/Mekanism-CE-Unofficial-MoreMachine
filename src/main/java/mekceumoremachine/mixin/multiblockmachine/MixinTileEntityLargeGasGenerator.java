package mekceumoremachine.mixin.multiblockmachine;

import mekanism.common.base.IUpgradeableTile;
import mekanism.common.capabilities.gas.BasicGasTank;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.component.TileComponentUpgrade;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.generators.common.tile.TileEntityGenerator;
import mekanism.multiblockmachine.common.tile.generator.TileEntityLargeGasGenerator;
import mekceumoremachine.common.upgrade.LargeGasGeneratorUpgradeData;
import mekceumoremachine.common.upgrade.LargeMachineUpgradeDataApplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TileEntityLargeGasGenerator.class, remap = false)
public abstract class MixinTileEntityLargeGasGenerator extends TileEntityGenerator implements IUpgradeableTile {

    @Shadow
    public BasicGasTank fuelTank;

    @Shadow
    public int burnTicks;

    @Shadow
    public int maxBurnTicks;

    @Shadow
    public double generationRate;

    @Shadow
    public double clientUsed;

    @Shadow
    public TileComponentUpgrade upgradeComponent;

    public MixinTileEntityLargeGasGenerator(String soundPath, String name, double maxEnergy, double out) {
        super(soundPath, name, maxEnergy, out);
    }

    @Shadow
    public abstract void onPlace();

    @Override
    public boolean parseUpgradeData(IUpgradeData upgradeData) {
        if (!(upgradeData instanceof LargeGasGeneratorUpgradeData data) || data.getUpgradeTier() != BaseTier.ULTIMATE) {
            return false;
        }
        onPlace();
        LargeMachineUpgradeDataApplier.applyCommon(this, data, upgradeComponent, securityComponent);
        burnTicks = data.burnTicks;
        maxBurnTicks = data.maxBurnTicks;
        generationRate = data.generationRate;
        clientUsed = data.clientUsed;
        fuelTank.setGas(data.fuel == null ? null : data.fuel.copy());
        LargeMachineUpgradeDataApplier.finish(this, upgradeComponent);
        return true;
    }
}
