package mekceumoremachine.common.tile.machine;

import baubles.api.BaublesApi;
import com.google.common.base.Predicate;
import io.netty.buffer.ByteBuf;
import mekanism.api.Coord4D;
import mekanism.api.IConfigCardAccess.ISpecialConfigData;
import mekanism.api.TileNetworkList;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.Mekanism;
import mekanism.common.SideData;
import mekanism.common.base.*;
import mekanism.common.base.target.EnergyAcceptorTarget;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.entity.EntityRobit;
import mekanism.common.integration.MekanismHooks;
import mekanism.common.integration.computer.IComputerIntegration;
import mekanism.common.security.ISecurityTile;
import mekanism.common.tier.BaseTier;
import mekanism.common.tier.InductionCellTier;
import mekanism.common.tier.InductionProviderTier;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.TileComponentSecurity;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.prefab.TileEntityElectricBlock;
import mekanism.common.util.*;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.interfaces.ITierMachine;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
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
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TileEntityWirelessChargingStation extends TileEntityElectricBlock implements IComputerIntegration, IRedstoneControl, ISideConfiguration, ISecurityTile,
        ISpecialConfigData, IComparatorSupport, IBoundingBlock, ITierMachine<MachineTier> {

    private static final Predicate<EntityLivingBase> CHARGE_PREDICATE = entity -> (entity instanceof EntityPlayer player && !player.isSpectator()) || entity instanceof EntityRobit;

    public MachineTier tier = MachineTier.BASIC;

    public int currentRedstoneLevel;

    public RedstoneControl controlType;
    public int prevScale;
    public TileComponentEjector ejectorComponent;
    public TileComponentConfig configComponent;
    public TileComponentSecurity securityComponent;

    public boolean chargeRobit;
    public boolean playerArmor;
    public boolean playerInventory;
    public boolean chargeMachine;


    public TileEntityWirelessChargingStation() {
        super("WirelessChargingStation", 0);
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
    public void onUpdateServer() {
        super.onUpdateServer();
        ChargeUtils.charge(0, this);
        ChargeUtils.discharge(1, this);


        List<EntityLivingBase> entities = world.getEntitiesWithinAABB(EntityLivingBase.class, new AxisAlignedBB(getPos().getX() - getRang(), getPos().getY() + 4 - getRang(), getPos().getZ() - getRang(), getPos().getX() + getRang(), getPos().getY() + 4 + getRang(), getPos().getZ() + getRang()), CHARGE_PREDICATE);

        if (MekanismUtils.canFunction(this)) {
            List<EntityLivingBase> addeEtities = new ArrayList<>();
            //如果机器的安全选项不是公开的，只充能该机器对应的所有者
            if (getSecurity().getMode() != SecurityMode.PUBLIC) {
                for (EntityLivingBase entity : entities) {
                    if (entity instanceof EntityRobit robit && robit.getOwnerUUID().equals(getSecurity().getOwnerUUID())) {
                        addeEtities.add(entity);

                    }
                    if (entity instanceof EntityPlayer player && player.getUniqueID().equals(getSecurity().getOwnerUUID())) {
                        addeEtities.add(entity);

                    }
                }
            } else {
                addeEtities.addAll(entities);
            }

            if (!addeEtities.isEmpty()) {
                for (EntityLivingBase entity : addeEtities) {
                    if (chargeRobit && entity instanceof EntityRobit robit) {
                        double canGive = Math.min(getEnergy(), 1000);
                        double toGive = Math.min(robit.MAX_ELECTRICITY - robit.getEnergy(), canGive);
                        robit.setEnergy(robit.getEnergy() + toGive);
                        setEnergy(getEnergy() - toGive);
                    } else if (entity instanceof EntityPlayer player) {
                        double prevEnergy = getEnergy();
                        List<ItemStack> stacks = new ArrayList<>();
                        if (playerArmor) {
                            stacks.addAll(player.inventory.armorInventory);
                        }
                        if (playerInventory) {
                            stacks.addAll(player.inventory.offHandInventory);
                            stacks.addAll(player.inventory.mainInventory);
                            if (Mekanism.hooks.Baubles) {
                                stacks.addAll(chargeBaublesInventory(player));
                            }
                        }
                        if (!stacks.isEmpty()) {
                            for (ItemStack stack : stacks) {
                                ChargeUtils.charge(stack, this);
                                if (prevEnergy != getEnergy()) {
                                    break;
                                }
                            }
                        }
                    }
                }
            }


            if (chargeMachine) {
                emit();
            }
        }
        int newScale = getScaledEnergyLevel(20);
        if (newScale != prevScale) {
            Mekanism.packetHandler.sendUpdatePacket(this);
        }
        prevScale = newScale;

    }

    public void emit() {
        TileEntity tileEntity = this;
        if (tileEntity.getWorld().isRemote || !MekanismUtils.canFunction(tileEntity)) {
            return;
        }
        if (getRangMachine() != null && !getRangMachine().isEmpty()) {
            for (int i = 0; i < getRangMachine().size(); i++) {
                double energyToSend = Math.min(getEnergy(), getMaxOutput());
                if (!(energyToSend > 0)) {
                    break;
                }
                EnergyAcceptorTarget target = new EnergyAcceptorTarget();
                for (EnumFacing side : EnumFacing.VALUES) {
                    for (TileEntity tile : getRangMachine().values()) {
                        if (CableUtils.isAcceptor(tileEntity, tile, side)) {
                            EnumFacing opposite = side.getOpposite();
                            EnergyAcceptorWrapper acceptor = EnergyAcceptorWrapper.get(tile, opposite);
                            if (acceptor != null && acceptor.canReceiveEnergy(opposite) && acceptor.needsEnergy(opposite)) {
                                target.addHandler(opposite, acceptor);
                            }
                        }
                    }
                }
                int curHandlers = target.getHandlers().size();
                if (curHandlers > 0) {
                    double sent = EmitUtils.sendToAcceptors(java.util.Collections.singleton(target), curHandlers, energyToSend);
                    setEnergy(getEnergy() - sent);
                }
            }
        }
    }

    @Optional.Method(modid = MekanismHooks.Baubles_MOD_ID)
    public List<ItemStack> chargeBaublesInventory(EntityPlayer player) {
        IItemHandler baubles = BaublesApi.getBaublesHandler(player);
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < baubles.getSlots(); i++) {
            stacks.add(baubles.getStackInSlot(i));
        }
        return stacks;
    }

    public Map<BlockPos, TileEntity> getRangMachine() {
        if (!chargeMachine) {
            return new HashMap<>();
        }
        World world = getWorld();
        BlockPos currentPos = getPos().up(4);
        ChunkPos currentChunk = new ChunkPos(currentPos);
        Map<BlockPos, TileEntity> rangMachine = new HashMap<>();
        int rang = tier.processes;
        for (int chunkX = currentChunk.x - rang; chunkX <= currentChunk.x + rang; chunkX++) {
            for (int chunkZ = currentChunk.z - rang; chunkZ <= currentChunk.z + rang; chunkZ++) {
                Chunk chunk = world.getChunkProvider().getLoadedChunk(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                Map<BlockPos, TileEntity> tileEntityMap = chunk.getTileEntityMap();

                for (TileEntity tileEntity : tileEntityMap.values()) {
                    if (tileEntity instanceof TileEntityWirelessChargingStation) {
                        continue;
                    }
                    BlockPos tilePos = tileEntity.getPos();
                    double distanceSquared = currentPos.distanceSq(tilePos);
                    if (distanceSquared <= getRang() * getRang()) {
                        for (EnumFacing side : EnumFacing.values()) {
                            if (CableUtils.isAcceptor(this, tileEntity, side)) {
                                rangMachine.put(tilePos, tileEntity);
                            }
                        }
                    }
                }
            }
        }
        return rangMachine;
    }


    @Override
    public boolean supportsAsync() {
        return false;
    }


    @Override
    public boolean upgrade(BaseTier upgradeTier) {
        if (upgradeTier.ordinal() != tier.ordinal() + 1) {
            return false;
        }
        if (upgradeTier == BaseTier.CREATIVE){
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
        return LangUtils.localize("tile.WirelessChargingStation." + tier.getBaseTier().getSimpleName() + ".name");
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
                chargeRobit = !chargeRobit;
            } else if (type == 1) {
                playerArmor = !playerArmor;
            } else if (type == 2) {
                playerInventory = !playerInventory;
            } else if (type == 3) {
                chargeMachine = !chargeMachine;
            }
        }

        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            MachineTier prevTier = tier;
            tier = MachineTier.values()[dataStream.readInt()];
            controlType = RedstoneControl.values()[dataStream.readInt()];
            chargeRobit = dataStream.readBoolean();
            playerArmor = dataStream.readBoolean();
            playerInventory = dataStream.readBoolean();
            chargeMachine = dataStream.readBoolean();
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
        data.add(chargeRobit);
        data.add(playerArmor);
        data.add(playerInventory);
        data.add(chargeMachine);
        return data;
    }


    @Override
    public void readCustomNBT(NBTTagCompound nbtTags) {
        super.readCustomNBT(nbtTags);
        tier = MachineTier.values()[nbtTags.getInteger("tier")];
        controlType = RedstoneControl.values()[nbtTags.getInteger("controlType")];
        chargeRobit = nbtTags.getBoolean("chargeRobit");
        playerArmor = nbtTags.getBoolean("playerArmor");
        playerInventory = nbtTags.getBoolean("playerInventory");
        chargeMachine = nbtTags.getBoolean("chargeMachine");
    }

    @Override
    public void writeCustomNBT(NBTTagCompound nbtTags) {
        super.writeCustomNBT(nbtTags);
        nbtTags.setInteger("tier", tier.ordinal());
        nbtTags.setInteger("controlType", controlType.ordinal());
        nbtTags.setBoolean("chargeRobit", chargeRobit);
        nbtTags.setBoolean("playerArmor", playerArmor);
        nbtTags.setBoolean("playerInventory", playerInventory);
        nbtTags.setBoolean("chargeMachine", chargeMachine);
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
        nbtTags.setBoolean("chargeRobit", chargeRobit);
        nbtTags.setBoolean("playerArmor", playerArmor);
        nbtTags.setBoolean("playerInventory", playerInventory);
        nbtTags.setBoolean("chargeMachine", chargeMachine);
        return nbtTags;
    }

    @Override
    public void setConfigurationData(NBTTagCompound nbtTags) {
        chargeRobit = nbtTags.getBoolean("chargeRobit");
        playerArmor = nbtTags.getBoolean("playerArmor");
        playerInventory = nbtTags.getBoolean("playerInventory");
        chargeMachine = nbtTags.getBoolean("chargeMachine");
    }

    @Override
    public String getDataType() {
        return getName();
    }


    @Override
    public int getBlockGuiID(Block block, int i) {
        return 0;
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
        MekanismUtils.makeBoundingBlock(world, getPos().up(3), current);
        MekanismUtils.makeBoundingBlock(world, getPos().up(4), current);
    }

    @Override
    public void onBreak() {
        world.setBlockToAir(getPos().up(1));
        world.setBlockToAir(getPos().up(2));
        world.setBlockToAir(getPos().up(3));
        world.setBlockToAir(getPos().up(4));
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
}
