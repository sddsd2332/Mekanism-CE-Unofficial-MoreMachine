package mekceumoremachine.common.util;

import mekanism.common.base.IBoundingBlock;
import mekanism.common.base.IUpgradeableTile;
import mekanism.common.upgrade.IUpgradeData;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class MEKCeuMoreMachineUpgradeUtils {

    private MEKCeuMoreMachineUpgradeUtils() {
    }

    public static boolean replaceTileForUpgrade(TileEntity sourceTile, IBlockState targetState, IUpgradeData upgradeData) {
        World world = sourceTile.getWorld();
        BlockPos pos = sourceTile.getPos();
        if (world == null || pos == null) {
            return false;
        }
        if (sourceTile instanceof IUpgradeableTile upgradeable) {
            upgradeable.prepareForUpgrade();
        }
        if (sourceTile instanceof IBoundingBlock boundingBlock) {
            boundingBlock.onBreak();
        } else {
            world.setBlockToAir(pos);
        }
        if (!world.setBlockState(pos, targetState, 3)) {
            return false;
        }
        TileEntity upgradedTile = world.getTileEntity(pos);
        return upgradedTile instanceof IUpgradeableTile upgradeable && upgradeable.parseUpgradeData(upgradeData);
    }
}
