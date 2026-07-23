package mekceumoremachine.common.item.itemBlock;

import mekanism.common.Mekanism;
import mekanism.common.tile.prefab.TileEntityBasicBlock;
import mekceumoremachine.common.config.MoreMachineConfig;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.machine.TileEntityWirelessChargingEnergy;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.EntityLivingBase;
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
            tile.tier = MachineTier.get(getBaseTier(stack));
        }
    }


    @Override
    protected void restoreOtherPlacementData(ItemStack stack, EntityLivingBase placer, World world, BlockPos pos, TileEntityBasicBlock tileEntity) {
        if (!world.isRemote && tileEntity instanceof TileEntityWirelessChargingEnergy tile && placer.isSneaking()) {
            tile.setScanMachine();//通知机器进行首次扫描
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
    public double getMachineStorage(ItemStack stack) {
        return getWirelessStorage(stack);
    }

    @Override
    public double getMaxEnergy(ItemStack itemStack) {
        return getEnergyCapacity(itemStack);
    }

    private double getWirelessStorage(ItemStack itemStack) {
        if (itemStack.getCount() > 1) {
            return 0;
        }
        return switch (getBaseTier(itemStack)) {
            case BASIC -> MoreMachineConfig.current().config.BasicWirelessChargingMaxEnergy.val();
            case ADVANCED -> MoreMachineConfig.current().config.AdvancedWirelessChargingMaxEnergy.val();
            case ELITE -> MoreMachineConfig.current().config.EliteWirelessChargingMAXEnergy.val();
            case ULTIMATE -> MoreMachineConfig.current().config.UltimateWirelessChargingMaxEnergy.val();
            case CREATIVE -> 0;
        };
    }

}
