package mekceumoremachine.mixin.mekanism;

import mekanism.common.capabilities.gas.BasicGasTank;
import mekanism.common.base.IRedstoneControl;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.TileComponentSecurity;
import mekanism.common.tile.component.TileComponentUpgrade;
import mekanism.common.tile.machine.TileEntitySolarNeutronActivator;
import mekanism.common.tile.prefab.TileEntityContainerBlock;
import mekanism.common.upgrade.IUpgradeData;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.tile.interfaces.ITierFirstUpgrade;
import mekceumoremachine.common.upgrade.FirstSolarNeutronActivatorUpgradeData;
import net.minecraft.block.state.IBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TileEntitySolarNeutronActivator.class, remap = false)
public abstract class MixinTileEntitySolarNeutronActivator extends TileEntityContainerBlock implements ITierFirstUpgrade {

    @Shadow
    public BasicGasTank inputTank;

    @Shadow
    public BasicGasTank outputTank;

    @Shadow
    public TileComponentConfig configComponent;

    @Shadow
    public TileComponentSecurity securityComponent;

    @Shadow
    public TileComponentUpgrade upgradeComponent;

    @Shadow
    public abstract IRedstoneControl.RedstoneControl getControlType();

    @Shadow
    public TileComponentEjector ejectorComponent;

    @Shadow
    private boolean isActive;

    @Shadow
    public int operatingTicks;

    public MixinTileEntitySolarNeutronActivator(String name) {
        super(name);
    }


    public boolean isUpgrade = true;


    @Override
    public IBlockState getUpgradeResult(BaseTier upgradeTier) {
        return canInstallUpgrade(upgradeTier) ? MEKCeuMoreMachineBlocks.TierSolarNeutronActivator.getDefaultState() : null;
    }

    @Override
    public void prepareForUpgrade() {
        isUpgrade = false;
    }

    @Override
    public IUpgradeData getUpgradeData(BaseTier upgradeTier) {
        return canInstallUpgrade(upgradeTier)
              ? new FirstSolarNeutronActivatorUpgradeData(upgradeTier, this, operatingTicks, configComponent, ejectorComponent, inputTank, outputTank)
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

