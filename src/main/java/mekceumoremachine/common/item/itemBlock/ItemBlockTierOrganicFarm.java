package mekceumoremachine.common.item.itemBlock;

import mekanism.common.tier.BaseTier;
import mekceumoremachine.common.block.states.BlockStateTierOrganicFarm;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

public class ItemBlockTierOrganicFarm extends ItemBlockTierMachine {
    public ItemBlockTierOrganicFarm(Block block) { super(block, "TierOrganicFarm"); setHasSubtypes(true); }
    @Override public int getMetadata(int meta) { return meta; }

    @Override
    public BaseTier getBaseTier(ItemStack stack) {
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("tier")) {
            return super.getBaseTier(stack);
        }
        BlockStateTierOrganicFarm.MachineType type = BlockStateTierOrganicFarm.MachineType.get(stack);
        return type == null ? BaseTier.BASIC : type.tier.getBaseTier();
    }

    @Override
    void setTierMachine(TileEntity tileEntity, ItemStack stack) {
        // The block metadata selects a fixed tier-specific tile class.
    }
}
