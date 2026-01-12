package mekceumoremachine.common.item.itemBlock;

import mekanism.common.Mekanism;
import mekanism.common.tier.InductionCellTier;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.machine.TileEntityWirelessChargingEnergy;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class ItemBlockWirelessEnergy extends ItemBlockMekceuMoreMachineTier {


    public ItemBlockWirelessEnergy(Block block) {
        super(block, "WirelessChargingEnergy");
    }

    @Override
    void setTierMachine(TileEntity tileEntity, ItemStack stack) {
        if (tileEntity instanceof TileEntityWirelessChargingEnergy tile) {
            tile.tier = MachineTier.values()[getBaseTier(stack).ordinal()];
        }
    }


    @Override
    public void addOtherMachine(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, IBlockState state, TileEntity tileEntity) {
        super.addOtherMachine(stack, player, world, pos, side, hitX, hitY, hitZ, state, tileEntity);
        if (!world.isRemote) {
            if (tileEntity instanceof TileEntityWirelessChargingEnergy tile) {
                Mekanism.packetHandler.sendUpdatePacket(tile);
            }
        }
    }


    @Override
    public boolean canPlace(@Nonnull ItemStack stack, @Nonnull EntityPlayer player, World world, @Nonnull BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, @Nonnull IBlockState state) {
        for (int yPos = 1; yPos <= 2; yPos++) {
            BlockPos abovePos = pos.up(yPos);
            if (!world.isValid(abovePos) || !world.getBlockState(abovePos).getBlock().isReplaceable(world, abovePos)) {
                return true;
            }
        }
        return false;
    }


    @Override
    public double getMachineStorage() {
        return 0;
    }

    @Override
    public double getMaxEnergy(ItemStack itemStack) {
        if (itemStack.getCount() > 1) {
            return 0;
        }
        return InductionCellTier.values()[getBaseTier(itemStack).ordinal()].getMaxEnergy();
    }

}
