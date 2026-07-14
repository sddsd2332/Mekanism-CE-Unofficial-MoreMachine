package mekceumoremachine.mixin.mekanism;

import mekanism.common.tier.EnergyCubeTier;
import mekanism.common.tile.TileEntityEnergyCube;
import mekceumoremachine.common.tile.interfaces.INoWirelessChargingEnergy;
import mekceumoremachine.common.tile.interfaces.ITierMachine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TileEntityEnergyCube.class, remap = false)
public abstract class MixinTileEntityEnergyCube implements ITierMachine<EnergyCubeTier>, INoWirelessChargingEnergy {
    @Shadow
    public EnergyCubeTier tier;

    @Override
    public EnergyCubeTier getTier() {
        return tier;
    }

    @Override
    public boolean isChargingEnergy() {
        return tier == EnergyCubeTier.CREATIVE;
    }

}
