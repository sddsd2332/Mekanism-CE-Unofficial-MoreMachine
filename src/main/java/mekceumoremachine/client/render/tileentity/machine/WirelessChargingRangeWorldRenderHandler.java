package mekceumoremachine.client.render.tileentity.machine;

import mekceumoremachine.common.tile.machine.TileEntityWirelessChargingEnergy;
import mekceumoremachine.common.tile.machine.TileEntityWirelessChargingStation;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class WirelessChargingRangeWorldRenderHandler {

    private World cachedWorld;
    private long cachedWorldTime = Long.MIN_VALUE;
    private TileEntity cachedNearestTarget;

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.player;
        World world = minecraft.world;
        if (player == null || world == null || world.loadedTileEntityList.isEmpty()) {
            clearCachedTarget();
            return;
        }

        RenderTarget target = findNearestTarget(world, player, event.getPartialTicks());
        if (target == null) {
            return;
        }

        double cameraX = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.getPartialTicks();
        double cameraY = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.getPartialTicks();
        double cameraZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.getPartialTicks();
        Vec3d eyePosition = player.getPositionEyes(event.getPartialTicks());

        if (target.energy != null && target.scale > 0) {
            int radius = target.energy.getRang();
            boolean renderTop = shouldRenderFace(world, eyePosition, target.energy.getPos(), radius, true);
            boolean renderBottom = shouldRenderFace(world, eyePosition, target.energy.getPos(), radius, false);
            WirelessChargingEnergyVisualRenderer.render(target.energy, target.scale, cameraX, cameraY, cameraZ, renderTop, renderBottom);
        } else if (target.station != null) {
            if (target.scale > 0) {
                int radius = target.station.getRang();
                boolean renderTop = shouldRenderFace(world, eyePosition, target.station.getPos(), radius, true);
                boolean renderBottom = shouldRenderFace(world, eyePosition, target.station.getPos(), radius, false);
                WirelessChargingStationVisualRenderer.render(target.station, target.scale, cameraX, cameraY, cameraZ, renderTop, renderBottom);
            }
        }
    }

    private static boolean shouldRenderFace(World world, Vec3d eyePosition, BlockPos center, int radius, boolean topFace) {
        return isFaceVisible(world, eyePosition, center, radius, topFace);
    }

    private static boolean isFaceVisible(World world, Vec3d eyePosition, BlockPos center, int radius, boolean topFace) {
        double minX = center.getX() - radius + 1.01;
        double maxX = center.getX() + radius + 1 - 1.01;
        double minZ = center.getZ() - radius + 1.01;
        double maxZ = center.getZ() + radius + 1 - 1.01;
        double faceY = topFace ? center.getY() + radius - 0.01 : center.getY() - radius + 1.01;
        double[][] points = {
              {(minX + maxX) * 0.5, faceY, (minZ + maxZ) * 0.5},
              {minX + 0.2, faceY, minZ + 0.2},
              {minX + 0.2, faceY, maxZ - 0.2},
              {maxX - 0.2, faceY, minZ + 0.2},
              {maxX - 0.2, faceY, maxZ - 0.2}
        };
        for (double[] point : points) {
            Vec3d target = new Vec3d(point[0], point[1], point[2]);
            RayTraceResult hit = world.rayTraceBlocks(eyePosition, target, false, true, false);
            if (hit == null) {
                return true;
            }
            if (hit.typeOfHit == RayTraceResult.Type.BLOCK && center.equals(hit.getBlockPos())) {
                return true;
            }
        }
        return false;
    }

    private RenderTarget findNearestTarget(World world, EntityPlayer player, float partialTick) {
        long worldTime = world.getTotalWorldTime();
        if (cachedWorld == world && cachedWorldTime == worldTime) {
            return createRenderTarget(cachedNearestTarget, partialTick);
        }

        cachedWorld = world;
        cachedWorldTime = worldTime;
        cachedNearestTarget = null;
        RenderTarget nearest = null;
        double nearestDistanceSq = Double.MAX_VALUE;
        for (TileEntity tileEntity : world.loadedTileEntityList) {
            RenderTarget candidate = createRenderTarget(tileEntity, partialTick);
            if (candidate != null) {
                double distanceSq = player.getDistanceSq(tileEntity.getPos());
                if (distanceSq < nearestDistanceSq) {
                    nearestDistanceSq = distanceSq;
                    nearest = candidate;
                    cachedNearestTarget = tileEntity;
                }
            }
        }
        return nearest;
    }

    private static RenderTarget createRenderTarget(TileEntity tileEntity, float partialTick) {
        if (tileEntity == null || tileEntity.isInvalid()) {
            return null;
        }
        if (tileEntity instanceof TileEntityWirelessChargingEnergy energy) {
            float scale = RenderWirelessChargingEnergy.getAnimatedScale(energy, partialTick);
            return scale > 0 ? RenderTarget.forEnergy(energy, scale) : null;
        }
        if (tileEntity instanceof TileEntityWirelessChargingStation station) {
            float scale = RenderWirelessChargingStation.getAnimatedScale(station, partialTick);
            return scale > 0 ? RenderTarget.forStation(station, scale) : null;
        }
        return null;
    }

    private void clearCachedTarget() {
        cachedWorld = null;
        cachedWorldTime = Long.MIN_VALUE;
        cachedNearestTarget = null;
    }

    private static class RenderTarget {
        private final TileEntityWirelessChargingEnergy energy;
        private final TileEntityWirelessChargingStation station;
        private final float scale;

        private RenderTarget(TileEntityWirelessChargingEnergy energy, TileEntityWirelessChargingStation station, float scale) {
            this.energy = energy;
            this.station = station;
            this.scale = scale;
        }

        private static RenderTarget forEnergy(TileEntityWirelessChargingEnergy energy, float scale) {
            return new RenderTarget(energy, null, scale);
        }

        private static RenderTarget forStation(TileEntityWirelessChargingStation station, float scale) {
            return new RenderTarget(null, station, scale);
        }
    }
}
