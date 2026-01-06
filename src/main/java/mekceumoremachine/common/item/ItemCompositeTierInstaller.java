package mekceumoremachine.common.item;

import mekanism.common.base.ITierUpgradeable;
import mekanism.common.config.MekanismConfig;
import mekanism.common.item.ItemMekanism;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.prefab.TileEntityBasicBlock;
import mekanism.common.util.LangUtils;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.tile.interfaces.INeedRepeatTierUpgrade;
import mekceumoremachine.common.tile.interfaces.ITierFirstUpgrade;
import mekceumoremachine.common.tile.interfaces.ITierMachine;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.List;

public class ItemCompositeTierInstaller extends ItemMekanism {

    public ItemCompositeTierInstaller() {
        super();
        setMaxStackSize(MekanismConfig.current().mekce.MAXTierSize.val());
        setCreativeTab(MEKCeuMoreMachine.tabMEKCeuMoreMachine);
    }


    @Nonnull
    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand) {
        if (world.isRemote) {
            return EnumActionResult.PASS;
        }
        TileEntity firstTile = world.getTileEntity(pos);
        ItemStack stack = player.getHeldItem(hand);

        if (firstTile instanceof TileEntityBasicBlock basicBlock && !basicBlock.playersUsing.isEmpty()) {
            return EnumActionResult.FAIL;
        }
        //如果机器是首次升级
        //升级到基础
        if (firstTile instanceof ITierFirstUpgrade && firstTile instanceof ITierUpgradeable upgradeable) {
            if (upgradeable.CanInstalled()) {
                upgradeable.upgrade(BaseTier.BASIC);
            }
        }
        //重新获取该机器
        TileEntity tile = world.getTileEntity(pos);

        if (tile instanceof TileEntityBasicBlock basicBlock && !basicBlock.playersUsing.isEmpty()) {
            return EnumActionResult.FAIL;
        }
        //如果该方块是工厂
        if (tile instanceof INeedRepeatTierUpgrade<?> factory) {
            //如果工厂已经是终极或者创造了
            if (factory.getNowTier().getBaseTier() == BaseTier.ULTIMATE || factory.getNowTier().getBaseTier() == BaseTier.CREATIVE) {
                return EnumActionResult.PASS;
            }
            for (BaseTier tier : BaseTier.values()) {
                if (tier == BaseTier.BASIC) {
                    continue;
                }
                //工厂需要重复获取
                if (world.getTileEntity(pos) instanceof INeedRepeatTierUpgrade<?> machine) {
                    if (machine.getNowTier().getBaseTier() == BaseTier.ULTIMATE || machine.getNowTier().getBaseTier() == BaseTier.CREATIVE) {
                        break;
                    }
                    if (tier.ordinal() != machine.getNowTier().getBaseTier().ordinal() + 1) {
                        continue;
                    }
                    machine.upgrade(tier);
                }
            }
            //最后检查工厂是否是终极等级，如果是则清除
            if (world.getTileEntity(pos) instanceof INeedRepeatTierUpgrade<?> machine) {
                if (!player.capabilities.isCreativeMode && machine.getNowTier().getBaseTier() == BaseTier.ULTIMATE) {
                    stack.shrink(1);
                }
            }
            return EnumActionResult.SUCCESS;

        } else if (tile instanceof ITierMachine<?> upgradeable) {
            if (upgradeable.CanInstalled()) {
                for (BaseTier tier : BaseTier.values()) {
                    //获取机器的等级
                    BaseTier machineTier = upgradeable.getTier().getBaseTier();
                    //如果机器的等级是终极或者创造，结束升级
                    if (machineTier == BaseTier.ULTIMATE || machineTier == BaseTier.CREATIVE) {
                        break;
                    }
                    if (tier.ordinal() != machineTier.ordinal() + 1) {
                        continue;
                    }
                    upgradeable.upgrade(tier);
                }
                if (!player.capabilities.isCreativeMode && upgradeable.getTier().getBaseTier() == BaseTier.ULTIMATE) {
                    stack.shrink(1);
                }
                return EnumActionResult.SUCCESS;
            }
            return EnumActionResult.PASS;
        }
        return EnumActionResult.PASS;
    }


    @Nonnull
    @Override
    public String getTranslationKey(ItemStack stack) {
        return "item.CompositeTierInstaller";
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(@Nonnull ItemStack itemstack, World world, @Nonnull List<String> list, @Nonnull ITooltipFlag flag) {
        list.add(LangUtils.localize("tooltip.CompositeTierInstaller"));
    }

}
