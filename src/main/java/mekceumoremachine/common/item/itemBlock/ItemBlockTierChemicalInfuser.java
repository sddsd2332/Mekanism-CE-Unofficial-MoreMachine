package mekceumoremachine.common.item.itemBlock;

import mekanism.common.block.states.BlockStateMachine;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.machine.TileEntityTierChemicalInfuser;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

public class ItemBlockTierChemicalInfuser extends ItemBlockMekceuMoreMachineTier {

    public ItemBlockTierChemicalInfuser(Block block) {
        super(block, "TierChemicalInfuser");
    }

    @Override
    public void setTierMachine(TileEntity tileEntity, ItemStack stack) {
        if (tileEntity instanceof TileEntityTierChemicalInfuser tile) {
            tile.tier = MachineTier.values()[getBaseTier(stack).ordinal()];
            tile.leftTank.setMaxGas(tile.tier.processes * TileEntityTierChemicalInfuser.MAX_GAS);
            tile.rightTank.setMaxGas(tile.tier.processes * TileEntityTierChemicalInfuser.MAX_GAS);
            tile.centerTank.setMaxGas(tile.tier.processes * TileEntityTierChemicalInfuser.MAX_GAS);
        }
    }

    @Override
    public double getMachineStorage() {
        return BlockStateMachine.MachineType.CHEMICAL_INFUSER.getStorage();
    }
}
