package mekceumoremachine.common.upgrade;

import mekanism.api.gas.GasStack;
import mekanism.common.tier.BaseTier;
import mekceumoremachine.common.tile.machine.TileEntityTierChemicalWasher;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;

public class LargeChemicalWasherUpgradeData extends LargeMachineUpgradeData {

    public final double clientEnergyUsed;
    public final double prevEnergy;
    public final FluidStack fluid;
    public final GasStack inputGas;
    public final GasStack outputGas;

    public LargeChemicalWasherUpgradeData(@Nonnull BaseTier upgradeTier, @Nonnull TileEntityTierChemicalWasher source) {
        super(upgradeTier, source);
        clientEnergyUsed = source.clientEnergyUsed;
        prevEnergy = source.prevEnergy;
        fluid = source.fluidTank.getFluid() == null ? null : source.fluidTank.getFluid().copy();
        inputGas = source.inputTank.getGas() == null ? null : source.inputTank.getGas().copy();
        outputGas = source.outputTank.getGas() == null ? null : source.outputTank.getGas().copy();
    }
}
