package mekceumoremachine.common.inventory.container;

import mekanism.common.inventory.container.MekanismTileContainer;
import mekceumoremachine.common.tile.machine.TileEntityTierChemicalInfuser;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerTierChemicalInfuser extends MekanismTileContainer<TileEntityTierChemicalInfuser> {

    public ContainerTierChemicalInfuser(InventoryPlayer inventory, TileEntityTierChemicalInfuser tile) {
        super(tile, inventory);
    }

    @Override
    protected int getInventoryYOffset() {
        return 84;
    }
}
