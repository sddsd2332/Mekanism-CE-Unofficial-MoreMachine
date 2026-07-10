package mekceumoremachine.common.upgrade;

import mekanism.api.gas.GasStack;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.capabilities.gas.BasicGasTank;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.prefab.TileEntityContainerBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nonnull;
import java.util.List;

public class FirstChemicalDissolutionChamberUpgradeData extends LargeMachineUpgradeData {

    public final double prevEnergy;
    public final int operatingTicks;
    public final long usedSoFar;
    public final NBTTagCompound configComponentData;
    public final NBTTagCompound ejectorComponentData;
    public final GasStack injectGas;
    public final GasStack outputGas;
    public final ItemStack targetSlot0;
    public final ItemStack targetSlot1;
    public final ItemStack targetSlot2;
    public final ItemStack unmappedOutputGasSlot;

    public FirstChemicalDissolutionChamberUpgradeData(@Nonnull BaseTier upgradeTier, @Nonnull TileEntityContainerBlock source,
          double prevEnergy, int operatingTicks, long usedSoFar, TileComponentConfig configComponent, TileComponentEjector ejectorComponent,
          BasicGasTank injectTank, BasicGasTank outputTank) {
        super(upgradeTier, source);
        this.prevEnergy = prevEnergy;
        this.operatingTicks = operatingTicks;
        this.usedSoFar = usedSoFar;
        configComponentData = new NBTTagCompound();
        configComponent.write(configComponentData);
        ejectorComponentData = new NBTTagCompound();
        ejectorComponent.write(ejectorComponentData);
        injectGas = injectTank.getGas() == null ? null : injectTank.getGas().copy();
        outputGas = outputTank.getGas() == null ? null : outputTank.getGas().copy();
        targetSlot0 = copySlot(source, 0);
        targetSlot1 = copySlot(source, 3);
        targetSlot2 = copySlot(source, 1);
        unmappedOutputGasSlot = copySlot(source, 2);
    }

    private static ItemStack copySlot(TileEntityContainerBlock source, int slot) {
        List<IInventorySlot> slots = source.getInventorySlots(null);
        return slot < slots.size() ? copyStack(slots.get(slot).getStack()) : ItemStack.EMPTY;
    }
}
