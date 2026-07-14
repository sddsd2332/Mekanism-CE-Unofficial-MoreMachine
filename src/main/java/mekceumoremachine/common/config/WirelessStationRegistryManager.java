package mekceumoremachine.common.config;

import mekanism.api.Coord4D;
import mekanism.common.Mekanism;
import mekceumoremachine.common.tile.machine.TileEntityWirelessChargingEnergy;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class WirelessStationRegistryManager {

    private static final int DATA_VERSION = 1;
    private static final String FILE_NAME = "stations.dat";
    private static final Map<String, StationRegistry> REGISTRIES = new HashMap<>();

    private WirelessStationRegistryManager() {
    }

    public static void register(TileEntityWirelessChargingEnergy tile) {
        if (tile.getWorld() == null || tile.getWorld().isRemote) {
            return;
        }
        StationRegistry registry = getRegistry(tile.getWorld());
        if (registry != null) {
            registry.register(Coord4D.get(tile), tile.getConnectionDataId());
        }
    }

    public static void unregister(TileEntityWirelessChargingEnergy tile) {
        if (tile.getWorld() == null || tile.getWorld().isRemote) {
            return;
        }
        StationRegistry registry = getRegistry(tile.getWorld());
        if (registry != null) {
            registry.unregister(Coord4D.get(tile), tile.getConnectionDataId());
        }
    }

    public static void unregister(World world, Coord4D station) {
        StationRegistry registry = getRegistry(world);
        if (registry != null) {
            registry.unregister(station);
        }
    }

    public static UUID getStationId(World world, Coord4D station) {
        StationRegistry registry = getRegistry(world);
        return registry == null ? null : registry.get(station);
    }

    public static void clearAll() {
        synchronized (REGISTRIES) {
            REGISTRIES.clear();
        }
    }

    private static StationRegistry getRegistry(World world) {
        if (world == null || world.isRemote || world.getSaveHandler() == null) {
            return null;
        }
        File worldDirectory = world.getSaveHandler().getWorldDirectory();
        String worldPath = worldDirectory.getAbsolutePath();
        synchronized (REGISTRIES) {
            StationRegistry registry = REGISTRIES.get(worldPath);
            if (registry == null) {
                File directory = WirelessMachineTypeTableManager.getConnectionsDirectory(world);
                if (directory == null) {
                    return null;
                }
                registry = new StationRegistry(new File(directory, FILE_NAME));
                registry.load();
                REGISTRIES.put(worldPath, registry);
            }
            return registry;
        }
    }

    private static final class StationRegistry {

        private final File file;
        private final Map<StationKey, UUID> stations = new LinkedHashMap<>();

        private StationRegistry(File file) {
            this.file = file;
        }

        private synchronized void load() {
            stations.clear();
            if (!file.isFile()) {
                return;
            }
            try {
                NBTTagCompound root = CompressedStreamTools.read(file);
                if (root == null || root.getInteger("version") != DATA_VERSION) {
                    Mekanism.logger.warn("Unsupported wireless station registry version in {}", file);
                    return;
                }
                NBTTagList entries = root.getTagList("stations", NBT.TAG_COMPOUND);
                for (int i = 0; i < entries.tagCount(); i++) {
                    NBTTagCompound entry = entries.getCompoundTagAt(i);
                    String id = entry.getString("id");
                    if (id.isEmpty()) {
                        continue;
                    }
                    try {
                        stations.put(StationKey.read(entry), UUID.fromString(id));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            } catch (IOException | RuntimeException e) {
                stations.clear();
                Mekanism.logger.error("Failed to load wireless station registry from {}", file, e);
            }
        }

        private synchronized void register(Coord4D coord, UUID id) {
            StationKey key = new StationKey(coord);
            if (id.equals(stations.get(key))) {
                return;
            }
            stations.put(key, id);
            save();
        }

        private synchronized void unregister(Coord4D coord, UUID id) {
            StationKey key = new StationKey(coord);
            if (id.equals(stations.get(key))) {
                stations.remove(key);
                save();
            }
        }

        private synchronized void unregister(Coord4D coord) {
            if (stations.remove(new StationKey(coord)) != null) {
                save();
            }
        }

        private synchronized UUID get(Coord4D coord) {
            return stations.get(new StationKey(coord));
        }

        private void save() {
            if (stations.isEmpty()) {
                if (file.isFile() && !file.delete()) {
                    Mekanism.logger.warn("Failed to delete empty wireless station registry {}", file);
                }
                return;
            }
            File parent = file.getParentFile();
            if (!parent.isDirectory() && !parent.mkdirs()) {
                Mekanism.logger.warn("Failed to create wireless station registry directory {}", parent);
                return;
            }
            NBTTagCompound root = new NBTTagCompound();
            root.setInteger("version", DATA_VERSION);
            NBTTagList entries = new NBTTagList();
            for (Map.Entry<StationKey, UUID> station : stations.entrySet()) {
                NBTTagCompound entry = station.getKey().write(new NBTTagCompound());
                entry.setString("id", station.getValue().toString());
                entries.appendTag(entry);
            }
            root.setTag("stations", entries);
            try {
                CompressedStreamTools.safeWrite(root, file);
            } catch (IOException | RuntimeException e) {
                Mekanism.logger.error("Failed to save wireless station registry to {}", file, e);
            }
        }
    }

    private static final class StationKey {

        private final int dimension;
        private final int x;
        private final int y;
        private final int z;

        private StationKey(Coord4D coord) {
            this(coord.dimensionId, coord.x, coord.y, coord.z);
        }

        private StationKey(int dimension, int x, int y, int z) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private static StationKey read(NBTTagCompound tag) {
            return new StationKey(tag.getInteger("dimension"), tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z"));
        }

        private NBTTagCompound write(NBTTagCompound tag) {
            tag.setInteger("dimension", dimension);
            tag.setInteger("x", x);
            tag.setInteger("y", y);
            tag.setInteger("z", z);
            return tag;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof StationKey other)) {
                return false;
            }
            return dimension == other.dimension && x == other.x && y == other.y && z == other.z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(dimension, x, y, z);
        }
    }
}
