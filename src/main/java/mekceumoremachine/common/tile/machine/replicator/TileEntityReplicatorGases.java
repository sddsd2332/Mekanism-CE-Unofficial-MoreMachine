package mekceumoremachine.common.tile.machine.replicator;

import io.netty.buffer.ByteBuf;
import mekanism.api.TileNetworkList;
import mekanism.api.gas.*;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.SideData;
import mekanism.common.Upgrade;
import mekanism.common.base.IGuiProvider;
import mekanism.common.base.ISustainedData;
import mekanism.common.base.ITankManager;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.inputs.ChemicalGasInput;
import mekanism.common.recipe.machines.ReplicatorGasStackRecipe;
import mekanism.common.recipe.outputs.GasOutput;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.prefab.TileEntityBasicMachine;
import mekanism.common.util.*;
import mekceumoremachine.common.MEKCeuMoreMachine;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nonnull;
import java.util.Map;

public class TileEntityReplicatorGases extends TileEntityBasicMachine<ChemicalGasInput, GasOutput, ReplicatorGasStackRecipe> implements IGasHandler, ISustainedData, ITankManager {

    public static final int MAX_GAS = 10000;
    public GasTank inputTank = new GasTank(MAX_GAS);
    public GasTank uuTank = new GasTank(MAX_GAS);
    public GasTank outputTank = new GasTank(MAX_GAS);

    public TileEntityReplicatorGases() {
        super("prc", "ReplicatorGases", 80000D, 200, 1, 200);

        configComponent = new TileComponentConfig(this, TransmissionType.ENERGY, TransmissionType.GAS);

        configComponent.addOutput(TransmissionType.GAS, new SideData(DataType.NONE, InventoryUtils.EMPTY));
        configComponent.addOutput(TransmissionType.GAS, new SideData(DataType.INPUT_1, new int[]{0}));
        configComponent.addOutput(TransmissionType.GAS, new SideData(DataType.INPUT_2, new int[]{1}));
        configComponent.addOutput(TransmissionType.GAS, new SideData(DataType.OUTPUT, new int[]{2}));
        configComponent.addOutput(TransmissionType.GAS, new SideData(DataType.INPUT, new int[]{0, 1}));
        configComponent.addOutput(TransmissionType.GAS, new SideData(new int[]{0, 1, 2}, new boolean[]{false, false, true}));
        configComponent.setConfig(TransmissionType.GAS, new byte[]{0, 0, 3, 0, 1, 2});

        configComponent.setInputConfig(TransmissionType.ENERGY);

        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(TransmissionType.GAS, configComponent.getOutputs(TransmissionType.GAS).get(3));
        ejectorComponent.setInputOutputData(TransmissionType.GAS, configComponent.getOutputs(TransmissionType.GAS).get(5));

        inventory = NonNullListSynchronized.withSize(2, ItemStack.EMPTY);
    }

    @Override
    public void onAsyncUpdateServer() {
        super.onAsyncUpdateServer();
        ChargeUtils.discharge(0, this);
        ReplicatorGasStackRecipe recipe = getRecipe();
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
    public ReplicatorGasStackRecipe getRecipe() {
        ChemicalGasInput input = getInput();
        if (cachedRecipe == null || !input.testEquality(cachedRecipe.getInput())) {
            cachedRecipe = RecipeHandler.getReplicatorGasStackRecipe(input);
        }
        return cachedRecipe;
    }

    @Override
    public ChemicalGasInput getInput() {
        return new ChemicalGasInput(inputTank.getGas(), uuTank.getGas());
    }

    @Override
    public void operate(ReplicatorGasStackRecipe recipe) {
        recipe.operate(inputTank, uuTank, outputTank, 1);
        markNoUpdateSync();
    }

    @Override
    public boolean canOperate(ReplicatorGasStackRecipe recipe) {
        return recipe != null && recipe.canOperate(inputTank, uuTank, outputTank);
    }

    @Override
    public boolean canExtractItem(int slotID, @Nonnull ItemStack itemstack, @Nonnull EnumFacing side) {
        if (slotID == 0) {
            return ChargeUtils.canBeOutputted(itemstack, false);
        }
        return false;
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
        inputTank.read(nbtTags.getCompoundTag("inputTank"));
        uuTank.read(nbtTags.getCompoundTag("uuTank"));
        outputTank.read(nbtTags.getCompoundTag("outputTank"));
    }

    @Override
    public void writeCustomNBT(NBTTagCompound nbtTags) {
        super.writeCustomNBT(nbtTags);
        nbtTags.setTag("inputTank", inputTank.write(new NBTTagCompound()));
        nbtTags.setTag("uuTank", uuTank.write(new NBTTagCompound()));
        nbtTags.setTag("outputTank", outputTank.write(new NBTTagCompound()));
    }


    @Override
    public Map<ChemicalGasInput, ReplicatorGasStackRecipe> getRecipes() {
        return RecipeHandler.Recipe.REPLICATOR_GASES_RECIPE.get();
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
            if (configComponent.getOutput(TransmissionType.GAS, side, facing).hasSlot(0, 1)) {
                if (stack != null) {
                    if (inputTank.canReceive(stack.getGas()) && caninputTank(stack.getGas()) && uuTank.getGasType() != stack.getGas()) {
                        return inputTank.receive(stack, doTransfer);
                    }
                    if (uuTank.canReceive(stack.getGas()) && canUUTank(stack.getGas()) && inputTank.getGasType() != stack.getGas()) {
                        return uuTank.receive(stack, doTransfer);
                    }
                }
            }
        }
        return 0;
    }

    @Override
    public GasStack drawGas(EnumFacing side, int amount, boolean doTransfer) {
        if (canDrawGas(side, null)) {
            return outputTank.draw(amount, doTransfer);
        }
        return null;
    }

    @Override
    public boolean canReceiveGas(EnumFacing side, Gas type) {
        if (configComponent.getOutput(TransmissionType.GAS, side, facing).hasSlot(0, 1)) {
            return inputTank.canReceive(type) && caninputTank(type) || uuTank.canReceive(type) && canUUTank(type);
        }
        return false;
    }

    public boolean caninputTank(Gas type) {
        Map<ChemicalGasInput, ReplicatorGasStackRecipe> r = getRecipes();
        for (Map.Entry<ChemicalGasInput, ReplicatorGasStackRecipe> entry : r.entrySet()) {
            return entry.getKey().input.isGasEqual(type);
        }
        return false;
    }

    public boolean canUUTank(Gas type) {
        Map<ChemicalGasInput, ReplicatorGasStackRecipe> r = getRecipes();
        for (Map.Entry<ChemicalGasInput, ReplicatorGasStackRecipe> entry : r.entrySet()) {
            return entry.getKey().uu.isGasEqual(type);

        }
        return false;
    }

    @Override
    public boolean canDrawGas(EnumFacing side, Gas type) {
        return configComponent.getOutput(TransmissionType.GAS, side, facing).hasSlot(2) && outputTank.canDraw(type);
    }

    @Nonnull
    @Override
    public GasTankInfo[] getTankInfo() {
        return new GasTankInfo[]{inputTank, outputTank, uuTank};
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, EnumFacing side) {
        if (isCapabilityDisabled(capability, side)) {
            return false;
        }
        return capability == Capabilities.GAS_HANDLER_CAPABILITY || super.hasCapability(capability, side);
    }

    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, EnumFacing side) {
        if (isCapabilityDisabled(capability, side)) {
            return null;
        } else if (capability == Capabilities.GAS_HANDLER_CAPABILITY) {
            return Capabilities.GAS_HANDLER_CAPABILITY.cast(this);
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void writeSustainedData(ItemStack itemStack) {
        if (inputTank.getGas() != null) {
            ItemDataUtils.setCompound(itemStack, "inputTank", inputTank.getGas().write(new NBTTagCompound()));
        }
        if (uuTank.getGas() != null) {
            ItemDataUtils.setCompound(itemStack, "uuTank", uuTank.getGas().write(new NBTTagCompound()));
        }
        if (outputTank.getGas() != null) {
            ItemDataUtils.setCompound(itemStack, "outputTank", outputTank.getGas().write(new NBTTagCompound()));
        }
    }

    @Override
    public void readSustainedData(ItemStack itemStack) {
        inputTank.setGas(GasStack.readFromNBT(ItemDataUtils.getCompound(itemStack, "inputTank")));
        uuTank.setGas(GasStack.readFromNBT(ItemDataUtils.getCompound(itemStack, "uuTank")));
        outputTank.setGas(GasStack.readFromNBT(ItemDataUtils.getCompound(itemStack, "outputTank")));
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
        return 15;
    }

    @Override
    public IGuiProvider guiProvider() {
        return MEKCeuMoreMachine.proxy;
    }

    @Nonnull
    @Override
    public String getName() {
        return LangUtils.localize("tile.ReplicatorGases.name");
    }
}
