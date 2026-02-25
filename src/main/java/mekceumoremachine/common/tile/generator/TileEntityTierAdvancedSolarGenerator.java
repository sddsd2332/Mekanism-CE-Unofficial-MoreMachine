package mekceumoremachine.common.tile.generator;

import io.netty.buffer.ByteBuf;
import mekanism.api.Coord4D;
import mekanism.api.IEvaporationSolar;
import mekanism.api.TileNetworkList;
import mekanism.common.Mekanism;
import mekanism.common.base.IBoundingBlock;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.config.MekanismConfig;
import mekanism.common.tier.BaseTier;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.generators.common.tile.TileEntitySolarGenerator;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.interfaces.ITierMachine;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nonnull;

public class TileEntityTierAdvancedSolarGenerator extends TileEntitySolarGenerator implements IBoundingBlock, IEvaporationSolar, ITierMachine<MachineTier> {


    public MachineTier tier = MachineTier.BASIC;


    public TileEntityTierAdvancedSolarGenerator() {
        super("TierAdvancedSolarGenerator", 0, 0);
    }

    @Override
    public boolean sideIsOutput(EnumFacing side) {
        return side == facing;
    }


    @Override
    protected float getConfiguredMax() {
        return (float) MekanismConfig.current().generators.advancedSolarGeneration.val() * tier.processes;
    }

    @Override
    public double getMaxEnergy() {
        return MekanismConfig.current().generators.advancedSolarGeneratorStorage.val() * tier.processes;
    }

    @Override
    public double getMaxOutput(){
        return getMaxEnergy() * 2;
    }

    @Override
    public double getProduction() {
        return super.getProduction() * tier.processes;
    }

    @Override
    public void onPlace() {
        Coord4D current = Coord4D.get(this);
        MekanismUtils.makeBoundingBlock(world, getPos().add(0, 1, 0), current);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                MekanismUtils.makeBoundingBlock(world, getPos().add(x, 2, z), current);
            }
        }
    }

    @Override
    public void onBreak() {
        world.setBlockToAir(getPos().add(0, 1, 0));
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                world.setBlockToAir(getPos().add(x, 2, z));
            }
        }
        invalidate();
        world.setBlockToAir(getPos());
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, EnumFacing side) {
        return capability == Capabilities.EVAPORATION_SOLAR_CAPABILITY || super.hasCapability(capability, side);
    }

    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, EnumFacing side) {
        if (capability == Capabilities.EVAPORATION_SOLAR_CAPABILITY) {
            return Capabilities.EVAPORATION_SOLAR_CAPABILITY.cast(this);
        }
        return super.getCapability(capability, side);
    }

    @Override
    protected boolean canSeeSky() {
        return world.canSeeSky(getPos().up(3));
    }


    @Override
    public MachineTier getTier() {
        return tier;
    }

    @Override
    public boolean upgrade(BaseTier upgradeTier) {
        if (upgradeTier.ordinal() != tier.ordinal() + 1) {
            return false;
        }
        if (upgradeTier == BaseTier.CREATIVE) {
            return false;
        }
        tier = MachineTier.values()[upgradeTier.ordinal()];
        Mekanism.packetHandler.sendUpdatePacket(this);
        markNoUpdateSync();
        return true;
    }


    @Override
    public void handlePacketData(ByteBuf dataStream) {
        super.handlePacketData(dataStream);
        if (isRemote()) {
            MachineTier prevTier = tier;
            tier = MachineTier.values()[dataStream.readInt()];
            if (prevTier != tier) {
                MekanismUtils.updateBlock(world, getPos());
            }
        }
    }

    @Override
    public TileNetworkList getNetworkedData(TileNetworkList data) {
        super.getNetworkedData(data);
        data.add(tier.ordinal());
        return data;
    }

    @Override
    public void readCustomNBT(NBTTagCompound nbtTags) {
        super.readCustomNBT(nbtTags);
        tier = MachineTier.values()[nbtTags.getInteger("tier")];
    }

    @Override
    public void writeCustomNBT(NBTTagCompound nbtTags) {
        super.writeCustomNBT(nbtTags);
        nbtTags.setInteger("tier", tier.ordinal());
    }


    @Nonnull
    @Override
    public String getName() {
        return LangUtils.localize("tile." + fullName + "." + tier.getBaseTier().getSimpleName() + ".name");
    }
}
