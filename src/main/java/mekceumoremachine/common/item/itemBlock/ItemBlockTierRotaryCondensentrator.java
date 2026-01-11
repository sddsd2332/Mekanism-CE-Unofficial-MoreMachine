package mekceumoremachine.common.item.itemBlock;

import mekanism.common.block.states.BlockStateMachine;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.machine.TileEntityTierRotaryCondensentrator;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

public class ItemBlockTierRotaryCondensentrator extends ItemBlockMekceuMoreMachineTier {

    public ItemBlockTierRotaryCondensentrator(Block block) {
        super(block, "TierRotaryCondensentrator");
    }

    @Override
    public void setTierMachine(TileEntity tileEntity, ItemStack stack) {
        if (tileEntity instanceof TileEntityTierRotaryCondensentrator tile) {
            tile.tier = MachineTier.values()[getBaseTier(stack).ordinal()];
            tile.fluidTank.setCapacity(tile.tier.processes * TileEntityTierRotaryCondensentrator.MAX_FLUID);
            tile.gasTank.setMaxGas(tile.tier.processes * TileEntityTierRotaryCondensentrator.MAX_FLUID);
        }
    }

    @Override
    double getMachineStorage() {
        return BlockStateMachine.MachineType.ROTARY_CONDENSENTRATOR.getStorage();
    }

}
