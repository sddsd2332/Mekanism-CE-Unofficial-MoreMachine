package mekceumoremachine.common.item.itemBlock;

import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.machine.TileEntityTierSolarNeutronActivator;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class ItemBlockTierSolarNeutronActivator extends ItemBlockTierMachine {

    public ItemBlockTierSolarNeutronActivator(Block block) {
        super(block, "TierSolarNeutronActivator");
    }

    @Override
    public void setTierMachine(TileEntity tileEntity, ItemStack stack) {
        if (tileEntity instanceof TileEntityTierSolarNeutronActivator tile) {
            tile.tier = MachineTier.get(getBaseTier(stack));
            tile.inputTank.setMaxGas(tile.tier.processes * TileEntityTierSolarNeutronActivator.MAX_GAS);
            tile.outputTank.setMaxGas(tile.tier.processes * TileEntityTierSolarNeutronActivator.MAX_GAS);
        }
    }

    @Override
    public boolean canPlace(@Nonnull ItemStack stack, @Nonnull EntityPlayer player, World world, @Nonnull BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, @Nonnull IBlockState state) {
        BlockPos abovePos = pos.up();
        return !world.isValid(abovePos) || !world.getBlockState(abovePos).getBlock().isReplaceable(world, abovePos);
    }

}
