package mekceumoremachine.common.inventory.container;

import mekanism.common.inventory.container.ContainerEnergyStorage;
import mekceumoremachine.common.tile.machine.TileEntityWirelessChargingEnergy;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerWirelessEnergy extends ContainerEnergyStorage<TileEntityWirelessChargingEnergy> {

    public ContainerWirelessEnergy(InventoryPlayer inventory, TileEntityWirelessChargingEnergy tile) {
        super(tile, inventory);
    }
}
