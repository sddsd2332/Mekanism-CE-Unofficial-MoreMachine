package mekceumoremachine.common.capability;

import mekanism.api.Coord4D;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.INBTSerializable;

public interface LinkTileEntity extends INBTSerializable<NBTTagCompound> {

    void setLink(TileEntity tile);

    void stopLink();

    Coord4D isLink();


}
