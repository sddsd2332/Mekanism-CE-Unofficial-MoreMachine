package mekceumoremachine.common.inventory.container;

import mekanism.common.inventory.slot.SlotEnergy;
import mekanism.generators.common.inventory.container.ContainerPassiveGenerator;
import mekceumoremachine.common.tile.generator.TileEntityBaseWindGenerator;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerBaseWindGenerator extends ContainerPassiveGenerator<TileEntityBaseWindGenerator> {

    public ContainerBaseWindGenerator(InventoryPlayer inventory, TileEntityBaseWindGenerator generator) {
        super(inventory, generator);
    }

    @Override
    protected void addSlots() {
        addSlotToContainer(new SlotEnergy.SlotCharge(tileEntity, 0, 143, 35));
    }

}
