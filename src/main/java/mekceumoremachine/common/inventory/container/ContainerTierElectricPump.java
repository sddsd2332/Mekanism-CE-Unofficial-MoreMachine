package mekceumoremachine.common.inventory.container;

import mekanism.common.inventory.container.MekanismTileContainer;
import mekceumoremachine.common.tile.machine.TileEntityTierElectricPump;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerTierElectricPump extends MekanismTileContainer<TileEntityTierElectricPump> {

    public ContainerTierElectricPump(InventoryPlayer inventory, TileEntityTierElectricPump tile) {
        super(tile, inventory);
    }
}
