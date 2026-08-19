package mekceumoremachine.common.inventory.container;

import mekanism.common.inventory.container.MekanismTileContainer;
import mekanism.common.inventory.container.SelectedWindowData;
import mekanism.common.inventory.container.slot.VirtualInventoryContainerSlot;
import mekceumoremachine.common.inventory.slot.FarmOutputInventorySlot.FarmOutputContainerSlot;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.machine.TierOrganicFarm.TileEntityTierOrganicFarm;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

import javax.annotation.Nonnull;


public class ContainerTierOrganicFarm extends MekanismTileContainer<TileEntityTierOrganicFarm> {

    private FarmOutputContainerSlot[] outputSlots;
    public ContainerTierOrganicFarm(InventoryPlayer inventory, TileEntityTierOrganicFarm tile) {
        super(tile, inventory);
    }

    @Override
    protected int getInventoryYOffset() {
        return 95;
    }

    @Override
    protected int getInventoryXOffset() {
        return tile != null && tile.tier == MachineTier.ULTIMATE ? 26 : 8;
    }

    @Override
    protected void addSlots() {
        super.addSlots();
        outputSlots = new FarmOutputContainerSlot[tile == null ? 0 : tile.outputSlots.size()];
        if (tile == null) {
            return;
        }
        for (int i = 0; i < tile.outputSlots.size(); i++) {
            for (int slot = 0; slot < inventorySlots.size(); slot++) {
                if (inventorySlots.get(slot) instanceof FarmOutputContainerSlot virtual &&
                    virtual.getInventorySlot() == tile.outputSlots.get(i)) {
                    outputSlots[i] = virtual;
                    break;
                }
            }
        }
    }

    public VirtualInventoryContainerSlot getOutputSlot(int index) {
        return outputSlots != null && index >= 0 && index < outputSlots.length ? outputSlots[index] : null;
    }

    @Override
    public NonNullList<ItemStack> getInventory() {
        setOutputSlotsSyncing(true);
        try {
            return super.getInventory();
        } finally {
            setOutputSlotsSyncing(false);
        }
    }

    @Override
    public void detectAndSendChanges() {
        setOutputSlotsSyncing(true);
        try {
            super.detectAndSendChanges();
        } finally {
            setOutputSlotsSyncing(false);
        }
    }

    @Nonnull
    @Override
    public ItemStack slotClick(int slotId, int dragType, ClickType clickType, EntityPlayer player) {
        if (slotId >= 0 && slotId < inventorySlots.size()) {
            Slot slot = inventorySlots.get(slotId);
            if (slot instanceof FarmOutputContainerSlot outputSlot) {
                SelectedWindowData selected = player.world.isRemote ? getSelectedWindow() : getSelectedWindow(player.getUniqueID());
                if (!outputSlot.exists(selected) || clickType == ClickType.SWAP) {
                    return ItemStack.EMPTY;
                }
            }
        }
        return super.slotClick(slotId, dragType, clickType, player);
    }

    private void setOutputSlotsSyncing(boolean syncing) {
        if (outputSlots == null) {
            return;
        }
        for (FarmOutputContainerSlot outputSlot : outputSlots) {
            if (outputSlot != null) {
                outputSlot.setSyncing(syncing);
            }
        }
    }
}
