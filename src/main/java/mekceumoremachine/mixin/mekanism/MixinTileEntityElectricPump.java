package mekceumoremachine.mixin.mekanism;

import mekanism.api.Coord4D;
import mekanism.common.base.IRedstoneControl;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.component.TileComponentSecurity;
import mekanism.common.tile.component.TileComponentUpgrade;
import mekanism.common.tile.machine.TileEntityElectricPump;
import mekanism.common.tile.prefab.TileEntityElectricBlock;
import mekanism.common.upgrade.IUpgradeData;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.tile.interfaces.ITierFirstUpgrade;
import mekceumoremachine.common.upgrade.FirstElectricPumpUpgradeData;
import mekanism.common.capabilities.fluid.BasicFluidTank;
import net.minecraft.block.state.IBlockState;
import net.minecraftforge.fluids.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;

@Mixin(value = TileEntityElectricPump.class, remap = false)
public abstract class MixinTileEntityElectricPump extends TileEntityElectricBlock implements ITierFirstUpgrade {

    @Shadow
    public int operatingTicks;

    @Shadow
    public abstract IRedstoneControl.RedstoneControl getControlType();

    @Shadow
    public TileComponentUpgrade upgradeComponent;

    @Shadow
    public TileComponentSecurity securityComponent;

    @Shadow
    public BasicFluidTank fluidTank;

    @Shadow
    public Fluid activeType;

    @Shadow
    public Set<Coord4D> recurringNodes;

    public MixinTileEntityElectricPump(String name, double baseMaxEnergy) {
        super(name, baseMaxEnergy);
    }

    @Override
    public IBlockState getUpgradeResult(BaseTier upgradeTier) {
        return canInstallUpgrade(upgradeTier) ? MEKCeuMoreMachineBlocks.TierElectricPump.getDefaultState() : null;
    }

    @Override
    public IUpgradeData getUpgradeData(BaseTier upgradeTier) {
        return canInstallUpgrade(upgradeTier)
              ? new FirstElectricPumpUpgradeData(upgradeTier, this, operatingTicks, fluidTank, activeType, recurringNodes)
              : null;
    }


}
