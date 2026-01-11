package mekceumoremachine.common.item.itemBlock;

import mekanism.common.config.MekanismConfig;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.generator.TileEntityTierGasGenerator;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

public class ItemBlockTierGasGenerator extends ItemBlockMekceuMoreMachineTier {

    public ItemBlockTierGasGenerator(Block block) {
        super(block, "TierGasGenerator");
    }

    @Override
    public void setTierMachine(TileEntity tileEntity, ItemStack stack) {
        if (tileEntity instanceof TileEntityTierGasGenerator tile) {
            tile.tier = MachineTier.values()[getBaseTier(stack).ordinal()];
            tile.fuelTank.setMaxGas(tile.tier.processes * TileEntityTierGasGenerator.MAX_GAS);
        }
    }

    @Override
    public double getMachineStorage() {
        return MekanismConfig.current().general.FROM_H2.val() * 1000;
    }
}
