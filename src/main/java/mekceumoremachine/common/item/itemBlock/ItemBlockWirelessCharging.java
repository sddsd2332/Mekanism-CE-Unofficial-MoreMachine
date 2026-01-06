package mekceumoremachine.common.item.itemBlock;

import mekanism.common.Mekanism;
import mekanism.common.tier.InductionCellTier;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.machine.TileEntityWirelessChargingStation;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class ItemBlockWirelessCharging extends ItemBlockTierEnergyMachine {


    public ItemBlockWirelessCharging(Block block) {
        super(block, "WirelessChargingStation");
    }

    @Override
    void setTierMachine(TileEntity tileEntity, ItemStack stack) {
        if (tileEntity instanceof TileEntityWirelessChargingStation tile) {
            tile.tier = MachineTier.values()[getBaseTier(stack).ordinal()];
        }
    }


    @Override
    public void addOtherMachine(TileEntity tileEntity, ItemStack stack, World world) {
        super.addOtherMachine(tileEntity, stack, world);
        if (!world.isRemote) {
            if (tileEntity instanceof TileEntityWirelessChargingStation tile) {
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
