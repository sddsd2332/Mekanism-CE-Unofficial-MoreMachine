package mekceumoremachine.common.attachments.component;

import io.netty.buffer.ByteBuf;
import mekanism.api.Coord4D;
import mekanism.api.TileNetworkList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class ConnectionConfig {

    private static final int FACING_COUNT = 6;

    public Coord4D pos;
    public EnumFacing facing;

    public ConnectionConfig(TileEntity tileEntity, EnumFacing facing) {
        this(Coord4D.get(tileEntity), facing);
    }

    public ConnectionConfig(Coord4D pos, EnumFacing facing) {
        this.pos = pos;
        this.facing = facing;
    }

    public BlockPos getPos() {
        return pos.getPos();
    }

    public EnumFacing getFacing() {
        return facing;
    }

    /**
     * 保存到NBT
     */
    public NBTTagCompound toNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        pos.write(tag);
        tag.setInteger("facing", facing.getIndex());
        return tag;
    }

    /**
     * 从NBT加载
     */
    public static ConnectionConfig fromNBT(NBTTagCompound tag) {
        Coord4D pos = Coord4D.read(tag);
        EnumFacing facing = getFacing(tag.getInteger("facing"));
        return new ConnectionConfig(pos, facing);
    }

    public  void write(TileNetworkList data) {
        pos.write(data);
        data.add(facing.getIndex());
    }

    public static ConnectionConfig read(ByteBuf data) {
        Coord4D pos = Coord4D.read(data);
        EnumFacing facing = getFacing(data.readInt());
        return new ConnectionConfig(pos, facing);
    }

    private static EnumFacing getFacing(int index) {
        return index >= 0 && index < FACING_COUNT ? EnumFacing.byIndex(index) : EnumFacing.NORTH;
    }

}
