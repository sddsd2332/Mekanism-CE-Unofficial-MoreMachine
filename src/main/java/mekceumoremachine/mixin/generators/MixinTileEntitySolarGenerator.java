package mekceumoremachine.mixin.generators;

import mekanism.common.tier.BaseTier;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.generators.common.tile.TileEntityGenerator;
import mekanism.generators.common.tile.TileEntitySolarGenerator;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.tile.interfaces.ITierFirstUpgrade;
import mekceumoremachine.common.upgrade.FirstSolarGeneratorUpgradeData;
import net.minecraft.block.state.IBlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = TileEntitySolarGenerator.class,remap = false)
public abstract class MixinTileEntitySolarGenerator extends TileEntityGenerator implements ITierFirstUpgrade {


    public MixinTileEntitySolarGenerator(String soundPath, String name, double maxEnergy, double out) {
        super(soundPath, name, maxEnergy, out);
    }

    @Override
    public IBlockState getUpgradeResult(BaseTier upgradeTier) {
        return canInstallUpgrade(upgradeTier) ? MEKCeuMoreMachineBlocks.TierSolarGenerator.getDefaultState() : null;
    }

    @Override
    public IUpgradeData getUpgradeData(BaseTier upgradeTier) {
        return canInstallUpgrade(upgradeTier) ? new FirstSolarGeneratorUpgradeData(upgradeTier, this) : null;
    }
}
