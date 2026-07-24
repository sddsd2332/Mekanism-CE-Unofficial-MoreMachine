package mekceumoremachine.common.item.itemBlock;

import mekceumoremachine.common.config.MoreMachineConfig;
import mekceumoremachine.common.block.BlockTierMachine;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.machine.TileEntityVoidMineralGenerator;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class ItemBlockVoidMineralGenerator extends ItemBlockMekceuMoreMachineTier {


    public ItemBlockVoidMineralGenerator(Block block) {
        super(block, "VoidMineralGenerator");
    }

    @Override
    void setTierMachine(TileEntity tileEntity, ItemStack stack) {
        if (tileEntity instanceof TileEntityVoidMineralGenerator tile) {
            tile.tier = MachineTier.get(getBaseTier(stack));
        }
    }

    @Override
    public boolean canPlace(@Nonnull ItemStack stack, @Nonnull EntityPlayer player, World world, @Nonnull BlockPos pos, EnumFacing side, float hitX, float hitY,
          float hitZ, @Nonnull IBlockState state) {
        return block instanceof BlockTierMachine machine && !machine.canPlaceStructureAt(world, pos);
    }


    @Override
    public double getMachineStorage() {
        return MoreMachineConfig.current().config.VoidMineralGeneratorEnergyStorge.val();
    }


}
