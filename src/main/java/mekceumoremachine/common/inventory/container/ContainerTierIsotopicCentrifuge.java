package mekceumoremachine.common.inventory.container;

import mekanism.common.inventory.container.MekanismTileContainer;
import mekceumoremachine.common.tile.machine.TileEntityTierIsotopicCentrifuge;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerTierIsotopicCentrifuge extends MekanismTileContainer<TileEntityTierIsotopicCentrifuge> {

    public ContainerTierIsotopicCentrifuge(InventoryPlayer inventory, TileEntityTierIsotopicCentrifuge tile) {
        super(tile, inventory);
    }
}
