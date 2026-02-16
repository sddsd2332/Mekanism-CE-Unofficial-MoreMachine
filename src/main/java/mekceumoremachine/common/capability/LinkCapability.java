package mekceumoremachine.common.capability;

import mekanism.api.Coord4D;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import org.jetbrains.annotations.Nullable;

public class LinkCapability {

    private Coord4D link;

    public void setLink(TileEntity tile) {
        link = Coord4D.get(tile);
    }


    public void stopLink() {
        link = null;
    }

    public Coord4D isLink() {
        return link;
    }


    public NBTTagCompound serializeNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        if (link != null) {
            link.write(tag);
        }
        return tag;
    }


    public void deserializeNBT(NBTTagCompound nbt) {
        if (nbt.hasKey("link")) {
            link = Coord4D.read(nbt);
        }
    }

    public static class Storage implements Capability.IStorage<LinkCapability> {

        @Override
        public @Nullable NBTBase writeNBT(Capability<LinkCapability> capability, LinkCapability instance, EnumFacing side) {
            return instance.serializeNBT();
        }

        @Override
        public void readNBT(Capability<LinkCapability> capability, LinkCapability instance, EnumFacing side, NBTBase nbt) {
            instance.deserializeNBT((NBTTagCompound) nbt);
        }
    }

}
