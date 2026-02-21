package mekceumoremachine.common.tile.machine;

import io.netty.buffer.ByteBuf;
import mekanism.api.Coord4D;
import mekanism.api.EnumColor;
import mekanism.api.IConfigCardAccess.ISpecialConfigData;
import mekanism.api.TileNetworkList;
import mekanism.api.transmitters.ITransmitter;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.Mekanism;
import mekanism.common.SideData;
import mekanism.common.base.*;
import mekanism.common.base.target.EnergyAcceptorTarget;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.integration.computer.IComputerIntegration;
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
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class TileEntityWirelessChargingEnergy extends TileEntityElectricBlock implements IComputerIntegration, IRedstoneControl, ISideConfiguration, ISecurityTile,
        ISpecialConfigData, IComparatorSupport, IBoundingBlock, ITierMachine<MachineTier>, INoWirelessChargingEnergy, IHasVisualization, ITileConnect {


    public MachineTier tier = MachineTier.BASIC;

    public int currentRedstoneLevel;

    public RedstoneControl controlType;
    public int prevScale;
    public TileComponentEjector ejectorComponent;
    public TileComponentConfig configComponent;
    public TileComponentSecurity securityComponent;
    private List<ConnectionConfig> skipMachine = new ArrayList<>();
    public List<ConnectionConfig> connections = new ArrayList<>();

    public boolean enableEmit;
    public boolean clientRendering = false;
    public boolean scanMachine;

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
        if (scanMachine) {
            scan();
        }
        //每2.5s验证下链接内是否有效
        if (!connections.isEmpty() && ticksExisted % 50 == 0) {
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

    //扫描机器
    public void scan() {
        //通知机器断开链接了
        connections.forEach(connection -> {
            TileEntity tile = getWorldNN().getTileEntity(connection.getPos());
            if (tile != null) {
                MEKCeuMoreMachine.getLinkInfoCap(tile).ifPresent(LinkTileEntity::stopLink);
            }
        });
        //全部清空下
        connections.clear();
        //添加所有的扫描
        connections.addAll(getRangMachine());
        scanMachine = false;
    }


    public void isValidLinks() {
        //验证已经添加的数量
        int size = connections.size() - 1;
        //如果超出上限
        if (size >= getMaxLinks()) {
            //清除后面的链接
            for (int i = size; i > getMaxLinks(); i--) {
                TileEntity tile = getWorldNN().getTileEntity(connections.get(i).getPos());
                if (tile != null) {
                    //通知移除
                    MEKCeuMoreMachine.getLinkInfoCap(tile).ifPresent(LinkTileEntity::stopLink);
                }
                connections.remove(i);
            }
        }
        connections.removeIf(config -> !getWorldNN().isBlockLoaded(config.getPos()) || getWorldNN().isAirBlock(config.getPos()));
        Mekanism.packetHandler.sendUpdatePacket(this);
    }

    //充能机器
    public void emitMachine() {
        if (getWorld().isRemote || !MekanismUtils.canFunction(this) || connections.isEmpty()) {
            return;
        }
        double energyToSend = Math.min(getEnergy(), getMaxOutput());
        for (ConnectionConfig machine : connections) {
            if (getEnergy() <= 0) {
                break;
            }
            TileEntity tile = getWorldNN().getTileEntity(machine.getPos());
            //如果没有机器，则立刻删除并跳过本轮循环
            if (tile == null) {
                connections.remove(machine);
                Mekanism.packetHandler.sendUpdatePacket(this);
                continue;
            }

            EnumFacing opposite = machine.getFacing();
            try {
                EnergyAcceptorTarget target = new EnergyAcceptorTarget();
                EnergyAcceptorWrapper acceptor = EnergyAcceptorWrapper.get(tile, opposite);
                if (acceptor != null && acceptor.canReceiveEnergy(opposite) && acceptor.needsEnergy(opposite)) {
                    target.addHandler(opposite, acceptor);
                }
                int curHandlers = target.getHandlers().size();
                if (curHandlers > 0) {
                    double sent = EmitUtils.sendToAcceptors(Collections.singleton(target), curHandlers, energyToSend);
                    setEnergy(getEnergy() - sent);
                }
            } catch (Exception e) {
                Mekanism.logger.error("Wireless power station error occurred");
                Mekanism.logger.error("A machine that cannot be charged,pos: x {} y{} z {}", tile.getPos().getX(), tile.getPos().getY(), tile.getPos().getZ());
                Mekanism.logger.error("Will no longer charge this machine");
                skipMachine.add(machine);
                connections.remove(machine);
                Mekanism.packetHandler.sendUpdatePacket(this);
            }
        }
    }


    @Override
    public void invalidate() {
        //通知链接机器断开链接
        connections.forEach(connection -> {
            TileEntity tile = getWorldNN().getTileEntity(connection.getPos());
            if (tile != null) {
                MEKCeuMoreMachine.getLinkInfoCap(tile).ifPresent(LinkTileEntity::stopLink);
            }
        });
        super.invalidate();
    }


    //获取范围内的机器
    public List<ConnectionConfig> getRangMachine() {
        World world = getWorld();
        BlockPos currentPos = getPos().up(2);
        ChunkPos currentChunk = new ChunkPos(currentPos);
        List<ConnectionConfig> rangMachine = new ArrayList<>();
        int rang = tier.processes;
        Coord4D other = null;
        for (int chunkX = currentChunk.x - rang; chunkX <= currentChunk.x + rang; chunkX++) {
            for (int chunkZ = currentChunk.z - rang; chunkZ <= currentChunk.z + rang; chunkZ++) {
                Chunk chunk = world.getChunkProvider().getLoadedChunk(chunkX, chunkZ);
                if (chunk == null || chunk.isEmpty()) {
                    continue;
                }
                for (TileEntity tileEntity : chunk.getTileEntityMap().values()) {
                    //如果链接大于等于最大上限，结束链接
                    if (rangMachine.size() >= getMaxLinks()) {
                        break;
                    }
                    //跳过含有不能无线充能的机器
                    if (tileEntity instanceof INoWirelessChargingEnergy) {
                        continue;
                    }
                    //跳过黑名单匹配的机器
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
                    if (skipMachine.stream().anyMatch(tile -> tile.getPos().equals(tilePos))) {
                        continue;
                    }
                    //扫描链接的机器
                    Optional<LinkTileEntity> linkCapability = MEKCeuMoreMachine.getLinkInfoCap(tileEntity);
                    //如果机器是已经扫描了，且已经链接
                    if (linkCapability.isPresent()) {
                        //检查链接的供能站
                        Coord4D coord4D = linkCapability.get().isLink();
                        //如果临时缓存的匹配是不一样的
                        if (coord4D != null && !coord4D.equals(other)) {
                            BlockPos blockPos = coord4D.getPos();
                            //去看看机器有没有效
                            if (getWorldNN().getTileEntity(blockPos) == null) {
                                //无效通知方块清除链接
                                linkCapability.get().stopLink();
                                //否则临时添加下匹配缓存，并跳过该方块
                            } else {
                                other = coord4D;
                                continue;
                            }
                        }
                    }
                    double distanceSquared = currentPos.distanceSq(tilePos);
                    if (distanceSquared <= getRang() * getRang()) {
                        for (EnumFacing side : EnumFacing.VALUES) {
                            if (LinkUtils.isValidAcceptorOnSideInput(tileEntity, side)) {
                                rangMachine.add(new ConnectionConfig(tileEntity, side.getOpposite()));
                                MEKCeuMoreMachine.getLinkInfoCap(tileEntity).ifPresent(linkInfo -> linkInfo.setLink(tileEntity));
                                break;
                            }
                        }
                    }
                }
            }
        }
        return rangMachine;
    }





    //自动清除错误方块；
    public void autoClearErrorMachine() {
        if (MoreMachineConfig.current().config.enableAutoClearErrorMachine.val() && ticker % (MoreMachineConfig.current().config.AutoClearErrorMachineSecond.val() * 20) == 0) {
            for (ConnectionConfig machine : skipMachine) {
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
            case BASIC -> MoreMachineConfig.current().config.BasicWirelessChargingOutput.val();
            case ADVANCED -> MoreMachineConfig.current().config.AdvancedWirelessChargingOutput.val();
            case ELITE -> MoreMachineConfig.current().config.EliteWirelessChargingOutput.val();
            case ULTIMATE -> MoreMachineConfig.current().config.UltimateWirelessChargingOutput.val();
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
            case BASIC -> MoreMachineConfig.current().config.BasicWirelessChargingMaxEnergy.val();
            case ADVANCED -> MoreMachineConfig.current().config.AdvancedWirelessChargingMaxEnergy.val();
            case ELITE -> MoreMachineConfig.current().config.EliteWirelessChargingMAXEnergy.val();
            case ULTIMATE -> MoreMachineConfig.current().config.UltimateWirelessChargingMaxEnergy.val();
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
                enableEmit = !enableEmit;
            } else if (type == 1) {
                setScanMachine();
            }
        }

        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            MachineTier prevTier = tier;
            tier = MachineTier.values()[dataStream.readInt()];
            controlType = RedstoneControl.values()[dataStream.readInt()];
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
        tier = MachineTier.values()[nbtTags.getInteger("tier")];
        controlType = RedstoneControl.values()[nbtTags.getInteger("controlType")];
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

    //设置扫描机器
    public void setScanMachine() {
        scanMachine = true;
    }

    public int getScanMachineCount() {
        return connections.size();
    }

    public List<ConnectionConfig> getAllConnections() {
        return connections;
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
                MEKCeuMoreMachine.getLinkInfoCap(tileEntity).ifPresent(linkInfo -> linkInfo.setLink(tileEntity));
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
            if (tileEntity.getPos() == getPos()) {
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
            double distanceSquared = currentPos.distanceSq(tileEntity.getPos());
            if (distanceSquared <= getRang() * getRang()) {
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



}
