package mekceumoremachine.client.render.tileentity.machine;

import mekanism.client.render.MekanismRenderer;
import mekanism.common.util.MekanismUtils;
import mekceumoremachine.client.model.machine.ModelWirelessChargingStation;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.tile.machine.TileEntityWirelessChargingStation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@SideOnly(Side.CLIENT)
public class RenderWirelessChargingStation extends TileEntitySpecialRenderer<TileEntityWirelessChargingStation> {

    private static final float SCALE_ANIM_SPEED_PER_TICK = 0.025F;
    private static final int STALE_STATE_TICKS = 200;
    private static final int MAX_ANIM_STATES = 256;
    private static final Map<Long, ScaleAnimState> SCALE_ANIM_STATES = new HashMap<>();

    private ModelWirelessChargingStation model = new ModelWirelessChargingStation();

    @Override
    public void render(TileEntityWirelessChargingStation tileEntity, double x, double y, double z, float partialTick, int destroyStage, float alpha) {
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x + 0.5F, (float) y + 1.5F, (float) z + 0.5F);
        MekanismRenderer.rotate(tileEntity.facing, 0, 180, 90, 270);
        GlStateManager.rotate(180, 0, 0, 1);
        bindTexture(MekanismUtils.getResource(MEKCeuMoreMachine.MODID, MekanismUtils.ResourceType.RENDER, "Wireless_Charging_" + tileEntity.tier.getBaseTier().getSimpleName() + ".png"));
        model.renderModelTier(0.0625F);
        bindTexture(MekanismUtils.getResource(MEKCeuMoreMachine.MODID, MekanismUtils.ResourceType.RENDER, "Wireless_Charging.png"));
        model.renderModel(0.0625F);
        GlStateManager.popMatrix();
        float visualScale = getAnimatedScale(tileEntity, partialTick);
        if (tileEntity.clientRendering && visualScale > 0) {
            WirelessChargingStationVisualRenderer.render(tileEntity, visualScale);
        }
        MekanismRenderer.machineRenderer().render(tileEntity, x, y, z, partialTick, destroyStage, alpha);
    }

    private static float getAnimatedScale(TileEntityWirelessChargingStation tileEntity, float partialTick) {
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

    private static long buildAnimKey(TileEntityWirelessChargingStation tileEntity) {
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
