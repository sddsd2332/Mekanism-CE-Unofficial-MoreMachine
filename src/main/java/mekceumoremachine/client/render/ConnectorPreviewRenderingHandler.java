package mekceumoremachine.client.render;

import mekanism.api.Coord4D;
import mekceumoremachine.common.attachments.component.ConnectionConfig;
import mekceumoremachine.common.config.MoreMachineConfig;
import mekceumoremachine.common.item.ItemConnector;
import mekceumoremachine.common.tile.interfaces.IConnectorPreviewProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.*;

@SideOnly(Side.CLIENT)
public class ConnectorPreviewRenderingHandler {

    private static final int CYLINDER_SIDES = 24;
    private static final double CYLINDER_ANGLE_STEP = 2.0 * Math.PI / CYLINDER_SIDES;
    private static final int SPHERE_SLICES = 24;
    private static final int SPHERE_STACKS = 16;

    private static final float GLOW_RADIUS = 0.09F;
    private static final float CORE_RADIUS = 0.035F;
    private static final float ENDPOINT_GLOW_RADIUS = 0.12F;
    private static final float ENDPOINT_CORE_RADIUS = 0.055F;
    private static final float COLOR_R = 0.2F;
    private static final float COLOR_G = 1.0F;
    private static final float COLOR_B = 0.2F;

    private int currentDim = Integer.MIN_VALUE;
    private BlockPos currentSourcePos = null;
    private Vec3d currentSourceOrigin = null;
    private final Set<PreviewLine> cachedLines = new HashSet<>();
    private long lastSyncWorldTime = Long.MIN_VALUE;

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        World world = mc.world;
        if (player == null || world == null) {
            clearPreviewState();
            return;
        }

        BoundSource bound = getBoundSource(player, world.provider.getDimension());
        if (bound == null) {
            clearPreviewState();
            return;
        }

        boolean sourceChanged = currentSourcePos == null || !currentSourcePos.equals(bound.pos) || currentDim != bound.dim;
        if (sourceChanged) {
            currentSourcePos = bound.pos;
            currentDim = bound.dim;
            syncPreviewState(world, bound, true);
            lastSyncWorldTime = world.getTotalWorldTime();
        } else {
            long worldTime = world.getTotalWorldTime();
            if (worldTime != lastSyncWorldTime) {
                syncPreviewState(world, bound, false);
                lastSyncWorldTime = worldTime;
            }
        }

        if (currentSourceOrigin == null) {
            return;
        }

        double cameraX = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.getPartialTicks();
        double cameraY = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.getPartialTicks();
        double cameraZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.getPartialTicks();

        GlStateManager.pushMatrix();
        GlStateManager.translate(-cameraX, -cameraY, -cameraZ);
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);

        // Source marker: draw once at previewProvider.getPreviewOrigin().
        drawSphere(currentSourceOrigin, ENDPOINT_GLOW_RADIUS, COLOR_R, COLOR_G, COLOR_B, 0.25F);
        drawSphere(currentSourceOrigin, ENDPOINT_CORE_RADIUS, COLOR_R, COLOR_G, COLOR_B, 0.95F);

        Vec3d viewerPos = new Vec3d(cameraX, cameraY, cameraZ);
        for (RenderedLine renderedLine : collectLinesToRender(cachedLines, viewerPos, bound.showAll)) {
            Vec3d end = renderedLine.end;
            drawLaserCylinder(currentSourceOrigin, end, GLOW_RADIUS, COLOR_R, COLOR_G, COLOR_B, 0.25F);
            drawLaserCylinder(currentSourceOrigin, end, CORE_RADIUS, COLOR_R, COLOR_G, COLOR_B, 0.95F);
            drawSphere(end, ENDPOINT_GLOW_RADIUS, COLOR_R, COLOR_G, COLOR_B, 0.25F);
            drawSphere(end, ENDPOINT_CORE_RADIUS, COLOR_R, COLOR_G, COLOR_B, 0.95F);
        }

        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private BoundSource getBoundSource(EntityPlayer player, int currentDim) {
        BoundSource main = getBoundSource(player.getHeldItemMainhand(), currentDim);
        if (main != null) {
            return main;
        }
        return getBoundSource(player.getHeldItemOffhand(), currentDim);
    }

    private BoundSource getBoundSource(ItemStack stack, int currentDim) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemConnector connector)) {
            return null;
        }
        Coord4D data = connector.getDataType(stack);
        if (data == null || data.dimensionId != currentDim) {
            return null;
        }
        return new BoundSource(data.getPos(), data.dimensionId, connector.isPreviewShowAll(stack));
    }

    private void syncPreviewState(World world, BoundSource bound, boolean forceSet) {
        if (!world.isBlockLoaded(bound.pos)) {
            clearPreviewState();
            return;
        }
        TileEntity tileEntity = world.getTileEntity(bound.pos);
        if (!(tileEntity instanceof IConnectorPreviewProvider previewProvider)) {
            clearPreviewState();
            return;
        }

        Vec3d previewOrigin = previewProvider.getPreviewOrigin();
        if (previewOrigin == null) {
            clearPreviewState();
            return;
        }

        Set<PreviewLine> incoming = new HashSet<>();
        List<ConnectionConfig> sourceConnections = previewProvider.getPreviewConnections();
        if (sourceConnections == null) {
            sourceConnections = Collections.emptyList();
        }
        for (ConnectionConfig config : new ArrayList<>(sourceConnections)) {
            if (config != null) {
                incoming.add(new PreviewLine(config.getPos(), config.getFacing()));
            }
        }

        currentSourceOrigin = previewOrigin;
        if (forceSet) {
            cachedLines.clear();
            cachedLines.addAll(incoming);
        } else {
            cachedLines.removeIf(line -> !incoming.contains(line));
            cachedLines.addAll(incoming);
        }
    }

    private void clearPreviewState() {
        currentDim = Integer.MIN_VALUE;
        currentSourcePos = null;
        currentSourceOrigin = null;
        lastSyncWorldTime = Long.MIN_VALUE;
        cachedLines.clear();
    }

    private static Vec3d getTargetEndpoint(PreviewLine line) {
        Vec3d center = new Vec3d(line.pos.getX() + 0.5, line.pos.getY() + 0.5, line.pos.getZ() + 0.5);
        float offset = 0.51F;
        return center.add(switch (line.facing) {
            case UP -> new Vec3d(0, offset, 0);
            case DOWN -> new Vec3d(0, -offset, 0);
            case NORTH -> new Vec3d(0, 0, -offset);
            case SOUTH -> new Vec3d(0, 0, offset);
            case WEST -> new Vec3d(-offset, 0, 0);
            case EAST -> new Vec3d(offset, 0, 0);
        });
    }

    private static List<RenderedLine> collectLinesToRender(Set<PreviewLine> lines, Vec3d viewerPos, boolean showAll) {
        if (lines.isEmpty()) {
            return Collections.emptyList();
        }
        List<RenderedLine> resolvedLines = new ArrayList<>(lines.size());
        for (PreviewLine line : lines) {
            Vec3d end = getTargetEndpoint(line);
            resolvedLines.add(new RenderedLine(end, end.squareDistanceTo(viewerPos)));
        }
        if (showAll) {
            return resolvedLines;
        }
        int nearestLimit = MoreMachineConfig.local().client.ConnectorPreviewNearestLineLimit.val();
        if (resolvedLines.size() <= nearestLimit) {
            return resolvedLines;
        }
        resolvedLines.sort(Comparator.comparingDouble(line -> line.distanceSq));
        return new ArrayList<>(resolvedLines.subList(0, nearestLimit));
    }

    private static void drawLaserCylinder(Vec3d from, Vec3d to, float radius, float r, float g, float b, float alpha) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-6) {
            return;
        }

        double ax = dx / len;
        double ay = dy / len;
        double az = dz / len;

        double bx;
        double by;
        double bz;
        if (Math.abs(ax) <= Math.abs(ay) && Math.abs(ax) <= Math.abs(az)) {
            bx = 0;
            by = -az;
            bz = ay;
        } else if (Math.abs(ay) <= Math.abs(az)) {
            bx = -az;
            by = 0;
            bz = ax;
        } else {
            bx = -ay;
            by = ax;
            bz = 0;
        }

        double bLen = Math.sqrt(bx * bx + by * by + bz * bz);
        bx /= bLen;
        by /= bLen;
        bz /= bLen;

        double cx = ay * bz - az * by;
        double cy = az * bx - ax * bz;
        double cz = ax * by - ay * bx;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        GlStateManager.color(r, g, b, alpha);
        buffer.begin(GL11.GL_QUAD_STRIP, DefaultVertexFormats.POSITION);
        for (int i = 0; i <= CYLINDER_SIDES; i++) {
            double angle = CYLINDER_ANGLE_STEP * i;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double nx = radius * (cos * bx + sin * cx);
            double ny = radius * (cos * by + sin * cy);
            double nz = radius * (cos * bz + sin * cz);
            buffer.pos(from.x + nx, from.y + ny, from.z + nz).endVertex();
            buffer.pos(to.x + nx, to.y + ny, to.z + nz).endVertex();
        }
        tessellator.draw();
    }

    private static void drawSphere(Vec3d center, float radius, float r, float g, float b, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        GlStateManager.color(r, g, b, alpha);

        for (int stack = 0; stack < SPHERE_STACKS; stack++) {
            double phi1 = Math.PI * stack / SPHERE_STACKS;
            double phi2 = Math.PI * (stack + 1) / SPHERE_STACKS;
            double y1 = Math.cos(phi1);
            double y2 = Math.cos(phi2);
            double ring1 = Math.sin(phi1);
            double ring2 = Math.sin(phi2);

            buffer.begin(GL11.GL_QUAD_STRIP, DefaultVertexFormats.POSITION);
            for (int slice = 0; slice <= SPHERE_SLICES; slice++) {
                double theta = 2.0 * Math.PI * slice / SPHERE_SLICES;
                double cos = Math.cos(theta);
                double sin = Math.sin(theta);
                buffer.pos(center.x + radius * ring1 * cos, center.y + radius * y1, center.z + radius * ring1 * sin).endVertex();
                buffer.pos(center.x + radius * ring2 * cos, center.y + radius * y2, center.z + radius * ring2 * sin).endVertex();
            }
            tessellator.draw();
        }
    }

    private static class BoundSource {
        private final BlockPos pos;
        private final int dim;
        private final boolean showAll;

        private BoundSource(BlockPos pos, int dim, boolean showAll) {
            this.pos = pos;
            this.dim = dim;
            this.showAll = showAll;
        }
    }

    private static class RenderedLine {
        private final Vec3d end;
        private final double distanceSq;

        private RenderedLine(Vec3d end, double distanceSq) {
            this.end = end;
            this.distanceSq = distanceSq;
        }
    }

    private static class PreviewLine {
        private final BlockPos pos;
        private final EnumFacing facing;

        private PreviewLine(BlockPos pos, EnumFacing facing) {
            this.pos = pos;
            this.facing = facing;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof PreviewLine other)) {
                return false;
            }
            return Objects.equals(pos, other.pos) && facing == other.facing;
        }

        @Override
        public int hashCode() {
            return Objects.hash(pos, facing);
        }
    }
}
