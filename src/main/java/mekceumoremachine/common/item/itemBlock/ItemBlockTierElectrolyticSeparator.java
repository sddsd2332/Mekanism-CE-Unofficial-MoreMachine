package mekceumoremachine.common.item.itemBlock;

import mekanism.common.block.states.BlockStateMachine.MachineType;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.machine.TileEntityTierElectrolyticSeparator;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

public class ItemBlockTierElectrolyticSeparator extends ItemBlockMekceuMoreMachineTier {

    public ItemBlockTierElectrolyticSeparator(Block block) {
        super(block, "TierElectrolyticSeparator");
    }

    @Override
    public void setTierMachine(TileEntity tileEntity, ItemStack stack) {
        if (tileEntity instanceof TileEntityTierElectrolyticSeparator tile) {
            tile.tier = MachineTier.values()[getBaseTier(stack).ordinal()];
            tile.fluidTank.setCapacity(tile.tier.processes * TileEntityTierElectrolyticSeparator.MAX_GAS * 10);
            tile.leftTank.setMaxGas(tile.tier.processes * TileEntityTierElectrolyticSeparator.MAX_GAS);
            tile.rightTank.setMaxGas(tile.tier.processes * TileEntityTierElectrolyticSeparator.MAX_GAS);
        }
    }

    @Override
    double getMachineStorage() {
        return MachineType.ELECTROLYTIC_SEPARATOR.getStorage();
    }


}
