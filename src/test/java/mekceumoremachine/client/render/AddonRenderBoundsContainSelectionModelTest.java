package mekceumoremachine.client.render;

import mekanism.client.render.JsonModelSelectionBoxCache;
import mekanism.client.render.SpecialSelectionWireframeRegistry;
import mekanism.common.config.MekanismConfig;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.generator.TileEntityTierAdvancedSolarGenerator;
import mekceumoremachine.common.tile.generator.TileEntityTierGasGenerator;
import mekceumoremachine.common.tile.generator.TileEntityTierWindGenerator;
import mekceumoremachine.common.tile.machine.TierDissolution.TileEntityTierChemicalDissolutionChamber;
import mekceumoremachine.common.tile.machine.TierNutritional.TileEntityTierNutritionalLiquifier;
import mekceumoremachine.common.tile.machine.TileEntityTierIsotopicCentrifuge;
import mekceumoremachine.common.tile.machine.TileEntityTierRotaryCondensentrator;
import mekceumoremachine.common.tile.machine.TileEntityTierSolarNeutronActivator;
import mekceumoremachine.common.tile.machine.TileEntityVoidMineralGenerator;
import mekceumoremachine.common.tile.machine.TileEntityWirelessChargingEnergy;
import mekceumoremachine.common.tile.machine.TileEntityWirelessChargingStation;
import mekceumoremachine.common.tile.machine.replicator.TileEntityReplicatorFluidStack;
import mekceumoremachine.common.tile.machine.replicator.TileEntityReplicatorGases;
import mekceumoremachine.common.tile.machine.replicator.TileEntityReplicatorItemStack;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.Loader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddonRenderBoundsContainSelectionModelTest {

    private static final BlockPos ORIGIN = new BlockPos(10, 64, -4);
    private boolean previousSelectionSetting;

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

    @BeforeEach
    void enableSelectionWireframes() {
        previousSelectionSetting = MekanismConfig.current().client.enableSelectionWireframeRendering.val();
        MekanismConfig.current().client.enableSelectionWireframeRendering.set(true);
    }

    @AfterEach
    void restoreSelectionWireframes() {
        MekanismConfig.current().client.enableSelectionWireframeRendering.set(previousSelectionSetting);
    }

    @Test
    void addonModelRenderersStayInsideFiniteTileBounds() {
        assertAll(
              () -> assertContains(new TileEntityVoidMineralGenerator()),
              () -> assertContains(new TileEntityTierSolarNeutronActivator()),
              () -> assertContains(new TileEntityTierIsotopicCentrifuge()),
              () -> assertContains(new TileEntityTierRotaryCondensentrator()),
              () -> assertContains(new TileEntityTierChemicalDissolutionChamber(MachineTier.BASIC)),
              () -> assertContains(new TileEntityTierNutritionalLiquifier(MachineTier.BASIC)),
              () -> assertContains(new TileEntityReplicatorItemStack()),
              () -> assertContains(new TileEntityReplicatorGases()),
              () -> assertContains(new TileEntityReplicatorFluidStack()),
              () -> assertContains(new TileEntityWirelessChargingStation()),
              () -> assertContains(new TileEntityWirelessChargingEnergy()),
              () -> assertContains(new TileEntityTierGasGenerator()),
              () -> assertContains(new TileEntityTierAdvancedSolarGenerator()),
              () -> assertContains(new TileEntityTierWindGenerator())
        );
    }

    private static void assertContains(TileEntity tile) {
        tile.setPos(ORIGIN);
        if (tile instanceof mekanism.common.tile.prefab.TileEntityBasicBlock basic) {
            basic.facing = EnumFacing.NORTH;
        }
        IBlockState state = Blocks.STONE.getDefaultState();
        IBlockAccess world = new SingleTileBlockAccess(tile, state);
        JsonModelSelectionBoxCache.OutlineBox[] outlines = SpecialSelectionWireframeRegistry.getWireframes(state, world, ORIGIN);
        AxisAlignedBB modelBounds = null;
        for (JsonModelSelectionBoxCache.OutlineBox outline : outlines) {
            if (outline == null || outline.getBounds() == null) {
                continue;
            }
            AxisAlignedBB worldBounds = outline.getBounds().offset(ORIGIN);
            modelBounds = modelBounds == null ? worldBounds : modelBounds.union(worldBounds);
        }
        AxisAlignedBB renderBounds = tile.getRenderBoundingBox();
        assertTrue(modelBounds != null && renderBounds.minX <= modelBounds.minX + 1.0E-6D
                    && renderBounds.minY <= modelBounds.minY + 1.0E-6D
                    && renderBounds.minZ <= modelBounds.minZ + 1.0E-6D
                    && renderBounds.maxX >= modelBounds.maxX - 1.0E-6D
                    && renderBounds.maxY >= modelBounds.maxY - 1.0E-6D
                    && renderBounds.maxZ >= modelBounds.maxZ - 1.0E-6D,
              tile.getClass().getSimpleName() + " model=" + modelBounds + " render=" + renderBounds);
    }

    private static final class SingleTileBlockAccess implements IBlockAccess {

        private final TileEntity tile;
        private final IBlockState state;

        private SingleTileBlockAccess(TileEntity tile, IBlockState state) {
            this.tile = tile;
            this.state = state;
        }

        @Override
        public TileEntity getTileEntity(BlockPos pos) {
            return ORIGIN.equals(pos) ? tile : null;
        }

        @Override
        public IBlockState getBlockState(BlockPos pos) {
            return state;
        }

        @Override
        public int getCombinedLight(BlockPos pos, int lightValue) {
            return lightValue;
        }

        @Override
        public boolean isAirBlock(BlockPos pos) {
            return false;
        }

        @Override
        public Biome getBiome(BlockPos pos) {
            return Biome.getBiome(1);
        }

        @Override
        public int getStrongPower(BlockPos pos, EnumFacing direction) {
            return 0;
        }

        @Override
        public WorldType getWorldType() {
            return WorldType.DEFAULT;
        }

        @Override
        public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean defaultValue) {
            return defaultValue;
        }
    }
}
