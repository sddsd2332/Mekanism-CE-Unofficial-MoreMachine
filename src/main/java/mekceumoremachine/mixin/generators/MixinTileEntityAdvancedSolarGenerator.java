package mekceumoremachine.mixin.generators;

import mekanism.common.tier.BaseTier;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.generators.common.tile.TileEntityAdvancedSolarGenerator;
import mekanism.generators.common.tile.TileEntitySolarGenerator;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.tile.interfaces.ITierFirstUpgrade;
import mekceumoremachine.common.upgrade.FirstAdvancedSolarGeneratorUpgradeData;
import net.minecraft.block.state.IBlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = TileEntityAdvancedSolarGenerator.class, remap = false)
public class MixinTileEntityAdvancedSolarGenerator extends TileEntitySolarGenerator implements ITierFirstUpgrade {


    @Override
    public IBlockState getUpgradeResult(BaseTier upgradeTier) {
        return canInstallUpgrade(upgradeTier) ? MEKCeuMoreMachineBlocks.TierAdvancedSolarGenerator.getDefaultState() : null;
    }

    @Override
    public IUpgradeData getUpgradeData(BaseTier upgradeTier) {
        return canInstallUpgrade(upgradeTier) ? new FirstAdvancedSolarGeneratorUpgradeData(upgradeTier, this) : null;
    }
}
