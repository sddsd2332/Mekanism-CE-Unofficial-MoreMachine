package mekceumoremachine.mixin.mekanism;

import mekanism.api.gas.GasTank;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.Mekanism;
import mekanism.common.base.IBoundingBlock;
import mekanism.common.base.ITierUpgradeable;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.machine.TileEntityAmbientAccumulatorEnergy;
import mekanism.common.tile.prefab.TileEntityMachine;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.tile.interfaces.ITierFirstUpgrade;
import mekceumoremachine.common.tile.machine.TileEntityTierAmbientAccumulator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TileEntityAmbientAccumulatorEnergy.class, remap = false)
public abstract class MixinTileEntityAmbientAccumulatorEnergy extends TileEntityMachine implements ITierUpgradeable , ITierFirstUpgrade {

    @Shadow
    public double clientEnergyUsed;

    @Shadow
    public TileComponentEjector ejectorComponent;

    @Shadow
    public TileComponentConfig configComponent;

    @Shadow
    public GasTank outputTank;

    public MixinTileEntityAmbientAccumulatorEnergy(String sound, String name, double energyStorge, double energUsage, int upgradeSlot) {
        super(sound, name, energyStorge, energUsage, upgradeSlot);
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
        world.setBlockState(getPos(), MEKCeuMoreMachineBlocks.TierAmbientAccumulator.getDefaultState(), 3);
        if (world.getTileEntity(getPos()) instanceof TileEntityTierAmbientAccumulator tile) {
            //Basic
            tile.facing = facing;
            tile.clientFacing = clientFacing;
            tile.ticker = ticker;
            tile.redstone = redstone;
            tile.redstoneLastTick = redstoneLastTick;
            tile.doAutoSync = doAutoSync;

            //Electric
            tile.electricityStored.set(electricityStored.get());
            tile.clientEnergyUsed = clientEnergyUsed;

            //Machine
            tile.setActive(isActive);
            tile.setControlType(getControlType());
            tile.prevEnergy = prevEnergy;
            tile.upgradeComponent.readFrom(upgradeComponent);
            tile.upgradeComponent.setUpgradeSlot(upgradeComponent.getUpgradeSlot());

            tile.ejectorComponent.readFrom(ejectorComponent);
            tile.ejectorComponent.setOutputData(TransmissionType.GAS, tile.configComponent.getOutputs(TransmissionType.GAS).get(1));

            tile.securityComponent.readFrom(securityComponent);

            configComponent.getTransmissions().forEach(transmission -> {
                tile.configComponent.setConfig(transmission, configComponent.getConfig(transmission).asByteArray());
                tile.configComponent.setEjecting(transmission, configComponent.isEjecting(transmission));
            });

            for (int i = 0; i < inventory.size(); i++) {
                tile.inventory.set(i, inventory.get(i));
            }

            tile.outputTank.setGas(outputTank.getGas());

            tile.upgradeComponent.getSupportedTypes().forEach(tile::recalculateUpgradables);
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
