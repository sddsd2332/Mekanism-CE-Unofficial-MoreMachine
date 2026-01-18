package mekceumoremachine.client.render.tileentity.machine;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import mekanism.client.render.MekanismRenderer;
import mekanism.client.render.MekanismRenderer.DisplayInteger;
import mekanism.client.render.MekanismRenderer.Model3D;
import mekceumoremachine.common.tile.machine.TileEntityWirelessChargingEnergy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.init.Blocks;
import org.lwjgl.opengl.GL11;

import java.util.Map;

public class WirelessChargingEnergyVisualRenderer {

    private static final double offset = 0.01;
    private static Minecraft mc = Minecraft.getMinecraft();
    private static Map<MinerRenderData, DisplayInteger> cachedVisuals = new Object2ObjectOpenHashMap<>();

    public static void render(TileEntityWirelessChargingEnergy tile) {
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) getX(tile.getPos().getX()), (float) getY(tile.getPos().getY()), (float) getZ(tile.getPos().getZ()));
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.disableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        MekanismRenderer.GlowInfo glowInfo = MekanismRenderer.enableGlow();
        GlStateManager.enableCull();
        GlStateManager.color(1, 1, 1, 0.8F);
        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        getList(new MinerRenderData(tile)).render();
        MekanismRenderer.resetColor();
        GlStateManager.disableCull();
        MekanismRenderer.disableGlow(glowInfo);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.popMatrix();
    }

    private static DisplayInteger getList(MinerRenderData data) {
        if (cachedVisuals.containsKey(data)) {
            return cachedVisuals.get(data);
        }
        DisplayInteger display = DisplayInteger.createAndStart();
        cachedVisuals.put(data, display);
        Model3D toReturn = new Model3D();
        if (data.radius <= 64) {
            toReturn.setBlockBounds(-data.radius + 1.01, -data.radius - data.yCoord + 1.01, -data.radius + 1.01, data.radius + 1 - 1.01, data.radius - data.yCoord + 1 - 1.01, data.radius + 1 - 1.01);
            toReturn.baseBlock = Blocks.WATER;
            toReturn.setTexture(MekanismRenderer.energyIcon);
            MekanismRenderer.renderObject(toReturn);
        }
        DisplayInteger.endList();
        return display;
    }


    private static double getX(int x) {
        return x - TileEntityRendererDispatcher.staticPlayerX;
    }

    private static double getY(int y) {
        return y - TileEntityRendererDispatcher.staticPlayerY;
    }

    private static double getZ(int z) {
        return z - TileEntityRendererDispatcher.staticPlayerZ;
    }

    public static class MinerRenderData {

        public int radius;
        public int yCoord;

        public MinerRenderData(int rad, int y) {
            radius = rad;
            yCoord = y;
        }

        public MinerRenderData(TileEntityWirelessChargingEnergy miner) {
            this(miner.getRang(), miner.getPos().up(2).getY());
        }

        @Override
        public boolean equals(Object data) {
            return data instanceof MinerRenderData minerRenderData && minerRenderData.radius == radius && minerRenderData.yCoord == yCoord;

        }

        @Override
        public int hashCode() {
            int code = 1;
            code = 31 * code + radius;
            code = 31 * code + yCoord;
            return code;
        }
    }

}
