package mekceumoremachine.common.inventory.container;

import mekanism.common.inventory.container.MekanismTileContainer;
import mekceumoremachine.common.tile.machine.TileEntityTierElectrolyticSeparator;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerTierElectrolyticSeparator extends MekanismTileContainer<TileEntityTierElectrolyticSeparator> {

    public ContainerTierElectrolyticSeparator(InventoryPlayer inventory, TileEntityTierElectrolyticSeparator tile) {
        super(tile, inventory);
    }
}
