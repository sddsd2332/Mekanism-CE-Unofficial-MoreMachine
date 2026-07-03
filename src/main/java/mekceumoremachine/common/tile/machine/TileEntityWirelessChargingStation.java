package mekceumoremachine.common.tile.machine;

import baubles.api.BaublesApi;
import com.google.common.base.Predicate;
import io.netty.buffer.ByteBuf;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.Coord4D;
import mekanism.api.IConfigCardAccess.ISpecialConfigData;
import mekanism.api.IContentsListener;
import mekanism.api.TileNetworkList;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.Mekanism;
import mekanism.common.base.*;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.entity.EntityRobit;
import mekanism.common.integration.energy.EnergyCompatUtils;
import mekanism.common.integration.MekanismHooks;
import mekanism.common.integration.computer.IComputerIntegration;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.security.ISecurityTile;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.TileComponentSecurity;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.prefab.TileEntityElectricBlock;
import mekanism.common.util.*;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.config.MoreMachineConfig;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.interfaces.ITierMachine;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class TileEntityWirelessChargingStation extends TileEntityElectricBlock implements IComputerIntegration, IRedstoneControl, ISideConfiguration, ISecurityTile,
        ISpecialConfigData, IComparatorSupport, IBoundingBlock, ITierMachine<MachineTier>, IHasVisualization, ISpecialSelectionWireframeTile {

    private static final Predicate<EntityLivingBase> CHARGE_PREDICATE = entity -> entity.isEntityAlive() && !entity.isDead && ((entity instanceof EntityPlayer player && !player.isSpectator()) || entity instanceof EntityRobit);

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
    public boolean clientRendering = false;
    private EnergyInventorySlot chargeSlot;
    private EnergyInventorySlot dischargeSlot;

    public TileEntityWirelessChargingStation() {
        super("WirelessChargingStation", 0);
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
        chargeSlot = builder.addSlot(EnergyInventorySlot.drain(getMainEnergyContainer(listener), listener, 26, 56 + 2));
        chargeSlot.setSlotOverlay(SlotOverlay.PLUS);
        dischargeSlot = builder.addSlot(EnergyInventorySlot.fillOrConvert(getMainEnergyContainer(listener), this::getWorld, listener, 26, 14 + 2));
        dischargeSlot.setSlotOverlay(SlotOverlay.MINUS);
        return builder.build();
    }

    @Override
    public void onUpdateServer() {
        super.onUpdateServer();
        chargeSlot.drainContainer();
        dischargeSlot.fillContainerOrConvert();
        List<EntityLivingBase> entities = world.getEntitiesWithinAABB(EntityLivingBase.class, getChargeBox(), CHARGE_PREDICATE);
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
                        provideEnergy(robit, 1_000);
                    } else if (entity instanceof EntityPlayer player) {
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
                                if (getEnergy() <= 0) {
                                    break;
                                }
                                provideEnergy(EnergyCompatUtils.getStrictEnergyHandler(stack), getMaxOutput());
                            }
                        }
                    }
                }
            }
        }
        int newScale = getScaledEnergyLevel(20);
        if (newScale != prevScale) {
            Mekanism.packetHandler.sendUpdatePacket(this);
        }
        prevScale = newScale;
    }

    private boolean provideEnergy(EntityRobit robit, double maxTransfer) {
        double energyToGive = Math.min(Math.min(getEnergy(), maxTransfer), robit.MAX_ELECTRICITY);
        double simulatedRemainder = robit.insert(energyToGive, Action.SIMULATE, AutomationType.INTERNAL);
        if (simulatedRemainder < energyToGive) {
            double extracted = getMainEnergyContainer().extract(energyToGive - simulatedRemainder, Action.EXECUTE, AutomationType.INTERNAL);
            if (extracted > 0) {
                double remainder = robit.insert(extracted, Action.EXECUTE, AutomationType.INTERNAL);
                if (remainder > 0) {
                    getMainEnergyContainer().insert(remainder, Action.EXECUTE, AutomationType.INTERNAL);
                }
                return remainder < extracted;
            }
        }
        return false;
    }

    private boolean provideEnergy(IStrictEnergyHandler energyHandler, double maxTransfer) {
        if (energyHandler == null) {
            return false;
        }
        double energyToGive = Math.min(getEnergy(), maxTransfer);
        double simulatedRemainder = energyHandler.insertEnergy(energyToGive, Action.SIMULATE);
        if (simulatedRemainder < energyToGive) {
            double extracted = getMainEnergyContainer().extract(energyToGive - simulatedRemainder, Action.EXECUTE, AutomationType.INTERNAL);
            if (extracted > 0) {
                double remainder = energyHandler.insertEnergy(extracted, Action.EXECUTE);
                if (remainder > 0) {
                    getMainEnergyContainer().insert(remainder, Action.EXECUTE, AutomationType.INTERNAL);
                }
                return remainder < extracted;
            }
        }
        return false;
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
        return LangUtils.localize("tile.WirelessChargingStation." + tier.getBaseTier().getSimpleName() + ".name");
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
                chargeRobit = !chargeRobit;
            } else if (type == 1) {
                playerArmor = !playerArmor;
            } else if (type == 2) {
                playerInventory = !playerInventory;
            }
        }

        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            MachineTier prevTier = tier;
            tier = MachineTier.byIndex(dataStream.readInt());
            controlType = MekanismUtils.getByIndex(RedstoneControl.values(), dataStream.readInt(), controlType);
            chargeRobit = dataStream.readBoolean();
            playerArmor = dataStream.readBoolean();
            playerInventory = dataStream.readBoolean();
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
        return data;
    }


    @Override
    public void readCustomNBT(NBTTagCompound nbtTags) {
        super.readCustomNBT(nbtTags);
        tier = MachineTier.byIndex(nbtTags.getInteger("tier"));
        controlType = MekanismUtils.getByIndex(RedstoneControl.values(), nbtTags.getInteger("controlType"), controlType);
        chargeRobit = nbtTags.getBoolean("chargeRobit");
        playerArmor = nbtTags.getBoolean("playerArmor");
        playerInventory = nbtTags.getBoolean("playerInventory");
    }

    @Override
    public void writeCustomNBT(NBTTagCompound nbtTags) {
        super.writeCustomNBT(nbtTags);
        nbtTags.setInteger("tier", tier.ordinal());
        nbtTags.setInteger("controlType", controlType.ordinal());
        nbtTags.setBoolean("chargeRobit", chargeRobit);
        nbtTags.setBoolean("playerArmor", playerArmor);
        nbtTags.setBoolean("playerInventory", playerInventory);
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
        return nbtTags;
    }

    @Override
    public void setConfigurationData(NBTTagCompound nbtTags) {
        chargeRobit = nbtTags.getBoolean("chargeRobit");
        playerArmor = nbtTags.getBoolean("playerArmor");
        playerInventory = nbtTags.getBoolean("playerInventory");
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
        int radius = getRang();
        return new AxisAlignedBB(
                getPos().getX() - radius,
                getPos().getY() - radius,
                getPos().getZ() - radius,
                getPos().getX() + radius + 1,
                getPos().getY() + radius + 1,
                getPos().getZ() + radius + 1
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

    private AxisAlignedBB getChargeBox() {
        int range = getRang();
        return new AxisAlignedBB(
                getPos().getX() - range,
                getPos().getY() + 2 - range,
                getPos().getZ() - range,
                getPos().getX() + range,
                getPos().getY() + 2 + range,
                getPos().getZ() + range
        );
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Class<?> getSelectionWireframeModelClass() {
        return mekceumoremachine.client.model.machine.ModelWirelessChargingStation.class;
    }
}
