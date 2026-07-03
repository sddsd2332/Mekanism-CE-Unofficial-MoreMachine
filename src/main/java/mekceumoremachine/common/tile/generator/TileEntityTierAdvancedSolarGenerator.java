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
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.generators.common.tile.TileEntitySolarGenerator;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.interfaces.ITierMachine;
import mekceumoremachine.common.upgrade.FirstAdvancedSolarGeneratorUpgradeData;
import mekceumoremachine.common.upgrade.LargeMachineUpgradeDataApplier;
import micdoodle8.mods.galacticraft.api.world.ISolarLevel;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

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
    public double getMaxOutput() {
        double totalPeak = 0;
        for (BlockPos pos : getSolarPanelPositions()) {
            totalPeak += getPeakMultiplier(pos);
        }
        return getConfiguredMax() * (totalPeak / 9D) * 2;
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
        int visiblePanels = 0;
        for (BlockPos pos : getSolarPanelPositions()) {
            if (canPanelSeeSun(pos)) {
                visiblePanels++;
            }
        }
        return visiblePanels > 4;
    }

    @Override
    public double getProduction() {
        if (world == null) {
            return 0;
        }
        double generationMultiplier = 0;
        for (BlockPos pos : getSolarPanelPositions()) {
            if (canPanelSeeSun(pos)) {
                generationMultiplier += getGenerationMultiplier(pos);
            }
        }
        return getConfiguredMax() * getBrightnessMultiplier() * (generationMultiplier / 9D);
    }

    private BlockPos[] getSolarPanelPositions() {
        BlockPos topPos = getSolarCheckPos();
        return new BlockPos[]{
                topPos.add(-1, 0, -1),
                topPos.add(-1, 0, 0),
                topPos.add(-1, 0, 1),
                topPos.add(0, 0, -1),
                topPos,
                topPos.add(0, 0, 1),
                topPos.add(1, 0, -1),
                topPos.add(1, 0, 0),
                topPos.add(1, 0, 1)
        };
    }

    protected BlockPos getSolarCheckPos() {
        return getPos().up(3);
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
    public MachineTier getTier() {
        return tier;
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
        if (upgradeData instanceof FirstAdvancedSolarGeneratorUpgradeData data && data.getUpgradeTier() == tier.getBaseTier()) {
            onPlace();
            LargeMachineUpgradeDataApplier.applyCommon(this, data, null, securityComponent);
            LargeMachineUpgradeDataApplier.finish(this, null);
            return true;
        }
        return ITierMachine.super.parseUpgradeData(upgradeData);
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


    @Nonnull
    @Override
    public String getName() {
        return LangUtils.localize("tile." + fullName + "." + tier.getBaseTier().getSimpleName() + ".name");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Class<?> getSelectionWireframeModelClass() {
        return mekanism.generators.client.model.ModelAdvancedSolarGenerator.class;
    }

    @Override
    public boolean shouldApplyDefaultSelectionWireframeFacingRotation(IBlockState state, IBlockAccess world, BlockPos pos) {
        return true;
    }
}
