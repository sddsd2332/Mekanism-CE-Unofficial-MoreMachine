package mekceumoremachine.common.tile.machine;

import mekanism.api.NBTConstants;
import mekceumoremachine.common.tier.MachineTier;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.Loader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TierRadioactiveWasteBarrelDecayTest {

    @BeforeAll
    static void bootstrapMinecraft() throws ReflectiveOperationException {
        Loader loader = Loader.instance();
        Field namedMods = Loader.class.getDeclaredField("namedMods");
        namedMods.setAccessible(true);
        if (namedMods.get(loader) == null) {
            namedMods.set(loader, Collections.emptyMap());
        }
        Bootstrap.register();
    }

    @Test
    void defaultConfiguredAmountPreservesExistingTierRates() {
        assertEquals(30, TileEntityTierRadioactiveWasteBarrel.getTierDecayAmount(1, MachineTier.BASIC.processes));
        assertEquals(50, TileEntityTierRadioactiveWasteBarrel.getTierDecayAmount(1, MachineTier.ADVANCED.processes));
        assertEquals(70, TileEntityTierRadioactiveWasteBarrel.getTierDecayAmount(1, MachineTier.ELITE.processes));
        assertEquals(90, TileEntityTierRadioactiveWasteBarrel.getTierDecayAmount(1, MachineTier.ULTIMATE.processes));
    }

    @Test
    void configuredAmountScalesAndSaturatesSafely() {
        assertEquals(0, TileEntityTierRadioactiveWasteBarrel.getTierDecayAmount(0, MachineTier.ULTIMATE.processes));
        assertEquals(180, TileEntityTierRadioactiveWasteBarrel.getTierDecayAmount(2, MachineTier.ULTIMATE.processes));
        assertEquals(Integer.MAX_VALUE,
              TileEntityTierRadioactiveWasteBarrel.getTierDecayAmount(Integer.MAX_VALUE, MachineTier.ULTIMATE.processes));
    }

    @Test
    void processProgressRoundTripsThroughTileNbt() throws ReflectiveOperationException {
        Field processTicks = TileEntityTierRadioactiveWasteBarrel.class.getDeclaredField("processTicks");
        processTicks.setAccessible(true);
        TileEntityTierRadioactiveWasteBarrel original = new TileEntityTierRadioactiveWasteBarrel();
        processTicks.setInt(original, 13);
        NBTTagCompound saved = new NBTTagCompound();

        original.writeCustomNBT(saved);

        assertEquals(13, saved.getInteger(NBTConstants.PROGRESS));
        TileEntityTierRadioactiveWasteBarrel loaded = new TileEntityTierRadioactiveWasteBarrel();
        loaded.readCustomNBT(saved);
        assertEquals(13, processTicks.getInt(loaded));
    }
}
