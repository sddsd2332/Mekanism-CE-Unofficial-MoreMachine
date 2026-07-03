package mekceumoremachine.common.upgrade;

import mekanism.api.Coord4D;
import mekanism.common.capabilities.fluid.BasicFluidTank;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.prefab.TileEntityContainerBlock;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

public class FirstElectricPumpUpgradeData extends LargeMachineUpgradeData {

    public final int operatingTicks;
    public final FluidStack fluid;
    public final Fluid activeType;
    public final Set<Coord4D> recurringNodes;

    public FirstElectricPumpUpgradeData(@Nonnull BaseTier upgradeTier, @Nonnull TileEntityContainerBlock source,
          int operatingTicks, BasicFluidTank fluidTank, Fluid activeType, Set<Coord4D> recurringNodes) {
        super(upgradeTier, source);
        this.operatingTicks = operatingTicks;
        fluid = fluidTank.getFluid() == null ? null : fluidTank.getFluid().copy();
        this.activeType = activeType;
        this.recurringNodes = copyRecurringNodes(recurringNodes);
    }

    private static Set<Coord4D> copyRecurringNodes(Set<Coord4D> source) {
        Set<Coord4D> copy = new HashSet<>();
        for (Coord4D node : source) {
            copy.add(new Coord4D(node.x, node.y, node.z, node.dimensionId));
        }
        return copy;
    }
}
