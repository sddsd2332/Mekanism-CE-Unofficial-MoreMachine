package mekceumoremachine.mixin.generators;


import mekanism.common.capabilities.gas.BasicGasTank;
import mekanism.common.tier.BaseTier;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.generators.common.tile.TileEntityGasGenerator;
import mekanism.generators.common.tile.TileEntityGenerator;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.tile.interfaces.ITierFirstUpgrade;
import mekceumoremachine.common.upgrade.FirstGasGeneratorUpgradeData;
import net.minecraft.block.state.IBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TileEntityGasGenerator.class, remap = false)
public abstract class MixinTileEntityGasGenerator extends TileEntityGenerator implements ITierFirstUpgrade {

    @Shadow
    public double clientUsed;

    @Shadow
    public int burnTicks;

    @Shadow
    public int maxBurnTicks;

    @Shadow
    public double generationRate;

    @Shadow
    public BasicGasTank fuelTank;

    public MixinTileEntityGasGenerator(String soundPath, String name, double maxEnergy, double out) {
        super(soundPath, name, maxEnergy, out);
    }

    public boolean isUpgrade = true;

    @Override
    public IBlockState getUpgradeResult(BaseTier upgradeTier) {
        return canInstallUpgrade(upgradeTier) ? MEKCeuMoreMachineBlocks.TierGasGenerator.getDefaultState() : null;
    }

    @Override
    public void prepareForUpgrade() {
        isUpgrade = false;
    }

    @Override
    public IUpgradeData getUpgradeData(BaseTier upgradeTier) {
        return canInstallUpgrade(upgradeTier)
              ? new FirstGasGeneratorUpgradeData(upgradeTier, this, burnTicks, maxBurnTicks, generationRate, clientUsed, fuelTank.getGas())
              : null;
    }

    @Override
    public boolean shouldDumpRadiation() {
        return isUpgrade;
    }
}
