package mekceumoremachine.common.config;

import mekanism.common.Mekanism;
import mekanism.api.Coord4D;
import mekceumoremachine.common.attachments.component.ConnectionConfig;
import mekceumoremachine.common.tile.machine.TileEntityWirelessChargingEnergy;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class WirelessConnectionDataManager {

    private static final int DATA_VERSION = 0;
    private static final int FACING_COUNT = 6;
    private static final Map<String, Map<UUID, ConnectionIndex>> CONNECTION_INDEXES = new HashMap<>();

    private WirelessConnectionDataManager() {
    }

    public static LoadedData load(TileEntityWirelessChargingEnergy tile) {
        File file = resolveFile(tile);
        if (file == null || !file.isFile()) {
            return LoadedData.EMPTY;
        }
        try {
            NBTTagCompound root = CompressedStreamTools.read(file);
            if (root == null || !root.hasKey("version", NBT.TAG_INT) || root.getInteger("version") != DATA_VERSION) {
                Mekanism.logger.warn("Unsupported wireless connection data version in {}", file);
                return LoadedData.EMPTY;
            }
            Coord4D stationPos = Coord4D.get(tile);
            if (!root.hasKey("station", NBT.TAG_STRING) || !tile.getConnectionDataId().toString().equals(root.getString("station")) ||
                !root.hasKey("stationPos", NBT.TAG_COMPOUND) || !stationPos.equals(Coord4D.read(root.getCompoundTag("stationPos")))) {
                Mekanism.logger.warn("Wireless connection data {} belongs to another station; assigning a new data id", file);
                tile.regenerateConnectionDataId();
                return LoadedData.EMPTY;
            }
            List<ConnectionConfig> connections = readConnectionGroups(root.getTagList("groups", NBT.TAG_COMPOUND), stationPos.dimensionId);
            updateIndex(tile, connections);
            return new LoadedData(connections, Math.max(0, root.getInteger("revision")));
        } catch (IOException | RuntimeException e) {
            Mekanism.logger.error("Failed to load wireless connection data from {}", file, e);
            return LoadedData.EMPTY;
        }
    }

    public static void save(TileEntityWirelessChargingEnergy tile, List<ConnectionConfig> connections, int revision) {
        updateIndex(tile, connections);
        File file = resolveFile(tile);
        if (file == null) {
            return;
        }
        if (connections.isEmpty()) {
            if (file.isFile() && !file.delete()) {
                Mekanism.logger.warn("Failed to delete empty wireless connection data {}", file);
            }
            return;
        }
        File parent = file.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) {
            Mekanism.logger.warn("Failed to create wireless connection data directory {}", parent);
            return;
        }
        NBTTagCompound root = new NBTTagCompound();
        root.setInteger("version", DATA_VERSION);
        root.setInteger("revision", Math.max(0, revision));
        root.setString("station", tile.getConnectionDataId().toString());
        NBTTagCompound stationPos = new NBTTagCompound();
        Coord4D.get(tile).write(stationPos);
        root.setTag("stationPos", stationPos);
        root.setTag("groups", writeConnectionGroups(connections));
        try {
            CompressedStreamTools.safeWrite(root, file);
        } catch (IOException | RuntimeException e) {
            Mekanism.logger.error("Failed to save wireless connection data to {}", file, e);
        }
    }

    public static void delete(TileEntityWirelessChargingEnergy tile) {
        removeIndex(tile.getWorld(), tile.getConnectionDataId());
        File file = resolveFile(tile);
        if (file != null && file.isFile() && !file.delete()) {
            Mekanism.logger.warn("Failed to delete wireless connection data {}", file);
        }
    }

    public static boolean containsConnection(World world, UUID stationId, Coord4D station, BlockPos machinePos) {
        if (world == null || world.isRemote || stationId == null || station == null || machinePos == null) {
            return false;
        }
        String worldPath = getWorldPath(world);
        if (worldPath == null) {
            return false;
        }
        ConnectionIndex index;
        synchronized (CONNECTION_INDEXES) {
            Map<UUID, ConnectionIndex> worldIndexes = CONNECTION_INDEXES.computeIfAbsent(worldPath, ignored -> new HashMap<>());
            index = worldIndexes.get(stationId);
            if (index == null) {
                index = readIndex(world, stationId);
                worldIndexes.put(stationId, index);
            }
        }
        return station.equals(index.station) && index.machinePositions.contains(machinePos);
    }

    public static void clearAll() {
        synchronized (CONNECTION_INDEXES) {
            CONNECTION_INDEXES.clear();
        }
    }

    static NBTTagList writeConnectionGroups(List<ConnectionConfig> connections) {
        Map<String, ConnectionGroup> grouped = new LinkedHashMap<>();
        for (ConnectionConfig connection : connections) {
            if (connection.getMachineTypeKey().isEmpty()) {
                continue;
            }
            ConnectionGroup group = grouped.computeIfAbsent(connection.getMachineTypeKey(),
                  key -> new ConnectionGroup(key, connection.getMachineNameKey(), connection.getMachineStack()));
            if (group.machineNameKey.isEmpty() && !connection.getMachineNameKey().isEmpty()) {
                group.machineNameKey = connection.getMachineNameKey();
            }
            if (group.machineStack.isEmpty() && !connection.getMachineStack().isEmpty()) {
                group.machineStack = connection.getMachineStack();
            }
            group.targets.add(connection);
        }

        NBTTagList groups = new NBTTagList();
        for (ConnectionGroup group : grouped.values()) {
            NBTTagCompound groupTag = new NBTTagCompound();
            groupTag.setString("key", group.machineTypeKey);
            groupTag.setString("nameKey", group.machineNameKey);
            if (!group.machineStack.isEmpty()) {
                groupTag.setTag("machineStack", group.machineStack.writeToNBT(new NBTTagCompound()));
            }
            NBTTagList targets = new NBTTagList();
            for (ConnectionConfig connection : group.targets) {
                NBTTagCompound targetTag = new NBTTagCompound();
                targetTag.setInteger("x", connection.getCoord().x);
                targetTag.setInteger("y", connection.getCoord().y);
                targetTag.setInteger("z", connection.getCoord().z);
                targetTag.setInteger("machineX", connection.getMachineCoord().x);
                targetTag.setInteger("machineY", connection.getMachineCoord().y);
                targetTag.setInteger("machineZ", connection.getMachineCoord().z);
                targetTag.setInteger("facing", connection.getFacing().getIndex());
                targetTag.setBoolean("enabled", connection.isChargingEnabled());
                targets.appendTag(targetTag);
            }
            groupTag.setTag("targets", targets);
            groups.appendTag(groupTag);
        }
        return groups;
    }

    static List<ConnectionConfig> readConnectionGroups(NBTTagList groups, int dimensionId) {
        List<ConnectionConfig> connections = new ArrayList<>();
        Set<BlockPos> machinePositions = new HashSet<>();
        for (int groupIndex = 0; groupIndex < groups.tagCount(); groupIndex++) {
            NBTTagCompound groupTag = groups.getCompoundTagAt(groupIndex);
            String machineTypeKey = groupTag.getString("key");
            if (machineTypeKey.isEmpty()) {
                continue;
            }
            String machineNameKey = groupTag.getString("nameKey");
            ItemStack machineStack = groupTag.hasKey("machineStack", NBT.TAG_COMPOUND) ?
                  new ItemStack(groupTag.getCompoundTag("machineStack")) : ItemStack.EMPTY;
            NBTTagList targets = groupTag.getTagList("targets", NBT.TAG_COMPOUND);
            for (int targetIndex = 0; targetIndex < targets.tagCount(); targetIndex++) {
                NBTTagCompound targetTag = targets.getCompoundTagAt(targetIndex);
                BlockPos pos = new BlockPos(targetTag.getInteger("x"), targetTag.getInteger("y"), targetTag.getInteger("z"));
                BlockPos machinePos = new BlockPos(targetTag.getInteger("machineX"), targetTag.getInteger("machineY"),
                      targetTag.getInteger("machineZ"));
                if (!machinePositions.add(machinePos)) {
                    continue;
                }
                connections.add(new ConnectionConfig(new Coord4D(pos.getX(), pos.getY(), pos.getZ(), dimensionId),
                      new Coord4D(machinePos.getX(), machinePos.getY(), machinePos.getZ(), dimensionId),
                      getFacing(targetTag.getInteger("facing")), machineTypeKey, machineNameKey, machineStack,
                      targetTag.getBoolean("enabled")));
            }
        }
        return connections;
    }

    private static File resolveFile(TileEntityWirelessChargingEnergy tile) {
        return resolveFile(tile.getWorld(), tile.getConnectionDataId());
    }

    private static File resolveFile(World world, UUID stationId) {
        if (world == null || world.isRemote || stationId == null || world.getSaveHandler() == null) {
            return null;
        }
        File connectionDirectory = WirelessMachineTypeTableManager.getConnectionsDirectory(world);
        if (connectionDirectory == null) {
            return null;
        }
        return new File(connectionDirectory, stationId + ".dat");
    }

    private static ConnectionIndex readIndex(World world, UUID stationId) {
        File file = resolveFile(world, stationId);
        if (file == null || !file.isFile()) {
            return ConnectionIndex.EMPTY;
        }
        try {
            NBTTagCompound root = CompressedStreamTools.read(file);
            if (root == null || !root.hasKey("version", NBT.TAG_INT) || root.getInteger("version") != DATA_VERSION ||
                !stationId.toString().equals(root.getString("station")) || !root.hasKey("stationPos", NBT.TAG_COMPOUND)) {
                return ConnectionIndex.EMPTY;
            }
            Coord4D station = Coord4D.read(root.getCompoundTag("stationPos"));
            Set<BlockPos> machinePositions = new HashSet<>();
            NBTTagList groups = root.getTagList("groups", NBT.TAG_COMPOUND);
            for (int groupIndex = 0; groupIndex < groups.tagCount(); groupIndex++) {
                NBTTagList targets = groups.getCompoundTagAt(groupIndex).getTagList("targets", NBT.TAG_COMPOUND);
                for (int targetIndex = 0; targetIndex < targets.tagCount(); targetIndex++) {
                    NBTTagCompound target = targets.getCompoundTagAt(targetIndex);
                    if (target.hasKey("machineX", NBT.TAG_INT) && target.hasKey("machineY", NBT.TAG_INT) &&
                        target.hasKey("machineZ", NBT.TAG_INT)) {
                        machinePositions.add(new BlockPos(target.getInteger("machineX"), target.getInteger("machineY"),
                              target.getInteger("machineZ")));
                    }
                }
            }
            return new ConnectionIndex(station, machinePositions);
        } catch (IOException | RuntimeException e) {
            Mekanism.logger.error("Failed to read wireless connection index from {}", file, e);
            return ConnectionIndex.EMPTY;
        }
    }

    private static void updateIndex(TileEntityWirelessChargingEnergy tile, List<ConnectionConfig> connections) {
        String worldPath = getWorldPath(tile.getWorld());
        if (worldPath == null) {
            return;
        }
        Set<BlockPos> machinePositions = new HashSet<>();
        for (ConnectionConfig connection : connections) {
            machinePositions.add(connection.getMachinePos());
        }
        synchronized (CONNECTION_INDEXES) {
            CONNECTION_INDEXES.computeIfAbsent(worldPath, ignored -> new HashMap<>()).put(tile.getConnectionDataId(),
                  new ConnectionIndex(Coord4D.get(tile), machinePositions));
        }
    }

    private static void removeIndex(World world, UUID stationId) {
        String worldPath = getWorldPath(world);
        if (worldPath == null) {
            return;
        }
        synchronized (CONNECTION_INDEXES) {
            Map<UUID, ConnectionIndex> worldIndexes = CONNECTION_INDEXES.get(worldPath);
            if (worldIndexes != null) {
                worldIndexes.remove(stationId);
                if (worldIndexes.isEmpty()) {
                    CONNECTION_INDEXES.remove(worldPath);
                }
            }
        }
    }

    private static String getWorldPath(World world) {
        return world == null || world.isRemote || world.getSaveHandler() == null ? null :
              world.getSaveHandler().getWorldDirectory().getAbsolutePath();
    }

    private static EnumFacing getFacing(int index) {
        return index >= 0 && index < FACING_COUNT ? EnumFacing.byIndex(index) : EnumFacing.NORTH;
    }

    private static final class ConnectionIndex {

        private static final ConnectionIndex EMPTY = new ConnectionIndex(null, Collections.emptySet());

        private final Coord4D station;
        private final Set<BlockPos> machinePositions;

        private ConnectionIndex(Coord4D station, Set<BlockPos> machinePositions) {
            this.station = station;
            this.machinePositions = machinePositions;
        }
    }

    private static class ConnectionGroup {

        private final String machineTypeKey;
        private String machineNameKey;
        private ItemStack machineStack;
        private final List<ConnectionConfig> targets = new ArrayList<>();

        private ConnectionGroup(String machineTypeKey, String machineNameKey, ItemStack machineStack) {
            this.machineTypeKey = machineTypeKey;
            this.machineNameKey = machineNameKey;
            this.machineStack = machineStack;
        }
    }

    public static class LoadedData {

        private static final LoadedData EMPTY = new LoadedData(new ArrayList<>(), 0);

        private final List<ConnectionConfig> connections;
        private final int revision;

        private LoadedData(List<ConnectionConfig> connections, int revision) {
            this.connections = connections;
            this.revision = revision;
        }

        public List<ConnectionConfig> getConnections() {
            return new ArrayList<>(connections);
        }

        public int getRevision() {
            return revision;
        }
    }
}
