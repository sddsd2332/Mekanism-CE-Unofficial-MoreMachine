package mekceumoremachine.common.inventory.container;

import mekanism.common.inventory.container.ContainerEnergyStorage;
import mekceumoremachine.common.tile.machine.TileEntityWirelessChargingStation;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerWirelessCharging extends ContainerEnergyStorage<TileEntityWirelessChargingStation> {

    public ContainerWirelessCharging(InventoryPlayer inventory, TileEntityWirelessChargingStation tile) {
        super(tile, inventory);
    }

    @Override
    protected void addSlots() {
        super.addSlots();
        addArmorSlots(inv, 180, 37 + 5, 0);
    }

    @Override
    protected int getInventoryYOffset() {
        return 84 + 2;
    }
}
