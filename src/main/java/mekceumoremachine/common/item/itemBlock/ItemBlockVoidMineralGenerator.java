package mekceumoremachine.common.item.itemBlock;

import mekceumoremachine.common.config.MoreMachineConfig;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.machine.TileEntityVoidMineralGenerator;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

public class ItemBlockVoidMineralGenerator extends ItemBlockMekceuMoreMachineTier {


    public ItemBlockVoidMineralGenerator(Block block) {
        super(block, "VoidMineralGenerator");
    }

    @Override
    void setTierMachine(TileEntity tileEntity, ItemStack stack) {
        if (tileEntity instanceof TileEntityVoidMineralGenerator tile) {
            tile.tier = MachineTier.values()[getBaseTier(stack).ordinal()];
        }
    }


    @Override
    public double getMachineStorage() {
        return MoreMachineConfig.current().config.VoidMineralGeneratorEnergyStorge.val();
    }


}
