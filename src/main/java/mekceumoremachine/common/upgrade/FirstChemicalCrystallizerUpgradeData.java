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

public class FirstChemicalCrystallizerUpgradeData extends LargeMachineUpgradeData {

    public final double prevEnergy;
    public final int operatingTicks;
    public final NBTTagCompound configComponentData;
    public final NBTTagCompound ejectorComponentData;
    public final GasStack inputGas;
    public final ItemStack outputSlot;
    public final ItemStack energySlot;

    public FirstChemicalCrystallizerUpgradeData(@Nonnull BaseTier upgradeTier, @Nonnull TileEntityContainerBlock source,
          double prevEnergy, int operatingTicks, TileComponentConfig configComponent, TileComponentEjector ejectorComponent, BasicGasTank inputTank) {
        super(upgradeTier, source);
        this.prevEnergy = prevEnergy;
        this.operatingTicks = operatingTicks;
        configComponentData = new NBTTagCompound();
        configComponent.write(configComponentData);
        ejectorComponentData = new NBTTagCompound();
        ejectorComponent.write(ejectorComponentData);
        inputGas = inputTank.getGas() == null ? null : inputTank.getGas().copy();
        outputSlot = copySlot(source, 1);
        energySlot = copySlot(source, 2);
    }

    private static ItemStack copySlot(TileEntityContainerBlock source, int slot) {
        List<IInventorySlot> slots = source.getInventorySlots(null);
        return slot < slots.size() ? copyStack(slots.get(slot).getStack()) : ItemStack.EMPTY;
    }
}
