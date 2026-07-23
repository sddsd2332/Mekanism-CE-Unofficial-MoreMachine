package mekceumoremachine.common.item.itemBlock;

import mekanism.common.base.ISustainedTank;
import mekanism.common.block.states.BlockStateMachine;
import mekanism.common.util.ItemDataUtils;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.machine.TileEntityTierElectricPump;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;

public class ItemBlockTierElectricPump extends ItemBlockMekceuMoreMachineTier implements ISustainedTank {

    public ItemBlockTierElectricPump(Block block) {
        super(block, "TierElectricPump");
    }


    @Override
    public void setTierMachine(TileEntity tileEntity, ItemStack stack) {
        if (tileEntity instanceof TileEntityTierElectricPump tile) {
            tile.tier = MachineTier.get(getBaseTier(stack));
            tile.fluidTank.setCapacity(tile.tier.processes * TileEntityTierElectricPump.MAX_FLUID);
        }
    }


    @Override
    public void setFluidStack(FluidStack fluidStack, Object... data) {
        if (data[0] instanceof ItemStack itemStack) {
            ItemDataUtils.setStoredFluid(itemStack, "fluidTank", fluidStack, getCapacity(itemStack));
        }
    }

    @Override
    public FluidStack getFluidStack(Object... data) {
        if (data[0] instanceof ItemStack itemStack) {
            return ItemDataUtils.getStoredFluid(itemStack, "fluidTank");
        }
        return null;
    }

    @Override
    public boolean hasTank(Object... data) {
        return data[0] instanceof ItemStack stack && stack.getItem() instanceof ISustainedTank;
    }

    @Override
    public double getMachineStorage() {
        return BlockStateMachine.MachineType.ELECTRIC_PUMP.getStorage();
    }

    public FluidStack getFluid(ItemStack container) {
        return getFluidStack(container);
    }

    public int getCapacity(ItemStack container) {
        return MachineTier.get(getBaseTier(container)).processes * TileEntityTierElectricPump.MAX_FLUID;
    }

    public int fill(ItemStack container, FluidStack resource, boolean doFill) {
        return 0;
    }

    public FluidStack drain(ItemStack container, int maxDrain, boolean doDrain) {
        return null;
    }


}
