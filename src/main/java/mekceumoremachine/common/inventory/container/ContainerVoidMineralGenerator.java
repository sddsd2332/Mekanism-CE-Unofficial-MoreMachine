package mekceumoremachine.common.inventory.container;

import mekanism.common.inventory.container.MekanismTileContainer;
import mekceumoremachine.common.tile.machine.TileEntityVoidMineralGenerator;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerVoidMineralGenerator extends MekanismTileContainer<TileEntityVoidMineralGenerator> {

    public ContainerVoidMineralGenerator(InventoryPlayer inventory, TileEntityVoidMineralGenerator tile) {
        super(tile, inventory);
    }

    @Override
    protected int getInventoryYOffset() {
        return 186;
    }
}
