package mekceumoremachine.common.config;

import mekanism.api.Coord4D;
import mekceumoremachine.common.attachments.component.ConnectionConfig;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.util.Constants.NBT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WirelessPreviewSnapshot {

    private final int revision;
    private final List<PreviewConnection> connections;

    public WirelessPreviewSnapshot(int revision, List<PreviewConnection> connections) {
        this.revision = revision;
        this.connections = Collections.unmodifiableList(new ArrayList<>(connections));
    }

    public static WirelessPreviewSnapshot create(int revision, List<ConnectionConfig> connections) {
        List<PreviewConnection> previewConnections = new ArrayList<>(connections.size());
        for (ConnectionConfig connection : connections) {
            previewConnections.add(new PreviewConnection(connection.getCoord(), connection.getFacing()));
        }
        return new WirelessPreviewSnapshot(revision, previewConnections);
    }

    public int getRevision() {
        return revision;
    }

    public List<PreviewConnection> getConnections() {
        return connections;
    }

    public NBTTagCompound write(NBTTagCompound tag) {
        tag.setInteger("revision", revision);
        NBTTagList list = new NBTTagList();
        for (PreviewConnection connection : connections) {
            NBTTagCompound connectionTag = new NBTTagCompound();
            connection.getCoord().write(connectionTag);
            connectionTag.setInteger("facing", connection.getFacing().getIndex());
            list.appendTag(connectionTag);
        }
        tag.setTag("connections", list);
        return tag;
    }

    public static WirelessPreviewSnapshot read(NBTTagCompound tag) {
        List<PreviewConnection> connections = new ArrayList<>();
        NBTTagList list = tag.getTagList("connections", NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound connectionTag = list.getCompoundTagAt(i);
            connections.add(new PreviewConnection(Coord4D.read(connectionTag), EnumFacing.byIndex(connectionTag.getInteger("facing"))));
        }
        return new WirelessPreviewSnapshot(Math.max(0, tag.getInteger("revision")), connections);
    }

    public static class PreviewConnection {

        private final Coord4D coord;
        private final EnumFacing facing;

        public PreviewConnection(Coord4D coord, EnumFacing facing) {
            this.coord = coord;
            this.facing = facing;
        }

        public Coord4D getCoord() {
            return coord;
        }

        public EnumFacing getFacing() {
            return facing;
        }
    }
}
