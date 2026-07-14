package mekceumoremachine.common.network;

import io.netty.buffer.ByteBuf;
import mekanism.api.Coord4D;
import mekanism.common.PacketHandler;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.SecurityUtils;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.config.WirelessConnectionClientCache;
import mekceumoremachine.common.config.WirelessConnectionSnapshot;
import mekceumoremachine.common.config.WirelessConnectionStatusSnapshot;
import mekceumoremachine.common.config.WirelessPreviewSnapshot;
import mekceumoremachine.common.item.ItemConnector;
import mekceumoremachine.common.network.PacketWirelessConnection.WirelessConnectionMessage;
import mekceumoremachine.common.tile.machine.TileEntityWirelessChargingEnergy;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.HashSet;
import java.util.Set;

public class PacketWirelessConnection implements IMessageHandler<WirelessConnectionMessage, IMessage> {

    @Override
    public IMessage onMessage(WirelessConnectionMessage message, MessageContext context) {
        EntityPlayer player = PacketHandler.getPlayer(context);
        if (player == null) {
            return null;
        }
        PacketHandler.handlePacket(() -> {
            if (player.world.isRemote) {
                handleClient(message);
            } else {
                handleServer(message, player);
            }
        }, player);
        return null;
    }

    private void handleServer(WirelessConnectionMessage message, EntityPlayer player) {
        if (message.station == null || message.station.dimensionId != player.world.provider.getDimension()) {
            return;
        }
        if (message.packetType == ConnectionPacket.CONFIG_SNAPSHOT || message.packetType == ConnectionPacket.PREVIEW_SNAPSHOT ||
            message.packetType == ConnectionPacket.STATUS_SNAPSHOT ||
            !player.world.isBlockLoaded(message.station.getPos())) {
            return;
        }
        TileEntity targetTile = message.station.getTileEntity(player.world);
        if (!(targetTile instanceof TileEntityWirelessChargingEnergy tile)) {
            return;
        }
        boolean previewRequest = message.packetType == ConnectionPacket.REQUEST_PREVIEW;
        if (previewRequest ? !canRequestPreview(player, tile, message.station) : !PacketHandler.canAccessTile(player, tile, true)) {
            return;
        }
        EntityPlayerMP playerMP = player instanceof EntityPlayerMP ? (EntityPlayerMP) player : null;
        if (previewRequest) {
            sendPreviewSnapshot(tile, playerMP);
            return;
        }
        if (message.packetType == ConnectionPacket.REQUEST_CONFIG) {
            sendConfigSnapshot(tile, playerMP);
            return;
        }
        if (message.packetType == ConnectionPacket.REQUEST_STATUS) {
            sendStatusSnapshot(tile, playerMP);
            return;
        }
        boolean changed = switch (message.packetType) {
            case SET_ALL_ENABLED -> tile.setAllConnectionsEnabled(message.enabled);
            case SET_TYPE_ENABLED -> !message.machineTypeKey.isEmpty() && tile.setMachineTypeEnabled(message.machineTypeKey, message.enabled);
            case SET_TARGET_ENABLED -> message.target != null && tile.setConnectionEnabled(message.target, message.enabled);
            case MOVE_TYPE -> !message.machineTypeKey.isEmpty() && tile.moveMachineType(message.machineTypeKey, message.direction, false);
            case MOVE_TARGET -> message.target != null && !message.machineTypeKey.isEmpty() &&
                                tile.moveConnection(message.machineTypeKey, message.target, message.direction, false);
            case MOVE_TYPE_TO_EDGE -> !message.machineTypeKey.isEmpty() && tile.moveMachineType(message.machineTypeKey, message.direction, true);
            case MOVE_TARGET_TO_EDGE -> message.target != null && !message.machineTypeKey.isEmpty() &&
                                        tile.moveConnection(message.machineTypeKey, message.target, message.direction, true);
            case SET_DYNAMIC_CHARGING -> tile.setDynamicWirelessCharging(message.enabled);
            case DELETE_TYPE -> !message.machineTypeKey.isEmpty() && tile.removeMachineType(message.machineTypeKey);
            case DELETE_TARGET -> message.target != null && !message.machineTypeKey.isEmpty() &&
                                  tile.removeConnection(message.machineTypeKey, message.target);
            case DELETE_ALL -> tile.removeAllConnections();
            default -> false;
        };
        if (!changed) {
            sendConfigSnapshot(tile, playerMP);
        }
    }

    private boolean canRequestPreview(EntityPlayer player, TileEntityWirelessChargingEnergy tile, Coord4D station) {
        return !tile.isInvalid() && tile.getWorld() == player.world && SecurityUtils.canAccess(player, tile) &&
               (isConnectorBoundTo(player.getHeldItemMainhand(), station) || isConnectorBoundTo(player.getHeldItemOffhand(), station));
    }

    private boolean isConnectorBoundTo(ItemStack stack, Coord4D station) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemConnector connector)) {
            return false;
        }
        return station.equals(connector.getDataType(stack));
    }

    private void handleClient(WirelessConnectionMessage message) {
        if (message.station == null) {
            return;
        }
        if (message.packetType == ConnectionPacket.CONFIG_SNAPSHOT) {
            WirelessConnectionClientCache.setConfigSnapshot(message.station, WirelessConnectionSnapshot.read(message.payload));
        } else if (message.packetType == ConnectionPacket.STATUS_SNAPSHOT) {
            WirelessConnectionClientCache.applyStatusSnapshot(message.station, WirelessConnectionStatusSnapshot.read(message.payload));
        } else if (message.packetType == ConnectionPacket.PREVIEW_SNAPSHOT) {
            WirelessConnectionClientCache.setPreviewSnapshot(message.station, WirelessPreviewSnapshot.read(message.payload));
        }
    }

    public static void syncOpenConfigWindows(TileEntityWirelessChargingEnergy tile) {
        if (tile.getWorld() == null || tile.getWorld().isRemote || tile.playersUsing.isEmpty()) {
            return;
        }
        Set<EntityPlayerMP> viewers = new HashSet<>();
        for (EntityPlayer player : tile.playersUsing) {
            if (player instanceof EntityPlayerMP playerMP && PacketHandler.canAccessTile(player, tile, true) && viewers.add(playerMP)) {
                sendConfigSnapshot(tile, playerMP);
            }
        }
    }

    private static void sendConfigSnapshot(TileEntityWirelessChargingEnergy tile, EntityPlayerMP player) {
        if (player == null) {
            return;
        }
        WirelessConnectionSnapshot snapshot = WirelessConnectionSnapshot.create(tile, true);
        MEKCeuMoreMachine.packetHandler.sendTo(WirelessConnectionMessage.configSnapshot(Coord4D.get(tile), snapshot), player);
    }

    private static void sendPreviewSnapshot(TileEntityWirelessChargingEnergy tile, EntityPlayerMP player) {
        if (player == null) {
            return;
        }
        WirelessPreviewSnapshot snapshot = WirelessPreviewSnapshot.create(tile.getConnectionRevision(), tile.getConnectionsSnapshot());
        MEKCeuMoreMachine.packetHandler.sendTo(WirelessConnectionMessage.previewSnapshot(Coord4D.get(tile), snapshot), player);
    }

    private static void sendStatusSnapshot(TileEntityWirelessChargingEnergy tile, EntityPlayerMP player) {
        if (player == null) {
            return;
        }
        WirelessConnectionStatusSnapshot snapshot = WirelessConnectionStatusSnapshot.create(tile);
        MEKCeuMoreMachine.packetHandler.sendTo(WirelessConnectionMessage.statusSnapshot(Coord4D.get(tile), snapshot), player);
    }

    public enum ConnectionPacket {
        REQUEST_CONFIG,
        CONFIG_SNAPSHOT,
        SET_ALL_ENABLED,
        SET_TYPE_ENABLED,
        SET_TARGET_ENABLED,
        MOVE_TYPE,
        MOVE_TARGET,
        MOVE_TYPE_TO_EDGE,
        MOVE_TARGET_TO_EDGE,
        SET_DYNAMIC_CHARGING,
        REQUEST_PREVIEW,
        PREVIEW_SNAPSHOT,
        REQUEST_STATUS,
        STATUS_SNAPSHOT,
        DELETE_TYPE,
        DELETE_TARGET,
        DELETE_ALL
    }

    public static class WirelessConnectionMessage implements IMessage {

        private ConnectionPacket packetType = ConnectionPacket.REQUEST_CONFIG;
        private Coord4D station;
        private String machineTypeKey = "";
        private Coord4D target;
        private int direction;
        private boolean enabled;
        private NBTTagCompound payload = new NBTTagCompound();

        public WirelessConnectionMessage() {
        }

        public WirelessConnectionMessage(Coord4D station, ConnectionPacket packetType) {
            this.station = station;
            this.packetType = packetType;
        }

        public WirelessConnectionMessage(Coord4D station, ConnectionPacket packetType, String machineTypeKey, Coord4D target, int direction,
              boolean enabled) {
            this.station = station;
            this.packetType = packetType;
            this.machineTypeKey = machineTypeKey == null ? "" : machineTypeKey;
            this.target = target;
            this.direction = direction;
            this.enabled = enabled;
        }

        public static WirelessConnectionMessage configSnapshot(Coord4D station, WirelessConnectionSnapshot snapshot) {
            WirelessConnectionMessage message = new WirelessConnectionMessage(station, ConnectionPacket.CONFIG_SNAPSHOT);
            message.payload = snapshot.write(new NBTTagCompound());
            return message;
        }

        public static WirelessConnectionMessage previewSnapshot(Coord4D station, WirelessPreviewSnapshot snapshot) {
            WirelessConnectionMessage message = new WirelessConnectionMessage(station, ConnectionPacket.PREVIEW_SNAPSHOT);
            message.payload = snapshot.write(new NBTTagCompound());
            return message;
        }

        public static WirelessConnectionMessage statusSnapshot(Coord4D station, WirelessConnectionStatusSnapshot snapshot) {
            WirelessConnectionMessage message = new WirelessConnectionMessage(station, ConnectionPacket.STATUS_SNAPSHOT);
            message.payload = snapshot.write(new NBTTagCompound());
            return message;
        }

        @Override
        public void toBytes(ByteBuf dataStream) {
            dataStream.writeInt(packetType.ordinal());
            station.write(dataStream);
            PacketHandler.writeString(dataStream, machineTypeKey);
            dataStream.writeBoolean(target != null);
            if (target != null) {
                target.write(dataStream);
            }
            dataStream.writeInt(direction);
            dataStream.writeBoolean(enabled);
            PacketHandler.writeNBT(dataStream, payload);
        }

        @Override
        public void fromBytes(ByteBuf dataStream) {
            packetType = MekanismUtils.getByIndex(ConnectionPacket.values(), dataStream.readInt(), ConnectionPacket.REQUEST_CONFIG);
            station = Coord4D.read(dataStream);
            machineTypeKey = PacketHandler.readString(dataStream);
            target = dataStream.readBoolean() ? Coord4D.read(dataStream) : null;
            direction = dataStream.readInt();
            enabled = dataStream.readBoolean();
            payload = PacketHandler.readNBT(dataStream);
            if (payload == null) {
                payload = new NBTTagCompound();
            }
        }
    }
}
