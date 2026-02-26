package mekceumoremachine.mixin.mekanism;

import mekanism.api.gas.GasTank;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.Mekanism;
import mekanism.common.base.IBoundingBlock;
import mekanism.common.base.ITierUpgradeable;
import mekanism.common.block.states.BlockStateMachine;
import mekanism.common.recipe.inputs.FluidInput;
import mekanism.common.recipe.machines.SeparatorRecipe;
import mekanism.common.recipe.outputs.ChemicalPairOutput;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.TileEntityGasTank;
import mekanism.common.tile.machine.TileEntityElectrolyticSeparator;
import mekanism.common.tile.prefab.TileEntityBasicMachine;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.tile.interfaces.ITierFirstUpgrade;
import mekceumoremachine.common.tile.machine.TileEntityTierElectrolyticSeparator;
import net.minecraftforge.fluids.FluidTank;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TileEntityElectrolyticSeparator.class, remap = false)
public abstract class MixinTileEntityElectrolyticSeparator extends TileEntityBasicMachine<FluidInput, ChemicalPairOutput, SeparatorRecipe> implements ITierUpgradeable , ITierFirstUpgrade {

    @Shadow
    public GasTank rightTank;

    @Shadow
    public GasTank leftTank;

    @Shadow
    public FluidTank fluidTank;

    @Shadow
    public TileEntityGasTank.GasMode dumpLeft;

    @Shadow
    public TileEntityGasTank.GasMode dumpRight;

    @Shadow
    public double clientEnergyUsed;


    public MixinTileEntityElectrolyticSeparator(String soundPath, BlockStateMachine.MachineType type, int upgradeSlot, int baseTicksRequired) {
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
        world.setBlockState(getPos(), MEKCeuMoreMachineBlocks.TierElectrolyticSeparator.getDefaultState(), 3);
        if (world.getTileEntity(getPos()) instanceof TileEntityTierElectrolyticSeparator tile) {
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
            tile.ejectorComponent.setOutputData(TransmissionType.FLUID, tile.configComponent.getOutputs(TransmissionType.FLUID).get(1));

            tile.securityComponent.readFrom(securityComponent);
            configComponent.getTransmissions().forEach(transmission -> {
                tile.configComponent.setConfig(transmission, configComponent.getConfig(transmission).asByteArray());
                tile.configComponent.setEjecting(transmission, configComponent.isEjecting(transmission));
            });

            for (int i = 0; i < inventory.size(); i++) {
                tile.inventory.set(i, inventory.get(i));
            }

            tile.leftTank.setGas(leftTank.getGas());
            tile.rightTank.setGas(rightTank.getGas());
            tile.fluidTank.setFluid(fluidTank.getFluid());
            tile.dumpLeft = dumpLeft;
            tile.dumpRight = dumpRight;
            tile.upgradeComponent.getSupportedTypes().forEach(tile::recalculateUpgradables);
            tile.isUpgrade = true;
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
