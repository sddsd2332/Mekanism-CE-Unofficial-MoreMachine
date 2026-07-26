package mekceumoremachine.common.upgrade;

import mekanism.common.tier.BaseTier;
import mekanism.common.upgrade.ITileUpgradeAdapter;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.upgrade.TileUpgradeRegistry;
import mekanism.multiblockmachine.common.tile.generator.TileEntityLargeGasGenerator;
import mekanism.multiblockmachine.common.tile.generator.TileEntityLargeWindGenerator;
import mekanism.multiblockmachine.common.tile.machine.TileEntityLargeChemicalInfuser;
import mekanism.multiblockmachine.common.tile.machine.TileEntityLargeChemicalWasher;
import mekanism.multiblockmachine.common.tile.machine.TileEntityLargeElectrolyticSeparator;
import mekanism.multiblockmachine.common.tile.machine.TileEntityLargeSolarNeutronActivator;
import mekceumoremachine.common.MEKCeuMoreMachine;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import java.util.function.BiPredicate;

/** Optional adapters that apply MoreMachine upgrade data to large Mekanism machines. */
public final class MoreMachineMultiblockUpgradeAdapters {

    private MoreMachineMultiblockUpgradeAdapters() {
    }

    public static void register() {
        registerTarget("large_chemical_infuser", TileEntityLargeChemicalInfuser.class, (tile, rawData) -> {
            if (!(rawData instanceof LargeChemicalInfuserUpgradeData data) || data.getUpgradeTier() != BaseTier.ULTIMATE) {
                return false;
            }
            tile.onPlace();
            LargeMachineUpgradeDataApplier.applyCommon(tile, data, tile.upgradeComponent, tile.securityComponent);
            tile.clientEnergyUsed = data.clientEnergyUsed;
            tile.prevEnergy = data.prevEnergy;
            tile.leftTank.setGas(copy(data.leftGas));
            tile.rightTank.setGas(copy(data.rightGas));
            tile.centerTank.setGas(copy(data.centerGas));
            LargeMachineUpgradeDataApplier.finish(tile, tile.upgradeComponent);
            return true;
        });
        registerTarget("large_chemical_washer", TileEntityLargeChemicalWasher.class, (tile, rawData) -> {
            if (!(rawData instanceof LargeChemicalWasherUpgradeData data) || data.getUpgradeTier() != BaseTier.ULTIMATE) {
                return false;
            }
            tile.onPlace();
            LargeMachineUpgradeDataApplier.applyCommon(tile, data, tile.upgradeComponent, tile.securityComponent);
            tile.clientEnergyUsed = data.clientEnergyUsed;
            tile.prevEnergy = data.prevEnergy;
            tile.fluidTank.setFluid(data.fluid == null ? null : data.fluid.copy());
            tile.inputTank.setGas(copy(data.inputGas));
            tile.outputTank.setGas(copy(data.outputGas));
            LargeMachineUpgradeDataApplier.finish(tile, tile.upgradeComponent);
            return true;
        });
        registerTarget("large_electrolytic_separator", TileEntityLargeElectrolyticSeparator.class, (tile, rawData) -> {
            if (!(rawData instanceof LargeElectrolyticSeparatorUpgradeData data) || data.getUpgradeTier() != BaseTier.ULTIMATE) {
                return false;
            }
            tile.onPlace();
            LargeMachineUpgradeDataApplier.applyCommon(tile, data, tile.upgradeComponent, tile.securityComponent);
            tile.clientEnergyUsed = data.clientEnergyUsed;
            tile.prevEnergy = data.prevEnergy;
            tile.fluidTank.setFluid(data.fluid == null ? null : data.fluid.copy());
            tile.leftTank.setGas(copy(data.leftGas));
            tile.rightTank.setGas(copy(data.rightGas));
            tile.dumpLeft = data.dumpLeft;
            tile.dumpRight = data.dumpRight;
            LargeMachineUpgradeDataApplier.finish(tile, tile.upgradeComponent);
            return true;
        });
        registerTarget("large_gas_generator", TileEntityLargeGasGenerator.class, (tile, rawData) -> {
            if (!(rawData instanceof LargeGasGeneratorUpgradeData data) || data.getUpgradeTier() != BaseTier.ULTIMATE) {
                return false;
            }
            tile.onPlace();
            LargeMachineUpgradeDataApplier.applyCommon(tile, data, tile.upgradeComponent, tile.securityComponent);
            tile.burnTicks = data.burnTicks;
            tile.maxBurnTicks = data.maxBurnTicks;
            tile.generationRate = data.generationRate;
            tile.clientUsed = data.clientUsed;
            tile.fuelTank.setGas(copy(data.fuel));
            LargeMachineUpgradeDataApplier.finish(tile, tile.upgradeComponent);
            return true;
        });
        registerTarget("large_solar_neutron_activator", TileEntityLargeSolarNeutronActivator.class, (tile, rawData) -> {
            if (!(rawData instanceof LargeSolarNeutronActivatorUpgradeData data) || data.getUpgradeTier() != BaseTier.ULTIMATE) {
                return false;
            }
            tile.onPlace();
            LargeMachineUpgradeDataApplier.applyCommon(tile, data, tile.upgradeComponent, tile.securityComponent);
            tile.operatingTicks = data.operatingTicks;
            tile.inputTank.setGas(copy(data.inputGas));
            tile.outputTank.setGas(copy(data.outputGas));
            LargeMachineUpgradeDataApplier.finish(tile, tile.upgradeComponent);
            return true;
        });
        registerTarget("large_wind_generator", TileEntityLargeWindGenerator.class, (tile, rawData) -> {
            if (!(rawData instanceof LargeWindGeneratorUpgradeData data) || data.getUpgradeTier() != BaseTier.ULTIMATE) {
                return false;
            }
            tile.onPlace();
            LargeMachineUpgradeDataApplier.applyCommon(tile, data, tile.upgradeComponent, tile.securityComponent);
            tile.setAngle(data.angle);
            LargeMachineUpgradeDataApplier.finish(tile, tile.upgradeComponent);
            return true;
        });
    }

    private static <TILE extends TileEntity> void registerTarget(String path, Class<TILE> tileClass,
          BiPredicate<TILE, IUpgradeData> parser) {
        TileUpgradeRegistry.register(new ResourceLocation(MEKCeuMoreMachine.MODID, path), tileClass,
              new ITileUpgradeAdapter<TILE>() {
                  @Override
                  public boolean parseUpgradeData(TILE tile, IUpgradeData upgradeData) {
                      return parser.test(tile, upgradeData);
                  }
              });
    }

    private static mekanism.api.gas.GasStack copy(mekanism.api.gas.GasStack stack) {
        return stack == null ? null : stack.copy();
    }
}
