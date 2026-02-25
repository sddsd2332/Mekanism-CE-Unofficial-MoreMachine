package mekceumoremachine.common.item.itemBlock;

import mekanism.common.config.MekanismConfig;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.generator.TileEntityTierAdvancedSolarGenerator;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class ItemBlockTierAdvancedSolarGenerator extends ItemBlockMekceuMoreMachineTier {

    public ItemBlockTierAdvancedSolarGenerator(Block block) {
        super(block, "TierAdvancedSolarGenerator");
    }

    @Override
    public void setTierMachine(TileEntity tileEntity, ItemStack stack) {
        if (tileEntity instanceof TileEntityTierAdvancedSolarGenerator tile) {
            tile.tier = MachineTier.values()[getBaseTier(stack).ordinal()];
        }
    }

    @Override
    public boolean canPlace(@Nonnull ItemStack stack, @Nonnull EntityPlayer player, World world, @Nonnull BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, @Nonnull IBlockState state) {
        Block block = world.getBlockState(pos).getBlock();
        if (!(block.isReplaceable(world, pos) && world.isAirBlock(pos.add(0, 1, 0)))) {
            return true;
        }
        for (int xPos = -1; xPos <= 1; xPos++) {
            for (int zPos = -1; zPos <= 1; zPos++) {
                if (!world.isAirBlock(pos.add(xPos, 2, zPos)) || pos.getY() + 2 > 255) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public double getMachineStorage() {
        return MekanismConfig.current().generators.advancedSolarGeneratorStorage.val();
    }
}
