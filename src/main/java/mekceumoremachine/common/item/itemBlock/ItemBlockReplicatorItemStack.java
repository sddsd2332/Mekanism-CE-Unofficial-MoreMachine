package mekceumoremachine.common.item.itemBlock;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

public class ItemBlockReplicatorItemStack extends ItemBlockMekceuMoreMachineEnergy {

    public ItemBlockReplicatorItemStack(Block block) {
        super(block, "ReplicatorItemStack");
    }


    @Override
    public double getMachineStorage(ItemStack stack) {
        return 80000D;
    }


}
