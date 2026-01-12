package mekceumoremachine.common.block;

import mekceumoremachine.common.block.states.BlockStateTierMachine;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.tile.machine.TileEntityWirelessChargingEnergy;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.jetbrains.annotations.NotNull;

public class BlockWirelessEnergy extends BlockTierMachine {

    public BlockWirelessEnergy() {
        super();
    }


    @Override
    public IBlockState AddActualState(@NotNull IBlockState state, IBlockAccess worldIn, BlockPos pos, TileEntity tile) {
        if (tile instanceof TileEntityWirelessChargingEnergy tiers) {
            if (tiers.tier != null) {
                state = state.withProperty(BlockStateTierMachine.typeProperty, tiers.tier);
            }
        }
        return state;
    }

    @Override
    public Block getBlock() {
        return this;
    }


    @Override
    public boolean canCharged() {
        return true;
    }

    @Override
    public int getGuiID() {
        return 17;
    }

    @Override
    public TileEntity getTileEntity() {
        return new TileEntityWirelessChargingEnergy();
    }


    @Override
    public Block getMachineBlock() {
        return MEKCeuMoreMachineBlocks.WirelessEnergy;
    }


}
