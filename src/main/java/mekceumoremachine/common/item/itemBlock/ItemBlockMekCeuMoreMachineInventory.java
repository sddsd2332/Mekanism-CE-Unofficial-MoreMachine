package mekceumoremachine.common.item.itemBlock;

import mekanism.common.base.ISustainedInventory;
import mekanism.common.config.MekanismConfig;
import mekanism.common.security.ISecurityItem;
import mekanism.common.security.ISecurityTile;
import mekanism.common.util.ItemDataUtils;
import mekceumoremachine.common.item.interfaces.IItemTipName;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.UUID;

public abstract class ItemBlockMekCeuMoreMachineInventory extends ItemBlockMekceuMoreMachine implements ISustainedInventory, ISecurityItem, IItemTipName {


    public String name;

    public ItemBlockMekCeuMoreMachineInventory(Block block, String tierName) {
        super(block);
        setNoRepair();
        name = tierName;
    }

    @Override
    public void addOtherMachine(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, IBlockState state , TileEntity tileEntity) {
        super.addOtherMachine(stack,player,world,pos,side,hitX,hitY,hitZ,state,tileEntity);
        if (tileEntity instanceof ISecurityTile security) {
            security.getSecurity().setOwnerUUID(getOwnerUUID(stack));
            if (hasSecurity(stack)) {
                security.getSecurity().setMode(getSecurity(stack));
            }
            if (getOwnerUUID(stack) == null) {
                security.getSecurity().setOwnerUUID(player.getUniqueID());
            }
        }
        if (tileEntity instanceof ISustainedInventory inventory) {
            inventory.setInventory(getInventory(stack));
        }
    }

    @Override
    public void setInventory(NBTTagList nbtTags, Object... data) {
        if (data[0] instanceof ItemStack stack) {
            ItemDataUtils.setList(stack, "Items", nbtTags);
        }
    }

    @Override
    public NBTTagList getInventory(Object... data) {
        if (data[0] instanceof ItemStack stack) {
            return ItemDataUtils.getList(stack, "Items");
        }
        return null;
    }


    @Override
    public UUID getOwnerUUID(ItemStack stack) {
        if (ItemDataUtils.hasData(stack, "ownerUUID")) {
            return UUID.fromString(ItemDataUtils.getString(stack, "ownerUUID"));
        }
        return null;
    }

    @Override
    public void setOwnerUUID(ItemStack stack, UUID owner) {
        if (owner == null) {
            ItemDataUtils.removeData(stack, "ownerUUID");
        } else {
            ItemDataUtils.setString(stack, "ownerUUID", owner.toString());
        }
    }

    @Override
    public ISecurityTile.SecurityMode getSecurity(ItemStack stack) {
        if (!MekanismConfig.current().general.allowProtection.val()) {
            return ISecurityTile.SecurityMode.PUBLIC;
        }
        return ISecurityTile.SecurityMode.values()[ItemDataUtils.getInt(stack, "security")];
    }

    @Override
    public void setSecurity(ItemStack stack, ISecurityTile.SecurityMode mode) {
        if (getOwnerUUID(stack) == null) {
            ItemDataUtils.removeData(stack, "security");
        } else {
            ItemDataUtils.setInt(stack, "security", mode.ordinal());
        }
    }

    @Override
    public boolean hasSecurity(ItemStack stack) {
        return true;
    }

    @Override
    public boolean hasOwner(ItemStack stack) {
        return hasSecurity(stack);
    }

    public String getItemName() {
        return name;
    }

}
