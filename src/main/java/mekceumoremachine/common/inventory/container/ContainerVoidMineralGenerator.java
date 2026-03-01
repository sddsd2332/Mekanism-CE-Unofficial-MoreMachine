package mekceumoremachine.common.inventory.container;

import mekanism.common.inventory.container.ContainerMekanism;
import mekanism.common.inventory.slot.SlotEnergy;
import mekanism.common.inventory.slot.SlotOutput;
import mekanism.common.util.ChargeUtils;
import mekceumoremachine.common.tile.machine.TileEntityVoidMineralGenerator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class ContainerVoidMineralGenerator extends ContainerMekanism<TileEntityVoidMineralGenerator> {


    public ContainerVoidMineralGenerator(InventoryPlayer inventory, TileEntityVoidMineralGenerator tile) {
        super(tile, inventory);
    }

    @Override
    protected void addSlots() {
        addSlotToContainer(new SlotEnergy.SlotDischarge(tileEntity, 1, 15, 57));
        for (int slotY = 0; slotY < 9; slotY++) {
            for (int slotX = 0; slotX < 9; slotX++) {
                addSlotToContainer(new SlotOutput(tileEntity, 2 + slotX + slotY * 9, 81 + slotX * 18, 14 + slotY * 18));
            }
        }
    }


    @Nonnull
    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotID) {
        ItemStack stack = ItemStack.EMPTY;
        Slot currentSlot = inventorySlots.get(slotID);
        if (currentSlot != null && currentSlot.getHasStack()) {
            ItemStack slotStack = currentSlot.getStack();
            stack = slotStack.copy();
            if (ChargeUtils.canBeDischarged(slotStack)) {
                if (slotID != 0) {
                    // player inv → energy slot
                    if (!mergeItemStack(slotStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    // energy slot → player inv
                    if (!mergeItemStack(slotStack, 82, inventorySlots.size(), true)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else if (slotID >= 1 && slotID <= 81) {
                // output slot → player inv
                if (!mergeItemStack(slotStack, 82, inventorySlots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotID >= 82 && slotID <= 108) {
                // player main inv → hotbar
                if (!mergeItemStack(slotStack, 109, inventorySlots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotID >= 109) {
                // hotbar → player main inv
                if (!mergeItemStack(slotStack, 82, 109, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (slotStack.getCount() == 0) {
                currentSlot.putStack(ItemStack.EMPTY);
            } else {
                currentSlot.onSlotChanged();
            }
            if (slotStack.getCount() == stack.getCount()) {
                return ItemStack.EMPTY;
            }
            currentSlot.onTake(player, slotStack);
        }
        return stack;
    }

    @Override
    protected int getInventorYOffset() {
        return 186;
    }

}
