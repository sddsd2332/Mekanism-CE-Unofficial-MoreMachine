package mekceumoremachine.common.item.itemBlock;

import mekanism.common.base.ITierItem;
import mekanism.common.tier.BaseTier;
import mekanism.common.util.LangUtils;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.interfaces.ITierMachine;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public abstract class ItemBlockMekceuMoreMachineTier extends ItemBlockMekceuMoreMachineEnergy implements ITierItem {


    public ItemBlockMekceuMoreMachineTier(Block block, String tierName) {
        super(block, tierName);
    }


    @Nonnull
    @Override
    public String getTranslationKey(ItemStack itemstack) {
        return getTranslationKey() + "." + getBaseTier(itemstack).getSimpleName();
    }

    @Nonnull
    @Override
    public String getItemStackDisplayName(@Nonnull ItemStack itemstack) {
        return getBaseTier(itemstack).getColor() + LangUtils.localize("tile." + name + "." + getBaseTier(itemstack).getSimpleName() + ".name");
    }


    @Override
    public double getMachineStorage(ItemStack stack) {
        return getMachineStorage() * MachineTier.values()[getBaseTier(stack).ordinal()].processes;
    }

    abstract double getMachineStorage();

    public void addOtherMachine(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, IBlockState state, TileEntity tileEntity) {
        super.addOtherMachine(stack, player, world, pos, side, hitX, hitY, hitZ, state, tileEntity);
        if (tileEntity instanceof ITierMachine<?>) {
            setTierMachine(tileEntity, stack);
        }
    }

    @Override
    public BaseTier getBaseTier(ItemStack itemstack) {
        if (!itemstack.hasTagCompound()) {
            return BaseTier.BASIC;
        }
        return BaseTier.values()[itemstack.getTagCompound().getInteger("tier")];
    }

    @Override
    public void setBaseTier(ItemStack itemstack, BaseTier tier) {
        if (!itemstack.hasTagCompound()) {
            itemstack.setTagCompound(new NBTTagCompound());
        }
        itemstack.getTagCompound().setInteger("tier", tier.ordinal());
    }

    abstract void setTierMachine(TileEntity tileEntity, ItemStack stack);
}
