package mekceumoremachine.common.item.itemBlock;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

public class ItemBlockReplicatorGases extends ItemBlockMekceuMoreMachineEnergy {

    public ItemBlockReplicatorGases(Block block) {
        super(block, "ReplicatorGases");
    }


    @Override
    public double getMachineStorage(ItemStack stack) {
        return 80000D;
    }


}
