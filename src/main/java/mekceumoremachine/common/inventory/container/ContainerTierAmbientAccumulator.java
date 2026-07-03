package mekceumoremachine.common.inventory.container;

import mekanism.common.inventory.container.MekanismTileContainer;
import mekceumoremachine.common.tile.machine.TileEntityTierAmbientAccumulator;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerTierAmbientAccumulator extends MekanismTileContainer<TileEntityTierAmbientAccumulator> {

    public ContainerTierAmbientAccumulator(InventoryPlayer inventory, TileEntityTierAmbientAccumulator tile) {
        super(tile, inventory);
    }

    @Override
    protected int getInventoryYOffset() {
        return 89;
    }
}
