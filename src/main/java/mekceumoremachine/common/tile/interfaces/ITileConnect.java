package mekceumoremachine.common.tile.interfaces;


import mekanism.api.Coord4D;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

public interface ITileConnect {

    ConnectStatus connectOrCut(TileEntity tileEntity, EnumFacing facing, EntityPlayer player);

    Coord4D getPosition();


     enum ConnectStatus {
        CONNECT, //成功连接
        DISCONNECT, //断开连接
        CONNECT_FAIL// 连接失败
    }

}

