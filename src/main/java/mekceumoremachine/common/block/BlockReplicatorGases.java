package mekceumoremachine.common.block;

import mekanism.common.block.states.BlockStateFacing;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.tile.machine.replicator.TileEntityReplicatorGases;
import mekceumoremachine.common.util.MEKCeuMoreMachineUtils;
import net.minecraft.block.Block;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public class BlockReplicatorGases extends BlockTierMachine {

    public BlockReplicatorGases() {
        super();
    }

    @Override
    public void getSubBlocks(CreativeTabs creativetabs, NonNullList<ItemStack> list) {
        MEKCeuMoreMachineUtils.getSubBlocks(getBlock(),list,false);
    }

    @Nonnull
    @Override
    public BlockStateContainer createBlockState() {
        return new BlockStateFacing(this);
    }

    @Override
    public IBlockState AddActualState(@NotNull IBlockState state, IBlockAccess worldIn, BlockPos pos, TileEntity tile) {
        return state;
    }

    @Override
    public Block getBlock() {
        return this;
    }


    @Override
    public int getGuiID() {
        return 15;
    }


    @Override
    public TileEntity getTileEntity() {
        return new TileEntityReplicatorGases();
    }

    @Override
    public Block getMachineBlock() {
        return MEKCeuMoreMachineBlocks.ReplicatorGases;
    }
}
