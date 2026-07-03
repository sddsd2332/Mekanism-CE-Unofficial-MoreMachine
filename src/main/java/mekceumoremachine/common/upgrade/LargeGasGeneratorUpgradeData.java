package mekceumoremachine.common.upgrade;

import mekanism.api.gas.GasStack;
import mekanism.common.tier.BaseTier;
import mekceumoremachine.common.tile.generator.TileEntityTierGasGenerator;

import javax.annotation.Nonnull;

public class LargeGasGeneratorUpgradeData extends LargeMachineUpgradeData {

    public final int burnTicks;
    public final int maxBurnTicks;
    public final double generationRate;
    public final double clientUsed;
    public final GasStack fuel;

    public LargeGasGeneratorUpgradeData(@Nonnull BaseTier upgradeTier, @Nonnull TileEntityTierGasGenerator source) {
        super(upgradeTier, source);
        burnTicks = source.burnTicks;
        maxBurnTicks = source.maxBurnTicks;
        generationRate = source.generationRate;
        clientUsed = source.clientUsed;
        fuel = source.fuelTank.getGas() == null ? null : source.fuelTank.getGas().copy();
    }
}
