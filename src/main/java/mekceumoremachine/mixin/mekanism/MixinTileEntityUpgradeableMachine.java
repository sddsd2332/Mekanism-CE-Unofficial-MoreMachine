package mekceumoremachine.mixin.mekanism;

import mekanism.common.tile.prefab.TileEntityUpgradeableMachine;
import mekceumoremachine.common.tile.interfaces.ITierFirstUpgrade;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = TileEntityUpgradeableMachine.class, remap = false)
public abstract class MixinTileEntityUpgradeableMachine implements ITierFirstUpgrade {
}
