package mekceumoremachine.common.item.itemBlock;

import mekanism.common.config.MekanismConfig;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.generator.TileEntityTierWindGenerator;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class ItemBlockTierWindGenerator extends ItemBlockTierEnergyMachine {

    public ItemBlockTierWindGenerator(Block block) {
        super(block, "TierWindGenerator");
    }

    @Override
    public void setTierMachine(TileEntity tileEntity, ItemStack stack) {
        if (tileEntity instanceof TileEntityTierWindGenerator tile) {
            tile.tier = MachineTier.values()[getBaseTier(stack).ordinal()];
        }
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
    public double getMachineStorage() {
        return MekanismConfig.current().generators.windGeneratorStorage.val();
    }
}
