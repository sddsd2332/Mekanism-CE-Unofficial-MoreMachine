package mekceumoremachine.common.block;

import mekceumoremachine.common.block.states.BlockStateTierMachine;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.tile.machine.TileEntityWirelessChargingStation;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.jetbrains.annotations.NotNull;

public class BlockWirelessCharging extends BlockTierMachine {

    public BlockWirelessCharging() {
        super();
    }

    @Override
    protected BlockPos[] getStructureOffsets() {
        return MachineStructureOffsets.TWO_ABOVE;
    }


    @Override
    public IBlockState AddActualState(@NotNull IBlockState state, IBlockAccess worldIn, BlockPos pos, TileEntity tile) {
        if (tile instanceof TileEntityWirelessChargingStation tiers) {
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
        return 0;
    }

    @Override
    public TileEntity getTileEntity() {
        return new TileEntityWirelessChargingStation();
    }


    @Override
    public Block getMachineBlock() {
        return MEKCeuMoreMachineBlocks.WirelessCharging;
    }


}
