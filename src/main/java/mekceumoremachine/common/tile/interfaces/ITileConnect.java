package mekceumoremachine.common.tile.interfaces;


import mekanism.api.Coord4D;
import mekceumoremachine.common.tile.machine.TileEntityWirelessChargingEnergy.ConnectStatus;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

public interface ITileConnect {

    ConnectStatus connectOrCut(TileEntity tileEntity, EnumFacing facing, EntityPlayer player);

    Coord4D getPosition();

}

