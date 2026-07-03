package mekceumoremachine.common.upgrade;

import mekanism.api.gas.GasStack;
import mekanism.common.tier.BaseTier;
import mekceumoremachine.common.capability.ResizableGasTank;
import mekceumoremachine.common.tile.machine.TierCrystallizer.TileEntityTierChemicalCrystallizer;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nonnull;

public class RepeatedChemicalCrystallizerUpgradeData extends LargeMachineUpgradeData {

    public final double prevEnergy;
    public final int[] progress;
    public final NBTTagCompound configComponentData;
    public final NBTTagCompound ejectorComponentData;
    public final GasStack[] inputGases;
    public final boolean sorting;

    public RepeatedChemicalCrystallizerUpgradeData(@Nonnull BaseTier upgradeTier, @Nonnull TileEntityTierChemicalCrystallizer source) {
        super(upgradeTier, source);
        prevEnergy = source.prevEnergy;
        progress = source.progress.clone();
        configComponentData = new NBTTagCompound();
        source.configComponent.write(configComponentData);
        ejectorComponentData = new NBTTagCompound();
        source.ejectorComponent.write(ejectorComponentData);
        inputGases = copyGases(source.inputTanks);
        sorting = source.sorting;
    }

    private static GasStack[] copyGases(ResizableGasTank[] tanks) {
        GasStack[] gases = new GasStack[tanks.length];
        for (int i = 0; i < gases.length; i++) {
            gases[i] = tanks[i].getGas() == null ? null : tanks[i].getGas().copy();
        }
        return gases;
    }
}
