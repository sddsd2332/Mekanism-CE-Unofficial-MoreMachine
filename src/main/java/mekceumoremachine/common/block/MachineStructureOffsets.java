package mekceumoremachine.common.block;

import net.minecraft.util.math.BlockPos;

import java.util.function.BiConsumer;

public final class MachineStructureOffsets {

    static final BlockPos[] ONE_ABOVE = {new BlockPos(0, 1, 0)};
    static final BlockPos[] TWO_ABOVE = {new BlockPos(0, 1, 0), new BlockPos(0, 2, 0)};
    static final BlockPos[] WIND_GENERATOR = {
          new BlockPos(0, 1, 0), new BlockPos(0, 2, 0), new BlockPos(0, 3, 0), new BlockPos(0, 4, 0)
    };
    static final BlockPos[] ADVANCED_SOLAR_GENERATOR = createAdvancedSolarGenerator();

    private MachineStructureOffsets() {
    }

    public static void collectOneAbove(BlockPos origin, BiConsumer<BlockPos, Boolean> consumer) {
        collect(origin, ONE_ABOVE, consumer);
    }

    public static void collectTwoAbove(BlockPos origin, BiConsumer<BlockPos, Boolean> consumer) {
        collect(origin, TWO_ABOVE, consumer);
    }

    public static void collectWindGenerator(BlockPos origin, BiConsumer<BlockPos, Boolean> consumer) {
        collect(origin, WIND_GENERATOR, consumer);
    }

    public static void collectAdvancedSolarGenerator(BlockPos origin, BiConsumer<BlockPos, Boolean> consumer) {
        collect(origin, ADVANCED_SOLAR_GENERATOR, consumer);
    }

    private static void collect(BlockPos origin, BlockPos[] offsets, BiConsumer<BlockPos, Boolean> consumer) {
        for (BlockPos offset : offsets) {
            consumer.accept(origin.add(offset), false);
        }
    }

    private static BlockPos[] createAdvancedSolarGenerator() {
        BlockPos[] offsets = new BlockPos[10];
        offsets[0] = new BlockPos(0, 1, 0);
        int index = 1;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                offsets[index++] = new BlockPos(x, 2, z);
            }
        }
        return offsets;
    }
}
