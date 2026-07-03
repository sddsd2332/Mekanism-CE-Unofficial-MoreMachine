package mekceumoremachine.common.inventory.container;

import mekanism.common.inventory.container.MekanismTileContainer;
import mekceumoremachine.common.tile.machine.TileEntityTierChemicalWasher;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerTierChemicalWasher extends MekanismTileContainer<TileEntityTierChemicalWasher> {

    public ContainerTierChemicalWasher(InventoryPlayer inventory, TileEntityTierChemicalWasher tile) {
        super(tile, inventory);
    }
}
