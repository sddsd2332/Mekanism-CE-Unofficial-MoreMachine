package mekceumoremachine.mixin.mekanism;

import mekanism.common.capabilities.gas.BasicGasTank;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.machine.TileEntityAmbientAccumulatorEnergy;
import mekanism.common.tile.prefab.TileEntityMachine;
import mekanism.common.upgrade.IUpgradeData;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.tile.interfaces.ITierFirstUpgrade;
import mekceumoremachine.common.upgrade.FirstAmbientAccumulatorUpgradeData;
import net.minecraft.block.state.IBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TileEntityAmbientAccumulatorEnergy.class, remap = false)
public abstract class MixinTileEntityAmbientAccumulatorEnergy extends TileEntityMachine implements ITierFirstUpgrade {

    @Shadow
    public double clientEnergyUsed;

    @Shadow
    public TileComponentEjector ejectorComponent;

    @Shadow
    public TileComponentConfig configComponent;

    @Shadow
    public BasicGasTank outputTank;

    public MixinTileEntityAmbientAccumulatorEnergy(String sound, String name, double energyStorge, double energUsage, int upgradeSlot) {
        super(sound, name, energyStorge, energUsage, upgradeSlot);
    }

    public boolean isUpgrade = true;

    @Override
    public IBlockState getUpgradeResult(BaseTier upgradeTier) {
        return canInstallUpgrade(upgradeTier) ? MEKCeuMoreMachineBlocks.TierAmbientAccumulator.getDefaultState() : null;
    }

    @Override
    public void prepareForUpgrade() {
        isUpgrade = false;
    }

    @Override
    public IUpgradeData getUpgradeData(BaseTier upgradeTier) {
        return canInstallUpgrade(upgradeTier)
              ? new FirstAmbientAccumulatorUpgradeData(upgradeTier, this, clientEnergyUsed, prevEnergy, configComponent, ejectorComponent, outputTank)
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
