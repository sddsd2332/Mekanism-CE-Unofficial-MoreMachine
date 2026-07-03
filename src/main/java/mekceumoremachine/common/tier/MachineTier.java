package mekceumoremachine.common.tier;

import mekanism.common.tier.BaseTier;
import mekanism.common.tier.GasTankTier;
import mekanism.common.tier.ITier;
import net.minecraft.util.IStringSerializable;

import java.util.Locale;

public enum MachineTier implements ITier, IStringSerializable {

    BASIC(BaseTier.BASIC, 3),
    ADVANCED(BaseTier.ADVANCED, 5),
    ELITE(BaseTier.ELITE, 7),
    ULTIMATE(BaseTier.ULTIMATE, 9);

    private static final MachineTier[] TIERS = values();

    public final int processes;
    private final BaseTier baseTier;

    MachineTier(BaseTier tier, int process) {
        processes = process;
        baseTier = tier;
    }

    public static MachineTier get(BaseTier baseTier) {
        for (MachineTier tier : TIERS) {
            if (tier.getBaseTier() == baseTier) {
                return tier;
            }
        }
        return BASIC;
    }

    public static MachineTier byIndex(int index) {
        return index >= 0 && index < TIERS.length ? TIERS[index] : BASIC;
    }

    public static BaseTier getBaseTierByIndex(int index) {
        return byIndex(index).getBaseTier();
    }

    public static int getIndex(BaseTier baseTier) {
        return get(baseTier).ordinal();
    }

    public GasTankTier getGasTankTier() {
        for (GasTankTier tier : GasTankTier.values()) {
            if (tier.getBaseTier() == baseTier) {
                return tier;
            }
        }
        return GasTankTier.BASIC;
    }

    public boolean canUpgradeTo(BaseTier upgradeTier) {
        if (upgradeTier == null || upgradeTier == BaseTier.CREATIVE) {
            return false;
        }
        int nextIndex = ordinal() + 1;
        return nextIndex < TIERS.length && TIERS[nextIndex].getBaseTier() == upgradeTier;
    }

    @Override
    public BaseTier getBaseTier() {
        return baseTier;
    }


    @Override
    public String getName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
