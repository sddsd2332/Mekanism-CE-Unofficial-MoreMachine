package mekceumoremachine.common.tile.interfaces;

import mekanism.common.base.IUpgradeableTile;
import mekanism.common.tier.BaseTier;
import mekanism.common.upgrade.IUpgradeData;
import mekceumoremachine.common.util.MEKCeuMoreMachineUpgradeUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;

public interface ILargeMachine extends IUpgradeableTile {

    boolean canLargeMachineUpgrade(EntityPlayer player);

    default boolean applyLargeMachineUpgrade() {
        if (!(this instanceof TileEntity tile)) {
            return false;
        }
        IUpgradeData upgradeData = getUpgradeData(BaseTier.ULTIMATE);
        IBlockState upgradeResult = getUpgradeResult(BaseTier.ULTIMATE);
        return upgradeData != null && upgradeResult != null && MEKCeuMoreMachineUpgradeUtils.replaceTileForUpgrade(tile, upgradeResult, upgradeData);
    }
}
