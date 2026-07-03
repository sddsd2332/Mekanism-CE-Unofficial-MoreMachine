package mekceumoremachine.common.upgrade;

import mekanism.api.gas.GasStack;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.TileEntityGasTank.GasMode;
import mekceumoremachine.common.tile.machine.TileEntityTierElectrolyticSeparator;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;

public class LargeElectrolyticSeparatorUpgradeData extends LargeMachineUpgradeData {

    public final double clientEnergyUsed;
    public final double prevEnergy;
    public final FluidStack fluid;
    public final GasStack leftGas;
    public final GasStack rightGas;
    public final GasMode dumpLeft;
    public final GasMode dumpRight;

    public LargeElectrolyticSeparatorUpgradeData(@Nonnull BaseTier upgradeTier, @Nonnull TileEntityTierElectrolyticSeparator source) {
        super(upgradeTier, source);
        clientEnergyUsed = source.clientEnergyUsed;
        prevEnergy = source.prevEnergy;
        fluid = source.fluidTank.getFluid() == null ? null : source.fluidTank.getFluid().copy();
        leftGas = source.leftTank.getGas() == null ? null : source.leftTank.getGas().copy();
        rightGas = source.rightTank.getGas() == null ? null : source.rightTank.getGas().copy();
        dumpLeft = source.dumpLeft;
        dumpRight = source.dumpRight;
    }
}
