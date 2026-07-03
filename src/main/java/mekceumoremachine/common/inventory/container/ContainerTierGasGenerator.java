package mekceumoremachine.common.inventory.container;

import mekanism.generators.common.inventory.container.ContainerFuelGenerator;
import mekceumoremachine.common.tile.generator.TileEntityTierGasGenerator;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerTierGasGenerator extends ContainerFuelGenerator<TileEntityTierGasGenerator> {

    public ContainerTierGasGenerator(InventoryPlayer inventory, TileEntityTierGasGenerator generator) {
        super(inventory, generator);
    }
}
