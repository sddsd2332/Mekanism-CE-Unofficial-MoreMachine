package mekceumoremachine.common.capability;

import mekanism.api.Coord4D;
import mekceumoremachine.common.MEKCeuMoreMachine;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DefaultLinkCapability implements LinkTileEntity {

    private Coord4D link = null;

    @Override
    public void setLink(TileEntity tile) {
        link = Coord4D.get(tile);
    }


    @Override
    public void stopLink() {
        link = null;
    }

    @Override
    public Coord4D isLink() {
        return link;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        if (link != null) {
            link.write(tag);
        }
        return tag;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        if (nbt.hasKey("link")) {
            link = Coord4D.read(nbt);
        }
    }

    public static void register() {
        CapabilityManager.INSTANCE.register(LinkTileEntity.class, new Capability.IStorage<>() {
            @Override
            public NBTTagCompound writeNBT(Capability<LinkTileEntity> capability, LinkTileEntity instance, EnumFacing side) {
                return instance.serializeNBT();
            }

            @Override
            public void readNBT(Capability<LinkTileEntity> capability, LinkTileEntity instance, EnumFacing side, NBTBase nbt) {
                if (nbt instanceof NBTTagCompound tag) {
                    instance.deserializeNBT(tag);
                }
            }
        }, DefaultLinkCapability::new);
    }

    public static class Provider implements ICapabilitySerializable<NBTTagCompound> {


        @CapabilityInject(LinkTileEntity.class)
        public static Capability<LinkTileEntity> LINK_CAPABILITY = null;

        public static final ResourceLocation NAME = new ResourceLocation(MEKCeuMoreMachine.MODID, "link");
        private final LinkTileEntity defaultImpl = new DefaultLinkCapability();

        @Override
        public boolean hasCapability(@NotNull Capability<?> capability, @Nullable EnumFacing facing) {
            return capability == LINK_CAPABILITY;
        }

        @Override
        public <T> T getCapability(Capability<T> capability, EnumFacing side) {
            if (capability == LINK_CAPABILITY) {
                return LINK_CAPABILITY.cast(defaultImpl);
            }
            return null;
        }

        @Override
        public NBTTagCompound serializeNBT() {
            return defaultImpl.serializeNBT();
        }

        @Override
        public void deserializeNBT(NBTTagCompound nbt) {
            defaultImpl.deserializeNBT(nbt);
        }
    }
}
