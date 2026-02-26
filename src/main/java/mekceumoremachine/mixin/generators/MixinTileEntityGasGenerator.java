package mekceumoremachine.mixin.generators;


import mekanism.api.gas.GasTank;
import mekanism.common.Mekanism;
import mekanism.common.base.IBoundingBlock;
import mekanism.common.base.ITierUpgradeable;
import mekanism.common.tier.BaseTier;
import mekanism.generators.common.tile.TileEntityGasGenerator;
import mekanism.generators.common.tile.TileEntityGenerator;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.tile.generator.TileEntityTierGasGenerator;
import mekceumoremachine.common.tile.interfaces.ITierFirstUpgrade;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TileEntityGasGenerator.class, remap = false)
public abstract class MixinTileEntityGasGenerator extends TileEntityGenerator implements ITierUpgradeable, ITierFirstUpgrade {

    @Shadow
    public double clientUsed;

    @Shadow
    public int maxBurnTicks;

    @Shadow
    public double generationRate;

    @Shadow
    public GasTank fuelTank;

    public MixinTileEntityGasGenerator(String soundPath, String name, double maxEnergy, double out) {
        super(soundPath, name, maxEnergy, out);
    }

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
        world.setBlockState(getPos(), MEKCeuMoreMachineBlocks.TierGasGenerator.getDefaultState(), 3);
        if (world.getTileEntity(getPos()) instanceof TileEntityTierGasGenerator tile) {
            //Basic
            tile.facing = facing;
            tile.clientFacing = clientFacing;
            tile.ticker = ticker;
            tile.redstone = redstone;
            tile.redstoneLastTick = redstoneLastTick;
            tile.doAutoSync = doAutoSync;

            //Electric
            tile.electricityStored.set(electricityStored.get());
            tile.clientUsed = clientUsed;
            tile.maxBurnTicks = maxBurnTicks;
            tile.generationRate = generationRate;  //Machine
            tile.setActive(isActive);
            tile.setControlType(getControlType());

            tile.securityComponent.readFrom(securityComponent);
            for (int i = 0; i < inventory.size(); i++) {
                tile.inventory.set(i, inventory.get(i));
            }
            tile.fuelTank.setGas(fuelTank.getGas());
            tile.isUpgrade = true;
            tile.markNoUpdateSync();
            Mekanism.packetHandler.sendUpdatePacket(tile);
            markNoUpdateSync();
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldDumpRadiation() {
        return isUpgrade;
    }
}
