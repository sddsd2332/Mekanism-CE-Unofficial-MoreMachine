package mekceumoremachine.common.upgrade;

import mekanism.api.gas.GasStack;
import mekanism.common.tier.BaseTier;
import mekceumoremachine.common.tile.machine.TileEntityTierChemicalInfuser;

import javax.annotation.Nonnull;

public class LargeChemicalInfuserUpgradeData extends LargeMachineUpgradeData {

    public final double clientEnergyUsed;
    public final double prevEnergy;
    public final GasStack leftGas;
    public final GasStack rightGas;
    public final GasStack centerGas;

    public LargeChemicalInfuserUpgradeData(@Nonnull BaseTier upgradeTier, @Nonnull TileEntityTierChemicalInfuser source) {
        super(upgradeTier, source);
        clientEnergyUsed = source.clientEnergyUsed;
        prevEnergy = source.prevEnergy;
        leftGas = source.leftTank.getGas() == null ? null : source.leftTank.getGas().copy();
        rightGas = source.rightTank.getGas() == null ? null : source.rightTank.getGas().copy();
        centerGas = source.centerTank.getGas() == null ? null : source.centerTank.getGas().copy();
    }
}
