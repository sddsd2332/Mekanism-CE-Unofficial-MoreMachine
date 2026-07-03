package mekceumoremachine.common.upgrade;

import mekanism.api.gas.GasStack;
import mekanism.common.capabilities.gas.BasicGasTank;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.TileEntityRadioactiveWasteBarrel;

import javax.annotation.Nonnull;

public class FirstRadioactiveWasteBarrelUpgradeData extends LargeMachineUpgradeData {

    public final GasStack gas;

    public FirstRadioactiveWasteBarrelUpgradeData(@Nonnull BaseTier upgradeTier, @Nonnull TileEntityRadioactiveWasteBarrel source,
          BasicGasTank gasTank) {
        super(upgradeTier, source);
        gas = gasTank.getGas() == null ? null : gasTank.getGas().copy();
    }
}
