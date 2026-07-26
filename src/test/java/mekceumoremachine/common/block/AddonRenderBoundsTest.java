package mekceumoremachine.common.block;

import mekceumoremachine.common.tile.generator.TileEntityTierAdvancedSolarGenerator;
import mekceumoremachine.common.tile.generator.TileEntityTierWindGenerator;
import mekceumoremachine.common.tile.machine.TileEntityTierIsotopicCentrifuge;
import mekceumoremachine.common.tile.machine.TileEntityTierSolarNeutronActivator;
import mekceumoremachine.common.tile.machine.TileEntityVoidMineralGenerator;
import mekceumoremachine.common.tile.machine.TileEntityWirelessChargingEnergy;
import mekceumoremachine.common.tile.machine.TileEntityWirelessChargingStation;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Loader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddonRenderBoundsTest {

    private static final BlockPos POS = new BlockPos(10, 64, -4);

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
    void verticalAddonMachinesUseTheirDeclaredFootprints() {
        assertBox(atOrigin(new TileEntityVoidMineralGenerator()).getRenderBoundingBox(), 10, 64, -4, 11, 66, -3);
        assertBox(atOrigin(new TileEntityTierSolarNeutronActivator()).getRenderBoundingBox(), 10, 64, -4, 11, 66, -3);
        assertBox(atOrigin(new TileEntityTierIsotopicCentrifuge()).getRenderBoundingBox(), 10, 64, -4, 11, 66, -3);
        assertBox(atOrigin(new TileEntityWirelessChargingStation()).getRenderBoundingBox(), 10, 64, -4, 11, 67, -3);
        assertBox(atOrigin(new TileEntityWirelessChargingEnergy()).getRenderBoundingBox(), 10, 64, -4, 11, 67, -3);
    }

    @Test
    void addonGeneratorsUseFiniteModelBounds() {
        assertBox(atOrigin(new TileEntityTierAdvancedSolarGenerator()).getRenderBoundingBox(), 9, 64, -5, 12, 67, -2);
        assertBox(atOrigin(new TileEntityTierWindGenerator()).getRenderBoundingBox(), 8, 64, -6, 13, 71, -1);
    }

    private static <T extends TileEntity> T atOrigin(T tile) {
        tile.setPos(POS);
        return tile;
    }

    private static void assertBox(AxisAlignedBB box, double minX, double minY, double minZ,
                                  double maxX, double maxY, double maxZ) {
        // Keep this test compatible with released Mekanism jars while allowing
        // the shared 1/16-block render safety allowance.
        double epsilon = 0.0625D + 1.0E-9D;
        assertTrue(box.minX <= minX && box.minX >= minX - epsilon);
        assertTrue(box.minY <= minY && box.minY >= minY - epsilon);
        assertTrue(box.minZ <= minZ && box.minZ >= minZ - epsilon);
        assertTrue(box.maxX >= maxX && box.maxX <= maxX + epsilon);
        assertTrue(box.maxY >= maxY && box.maxY <= maxY + epsilon);
        assertTrue(box.maxZ >= maxZ && box.maxZ <= maxZ + epsilon);
        assertTrue(Double.isFinite(box.minX) && Double.isFinite(box.minY) && Double.isFinite(box.minZ));
        assertTrue(Double.isFinite(box.maxX) && Double.isFinite(box.maxY) && Double.isFinite(box.maxZ));
    }
}
