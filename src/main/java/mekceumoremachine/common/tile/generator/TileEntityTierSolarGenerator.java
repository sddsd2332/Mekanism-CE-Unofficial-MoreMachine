package mekceumoremachine.common.tile.generator;

import io.netty.buffer.ByteBuf;
import mekanism.api.TileNetworkList;
import mekanism.common.Mekanism;
import mekanism.common.config.MekanismConfig;
import mekanism.common.tier.BaseTier;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.generators.common.tile.TileEntitySolarGenerator;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.interfaces.ITierMachine;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nonnull;

public class TileEntityTierSolarGenerator extends TileEntitySolarGenerator implements ITierMachine<MachineTier> {

    public MachineTier tier = MachineTier.BASIC;

    public TileEntityTierSolarGenerator() {
        super("TierSolarGenerator", 0, 0);
    }


    @Override
    protected float getConfiguredMax() {
        return (float) MekanismConfig.current().generators.solarGeneration.val() * tier.processes;
    }

    @Override
    public double getMaxEnergy() {
        return MekanismConfig.current().generators.solarGeneratorStorage.val() * tier.processes;
    }

    @Override
    public double getProduction() {
        return super.getProduction() * tier.processes;
    }

    @Override
    public double getMaxOutput(){
        return getMaxEnergy() * 2;
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
    public MachineTier getTier() {
        return tier;
    }

    @Nonnull
    @Override
    public String getName() {
        return LangUtils.localize("tile." + fullName + "." + tier.getBaseTier().getSimpleName() + ".name");
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
}
