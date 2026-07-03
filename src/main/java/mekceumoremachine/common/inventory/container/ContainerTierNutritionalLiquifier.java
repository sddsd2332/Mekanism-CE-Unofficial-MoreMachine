package mekceumoremachine.common.inventory.container;

import mekanism.common.inventory.container.MekanismTileContainer;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.machine.TierNutritional.TileEntityTierNutritionalLiquifier;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerTierNutritionalLiquifier extends MekanismTileContainer<TileEntityTierNutritionalLiquifier> {

    public ContainerTierNutritionalLiquifier(InventoryPlayer inventory, TileEntityTierNutritionalLiquifier tile) {
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
