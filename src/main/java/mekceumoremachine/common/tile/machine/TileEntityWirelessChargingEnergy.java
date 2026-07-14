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
import mekceumoremachine.common.config.WirelessConnectionDataManager;
import mekceumoremachine.common.config.WirelessConnectionDataManager.LoadedData;
import mekceumoremachine.common.config.WirelessStationRegistryManager;
import mekceumoremachine.common.network.PacketWirelessConnection;
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
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.*;

public class TileEntityWirelessChargingEnergy extends TileEntityElectricBlock implements IComputerIntegration, IRedstoneControl, ISideConfiguration, ISecurityTile,
        ISpecialConfigData, IComparatorSupport, IBoundingBlock, ISustainedData, ITierMachine<MachineTier>, INoWirelessChargingEnergy, IHasVisualization,
        ITileConnect, IConnectorPreviewProvider, ISpecialSelectionWireframeTile {

    private static final int EMIT_TARGETS_PER_PROCESS = 16;
    private static final int VALIDATION_TARGETS_PER_PROCESS = 2;
    private static final String NBT_DYNAMIC_WIRELESS_CHARGING = "dynamicWirelessCharging";
    private static final String CONFIG_CARD_DATA_TYPE = "tile.WirelessChargingEnergy.name";


    public MachineTier tier = MachineTier.BASIC;

    public int currentRedstoneLevel;

    public RedstoneControl controlType;
    public int prevScale;
    public TileComponentEjector ejectorComponent;
    public TileComponentConfig configComponent;
    public TileComponentSecurity securityComponent;
    private final Set<BlockPos> skipMachine = new HashSet<>();
    public final List<ConnectionConfig> connections = new ArrayList<>();
    private EnergyInventorySlot chargeSlot;
    private EnergyInventorySlot dischargeSlot;
    private int emitCursor;
    private int validationCursor;
    private boolean dynamicWirelessCharging;
    private boolean scanInProgress;
    private int scanMinChunkX;
    private int scanMaxChunkX;
    private int scanMinChunkZ;
    private int scanMaxChunkZ;
    private int scanChunkX;
    private int scanChunkZ;
    private BlockPos scanOrigin;
    private boolean scanChanged;
    private UUID connectionDataId = UUID.randomUUID();
    private boolean connectionsLoaded;
    private boolean connectionDataRemoved;
    private int connectionRevision;
    private int clientConnectionCount;

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
        ejectorComponent.setOutputData(configComponent, TransmissionType.ITEM);
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
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) {
            WirelessStationRegistryManager.register(this);
        }
    }


    @Override
    public void onUpdateServer() {
        super.onUpdateServer();
        ensureConnectionsLoaded();
        chargeSlot.drainContainer();
        dischargeSlot.fillContainerOrConvert();
        validateConnectionsStep();
        if (scanMachine) {
            scan();
        }

        emitMachine();
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
        ensureConnectionsLoaded();
        scanChanged = false;
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
            try {
                tryAddScannedMachine(tileEntity);
            } catch (RuntimeException e) {
                markMachineError(tileEntity, e);
            }
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
        if (scanChanged) {
            connectionDataChanged();
        } else {
            Mekanism.packetHandler.sendUpdatePacket(this);
        }
    }

    private int getScanChunksPerTick() {
        return Math.max(1, tier.processes);
    }

    private int getEmitTargetsPerTick() {
        return Math.max(1, tier.processes * EMIT_TARGETS_PER_PROCESS);
    }

    private int getValidationTargetsPerTick() {
        return Math.max(1, tier.processes * VALIDATION_TARGETS_PER_PROCESS);
    }

    private void tryAddScannedMachine(TileEntity tileEntity) {
        if (tileEntity == null || scanOrigin == null) {
            return;
        }
        if (tileEntity instanceof INoWirelessChargingEnergy energy && energy.isChargingEnergy()) {
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
        TileEntity machineTile = ConnectionConfig.resolveMachineTile(tileEntity);
        if (machineTile == tileEntity && tileEntity instanceof mekanism.common.tile.TileEntityBoundingBlock) {
            return;
        }
        BlockPos machinePos = ConnectionConfig.resolveMachineCoord(tileEntity).getPos();
        if (hasConnection(machinePos) || skipMachine.contains(machinePos)) {
            return;
        }
        Optional<LinkTileEntity> linkCapability = MEKCeuMoreMachine.getLinkInfoCap(machineTile);
        if (linkCapability.isPresent() && isClaimedByOtherStation(linkCapability.get(), machineTile)) {
            return;
        }
        if (isWithinSquareRange(scanOrigin, tilePos, getRang())) {
            for (EnumFacing targetSide : EnumFacing.VALUES) {
                if (LinkUtils.isValidAcceptorOnTargetSideInput(tileEntity, targetSide)) {
                    addConnectionGrouped(new ConnectionConfig(tileEntity, targetSide));
                    linkCapability.ifPresent(linkInfo -> {
                        linkInfo.setLink(this);
                        machineTile.markDirty();
                    });
                    scanChanged = true;
                    break;
                }
            }
        }
    }


    private void validateConnectionsStep() {
        ensureConnectionsLoaded();
        boolean changed = false;
        while (connections.size() > getMaxLinks()) {
            removeConnectionAt(connections.size() - 1);
            changed = true;
        }
        int validated = 0;
        while (validated < getValidationTargetsPerTick() && !connections.isEmpty()) {
            if (validationCursor >= connections.size()) {
                validationCursor = 0;
            }
            int result = validateConnectionAt(validationCursor);
            changed |= result != 0;
            if (result != 2) {
                validationCursor++;
            }
            validated++;
        }
        if (emitCursor >= connections.size()) {
            emitCursor = 0;
        }
        if (changed) {
            normalizeConnectionOrder();
            validationCursor = 0;
            connectionDataChanged();
        }
    }

    // 0: unchanged, 1: refreshed, 2: removed
    private int validateConnectionAt(int index) {
        ConnectionConfig current = connections.get(index);
        if (!isConnectionChunkLoaded(current)) {
            return 0;
        }
        TileEntity endpointTile = getWorldNN().getTileEntity(current.getPos());
        if (endpointTile == null || getWorldNN().isAirBlock(current.getPos())) {
            removeConnectionAt(index);
            return 2;
        }
        try {
            EnumFacing validFacing = findValidTargetSide(endpointTile, current.getFacing());
            if (validFacing == null || isBlacklistMachine(endpointTile) || endpointTile instanceof ITransmitter) {
                removeConnectionAt(index);
                return 2;
            }
            ConnectionConfig refreshed = new ConnectionConfig(endpointTile, validFacing, current.isChargingEnabled());
            if (hasConnection(refreshed.getMachinePos(), index)) {
                removeConnectionAt(index);
                return 2;
            }
            TileEntity refreshedMachineTile = getWorldNN().getTileEntity(refreshed.getMachinePos());
            if (refreshedMachineTile == null || getWorldNN().isAirBlock(refreshed.getMachinePos())) {
                removeConnectionAt(index);
                return 2;
            }
            Optional<LinkTileEntity> refreshedLink = MEKCeuMoreMachine.getLinkInfoCap(refreshedMachineTile);
            if (refreshedLink.isPresent() && isClaimedByOtherStation(refreshedLink.get(), refreshedMachineTile)) {
                removeConnectionAt(index);
                return 2;
            }
            boolean changed = !current.hasSameTarget(refreshed);
            if (changed && !current.getMachineCoord().equals(refreshed.getMachineCoord())) {
                disconnectConnection(current);
            }
            if (changed) {
                connections.set(index, refreshed);
            }
            claimConnection(refreshedMachineTile, refreshedLink);
            return changed ? 1 : 0;
        } catch (RuntimeException e) {
            markMachineError(current, endpointTile, e);
            removeConnectionAt(index);
            return 2;
        }
    }

    private EnumFacing findValidTargetSide(TileEntity tile, EnumFacing preferred) {
        if (preferred != null && LinkUtils.isValidAcceptorOnTargetSideInput(tile, preferred)) {
            return preferred;
        }
        for (EnumFacing side : EnumFacing.VALUES) {
            if (side != preferred && LinkUtils.isValidAcceptorOnTargetSideInput(tile, side)) {
                return side;
            }
        }
        return null;
    }

    private void claimConnection(TileEntity machineTile, Optional<LinkTileEntity> linkCapability) {
        linkCapability.ifPresent(link -> {
            if (!Coord4D.get(this).equals(link.isLink())) {
                link.setLink(this);
                machineTile.markDirty();
            }
        });
    }

    private boolean isConnectionChunkLoaded(ConnectionConfig connection) {
        return connection.getCoord().dimensionId == getWorldNN().provider.getDimension() &&
               connection.getMachineCoord().dimensionId == getWorldNN().provider.getDimension() &&
               getWorldNN().isBlockLoaded(connection.getPos()) && getWorldNN().isBlockLoaded(connection.getMachinePos());
    }

    public boolean isConnectionLoaded(ConnectionConfig connection) {
        return isConnectionChunkLoaded(connection) && getWorldNN().getTileEntity(connection.getPos()) != null &&
               getWorldNN().getTileEntity(connection.getMachinePos()) != null;
    }

    //充能机器
    public void emitMachine() {
        if (getWorld().isRemote || !MekanismUtils.canFunction(this) || connections.isEmpty()) {
            return;
        }
        if (dynamicWirelessCharging) {
            emitDynamicMachine();
        } else {
            emitAllMachines();
        }
    }

    private void emitAllMachines() {
        boolean changed = false;
        int index = 0;
        while (index < connections.size()) {
            if (getEnergy() <= 0) {
                break;
            }
            ConnectionConfig machine = connections.get(index);
            if (!machine.isChargingEnabled()) {
                index++;
                continue;
            }
            double energyToSend = Math.min(getEnergy(), getMaxOutput());
            if (!isConnectionChunkLoaded(machine)) {
                index++;
                continue;
            }
            TileEntity tile = getWorldNN().getTileEntity(machine.getPos());
            if (tile == null) {
                removeConnectionAt(index);
                changed = true;
                continue;
            }

            EnumFacing targetSide = machine.getFacing();
            try {
                EnergyAcceptorTarget target = new EnergyAcceptorTarget();
                EnergyAcceptorWrapper acceptor = EnergyAcceptorWrapper.get(tile, targetSide);
                if (acceptor != null && acceptor.canReceiveEnergy(targetSide) && acceptor.needsEnergy(targetSide)) {
                    target.addHandler(targetSide, acceptor);
                }
                int curHandlers = target.getHandlerCount();
                if (curHandlers > 0) {
                    double sent = EmitUtils.sendToAcceptors(target, energyToSend);
                    getMainEnergyContainer().extract(sent, Action.EXECUTE, AutomationType.INTERNAL);
                }
                index++;
            } catch (Exception e) {
                markMachineError(machine, tile, e);
                removeConnectionAt(index);
                changed = true;
            }
        }
        if (emitCursor >= connections.size()) {
            emitCursor = 0;
        }
        if (changed) {
            connectionDataChanged();
        }
    }

    private void emitDynamicMachine() {
        int visited = 0;
        boolean changed = false;
        int visitLimit = Math.min(getEmitTargetsPerTick(), connections.size());
        while (visited < visitLimit && !connections.isEmpty() && getEnergy() > 0) {
            if (emitCursor >= connections.size()) {
                emitCursor = 0;
            }
            ConnectionConfig machine = connections.get(emitCursor);
            visited++;
            if (!machine.isChargingEnabled()) {
                emitCursor++;
                continue;
            }
            if (!isConnectionChunkLoaded(machine)) {
                emitCursor++;
                continue;
            }
            TileEntity tile = getWorldNN().getTileEntity(machine.getPos());
            if (tile == null) {
                removeConnectionAt(emitCursor);
                changed = true;
                continue;
            }

            EnumFacing targetSide = machine.getFacing();
            try {
                double energyToSend = Math.min(getEnergy(), getMaxOutput());
                EnergyAcceptorTarget target = new EnergyAcceptorTarget();
                EnergyAcceptorWrapper acceptor = EnergyAcceptorWrapper.get(tile, targetSide);
                if (acceptor != null && acceptor.canReceiveEnergy(targetSide) && acceptor.needsEnergy(targetSide)) {
                    target.addHandler(targetSide, acceptor);
                }
                int curHandlers = target.getHandlerCount();
                if (curHandlers > 0) {
                    double sent = EmitUtils.sendToAcceptors(target, energyToSend);
                    getMainEnergyContainer().extract(sent, Action.EXECUTE, AutomationType.INTERNAL);
                }
                emitCursor++;
            } catch (Exception e) {
                markMachineError(machine, tile, e);
                removeConnectionAt(emitCursor);
                changed = true;
            }
        }
        if (emitCursor >= connections.size()) {
            emitCursor = 0;
        }
        if (changed) {
            connectionDataChanged();
        }
    }


    @Override
    public void invalidate() {
        if (!isRemote() && connectionsLoaded && !connectionDataRemoved) {
            flushConnectionData();
        }
        super.invalidate();
    }

    @Override
    public void onChunkUnload() {
        if (!isRemote() && connectionsLoaded && !connectionDataRemoved) {
            flushConnectionData();
        }
        super.onChunkUnload();
    }

    private void disconnectAllConnections() {
        connections.forEach(this::disconnectConnection);
    }

    private void disconnectConnection(ConnectionConfig connection) {
        if (!getWorldNN().isBlockLoaded(connection.getMachinePos())) {
            return;
        }
        TileEntity tile = getWorldNN().getTileEntity(connection.getMachinePos());
        if (tile != null) {
            try {
                MEKCeuMoreMachine.getLinkInfoCap(tile).ifPresent(link -> {
                    if (Coord4D.get(this).equals(link.isLink())) {
                        link.stopLink();
                        tile.markDirty();
                    }
                });
            } catch (RuntimeException e) {
                Mekanism.logger.warn("Failed to clear wireless charging link from target {} ({})", tile.getPos(), tile.getClass().getName(), e);
            }
        }
    }

    private void ensureConnectionsLoaded() {
        if (connectionsLoaded || getWorld() == null || getWorld().isRemote) {
            return;
        }
        LoadedData loadedData = WirelessConnectionDataManager.load(this);
        connections.clear();
        connections.addAll(loadedData.getConnections());
        connectionRevision = loadedData.getRevision();
        clientConnectionCount = connections.size();
        connectionsLoaded = true;
        WirelessStationRegistryManager.register(this);
        normalizeConnectionOrder();
        if (connections.size() > getMaxLinks()) {
            while (connections.size() > getMaxLinks()) {
                removeConnectionAt(connections.size() - 1);
            }
            connectionDataChanged();
        }
    }

    private void saveConnectionData() {
        WirelessConnectionDataManager.save(this, connections, connectionRevision);
    }

    private void flushConnectionData() {
        if (scanChanged) {
            connectionRevision = connectionRevision == Integer.MAX_VALUE ? 1 : connectionRevision + 1;
            clientConnectionCount = connections.size();
            scanChanged = false;
        }
        saveConnectionData();
    }

    private void connectionDataChanged() {
        connectionRevision = connectionRevision == Integer.MAX_VALUE ? 1 : connectionRevision + 1;
        clientConnectionCount = connections.size();
        if (emitCursor >= connections.size()) {
            emitCursor = 0;
        }
        if (validationCursor >= connections.size()) {
            validationCursor = 0;
        }
        scanChanged = false;
        saveConnectionData();
        Mekanism.packetHandler.sendUpdatePacket(this);
        PacketWirelessConnection.syncOpenConfigWindows(this);
    }

    private void addConnectionGrouped(ConnectionConfig connection) {
        int insertIndex = connections.size();
        for (int i = connections.size() - 1; i >= 0; i--) {
            if (connections.get(i).getMachineTypeKey().equals(connection.getMachineTypeKey())) {
                insertIndex = i + 1;
                break;
            }
        }
        connections.add(insertIndex, connection);
        if (insertIndex <= emitCursor && connections.size() > 1) {
            emitCursor++;
        }
        if (insertIndex <= validationCursor && connections.size() > 1) {
            validationCursor++;
        }
    }

    private ConnectionConfig removeConnectionAt(int index) {
        ConnectionConfig removed = connections.remove(index);
        disconnectConnection(removed);
        if (emitCursor > index) {
            emitCursor--;
        }
        if (validationCursor > index) {
            validationCursor--;
        }
        return removed;
    }

    private void markMachineError(ConnectionConfig connection, TileEntity tile, Exception exception) {
        Mekanism.logger.error("Wireless power station failed to handle target {} ({}) and will skip it",
                tile.getPos(), tile.getClass().getName(), exception);
        skipMachine.add(connection.getMachinePos());
    }

    private void markMachineError(TileEntity tile, Exception exception) {
        BlockPos machinePos;
        try {
            machinePos = ConnectionConfig.resolveMachineCoord(tile).getPos();
        } catch (RuntimeException ignored) {
            machinePos = tile.getPos();
        }
        Mekanism.logger.error("Wireless power station failed to inspect target {} ({}) and will skip it",
              tile.getPos(), tile.getClass().getName(), exception);
        skipMachine.add(machinePos);
    }

    private boolean isClaimedByOtherStation(LinkTileEntity link, TileEntity target) {
        Coord4D linkedStation = link.isLink();
        if (linkedStation == null || linkedStation.equals(Coord4D.get(this))) {
            return false;
        }
        if (linkedStation.dimensionId == getWorldNN().provider.getDimension() && getWorldNN().isBlockLoaded(linkedStation.getPos())) {
            TileEntity source = getWorldNN().getTileEntity(linkedStation.getPos());
            if (source instanceof TileEntityWirelessChargingEnergy station && !source.isInvalid()) {
                WirelessStationRegistryManager.register(station);
                if (station.hasConnection(target.getPos())) {
                    return true;
                }
            } else {
                WirelessStationRegistryManager.unregister(getWorldNN(), linkedStation);
            }
        } else {
            UUID stationId = WirelessStationRegistryManager.getStationId(getWorldNN(), linkedStation);
            if (stationId != null && WirelessConnectionDataManager.containsConnection(getWorldNN(), stationId, linkedStation, target.getPos())) {
                return true;
            }
        }
        link.stopLink();
        target.markDirty();
        return false;
    }

    private void normalizeConnectionOrder() {
        List<String> typeOrder = getMachineTypeOrder();
        rebuildByTypeOrder(typeOrder);
    }

    private List<String> getMachineTypeOrder() {
        List<String> typeOrder = new ArrayList<>();
        for (ConnectionConfig connection : connections) {
            if (!typeOrder.contains(connection.getMachineTypeKey())) {
                typeOrder.add(connection.getMachineTypeKey());
            }
        }
        return typeOrder;
    }

    private void rebuildByTypeOrder(List<String> typeOrder) {
        Map<String, List<ConnectionConfig>> grouped = new LinkedHashMap<>();
        for (ConnectionConfig connection : connections) {
            grouped.computeIfAbsent(connection.getMachineTypeKey(), key -> new ArrayList<>()).add(connection);
        }
        connections.clear();
        for (String typeKey : typeOrder) {
            List<ConnectionConfig> group = grouped.remove(typeKey);
            if (group != null) {
                connections.addAll(group);
            }
        }
        grouped.values().forEach(connections::addAll);
    }

    private void rebuildConnectionGroup(String typeKey, List<ConnectionConfig> reorderedGroup) {
        List<ConnectionConfig> rebuilt = new ArrayList<>(connections.size());
        boolean inserted = false;
        for (ConnectionConfig connection : connections) {
            if (connection.getMachineTypeKey().equals(typeKey)) {
                if (!inserted) {
                    rebuilt.addAll(reorderedGroup);
                    inserted = true;
                }
            } else {
                rebuilt.add(connection);
            }
        }
        connections.clear();
        connections.addAll(rebuilt);
    }

    public UUID getConnectionDataId() {
        return connectionDataId;
    }

    public int getConnectionRevision() {
        ensureConnectionsLoaded();
        return connectionRevision;
    }

    public boolean isDynamicWirelessCharging() {
        return dynamicWirelessCharging;
    }

    public boolean setDynamicWirelessCharging(boolean dynamicWirelessCharging) {
        if (this.dynamicWirelessCharging == dynamicWirelessCharging) {
            return false;
        }
        this.dynamicWirelessCharging = dynamicWirelessCharging;
        emitCursor = 0;
        markDirty();
        if (getWorld() != null && !getWorld().isRemote) {
            Mekanism.packetHandler.sendUpdatePacket(this);
        }
        return true;
    }

    public void regenerateConnectionDataId() {
        connectionDataId = UUID.randomUUID();
        markDirty();
        if (getWorld() != null && !getWorld().isRemote) {
            WirelessStationRegistryManager.register(this);
        }
    }

    public List<ConnectionConfig> getConnectionsSnapshot() {
        ensureConnectionsLoaded();
        return new ArrayList<>(connections);
    }

    public boolean setAllConnectionsEnabled(boolean enabled) {
        ensureConnectionsLoaded();
        boolean changed = false;
        for (ConnectionConfig connection : connections) {
            changed |= connection.setChargingEnabled(enabled);
        }
        if (changed) {
            connectionDataChanged();
        }
        return changed;
    }

    public boolean setMachineTypeEnabled(String machineTypeKey, boolean enabled) {
        ensureConnectionsLoaded();
        boolean changed = false;
        for (ConnectionConfig connection : connections) {
            if (connection.getMachineTypeKey().equals(machineTypeKey)) {
                changed |= connection.setChargingEnabled(enabled);
            }
        }
        if (changed) {
            connectionDataChanged();
        }
        return changed;
    }

    public boolean setConnectionEnabled(Coord4D target, boolean enabled) {
        ensureConnectionsLoaded();
        for (ConnectionConfig connection : connections) {
            if (connection.getMachineCoord().equals(target) && connection.setChargingEnabled(enabled)) {
                connectionDataChanged();
                return true;
            }
        }
        return false;
    }

    public boolean removeMachineType(String machineTypeKey) {
        ensureConnectionsLoaded();
        boolean changed = false;
        for (int i = connections.size() - 1; i >= 0; i--) {
            if (connections.get(i).getMachineTypeKey().equals(machineTypeKey)) {
                removeConnectionAt(i);
                changed = true;
            }
        }
        if (changed) {
            emitCursor = 0;
            validationCursor = 0;
            connectionDataChanged();
        }
        return changed;
    }

    public boolean removeAllConnections() {
        ensureConnectionsLoaded();
        if (connections.isEmpty()) {
            return false;
        }
        disconnectAllConnections();
        connections.clear();
        emitCursor = 0;
        validationCursor = 0;
        connectionDataChanged();
        return true;
    }

    public boolean removeConnection(String machineTypeKey, Coord4D target) {
        ensureConnectionsLoaded();
        for (int i = 0; i < connections.size(); i++) {
            ConnectionConfig connection = connections.get(i);
            if (connection.getMachineTypeKey().equals(machineTypeKey) && connection.getMachineCoord().equals(target)) {
                removeConnectionAt(i);
                emitCursor = 0;
                validationCursor = 0;
                connectionDataChanged();
                return true;
            }
        }
        return false;
    }

    public boolean moveMachineType(String machineTypeKey, int direction, boolean toEdge) {
        ensureConnectionsLoaded();
        List<String> typeOrder = getMachineTypeOrder();
        int index = typeOrder.indexOf(machineTypeKey);
        if (index < 0) {
            return false;
        }
        int targetIndex = toEdge ? (direction < 0 ? 0 : typeOrder.size() - 1) : index + Integer.signum(direction);
        if (targetIndex < 0 || targetIndex >= typeOrder.size() || targetIndex == index) {
            return false;
        }
        typeOrder.remove(index);
        typeOrder.add(targetIndex, machineTypeKey);
        rebuildByTypeOrder(typeOrder);
        emitCursor = 0;
        validationCursor = 0;
        connectionDataChanged();
        return true;
    }

    public boolean moveConnection(String machineTypeKey, Coord4D target, int direction, boolean toEdge) {
        ensureConnectionsLoaded();
        List<ConnectionConfig> group = new ArrayList<>();
        for (ConnectionConfig connection : connections) {
            if (connection.getMachineTypeKey().equals(machineTypeKey)) {
                group.add(connection);
            }
        }
        int index = -1;
        for (int i = 0; i < group.size(); i++) {
            if (group.get(i).getMachineCoord().equals(target)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return false;
        }
        int targetIndex = toEdge ? (direction < 0 ? 0 : group.size() - 1) : index + Integer.signum(direction);
        if (targetIndex < 0 || targetIndex >= group.size() || targetIndex == index) {
            return false;
        }
        ConnectionConfig connection = group.remove(index);
        group.add(targetIndex, connection);
        rebuildConnectionGroup(machineTypeKey, group);
        emitCursor = 0;
        validationCursor = 0;
        connectionDataChanged();
        return true;
    }

    public void removeConnectionData() {
        if (getWorld() == null || getWorld().isRemote || connectionDataRemoved) {
            return;
        }
        ensureConnectionsLoaded();
        disconnectAllConnections();
        connections.clear();
        clientConnectionCount = 0;
        connectionDataRemoved = true;
        WirelessConnectionDataManager.delete(this);
        WirelessStationRegistryManager.unregister(this);
    }

    private boolean hasConnection(BlockPos pos) {
        return hasConnection(pos, -1);
    }

    private boolean hasConnection(BlockPos pos, int excludedIndex) {
        ensureConnectionsLoaded();
        for (int i = 0; i < connections.size(); i++) {
            if (i != excludedIndex && connections.get(i).getMachinePos().equals(pos)) {
                return true;
            }
        }
        return false;
    }

    //自动清除错误方块；
    public void autoClearErrorMachine() {
        if (MoreMachineConfig.current().config.enableAutoClearErrorMachine.val() && ticker % (MoreMachineConfig.current().config.AutoClearErrorMachineSecond.val() * 20) == 0) {
            skipMachine.removeIf(machine -> {
                ChunkPos currentChunk = new ChunkPos(machine);
                Chunk chunk = getWorld().getChunkProvider().getLoadedChunk(currentChunk.x, currentChunk.z);
                if (chunk == null || chunk.isEmpty()) {
                    return false;
                }
                return getWorldNN().getTileEntity(machine) == null;
            });
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
            if (type == 1) {
                setScanMachine();
            }
        }

        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            MachineTier prevTier = tier;
            tier = MachineTier.byIndex(dataStream.readInt());
            controlType = MekanismUtils.getByIndex(RedstoneControl.values(), dataStream.readInt(), controlType);
            scanMachine = dataStream.readBoolean();
            clientConnectionCount = dataStream.readInt();
            connectionRevision = dataStream.readInt();
            dynamicWirelessCharging = dataStream.readBoolean();
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
        data.add(scanMachine);
        data.add(getScanMachineCount());
        data.add(connectionRevision);
        data.add(dynamicWirelessCharging);
        return data;
    }


    @Override
    public void readCustomNBT(NBTTagCompound nbtTags) {
        super.readCustomNBT(nbtTags);
        tier = MachineTier.byIndex(nbtTags.getInteger("tier"));
        controlType = MekanismUtils.getByIndex(RedstoneControl.values(), nbtTags.getInteger("controlType"), controlType);
        scanMachine = nbtTags.getBoolean("scan");
        dynamicWirelessCharging = nbtTags.getBoolean(NBT_DYNAMIC_WIRELESS_CHARGING);
        connectionDataId = nbtTags.hasUniqueId("connectionDataId") ? nbtTags.getUniqueId("connectionDataId") : UUID.randomUUID();
        connections.clear();
        connectionsLoaded = false;
        connectionDataRemoved = false;
        connectionRevision = 0;
        clientConnectionCount = 0;
    }

    @Override
    public void writeCustomNBT(NBTTagCompound nbtTags) {
        super.writeCustomNBT(nbtTags);
        nbtTags.setInteger("tier", tier.ordinal());
        nbtTags.setInteger("controlType", controlType.ordinal());
        nbtTags.setBoolean("scan", scanMachine);
        nbtTags.setBoolean(NBT_DYNAMIC_WIRELESS_CHARGING, dynamicWirelessCharging);
        nbtTags.setUniqueId("connectionDataId", connectionDataId);
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
        nbtTags.setBoolean(NBT_DYNAMIC_WIRELESS_CHARGING, dynamicWirelessCharging);
        return nbtTags;
    }

    @Override
    public void setConfigurationData(NBTTagCompound nbtTags) {
        setDynamicWirelessCharging(nbtTags.getBoolean(NBT_DYNAMIC_WIRELESS_CHARGING));
    }

    @Override
    public String getDataType() {
        return CONFIG_CARD_DATA_TYPE;
    }

    @Override
    public void writeSustainedData(ItemStack itemStack) {
        ItemDataUtils.setBoolean(itemStack, NBT_DYNAMIC_WIRELESS_CHARGING, dynamicWirelessCharging);
    }

    @Override
    public void readSustainedData(ItemStack itemStack) {
        setDynamicWirelessCharging(ItemDataUtils.getBoolean(itemStack, NBT_DYNAMIC_WIRELESS_CHARGING));
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
        removeConnectionData();
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
        ensureConnectionsLoaded();
        if (scanInProgress && scanChanged) {
            flushConnectionData();
        }
        scanMachine = true;
        scanInProgress = false;
    }

    public int getScanMachineCount() {
        if (getWorld() != null && !getWorld().isRemote) {
            ensureConnectionsLoaded();
            return connections.size();
        }
        return clientConnectionCount;
    }

    @Override
    public List<ConnectionConfig> getPreviewConnections() {
        return getConnectionsSnapshot();
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
        if (tileEntity == null || player == null) {
            return ConnectStatus.CONNECT_FAIL;
        }
        TileEntity targetTile = ConnectionConfig.resolveMachineTile(tileEntity);
        if (!SecurityUtils.canAccess(player, this) || !SecurityUtils.canAccess(player, targetTile)) {
            if (!getWorldNN().isRemote) {
                SecurityUtils.displayNoAccess(player);
            }
            return ConnectStatus.CONNECT_FAIL;
        }
        ConnectStatus status = linkOrCut(tileEntity, facing, player);
        if (status != ConnectStatus.CONNECT_FAIL && !getWorldNN().isRemote) {
            if (status == ConnectStatus.DISCONNECT) {
                //发送成功断开链接的消息
                player.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + Mekanism.LOG_TAG + " " + EnumColor.INDIGO + LangUtils.localize("tooltip.connector.disconnect")));
            } else {
                //发送成功链接的消息
                player.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + Mekanism.LOG_TAG + " " + EnumColor.INDIGO + LangUtils.localize("tooltip.connector.to")));
            }
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
        ensureConnectionsLoaded();
        TileEntity machineTile = ConnectionConfig.resolveMachineTile(tileEntity);
        if (machineTile == tileEntity && tileEntity instanceof mekanism.common.tile.TileEntityBoundingBlock) {
            return ConnectStatus.CONNECT_FAIL;
        }
        Coord4D machineCoord = ConnectionConfig.resolveMachineCoord(tileEntity);
        // 已存在,移除连接
        for (int i = 0; i < connections.size(); i++) {
            if (connections.get(i).getMachineCoord().equals(machineCoord)) {
                //使用逻辑机器坐标移除，因此点击同一机器的任意能源端点都能断开。
                removeConnectionAt(i);
                connectionDataChanged();
                return ConnectStatus.DISCONNECT;
            }
        }
        {
            if (machineCoord.getPos().equals(getPos())) {
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
                Optional<LinkTileEntity> linkCapability;
                try {
                    linkCapability = MEKCeuMoreMachine.getLinkInfoCap(machineTile);
                    if (linkCapability.isPresent() && isClaimedByOtherStation(linkCapability.get(), machineTile)) {
                        if (!getWorldNN().isRemote) {
                            player.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + Mekanism.LOG_TAG + " " + EnumColor.RED + LangUtils.localize("tooltip.connector.fail")));
                        }
                        return ConnectStatus.CONNECT_FAIL;
                    }
                    if (!LinkUtils.isValidAcceptorOnTargetSideInput(tileEntity, facing)) {
                        if (!getWorldNN().isRemote) {
                            //无法连接到该方块，因为是不支持的能量系统或者没有能量接口
                            player.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + Mekanism.LOG_TAG + " " + EnumColor.RED + LangUtils.localize("tooltip.connector.fail")));
                        }
                        return ConnectStatus.CONNECT_FAIL;
                    }
                } catch (RuntimeException e) {
                    markMachineError(tileEntity, e);
                    if (!getWorldNN().isRemote) {
                        player.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + Mekanism.LOG_TAG + " " + EnumColor.RED + LangUtils.localize("tooltip.connector.fail")));
                    }
                    return ConnectStatus.CONNECT_FAIL;
                }
                ConnectionConfig config = new ConnectionConfig(tileEntity, facing);
                addConnectionGrouped(config);
                claimConnection(machineTile, linkCapability);
                connectionDataChanged();
                return ConnectStatus.CONNECT;

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
