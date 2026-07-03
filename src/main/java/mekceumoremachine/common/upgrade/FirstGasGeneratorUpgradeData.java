package mekceumoremachine.common.upgrade;

import mekanism.api.gas.GasStack;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.prefab.TileEntityContainerBlock;

import javax.annotation.Nonnull;

public class FirstGasGeneratorUpgradeData extends LargeMachineUpgradeData {

    public final int burnTicks;
    public final int maxBurnTicks;
    public final double generationRate;
    public final double clientUsed;
    public final GasStack fuel;

    public FirstGasGeneratorUpgradeData(@Nonnull BaseTier upgradeTier, @Nonnull TileEntityContainerBlock source, int burnTicks, int maxBurnTicks,
          double generationRate, double clientUsed, GasStack fuel) {
        super(upgradeTier, source);
        this.burnTicks = burnTicks;
        this.maxBurnTicks = maxBurnTicks;
        this.generationRate = generationRate;
        this.clientUsed = clientUsed;
        this.fuel = fuel == null ? null : fuel.copy();
    }
}
