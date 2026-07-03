package mekceumoremachine.common.upgrade;

import mekanism.api.gas.GasStack;
import mekanism.common.tier.BaseTier;
import mekceumoremachine.common.tile.machine.TileEntityTierSolarNeutronActivator;

import javax.annotation.Nonnull;

public class LargeSolarNeutronActivatorUpgradeData extends LargeMachineUpgradeData {

    public final int operatingTicks;
    public final GasStack inputGas;
    public final GasStack outputGas;

    public LargeSolarNeutronActivatorUpgradeData(@Nonnull BaseTier upgradeTier, @Nonnull TileEntityTierSolarNeutronActivator source) {
        super(upgradeTier, source);
        operatingTicks = source.operatingTicks;
        inputGas = source.inputTank.getGas() == null ? null : source.inputTank.getGas().copy();
        outputGas = source.outputTank.getGas() == null ? null : source.outputTank.getGas().copy();
    }
}
