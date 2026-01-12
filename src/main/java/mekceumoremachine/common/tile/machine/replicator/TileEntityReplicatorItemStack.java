package mekceumoremachine.common.tile.machine.replicator;

import io.netty.buffer.ByteBuf;
import mekanism.api.TileNetworkList;
import mekanism.api.gas.*;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.SideData;
import mekanism.common.Upgrade;
import mekanism.common.base.IGuiProvider;
import mekanism.common.base.ISustainedData;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.item.ItemUpgrade;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.inputs.NucleosynthesizerInput;
import mekanism.common.recipe.machines.ReplicatorItemStackRecipe;
import mekanism.common.recipe.outputs.ItemStackOutput;
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
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nonnull;
import java.util.Map;

public class TileEntityReplicatorItemStack extends TileEntityBasicMachine<NucleosynthesizerInput, ItemStackOutput, ReplicatorItemStackRecipe> implements IGasHandler, ISustainedData {

    private static final String[] methods = new String[]{"getEnergy", "getProgress", "isActive", "facing", "canOperate", "getMaxEnergy", "getEnergyNeeded", "getGasStored"};
    public GasTank inputGasTank = new GasTank(10000);

    public TileEntityReplicatorItemStack() {
        super("prc", "ReplicatorItemStack", MoreMachineConfig.current().config.ReplicatorItemStackEnergyStorge.val(), MoreMachineConfig.current().config.ReplicatorItemStackEnergyUsage.val(), 3, 200);

        configComponent = new TileComponentConfig(this, TransmissionType.ITEM, TransmissionType.ENERGY, TransmissionType.GAS);

        configComponent.addOutput(TransmissionType.ITEM, new SideData(DataType.NONE, InventoryUtils.EMPTY));
        configComponent.addOutput(TransmissionType.ITEM, new SideData(DataType.INPUT, new int[]{0}));
        configComponent.addOutput(TransmissionType.ITEM, new SideData(DataType.ENERGY, new int[]{1}));
        configComponent.addOutput(TransmissionType.ITEM, new SideData(DataType.OUTPUT, new int[]{2}));
        configComponent.addOutput(TransmissionType.ITEM, new SideData(new int[]{0, 2}, new boolean[]{false, true}));
        configComponent.setConfig(TransmissionType.ITEM, new byte[]{2, 1, 0, 0, 0, 3});

        configComponent.setInputConfig(TransmissionType.GAS);
        configComponent.setInputConfig(TransmissionType.ENERGY);
        inventory = NonNullListSynchronized.withSize(4, ItemStack.EMPTY);

        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(TransmissionType.ITEM, configComponent.getOutputs(TransmissionType.ITEM).get(3));
        ejectorComponent.setInputOutputData(TransmissionType.ITEM, configComponent.getOutputs(TransmissionType.ITEM).get(4));
    }

    @Override
    public void onAsyncUpdateServer() {
        super.onAsyncUpdateServer();
        ReplicatorItemStackRecipe recipe = getRecipe();
        ChargeUtils.discharge(1, this);
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
        if (slotID == 0) {
            return isInRecipe(itemstack);
        } else if (slotID == 1) {
            return ChargeUtils.canBeDischarged(itemstack);
        } else if (slotID == 3) {
            return itemstack.getItem() instanceof ItemUpgrade;
        }
        return false;
    }

    public static boolean isInRecipe(@Nonnull ItemStack stack) {
        if (!stack.isEmpty()) {
            for (NucleosynthesizerInput key : RecipeHandler.Recipe.REPLICATOR_ITEMSTACK_RECIPE.get().keySet()) {
                if (key.containsType(stack)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public ReplicatorItemStackRecipe getRecipe() {
        NucleosynthesizerInput input = getInput();
        if (cachedRecipe == null || !input.testEquality(cachedRecipe.getInput())) {
            cachedRecipe = RecipeHandler.getReplicatorItemStackRecipe(input);
        }
        return cachedRecipe;
    }

    @Override
    public NucleosynthesizerInput getInput() {
        return new NucleosynthesizerInput(inventory.get(0), inputGasTank.getGas());
    }

    @Override
    public void operate(ReplicatorItemStackRecipe recipe) {
        recipe.operate(inventory, 0, inputGasTank, 2);
        markNoUpdateSync();
    }

    @Override
    public boolean canOperate(ReplicatorItemStackRecipe recipe) {
        return recipe != null && recipe.canOperate(inventory, 0, 2, inputGasTank);
    }

    @Override
    public boolean canExtractItem(int slotID, @Nonnull ItemStack itemstack, @Nonnull EnumFacing side) {
        if (slotID == 1) {
            return ChargeUtils.canBeOutputted(itemstack, false);
        }
        return slotID == 2 || slotID == 4;
    }

    @Override
    public TileNetworkList getNetworkedData(TileNetworkList data) {
        super.getNetworkedData(data);
        TileUtils.addTankData(data, inputGasTank);
        return data;
    }

    @Override
    public void handlePacketData(ByteBuf dataStream) {
        super.handlePacketData(dataStream);
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            TileUtils.readTankData(dataStream, inputGasTank);
        }
    }

    @Override
    public void readCustomNBT(NBTTagCompound nbtTags) {
        super.readCustomNBT(nbtTags);
        inputGasTank.read(nbtTags.getCompoundTag("inputGasTank"));
    }

    @Override
    public void writeCustomNBT(NBTTagCompound nbtTags) {
        super.writeCustomNBT(nbtTags);
        nbtTags.setTag("inputGasTank", inputGasTank.write(new NBTTagCompound()));
    }

    @Override
    public Map<NucleosynthesizerInput, ReplicatorItemStackRecipe> getRecipes() {
        return RecipeHandler.Recipe.REPLICATOR_ITEMSTACK_RECIPE.get();
    }

    @Override
    public String[] getMethods() {
        return methods;
    }

    @Override
    public Object[] invoke(int method, Object[] arguments) throws NoSuchMethodException {
        return switch (method) {
            case 0 -> new Object[]{getEnergy()};
            case 1 -> new Object[]{operatingTicks};
            case 2 -> new Object[]{isActive};
            case 3 -> new Object[]{facing};
            case 4 -> new Object[]{canOperate(getRecipe())};
            case 5 -> new Object[]{getMaxEnergy()};
            case 6 -> new Object[]{getMaxEnergy() - getEnergy()};
            case 7 -> new Object[]{inputGasTank.getStored()};
            default -> throw new NoSuchMethodException();
        };
    }

    @Override
    public int receiveGas(EnumFacing side, GasStack stack, boolean doTransfer) {
        if (canReceiveGas(side, stack.getGas())) {
            return inputGasTank.receive(stack, doTransfer);
        }
        return 0;
    }

    @Override
    public GasStack drawGas(EnumFacing side, int amount, boolean doTransfer) {
        return null;
    }

    @Override
    public boolean canReceiveGas(EnumFacing side, Gas type) {
        return configComponent.getOutput(TransmissionType.GAS, side, facing).ioState == SideData.IOState.INPUT && inputGasTank.canReceive(type);
    }

    @Override
    public boolean canDrawGas(EnumFacing side, Gas type) {
        return false;
    }

    @Nonnull
    @Override
    public GasTankInfo[] getTankInfo() {
        return new GasTankInfo[]{inputGasTank};
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
        if (inputGasTank.getGas() != null) {
            ItemDataUtils.setCompound(itemStack, "inputGasTank", inputGasTank.getGas().write(new NBTTagCompound()));
        }
    }

    @Override
    public void readSustainedData(ItemStack itemStack) {
        inputGasTank.setGas(GasStack.readFromNBT(ItemDataUtils.getCompound(itemStack, "inputGasTank")));
    }

    @Override
    public boolean getEnergySlot() {
        return inventory.get(1).isEmpty();
    }

    @Override
    public boolean getInputSlot() {
        return inventory.get(0).isEmpty();
    }

    @Override
    public boolean getOuputSlot() {
        return inventory.get(2).isEmpty();
    }

    @Override
    public int getBlockGuiID(Block block, int metadata) {
        return 14;
    }

    @Override
    public IGuiProvider guiProvider() {
        return MEKCeuMoreMachine.proxy;
    }

    @Nonnull
    @Override
    public String getName() {
        return LangUtils.localize("tile.ReplicatorItemStack.name");
    }
}
