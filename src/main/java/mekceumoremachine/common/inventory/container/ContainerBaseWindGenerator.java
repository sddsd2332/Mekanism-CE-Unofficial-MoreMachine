package mekceumoremachine.common.inventory.container;

import mekanism.generators.common.inventory.container.ContainerPassiveGenerator;
import mekceumoremachine.common.tile.generator.TileEntityBaseWindGenerator;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerBaseWindGenerator extends ContainerPassiveGenerator<TileEntityBaseWindGenerator> {

    public ContainerBaseWindGenerator(InventoryPlayer inventory, TileEntityBaseWindGenerator generator) {
        super(inventory, generator);
    }
}
