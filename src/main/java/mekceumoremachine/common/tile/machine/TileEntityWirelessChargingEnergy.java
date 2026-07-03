package mekceumoremachine.common.tile.machine;

import io.netty.buffer.ByteBuf;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.Coord4D;
import mekanism.api.EnumColor;
import mekanism.api.IConfigCardAccess.ISpecialConfigData;
import mekanism.api.IContentsListener;
import mekanism.api.TileNetworkList;
import mekanism.api.transmitters.ITransmitter;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.Mekanism;
import mekanism.common.base.*;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.content.network.distribution.EnergyAcceptorTarget;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.integration.computer.IComputerIntegration;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.security.ISecurityTile;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.TileComponentSecurity;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.multiblock.TileEntityInductionPort;
import mekanism.common.tile.prefab.TileEntityElectricBlock;
import mekanism.common.util.*;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.attachments.component.ConnectionConfig;
import mekceumoremachine.common.capability.LinkTileEntity;
import mekceumoremachine.common.config.MoreMachineConfig;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.interfaces.INoWirelessChargingEnergy;
import mekceumoremachine.common.tile.interfaces.IConnectorPreviewProvider;
import mekceumoremachine.common.tile.interfaces.ITierMachine;
import mekceumoremachine.common.tile.interfaces.ITileConnect;
import mekceumoremachine.common.util.LinkUtils;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.*;

public class TileEntityWirelessChargingEnergy extends TileEntityElectricBlock implements IComputerIntegration, IRedstoneControl, ISideConfiguration, ISecurityTile,
        ISpecialConfigData, IComparatorSupport, IBoundingBlock, ITierMachine<MachineTier>, INoWirelessChargingEnergy, IHasVisualization, ITileConnect, IConnectorPreviewProvider, ISpecialSelectionWireframeTile {

    private static final int EMIT_TARGETS_PER_PROCESS = 16;


    public MachineTier tier = MachineTier.BASIC;

    public int currentRedstoneLevel;

    public RedstoneControl controlType;
    public int prevScale;
    public TileComponentEjector ejectorComponent;
    public TileComponentConfig configComponent;
    public TileComponentSecurity securityComponent;
    public List<ConnectionConfig> skipMachine = new ArrayList<>();
    public List<ConnectionConfig> connections = new ArrayList<>();
    private EnergyInventorySlot chargeSlot;
    private EnergyInventorySlot dischargeSlot;
    private int emitCursor;
    private boolean scanInProgress;
    private int scanMinChunkX;
    private int scanMaxChunkX;
    private int scanMinChunkZ;
    private int scanMaxChunkZ;
    private int scanChunkX;
    private int scanChunkZ;
    private BlockPos scanOrigin;
    private final Set<Coord4D> scanOccupiedLinks = new HashSet<>();

    public boolean enableEmit;
    public boolean clientRendering = false;
    public boolean scanMachine;

    public TileEntityWirelessChargingEnergy() {
        super("WirelessChargingEnergy", 0);
        configComponent = new TileComponentConfig(this, TransmissionType.ENERGY, TransmissionType.ITEM);
        initializeInventorySlots();
        configComponent.setupItemIOConfig(chargeSlot, dischargeSlot);
        configComponent.setConfig(TransmissionType.ITEM, DataType.NONE, DataType.EMPTY, DataType.NONE, DataType.NONE, DataType.NONE, DataType.INPUT);
        configComponent.setCanEject(TransmissionType.ITEM, false);

        configComponent.setInputConfig(TransmissionType.ENERGY);
        configComponent.setConfig(TransmissionType.ENERGY, DataType.INPUT, DataType.EMPTY, DataType.INPUT, DataType.INPUT, DataType.INPUT, DataType.INPUT);

        controlType = RedstoneControl.DISABLED;
        ejectorComponent = new TileComponentEjector(this);
        securityComponent = new TileComponentSecurity(this);
    }

    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = createInventorySlotHelper();
        chargeSlot = builder.addSlot(EnergyInventorySlot.drain(getMainEnergyContainer(listener), listener, 143, 35));
        chargeSlot.setSlotOverlay(SlotOverlay.PLUS);
        dischargeSlot = builder.addSlot(EnergyInventorySlot.fillOrConvert(getMainEnergyContainer(listener), this::getWorld, listener, 17, 35));
        dischargeSlot.setSlotOverlay(SlotOverlay.MINUS);
        return builder.build();
    }

    @Override
    public void validate() {
        super.validate();
        for (String entry : MoreMachineConfig.current().config.BlacklistMachine.get()) {
            blacklistTileClassNamePrefix(entry);
        }
    }


    @Override
    public void onUpdateServer() {
        super.onUpdateServer();
        chargeSlot.drainContainer();
        dischargeSlot.fillContainerOrConvert();
        if (scanMachine) {
            scan();
        }
        //每2.5s验证下链接内是否有效
        if (!scanInProgress && !connections.isEmpty() && ticksExisted % 50 == 0) {
            isValidLinks();
        }

        if (enableEmit) {
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

    @Override
    public boolean supportsAsync() {
        return false;
    }

    //扫描机器
    public void scan() {
        if (!scanInProgress) {
            beginScan();
        }
        runScanStep();
    }

    private void beginScan() {
        disconnectAllConnections();
        connections.clear();
        emitCursor = 0;
        scanOccupiedLinks.clear();
        scanOrigin = getPos().up(2);
        ChunkPos currentChunk = new ChunkPos(scanOrigin);
        int rang = tier.processes;
        scanMinChunkX = currentChunk.x - rang;
        scanMaxChunkX = currentChunk.x + rang;
        scanMinChunkZ = currentChunk.z - rang;
        scanMaxChunkZ = currentChunk.z + rang;
        scanChunkX = scanMinChunkX;
        scanChunkZ = scanMinChunkZ;
        scanInProgress = true;
    }

    private void runScanStep() {
        int chunks = 0;
        int maxLinks = getMaxLinks();
        while (chunks < getScanChunksPerTick() && scanChunkX <= scanMaxChunkX && connections.size() < maxLinks) {
            scanChunk(scanChunkX, scanChunkZ);
            advanceScanChunk();
            chunks++;
        }
        if (scanChunkX > scanMaxChunkX || connections.size() >= maxLinks) {
            finishScan();
        }
    }

    private void scanChunk(int chunkX, int chunkZ) {
        Chunk chunk = getWorld().getChunkProvider().getLoadedChunk(chunkX, chunkZ);
        if (chunk == null || chunk.isEmpty()) {
            return;
        }
        for (TileEntity tileEntity : new ArrayList<>(chunk.getTileEntityMap().values())) {
            if (connections.size() >= getMaxLinks()) {
                break;
            }
            tryAddScannedMachine(tileEntity);
        }
    }

    private void advanceScanChunk() {
        scanChunkZ++;
        if (scanChunkZ > scanMaxChunkZ) {
            scanChunkZ = scanMinChunkZ;
            scanChunkX++;
        }
    }

    private void finishScan() {
        scanInProgress = false;
        scanMachine = false;
        scanOrigin = null;
        scanOccupiedLinks.clear();
        Mekanism.packetHandler.sendUpdatePacket(this);
    }

    private int getScanChunksPerTick() {
        return Math.max(1, tier.processes);
    }

    private int getEmitTargetsPerTick() {
        return Math.max(1, tier.processes * EMIT_TARGETS_PER_PROCESS);
    }

    private void tryAddScannedMachine(TileEntity tileEntity) {
        if (tileEntity == null || scanOrigin == null) {
            return;
        }
        if (tileEntity instanceof INoWirelessChargingEnergy) {
            return;
        }
        if (isBlacklistMachine(tileEntity)) {
            return;
        }
        if (tileEntity instanceof ITransmitter) {
            return;
        }
        if (tileEntity instanceof TileEntityInductionPort) {
            return;
        }
        BlockPos tilePos = tileEntity.getPos();
        if (hasConnection(tilePos) || skipMachine.stream().anyMatch(tile -> tile.getPos().equals(tilePos))) {
            return;
        }
        Optional<LinkTileEntity> linkCapability = MEKCeuMoreMachine.getLinkInfoCap(tileEntity);
        if (linkCapability.isPresent()) {
            Coord4D coord4D = linkCapability.get().isLink();
            if (coord4D != null) {
                if (scanOccupiedLinks.contains(coord4D)) {
                    return;
                }
                if (!getWorldNN().isBlockLoaded(coord4D.getPos()) || getWorldNN().getTileEntity(coord4D.getPos()) == null) {
                    linkCapability.get().stopLink();
                } else {
                    scanOccupiedLinks.add(coord4D);
                    return;
                }
            }
        }
        if (isWithinSquareRange(scanOrigin, tilePos, getRang())) {
            for (EnumFacing side : EnumFacing.VALUES) {
                if (LinkUtils.isValidAcceptorOnSideInput(tileEntity, side)) {
                    connections.add(new ConnectionConfig(tileEntity, side.getOpposite()));
                    linkCapability.ifPresent(linkInfo -> linkInfo.setLink(this));
                    break;
                }
            }
        }
    }


    public void isValidLinks() {
        boolean changed = false;
        while (connections.size() > getMaxLinks()) {
            disconnectConnection(connections.remove(connections.size() - 1));
            changed = true;
        }
        for (int i = connections.size() - 1; i >= 0; i--) {
            ConnectionConfig config = connections.get(i);
            boolean loaded = getWorldNN().isBlockLoaded(config.getPos());
            if (!loaded || getWorldNN().isAirBlock(config.getPos())) {
                ConnectionConfig removed = connections.remove(i);
                if (loaded) {
                    disconnectConnection(removed);
                }
                if (emitCursor > i) {
                    emitCursor--;
                }
                changed = true;
            }
        }
        if (emitCursor >= connections.size()) {
            emitCursor = 0;
        }
        if (changed) {
            Mekanism.packetHandler.sendUpdatePacket(this);
        }
    }

    //充能机器
    public void emitMachine() {
        if (getWorld().isRemote || !MekanismUtils.canFunction(this) || connections.isEmpty()) {
            return;
        }
        if (MoreMachineConfig.current().config.enableDynamicWirelessCharging.val()) {
            emitDynamicMachine();
        } else {
            emitAllMachines();
        }
    }

    private void emitAllMachines() {
        boolean changed = false;
        for (ConnectionConfig machine : new ArrayList<>(connections)) {
            if (getEnergy() <= 0) {
                break;
            }
            if (!connections.contains(machine)) {
                continue;
            }
            double energyToSend = Math.min(getEnergy(), getMaxOutput());
            if (!getWorldNN().isBlockLoaded(machine.getPos())) {
                connections.remove(machine);
                changed = true;
                continue;
            }
            TileEntity tile = getWorldNN().getTileEntity(machine.getPos());
            if (tile == null) {
                connections.remove(machine);
                changed = true;
                continue;
            }

            EnumFacing opposite = machine.getFacing();
            try {
                EnergyAcceptorTarget target = new EnergyAcceptorTarget();
                EnergyAcceptorWrapper acceptor = EnergyAcceptorWrapper.get(tile, opposite);
                if (acceptor != null && acceptor.canReceiveEnergy(opposite) && acceptor.needsEnergy(opposite)) {
                    target.addHandler(opposite, acceptor);
                }
                int curHandlers = target.getHandlerCount();
                if (curHandlers > 0) {
                    double sent = EmitUtils.sendToAcceptors(target, energyToSend);
                    getMainEnergyContainer().extract(sent, Action.EXECUTE, AutomationType.INTERNAL);
                }
            } catch (Exception e) {
                Mekanism.logger.error("Wireless power station error occurred");
                Mekanism.logger.error("A machine that cannot be charged,pos: x {} y{} z {}, tile name:{}", tile.getPos().getX(), tile.getPos().getY(), tile.getPos().getZ(), tile.getDisplayName());
                Mekanism.logger.error("Will no longer charge this machine");
                skipMachine.add(machine);
                disconnectConnection(machine);
                connections.remove(machine);
                changed = true;
            }
        }
        if (emitCursor >= connections.size()) {
            emitCursor = 0;
        }
        if (changed) {
            Mekanism.packetHandler.sendUpdatePacket(this);
        }
    }

    private void emitDynamicMachine() {
        int processed = 0;
        boolean changed = false;
        while (processed < getEmitTargetsPerTick() && !connections.isEmpty() && getEnergy() > 0) {
            if (emitCursor >= connections.size()) {
                emitCursor = 0;
            }
            ConnectionConfig machine = connections.get(emitCursor);
            double energyToSend = Math.min(getEnergy(), getMaxOutput());
            if (!getWorldNN().isBlockLoaded(machine.getPos())) {
                connections.remove(emitCursor);
                changed = true;
                processed++;
                continue;
            }
            TileEntity tile = getWorldNN().getTileEntity(machine.getPos());
            if (tile == null) {
                connections.remove(emitCursor);
                changed = true;
                processed++;
                continue;
            }

            EnumFacing opposite = machine.getFacing();
            try {
                EnergyAcceptorTarget target = new EnergyAcceptorTarget();
                EnergyAcceptorWrapper acceptor = EnergyAcceptorWrapper.get(tile, opposite);
                if (acceptor != null && acceptor.canReceiveEnergy(opposite) && acceptor.needsEnergy(opposite)) {
                    target.addHandler(opposite, acceptor);
                }
                int curHandlers = target.getHandlerCount();
                if (curHandlers > 0) {
                    double sent = EmitUtils.sendToAcceptors(target, energyToSend);
                    getMainEnergyContainer().extract(sent, Action.EXECUTE, AutomationType.INTERNAL);
                }
                emitCursor++;
            } catch (Exception e) {
                Mekanism.logger.error("Wireless power station error occurred");
                Mekanism.logger.error("A machine that cannot be charged,pos: x {} y{} z {}, tile name:{}", tile.getPos().getX(), tile.getPos().getY(), tile.getPos().getZ(), tile.getDisplayName());
                Mekanism.logger.error("Will no longer charge this machine");
                skipMachine.add(machine);
                disconnectConnection(connections.remove(emitCursor));
                changed = true;
            }
            processed++;
        }
        if (emitCursor >= connections.size()) {
            emitCursor = 0;
        }
        if (changed) {
            Mekanism.packetHandler.sendUpdatePacket(this);
        }
    }


    @Override
    public void invalidate() {
        disconnectAllConnections();
        super.invalidate();
    }

    private void disconnectAllConnections() {
        connections.forEach(this::disconnectConnection);
    }

    private void disconnectConnection(ConnectionConfig connection) {
        if (!getWorldNN().isBlockLoaded(connection.getPos())) {
            return;
        }
        TileEntity tile = getWorldNN().getTileEntity(connection.getPos());
        if (tile != null) {
            MEKCeuMoreMachine.getLinkInfoCap(tile).ifPresent(LinkTileEntity::stopLink);
        }
    }

    private boolean hasConnection(BlockPos pos) {
        return connections.stream().anyMatch(connection -> connection.getPos().equals(pos));
    }

    //自动清除错误方块；
    public void autoClearErrorMachine() {
        if (MoreMachineConfig.current().config.enableAutoClearErrorMachine.val() && ticker % (MoreMachineConfig.current().config.AutoClearErrorMachineSecond.val() * 20) == 0) {
            for (ConnectionConfig machine : new ArrayList<>(skipMachine)) {
                BlockPos pos = machine.getPos();
                ChunkPos currentChunk = new ChunkPos(pos);
                Chunk chunk = getWorld().getChunkProvider().getLoadedChunk(currentChunk.x, currentChunk.z);
                if (chunk == null || chunk.isEmpty()) {
                    continue;
                }
                skipMachine.removeIf(tile -> getWorldNN().getTileEntity(pos) == null);
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
    public boolean applyTierUpgrade(BaseTier upgradeTier) {
        if (!tier.canUpgradeTo(upgradeTier)) {
            return false;
        }
        tier = MachineTier.get(upgradeTier);
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
            case BASIC -> MoreMachineConfig.current().config.BasicWirelessChargingOutput.val();
            case ADVANCED -> MoreMachineConfig.current().config.AdvancedWirelessChargingOutput.val();
            case ELITE -> MoreMachineConfig.current().config.EliteWirelessChargingOutput.val();
            case ULTIMATE -> MoreMachineConfig.current().config.UltimateWirelessChargingOutput.val();
        };
    }


    @Override
    public boolean sideIsConsumer(EnumFacing side) {
        return configComponent.hasSideForData(TransmissionType.ENERGY, facing, DataType.INPUT, side);
    }


    @Override
    public boolean sideIsOutput(EnumFacing side) {
        return false;
    }


    @Override
    public double getMaxEnergy() {
        return switch (tier) {
            case BASIC -> MoreMachineConfig.current().config.BasicWirelessChargingMaxEnergy.val();
            case ADVANCED -> MoreMachineConfig.current().config.AdvancedWirelessChargingMaxEnergy.val();
            case ELITE -> MoreMachineConfig.current().config.EliteWirelessChargingMAXEnergy.val();
            case ULTIMATE -> MoreMachineConfig.current().config.UltimateWirelessChargingMaxEnergy.val();
        };
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
                enableEmit = !enableEmit;
            } else if (type == 1) {
                setScanMachine();
            }
        }

        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            MachineTier prevTier = tier;
            tier = MachineTier.byIndex(dataStream.readInt());
            controlType = MekanismUtils.getByIndex(RedstoneControl.values(), dataStream.readInt(), controlType);
            enableEmit = dataStream.readBoolean();
            scanMachine = dataStream.readBoolean();
            connections.clear();
            int amount = dataStream.readInt();
            for (int i = 0; i < amount; i++) {
                connections.add(ConnectionConfig.read(dataStream));
            }
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
        data.add(enableEmit);
        data.add(scanMachine);
        data.add(connections.size());
        connections.forEach(c -> c.write(data));
        return data;
    }


    @Override
    public void readCustomNBT(NBTTagCompound nbtTags) {
        super.readCustomNBT(nbtTags);
        tier = MachineTier.byIndex(nbtTags.getInteger("tier"));
        controlType = MekanismUtils.getByIndex(RedstoneControl.values(), nbtTags.getInteger("controlType"), controlType);
        enableEmit = nbtTags.getBoolean("enable");
        scanMachine = nbtTags.getBoolean("scan");
        if (nbtTags.hasKey("connections")) {
            NBTTagList tagList = nbtTags.getTagList("connections", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < tagList.tagCount(); i++) {
                connections.add(ConnectionConfig.fromNBT(tagList.getCompoundTagAt(i)));
            }
        }
    }

    @Override
    public void writeCustomNBT(NBTTagCompound nbtTags) {
        super.writeCustomNBT(nbtTags);
        nbtTags.setInteger("tier", tier.ordinal());
        nbtTags.setInteger("controlType", controlType.ordinal());
        nbtTags.setBoolean("enable", enableEmit);
        nbtTags.setBoolean("scan", scanMachine);
        NBTTagList list = new NBTTagList();
        connections.forEach(c -> list.appendTag(c.toNBT()));
        if (list.tagCount() != 0) {
            nbtTags.setTag("connections", list);
        }

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
        nbtTags.setBoolean("enable", enableEmit);
        return nbtTags;
    }

    @Override
    public void setConfigurationData(NBTTagCompound nbtTags) {
        enableEmit = nbtTags.getBoolean("enable");
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
        BlockPos pos = getPos();
        int radius = getRang();
        return new AxisAlignedBB(
                pos.getX() - radius,
                pos.getY() - radius,
                pos.getZ() - radius,
                pos.getX() + radius + 1,
                pos.getY() + radius + 1,
                pos.getZ() + radius + 1
        );
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

    //设置扫描机器
    public void setScanMachine() {
        scanMachine = true;
        scanInProgress = false;
    }

    public int getScanMachineCount() {
        return connections.size();
    }

    @Override
    public List<ConnectionConfig> getPreviewConnections() {
        return connections;
    }

    @Override
    public Vec3d getPreviewOrigin() {
        BlockPos pos = getPos();
        return new Vec3d(pos.getX() + 0.5, pos.getY() + 2.8125, pos.getZ() + 0.5);
    }


    public int getMaxLinks() {
        return switch (tier) {
            case BASIC -> MoreMachineConfig.current().config.BasicWirelessChargingLink.val();
            case ADVANCED -> MoreMachineConfig.current().config.AdvancedWirelessChargingLink.val();
            case ELITE -> MoreMachineConfig.current().config.EliteWirelessChargingLink.val();
            case ULTIMATE -> MoreMachineConfig.current().config.UltimateWirelessChargingLink.val();
        };
    }


    @Override
    public ConnectStatus connectOrCut(TileEntity tileEntity, EnumFacing facing, EntityPlayer player) {
        ConnectStatus status = linkOrCut(tileEntity, facing, player);
        if (status != ConnectStatus.CONNECT_FAIL && !getWorldNN().isRemote) {
            if (status == ConnectStatus.DISCONNECT) {
                //通知机器断开
                MEKCeuMoreMachine.getLinkInfoCap(tileEntity).ifPresent(LinkTileEntity::stopLink);
                //发送成功断开链接的消息
                player.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + Mekanism.LOG_TAG + " " + EnumColor.INDIGO + LangUtils.localize("tooltip.connector.disconnect")));
            } else {
                //通知机器链接
                MEKCeuMoreMachine.getLinkInfoCap(tileEntity).ifPresent(linkInfo -> linkInfo.setLink(this));
                //发送成功链接的消息
                player.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + Mekanism.LOG_TAG + " " + EnumColor.INDIGO + LangUtils.localize("tooltip.connector.to")));
            }
            //通知更新机器
            Mekanism.packetHandler.sendUpdatePacket(this);
        }
        return status;
    }

    @Override
    public Coord4D getPosition() {
        return Coord4D.get(this);
    }


    public ConnectStatus linkOrCut(TileEntity tileEntity, EnumFacing facing, EntityPlayer player) {
        if (tileEntity == null) {
            return ConnectStatus.CONNECT_FAIL;
        }
        ConnectionConfig config = new ConnectionConfig(tileEntity, facing);
        // 已存在,移除连接
        if (connections.stream().anyMatch(c -> c.getPos().equals(config.getPos()))) {
            //注意，这是通过方块坐标来移除的，所以即使方块的面不对也能正确移除
            connections.removeIf(c -> c.getPos().equals(config.getPos()));
            return ConnectStatus.DISCONNECT;
        } else {
            if (tileEntity.getPos().equals(getPos())) {
                if (!getWorldNN().isRemote) {
                    player.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + Mekanism.LOG_TAG + " " + EnumColor.RED + LangUtils.localize("tooltip.connector.self")));
                }
                return ConnectStatus.CONNECT_FAIL;
            }
            if (connections.size() >= getMaxLinks()) {
                if (!getWorldNN().isRemote) {
                    player.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + Mekanism.LOG_TAG + " " + EnumColor.RED + LangUtils.localize("tooltip.connector.maxlinks")));
                }
                return ConnectStatus.CONNECT_FAIL;
            }
            BlockPos currentPos = getPos().up(2);
            if (isWithinSquareRange(currentPos, tileEntity.getPos(), getRang())) {
                //跳过黑名单匹配的机器
                if (isBlacklistMachine(tileEntity)) {
                    if (!getWorldNN().isRemote) {
                        player.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + Mekanism.LOG_TAG + " " + EnumColor.RED + LangUtils.localize("tooltip.connector.fail")));
                    }
                    return ConnectStatus.CONNECT_FAIL;
                }
                //跳过mek的线缆
                if (tileEntity instanceof ITransmitter) {
                    if (!getWorldNN().isRemote) {
                        player.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + Mekanism.LOG_TAG + " " + EnumColor.RED + LangUtils.localize("tooltip.connector.fail")));
                    }
                    return ConnectStatus.CONNECT_FAIL;
                }
                if (LinkUtils.isValidAcceptorOnSideInput(tileEntity, facing)) {
                    connections.add(config);
                    return ConnectStatus.CONNECT;
                } else {
                    if (!getWorldNN().isRemote) {
                        //无法连接到该方块，因为是不支持的能量系统或者没有能量接口
                        player.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + Mekanism.LOG_TAG + " " + EnumColor.RED + LangUtils.localize("tooltip.connector.fail")));
                    }
                    return ConnectStatus.CONNECT_FAIL;
                }

            } else {
                //连接过远，无法连接
                if (!getWorldNN().isRemote) {
                    player.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + Mekanism.LOG_TAG + " " + EnumColor.RED + LangUtils.localize("tooltip.connector.fail_pos")));
                }
                return ConnectStatus.CONNECT_FAIL;
            }
        }
    }

    private static boolean isWithinSquareRange(BlockPos source, BlockPos target, int range) {
        return Math.abs(source.getX() - target.getX()) <= range
                && Math.abs(source.getY() - target.getY()) <= range
                && Math.abs(source.getZ() - target.getZ()) <= range;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Class<?> getSelectionWireframeModelClass() {
        return mekceumoremachine.client.model.machine.ModelWirelessChargingEnergy.class;
    }

}
