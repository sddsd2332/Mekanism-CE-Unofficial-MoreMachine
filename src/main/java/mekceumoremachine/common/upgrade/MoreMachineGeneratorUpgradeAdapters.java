package mekceumoremachine.common.upgrade;

import mekanism.common.tier.BaseTier;
import mekanism.common.upgrade.ITileUpgradeAdapter;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.upgrade.TileUpgradeRegistry;
import mekanism.generators.common.tile.TileEntityAdvancedSolarGenerator;
import mekanism.generators.common.tile.TileEntityGasGenerator;
import mekanism.generators.common.tile.TileEntitySolarGenerator;
import mekanism.generators.common.tile.TileEntityWindGenerator;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/** Optional Mekanism Generators upgrade adapters. */
public final class MoreMachineGeneratorUpgradeAdapters {

    private MoreMachineGeneratorUpgradeAdapters() {
    }

    public static void register() {
        registerFirst("advanced_solar_generator", TileEntityAdvancedSolarGenerator.class,
              () -> MEKCeuMoreMachineBlocks.TierAdvancedSolarGenerator.getDefaultState(),
              (tile, tier) -> new FirstAdvancedSolarGeneratorUpgradeData(tier, tile));
        registerFirst("gas_generator", TileEntityGasGenerator.class,
              () -> MEKCeuMoreMachineBlocks.TierGasGenerator.getDefaultState(),
              (tile, tier) -> new FirstGasGeneratorUpgradeData(tier, tile, tile.burnTicks, tile.maxBurnTicks,
                    tile.generationRate, tile.clientUsed, tile.fuelTank.getGas()));
        registerFirst("solar_generator", TileEntitySolarGenerator.class,
              () -> MEKCeuMoreMachineBlocks.TierSolarGenerator.getDefaultState(),
              (tile, tier) -> new FirstSolarGeneratorUpgradeData(tier, tile));
        registerFirst("wind_generator", TileEntityWindGenerator.class,
              () -> MEKCeuMoreMachineBlocks.TierWindGenerator.getDefaultState(),
              (tile, tier) -> new FirstWindGeneratorUpgradeData(tier, tile, tile.getAngle()));
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
