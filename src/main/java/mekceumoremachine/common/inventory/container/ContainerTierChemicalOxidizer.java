package mekceumoremachine.common.inventory.container;

import mekanism.common.inventory.container.MekanismTileContainer;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.machine.TierOxidizer.TileEntityTierChemicalOxidizer;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerTierChemicalOxidizer extends MekanismTileContainer<TileEntityTierChemicalOxidizer> {

    public ContainerTierChemicalOxidizer(InventoryPlayer inventory, TileEntityTierChemicalOxidizer tile) {
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
