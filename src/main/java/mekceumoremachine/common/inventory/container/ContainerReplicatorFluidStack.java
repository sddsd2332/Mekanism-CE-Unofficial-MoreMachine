package mekceumoremachine.common.inventory.container;

import mekanism.common.inventory.container.MekanismTileContainer;
import mekceumoremachine.common.tile.machine.replicator.TileEntityReplicatorFluidStack;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerReplicatorFluidStack extends MekanismTileContainer<TileEntityReplicatorFluidStack> {

    public ContainerReplicatorFluidStack(InventoryPlayer inventory, TileEntityReplicatorFluidStack tile) {
        super(tile, inventory);
    }
}
