package mekceumoremachine.common.inventory.container;

import mekanism.common.inventory.container.ContainerEnergyStorage;
import mekanism.common.inventory.slot.SlotEnergy.SlotCharge;
import mekanism.common.inventory.slot.SlotEnergy.SlotDischarge;
import mekceumoremachine.common.tile.machine.TileEntityWirelessChargingEnergy;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerWirelessEnergy extends ContainerEnergyStorage<TileEntityWirelessChargingEnergy> {

    public ContainerWirelessEnergy(InventoryPlayer inventory, TileEntityWirelessChargingEnergy tile) {
        super(tile, inventory);
    }

    @Override
    protected void addSlots() {
        addSlotToContainer(new SlotCharge(tileEntity, 0, 143, 35));
        addSlotToContainer(new SlotDischarge(tileEntity, 1, 17, 35));
    }

}
