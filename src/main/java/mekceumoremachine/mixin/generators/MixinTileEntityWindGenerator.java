package mekceumoremachine.mixin.generators;

import mekanism.common.tier.BaseTier;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.generators.common.tile.TileEntityGenerator;
import mekanism.generators.common.tile.TileEntityWindGenerator;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.tile.interfaces.ITierFirstUpgrade;
import mekceumoremachine.common.upgrade.FirstWindGeneratorUpgradeData;
import net.minecraft.block.state.IBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TileEntityWindGenerator.class, remap = false)
public abstract class MixinTileEntityWindGenerator extends TileEntityGenerator implements ITierFirstUpgrade {

    @Shadow
    private double angle;

    public MixinTileEntityWindGenerator(String soundPath, String name, double maxEnergy, double out) {
        super(soundPath, name, maxEnergy, out);
    }

    @Override
    public IBlockState getUpgradeResult(BaseTier upgradeTier) {
        return canInstallUpgrade(upgradeTier) ? MEKCeuMoreMachineBlocks.TierWindGenerator.getDefaultState() : null;
    }

    @Override
    public IUpgradeData getUpgradeData(BaseTier upgradeTier) {
        return canInstallUpgrade(upgradeTier) ? new FirstWindGeneratorUpgradeData(upgradeTier, this, angle) : null;
    }
}
