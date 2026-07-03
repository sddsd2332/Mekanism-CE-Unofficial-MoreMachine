package mekceumoremachine.common.item;

import mekanism.common.base.IUpgradeableTile;
import mekanism.common.config.MekanismConfig;
import mekanism.common.item.ItemMekanism;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.prefab.TileEntityBasicBlock;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.util.LangUtils;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.tile.interfaces.ILargeMachine;
import mekceumoremachine.common.util.MEKCeuMoreMachineUpgradeUtils;
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

public class ItemLargeMachineryUpgradeComponents extends ItemMekanism {

    public ItemLargeMachineryUpgradeComponents() {
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
        TileEntity tile = world.getTileEntity(pos);
        ItemStack stack = player.getHeldItem(hand);
        if (tile instanceof ILargeMachine largeMachine && tile instanceof IUpgradeableTile upgradeable) {
            if (tile instanceof TileEntityBasicBlock basicBlock && !basicBlock.playersUsing.isEmpty()) {
                return EnumActionResult.FAIL;
            }
            if (!largeMachine.canLargeMachineUpgrade(player)) {
                return EnumActionResult.PASS;
            }
            IUpgradeData upgradeData = upgradeable.getUpgradeData(BaseTier.ULTIMATE);
            IBlockState upgradeResult = upgradeable.getUpgradeResult(BaseTier.ULTIMATE);
            boolean upgraded = upgradeData != null && (upgradeResult == null ? upgradeable.parseUpgradeData(upgradeData) : MEKCeuMoreMachineUpgradeUtils.replaceTileForUpgrade(tile, upgradeResult, upgradeData));
            if (upgraded) {
                if (!player.capabilities.isCreativeMode) {
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
        return "item.LargeMachineryUpgradeComponents";
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(@Nonnull ItemStack itemstack, World world, @Nonnull List<String> list, @Nonnull ITooltipFlag flag) {
        list.add(LangUtils.localize("tooltip.LargeMachineryUpgradeComponents"));
    }
}
