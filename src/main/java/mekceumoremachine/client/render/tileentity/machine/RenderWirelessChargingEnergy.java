package mekceumoremachine.client.render.tileentity.machine;

import mekanism.client.render.MekanismRenderer;
import mekanism.common.util.MekanismUtils;
import mekceumoremachine.client.model.machine.ModelWirelessChargingEnergy;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.attachments.component.ConnectionConfig;
import mekceumoremachine.common.item.ItemConnector;
import mekceumoremachine.common.tile.machine.TileEntityWirelessChargingEnergy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@SideOnly(Side.CLIENT)
public class RenderWirelessChargingEnergy extends TileEntitySpecialRenderer<TileEntityWirelessChargingEnergy> {

    private static final float SCALE_ANIM_SPEED_PER_TICK = 0.025F;
    private static final int STALE_STATE_TICKS = 200;
    private static final int MAX_ANIM_STATES = 256;
    private static final Map<Long, ScaleAnimState> SCALE_ANIM_STATES = new HashMap<>();

    private ModelWirelessChargingEnergy model = new ModelWirelessChargingEnergy();

    public static final int MAX_LIGHT_X = 0xF000F0;
    public static final int MAX_LIGHT_Y = 0xF000F0;

    @Override
    public void render(TileEntityWirelessChargingEnergy tileEntity, double x, double y, double z, float partialTick, int destroyStage, float alpha) {
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x + 0.5F, (float) y + 1.5F, (float) z + 0.5F);
        MekanismRenderer.rotate(tileEntity.facing, 0, 180, 90, 270);
        GlStateManager.rotate(180, 0, 0, 1);
        bindTexture(MekanismUtils.getResource(MEKCeuMoreMachine.MODID, MekanismUtils.ResourceType.RENDER, "Wireless_Energy_" + tileEntity.tier.getBaseTier().getSimpleName() + ".png"));
        model.renderModelTier(0.0625F);
        bindTexture(MekanismUtils.getResource(MEKCeuMoreMachine.MODID, MekanismUtils.ResourceType.RENDER, "Wireless_Energy.png"));
        model.renderModel(0.0625F);
        GlStateManager.popMatrix();
        float visualScale = getAnimatedScale(tileEntity, partialTick);
        if (tileEntity.clientRendering && visualScale > 0) {
            WirelessChargingEnergyVisualRenderer.render(tileEntity, visualScale);
        }
        if (isHandHeldConnector()) {
            BlockPos stationPos = tileEntity.getPos();
            for (ConnectionConfig config : tileEntity.getAllConnections()) {
                renderConnection(stationPos, config);
            }
        }
        MekanismRenderer.machineRenderer().render(tileEntity, x, y, z, partialTick, destroyStage, alpha);
    }


    private boolean isHandHeldConnector() {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player != null) {
            ItemStack mainHand = player.getHeldItemOffhand();
            ItemStack offHand = player.getHeldItemMainhand();
            return isConfigurator(mainHand) || isConfigurator(offHand);
        }
        return false;
    }

    private boolean isConfigurator(ItemStack stack) {
        return stack.getItem() instanceof ItemConnector;
    }


    private void renderConnection(BlockPos pos, ConnectionConfig config) {
        BlockPos targetPos = config.getPos();
        EnumFacing targetFace = config.getFacing();
        Vec3d machine = new Vec3d(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5);
        Vec3d faceOffset = getFaceOffset(targetFace);
        Vec3d end = machine.add(faceOffset);
        renderLaser(pos.getX() + 0.5, pos.getY() + 2.8125, pos.getZ() + 0.5, end, 0.35F, 0.05);
    }


    private Vec3d getFaceOffset(EnumFacing face) {
        // 根据方向偏移到面的中心
        // 偏移量为 0.5 (半个方块) 加上一个小的间隙 (0.01) 避免 Z-fighting
        float offset = 0.51f;
        return switch (face) {
            case UP -> new Vec3d(0, offset, 0);
            case DOWN -> new Vec3d(0, -offset, 0);
            case NORTH -> new Vec3d(0, 0, -offset);
            case SOUTH -> new Vec3d(0, 0, offset);
            case WEST -> new Vec3d(-offset, 0, 0);
            case EAST -> new Vec3d(offset, 0, 0);
        };
    }


    public void renderLaser(double firstX, double firstY, double firstZ, Vec3d second, float alpha, double beamWidth) {
        Tessellator tessy = Tessellator.getInstance();
        BufferBuilder render = tessy.getBuffer();
        float r = 0F;
        float g = 1F;
        float b = 0F;

        Vec3d vec1 = new Vec3d(firstX, firstY, firstZ);
        Vec3d vec2 = new Vec3d(second.x, second.y, second.z);
        Vec3d combinedVec = vec2.subtract(vec1);

        double pitch = Math.atan2(combinedVec.y, Math.sqrt(combinedVec.x * combinedVec.x + combinedVec.z * combinedVec.z));
        double yaw = Math.atan2(-combinedVec.z, combinedVec.x);

        double length = combinedVec.length();

        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        int func = GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC);
        float ref = GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);
        GlStateManager.alphaFunc(GL11.GL_ALWAYS, 0);
        GlStateManager.translate(firstX - TileEntityRendererDispatcher.staticPlayerX, firstY - TileEntityRendererDispatcher.staticPlayerY, firstZ - TileEntityRendererDispatcher.staticPlayerZ);
        GlStateManager.rotate((float) (180 * yaw / Math.PI), 0, 1, 0);
        GlStateManager.rotate((float) (180 * pitch / Math.PI), 0, 0, 1);

        GlStateManager.disableTexture2D();
        render.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_LMAP_COLOR);

        for (double i = 0; i < 4; i++) {
            double width = beamWidth * (i / 4.0);
            render.pos(length, width, width).tex(0, 0).lightmap(MAX_LIGHT_X, MAX_LIGHT_Y).color(r, g, b, alpha).endVertex();
            render.pos(0, width, width).tex(0, 0).lightmap(MAX_LIGHT_X, MAX_LIGHT_Y).color(r, g, b, alpha).endVertex();
            render.pos(0, -width, width).tex(0, 0).lightmap(MAX_LIGHT_X, MAX_LIGHT_Y).color(r, g, b, alpha).endVertex();
            render.pos(length, -width, width).tex(0, 0).lightmap(MAX_LIGHT_X, MAX_LIGHT_Y).color(r, g, b, alpha).endVertex();

            render.pos(length, -width, -width).tex(0, 0).lightmap(MAX_LIGHT_X, MAX_LIGHT_Y).color(r, g, b, alpha).endVertex();
            render.pos(0, -width, -width).tex(0, 0).lightmap(MAX_LIGHT_X, MAX_LIGHT_Y).color(r, g, b, alpha).endVertex();
            render.pos(0, width, -width).tex(0, 0).lightmap(MAX_LIGHT_X, MAX_LIGHT_Y).color(r, g, b, alpha).endVertex();
            render.pos(length, width, -width).tex(0, 0).lightmap(MAX_LIGHT_X, MAX_LIGHT_Y).color(r, g, b, alpha).endVertex();

            render.pos(length, width, -width).tex(0, 0).lightmap(MAX_LIGHT_X, MAX_LIGHT_Y).color(r, g, b, alpha).endVertex();
            render.pos(0, width, -width).tex(0, 0).lightmap(MAX_LIGHT_X, MAX_LIGHT_Y).color(r, g, b, alpha).endVertex();
            render.pos(0, width, width).tex(0, 0).lightmap(MAX_LIGHT_X, MAX_LIGHT_Y).color(r, g, b, alpha).endVertex();
            render.pos(length, width, width).tex(0, 0).lightmap(MAX_LIGHT_X, MAX_LIGHT_Y).color(r, g, b, alpha).endVertex();

            render.pos(length, -width, width).tex(0, 0).lightmap(MAX_LIGHT_X, MAX_LIGHT_Y).color(r, g, b, alpha).endVertex();
            render.pos(0, -width, width).tex(0, 0).lightmap(MAX_LIGHT_X, MAX_LIGHT_Y).color(r, g, b, alpha).endVertex();
            render.pos(0, -width, -width).tex(0, 0).lightmap(MAX_LIGHT_X, MAX_LIGHT_Y).color(r, g, b, alpha).endVertex();
            render.pos(length, -width, -width).tex(0, 0).lightmap(MAX_LIGHT_X, MAX_LIGHT_Y).color(r, g, b, alpha).endVertex();
        }
        tessy.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.alphaFunc(func, ref);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();

    }

    private static float getAnimatedScale(TileEntityWirelessChargingEnergy tileEntity, float partialTick) {
        if (tileEntity.getWorld() == null) {
            return tileEntity.clientRendering ? 1.0F : 0.0F;
        }
        long worldTime = tileEntity.getWorld().getTotalWorldTime();
        long key = buildAnimKey(tileEntity);
        ScaleAnimState state = SCALE_ANIM_STATES.computeIfAbsent(key, k -> new ScaleAnimState());
        float renderTime = worldTime + partialTick;

        if (!tileEntity.clientRendering) {
            state.enabled = false;
            state.progress = 0.0F;
            state.lastRenderTime = renderTime;
            state.lastSeenWorldTime = worldTime;
            if (SCALE_ANIM_STATES.size() > MAX_ANIM_STATES) {
                pruneStates(worldTime);
            }
            return 0.0F;
        }

        if (!state.enabled) {
            state.enabled = true;
            state.progress = 0.0F;
            state.lastRenderTime = renderTime;
        } else {
            float delta = renderTime - state.lastRenderTime;
            if (delta < 0) {
                delta = 0;
            } else if (delta > 5) {
                delta = 5;
            }
            state.progress = Math.min(1.0F, state.progress + delta * SCALE_ANIM_SPEED_PER_TICK);
            state.lastRenderTime = renderTime;
        }

        state.lastSeenWorldTime = worldTime;
        if (SCALE_ANIM_STATES.size() > MAX_ANIM_STATES) {
            pruneStates(worldTime);
        }
        return easeOutCubic(state.progress);
    }

    private static void pruneStates(long worldTime) {
        Iterator<Map.Entry<Long, ScaleAnimState>> iterator = SCALE_ANIM_STATES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, ScaleAnimState> entry = iterator.next();
            if (worldTime - entry.getValue().lastSeenWorldTime > STALE_STATE_TICKS) {
                iterator.remove();
            }
        }
    }

    private static float easeOutCubic(float progress) {
        float clamped = Math.max(0.0F, Math.min(progress, 1.0F));
        float inv = 1.0F - clamped;
        return 1.0F - inv * inv * inv;
    }

    private static long buildAnimKey(TileEntityWirelessChargingEnergy tileEntity) {
        int dimension = tileEntity.getWorld().provider.getDimension();
        return tileEntity.getPos().toLong() ^ ((long) dimension << 56);
    }

    private static class ScaleAnimState {
        private boolean enabled;
        private float progress;
        private float lastRenderTime;
        private long lastSeenWorldTime;
    }


}
