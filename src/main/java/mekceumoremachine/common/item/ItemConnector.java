package mekceumoremachine.common.item;

import mekanism.api.Coord4D;
import mekanism.api.EnumColor;
import mekanism.common.Mekanism;
import mekanism.common.item.ItemEnergized;
import mekanism.common.util.ItemDataUtils;
import mekanism.common.util.LangUtils;
import mekanism.common.util.WorldUtils;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.tile.interfaces.ITileConnect;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.List;

public class ItemConnector extends ItemEnergized {

    public final int ENERGY_PER_CONFIGURE = 400;

    public ItemConnector() {
        super(60000);
        setRarity(EnumRarity.UNCOMMON);
        setMaxStackSize(1);
        /*
        this.addPropertyOverride(new ResourceLocation("mode"), new IItemPropertyGetter() {
            @SideOnly(Side.CLIENT)
            public float apply(ItemStack stack, @Nullable World worldIn, @Nullable EntityLivingBase entityIn) {
                return getEnergy(stack) > 0 ? 1.0F : 0.0F;
            }
        });
         */
        setCreativeTab(MEKCeuMoreMachine.tabMEKCeuMoreMachine);
    }


    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack itemstack, World world, List<String> list, ITooltipFlag flag) {
        super.addInformation(itemstack, world, list, flag);
        if (getEnergy(itemstack) >= ENERGY_PER_CONFIGURE) {
            Coord4D data = getDataType(itemstack);
            if (data != null) {
                list.add(EnumColor.ORANGE + LangUtils.localize("tooltip.connector.detail"));
                list.add(EnumColor.ORANGE + LangUtils.localize("tooltip.connector.dimension") + ": " + data.dimensionId);
                list.add(EnumColor.ORANGE + LangUtils.localize("tooltip.connector.pos") + ": [" + data.x + "," + data.y + "," + data.z + "]");
            } else {
                list.add(EnumColor.RED + LangUtils.localize("tooltip.connector.no_got"));
            }
        }
    }

    @Nonnull
    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile == null) {
            return EnumActionResult.PASS;
        }
        ItemStack stack = player.getHeldItem(hand);
        if (player.isSneaking()) {
            //如果机器没有这个接口
            if (!(tile instanceof ITileConnect)) {
                return EnumActionResult.PASS;
            }
            if (getEnergy(stack) >= ENERGY_PER_CONFIGURE) {
                if (!world.isRemote) {
                    NBTTagCompound data = getBaseData(tile);
                    if (!data.isEmpty()) {
                        if (!player.isCreative()) {
                            // consume energy for configuration (handled by helper)
                            consumeConfigureEnergy(stack, player);
                        }
                        setData(stack, data);
                        sendServerMessage(world, player, EnumColor.GREY, "tooltip.connector.got");
                    }
                }
            } else {
                sendServerMessage(world, player, EnumColor.RED, "tooltip.connector.no_energy");
                return EnumActionResult.FAIL;
            }
        } else {
            Coord4D data = getDataType(stack);
            if (data == null) {
                return EnumActionResult.PASS;
            }
            //跨维度不可用
            if (world.provider.getDimension() != data.dimensionId) {
                sendServerMessage(world, player, EnumColor.RED, "tooltip.connector.across_dimension");
                return EnumActionResult.PASS;
            }
            //绑定的方块相同
            if (pos == data.getPos()) {
                sendServerMessage(world, player, EnumColor.RED, "tooltip.connector.self");
                return EnumActionResult.PASS;
            }

            // 获取保存位置的方块实体
            if (WorldUtils.getTileEntity(world, data.getPos()) instanceof ITileConnect linkTile) {
                if (world.isBlockLoaded(linkTile.getPosition().getPos())) {
                    if (getEnergy(stack) >= ENERGY_PER_CONFIGURE) {
                        //如果是客户端直接返回成功，等服务端处理完后会同步数据到客户端，客户端再刷新显示数据
                        //主要是是防止IC2的获取机器报错
                        if (world.isRemote) {
                            return EnumActionResult.SUCCESS;
                        }
                        switch (linkTile.connectOrCut(tile, side, player)) {
                            case CONNECT, DISCONNECT -> {
                                consumeConfigureEnergy(stack, player);
                                return EnumActionResult.SUCCESS;
                            }
                            case CONNECT_FAIL -> {
                                return EnumActionResult.FAIL;
                            }
                        }
                    } else {
                        sendServerMessage(world, player, EnumColor.RED, "tooltip.connector.no_energy");
                        return EnumActionResult.PASS;
                    }
                } else {
                    sendServerMessage(world, player, EnumColor.RED, "tooltip.connector.fail_chunk");
                    return EnumActionResult.PASS;
                }
            } else {
                //发送当前绑定的机器不存在
                sendServerMessage(world, player, EnumColor.RED, "tooltip.connector.fail_machine");
                return EnumActionResult.FAIL;
            }
        }
        return EnumActionResult.SUCCESS;
    }


    private NBTTagCompound getBaseData(TileEntity tile) {
        NBTTagCompound nbtTags = new NBTTagCompound();
        if (tile instanceof ITileConnect connect) {
            connect.getPosition().write(nbtTags);
        }
        return nbtTags;
    }

    // Helper to consume configuration energy if player is not in creative mode
    private void consumeConfigureEnergy(ItemStack stack, EntityPlayer player) {
        if (!player.isCreative()) {
            setEnergy(stack, getEnergy(stack) - ENERGY_PER_CONFIGURE);
        }
    }

    // Helper: send a localized message from server side only (avoids client-side calls)
    private void sendServerMessage(World world, EntityPlayer player, EnumColor color, String localizationKey) {
        if (!world.isRemote) {
            player.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + Mekanism.LOG_TAG + " " + color + LangUtils.localize(localizationKey)));
        }
    }

    public void setData(ItemStack itemstack, NBTTagCompound data) {
        if (data != null) {
            ItemDataUtils.setCompound(itemstack, "data", data);
        } else {
            ItemDataUtils.removeData(itemstack, "data");
        }
    }


    public Coord4D getDataType(ItemStack itemstack) {
        NBTTagCompound data = getData(itemstack);
        if (data != null) {
            return Coord4D.read(data);
        }
        return null;
    }

    public NBTTagCompound getData(ItemStack itemstack) {
        NBTTagCompound data = ItemDataUtils.getCompound(itemstack, "data");
        if (data.isEmpty()) {
            return null;
        }
        return ItemDataUtils.getCompound(itemstack, "data");
    }

    //右键空气清除坐标
    @Nonnull
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, @Nonnull EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (player.isSneaking()) {
            if (!world.isRemote) {
                setData(stack, null);
                //发送成功清除数据的消息
                player.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + Mekanism.LOG_TAG + " " + EnumColor.GREY + LangUtils.localize("tooltip.connector.cleared") + "."));
            }
            // Always return success on both sides so the action is considered handled client-side too.
            return ActionResult.newResult(EnumActionResult.SUCCESS, stack);
        }
        return super.onItemRightClick(world, player, hand);
    }


}
