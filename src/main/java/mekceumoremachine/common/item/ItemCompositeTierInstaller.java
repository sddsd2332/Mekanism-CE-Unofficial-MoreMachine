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
        ItemStack stack = player.getHeldItem(hand);
        TileEntity tile = world.getTileEntity(pos);

        if (isBusy(tile)) {
            return EnumActionResult.FAIL;
        }
        installBaseTierIfNeeded(tile);

        // Upgrade may replace the tile, so fetch again.
        tile = world.getTileEntity(pos);
        if (isBusy(tile)) {
            return EnumActionResult.FAIL;
        }

        if (tile instanceof INeedRepeatTierUpgrade<?>) {
            return upgradeFactory(player, world, pos, stack);
        }
        if (tile instanceof ITierMachine<?> machine) {
            return upgradeMachine(player, machine, stack);
        }
        return EnumActionResult.PASS;
    }

    private static boolean isBusy(TileEntity tile) {
        return tile instanceof TileEntityBasicBlock basicBlock && !basicBlock.playersUsing.isEmpty();
    }

    private static void installBaseTierIfNeeded(TileEntity tile) {
        if (tile instanceof ITierFirstUpgrade && tile instanceof ITierUpgradeable upgradeable && upgradeable.CanInstalled()) {
            upgradeable.upgrade(BaseTier.BASIC);
        }
    }

    private static EnumActionResult upgradeFactory(EntityPlayer player, World world, BlockPos pos, ItemStack stack) {
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof INeedRepeatTierUpgrade<?> factory)) {
            return EnumActionResult.PASS;
        }

        BaseTier tier = factory.getNowTier().getBaseTier();
        if (tier == BaseTier.ULTIMATE || tier == BaseTier.CREATIVE) {
            return EnumActionResult.PASS;
        }

        BaseTier lastTier = null;
        while (world.getTileEntity(pos) instanceof INeedRepeatTierUpgrade<?> machine) {
            BaseTier current = machine.getNowTier().getBaseTier();
            if (current == BaseTier.ULTIMATE || current == BaseTier.CREATIVE || current == lastTier) {
                break;
            }
            BaseTier next = getNextTier(current);
            if (next == null) {
                break;
            }
            lastTier = current;
            machine.upgrade(next);
        }

        if (!player.capabilities.isCreativeMode
                && world.getTileEntity(pos) instanceof INeedRepeatTierUpgrade<?> machine
                && machine.getNowTier().getBaseTier() == BaseTier.ULTIMATE) {
            stack.shrink(1);
        }
        return EnumActionResult.SUCCESS;
    }

    private static EnumActionResult upgradeMachine(EntityPlayer player, ITierMachine<?> machine, ItemStack stack) {
        if (machine.getTier().getBaseTier() == BaseTier.ULTIMATE) {
            return EnumActionResult.PASS;
        }
        if (!machine.CanInstalled()) {
            return EnumActionResult.PASS;
        }

        BaseTier lastTier = null;
        while (true) {
            BaseTier current = machine.getTier().getBaseTier();
            if (current == BaseTier.ULTIMATE || current == BaseTier.CREATIVE || current == lastTier) {
                break;
            }
            BaseTier next = getNextTier(current);
            if (next == null) {
                break;
            }
            lastTier = current;
            machine.upgrade(next);
        }

        if (!player.capabilities.isCreativeMode && machine.getTier().getBaseTier() == BaseTier.ULTIMATE) {
            stack.shrink(1);
        }
        return EnumActionResult.SUCCESS;
    }

    private static BaseTier getNextTier(BaseTier tier) {
        int nextOrdinal = tier.ordinal() + 1;
        BaseTier[] tiers = BaseTier.values();
        return nextOrdinal >= 0 && nextOrdinal < tiers.length ? tiers[nextOrdinal] : null;
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
