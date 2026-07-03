package mekceumoremachine.common.item.itemBlock;

import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.machine.TileEntityTierRadioactiveWasteBarrel;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

public class ItemBlockTierRadioactiveWasteBarrel extends ItemBlockTierMachine {

    public ItemBlockTierRadioactiveWasteBarrel(Block block) {
        super(block, "TierRadioactiveWasteBarrel");
    }

    @Override
    public void setTierMachine(TileEntity tileEntity, ItemStack stack) {
        if (tileEntity instanceof TileEntityTierRadioactiveWasteBarrel tile) {
            tile.tier = MachineTier.get(getBaseTier(stack));
            tile.gasTank.setMaxGas(tile.tier.processes * TileEntityTierRadioactiveWasteBarrel.MAX_GAS);
        }
    }

}
