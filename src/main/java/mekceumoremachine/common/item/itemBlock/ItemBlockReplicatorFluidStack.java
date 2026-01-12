package mekceumoremachine.common.item.itemBlock;

import mekceumoremachine.common.config.MoreMachineConfig;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

public class ItemBlockReplicatorFluidStack extends ItemBlockMekceuMoreMachineEnergy {

    public ItemBlockReplicatorFluidStack(Block block) {
        super(block, "ReplicatorFluidStack");
    }


    @Override
    public double getMachineStorage(ItemStack stack) {
        return MoreMachineConfig.current().config.ReplicatorFluidStackEnergyStorge.val();
    }


}
