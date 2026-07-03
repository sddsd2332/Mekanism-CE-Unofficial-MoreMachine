package mekceumoremachine.mixin.mekanism;

import mekanism.common.capabilities.fluid.BasicFluidTank;
import mekanism.common.capabilities.gas.BasicGasTank;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.machine.TileEntityRotaryCondensentrator;
import mekanism.common.tile.prefab.TileEntityMachine;
import mekanism.common.upgrade.IUpgradeData;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.tile.interfaces.ITierFirstUpgrade;
import mekceumoremachine.common.upgrade.FirstRotaryCondensentratorUpgradeData;
import net.minecraft.block.state.IBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TileEntityRotaryCondensentrator.class, remap = false)
public abstract class MixinTileEntityRotaryCondensentrator extends TileEntityMachine implements ITierFirstUpgrade {


    @Shadow
    public TileComponentEjector ejectorComponent;

    @Shadow
    public TileComponentConfig configComponent;

    @Shadow
    public BasicGasTank gasTank;

    @Shadow
    public BasicFluidTank fluidTank;

    @Shadow
    public int mode;

    @Shadow
    public double clientEnergyUsed;

    public MixinTileEntityRotaryCondensentrator(String sound, String name, double energyStorge, double energUsage, int upgradeSlot) {
        super(sound, name, energyStorge, energUsage, upgradeSlot);
    }

    public boolean isUpgrade = true;

    @Override
    public IBlockState getUpgradeResult(BaseTier upgradeTier) {
        return canInstallUpgrade(upgradeTier) ? MEKCeuMoreMachineBlocks.TierRotaryCondensentrator.getDefaultState() : null;
    }

    @Override
    public void prepareForUpgrade() {
        isUpgrade = false;
    }

    @Override
    public IUpgradeData getUpgradeData(BaseTier upgradeTier) {
        return canInstallUpgrade(upgradeTier)
              ? new FirstRotaryCondensentratorUpgradeData(upgradeTier, this, clientEnergyUsed, prevEnergy, configComponent, ejectorComponent,
                    gasTank, fluidTank, mode)
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
