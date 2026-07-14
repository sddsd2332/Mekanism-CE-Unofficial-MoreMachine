package mekceumoremachine.common.attachments.component;

import net.minecraft.item.ItemStack;

public final class MachineTypeDescriptor {

    private final String machineTypeKey;
    private final String machineNameKey;
    private final ItemStack machineStack;

    public MachineTypeDescriptor(String machineTypeKey, String machineNameKey, ItemStack machineStack) {
        this.machineTypeKey = machineTypeKey == null ? "" : machineTypeKey;
        this.machineNameKey = machineNameKey == null ? "" : machineNameKey;
        this.machineStack = copyCountOne(machineStack);
    }

    public String getMachineTypeKey() {
        return machineTypeKey;
    }

    public String getMachineNameKey() {
        return machineNameKey;
    }

    public ItemStack getMachineStack() {
        return machineStack;
    }

    public MachineTypeDescriptor copy() {
        return new MachineTypeDescriptor(machineTypeKey, machineNameKey, machineStack);
    }

    private static ItemStack copyCountOne(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }
}
