package mekceumoremachine.common.config;

import mekanism.api.Coord4D;
import mekceumoremachine.common.attachments.component.ConnectionConfig;
import mekceumoremachine.common.tile.machine.TileEntityWirelessChargingEnergy;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.util.Constants.NBT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WirelessConnectionSnapshot {

    public static final WirelessConnectionSnapshot EMPTY = new WirelessConnectionSnapshot(0, false, Collections.emptyList());

    private final int revision;
    private final boolean editable;
    private final List<MachineGroup> machineGroups;

    public WirelessConnectionSnapshot(int revision, boolean editable, List<MachineGroup> machineGroups) {
        this.revision = revision;
        this.editable = editable;
        this.machineGroups = Collections.unmodifiableList(new ArrayList<>(machineGroups));
    }

    public static WirelessConnectionSnapshot create(TileEntityWirelessChargingEnergy tile, boolean editable) {
        Map<String, GroupBuilder> grouped = new LinkedHashMap<>();
        for (ConnectionConfig connection : tile.getConnectionsSnapshot()) {
            GroupBuilder builder = grouped.computeIfAbsent(connection.getMachineTypeKey(),
                  key -> new GroupBuilder(key, connection.getMachineNameKey(), connection.getMachineStack()));
            if (builder.machineStack.isEmpty() && !connection.getMachineStack().isEmpty()) {
                builder.machineStack = connection.getMachineStack();
            }
            builder.targets.add(new Target(connection.getMachineCoord(), connection.getCoord(), connection.getFacing(),
                  connection.isChargingEnabled(), tile.isConnectionLoaded(connection)));
        }
        List<MachineGroup> groups = new ArrayList<>();
        for (GroupBuilder builder : grouped.values()) {
            groups.add(new MachineGroup(builder.key, builder.nameKey, builder.machineStack, builder.targets));
        }
        return new WirelessConnectionSnapshot(tile.getConnectionRevision(), editable, groups);
    }

    public int getRevision() {
        return revision;
    }

    public boolean isEditable() {
        return editable;
    }

    public List<MachineGroup> getMachineGroups() {
        return machineGroups;
    }

    public boolean isEmpty() {
        return machineGroups.isEmpty();
    }

    public WirelessConnectionSnapshot withLoadedStates(WirelessConnectionStatusSnapshot status) {
        if (status.getRevision() != revision || status.size() != getTargetCount()) {
            return this;
        }
        int statusIndex = 0;
        boolean changed = false;
        for (MachineGroup group : machineGroups) {
            for (Target target : group.getTargets()) {
                if (target.isLoaded() != status.isLoaded(statusIndex++)) {
                    changed = true;
                }
            }
        }
        if (!changed) {
            return this;
        }
        statusIndex = 0;
        List<MachineGroup> updatedGroups = new ArrayList<>(machineGroups.size());
        for (MachineGroup group : machineGroups) {
            List<Target> updatedTargets = new ArrayList<>(group.getTargets().size());
            for (Target target : group.getTargets()) {
                updatedTargets.add(new Target(target.getCoord(), target.getEndpoint(), target.getFacing(), target.isEnabled(),
                      status.isLoaded(statusIndex++)));
            }
            updatedGroups.add(new MachineGroup(group.getKey(), group.getNameKey(), group.getMachineStack(), updatedTargets));
        }
        return new WirelessConnectionSnapshot(revision, editable, updatedGroups);
    }

    private int getTargetCount() {
        int count = 0;
        for (MachineGroup group : machineGroups) {
            count += group.getTargets().size();
        }
        return count;
    }

    public NBTTagCompound write(NBTTagCompound tag) {
        tag.setInteger("revision", revision);
        tag.setBoolean("editable", editable);
        NBTTagList groups = new NBTTagList();
        for (MachineGroup group : machineGroups) {
            NBTTagCompound groupTag = new NBTTagCompound();
            groupTag.setString("key", group.getKey());
            groupTag.setString("nameKey", group.getNameKey());
            if (!group.getMachineStack().isEmpty()) {
                groupTag.setTag("machineStack", group.getMachineStack().writeToNBT(new NBTTagCompound()));
            }
            NBTTagList targets = new NBTTagList();
            for (Target target : group.getTargets()) {
                NBTTagCompound targetTag = new NBTTagCompound();
                target.getCoord().write(targetTag);
                targetTag.setTag("endpoint", target.getEndpoint().write(new NBTTagCompound()));
                targetTag.setInteger("facing", target.getFacing().getIndex());
                targetTag.setBoolean("enabled", target.isEnabled());
                targetTag.setBoolean("loaded", target.isLoaded());
                targets.appendTag(targetTag);
            }
            groupTag.setTag("targets", targets);
            groups.appendTag(groupTag);
        }
        tag.setTag("groups", groups);
        return tag;
    }

    public static WirelessConnectionSnapshot read(NBTTagCompound tag) {
        List<MachineGroup> groups = new ArrayList<>();
        NBTTagList groupList = tag.getTagList("groups", NBT.TAG_COMPOUND);
        for (int i = 0; i < groupList.tagCount(); i++) {
            NBTTagCompound groupTag = groupList.getCompoundTagAt(i);
            String key = groupTag.getString("key");
            if (key.isEmpty()) {
                continue;
            }
            List<Target> targets = new ArrayList<>();
            NBTTagList targetList = groupTag.getTagList("targets", NBT.TAG_COMPOUND);
            for (int targetIndex = 0; targetIndex < targetList.tagCount(); targetIndex++) {
                NBTTagCompound targetTag = targetList.getCompoundTagAt(targetIndex);
                Coord4D coord = Coord4D.read(targetTag);
                Coord4D endpoint = targetTag.hasKey("endpoint", NBT.TAG_COMPOUND) ? Coord4D.read(targetTag.getCompoundTag("endpoint")) : coord;
                targets.add(new Target(coord, endpoint, EnumFacing.byIndex(targetTag.getInteger("facing")), targetTag.getBoolean("enabled"),
                      targetTag.getBoolean("loaded")));
            }
            if (!targets.isEmpty()) {
                ItemStack machineStack = groupTag.hasKey("machineStack", NBT.TAG_COMPOUND) ?
                      new ItemStack(groupTag.getCompoundTag("machineStack")) : ItemStack.EMPTY;
                groups.add(new MachineGroup(key, groupTag.getString("nameKey"), machineStack, targets));
            }
        }
        return groups.isEmpty() && !tag.getBoolean("editable") ? EMPTY :
              new WirelessConnectionSnapshot(Math.max(0, tag.getInteger("revision")), tag.getBoolean("editable"), groups);
    }

    private static class GroupBuilder {

        private final String key;
        private final String nameKey;
        private ItemStack machineStack;
        private final List<Target> targets = new ArrayList<>();

        private GroupBuilder(String key, String nameKey, ItemStack machineStack) {
            this.key = key;
            this.nameKey = nameKey;
            this.machineStack = normalizeMachineStack(machineStack);
        }
    }

    public static class MachineGroup {

        private final String key;
        private final String nameKey;
        private final ItemStack machineStack;
        private final List<Target> targets;

        public MachineGroup(String key, String nameKey, List<Target> targets) {
            this(key, nameKey, ItemStack.EMPTY, targets);
        }

        public MachineGroup(String key, String nameKey, ItemStack machineStack, List<Target> targets) {
            this.key = key;
            this.nameKey = nameKey;
            this.machineStack = normalizeMachineStack(machineStack);
            this.targets = Collections.unmodifiableList(new ArrayList<>(targets));
        }

        public String getKey() {
            return key;
        }

        public String getNameKey() {
            return nameKey;
        }

        public ItemStack getMachineStack() {
            return machineStack;
        }

        public List<Target> getTargets() {
            return targets;
        }

        public int getEnabledCount() {
            int enabled = 0;
            for (Target target : targets) {
                if (target.isEnabled()) {
                    enabled++;
                }
            }
            return enabled;
        }

        public int getLoadedCount() {
            int loaded = 0;
            for (Target target : targets) {
                if (target.isLoaded()) {
                    loaded++;
                }
            }
            return loaded;
        }
    }

    public static class Target {

        private final Coord4D coord;
        private final Coord4D endpoint;
        private final EnumFacing facing;
        private final boolean enabled;
        private final boolean loaded;

        public Target(Coord4D coord, Coord4D endpoint, EnumFacing facing, boolean enabled, boolean loaded) {
            this.coord = coord;
            this.endpoint = endpoint;
            this.facing = facing;
            this.enabled = enabled;
            this.loaded = loaded;
        }

        public Coord4D getCoord() {
            return coord;
        }

        public Coord4D getEndpoint() {
            return endpoint;
        }

        public EnumFacing getFacing() {
            return facing;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public boolean isLoaded() {
            return loaded;
        }

        public String getCoordinateText() {
            return coord.x + ", " + coord.y + ", " + coord.z;
        }
    }

    private static ItemStack normalizeMachineStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack normalized = stack.copy();
        normalized.setCount(1);
        return normalized;
    }
}
