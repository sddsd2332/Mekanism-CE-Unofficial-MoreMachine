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

import java.util.ArrayList;

@SideOnly(Side.CLIENT)
public class WirelessChargingRangeWorldRenderHandler {

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.player;
        World world = minecraft.world;
        if (player == null || world == null || world.loadedTileEntityList.isEmpty()) {
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

    private static RenderTarget findNearestTarget(World world, EntityPlayer player, float partialTick) {
        RenderTarget nearest = null;
        double nearestDistanceSq = Double.MAX_VALUE;
        for (TileEntity tileEntity : new ArrayList<>(world.loadedTileEntityList)) {
            if (tileEntity == null || tileEntity.isInvalid()) {
                continue;
            }
            if (tileEntity instanceof TileEntityWirelessChargingEnergy energy) {
                float scale = RenderWirelessChargingEnergy.getAnimatedScale(energy, partialTick);
                if (scale <= 0) {
                    continue;
                }
                double distanceSq = player.getDistanceSq(energy.getPos());
                if (distanceSq < nearestDistanceSq) {
                    nearestDistanceSq = distanceSq;
                    nearest = RenderTarget.forEnergy(energy, scale);
                }
            } else if (tileEntity instanceof TileEntityWirelessChargingStation station) {
                float scale = RenderWirelessChargingStation.getAnimatedScale(station, partialTick);
                if (scale <= 0) {
                    continue;
                }
                double distanceSq = player.getDistanceSq(station.getPos());
                if (distanceSq < nearestDistanceSq) {
                    nearestDistanceSq = distanceSq;
                    nearest = RenderTarget.forStation(station, scale);
                }
            }
        }
        return nearest;
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
