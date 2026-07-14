package mekceumoremachine.common.config;

import mekanism.api.Coord4D;
import mekceumoremachine.common.attachments.component.ConnectionConfig;
import mekceumoremachine.common.config.WirelessConnectionSnapshot.MachineGroup;
import mekceumoremachine.common.config.WirelessConnectionSnapshot.Target;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WirelessConnectionDataTest {

    @Test
    void externalDataRoundTripPreservesGroupedOrderAndTargetOrder() {
        Coord4D firstCoord = new Coord4D(12, 64, -9, 7);
        Coord4D secondCoord = new Coord4D(13, 65, -8, 7);
        Coord4D thirdCoord = new Coord4D(14, 66, -7, 7);
        ConnectionConfig first = new ConnectionConfig(firstCoord, firstCoord, EnumFacing.WEST,
              "type-a", "tile.example.Advanced", false);
        ConnectionConfig second = new ConnectionConfig(secondCoord, secondCoord, EnumFacing.EAST,
              "type-a", "tile.example.Advanced", true);
        ConnectionConfig third = new ConnectionConfig(thirdCoord, thirdCoord, EnumFacing.UP,
              "type-b", "tile.example.Ultimate", true);

        NBTTagList groups = WirelessConnectionDataManager.writeConnectionGroups(Arrays.asList(first, second, third));

        assertEquals(2, groups.tagCount());
        assertEquals("type-a", groups.getCompoundTagAt(0).getString("key"));
        assertEquals("tile.example.Advanced", groups.getCompoundTagAt(0).getString("nameKey"));
        assertEquals(2, groups.getCompoundTagAt(0).getTagList("targets", 10).tagCount());
        NBTTagCompound firstTarget = groups.getCompoundTagAt(0).getTagList("targets", 10).getCompoundTagAt(0);
        assertFalse(firstTarget.hasKey("dimensionId"));
        assertFalse(firstTarget.hasKey("machineType"));
        assertFalse(firstTarget.hasKey("nameKey"));

        List<ConnectionConfig> restored = WirelessConnectionDataManager.readConnectionGroups(groups, 7);

        assertEquals(3, restored.size());
        assertEquals(first.getCoord(), restored.get(0).getCoord());
        assertEquals(first.getMachineCoord(), restored.get(0).getMachineCoord());
        assertEquals(second.getCoord(), restored.get(1).getCoord());
        assertEquals(third.getCoord(), restored.get(2).getCoord());
        assertEquals(EnumFacing.WEST, restored.get(0).getFacing());
        assertEquals("tile.example.Advanced", restored.get(0).getMachineNameKey());
        assertFalse(restored.get(0).isChargingEnabled());
        assertTrue(restored.get(1).isChargingEnabled());
    }

    @Test
    void externalDataLoadDeduplicatesLogicalMachinesAcrossGroups() {
        Coord4D machine = new Coord4D(1, 2, 3, 4);
        NBTTagList groups = WirelessConnectionDataManager.writeConnectionGroups(Arrays.asList(
              new ConnectionConfig(new Coord4D(1, 3, 3, 4), machine, EnumFacing.NORTH, "first", "tile.first", true),
              new ConnectionConfig(new Coord4D(1, 4, 3, 4), machine, EnumFacing.SOUTH, "second", "tile.second", false)));

        List<ConnectionConfig> restored = WirelessConnectionDataManager.readConnectionGroups(groups, 4);

        assertEquals(1, restored.size());
        assertEquals("first", restored.get(0).getMachineTypeKey());
        assertEquals(machine, restored.get(0).getMachineCoord());
    }

    @Test
    void snapshotRoundTripPreservesGroupAndTargetOrder() {
        Coord4D firstCoord = new Coord4D(1, 2, 3, 0);
        Coord4D firstEndpoint = new Coord4D(1, 3, 3, 0);
        Coord4D secondCoord = new Coord4D(4, 5, 6, 0);
        Target first = new Target(firstCoord, firstEndpoint, EnumFacing.NORTH, true, true);
        Target second = new Target(secondCoord, secondCoord, EnumFacing.SOUTH, false, false);
        MachineGroup typeA = new MachineGroup("type-a", "tile.TypeA", Arrays.asList(first, second));
        MachineGroup typeB = new MachineGroup("type-b", "tile.TypeB", Collections.singletonList(
              new Target(new Coord4D(7, 8, 9, 0), new Coord4D(7, 8, 9, 0), EnumFacing.UP, true, true)));
        WirelessConnectionSnapshot original = new WirelessConnectionSnapshot(37, true, Arrays.asList(typeA, typeB));

        WirelessConnectionSnapshot restored = WirelessConnectionSnapshot.read(original.write(new NBTTagCompound()));

        assertEquals(37, restored.getRevision());
        assertTrue(restored.isEditable());
        assertEquals(Arrays.asList("type-a", "type-b"), Arrays.asList(
              restored.getMachineGroups().get(0).getKey(), restored.getMachineGroups().get(1).getKey()));
        assertEquals("tile.TypeA", restored.getMachineGroups().get(0).getNameKey());
        assertEquals(1, restored.getMachineGroups().get(0).getLoadedCount());
        assertEquals(first.getCoord(), restored.getMachineGroups().get(0).getTargets().get(0).getCoord());
        assertEquals(firstEndpoint, restored.getMachineGroups().get(0).getTargets().get(0).getEndpoint());
        assertEquals(second.getCoord(), restored.getMachineGroups().get(0).getTargets().get(1).getCoord());
        assertEquals(1, restored.getMachineGroups().get(0).getEnabledCount());
        assertFalse(restored.getMachineGroups().get(0).getTargets().get(1).isLoaded());
    }

    @Test
    void statusSnapshotOnlyUpdatesMatchingConnectionRevision() {
        Coord4D firstCoord = new Coord4D(1, 2, 3, 0);
        Coord4D secondCoord = new Coord4D(4, 5, 6, 0);
        WirelessConnectionSnapshot original = new WirelessConnectionSnapshot(9, true, Collections.singletonList(
              new MachineGroup("type-a", "tile.TypeA", Arrays.asList(
                    new Target(firstCoord, firstCoord, EnumFacing.NORTH, true, true),
                    new Target(secondCoord, secondCoord, EnumFacing.SOUTH, true, false)))));

        WirelessConnectionSnapshot unchanged = original.withLoadedStates(new WirelessConnectionStatusSnapshot(8, new byte[]{0, 1}));
        WirelessConnectionSnapshot updated = original.withLoadedStates(new WirelessConnectionStatusSnapshot(9, new byte[]{0, 1}));

        assertTrue(unchanged == original);
        assertFalse(updated.getMachineGroups().get(0).getTargets().get(0).isLoaded());
        assertTrue(updated.getMachineGroups().get(0).getTargets().get(1).isLoaded());
    }
}
