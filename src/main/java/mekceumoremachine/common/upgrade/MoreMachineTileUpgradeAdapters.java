package mekceumoremachine.common.upgrade;

import mekanism.common.tier.BaseTier;
import mekanism.common.tile.TileEntityRadioactiveWasteBarrel;
import mekanism.common.tile.machine.TileEntityAmbientAccumulatorEnergy;
import mekanism.common.tile.machine.TileEntityChemicalCrystallizer;
import mekanism.common.tile.machine.TileEntityChemicalDissolutionChamber;
import mekanism.common.tile.machine.TileEntityChemicalInfuser;
import mekanism.common.tile.machine.TileEntityChemicalOxidizer;
import mekanism.common.tile.machine.TileEntityChemicalWasher;
import mekanism.common.tile.machine.TileEntityElectricPump;
import mekanism.common.tile.machine.TileEntityElectrolyticSeparator;
import mekanism.common.tile.machine.TileEntityIsotopicCentrifuge;
import mekanism.common.tile.machine.TileEntityNutritionalLiquifier;
import mekanism.common.tile.machine.TileEntityRotaryCondensentrator;
import mekanism.common.tile.machine.TileEntitySolarNeutronActivator;
import mekanism.common.upgrade.ITileUpgradeAdapter;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.upgrade.TileUpgradeRegistry;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/** Registers first-tier conversions from Mekanism machines to MoreMachine variants. */
public final class MoreMachineTileUpgradeAdapters {

    private MoreMachineTileUpgradeAdapters() {
    }

    public static void register() {
        registerFirst("ambient_accumulator", TileEntityAmbientAccumulatorEnergy.class,
              () -> MEKCeuMoreMachineBlocks.TierAmbientAccumulator.getDefaultState(),
              (tile, tier) -> new FirstAmbientAccumulatorUpgradeData(tier, tile, tile.clientEnergyUsed, tile.prevEnergy,
                    tile.configComponent, tile.ejectorComponent, tile.outputTank));
        registerFirst("chemical_crystallizer", TileEntityChemicalCrystallizer.class,
              () -> MEKCeuMoreMachineBlocks.TierChemicalCrystallizer.getStateFromMeta(0),
              (tile, tier) -> new FirstChemicalCrystallizerUpgradeData(tier, tile, tile.prevEnergy, tile.operatingTicks,
                    tile.configComponent, tile.ejectorComponent, tile.inputTank));
        registerFirst("chemical_dissolution_chamber", TileEntityChemicalDissolutionChamber.class,
              () -> MEKCeuMoreMachineBlocks.TierChemicalDissolutionChamber.getStateFromMeta(0),
              (tile, tier) -> new FirstChemicalDissolutionChamberUpgradeData(tier, tile, tile.prevEnergy, tile.operatingTicks,
                    tile.getSavedUsedSoFar(0), tile.configComponent, tile.ejectorComponent, tile.injectTank, tile.outputTank));
        registerFirst("chemical_infuser", TileEntityChemicalInfuser.class,
              () -> MEKCeuMoreMachineBlocks.TierChemicalInfuser.getDefaultState(),
              (tile, tier) -> new FirstChemicalInfuserUpgradeData(tier, tile, tile.clientEnergyUsed, tile.prevEnergy,
                    tile.configComponent, tile.ejectorComponent, tile.leftTank, tile.rightTank, tile.centerTank));
        registerFirst("chemical_oxidizer", TileEntityChemicalOxidizer.class,
              () -> MEKCeuMoreMachineBlocks.TierChemicalOxidizer.getStateFromMeta(0),
              (tile, tier) -> new FirstChemicalOxidizerUpgradeData(tier, tile, tile.prevEnergy, tile.operatingTicks,
                    tile.configComponent, tile.ejectorComponent, tile.gasTank));
        registerFirst("chemical_washer", TileEntityChemicalWasher.class,
              () -> MEKCeuMoreMachineBlocks.TierChemicalWasher.getDefaultState(),
              (tile, tier) -> new FirstChemicalWasherUpgradeData(tier, tile, tile.clientEnergyUsed, tile.prevEnergy,
                    tile.configComponent, tile.ejectorComponent, tile.fluidTank, tile.inputTank, tile.outputTank));
        registerFirst("electric_pump", TileEntityElectricPump.class,
              () -> MEKCeuMoreMachineBlocks.TierElectricPump.getDefaultState(),
              (tile, tier) -> new FirstElectricPumpUpgradeData(tier, tile, tile.operatingTicks, tile.fluidTank,
                    tile.activeType, tile.recurringNodes));
        registerFirst("electrolytic_separator", TileEntityElectrolyticSeparator.class,
              () -> MEKCeuMoreMachineBlocks.TierElectrolyticSeparator.getDefaultState(),
              (tile, tier) -> new FirstElectrolyticSeparatorUpgradeData(tier, tile, tile.clientEnergyUsed, tile.prevEnergy,
                    tile.configComponent, tile.ejectorComponent, tile.fluidTank, tile.leftTank, tile.rightTank,
                    tile.dumpLeft, tile.dumpRight));
        registerFirst("isotopic_centrifuge", TileEntityIsotopicCentrifuge.class,
              () -> MEKCeuMoreMachineBlocks.TierIsotopicCentrifuge.getDefaultState(),
              (tile, tier) -> new FirstIsotopicCentrifugeUpgradeData(tier, tile, tile.clientEnergyUsed, tile.prevEnergy,
                    tile.operatingTicks, tile.configComponent, tile.ejectorComponent, tile.inputTank, tile.outputTank));
        registerFirst("nutritional_liquifier", TileEntityNutritionalLiquifier.class,
              () -> MEKCeuMoreMachineBlocks.TierNutritionalLiquifier.getStateFromMeta(0),
              (tile, tier) -> new FirstNutritionalLiquifierUpgradeData(tier, tile, tile.prevEnergy, tile.operatingTicks,
                    tile.configComponent, tile.ejectorComponent, tile.gasTank));
        registerFirst("radioactive_waste_barrel", TileEntityRadioactiveWasteBarrel.class,
              () -> MEKCeuMoreMachineBlocks.TierRadioactiveWasteBarrel.getDefaultState(),
              (tile, tier) -> new FirstRadioactiveWasteBarrelUpgradeData(tier, tile, tile.gasTank));
        registerFirst("rotary_condensentrator", TileEntityRotaryCondensentrator.class,
              () -> MEKCeuMoreMachineBlocks.TierRotaryCondensentrator.getDefaultState(),
              (tile, tier) -> new FirstRotaryCondensentratorUpgradeData(tier, tile, tile.clientEnergyUsed, tile.prevEnergy,
                    tile.configComponent, tile.ejectorComponent, tile.gasTank, tile.fluidTank, tile.mode));
        registerFirst("solar_neutron_activator", TileEntitySolarNeutronActivator.class,
              () -> MEKCeuMoreMachineBlocks.TierSolarNeutronActivator.getDefaultState(),
              (tile, tier) -> new FirstSolarNeutronActivatorUpgradeData(tier, tile, tile.operatingTicks,
                    tile.configComponent, tile.ejectorComponent, tile.inputTank, tile.outputTank));
    }

    private static <TILE extends TileEntity> void registerFirst(String path, Class<TILE> tileClass,
          Supplier<IBlockState> result, BiFunction<TILE, BaseTier, IUpgradeData> dataFactory) {
        TileUpgradeRegistry.register(new ResourceLocation(MEKCeuMoreMachine.MODID, "first_" + path), tileClass,
              new ITileUpgradeAdapter<TILE>() {
                  @Override
                  public boolean canInstallUpgrade(TILE tile, BaseTier upgradeTier) {
                      return upgradeTier == BaseTier.BASIC;
                  }

                  @Override
                  public IBlockState getUpgradeResult(TILE tile, BaseTier upgradeTier) {
                      return canInstallUpgrade(tile, upgradeTier) ? result.get() : null;
                  }

                  @Override
                  public IUpgradeData getUpgradeData(TILE tile, BaseTier upgradeTier) {
                      return canInstallUpgrade(tile, upgradeTier) ? dataFactory.apply(tile, upgradeTier) : null;
                  }
              });
    }
}
