package mekceumoremachine.common.item.itemBlock;

import mekanism.common.block.states.BlockStateMachine;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.machine.TileEntityTierIsotopicCentrifuge;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class ItemBlockTierIsotopicCentrifuge extends ItemBlockMekceuMoreMachineTier {

    public ItemBlockTierIsotopicCentrifuge(Block block) {
        super(block, "TierIsotopicCentrifuge");
    }

    @Override
    public void setTierMachine(TileEntity tileEntity, ItemStack stack) {
        if (tileEntity instanceof TileEntityTierIsotopicCentrifuge tile) {
            tile.tier = MachineTier.values()[getBaseTier(stack).ordinal()];
            tile.inputTank.setMaxGas(tile.tier.processes * TileEntityTierIsotopicCentrifuge.MAX_GAS);
            tile.outputTank.setMaxGas(tile.tier.processes * TileEntityTierIsotopicCentrifuge.MAX_GAS);
        }
    }

    @Override
    public boolean canPlace(@Nonnull ItemStack stack, @Nonnull EntityPlayer player, World world, @Nonnull BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, @Nonnull IBlockState state) {
        BlockPos abovePos = pos.up();
        return !world.isValid(abovePos) || !world.getBlockState(abovePos).getBlock().isReplaceable(world, abovePos);
    }

    @Override
    public double getMachineStorage() {
        return BlockStateMachine.MachineType.ISOTOPIC_CENTRIFUGE.getStorage();
    }
}
