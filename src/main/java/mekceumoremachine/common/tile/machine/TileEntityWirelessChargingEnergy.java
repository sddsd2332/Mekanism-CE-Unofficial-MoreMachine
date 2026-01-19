package mekceumoremachine.common.tile.machine;

import cofh.redstoneflux.api.IEnergyReceiver;
import io.netty.buffer.ByteBuf;
import mekanism.api.Coord4D;
import mekanism.api.IConfigCardAccess.ISpecialConfigData;
import mekanism.api.TileNetworkList;
import mekanism.api.energy.IStrictEnergyAcceptor;
import mekanism.api.transmitters.ITransmitter;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.Mekanism;
import mekanism.common.SideData;
import mekanism.common.base.*;
import mekanism.common.base.target.EnergyAcceptorTarget;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.integration.computer.IComputerIntegration;
import mekanism.common.integration.ic2.IC2Integration;
import mekanism.common.security.ISecurityTile;
import mekanism.common.tier.BaseTier;
import mekanism.common.tier.InductionCellTier;
import mekanism.common.tier.InductionProviderTier;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.TileComponentSecurity;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.multiblock.TileEntityInductionPort;
import mekanism.common.tile.prefab.TileEntityElectricBlock;
import mekanism.common.util.*;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.config.MoreMachineConfig;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.interfaces.INoWirelessChargingEnergy;
import mekceumoremachine.common.tile.interfaces.ITierMachine;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TileEntityWirelessChargingEnergy extends TileEntityElectricBlock implements IComputerIntegration, IRedstoneControl, ISideConfiguration, ISecurityTile,
        ISpecialConfigData, IComparatorSupport, IBoundingBlock, ITierMachine<MachineTier>, INoWirelessChargingEnergy, IHasVisualization {


    public MachineTier tier = MachineTier.BASIC;

    public int currentRedstoneLevel;

    public RedstoneControl controlType;
    public int prevScale;
    public TileComponentEjector ejectorComponent;
    public TileComponentConfig configComponent;
    public TileComponentSecurity securityComponent;
    public Map<BlockPos, TileEntity> skipMachine = new HashMap<>();

    public boolean enable;
    public boolean clientRendering = false;

    public TileEntityWirelessChargingEnergy() {
        super("WirelessChargingEnergy", 0);
        configComponent = new TileComponentConfig(this, TransmissionType.ENERGY, TransmissionType.ITEM);
        configComponent.addOutput(TransmissionType.ITEM, new SideData(DataType.NONE, InventoryUtils.EMPTY));
        configComponent.addOutput(TransmissionType.ITEM, new SideData(DataType.INPUT, new int[]{0}));
        configComponent.addOutput(TransmissionType.ITEM, new SideData(DataType.OUTPUT, new int[]{1}));
        configComponent.setConfig(TransmissionType.ITEM, new byte[]{0, -1, 0, 0, 0, 1});
        configComponent.setCanEject(TransmissionType.ITEM, false);

        configComponent.setInputConfig(TransmissionType.ENERGY);
        configComponent.setConfig(TransmissionType.ENERGY, new byte[]{1, -1, 1, 1, 1, 1});

        inventory = NonNullListSynchronized.withSize(2, ItemStack.EMPTY);
        controlType = RedstoneControl.DISABLED;
        ejectorComponent = new TileComponentEjector(this);
        securityComponent = new TileComponentSecurity(this);
    }

    @Override
    public void validate() {
        super.validate();
        for (String entry : MoreMachineConfig.current().config.BlacklistMachine.get()) {
            blacklistTileClassNamePrefix(entry);
        }
    }

    @Override
    public void onAsyncUpdateServer() {
        super.onAsyncUpdateServer();
        ChargeUtils.charge(0, this);
        ChargeUtils.discharge(1, this);
        if (enable) {
            emitMachine();
        }
        if (!skipMachine.isEmpty()) {
            autoClearErrorMachine();
        }
        int newScale = getScaledEnergyLevel(20);
        if (newScale != prevScale) {
            Mekanism.packetHandler.sendUpdatePacket(this);
        }
        prevScale = newScale;
    }

    //充能机器
    public void emitMachine() {
        if (getWorld().isRemote || !MekanismUtils.canFunction(this)) {
            return;
        }
        Map<BlockPos, TileEntity> rangMachine = getRangMachine();
        double energyToSend = Math.min(getEnergy(), getMaxOutput());
        if (rangMachine.isEmpty()) {
            return;
        }
        for (Map.Entry<BlockPos, TileEntity> machine : rangMachine.entrySet()) {
            if (getEnergy() <= 0) {
                break;
            }
            TileEntity tile = machine.getValue();
            try {
                EnergyAcceptorTarget target = new EnergyAcceptorTarget();
                for (EnumFacing side : EnumFacing.VALUES) {
                    EnumFacing opposite = side.getOpposite();
                    EnergyAcceptorWrapper acceptor = EnergyAcceptorWrapper.get(tile, opposite);
                    if (acceptor != null && acceptor.canReceiveEnergy(opposite) && acceptor.needsEnergy(opposite)) {
                        target.addHandler(opposite, acceptor);
                    }
                }
                int curHandlers = target.getHandlers().size();
                if (curHandlers > 0) {
                    double sent = EmitUtils.sendToAcceptors(java.util.Collections.singleton(target), curHandlers, energyToSend);
                    setEnergy(getEnergy() - sent);
                }
            } catch (Exception e) {
                Mekanism.logger.error("Wireless power station error occurred");
                Mekanism.logger.error("A machine that cannot be charged,pos: x {} y{} z {}", tile.getPos().getX(), tile.getPos().getY(), tile.getPos().getZ());
                Mekanism.logger.error("Will no longer charge this machine");
                skipMachine.put(tile.getPos(), tile);
            }
        }
    }

    //获取范围内的机器
    public Map<BlockPos, TileEntity> getRangMachine() {
        if (!enable) {
            return new HashMap<>();
        }
        World world = getWorld();
        BlockPos currentPos = getPos().up(2);
        ChunkPos currentChunk = new ChunkPos(currentPos);
        Map<BlockPos, TileEntity> rangMachine = new HashMap<>();
        int rang = tier.processes;
        for (int chunkX = currentChunk.x - rang; chunkX <= currentChunk.x + rang; chunkX++) {
            for (int chunkZ = currentChunk.z - rang; chunkZ <= currentChunk.z + rang; chunkZ++) {
                Chunk chunk = world.getChunkProvider().getLoadedChunk(chunkX, chunkZ);
                if (chunk == null || chunk.isEmpty()) {
                    continue;
                }
                for (TileEntity tileEntity : chunk.getTileEntityMap().values()) {
                    //跳过含有不能无线充能的机器
                    if (tileEntity instanceof INoWirelessChargingEnergy) {
                        continue;
                    }
                    if (isBlacklistMachine(tileEntity)) {
                        continue;
                    }
                    //跳过mek的线缆
                    if (tileEntity instanceof ITransmitter) {
                        continue;
                    }
                    //跳过端口
                    if (tileEntity instanceof TileEntityInductionPort) {
                        continue;
                    }
                    BlockPos tilePos = tileEntity.getPos();
                    //跳过错误的机器
                    if (skipMachine.containsKey(tilePos)) {
                        continue;
                    }
                    double distanceSquared = currentPos.distanceSq(tilePos);
                    if (distanceSquared <= getRang() * getRang()) {
                        //如果有这个机器方块位置了，跳过
                        if (rangMachine.containsKey(tilePos)) {
                            continue;
                        }
                        //MEK自己的机器
                        if (tileEntity instanceof IStrictEnergyAcceptor acceptor) {
                            for (EnumFacing side : EnumFacing.VALUES) {
                                if (acceptor.canReceiveEnergy(side.getOpposite())) {
                                    rangMachine.put(tilePos, tileEntity);
                                    break;
                                }
                            }
                            continue;
                        }

                        //forge能量系统
                        if (MekanismUtils.useForge() && MoreMachineConfig.current().config.enableFEWirelessRecharge.val()) {
                            if (tileEntity instanceof IEnergyStorage iEnergyStorage) {
                                if (iEnergyStorage.canReceive()) {
                                    rangMachine.put(tilePos, tileEntity);
                                    continue;
                                }
                            }
                        }

                        //RF能量系统
                        if (MekanismUtils.useRF() && MoreMachineConfig.current().config.enableRFWirelessRecharge.val()) {
                            if (tileEntity instanceof IEnergyReceiver receiver) {
                                for (EnumFacing side : EnumFacing.VALUES) {
                                    if (receiver.canConnectEnergy(side)) {
                                        rangMachine.put(tilePos, tileEntity);
                                        break;
                                    }
                                }
                                continue;
                            }
                        }

                        //Tesla能量系统
                        if (MekanismUtils.useTesla() && MoreMachineConfig.current().config.enableTeslaWirelessRecharge.val()) {
                            if (hasCapability(tileEntity, Capabilities.TESLA_CONSUMER_CAPABILITY)) {
                                rangMachine.put(tilePos, tileEntity);
                                continue;
                            }
                        }
                        //IC2的能量系统
                        if (MekanismUtils.useIC2() && MoreMachineConfig.current().config.enableEuWirelessRecharge.val()) {
                            for (EnumFacing side : EnumFacing.VALUES) {
                                if (IC2Integration.isAcceptor(tileEntity, side)) {
                                    rangMachine.put(tilePos, tileEntity);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        return rangMachine;
    }


    //检查各个面是否有给定的cap
    public boolean hasCapability(TileEntity tileEntity, Capability<?> cap) {
        for (EnumFacing side : EnumFacing.VALUES) {
            if (CapabilityUtils.hasCapability(tileEntity, cap, side.getOpposite())) {
                return true;
            }
        }
        return false;
    }

    public void autoClearErrorMachine() {
        if (MoreMachineConfig.current().config.enableAutoClearErrorMachine.val() && ticker % (MoreMachineConfig.current().config.AutoClearErrorMachineSecond.val() * 20) == 0) {
            for (Map.Entry<BlockPos, TileEntity> machine : skipMachine.entrySet()) {
                BlockPos pos = machine.getKey();
                ChunkPos currentChunk = new ChunkPos(pos);
                Chunk chunk = getWorld().getChunkProvider().getLoadedChunk(currentChunk.x, currentChunk.z);
                if (chunk == null || chunk.isEmpty()) {
                    continue;
                }
                skipMachine.values().removeIf(tile -> getWorld().getTileEntity(pos) == null);
            }
        }
    }


    private List<String> blacklistedPrefixes = new LinkedList<>();

    public boolean isBlacklistMachine(TileEntity tile) {
        if (tile == null) {
            return true; //Nothing there.
        }
        Class<?> tClass = tile.getClass();
        String className = tClass.getName();
        String lCClassName = className.toLowerCase();
        if (lCClassName.startsWith("[L")) {
            lCClassName = lCClassName.substring(2); //Cull descriptor
        }
        for (String pref : blacklistedPrefixes) {
            if (lCClassName.startsWith(pref)) {
                return true;
            }
        }
        return false;
    }

    public void blacklistTileClassNamePrefix(String prefix) {
        if (!blacklistedPrefixes.contains(prefix.toLowerCase())) {
            blacklistedPrefixes.add(prefix.toLowerCase());
        }
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


    @Nonnull
    @Override
    public String getName() {
        return LangUtils.localize("tile.WirelessChargingEnergy." + tier.getBaseTier().getSimpleName() + ".name");
    }

    @Override
    public double getMaxOutput() {
        return switch (tier) {
            case BASIC -> InductionProviderTier.BASIC.getOutput();
            case ADVANCED -> InductionProviderTier.ADVANCED.getOutput();
            case ELITE -> InductionProviderTier.ELITE.getOutput();
            case ULTIMATE -> InductionProviderTier.ULTIMATE.getOutput();
        };
    }


    @Override
    public boolean isItemValidForSlot(int slotID, @Nonnull ItemStack itemstack) {
        if (slotID == 0) {
            return ChargeUtils.canBeCharged(itemstack);
        } else if (slotID == 1) {
            return ChargeUtils.canBeDischarged(itemstack);
        }
        return true;
    }


    @Override
    public boolean sideIsConsumer(EnumFacing side) {
        return configComponent.hasSideForData(TransmissionType.ENERGY, facing, 1, side);
    }


    @Override
    public boolean sideIsOutput(EnumFacing side) {
        return false;
    }


    @Override
    public double getMaxEnergy() {
        return switch (tier) {
            case BASIC -> InductionCellTier.BASIC.getMaxEnergy();
            case ADVANCED -> InductionCellTier.ADVANCED.getMaxEnergy();
            case ELITE -> InductionCellTier.ELITE.getMaxEnergy();
            case ULTIMATE -> InductionCellTier.ULTIMATE.getMaxEnergy();
        };
    }

    @Nonnull
    @Override
    public int[] getSlotsForFace(@Nonnull EnumFacing side) {
        return configComponent.getOutput(TransmissionType.ITEM, side, facing).availableSlots;
    }

    @Override
    public boolean canExtractItem(int slotID, @Nonnull ItemStack itemstack, @Nonnull EnumFacing side) {
        if (slotID == 1) {
            return ChargeUtils.canBeOutputted(itemstack, false);
        } else if (slotID == 0) {
            return ChargeUtils.canBeOutputted(itemstack, true);
        }
        return false;
    }


    @Override
    public String[] getMethods() {
        return new String[0];
    }

    @Override
    public Object[] invoke(int method, Object[] args) throws NoSuchMethodException {
        return new Object[0];
    }

    @Override
    public void handlePacketData(ByteBuf dataStream) {
        super.handlePacketData(dataStream);
        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            int type = dataStream.readInt();
            if (type == 0) {
                enable = !enable;
            }
        }

        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            MachineTier prevTier = tier;
            tier = MachineTier.values()[dataStream.readInt()];
            controlType = RedstoneControl.values()[dataStream.readInt()];
            enable = dataStream.readBoolean();
            if (prevTier != tier) {
                MekanismUtils.updateBlock(world, getPos());
            }
        }
    }

    @Override
    public TileNetworkList getNetworkedData(TileNetworkList data) {
        super.getNetworkedData(data);
        data.add(tier.ordinal());
        data.add(controlType.ordinal());
        data.add(enable);
        return data;
    }


    @Override
    public void readCustomNBT(NBTTagCompound nbtTags) {
        super.readCustomNBT(nbtTags);
        tier = MachineTier.values()[nbtTags.getInteger("tier")];
        controlType = RedstoneControl.values()[nbtTags.getInteger("controlType")];
        enable = nbtTags.getBoolean("enable");
    }

    @Override
    public void writeCustomNBT(NBTTagCompound nbtTags) {
        super.writeCustomNBT(nbtTags);
        nbtTags.setInteger("tier", tier.ordinal());
        nbtTags.setInteger("controlType", controlType.ordinal());
        nbtTags.setBoolean("enable", enable);
    }


    @Override
    public void setEnergy(double energy) {
        super.setEnergy(energy);
        int newRedstoneLevel = getRedstoneLevel();
        if (newRedstoneLevel != currentRedstoneLevel) {
            markNoUpdateSync();
            currentRedstoneLevel = newRedstoneLevel;
        }
    }

    @Override
    public int getRedstoneLevel() {
        return MekanismUtils.redstoneLevelFromContents(getEnergy(), getMaxEnergy());
    }

    @Override
    public RedstoneControl getControlType() {
        return controlType;
    }

    @Override
    public void setControlType(RedstoneControl type) {
        controlType = type;
    }

    @Override
    public boolean canPulse() {
        return false;
    }

    @Override
    public TileComponentEjector getEjector() {
        return ejectorComponent;
    }

    @Override
    public TileComponentConfig getConfig() {
        return configComponent;
    }

    @Override
    public EnumFacing getOrientation() {
        return facing;
    }

    @Override
    public TileComponentSecurity getSecurity() {
        return securityComponent;
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, EnumFacing side) {
        return capability == Capabilities.CONFIG_CARD_CAPABILITY || capability == Capabilities.SPECIAL_CONFIG_DATA_CAPABILITY || super.hasCapability(capability, side);
    }

    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, EnumFacing side) {
        //Special isCapabilityDisabled override not needed here as it already gets handled in TileEntityElectricBlock
        if (capability == Capabilities.CONFIG_CARD_CAPABILITY) {
            return Capabilities.CONFIG_CARD_CAPABILITY.cast(this);
        } else if (capability == Capabilities.SPECIAL_CONFIG_DATA_CAPABILITY) {
            return Capabilities.SPECIAL_CONFIG_DATA_CAPABILITY.cast(this);
        }
        return super.getCapability(capability, side);
    }

    @Override
    public boolean canSetFacing(@Nonnull EnumFacing facing) {
        return facing != EnumFacing.DOWN && facing != EnumFacing.UP;
    }

    @Override
    public NBTTagCompound getConfigurationData(NBTTagCompound nbtTags) {
        nbtTags.setBoolean("enable", enable);
        return nbtTags;
    }

    @Override
    public void setConfigurationData(NBTTagCompound nbtTags) {
        enable = nbtTags.getBoolean("enable");
    }

    @Override
    public String getDataType() {
        return getName();
    }


    @Override
    public int getBlockGuiID(Block block, int i) {
        return 17;
    }

    @Override
    public IGuiProvider guiProvider() {
        return MEKCeuMoreMachine.proxy;
    }


    @Override
    public void onPlace() {
        Coord4D current = Coord4D.get(this);
        MekanismUtils.makeBoundingBlock(world, getPos().up(), current);
        MekanismUtils.makeBoundingBlock(world, getPos().up(2), current);
    }

    @Override
    public void onBreak() {
        world.setBlockToAir(getPos().up());
        world.setBlockToAir(getPos().up(2));
        world.setBlockToAir(getPos());
    }

    @Nonnull
    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }

    @Override
    public MachineTier getTier() {
        return tier;
    }

    public int getRang() {
        return tier.processes * 16;
    }

    @Override
    public boolean isClientRendering() {
        return clientRendering;
    }

    @Override
    public void toggleClientRendering() {
        clientRendering = !clientRendering;
    }

}
