package mekceumoremachine.common.tile.machine.replicator;

import io.netty.buffer.ByteBuf;
import mekanism.api.IContentsListener;
import mekanism.api.TileNetworkList;
import mekanism.api.gas.*;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.Upgrade;
import mekanism.common.base.*;
import mekanism.common.capabilities.holder.fluid.FluidTankHelper;
import mekanism.common.capabilities.holder.fluid.IFluidTankHolder;
import mekanism.common.capabilities.holder.gas.GasTankHelper;
import mekanism.common.capabilities.holder.gas.IGasTankHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.cache.CachedRecipe;
import mekanism.common.recipe.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.common.recipe.cache.TwoInputCachedRecipe;
import mekanism.common.recipe.cache.inputs.InputHelper;
import mekanism.common.recipe.cache.outputs.OutputHelper;
import mekanism.common.recipe.inputs.GasAndFluidInput;
import mekanism.common.recipe.machines.ReplicatorFluidStackRecipe;
import mekanism.common.recipe.outputs.FluidOutput;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.prefab.TileEntityBasicMachine;
import mekanism.common.util.*;
import mekceumoremachine.common.capability.ResizableFluidTank;
import mekceumoremachine.common.capability.ResizableGasTank;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.config.MoreMachineConfig;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.Map;

public class TileEntityReplicatorFluidStack extends TileEntityBasicMachine<GasAndFluidInput, FluidOutput, ReplicatorFluidStackRecipe> implements ISustainedData, ITankManager, ISpecialSelectionWireframeTile {

    public static final int MAX_GAS_OR_FLUID = 10000;
    public ResizableFluidTank inputTank;
    public ResizableGasTank uuTank;
    public ResizableFluidTank outputTank;
    private EnergyInventorySlot energySlot;


    public TileEntityReplicatorFluidStack() {
        super("prc", "ReplicatorFluidStack", MoreMachineConfig.current().config.ReplicatorFluidStackEnergyStorge.val(), MoreMachineConfig.current().config.ReplicatorFluidStackEnergyUsage.val(), 1, 200);

        configComponent = new TileComponentConfig(this, TransmissionType.ENERGY, TransmissionType.FLUID, TransmissionType.GAS);

        initializeInventorySlots();
        configComponent.setupInputConfig(TransmissionType.GAS, uuTank);

        configComponent.setupFluidIOConfig(inputTank, outputTank);
        configComponent.setConfig(TransmissionType.FLUID, DataType.NONE, DataType.NONE, DataType.OUTPUT, DataType.NONE, DataType.INPUT, DataType.NONE);

        configComponent.setInputConfig(TransmissionType.ENERGY);

        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(configComponent, TransmissionType.FLUID);
    }

    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = createInventorySlotHelper();
        energySlot = builder.addSlot(EnergyInventorySlot.fillOrConvert(getMainEnergyContainer(), this::getWorld, listener, 154, 13));
        return builder.build();
    }

    @Override
    protected IFluidTankHolder getInitialFluidTanks(IContentsListener listener) {
        FluidTankHelper builder = createFluidTankHelper();
        builder.addTank(getOrCreateInputTank(listener));
        builder.addTank(getOrCreateOutputTank(listener));
        return builder.build();
    }

    @Override
    protected IGasTankHolder getInitialGasTanks(IContentsListener listener) {
        GasTankHelper builder = createGasTankHelper();
        builder.addTank(getOrCreateUUTank(listener));
        return builder.build();
    }

    private ResizableFluidTank getOrCreateInputTank(IContentsListener listener) {
        if (inputTank == null) {
            inputTank = ResizableFluidTank.input(MAX_GAS_OR_FLUID, fluid -> true, listener);
        }
        return inputTank;
    }

    private ResizableFluidTank getOrCreateOutputTank(IContentsListener listener) {
        if (outputTank == null) {
            outputTank = ResizableFluidTank.output(MAX_GAS_OR_FLUID, listener);
        }
        return outputTank;
    }

    private ResizableGasTank getOrCreateUUTank(IContentsListener listener) {
        if (uuTank == null) {
            uuTank = ResizableGasTank.input(MAX_GAS_OR_FLUID, gas -> true, listener);
        }
        return uuTank;
    }

    @Override
    public void onAsyncUpdateServer() {
        super.onAsyncUpdateServer();
        energySlot.fillContainerOrConvert();
        processRecipe(getMainEnergyContainer());
        prevEnergy = getEnergy();
    }

    @Override
    protected void setNoFinish() {
        BASE_TICKS_REQUIRED = 200;
    }

    @Override
    public void onCachedRecipeChanged(CachedRecipe<ReplicatorFluidStackRecipe> cachedRecipe, int cacheIndex) {
        super.onCachedRecipeChanged(cachedRecipe, cacheIndex);
        int ticks = cachedRecipe == null ? 200 : cachedRecipe.getRecipe().ticks;
        boolean update = BASE_TICKS_REQUIRED != ticks;
        BASE_TICKS_REQUIRED = ticks;
        if (update) {
            recalculateUpgradables(Upgrade.SPEED);
        }
    }

    @Override
    public ReplicatorFluidStackRecipe getRecipe() {
        refreshRecipeLookupCache();
        GasAndFluidInput input = getInput();
        if (cachedRecipe == null || !input.testEquality(cachedRecipe.getInput())) {
            cachedRecipe = RecipeHandler.getReplicatorFluidStackRecipe(input);
        }
        return cachedRecipe;
    }

    @Override
    protected void clearRecipeLookupCache() {
        super.clearRecipeLookupCache();
        cachedRecipe = null;
    }

    @Override
    public GasAndFluidInput getInput() {
        return new GasAndFluidInput(uuTank.getGas(), inputTank.getFluid());
    }

    @Override
    public boolean canOperate(ReplicatorFluidStackRecipe recipe) {
        return recipe != null && recipe.canOperate(uuTank, inputTank, outputTank);
    }

    @Override
    public ReplicatorFluidStackRecipe getRecipe(int cacheIndex) {
        return getRecipe();
    }

    @Override
    public CachedRecipe<ReplicatorFluidStackRecipe> createNewCachedRecipe(ReplicatorFluidStackRecipe recipe, int cacheIndex) {
        return new TwoInputCachedRecipe<>(recipe, this::shouldRecheckAllRecipeErrors,
              InputHelper.getGasInputHandler(uuTank, RecipeError.NOT_ENOUGH_INPUT),
              InputHelper.getFluidInputHandler(inputTank, RecipeError.NOT_ENOUGH_SECONDARY_INPUT),
              OutputHelper.getOutputHandler(outputTank, RecipeError.NOT_ENOUGH_OUTPUT_SPACE),
              () -> recipe.getInput().ingredientGas, () -> recipe.getInput().ingredientFluid,
              (gas, fluid) -> gas != null && gas.isGasEqual(recipe.getInput().ingredientGas)
                    && fluid != null && fluid.isFluidEqual(recipe.getInput().ingredientFluid),
              (gas, fluid) -> recipe.getOutput().output.copy(),
              gas -> gas == null || gas.amount <= 0,
              fluid -> fluid == null || fluid.amount <= 0,
              output -> output == null || output.amount <= 0)
              .setCanHolderFunction(() -> MekanismUtils.canFunction(this))
              .setActive(active -> {
                  if (active || prevEnergy >= getEnergy()) {
                      setActive(active);
                  }
              })
              .setEnergyRequirements(() -> getReplicatorEnergyPerTick(recipe), getMainEnergyContainer())
              .setRequiredTicks(() -> ticksRequired)
              .setBaselineMaxOperations(() -> getBaselineMaxOperations(getReplicatorEnergyPerTick(recipe), true))
              .setOperatingTicksChanged(ticks -> operatingTicks = ticks)
              .setErrorsChanged(this::onRecipeErrorsChanged)
              .setOnFinish(this::onCachedRecipeFinish);
    }

    private double getReplicatorEnergyPerTick(ReplicatorFluidStackRecipe recipe) {
        return MekanismUtils.getEnergyPerTick(this, BASE_ENERGY_PER_TICK + recipe.extraEnergy);
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
    public Object[] getManagedTanks() {
        return new Object[]{inputTank, uuTank, outputTank};
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
    public boolean shouldDumpRadiation() {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Class<?> getSelectionWireframeModelClass() {
        return mekceumoremachine.client.model.machine.ModelReplicatorBase.class;
    }
}
