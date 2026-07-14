package mekceumoremachine.client.render;

import mekanism.api.Coord4D;
import mekanism.client.render.JsonModelSelectionBoxCache;
import mekanism.client.render.SelectionWireframeRenderer;
import mekanism.client.render.SpecialSelectionWireframeRegistry;
import mekanism.common.block.BlockBounding;
import mekanism.common.block.interfaces.IHighlightBoxProvider;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@SideOnly(Side.CLIENT)
public final class WirelessConnectionHighlightHandler {

    public static final WirelessConnectionHighlightHandler INSTANCE = new WirelessConnectionHighlightHandler();

    private static final long DURATION_MS = 20_000L;
    private static final long BLINK_INTERVAL_MS = 300L;
    private static final double BOX_GROW = 0.03D;
    private static final float RED = 1.0F;
    private static final float GREEN = 0.35F;
    private static final float BLUE = 0.35F;
    private static final float ALPHA = 0.95F;

    private final Set<BlockPos> locations = new LinkedHashSet<>();
    private int highlightedDimension = Integer.MIN_VALUE;
    private long expiresAt;

    private WirelessConnectionHighlightHandler() {
    }

    public void showLocation(Coord4D location) {
        showLocations(Collections.singletonList(location));
    }

    public void showLocations(Collection<Coord4D> newLocations) {
        Minecraft minecraft = Minecraft.getMinecraft();
        clear();
        if (minecraft.world == null || minecraft.player == null || newLocations == null || newLocations.isEmpty()) {
            return;
        }

        int currentDimension = minecraft.world.provider.getDimension();
        for (Coord4D location : newLocations) {
            if (location != null && location.dimensionId == currentDimension) {
                locations.add(new BlockPos(location.x, location.y, location.z));
            }
        }
        if (!locations.isEmpty()) {
            highlightedDimension = currentDimension;
            expiresAt = System.currentTimeMillis() + DURATION_MS;
        }
    }

    @SubscribeEvent
    public void renderWorldLast(RenderWorldLastEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.world == null || minecraft.player == null) {
            clear();
            return;
        }
        if (locations.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (minecraft.world.provider.getDimension() != highlightedDimension || now > expiresAt) {
            clear();
            return;
        }
        if (((now / BLINK_INTERVAL_MS) & 1L) == 1L) {
            return;
        }

        double cameraX = minecraft.getRenderManager().viewerPosX;
        double cameraY = minecraft.getRenderManager().viewerPosY;
        double cameraZ = minecraft.getRenderManager().viewerPosZ;
        boolean mekanismSelectionEnabled = SelectionWireframeRenderer.isSelectionWireframeRenderingEnabled();

        SelectionWireframeRenderer.begin(SelectionWireframeRenderer.getConfiguredLineWidth(), true);
        try {
            for (BlockPos location : locations) {
                renderLocation(minecraft.world, location, cameraX, cameraY, cameraZ, mekanismSelectionEnabled);
            }
        } finally {
            SelectionWireframeRenderer.end(true);
        }
    }

    private static void renderLocation(World world, BlockPos location, double cameraX, double cameraY, double cameraZ,
          boolean mekanismSelectionEnabled) {
        BlockPos renderPos = location;
        IBlockState state = null;
        if (world.isBlockLoaded(location, false)) {
            try {
                state = world.getBlockState(location);
                Block block = state.getBlock();
                if (block instanceof BlockBounding) {
                    BlockPos mainPos = BlockBounding.getMainBlockPos(world, location);
                    if (mainPos != null && world.isBlockLoaded(mainPos, false)) {
                        renderPos = mainPos;
                        state = world.getBlockState(mainPos);
                    }
                }
            } catch (RuntimeException ignored) {
                state = null;
                renderPos = location;
            }
        }

        if (state != null) {
            if (mekanismSelectionEnabled && renderMekanismSelection(state, world, renderPos, cameraX, cameraY, cameraZ)) {
                return;
            }
            try {
                AxisAlignedBB selectedBox = state.getSelectedBoundingBox(world, renderPos);
                if (drawWorldBox(selectedBox, cameraX, cameraY, cameraZ)) {
                    return;
                }
            } catch (RuntimeException ignored) {
            }
        }
        drawWorldBox(new AxisAlignedBB(renderPos), cameraX, cameraY, cameraZ);
    }

    private static boolean renderMekanismSelection(IBlockState state, World world, BlockPos pos,
          double cameraX, double cameraY, double cameraZ) {
        Block block = state.getBlock();
        if (block instanceof IHighlightBoxProvider provider) {
            AxisAlignedBB[] boxes = null;
            try {
                boxes = provider.getHighlightBoxes(state, world, pos);
            } catch (RuntimeException ignored) {
            }
            if (drawLocalBoxes(boxes, pos, cameraX, cameraY, cameraZ)) {
                return true;
            }
        }

        JsonModelSelectionBoxCache.OutlineBox[] wireframes = null;
        try {
            wireframes = SpecialSelectionWireframeRegistry.getWireframes(state, world, pos);
        } catch (RuntimeException ignored) {
        }
        if (wireframes == null || wireframes.length == 0) {
            try {
                wireframes = JsonModelSelectionBoxCache.getWireframes(state, world, pos);
            } catch (RuntimeException ignored) {
            }
        }
        if (wireframes == null || wireframes.length == 0) {
            return false;
        }

        SelectionWireframeRenderer.drawWireframes(wireframes, pos, cameraX, cameraY, cameraZ,
              RED, GREEN, BLUE, ALPHA, SelectionWireframeRenderer.keepVisibleInternalEdgesForState(state));
        return true;
    }

    private static boolean drawLocalBoxes(AxisAlignedBB[] boxes, BlockPos pos,
          double cameraX, double cameraY, double cameraZ) {
        if (boxes == null || boxes.length == 0) {
            return false;
        }
        boolean rendered = false;
        for (AxisAlignedBB box : boxes) {
            if (isValidBox(box)) {
                drawWorldBox(box.offset(pos), cameraX, cameraY, cameraZ);
                rendered = true;
            }
        }
        return rendered;
    }

    private static boolean drawWorldBox(AxisAlignedBB box, double cameraX, double cameraY, double cameraZ) {
        if (!isValidBox(box)) {
            return false;
        }
        RenderGlobal.drawSelectionBoundingBox(box.grow(BOX_GROW).offset(-cameraX, -cameraY, -cameraZ),
              RED, GREEN, BLUE, ALPHA);
        return true;
    }

    private static boolean isValidBox(AxisAlignedBB box) {
        return box != null && Double.isFinite(box.minX) && Double.isFinite(box.minY) && Double.isFinite(box.minZ) &&
               Double.isFinite(box.maxX) && Double.isFinite(box.maxY) && Double.isFinite(box.maxZ) &&
               box.maxX > box.minX && box.maxY > box.minY && box.maxZ > box.minZ;
    }

    private void clear() {
        locations.clear();
        highlightedDimension = Integer.MIN_VALUE;
        expiresAt = 0;
    }
}
