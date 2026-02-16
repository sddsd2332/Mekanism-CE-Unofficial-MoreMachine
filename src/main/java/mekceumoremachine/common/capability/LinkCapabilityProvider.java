package mekceumoremachine.common.capability;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class LinkCapabilityProvider implements ICapabilitySerializable<NBTTagCompound> {

    @CapabilityInject(LinkCapability.class)
    public static Capability<LinkCapability> LINK_CAPABILITY = null;
    private LinkCapability instance = LINK_CAPABILITY.getDefaultInstance();


    @Override
    public boolean hasCapability(@Nonnull Capability<?> cap, @Nullable EnumFacing side) {
        return cap == LINK_CAPABILITY;
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> cap, @Nullable EnumFacing side) {
        if (this.hasCapability(cap, side)) {
            return LINK_CAPABILITY.cast(this.createCapability());
        }
        return null;
    }

    @Nonnull
    private LinkCapability createCapability() {
        if (this.instance == null) {
            this.instance = new LinkCapability();
        }
        return this.instance;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        this.createCapability().deserializeNBT(nbt);
    }

    @Override
    public NBTTagCompound serializeNBT() {
        return this.createCapability().serializeNBT();
    }
}
