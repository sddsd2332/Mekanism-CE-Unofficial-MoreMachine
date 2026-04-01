package mekceumoremachine.client.render.tileentity.machine;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
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

import java.util.Iterator;
import java.lang.reflect.Method;

public class WirelessChargingEnergyVisualRenderer {

    private static final int MAX_CACHED_VISUALS = 16;
    private static Minecraft mc = Minecraft.getMinecraft();
    private static Int2ObjectOpenHashMap<DisplayInteger> cachedVisuals = new Int2ObjectOpenHashMap<>();
    private static Int2ObjectOpenHashMap<Long> lastUseTick = new Int2ObjectOpenHashMap<>();
    private static long usageTicker;

    public static void render(TileEntityWirelessChargingEnergy tile) {
        render(tile, 1.0F);
    }

    public static void render(TileEntityWirelessChargingEnergy tile, float scale) {
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) getX(tile.getPos().getX()), (float) getY(tile.getPos().getY()), (float) getZ(tile.getPos().getZ()));
        GlStateManager.scale(scale, scale, scale);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.disableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        MekanismRenderer.GlowInfo glowInfo = MekanismRenderer.enableGlow();
        GlStateManager.enableCull();
        GlStateManager.color(1, 1, 1, 0.8F);
        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        getList(tile.getRang()).render();
        MekanismRenderer.resetColor();
        GlStateManager.disableCull();
        MekanismRenderer.disableGlow(glowInfo);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.popMatrix();
    }

    private static DisplayInteger getList(int radius) {
        usageTicker++;
        DisplayInteger cached = cachedVisuals.get(radius);
        if (cached != null) {
            lastUseTick.put(radius, Long.valueOf(usageTicker));
            return cached;
        }
        evictIfNeeded();
        DisplayInteger display = DisplayInteger.createAndStart();
        cachedVisuals.put(radius, display);
        lastUseTick.put(radius, Long.valueOf(usageTicker));
        Model3D toReturn = new Model3D();
        toReturn.setBlockBounds(-radius + 1.01, -radius + 1.01, -radius + 1.01, radius + 1 - 1.01, radius + 1 - 1.01, radius + 1 - 1.01);
        toReturn.baseBlock = Blocks.WATER;
        toReturn.setTexture(MekanismRenderer.energyIcon);
        MekanismRenderer.renderObject(toReturn);
        DisplayInteger.endList();
        return display;
    }

    private static void evictIfNeeded() {
        if (cachedVisuals.size() < MAX_CACHED_VISUALS) {
            return;
        }
        int lruRadius = 0;
        long lruTick = Long.MAX_VALUE;
        for (Int2ObjectMap.Entry<Long> entry : lastUseTick.int2ObjectEntrySet()) {
            long tick = entry.getValue();
            if (tick < lruTick) {
                lruTick = tick;
                lruRadius = entry.getIntKey();
            }
        }
        DisplayInteger removed = cachedVisuals.remove(lruRadius);
        releaseDisplayList(removed);
        lastUseTick.remove(lruRadius);
    }

    private static void releaseDisplayList(DisplayInteger display) {
        if (display == null) {
            return;
        }
        String[] methods = {"delete", "deleteList", "dispose", "release"};
        for (String methodName : methods) {
            try {
                Method method = display.getClass().getMethod(methodName);
                method.invoke(display);
                return;
            } catch (ReflectiveOperationException ignored) {
            }
        }
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
}
