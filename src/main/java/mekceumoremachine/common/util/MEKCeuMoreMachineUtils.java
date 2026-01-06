package mekceumoremachine.common.util;

import mekanism.api.EnumColor;
import mekanism.api.IMekWrench;
import mekanism.api.energy.IEnergizedItem;
import mekanism.api.energy.IStrictEnergyStorage;
import mekanism.client.MekKeyHandler;
import mekanism.client.MekanismClient;
import mekanism.client.MekanismKeyHandler;
import mekanism.common.Upgrade;
import mekanism.common.base.*;
import mekanism.common.integration.wrenches.Wrenches;
import mekanism.common.security.ISecurityItem;
import mekanism.common.security.ISecurityTile;
import mekanism.common.tile.prefab.TileEntityBasicBlock;
import mekanism.common.tile.prefab.TileEntityContainerBlock;
import mekanism.common.util.ItemDataUtils;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.SecurityUtils;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.item.interfaces.IItemTipName;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.interfaces.ITierMachine;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.List;

public class MEKCeuMoreMachineUtils {

    public static void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        if (world.getTileEntity(pos) instanceof TileEntityBasicBlock tileEntity) {
            EnumFacing change = EnumFacing.SOUTH;
            if (tileEntity.canSetFacing(EnumFacing.DOWN) && tileEntity.canSetFacing(EnumFacing.UP)) {
                int height = Math.round(placer.rotationPitch);
                if (height >= 65) {
                    change = EnumFacing.UP;
                } else if (height <= -65) {
                    change = EnumFacing.DOWN;
                }
            }

            if (change != EnumFacing.DOWN && change != EnumFacing.UP) {
                int side = MathHelper.floor((double) (placer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
                change = switch (side) {
                    case 0 -> EnumFacing.NORTH;
                    case 1 -> EnumFacing.EAST;
                    case 2 -> EnumFacing.SOUTH;
                    case 3 -> EnumFacing.WEST;
                    default -> change;
                };
            }

            tileEntity.setFacing(change);
            tileEntity.redstone = world.getRedstonePowerFromNeighbors(pos) > 0;
            if (tileEntity instanceof IBoundingBlock block) {
                block.onPlace();
            }
        }
    }

    public static void breakBlock(World world, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
        if (world.getTileEntity(pos) instanceof TileEntityBasicBlock tileEntity) {
            if (tileEntity instanceof IBoundingBlock block) {
                block.onBreak();
            }
        }
    }

    public static void getSubBlocks(Block block, NonNullList<ItemStack> list, boolean charged) {
        for (MachineTier tier : MachineTier.values()) {
            ItemStack addItem = new ItemStack(block);
            if (addItem.getItem() instanceof ITierItem item) {
                item.setBaseTier(addItem, tier.getBaseTier());
            }
            list.add(addItem);
            if (charged) {
                ItemStack addChargedItem = new ItemStack(block);
                if (addChargedItem.getItem() instanceof ITierItem item) {
                    item.setBaseTier(addChargedItem, tier.getBaseTier());
                }
                if (addChargedItem.getItem() instanceof IEnergizedItem item) {
                    item.setEnergy(addChargedItem, item.getMaxEnergy(addChargedItem));
                }
                list.add(addChargedItem);
            }
        }
    }

    public static boolean onBlockActivated(Block block, int guiId, World world, BlockPos pos, IBlockState state, EntityPlayer entityplayer, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }
        if (world.getTileEntity(pos) instanceof TileEntityBasicBlock tileEntity) {
            ItemStack stack = entityplayer.getHeldItem(hand);
            if (!stack.isEmpty()) {
                IMekWrench wrenchHandler = Wrenches.getHandler(stack);
                if (wrenchHandler != null) {
                    RayTraceResult raytrace = new RayTraceResult(new Vec3d(hitX, hitY, hitZ), side, pos);
                    if (wrenchHandler.canUseWrench(entityplayer, hand, stack, raytrace)) {
                        if (SecurityUtils.canAccess(entityplayer, tileEntity)) {
                            wrenchHandler.wrenchUsed(entityplayer, hand, stack, raytrace);
                            if (entityplayer.isSneaking()) {
                                MekanismUtils.dismantleBlock(block, state, world, pos);
                                return true;
                            }
                            tileEntity.setFacing(tileEntity.facing.rotateAround(side.getAxis()));
                            world.notifyNeighborsOfStateChange(pos, block, true);
                        } else {
                            SecurityUtils.displayNoAccess(entityplayer);
                        }
                        return true;
                    }
                }
            }

            if (!entityplayer.isSneaking() && guiId >= 0) {
                if (SecurityUtils.canAccess(entityplayer, tileEntity)) {
                    entityplayer.openGui(MEKCeuMoreMachine.instance, guiId, world, pos.getX(), pos.getY(), pos.getZ());
                } else {
                    SecurityUtils.displayNoAccess(entityplayer);
                }
                return true;
            }

        }
        return false;
    }

    public static ItemStack getDropItem(Block block, @Nonnull IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos) {
        ItemStack itemStack = new ItemStack(block, 1, state.getBlock().getMetaFromState(state));
        if (itemStack.getTagCompound() == null) {
            itemStack.setTagCompound(new NBTTagCompound());
        }
        if (world.getTileEntity(pos) instanceof TileEntityBasicBlock tileEntity) {
            if (tileEntity instanceof ITierMachine<?> tierMachine) {
                if (itemStack.getItem() instanceof ITierItem tierItem) {
                    tierItem.setBaseTier(itemStack, tierMachine.getTier().getBaseTier());
                }
            }
            if (tileEntity instanceof ISecurityTile securityTile) {
                if (itemStack.getItem() instanceof ISecurityItem securityItem) {
                    if (securityItem.hasSecurity(itemStack)) {
                        securityItem.setOwnerUUID(itemStack, securityTile.getSecurity().getOwnerUUID());
                        securityItem.setSecurity(itemStack, securityTile.getSecurity().getMode());
                    }
                }
            }
            if (tileEntity instanceof IUpgradeTile upgradeTile) {
                upgradeTile.getComponent().write(ItemDataUtils.getDataMap(itemStack));
            }
            if (tileEntity instanceof ISideConfiguration config) {
                config.getConfig().write(ItemDataUtils.getDataMap(itemStack));
                config.getEjector().write(ItemDataUtils.getDataMap(itemStack));
            }
            if (tileEntity instanceof ISustainedData data) {
                data.writeSustainedData(itemStack);
            }
            if (tileEntity instanceof IRedstoneControl control) {
                ItemDataUtils.setInt(itemStack, "controlType", control.getControlType().ordinal());
            }
            if (tileEntity instanceof TileEntityContainerBlock containerBlock && !containerBlock.inventory.isEmpty()) {
                if (itemStack.getItem() instanceof ISustainedInventory inventory) {
                    inventory.setInventory(containerBlock.getInventory(), itemStack);
                }
            }
            if (tileEntity instanceof ISustainedTank tank) {
                if (itemStack.getItem() instanceof ISustainedTank itemTank && itemTank.hasTank(itemStack)) {
                    if (tank.getFluidStack() != null) {
                        itemTank.setFluidStack(tank.getFluidStack(), itemStack);
                    }
                }
            }
            if (tileEntity instanceof IStrictEnergyStorage storage) {
                if (itemStack.getItem() instanceof IEnergizedItem energizedItem) {
                    energizedItem.setEnergy(itemStack, storage.getEnergy());
                }
            }
        }
        return itemStack;
    }

    public static int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        if (world.getTileEntity(pos) instanceof IComparatorSupport support) {
            return support.getRedstoneLevel();
        }
        return 0;
    }

    public static EnumFacing[] getValidRotations(World world, @Nonnull BlockPos pos) {
        EnumFacing[] valid = new EnumFacing[6];
        if (world.getTileEntity(pos) instanceof TileEntityBasicBlock basicTile) {
            for (EnumFacing dir : EnumFacing.VALUES) {
                if (basicTile.canSetFacing(dir)) {
                    valid[dir.ordinal()] = dir;
                }
            }
        }
        return valid;
    }

    public static boolean rotateBlock(World world, @Nonnull BlockPos pos, @Nonnull EnumFacing axis) {
        if (world.getTileEntity(pos) instanceof TileEntityBasicBlock basicTile) {
            if (basicTile.canSetFacing(axis)) {
                basicTile.setFacing(axis);
                return true;
            }
        }
        return false;
    }



}
