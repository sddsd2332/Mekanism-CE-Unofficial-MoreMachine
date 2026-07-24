package mekceumoremachine.common.block;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BlockTierMachineStructureTest {

    @Test
    void declaresVerticalStructures() {
        assertOffsets(MachineStructureOffsets.ONE_ABOVE, new BlockPos(0, 1, 0));
        assertOffsets(MachineStructureOffsets.TWO_ABOVE, new BlockPos(0, 1, 0), new BlockPos(0, 2, 0));
        assertOffsets(MachineStructureOffsets.WIND_GENERATOR,
              new BlockPos(0, 1, 0), new BlockPos(0, 2, 0), new BlockPos(0, 3, 0), new BlockPos(0, 4, 0));
    }

    @Test
    void declaresAdvancedSolarStructure() {
        Set<BlockPos> expected = new HashSet<>();
        expected.add(new BlockPos(0, 1, 0));
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                expected.add(new BlockPos(x, 2, z));
            }
        }
        assertEquals(expected, new HashSet<>(Arrays.asList(MachineStructureOffsets.ADVANCED_SOLAR_GENERATOR)));
    }

    @Test
    void tileCollectorsReuseTheBlockPreflightLayouts() {
        assertCollector(MachineStructureOffsets.ONE_ABOVE, MachineStructureOffsets::collectOneAbove);
        assertCollector(MachineStructureOffsets.TWO_ABOVE, MachineStructureOffsets::collectTwoAbove);
        assertCollector(MachineStructureOffsets.WIND_GENERATOR, MachineStructureOffsets::collectWindGenerator);
        assertCollector(MachineStructureOffsets.ADVANCED_SOLAR_GENERATOR, MachineStructureOffsets::collectAdvancedSolarGenerator);
    }

    private static void assertOffsets(BlockPos[] actual, BlockPos... expected) {
        assertEquals(expected.length, actual.length, "Structure layout contains duplicate offsets");
        assertEquals(new HashSet<>(Arrays.asList(expected)), new HashSet<>(Arrays.asList(actual)));
        assertFalse(Arrays.asList(actual).contains(BlockPos.ORIGIN), "Structure layout must not include the controller");
    }

    private static void assertCollector(BlockPos[] offsets, LayoutCollector collector) {
        BlockPos origin = new BlockPos(10, 64, -4);
        List<BlockPos> actual = new ArrayList<>();
        collector.collect(origin, (position, advanced) -> {
            assertFalse(advanced, "Addon layouts use normal bounding blocks");
            actual.add(position);
        });
        List<BlockPos> expected = new ArrayList<>();
        for (BlockPos offset : offsets) {
            expected.add(origin.add(offset));
        }
        assertEquals(expected, actual);
        assertEquals(actual.size(), new HashSet<>(actual).size(), "Collected layout contains duplicate positions");
        assertFalse(actual.contains(origin), "Collected layout must not include the controller");
    }

    @FunctionalInterface
    private interface LayoutCollector {

        void collect(BlockPos origin, BiConsumer<BlockPos, Boolean> consumer);
    }
}
