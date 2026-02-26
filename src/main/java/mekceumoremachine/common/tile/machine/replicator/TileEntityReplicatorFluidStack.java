package mekceumoremachine.common.tile.machine.replicator;

import io.netty.buffer.ByteBuf;
import mekanism.api.TileNetworkList;
import mekanism.api.gas.*;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.SideData;
import mekanism.common.Upgrade;
import mekanism.common.base.*;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.inputs.GasAndFluidInput;
import mekanism.common.recipe.machines.ReplicatorFluidStackRecipe;
import mekanism.common.recipe.outputs.FluidOutput;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.prefab.TileEntityBasicMachine;
import mekanism.common.util.*;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.config.MoreMachineConfig;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public class TileEntityReplicatorFluidStack extends TileEntityBasicMachine<GasAndFluidInput, FluidOutput, ReplicatorFluidStackRecipe> implements IGasHandler, IFluidHandlerWrapper, ISustainedData, ITankManager {

    public static final int MAX_GAS_OR_FLUID = 10000;
    public FluidTank inputTank = new FluidTankSync(MAX_GAS_OR_FLUID);
    public GasTank uuTank = new GasTank(MAX_GAS_OR_FLUID);
    public FluidTank outputTank = new FluidTankSync(MAX_GAS_OR_FLUID);


    public TileEntityReplicatorFluidStack() {
        super("prc", "ReplicatorFluidStack", MoreMachineConfig.current().config.ReplicatorFluidStackEnergyStorge.val(), MoreMachineConfig.current().config.ReplicatorFluidStackEnergyUsage.val(), 1, 200);

        configComponent = new TileComponentConfig(this, TransmissionType.ENERGY, TransmissionType.FLUID, TransmissionType.GAS);

        configComponent.setInputConfig(TransmissionType.GAS);

        configComponent.addOutput(TransmissionType.FLUID, new SideData(DataType.NONE, InventoryUtils.EMPTY));
        configComponent.addOutput(TransmissionType.FLUID, new SideData(DataType.INPUT, new int[]{0}));
        configComponent.addOutput(TransmissionType.FLUID, new SideData(DataType.OUTPUT, new int[]{1}));
        configComponent.addOutput(TransmissionType.FLUID, new SideData(DataType.INPUT, new int[]{0}));
        configComponent.addOutput(TransmissionType.FLUID, new SideData(new int[]{0, 1}, new boolean[]{false, true}));
        configComponent.setConfig(TransmissionType.FLUID, new byte[]{0, 0, 2, 0, 1, 0});

        configComponent.setInputConfig(TransmissionType.ENERGY);

        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(TransmissionType.FLUID, configComponent.getOutputs(TransmissionType.FLUID).get(2));
        ejectorComponent.setInputOutputData(TransmissionType.FLUID, configComponent.getOutputs(TransmissionType.FLUID).get(4));

        inventory = NonNullListSynchronized.withSize(2, ItemStack.EMPTY);
    }

    @Override
    public void onAsyncUpdateServer() {
        super.onAsyncUpdateServer();
        ChargeUtils.discharge(0, this);
        ReplicatorFluidStackRecipe recipe = getRecipe();
        double energy = recipe != null ? MekanismUtils.getEnergyPerTick(this, BASE_ENERGY_PER_TICK + recipe.extraEnergy) : 0;
        getProcess(recipe, true, energy);
        prevEnergy = getEnergy();
    }

    @Override
    protected void setNoFinish() {
        BASE_TICKS_REQUIRED = 200;
    }

    @Override
    protected void setupVariableValues() {
        if (getRecipe() == null) {
            return;
        }
        boolean update = BASE_TICKS_REQUIRED != getRecipe().ticks;
        BASE_TICKS_REQUIRED = getRecipe().ticks;
        if (update) {
            recalculateUpgradables(Upgrade.SPEED);
        }
    }

    @Override
    public boolean isItemValidForSlot(int slotID, @Nonnull ItemStack itemstack) {
        return slotID == 0 && ChargeUtils.canBeDischarged(itemstack);
    }


    @Override
    public ReplicatorFluidStackRecipe getRecipe() {
        GasAndFluidInput input = getInput();
        if (cachedRecipe == null || !input.testEquality(cachedRecipe.getInput())) {
            cachedRecipe = RecipeHandler.getReplicatorFluidStackRecipe(input);
        }
        return cachedRecipe;
    }

    @Override
    public GasAndFluidInput getInput() {
        return new GasAndFluidInput(uuTank.getGas(), inputTank.getFluid());
    }

    @Override
    public void operate(ReplicatorFluidStackRecipe recipe) {
        recipe.operate(uuTank, inputTank, outputTank, 1);
        markNoUpdateSync();
    }

    @Override
    public boolean canOperate(ReplicatorFluidStackRecipe recipe) {
        return recipe != null && recipe.canOperate(uuTank, inputTank, outputTank);
    }

    @Override
    public boolean canExtractItem(int slotID, @Nonnull ItemStack itemstack, @Nonnull EnumFacing side) {
        return slotID == 0 && ChargeUtils.canBeOutputted(itemstack, false);
    }

    @Override
    public TileNetworkList getNetworkedData(TileNetworkList data) {
        super.getNetworkedData(data);
        TileUtils.addTankData(data, inputTank);
        TileUtils.addTankData(data, uuTank);
        TileUtils.addTankData(data, outputTank);
        return data;
    }

    @Override
    public void handlePacketData(ByteBuf dataStream) {
        super.handlePacketData(dataStream);
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            TileUtils.readTankData(dataStream, inputTank);
            TileUtils.readTankData(dataStream, uuTank);
            TileUtils.readTankData(dataStream, outputTank);
        }
    }

    @Override
    public void readCustomNBT(NBTTagCompound nbtTags) {
        super.readCustomNBT(nbtTags);
        inputTank.readFromNBT(nbtTags.getCompoundTag("inputTank"));
        uuTank.read(nbtTags.getCompoundTag("uuTank"));
        outputTank.readFromNBT(nbtTags.getCompoundTag("outputTank"));
    }

    @Override
    public void writeCustomNBT(NBTTagCompound nbtTags) {
        super.writeCustomNBT(nbtTags);
        nbtTags.setTag("inputTank", inputTank.writeToNBT(new NBTTagCompound()));
        nbtTags.setTag("uuTank", uuTank.write(new NBTTagCompound()));
        nbtTags.setTag("outputTank", outputTank.writeToNBT(new NBTTagCompound()));
    }


    @Override
    public Map<GasAndFluidInput, ReplicatorFluidStackRecipe> getRecipes() {
        return RecipeHandler.Recipe.REPLICATOR_FLUIDSTACK_RECIPE.get();
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
    public int receiveGas(EnumFacing side, GasStack stack, boolean doTransfer) {
        if (canReceiveGas(side, stack != null ? stack.getGas() : null)) {
            return uuTank.receive(stack, doTransfer);
        }
        return 0;
    }

    @Override
    public GasStack drawGas(EnumFacing side, int amount, boolean doTransfer) {
        return null;
    }

    @Override
    public boolean canReceiveGas(EnumFacing side, Gas type) {
        return configComponent.getOutput(TransmissionType.GAS, side, facing).ioState == SideData.IOState.INPUT && uuTank.canReceive(type);
    }


    @Override
    public boolean canDrawGas(EnumFacing side, Gas type) {
        return false;
    }

    @Nonnull
    @Override
    public GasTankInfo[] getTankInfo() {
        return new GasTankInfo[]{uuTank};
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, EnumFacing side) {
        if (isCapabilityDisabled(capability, side)) {
            return false;
        }
        return capability == Capabilities.GAS_HANDLER_CAPABILITY || capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, side);
    }

    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, EnumFacing side) {
        if (isCapabilityDisabled(capability, side)) {
            return null;
        } else if (capability == Capabilities.GAS_HANDLER_CAPABILITY) {
            return Capabilities.GAS_HANDLER_CAPABILITY.cast(this);
        } else if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(new FluidHandlerWrapper(this, side));
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void writeSustainedData(ItemStack itemStack) {
        if (inputTank.getFluid() != null) {
            ItemDataUtils.setCompound(itemStack, "inputTank", inputTank.getFluid().writeToNBT(new NBTTagCompound()));
        }
        if (uuTank.getGas() != null) {
            ItemDataUtils.setCompound(itemStack, "uuTank", uuTank.getGas().write(new NBTTagCompound()));
        }
        if (outputTank.getFluid() != null) {
            ItemDataUtils.setCompound(itemStack, "outputTank", outputTank.getFluid().writeToNBT(new NBTTagCompound()));
        }
    }

    @Override
    public void readSustainedData(ItemStack itemStack) {
        inputTank.setFluid(FluidStack.loadFluidStackFromNBT(ItemDataUtils.getCompound(itemStack, "inputTank")));
        uuTank.setGas(GasStack.readFromNBT(ItemDataUtils.getCompound(itemStack, "uuTank")));
        outputTank.setFluid(FluidStack.loadFluidStackFromNBT(ItemDataUtils.getCompound(itemStack, "outputTank")));
    }

    @Override
    public Object[] getTanks() {
        return new Object[]{inputTank, uuTank, outputTank};
    }

    @Override
    public boolean getEnergySlot() {
        return inventory.get(0).isEmpty();
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
        return 16;
    }

    @Override
    public IGuiProvider guiProvider() {
        return MEKCeuMoreMachine.proxy;
    }

    @Nonnull
    @Override
    public String getName() {
        return LangUtils.localize("tile.ReplicatorFluidStack.name");
    }

    @Override
    public int fill(EnumFacing from, @Nonnull FluidStack resource, boolean doFill) {
        return inputTank.fill(resource, doFill);
    }

    @Override
    public boolean canFill(EnumFacing from, @Nonnull FluidStack fluid) {
        return configComponent.getOutput(TransmissionType.FLUID, from, facing).hasSlot(0) && FluidContainerUtils.canFill(inputTank.getFluid(), fluid);

    }

    @Override
    @Nullable
    public FluidStack drain(EnumFacing from, int maxDrain, boolean doDrain) {
        return outputTank.drain(maxDrain, doDrain);
    }

    @Override
    public boolean canDrain(EnumFacing from, @Nullable FluidStack fluid) {
        return configComponent.getOutput(TransmissionType.FLUID, from, facing).hasSlot(1) && FluidContainerUtils.canDrain(outputTank.getFluid(), fluid);
    }

    @Override
    public FluidTankInfo[] getTankInfo(EnumFacing from) {
        SideData data = configComponent.getOutput(TransmissionType.FLUID, from, facing);
        return data.getFluidTankInfo(this);
    }

    @Override
    public FluidTankInfo[] getAllTanks() {
        return new FluidTankInfo[]{inputTank.getInfo(), outputTank.getInfo()};
    }

    @Override
    public boolean shouldDumpRadiation() {
        return true;
    }
}
