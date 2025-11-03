package mekceumoremachine.common.item.itemBlock;

import mekanism.api.EnumColor;
import mekanism.common.config.MekanismConfig;
import mekanism.common.util.LangUtils;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class ItemBlockBigWindGenerator extends ItemBlockTierEnergyMachine {

    public ItemBlockBigWindGenerator(Block block) {
        super(block, "BigWindGenerator");
    }

    @Override
    public void setTierMachine(TileEntity tileEntity, ItemStack stack) {
    }

    @Override
    public boolean canPlace(@Nonnull ItemStack stack, @Nonnull EntityPlayer player, World world, @Nonnull BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, @Nonnull IBlockState state) {
        for (int yPos = 1; yPos <= 4; yPos++) {
            BlockPos abovePos = pos.up(yPos);
            if (!world.isValid(abovePos) || !world.getBlockState(abovePos).getBlock().isReplaceable(world, abovePos)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public double getMaxEnergy(ItemStack itemStack) {
        if (itemStack.getCount() > 1) {
            return 0;
        }
        return getMachineStorage() * 1024 * MekanismConfig.current().mekce.MAXThreadUpgrade.val();
    }

    @Override
    public double getMachineStorage() {
        return MekanismConfig.current().generators.windGeneratorStorage.val();
    }

    @Nonnull
    @Override
    public String getTranslationKey(ItemStack itemstack) {
        return block.getTranslationKey();
    }

    @Nonnull
    @Override
    public String getItemStackDisplayName(@Nonnull ItemStack itemstack) {
        return EnumColor.YELLOW + LangUtils.localize("tile." + name + ".name");
    }
}
