package mekceumoremachine.client.render.tileentity.machine;

import mekceumoremachine.common.tile.machine.TileEntityWirelessChargingStation;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

public class WirelessChargingStationVisualRenderer {

    private static final double LINE_OFFSET = 0.0025D;
    private static final float COLOR_RED = 0.2F;
    private static final float COLOR_GREEN = 1.0F;
    private static final float COLOR_BLUE = 0.2F;
    private static final float NORTH_SOUTH_ALPHA = 0.82F;
    private static final float EAST_WEST_ALPHA = 0.78F;
    private static final float TOP_BOTTOM_ALPHA = 0.72F;
    private static final double SHRINK_PIVOT_X = 0.5D;
    private static final double SHRINK_PIVOT_Y = 2.8125D;
    private static final double SHRINK_PIVOT_Z = 0.5D;

    public static void render(TileEntityWirelessChargingStation tile) {
        render(tile, 1.0F);
    }

    public static void render(TileEntityWirelessChargingStation tile, float scale) {
        render(tile, scale, TileEntityRendererDispatcher.staticPlayerX, TileEntityRendererDispatcher.staticPlayerY, TileEntityRendererDispatcher.staticPlayerZ, TileEntityRendererDispatcher.staticPlayerY);
    }

    public static void render(TileEntityWirelessChargingStation tile, float scale, double cameraX, double cameraY, double cameraZ) {
        render(tile, scale, cameraX, cameraY, cameraZ, cameraY);
    }

    public static void render(TileEntityWirelessChargingStation tile, float scale, double cameraX, double cameraY, double cameraZ, double cameraEyeY) {
        int radius = tile.getRang();
        double minY = tile.getPos().getY() - radius + 1.01;
        double maxY = tile.getPos().getY() + radius - 0.01;
        boolean renderTop = cameraEyeY > maxY;
        boolean renderBottom = cameraEyeY < minY;
        render(tile, scale, cameraX, cameraY, cameraZ, renderTop, renderBottom);
    }

    public static void render(TileEntityWirelessChargingStation tile, float scale, double cameraX, double cameraY, double cameraZ, boolean renderTop, boolean renderBottom) {
        int radius = tile.getRang();
        double min = -radius + 1.01 + LINE_OFFSET;
        double max = radius + 1 - 1.01 - LINE_OFFSET;
        double pivotWorldX = tile.getPos().getX() + SHRINK_PIVOT_X;
        double pivotWorldY = tile.getPos().getY() + SHRINK_PIVOT_Y;
        double pivotWorldZ = tile.getPos().getZ() + SHRINK_PIVOT_Z;
        double scaledMinX = pivotWorldX + (min - SHRINK_PIVOT_X) * scale;
        double scaledMaxX = pivotWorldX + (max - SHRINK_PIVOT_X) * scale;
        double scaledMinY = pivotWorldY + (min - SHRINK_PIVOT_Y) * scale;
        double scaledMaxY = pivotWorldY + (max - SHRINK_PIVOT_Y) * scale;
        double scaledMinZ = pivotWorldZ + (min - SHRINK_PIVOT_Z) * scale;
        double scaledMaxZ = pivotWorldZ + (max - SHRINK_PIVOT_Z) * scale;
        boolean insidePiece = cameraX >= scaledMinX && cameraX <= scaledMaxX &&
                              cameraY >= scaledMinY && cameraY <= scaledMaxY &&
                              cameraZ >= scaledMinZ && cameraZ <= scaledMaxZ;

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) (pivotWorldX - cameraX), (float) (pivotWorldY - cameraY), (float) (pivotWorldZ - cameraZ));
        GlStateManager.scale(scale, scale, scale);
        GlStateManager.translate((float) -SHRINK_PIVOT_X, (float) -SHRINK_PIVOT_Y, (float) -SHRINK_PIVOT_Z);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableCull();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
              GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        renderCube(buffer, min, max, renderTop, renderBottom, insidePiece);
        tessellator.draw();

        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.disableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    private static void renderCube(BufferBuilder buffer, double min, double max, boolean renderTop, boolean renderBottom, boolean insidePiece) {
        addFace(buffer,
              min, min, min,
              min, max, min,
              max, max, min,
              max, min, min,
              NORTH_SOUTH_ALPHA, insidePiece);
        addFace(buffer,
              min, min, max,
              max, min, max,
              max, max, max,
              min, max, max,
              NORTH_SOUTH_ALPHA, insidePiece);
        addFace(buffer,
              min, min, min,
              min, min, max,
              min, max, max,
              min, max, min,
              EAST_WEST_ALPHA, insidePiece);
        addFace(buffer,
              max, min, min,
              max, max, min,
              max, max, max,
              max, min, max,
              EAST_WEST_ALPHA, insidePiece);
        if (renderTop) {
            addDoubleSidedFace(buffer,
                  min, max, min,
                  max, max, min,
                  max, max, max,
                  min, max, max,
                  TOP_BOTTOM_ALPHA);
        }
        if (renderBottom) {
            addDoubleSidedFace(buffer,
                  min, min, min,
                  min, min, max,
                  max, min, max,
                  max, min, min,
                  TOP_BOTTOM_ALPHA);
        }
    }

    private static void addFace(BufferBuilder buffer,
                                double x1, double y1, double z1,
                                double x2, double y2, double z2,
                                double x3, double y3, double z3,
                                double x4, double y4, double z4,
                                float alpha, boolean insidePiece) {
        if (insidePiece) {
            addQuad(buffer, x4, y4, z4, x3, y3, z3, x2, y2, z2, x1, y1, z1, alpha);
        } else {
            addQuad(buffer, x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4, alpha);
            addQuad(buffer, x4, y4, z4, x3, y3, z3, x2, y2, z2, x1, y1, z1, alpha);
        }
    }

    private static void addDoubleSidedFace(BufferBuilder buffer,
                                           double x1, double y1, double z1,
                                           double x2, double y2, double z2,
                                           double x3, double y3, double z3,
                                           double x4, double y4, double z4,
                                           float alpha) {
        addQuad(buffer, x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4, alpha);
        addQuad(buffer, x4, y4, z4, x3, y3, z3, x2, y2, z2, x1, y1, z1, alpha);
    }

    private static void addQuad(BufferBuilder buffer,
                                double x1, double y1, double z1,
                                double x2, double y2, double z2,
                                double x3, double y3, double z3,
                                double x4, double y4, double z4,
                                float alpha) {
        buffer.pos(x1, y1, z1).color(COLOR_RED, COLOR_GREEN, COLOR_BLUE, alpha).endVertex();
        buffer.pos(x2, y2, z2).color(COLOR_RED, COLOR_GREEN, COLOR_BLUE, alpha).endVertex();
        buffer.pos(x3, y3, z3).color(COLOR_RED, COLOR_GREEN, COLOR_BLUE, alpha).endVertex();
        buffer.pos(x4, y4, z4).color(COLOR_RED, COLOR_GREEN, COLOR_BLUE, alpha).endVertex();
    }
}
