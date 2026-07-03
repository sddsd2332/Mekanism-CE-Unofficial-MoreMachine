package mekceumoremachine.common.inventory.container;

import mekanism.common.inventory.container.MekanismTileContainer;
import mekceumoremachine.common.tile.machine.TileEntityTierSolarNeutronActivator;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerTierSolarNeutronActivator extends MekanismTileContainer<TileEntityTierSolarNeutronActivator> {

    public ContainerTierSolarNeutronActivator(InventoryPlayer inventory, TileEntityTierSolarNeutronActivator tile) {
        super(tile, inventory);
    }
}
