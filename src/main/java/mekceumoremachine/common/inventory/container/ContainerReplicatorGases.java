package mekceumoremachine.common.inventory.container;

import mekanism.common.inventory.container.MekanismTileContainer;
import mekceumoremachine.common.tile.machine.replicator.TileEntityReplicatorGases;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerReplicatorGases extends MekanismTileContainer<TileEntityReplicatorGases> {

    public ContainerReplicatorGases(InventoryPlayer inventory, TileEntityReplicatorGases tile) {
        super(tile, inventory);
    }
}
