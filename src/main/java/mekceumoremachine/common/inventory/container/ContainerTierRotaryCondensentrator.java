package mekceumoremachine.common.inventory.container;

import mekanism.common.inventory.container.MekanismTileContainer;
import mekceumoremachine.common.tile.machine.TileEntityTierRotaryCondensentrator;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerTierRotaryCondensentrator extends MekanismTileContainer<TileEntityTierRotaryCondensentrator> {

    public ContainerTierRotaryCondensentrator(InventoryPlayer inventory, TileEntityTierRotaryCondensentrator tile) {
        super(tile, inventory);
    }
}
