package mekceumoremachine.mixin.multiblockmachine;

import mekanism.common.base.IUpgradeableTile;
import mekanism.common.capabilities.gas.BasicGasTank;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.component.TileComponentSecurity;
import mekanism.common.tile.component.TileComponentUpgrade;
import mekanism.common.tile.prefab.TileEntityContainerBlock;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.multiblockmachine.common.tile.machine.TileEntityLargeSolarNeutronActivator;
import mekceumoremachine.common.upgrade.LargeMachineUpgradeDataApplier;
import mekceumoremachine.common.upgrade.LargeSolarNeutronActivatorUpgradeData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TileEntityLargeSolarNeutronActivator.class, remap = false)
public abstract class MixinTileEntityLargeSolarNeutronActivator extends TileEntityContainerBlock implements IUpgradeableTile {

    @Shadow
    public BasicGasTank inputTank;

    @Shadow
    public BasicGasTank outputTank;

    @Shadow
    public int operatingTicks;

    @Shadow
    public TileComponentUpgrade upgradeComponent;

    @Shadow
    public TileComponentSecurity securityComponent;

    public MixinTileEntityLargeSolarNeutronActivator(String name) {
        super(name);
    }

    @Shadow
    public abstract void onPlace();

    @Override
    public boolean parseUpgradeData(IUpgradeData upgradeData) {
        if (!(upgradeData instanceof LargeSolarNeutronActivatorUpgradeData data) || data.getUpgradeTier() != BaseTier.ULTIMATE) {
            return false;
        }
        onPlace();
        LargeMachineUpgradeDataApplier.applyCommon(this, data, upgradeComponent, securityComponent);
        operatingTicks = data.operatingTicks;
        inputTank.setGas(data.inputGas == null ? null : data.inputGas.copy());
        outputTank.setGas(data.outputGas == null ? null : data.outputGas.copy());
        LargeMachineUpgradeDataApplier.finish(this, upgradeComponent);
        return true;
    }
}
