package mekceumoremachine.common.config;

import mekceumoremachine.common.attachments.component.ConnectionConfig;
import mekceumoremachine.common.tile.machine.TileEntityWirelessChargingEnergy;
import net.minecraft.nbt.NBTTagCompound;

import java.util.List;

public final class WirelessConnectionStatusSnapshot {

    private final int revision;
    private final byte[] loadedStates;

    public WirelessConnectionStatusSnapshot(int revision, byte[] loadedStates) {
        this.revision = Math.max(0, revision);
        this.loadedStates = loadedStates.clone();
    }

    public static WirelessConnectionStatusSnapshot create(TileEntityWirelessChargingEnergy tile) {
        List<ConnectionConfig> connections = tile.getConnectionsSnapshot();
        byte[] loadedStates = new byte[connections.size()];
        for (int i = 0; i < connections.size(); i++) {
            loadedStates[i] = tile.isConnectionLoaded(connections.get(i)) ? (byte) 1 : 0;
        }
        return new WirelessConnectionStatusSnapshot(tile.getConnectionRevision(), loadedStates);
    }

    public int getRevision() {
        return revision;
    }

    public int size() {
        return loadedStates.length;
    }

    public boolean isLoaded(int index) {
        return loadedStates[index] != 0;
    }

    public NBTTagCompound write(NBTTagCompound tag) {
        tag.setInteger("revision", revision);
        tag.setByteArray("loaded", loadedStates);
        return tag;
    }

    public static WirelessConnectionStatusSnapshot read(NBTTagCompound tag) {
        return new WirelessConnectionStatusSnapshot(tag.getInteger("revision"), tag.getByteArray("loaded"));
    }
}
