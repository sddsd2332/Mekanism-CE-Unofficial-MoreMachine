package mekceumoremachine.common.item.itemBlock;

import mekanism.common.block.states.BlockStateMachine;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.machine.TileEntityTierAmbientAccumulator;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

public class ItemBlockTierAmbientAccumulator extends ItemBlockMekceuMoreMachineTier {

    public ItemBlockTierAmbientAccumulator(Block block) {
        super(block, "TierAmbientAccumulator");
    }

    @Override
    public void setTierMachine(TileEntity tileEntity, ItemStack stack) {
        if (tileEntity instanceof TileEntityTierAmbientAccumulator tile) {
            tile.tier = MachineTier.values()[getBaseTier(stack).ordinal()];
            tile.outputTank.setMaxGas(tile.tier.processes * TileEntityTierAmbientAccumulator.MAX_GAS);
        }
    }


    @Override
    public double getMachineStorage() {
        return BlockStateMachine.MachineType.AMBIENT_ACCUMULATOR_ENERGY.getStorage();
    }
}
