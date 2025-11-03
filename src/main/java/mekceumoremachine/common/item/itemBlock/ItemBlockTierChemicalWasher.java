package mekceumoremachine.common.item.itemBlock;

import mekanism.common.block.states.BlockStateMachine;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.machine.TileEntityTierChemicalWasher;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

public class ItemBlockTierChemicalWasher extends ItemBlockTierEnergyMachine {

    public ItemBlockTierChemicalWasher(Block block) {
        super(block, "TierChemicalWasher");
    }

    @Override
    public void setTierMachine(TileEntity tileEntity, ItemStack stack) {
        if (tileEntity instanceof TileEntityTierChemicalWasher tile) {
            tile.tier = MachineTier.values()[getBaseTier(stack).ordinal()];
            tile.fluidTank.setCapacity(tile.tier.processes * TileEntityTierChemicalWasher.MAX_FLUID);
            tile.inputTank.setMaxGas(tile.tier.processes * TileEntityTierChemicalWasher.MAX_GAS);
            tile.outputTank.setMaxGas(tile.tier.processes * TileEntityTierChemicalWasher.MAX_GAS);
        }
    }


    @Override
    public double getMachineStorage() {
        return BlockStateMachine.MachineType.CHEMICAL_WASHER.getStorage();
    }
}
