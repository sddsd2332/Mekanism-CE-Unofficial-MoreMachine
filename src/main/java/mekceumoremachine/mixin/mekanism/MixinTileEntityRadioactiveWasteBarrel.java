package mekceumoremachine.mixin.mekanism;

import mekanism.api.gas.GasTank;
import mekanism.common.Mekanism;
import mekanism.common.base.IBoundingBlock;
import mekanism.common.base.ITierUpgradeable;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.TileEntityRadioactiveWasteBarrel;
import mekanism.common.tile.prefab.TileEntityBasicBlock;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.tile.interfaces.ITierFirstUpgrade;
import mekceumoremachine.common.tile.machine.TileEntityTierRadioactiveWasteBarrel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TileEntityRadioactiveWasteBarrel.class, remap = false)
public class MixinTileEntityRadioactiveWasteBarrel extends TileEntityBasicBlock implements ITierUpgradeable, ITierFirstUpgrade {

    @Shadow
    public boolean isActive;
    @Shadow
    public boolean clientActive;
    @Shadow
    public GasTank gasTank;
    public boolean isUpgrade = true;

    @Override
    public boolean upgrade(BaseTier upgradeTier) {
        if (upgradeTier != BaseTier.BASIC) {
            return false;
        }
        isUpgrade = false;
        if (world.getTileEntity(getPos()) instanceof IBoundingBlock block) {
            block.onBreak();
        } else {
            world.setBlockToAir(getPos());
        }
        world.setBlockState(getPos(), MEKCeuMoreMachineBlocks.TierRadioactiveWasteBarrel.getDefaultState(), 3);
        if (world.getTileEntity(getPos()) instanceof TileEntityTierRadioactiveWasteBarrel tile) {
            //Basic
            tile.facing = facing;
            tile.clientFacing = clientFacing;
            tile.ticker = ticker;
            tile.redstone = redstone;
            tile.redstoneLastTick = redstoneLastTick;
            tile.doAutoSync = doAutoSync;
            //Machine
            tile.clientActive = clientActive;
            tile.setActive(isActive);
            tile.gasTank.setGas(gasTank.getGas());
            tile.markNoUpdateSync();
            Mekanism.packetHandler.sendUpdatePacket(tile);
            markNoUpdateSync();
            return true;
        }
        return false;
    }

    /**
     * @author sddsd2332
     * @reason 取消在升级的时候进行辐射判断
     */
    @Override
    public boolean shouldDumpRadiation() {
        return isUpgrade;
    }
}
