package mekceumoremachine.client.render.tileentity.machine;

import mekanism.client.render.MekanismRenderer;
import mekanism.common.util.MekanismUtils;
import mekceumoremachine.client.model.machine.ModelWirelessChargingEnergy;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.tile.machine.TileEntityWirelessChargingEnergy;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

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
        MekanismRenderer.machineRenderer().render(tileEntity, x, y, z, partialTick, destroyStage, alpha);
    }

    public static float getAnimatedScale(TileEntityWirelessChargingEnergy tileEntity, float partialTick) {
        if (tileEntity.getWorld() == null) {
            return tileEntity.clientRendering ? 1.0F : 0.0F;
        }
        long worldTime = tileEntity.getWorld().getTotalWorldTime();
        long key = buildAnimKey(tileEntity);
        float renderTime = worldTime + partialTick;
        ScaleAnimState state = SCALE_ANIM_STATES.get(key);
        if (state == null) {
            if (!tileEntity.clientRendering) {
                return 0.0F;
            }
            state = new ScaleAnimState();
            state.progress = 0.0F;
            state.lastRenderTime = renderTime;
            state.lastSeenWorldTime = worldTime;
            SCALE_ANIM_STATES.put(key, state);
        }

        float delta = renderTime - state.lastRenderTime;
        if (delta < 0) {
            delta = 0;
        } else if (delta > 5) {
            delta = 5;
        }
        float direction = tileEntity.clientRendering ? 1.0F : -1.0F;
        state.progress = Math.max(0.0F, Math.min(1.0F, state.progress + direction * delta * SCALE_ANIM_SPEED_PER_TICK));
        state.lastRenderTime = renderTime;

        state.lastSeenWorldTime = worldTime;
        if (SCALE_ANIM_STATES.size() > MAX_ANIM_STATES) {
            pruneStates(worldTime);
        }
        if (state.progress <= 0.0F && !tileEntity.clientRendering) {
            return 0.0F;
        }
        return tileEntity.clientRendering ? easeOutCubic(state.progress) : easeInCubic(state.progress);
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

    private static float easeInCubic(float progress) {
        float clamped = Math.max(0.0F, Math.min(progress, 1.0F));
        return clamped * clamped * clamped;
    }

    private static long buildAnimKey(TileEntityWirelessChargingEnergy tileEntity) {
        int dimension = tileEntity.getWorld().provider.getDimension();
        return tileEntity.getPos().toLong() ^ ((long) dimension << 56);
    }

    private static class ScaleAnimState {
        private float progress;
        private float lastRenderTime;
        private long lastSeenWorldTime;
    }
}
