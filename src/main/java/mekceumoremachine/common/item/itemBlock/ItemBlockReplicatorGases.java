package mekceumoremachine.common.item.itemBlock;

import mekceumoremachine.common.config.MoreMachineConfig;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

public class ItemBlockReplicatorGases extends ItemBlockMekceuMoreMachineEnergy {

    public ItemBlockReplicatorGases(Block block) {
        super(block, "ReplicatorGases");
    }


    @Override
    public double getMachineStorage(ItemStack stack) {
        return MoreMachineConfig.current().config.ReplicatorGasesEnergyStorge.val();
    }


}
