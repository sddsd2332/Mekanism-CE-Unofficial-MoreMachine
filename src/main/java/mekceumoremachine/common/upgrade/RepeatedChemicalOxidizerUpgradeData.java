package mekceumoremachine.common.upgrade;

import mekanism.api.gas.GasStack;
import mekanism.common.tier.BaseTier;
import mekceumoremachine.common.tile.machine.TierOxidizer.TileEntityTierChemicalOxidizer;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nonnull;

public class RepeatedChemicalOxidizerUpgradeData extends LargeMachineUpgradeData {

    public final double prevEnergy;
    public final int[] progress;
    public final boolean sorting;
    public final NBTTagCompound configComponentData;
    public final NBTTagCompound ejectorComponentData;
    public final GasStack[] outputGases;

    public RepeatedChemicalOxidizerUpgradeData(@Nonnull BaseTier upgradeTier, @Nonnull TileEntityTierChemicalOxidizer source) {
        super(upgradeTier, source);
        prevEnergy = source.prevEnergy;
        progress = source.progress.clone();
        sorting = source.sorting;
        configComponentData = new NBTTagCompound();
        source.configComponent.write(configComponentData);
        ejectorComponentData = new NBTTagCompound();
        source.ejectorComponent.write(ejectorComponentData);
        outputGases = copyGases(source.outPutTanks);
    }

    private static GasStack[] copyGases(mekceumoremachine.common.capability.ResizableGasTank[] tanks) {
        GasStack[] gases = new GasStack[tanks.length];
        for (int i = 0; i < gases.length; i++) {
            gases[i] = tanks[i].getGas() == null ? null : tanks[i].getGas().copy();
        }
        return gases;
    }
}
