package mekceumoremachine.common.inventory.container;

import mekanism.common.inventory.container.MekanismTileContainer;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.machine.TierCrystallizer.TileEntityTierChemicalCrystallizer;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerTierChemicalCrystallizer extends MekanismTileContainer<TileEntityTierChemicalCrystallizer> {

    public ContainerTierChemicalCrystallizer(InventoryPlayer inventory, TileEntityTierChemicalCrystallizer tile) {
        super(tile, inventory);
    }

    @Override
    protected int getInventoryYOffset() {
        return 97;
    }

    @Override
    protected int getInventoryXOffset() {
        return tile != null && tile.tier == MachineTier.ULTIMATE ? 27 : 8;
    }
}
