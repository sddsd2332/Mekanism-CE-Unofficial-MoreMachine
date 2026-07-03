package mekceumoremachine.common.inventory.container;

import mekanism.common.inventory.container.MekanismTileContainer;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.machine.TierDissolution.TileEntityTierChemicalDissolutionChamber;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerTierChemicalDissolutionChamber extends MekanismTileContainer<TileEntityTierChemicalDissolutionChamber> {

    public ContainerTierChemicalDissolutionChamber(InventoryPlayer inventory, TileEntityTierChemicalDissolutionChamber tile) {
        super(tile, inventory);
    }

    @Override
    protected int getInventoryYOffset() {
        return 100;
    }


    @Override
    protected int getInventoryXOffset() {
        return tile != null && tile.tier == MachineTier.ULTIMATE ? 27 : 8;
    }
}
