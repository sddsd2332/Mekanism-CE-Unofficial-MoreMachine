package mekceumoremachine.common.upgrade;

import mekanism.api.gas.GasStack;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.machine.TileEntityOrganicFarm;
import mekceumoremachine.common.tile.machine.TierOrganicFarm.TileEntityTierOrganicFarm;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import java.util.List;

/** Complete lane/shared-inventory snapshot used by first and repeated farm upgrades. */
public class OrganicFarmUpgradeData extends LargeMachineUpgradeData {

    public final double prevEnergy;
    public final ItemStack energySlot;
    public final ItemStack mediumSlot;
    public final ItemStack[] inputSlots;
    public final ItemStack[] outputSlots;
    public final int[] progress;
    public final long[] usedSoFar;
    public final boolean sorting;
    public final GasStack gas;
    public final FluidStack fluid;
    public final NBTTagCompound configComponentData = new NBTTagCompound();
    public final NBTTagCompound ejectorComponentData = new NBTTagCompound();

    public OrganicFarmUpgradeData(@Nonnull BaseTier upgradeTier, @Nonnull TileEntityTierOrganicFarm source) {
        super(upgradeTier, source);
        prevEnergy = source.prevEnergy;
        energySlot = copyStack(source.energySlot.getStack());
        mediumSlot = copyStack(source.mergedTankSlot.getStack());
        inputSlots = copyInputs(source);
        outputSlots = copyOutputs(source);
        progress = source.progress.clone();
        usedSoFar = source.usedSoFar.clone();
        sorting = source.sorting;
        gas = source.gasTank.getGas() == null ? null : source.gasTank.getGas().copy();
        fluid = source.fluidTank.getFluid() == null ? null : source.fluidTank.getFluid().copy();
        source.configComponent.write(configComponentData);
        source.ejectorComponent.write(ejectorComponentData);
    }

    public OrganicFarmUpgradeData(@Nonnull BaseTier upgradeTier, @Nonnull TileEntityOrganicFarm source) {
        super(upgradeTier, source);
        prevEnergy = source.prevEnergy;
        List<IInventorySlot> slots = source.getInventorySlots(null);
        inputSlots = new ItemStack[]{copySlot(slots, 0)};
        mediumSlot = copySlot(slots, 1);
        energySlot = copySlot(slots, 2);
        outputSlots = new ItemStack[64];
        for (int i = 0; i < outputSlots.length; i++) {
            outputSlots[i] = copySlot(slots, i + 3);
        }
        progress = new int[]{source.operatingTicks};
        usedSoFar = new long[]{source.getSavedUsedSoFar(0)};
        sorting = true;
        gas = source.gasTank.getGas() == null ? null : source.gasTank.getGas().copy();
        fluid = source.fluidTank.getFluid() == null ? null : source.fluidTank.getFluid().copy();
        source.configComponent.write(configComponentData);
        source.ejectorComponent.write(ejectorComponentData);
    }

    private static ItemStack[] copyInputs(TileEntityTierOrganicFarm source) {
        ItemStack[] stacks = new ItemStack[source.inputSlots.length];
        for (int i = 0; i < stacks.length; i++) {
            stacks[i] = copyStack(source.inputSlots[i].getStack());
        }
        return stacks;
    }

    private static ItemStack[] copyOutputs(TileEntityTierOrganicFarm source) {
        ItemStack[] stacks = new ItemStack[source.outputSlots.size()];
        for (int i = 0; i < stacks.length; i++) {
            stacks[i] = copyStack(source.outputSlots.get(i).getStack());
        }
        return stacks;
    }

    private static ItemStack copySlot(List<IInventorySlot> slots, int index) {
        return index >= 0 && index < slots.size() ? copyStack(slots.get(index).getStack()) : ItemStack.EMPTY;
    }
}
