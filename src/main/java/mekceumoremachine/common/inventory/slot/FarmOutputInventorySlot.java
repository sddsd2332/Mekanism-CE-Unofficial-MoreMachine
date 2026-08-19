package mekceumoremachine.common.inventory.slot;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.common.inventory.container.SelectedWindowData;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.inventory.container.slot.VirtualInventoryContainerSlot;
import mekanism.common.inventory.slot.BasicInventorySlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Shared Organic Farm output slot. Its capacity is scaled by the factory lane count. */
public class FarmOutputInventorySlot extends BasicInventorySlot {

    private static final String SYNC_COUNT = "MekCeuMoreMachineFarmOutputCount";
    private final int multiplier;
    private final SelectedWindowData windowData;

    public FarmOutputInventorySlot(int multiplier, @Nullable IContentsListener listener, int x, int y,
          SelectedWindowData windowData) {
        super(alwaysTrueBi, internalOnly, alwaysTrue, listener, x, y);
        if (multiplier < 1) {
            throw new IllegalArgumentException("Farm output multiplier must be positive");
        }
        this.multiplier = multiplier;
        this.windowData = windowData;
        // BasicInventorySlot normally clamps to ItemStack#getMaxStackSize. A
        // factory output deliberately allows a multiplied logical stack size.
        obeyStackLimit = false;
        setSlotType(ContainerSlotType.OUTPUT);
    }

    public int getMultiplier() {
        return multiplier;
    }

    @Override
    public int getLimit(@Nonnull ItemStack stack) {
        return stack.isEmpty() ? BasicInventorySlot.DEFAULT_LIMIT * multiplier : stack.getMaxStackSize() * multiplier;
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int amount, @Nonnull Action action, @Nonnull AutomationType automationType) {
        // QIO item-group ports request the whole logical overstack through the internal path.
        if (automationType != AutomationType.INTERNAL) {
            return super.extractItem(amount, action, automationType);
        }
        if (isEmpty() || amount < 1) {
            return ItemStack.EMPTY;
        }
        int toExtract = Math.min(amount, getCount());
        ItemStack extracted = current.copy();
        extracted.setCount(toExtract);
        if (action.execute()) {
            current.shrink(toExtract);
            if (current.getCount() <= 0) {
                current = ItemStack.EMPTY;
            }
            onContentsChanged();
        }
        return extracted;
    }

    @Override
    public FarmOutputContainerSlot createContainerSlot() {
        return new FarmOutputContainerSlot();
    }

    private void setFromContainer(ItemStack stack) {
        ItemStack decoded = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        if (!decoded.isEmpty() && decoded.hasTagCompound() && decoded.getTagCompound().hasKey(SYNC_COUNT)) {
            NBTTagCompound tag = decoded.getTagCompound().copy();
            int count = tag.getInteger(SYNC_COUNT);
            tag.removeTag(SYNC_COUNT);
            decoded.setTagCompound(tag.getKeySet().isEmpty() ? null : tag);
            decoded.setCount(Math.max(0, count));
        }
        setStackUnchecked(decoded);
    }

    public class FarmOutputContainerSlot extends VirtualInventoryContainerSlot {

        private boolean syncing;

        private FarmOutputContainerSlot() {
            super(FarmOutputInventorySlot.this, windowData, FarmOutputInventorySlot.this.getSlotOverlay(),
                  FarmOutputInventorySlot.this::setFromContainer);
        }

        public void setSyncing(boolean syncing) {
            this.syncing = syncing;
        }

        @Nonnull
        @Override
        public ItemStack getItem() {
            ItemStack stored = FarmOutputInventorySlot.this.getStack();
            if (stored.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack visible = stored.copy();
            visible.setCount(Math.min(stored.getCount(), Math.max(1, stored.getMaxStackSize())));
            if (syncing) {
                NBTTagCompound tag = visible.hasTagCompound() ? visible.getTagCompound().copy() : new NBTTagCompound();
                tag.setInteger(SYNC_COUNT, stored.getCount());
                visible.setTagCompound(tag);
            }
            return visible;
        }

        @Nonnull
        @Override
        public ItemStack getStackToRender() {
            return FarmOutputInventorySlot.this.getStack().copy();
        }

    }
}
