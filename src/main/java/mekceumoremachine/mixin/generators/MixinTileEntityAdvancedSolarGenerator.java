package mekceumoremachine.mixin.generators;

import mekanism.common.Mekanism;
import mekanism.common.base.IBoundingBlock;
import mekanism.common.base.ITierUpgradeable;
import mekanism.common.tier.BaseTier;
import mekanism.generators.common.tile.TileEntityAdvancedSolarGenerator;
import mekanism.generators.common.tile.TileEntitySolarGenerator;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.tile.generator.TileEntityTierAdvancedSolarGenerator;
import mekceumoremachine.common.tile.interfaces.ITierFirstUpgrade;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = TileEntityAdvancedSolarGenerator.class, remap = false)
public class MixinTileEntityAdvancedSolarGenerator extends TileEntitySolarGenerator implements ITierUpgradeable, ITierFirstUpgrade {


    @Override
    public boolean upgrade(BaseTier upgradeTier) {
        if (upgradeTier != BaseTier.BASIC) {
            return false;
        }
        if (world.getTileEntity(getPos()) instanceof IBoundingBlock block) {
            block.onBreak();
        } else {
            world.setBlockToAir(getPos());
        }
        world.setBlockState(getPos(), MEKCeuMoreMachineBlocks.TierAdvancedSolarGenerator.getDefaultState(), 3);
        if (world.getTileEntity(getPos()) instanceof TileEntityTierAdvancedSolarGenerator tile) {
            tile.onPlace();
            //Basic
            tile.facing = facing;
            tile.clientFacing = clientFacing;
            tile.ticker = ticker;
            tile.redstone = redstone;
            tile.redstoneLastTick = redstoneLastTick;
            tile.doAutoSync = doAutoSync;

            //Electric
            tile.electricityStored.set(electricityStored.get());

            //Machine
            tile.setActive(isActive);
            tile.setControlType(getControlType());

            tile.securityComponent.readFrom(securityComponent);

            for (int i = 0; i < inventory.size(); i++) {
                tile.inventory.set(i, inventory.get(i));
            }
            tile.markNoUpdateSync();
            Mekanism.packetHandler.sendUpdatePacket(tile);
            markNoUpdateSync();
            return true;
        }
        return false;
    }
}
