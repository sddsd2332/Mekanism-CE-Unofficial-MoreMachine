package mekceumoremachine.common.item;

import mekanism.common.config.MekanismConfig;
import mekanism.common.item.ItemMekanism;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.prefab.TileEntityBasicBlock;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.util.LangUtils;
import mekanism.common.util.UpgradeUtils;
import mekceumoremachine.common.MEKCeuMoreMachine;
import net.minecraft.block.state.IBlockState;
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

    private static final BaseTier[] INSTALL_ORDER = {
          BaseTier.BASIC, BaseTier.ADVANCED, BaseTier.ELITE, BaseTier.ULTIMATE
    };

    public ItemCompositeTierInstaller() {
        super();
        setMaxStackSize(MekanismConfig.current().mekce.MAXTierSize.val());
        setCreativeTab(MEKCeuMoreMachine.tabMEKCeuMoreMachine);
    }

    @Nonnull
    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX,
          float hitY, float hitZ, EnumHand hand) {
        if (world.isRemote) {
            return EnumActionResult.PASS;
        }
        TileEntity tile = world.getTileEntity(pos);
        if (isBusy(tile) || !UpgradeUtils.isUpgradeable(tile)) {
            return isBusy(tile) ? EnumActionResult.FAIL : EnumActionResult.PASS;
        }

        boolean upgraded = false;
        for (BaseTier tier : INSTALL_ORDER) {
            tile = world.getTileEntity(pos);
            if (!UpgradeUtils.canInstallUpgrade(tile, tier)) {
                continue;
            }
            if (!installUpgrade(tile, tier)) {
                break;
            }
            upgraded = true;
        }
        if (!upgraded) {
            return EnumActionResult.PASS;
        }
        if (!player.capabilities.isCreativeMode) {
            player.getHeldItem(hand).shrink(1);
        }
        return EnumActionResult.SUCCESS;
    }

    private static boolean isBusy(TileEntity tile) {
        return tile instanceof TileEntityBasicBlock basicBlock && !basicBlock.playersUsing.isEmpty();
    }

    private static boolean installUpgrade(TileEntity tile, BaseTier tier) {
        IUpgradeData upgradeData = UpgradeUtils.getUpgradeData(tile, tier);
        IBlockState upgradeResult = UpgradeUtils.getUpgradeResult(tile, tier);
        return upgradeData != null && (upgradeResult == null ? UpgradeUtils.parseUpgradeData(tile, upgradeData) :
              UpgradeUtils.replaceTileForUpgrade(tile, upgradeResult, upgradeData));
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
