    package mekceumoremachine.common.upgrade;

import mekanism.api.gas.GasStack;
import mekanism.common.tier.BaseTier;
import mekceumoremachine.common.tile.machine.TierDissolution.TileEntityTierChemicalDissolutionChamber;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nonnull;

public class RepeatedChemicalDissolutionChamberUpgradeData extends LargeMachineUpgradeData {

    public final double prevEnergy;
    public final int[] progress;
    public final long[] usedSoFar;
    public final boolean sorting;
    public final NBTTagCompound configComponentData;
    public final NBTTagCompound ejectorComponentData;
    public final GasStack injectGas;
    public final GasStack[] outputGases;

    public RepeatedChemicalDissolutionChamberUpgradeData(@Nonnull BaseTier upgradeTier, @Nonnull TileEntityTierChemicalDissolutionChamber source) {
        super(upgradeTier, source);
        prevEnergy = source.prevEnergy;
        progress = source.progress.clone();
        usedSoFar = source.usedSoFar.clone();
        sorting = source.sorting;
        configComponentData = new NBTTagCompound();
        source.configComponent.write(configComponentData);
        ejectorComponentData = new NBTTagCompound();
        source.ejectorComponent.write(ejectorComponentData);
        injectGas = source.injectTank.getGas() == null ? null : source.injectTank.getGas().copy();
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
