package mekceumoremachine.common.tile.machine;

import io.netty.buffer.ByteBuf;
import mekanism.api.IConfigCardAccess;
import mekanism.api.TileNetworkList;
import mekanism.api.gas.*;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.Mekanism;
import mekanism.common.SideData;
import mekanism.common.Upgrade;
import mekanism.common.base.*;
import mekanism.common.block.states.BlockStateMachine.MachineType;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.config.MekanismConfig;
import mekanism.common.network.PacketTileEntity;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.prefab.TileEntityMachine;
import mekanism.common.util.*;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.interfaces.ITierMachine;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class TileEntityTierRotaryCondensentrator extends TileEntityMachine implements ISustainedData, IFluidHandlerWrapper, IGasHandler, Upgrade.IUpgradeInfoHandler, ITankManager,
        IComparatorSupport, ISideConfiguration, IConfigCardAccess.ISpecialConfigData, IMachineSlotTip, ITierMachine<MachineTier> {


    public static final int MAX_FLUID = 10000;
    public GasTank gasTank;
    public FluidTank fluidTank;

    /**
     * true: gas -> fluid; false: fluid -> gas
     */
    public boolean mode = true;

    public double clientEnergyUsed;

    public TileComponentEjector ejectorComponent;

    public TileComponentConfig configComponent;

    private int currentRedstoneLevel;

    public MachineTier tier = MachineTier.BASIC;

    public TileEntityTierRotaryCondensentrator() {
        super("machine.rotarycondensentrator", "TierRotaryCondensentrator", 0, MachineType.ROTARY_CONDENSENTRATOR.getUsage(), 5);
        gasTank = new GasTank(MAX_FLUID * tier.processes);
        fluidTank = new FluidTankSync(MAX_FLUID * tier.processes);

        configComponent = new TileComponentConfig(this, TransmissionType.ITEM, TransmissionType.ENERGY, TransmissionType.GAS, TransmissionType.FLUID);

        configComponent.addOutput(TransmissionType.ITEM, new SideData(DataType.NONE, InventoryUtils.EMPTY));
        configComponent.addOutput(TransmissionType.ITEM, new SideData(DataType.ENERGY, new int[]{4}));
        configComponent.addOutput(TransmissionType.ITEM, new SideData(DataType.FLUID, new int[]{2, 3}));
        configComponent.addOutput(TransmissionType.ITEM, new SideData(DataType.GAS, new int[]{0, 1}));
        configComponent.setConfig(TransmissionType.ITEM, new byte[]{2, 1, 2, 2, 3, 2});

        configComponent.addOutput(TransmissionType.FLUID, new SideData(DataType.NONE, InventoryUtils.EMPTY));
        configComponent.addOutput(TransmissionType.FLUID, new SideData(DataType.FLUID, new int[]{0}));
        configComponent.setConfig(TransmissionType.FLUID, new byte[]{0, 0, 0, 0, 0, 1});

        configComponent.addOutput(TransmissionType.GAS, new SideData(DataType.NONE, InventoryUtils.EMPTY));
        configComponent.addOutput(TransmissionType.GAS, new SideData(DataType.GAS, new int[]{1}));
        configComponent.setConfig(TransmissionType.GAS, new byte[]{0, 0, 0, 0, 1, 0});

        configComponent.setInputConfig(TransmissionType.ENERGY);

        inventory = NonNullListSynchronized.withSize(6, ItemStack.EMPTY);

        ejectorComponent = new TileComponentEjector(this);

        ejectorComponent.setOutputData(TransmissionType.GAS, configComponent.getOutputs(TransmissionType.GAS).get(1));
        ejectorComponent.setOutputData(TransmissionType.FLUID, configComponent.getOutputs(TransmissionType.FLUID).get(1));
    }

    @Override
    public void onAsyncUpdateServer() {
        super.onAsyncUpdateServer();
        ChargeUtils.discharge(4, this);
        if (!MekanismConfig.current().mekce.RotaryCondensentratorAuto.val()) {
            Autoeject(); //Used to deal with the problem that gas/fluid will automatically return to the tank when the ejection mode is on.
        }
        if (mode) {
            TileUtils.receiveGasItem(inventory.get(1), gasTank);
            if (FluidContainerUtils.isFluidContainer(inventory.get(2))) {
                FluidContainerUtils.handleContainerItemFill(this, fluidTank, 2, 3);
                if (fluidTank.getFluid() != null && fluidTank.getFluidAmount() == 0) {
                    fluidTank.setFluid(null);
                }
            }
            if (getEnergy() >= energyPerTick && MekanismUtils.canFunction(this) && isValidGas(gasTank.getGas()) &&
                    (fluidTank.getFluid() == null || (fluidTank.getFluidAmount() < fluidTank.getCapacity() && gasEquals(gasTank.getGas(), fluidTank.getFluid())))) {
                int operations = getUpgradedUsage();
                double prev = getEnergy();

                setActive(true);
                fluidTank.fill(new FluidStack(gasTank.stored.getGas().getFluid(), operations), true);
                gasTank.draw(operations, true);
                setEnergy(getEnergy() - energyPerTick * operations);
                clientEnergyUsed = prev - getEnergy();
            } else if (prevEnergy >= getEnergy()) {
                setActive(false);
            }
        } else {
            TileUtils.drawGas(inventory.get(0), gasTank);
            if (FluidContainerUtils.isFluidContainer(inventory.get(2)) && fluidTank.getFluidAmount() != fluidTank.getCapacity()) {
                FluidContainerUtils.handleContainerItemEmpty(this, fluidTank, 2, 3);
            }

            if (getEnergy() >= energyPerTick && MekanismUtils.canFunction(this) && isValidFluid(fluidTank.getFluid()) &&
                    (gasTank.getGas() == null || (gasTank.getStored() < gasTank.getMaxGas() && gasEquals(gasTank.getGas(), fluidTank.getFluid())))) {
                int operations = getUpgradedUsage();
                double prev = getEnergy();

                setActive(true);
                gasTank.receive(new GasStack(GasRegistry.getGas(fluidTank.getFluid().getFluid()), operations), true);
                fluidTank.drain(operations, true);
                setEnergy(getEnergy() - energyPerTick * operations);
                clientEnergyUsed = prev - getEnergy();
            } else if (prevEnergy >= getEnergy()) {
                setActive(false);
            }
        }
        prevEnergy = getEnergy();
        int newRedstoneLevel = getRedstoneLevel();
        if (newRedstoneLevel != currentRedstoneLevel) {
            updateComparatorOutputLevelSync();
            currentRedstoneLevel = newRedstoneLevel;

        }
    }

    public void Autoeject() {
        if (mode) {
            configComponent.setEjecting(TransmissionType.GAS, false);
            configComponent.setEjecting(TransmissionType.FLUID, true);
        } else {
            configComponent.setEjecting(TransmissionType.GAS, true);
            configComponent.setEjecting(TransmissionType.FLUID, false);
        }
    }

    public int getUpgradedUsage() {
        int possibleProcess = Math.min((int) Math.pow(2, upgradeComponent.getUpgrades(Upgrade.SPEED)), MekanismConfig.current().mekce.MAXspeedmachines.val());
        possibleProcess *= tier.processes;
        if (mode) { //Gas to fluid
            possibleProcess = Math.min(Math.min(gasTank.getStored(), fluidTank.getCapacity() - fluidTank.getFluidAmount()), possibleProcess);
        } else { //Fluid to gas
            possibleProcess = Math.min(Math.min(fluidTank.getFluidAmount(), gasTank.getNeeded()), possibleProcess);
        }
        possibleProcess = Math.min((int) (getEnergy() / energyPerTick), possibleProcess);
        possibleProcess = Math.max(possibleProcess, 1);
        return Math.min(mode ? gasTank.getStored() : fluidTank.getFluidAmount(), possibleProcess);
    }

    public boolean isValidGas(GasStack g) {
        return g != null && g.getGas().hasFluid();
    }

    public boolean gasEquals(GasStack gas, FluidStack fluid) {
        return fluid != null && gas != null && gas.getGas().hasFluid() && gas.getGas().getFluid() == fluid.getFluid();
    }

    public boolean isValidFluid(@Nonnull Fluid f) {
        return GasRegistry.getGas(f) != null;
    }

    public boolean isValidFluid(FluidStack f) {
        return f != null && isValidFluid(f.getFluid());
    }

    @Override
    public void handlePacketData(ByteBuf dataStream) {
        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            int type = dataStream.readInt();
            if (type == 0) {
                mode = !mode;
            }
            playersUsing.forEach(player -> Mekanism.packetHandler.sendTo(new PacketTileEntity.TileEntityMessage(this), (EntityPlayerMP) player));
            return;
        }

        super.handlePacketData(dataStream);
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            MachineTier prevTier = tier;
            tier = MachineTier.values()[dataStream.readInt()];
            fluidTank.setCapacity(MAX_FLUID * tier.processes);
            gasTank.setMaxGas(MAX_FLUID * tier.processes);
            mode = dataStream.readBoolean();
            clientEnergyUsed = dataStream.readDouble();
            TileUtils.readTankData(dataStream, fluidTank);
            TileUtils.readTankData(dataStream, gasTank);
            if (prevTier != tier) {
                MekanismUtils.updateBlock(world, getPos());
            }
        }
    }


    @Override
    public TileNetworkList getNetworkedData(TileNetworkList data) {
        super.getNetworkedData(data);
        data.add(tier.ordinal());
        data.add(mode);
        data.add(clientEnergyUsed);
        TileUtils.addTankData(data, fluidTank);
        TileUtils.addTankData(data, gasTank);
        return data;
    }

    @Override
    public void readCustomNBT(NBTTagCompound nbtTags) {
        super.readCustomNBT(nbtTags);
        tier = MachineTier.values()[nbtTags.getInteger("tier")];
        mode = nbtTags.getBoolean("mode");
        gasTank.read(nbtTags.getCompoundTag("gasTank"));
        if (nbtTags.hasKey("fluidTank")) {
            fluidTank.readFromNBT(nbtTags.getCompoundTag("fluidTank"));
        }
    }


    @Override
    public void writeCustomNBT(NBTTagCompound nbtTags) {
        super.writeCustomNBT(nbtTags);
        nbtTags.setInteger("tier", tier.ordinal());
        nbtTags.setBoolean("mode", mode);
        nbtTags.setTag("gasTank", gasTank.write(new NBTTagCompound()));
        nbtTags.setTag("fluidTank", fluidTank.writeToNBT(new NBTTagCompound()));
    }

    @Override
    public int receiveGas(EnumFacing side, GasStack stack, boolean doTransfer) {
        if (canReceiveGas(side, stack.getGas())) {
            return gasTank.receive(stack, doTransfer);
        }
        return 0;
    }

    @Override
    public GasStack drawGas(EnumFacing side, int amount, boolean doTransfer) {
        if (canDrawGas(side, null)) {
            return gasTank.draw(amount, doTransfer);
        }
        return null;
    }

    @Override
    public boolean canDrawGas(EnumFacing side, Gas type) {
        return !mode && configComponent.getOutput(TransmissionType.GAS, side, facing).hasSlot(1) && gasTank.canDraw(type);
    }

    @Override
    public boolean canReceiveGas(EnumFacing side, Gas type) {
        return mode && configComponent.getOutput(TransmissionType.GAS, side, facing).hasSlot(1) && gasTank.canReceive(type);
    }

    @Nonnull
    @Override
    public GasTankInfo[] getTankInfo() {
        return new GasTankInfo[]{gasTank};
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, EnumFacing side) {
        if (isCapabilityDisabled(capability, side)) {
            return false;
        }
        return capability == Capabilities.GAS_HANDLER_CAPABILITY || capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || capability == Capabilities.CONFIG_CARD_CAPABILITY || capability == Capabilities.SPECIAL_CONFIG_DATA_CAPABILITY || super.hasCapability(capability, side);
    }

    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, EnumFacing side) {
        if (isCapabilityDisabled(capability, side)) {
            return null;
        } else if (capability == Capabilities.GAS_HANDLER_CAPABILITY) {
            return Capabilities.GAS_HANDLER_CAPABILITY.cast(this);
        } else if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(new FluidHandlerWrapper(this, side));
        } else if (capability == Capabilities.CONFIG_CARD_CAPABILITY) {
            return Capabilities.CONFIG_CARD_CAPABILITY.cast(this);
        } else if (capability == Capabilities.SPECIAL_CONFIG_DATA_CAPABILITY) {
            return Capabilities.SPECIAL_CONFIG_DATA_CAPABILITY.cast(this);
        }
        return super.getCapability(capability, side);
    }

    @Override
    public boolean isCapabilityDisabled(@Nonnull Capability<?> capability, EnumFacing side) {
        return configComponent.isCapabilityDisabled(capability, side, facing) || super.isCapabilityDisabled(capability, side);
    }

    @Override
    public void writeSustainedData(ItemStack itemStack) {
        if (fluidTank.getFluid() != null) {
            ItemDataUtils.setCompound(itemStack, "fluidTank", fluidTank.getFluid().writeToNBT(new NBTTagCompound()));
        }
        if (gasTank.getGas() != null) {
            ItemDataUtils.setCompound(itemStack, "gasTank", gasTank.getGas().write(new NBTTagCompound()));
        }
    }

    @Override
    public void readSustainedData(ItemStack itemStack) {
        fluidTank.setFluid(FluidStack.loadFluidStackFromNBT(ItemDataUtils.getCompound(itemStack, "fluidTank")));
        gasTank.setGas(GasStack.readFromNBT(ItemDataUtils.getCompound(itemStack, "gasTank")));
    }

    @Override
    public int fill(EnumFacing from, @Nonnull FluidStack resource, boolean doFill) {
        return fluidTank.fill(resource, doFill);
    }

    @Override
    @Nullable
    public FluidStack drain(EnumFacing from, int maxDrain, boolean doDrain) {
        return fluidTank.drain(maxDrain, doDrain);
    }

    @Override
    public boolean canFill(EnumFacing from, @Nonnull FluidStack fluid) {
        return !mode && configComponent.getOutput(TransmissionType.FLUID, from, facing).hasSlot(0) && FluidContainerUtils.canFill(fluidTank.getFluid(), fluid);
    }

    @Override
    public boolean canDrain(EnumFacing from, @Nullable FluidStack fluid) {
        return mode && configComponent.getOutput(TransmissionType.FLUID, from, facing).hasSlot(0) && FluidContainerUtils.canDrain(fluidTank.getFluid(), fluid);
    }

    @Override
    public FluidTankInfo[] getTankInfo(EnumFacing from) {

        SideData data = configComponent.getOutput(TransmissionType.FLUID, from, facing);
        return data.getFluidTankInfo(this);
    }

    @Override
    public FluidTankInfo[] getAllTanks() {
        return new FluidTankInfo[]{fluidTank.getInfo()};
    }

    @Override
    public List<String> getInfo(Upgrade upgrade) {
        return upgrade == Upgrade.SPEED ? upgrade.getExpScaledInfo(this) : upgrade.getMultScaledInfo(this);
    }

    @Override
    public Object[] getTanks() {
        return new Object[]{fluidTank, gasTank};
    }

    @Nonnull
    @Override
    public int[] getSlotsForFace(@Nonnull EnumFacing side) {
        return configComponent.getOutput(TransmissionType.ITEM, side, facing).availableSlots;
    }

    @Override
    public boolean isItemValidForSlot(int slot, @Nonnull ItemStack stack) {
        if (slot == 0) {
            //Gas
            return stack.getItem() instanceof IGasItem;
        } else if (slot == 2) {
            //Fluid
            return FluidContainerUtils.isFluidContainer(stack);
        } else if (slot == 4) {
            return ChargeUtils.canBeDischarged(stack);
        }
        return false;
    }


    @Override
    public int getRedstoneLevel() {
        if (mode) {
            return MekanismUtils.redstoneLevelFromContents(gasTank.getStored(), gasTank.getMaxGas());
        }
        return MekanismUtils.redstoneLevelFromContents(fluidTank.getFluidAmount(), fluidTank.getCapacity());
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
    public TileComponentEjector getEjector() {
        return ejectorComponent;
    }

    @Override
    public boolean getEnergySlot() {
        return inventory.get(4).isEmpty();
    }

    @Override
    public boolean getInputSlot() {
        return false;
    }

    @Override
    public boolean getOuputSlot() {
        return false;
    }

    @Override
    public int getBlockGuiID(Block block, int metadata) {
        return 3;
    }

    @Override
    public IGuiProvider guiProvider() {
        return MEKCeuMoreMachine.proxy;
    }


    @Override
    public NBTTagCompound getConfigurationData(NBTTagCompound nbtTags) {
        nbtTags.setBoolean("mode", mode);
        return nbtTags;
    }

    @Override
    public void setConfigurationData(NBTTagCompound nbtTags) {
        mode = nbtTags.getBoolean("mode");
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
        gasTank.setMaxGas(tier.processes * MAX_FLUID);
        fluidTank.setCapacity(tier.processes * MAX_FLUID);
        upgradeComponent.getSupportedTypes().forEach(this::recalculateUpgradables);
        Mekanism.packetHandler.sendUpdatePacket(this);
        markNoUpdateSync();
        return true;
    }


    @Override
    public double getMaxEnergy() {
        return upgradeComponent.isUpgradeInstalled(Upgrade.ENERGY) ? MekanismUtils.getMaxEnergy(this, getTierEnergy()) : getTierEnergy();
    }


    public double getTierEnergy() {
        return MachineType.ROTARY_CONDENSENTRATOR.getStorage() * tier.processes;
    }


    @Nonnull
    @Override
    public String getName() {
        return LangUtils.localize("tile.TierRotaryCondensentrator." + tier.getBaseTier().getSimpleName() + ".name");
    }


    @Override
    public String getDataType() {
        return getName();
    }

    @Override
    public MachineTier getTier() {
        return tier;
    }

}
