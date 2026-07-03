package mekceumoremachine.common.tile.generator;

import io.netty.buffer.ByteBuf;
import mekanism.api.TileNetworkList;
import mekanism.common.Mekanism;
import mekanism.common.config.MekanismConfig;
import mekanism.common.tier.BaseTier;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.generators.common.tile.TileEntitySolarGenerator;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.interfaces.ITierMachine;
import mekceumoremachine.common.upgrade.FirstSolarGeneratorUpgradeData;
import mekceumoremachine.common.upgrade.LargeMachineUpgradeDataApplier;
import micdoodle8.mods.galacticraft.api.world.ISolarLevel;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

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
        BlockPos pos = getSolarCheckPos();
        if (!canPanelSeeSun(pos)) {
            return 0;
        }
        return getConfiguredMax() * getBrightnessMultiplier() * getGenerationMultiplier(pos);
    }

    @Override
    public double getMaxOutput() {
        return getConfiguredMax() * getPeakMultiplier(getSolarCheckPos()) * 2;
    }

    protected BlockPos getSolarCheckPos() {
        return getPos();
    }

    protected boolean canPanelSeeSun(BlockPos pos) {
        return world != null && world.isDaytime() && !world.provider.isNether() && world.canSeeSky(pos);
    }

    protected float getBrightnessMultiplier() {
        if (world == null) {
            return 0;
        }
        float brightness = world.getSunBrightnessFactor(1.0F);
        if (MekanismUtils.existsAndInstance(world.provider, "micdoodle8.mods.galacticraft.api.world.ISolarLevel")) {
            brightness *= ((ISolarLevel) world.provider).getSolarEnergyMultiplier();
        }
        return brightness;
    }

    protected float getGenerationMultiplier(BlockPos pos) {
        float multiplier = getPeakMultiplier(pos);
        if (needsRainCheck(pos) && (world.isRaining() || world.isThundering())) {
            multiplier *= 0.2F;
        }
        return multiplier;
    }

    protected float getPeakMultiplier(BlockPos pos) {
        if (world == null) {
            return 0;
        }
        Biome biome = world.provider.getBiomeForCoords(pos);
        float tempEff = 0.3F * (0.8F - biome.getTemperature(pos));
        float humidityEff = -0.3F * (biome.canRain() ? biome.getRainfall() : 0.0F);
        return 1.0F + tempEff + humidityEff;
    }

    protected boolean needsRainCheck(BlockPos pos) {
        return world != null && world.provider.getBiomeForCoords(pos).canRain();
    }

    @Override
    public boolean applyTierUpgrade(BaseTier upgradeTier) {
        if (!tier.canUpgradeTo(upgradeTier)) {
            return false;
        }
        tier = MachineTier.get(upgradeTier);
        Mekanism.packetHandler.sendUpdatePacket(this);
        markNoUpdateSync();
        return true;
    }

    @Override
    public boolean parseUpgradeData(IUpgradeData upgradeData) {
        if (upgradeData instanceof FirstSolarGeneratorUpgradeData data && data.getUpgradeTier() == tier.getBaseTier()) {
            LargeMachineUpgradeDataApplier.applyCommon(this, data, null, securityComponent);
            LargeMachineUpgradeDataApplier.finish(this, null);
            return true;
        }
        return ITierMachine.super.parseUpgradeData(upgradeData);
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
            tier = MachineTier.byIndex(dataStream.readInt());
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
        tier = MachineTier.byIndex(nbtTags.getInteger("tier"));
    }

    @Override
    public void writeCustomNBT(NBTTagCompound nbtTags) {
        super.writeCustomNBT(nbtTags);
        nbtTags.setInteger("tier", tier.ordinal());
    }
}
