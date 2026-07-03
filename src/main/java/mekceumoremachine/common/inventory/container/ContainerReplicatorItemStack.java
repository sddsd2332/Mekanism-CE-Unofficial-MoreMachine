package mekceumoremachine.common.inventory.container;

import mekanism.common.inventory.container.MekanismTileContainer;
import mekceumoremachine.common.tile.machine.replicator.TileEntityReplicatorItemStack;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerReplicatorItemStack extends MekanismTileContainer<TileEntityReplicatorItemStack> {

    public ContainerReplicatorItemStack(InventoryPlayer inventory, TileEntityReplicatorItemStack tile) {
        super(tile, inventory);
    }
}
