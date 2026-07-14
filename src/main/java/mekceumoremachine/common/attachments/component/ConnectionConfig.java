package mekceumoremachine.common.attachments.component;

import mekanism.api.Coord4D;
import mekceumoremachine.common.config.WirelessMachineTypeTableManager;
import mekceumoremachine.common.util.MachineStackTypeResolver;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import mekanism.common.tile.TileEntityBoundingBlock;

public class ConnectionConfig {

    private final Coord4D pos;
    private final Coord4D machinePos;
    private final EnumFacing facing;
    private final String machineTypeKey;
    private final String machineNameKey;
    private final ItemStack machineStack;
    private boolean chargingEnabled;

    public ConnectionConfig(TileEntity tileEntity, EnumFacing facing) {
        this(tileEntity, facing, true);
    }

    public ConnectionConfig(TileEntity tileEntity, EnumFacing facing, boolean chargingEnabled) {
        this(Coord4D.get(tileEntity), resolveMachineCoord(tileEntity), facing,
              resolveMachineDescriptor(resolveMachineTile(tileEntity), facing), chargingEnabled);
    }

    public ConnectionConfig(Coord4D pos, Coord4D machinePos, EnumFacing facing, String machineTypeKey, String machineNameKey,
          boolean chargingEnabled) {
        this(pos, machinePos, facing, machineTypeKey, machineNameKey, ItemStack.EMPTY, chargingEnabled);
    }

    public ConnectionConfig(Coord4D pos, Coord4D machinePos, EnumFacing facing, String machineTypeKey, String machineNameKey,
          ItemStack machineStack, boolean chargingEnabled) {
        this.pos = pos;
        this.machinePos = machinePos;
        this.facing = facing;
        this.machineTypeKey = machineTypeKey;
        this.machineNameKey = machineNameKey;
        this.machineStack = MachineStackTypeResolver.normalizeForType(machineStack);
        this.chargingEnabled = chargingEnabled;
    }

    private ConnectionConfig(Coord4D pos, Coord4D machinePos, EnumFacing facing, MachineTypeDescriptor descriptor, boolean chargingEnabled) {
        this.pos = pos;
        this.machinePos = machinePos;
        this.facing = facing;
        this.machineTypeKey = descriptor.getMachineTypeKey();
        this.machineNameKey = descriptor.getMachineNameKey();
        this.machineStack = descriptor.getMachineStack();
        this.chargingEnabled = chargingEnabled;
    }

    public Coord4D getCoord() {
        return pos;
    }

    public BlockPos getPos() {
        return pos.getPos();
    }

    public Coord4D getMachineCoord() {
        return machinePos;
    }

    public BlockPos getMachinePos() {
        return machinePos.getPos();
    }

    public EnumFacing getFacing() {
        return facing;
    }

    public String getMachineTypeKey() {
        return machineTypeKey;
    }

    public String getMachineNameKey() {
        return machineNameKey;
    }

    public ItemStack getMachineStack() {
        return machineStack;
    }

    public boolean isChargingEnabled() {
        return chargingEnabled;
    }

    public boolean setChargingEnabled(boolean chargingEnabled) {
        if (this.chargingEnabled == chargingEnabled) {
            return false;
        }
        this.chargingEnabled = chargingEnabled;
        return true;
    }

    public boolean hasSameTarget(ConnectionConfig other) {
        return pos.equals(other.pos) && machinePos.equals(other.machinePos) && facing == other.facing &&
               machineTypeKey.equals(other.machineTypeKey) && machineNameKey.equals(other.machineNameKey) &&
               ItemStack.areItemsEqual(machineStack, other.machineStack) &&
               ItemStack.areItemStackTagsEqual(machineStack, other.machineStack);
    }

    public static Coord4D resolveMachineCoord(TileEntity tileEntity) {
        if (tileEntity instanceof TileEntityBoundingBlock bounding && bounding.receivedCoords) {
            return new Coord4D(bounding.getMainPos(), tileEntity.getWorld());
        }
        return Coord4D.get(tileEntity);
    }

    public static TileEntity resolveMachineTile(TileEntity tileEntity) {
        if (tileEntity instanceof TileEntityBoundingBlock bounding) {
            TileEntity mainTile = bounding.getMainTile();
            if (mainTile != null) {
                return mainTile;
            }
        }
        return tileEntity;
    }

    private static MachineTypeDescriptor resolveMachineDescriptor(TileEntity tileEntity, EnumFacing facing) {
        return WirelessMachineTypeTableManager.resolve(tileEntity, facing);
    }
}
