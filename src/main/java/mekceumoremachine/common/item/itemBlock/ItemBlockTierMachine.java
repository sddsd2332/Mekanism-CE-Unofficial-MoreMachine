package mekceumoremachine.common.item.itemBlock;

import mekanism.common.base.*;
import mekanism.common.config.MekanismConfig;
import mekanism.common.security.ISecurityItem;
import mekanism.common.security.ISecurityTile;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.prefab.TileEntityBasicBlock;
import mekanism.common.util.ItemDataUtils;
import mekanism.common.util.LangUtils;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.item.interfaces.IItemTipName;
import mekceumoremachine.common.tile.interfaces.ITierMachine;
import mekceumoremachine.common.util.MEKCeuMoreMachineUtils;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;

public abstract class ItemBlockTierMachine extends ItemBlockMekceuMoreMachine implements ISustainedInventory, ITierItem, ISecurityItem, IItemTipName {

    public String name;

    public ItemBlockTierMachine(Block block, String tierName) {
        super(block);
        name = tierName;
        setNoRepair();
        setCreativeTab(MEKCeuMoreMachine.tabMEKCeuMoreMachine);
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
    public boolean placeBlockAt(@Nonnull ItemStack stack, @Nonnull EntityPlayer player, World world, @Nonnull BlockPos pos, EnumFacing side, float hitX, float hitY,
                                float hitZ, @Nonnull IBlockState state) {
        if (stack.getCount() > 1 && MekanismConfig.current().mekce.StackingPlacementLimits.val()) {
            return false;
        }
        boolean place = true;
        Block block = world.getBlockState(pos).getBlock();
        if (!block.isReplaceable(world, pos)) {
            return false;
        }
        if (canPlace(stack, player, world, pos, side, hitX, hitY, hitZ, state)) {
            place = false;
        }

        if (place && super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, state)) {
            if (world.getTileEntity(pos) instanceof TileEntityBasicBlock tileEntity) {
                if (tileEntity instanceof ITierMachine<?>) {
                    setTierMachine(tileEntity, stack);
                }
                if (tileEntity instanceof ISecurityTile security) {
                    security.getSecurity().setOwnerUUID(getOwnerUUID(stack));
                    if (hasSecurity(stack)) {
                        security.getSecurity().setMode(getSecurity(stack));
                    }
                    if (getOwnerUUID(stack) == null) {
                        security.getSecurity().setOwnerUUID(player.getUniqueID());
                    }
                }
                if (tileEntity instanceof IUpgradeTile upgradeTile) {
                    if (ItemDataUtils.hasData(stack, "upgrades")) {
                        upgradeTile.getComponent().read(ItemDataUtils.getDataMap(stack));
                    }
                }
                if (tileEntity instanceof ISideConfiguration config) {
                    if (ItemDataUtils.hasData(stack, "sideDataStored")) {
                        config.getConfig().read(ItemDataUtils.getDataMap(stack));
                        config.getEjector().read(ItemDataUtils.getDataMap(stack));
                    }
                }
                if (tileEntity instanceof ISustainedData data) {
                    if (stack.getTagCompound() != null) {
                        data.readSustainedData(stack);
                    }
                }
                if (tileEntity instanceof IRedstoneControl redstoneControl) {
                    if (ItemDataUtils.hasData(stack, "controlType")) {
                        redstoneControl.setControlType(IRedstoneControl.RedstoneControl.values()[ItemDataUtils.getInt(stack, "controlType")]);
                    }
                }
                if (tileEntity instanceof ISustainedInventory inventory) {
                    inventory.setInventory(getInventory(stack));
                }
                addOtherMachine(tileEntity, stack, world);
            }
            return true;
        }
        return false;
    }

    abstract void setTierMachine(TileEntity tileEntity, ItemStack stack);


    public void addOtherMachine(TileEntity tileEntity, ItemStack stack, World world) {
    }

    public boolean canPlace(@Nonnull ItemStack stack, @Nonnull EntityPlayer player, World world, @Nonnull BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, @Nonnull IBlockState state) {
        return false;
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

    public  String getItemName(){
        return name;
    }

}
