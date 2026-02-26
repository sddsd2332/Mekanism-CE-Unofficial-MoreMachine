package mekceumoremachine.mixin.mekanism;

import mekanism.api.gas.GasTank;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.Mekanism;
import mekanism.common.base.IBoundingBlock;
import mekanism.common.base.ITierUpgradeable;
import mekanism.common.block.states.BlockStateMachine;
import mekanism.common.recipe.inputs.GasInput;
import mekanism.common.recipe.machines.IsotopicRecipe;
import mekanism.common.recipe.outputs.GasOutput;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.machine.TileEntityIsotopicCentrifuge;
import mekanism.common.tile.prefab.TileEntityBasicMachine;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.tile.interfaces.ITierFirstUpgrade;
import mekceumoremachine.common.tile.machine.TileEntityTierIsotopicCentrifuge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TileEntityIsotopicCentrifuge.class, remap = false)
public abstract class MixinTileEntityIsotopicCentrifuge extends TileEntityBasicMachine<GasInput, GasOutput, IsotopicRecipe> implements ITierUpgradeable, ITierFirstUpgrade {

    @Shadow
    public GasTank inputTank;

    @Shadow
    public GasTank outputTank;

    public MixinTileEntityIsotopicCentrifuge(String soundPath, BlockStateMachine.MachineType type, int upgradeSlot, int baseTicksRequired) {
        super(soundPath, type, upgradeSlot, baseTicksRequired);
    }


    public boolean isUpgrade = true;

    @Override
    public boolean upgrade(BaseTier upgradeTier) {
        if (upgradeTier != BaseTier.BASIC) {
            return false;
        }
        isUpgrade = false;
        if (world.getTileEntity(getPos()) instanceof IBoundingBlock block){
            block.onBreak();
        }else {
            world.setBlockToAir(getPos());
        }
        world.setBlockState(getPos(), MEKCeuMoreMachineBlocks.TierIsotopicCentrifuge.getDefaultState(), 3);
        if (world.getTileEntity(getPos()) instanceof TileEntityTierIsotopicCentrifuge tile) {

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
            tile.prevEnergy = prevEnergy;
            tile.operatingTicks = operatingTicks;

            tile.setControlType(getControlType());
            tile.upgradeComponent.readFrom(upgradeComponent);
            tile.ejectorComponent.readFrom(ejectorComponent);
            tile.configComponent.readFrom(configComponent);
            tile.ejectorComponent.setOutputData(TransmissionType.GAS, tile.configComponent.getOutputs(TransmissionType.GAS).get(2));
            tile.ejectorComponent.setInputOutputData(TransmissionType.GAS, tile.configComponent.getOutputs(TransmissionType.GAS).get(3));
            tile.upgradeComponent.setUpgradeSlot(upgradeComponent.getUpgradeSlot());
            tile.securityComponent.readFrom(securityComponent);

            configComponent.getTransmissions().forEach(transmission -> {
                tile.configComponent.setConfig(transmission, configComponent.getConfig(transmission).asByteArray());
                tile.configComponent.setEjecting(transmission, configComponent.isEjecting(transmission));
            });

            for (int i = 0; i < inventory.size(); i++) {
                tile.inventory.set(i, inventory.get(i));
            }
            tile.inputTank.setGas(inputTank.getGas());
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
