package mekceumoremachine.common.tile.machine.replicator;

import io.netty.buffer.ByteBuf;
import mekanism.api.IContentsListener;
import mekanism.api.TileNetworkList;
import mekanism.api.gas.*;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.Upgrade;
import mekanism.common.base.IGuiProvider;
import mekanism.common.base.ISpecialSelectionWireframeTile;
import mekanism.common.base.ISustainedData;
import mekanism.common.base.ITankManager;
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
import mekanism.common.recipe.inputs.ChemicalGasInput;
import mekanism.common.recipe.machines.ReplicatorGasStackRecipe;
import mekanism.common.recipe.outputs.GasOutput;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.prefab.TileEntityBasicMachine;
import mekanism.common.util.*;
import mekceumoremachine.common.capability.ResizableGasTank;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.config.MoreMachineConfig;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.Map;

public class TileEntityReplicatorGases extends TileEntityBasicMachine<ChemicalGasInput, GasOutput, ReplicatorGasStackRecipe> implements ISustainedData, ITankManager, ISpecialSelectionWireframeTile {

    public static final int MAX_GAS = 10000;
    public ResizableGasTank inputTank;
    public ResizableGasTank uuTank;
    public ResizableGasTank outputTank;
    private EnergyInventorySlot energySlot;

    public TileEntityReplicatorGases() {
        super("prc", "ReplicatorGases", MoreMachineConfig.current().config.ReplicatorGasesEnergyStorge.val(), MoreMachineConfig.current().config.ReplicatorGasesEnergyUsage.val(), 1, 200);

        configComponent = new TileComponentConfig(this, TransmissionType.ENERGY, TransmissionType.GAS);

        initializeInventorySlots();
        configComponent.setupGasDualInputOutputConfig(inputTank, uuTank, outputTank);
        configComponent.setConfig(TransmissionType.GAS, DataType.NONE, DataType.NONE, DataType.OUTPUT, DataType.NONE, DataType.INPUT_1, DataType.INPUT_2);

        configComponent.setInputConfig(TransmissionType.ENERGY);

        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(configComponent, TransmissionType.GAS);
    }

    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = createInventorySlotHelper();
        energySlot = builder.addSlot(EnergyInventorySlot.fillOrConvert(getMainEnergyContainer(), this::getWorld, listener, 154, 13));
        return builder.build();
    }

    @Override
    protected IGasTankHolder getInitialGasTanks(IContentsListener listener) {
        GasTankHelper builder = createGasTankHelper();
        builder.addTank(getOrCreateInputTank(listener));
        builder.addTank(getOrCreateUUTank(listener));
        builder.addTank(getOrCreateOutputTank(listener));
        return builder.build();
    }

    private ResizableGasTank getOrCreateInputTank(IContentsListener listener) {
        if (inputTank == null) {
            inputTank = ResizableGasTank.input(MAX_GAS, this::caninputTank, listener);
        }
        return inputTank;
    }

    private ResizableGasTank getOrCreateUUTank(IContentsListener listener) {
        if (uuTank == null) {
            uuTank = ResizableGasTank.input(MAX_GAS, this::canUUTank, listener);
        }
        return uuTank;
    }

    private ResizableGasTank getOrCreateOutputTank(IContentsListener listener) {
        if (outputTank == null) {
            outputTank = ResizableGasTank.output(MAX_GAS, listener);
        }
        return outputTank;
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
    public void onCachedRecipeChanged(CachedRecipe<ReplicatorGasStackRecipe> cachedRecipe, int cacheIndex) {
        super.onCachedRecipeChanged(cachedRecipe, cacheIndex);
        int ticks = cachedRecipe == null ? 200 : cachedRecipe.getRecipe().ticks;
        boolean update = BASE_TICKS_REQUIRED != ticks;
        BASE_TICKS_REQUIRED = ticks;
        if (update) {
            recalculateUpgradables(Upgrade.SPEED);
        }
    }

    @Override
    public ReplicatorGasStackRecipe getRecipe() {
        refreshRecipeLookupCache();
        ChemicalGasInput input = getInput();
        if (cachedRecipe == null || !input.testEquality(cachedRecipe.getInput())) {
            cachedRecipe = RecipeHandler.getReplicatorGasStackRecipe(input);
        }
        return cachedRecipe;
    }

    @Override
    protected void clearRecipeLookupCache() {
        super.clearRecipeLookupCache();
        cachedRecipe = null;
    }

    @Override
    public ChemicalGasInput getInput() {
        return new ChemicalGasInput(inputTank.getGas(), uuTank.getGas());
    }

    @Override
    public boolean canOperate(ReplicatorGasStackRecipe recipe) {
        return recipe != null && recipe.canOperate(inputTank, uuTank, outputTank);
    }

    @Override
    public ReplicatorGasStackRecipe getRecipe(int cacheIndex) {
        return getRecipe();
    }

    @Override
    public CachedRecipe<ReplicatorGasStackRecipe> createNewCachedRecipe(ReplicatorGasStackRecipe recipe, int cacheIndex) {
        return new TwoInputCachedRecipe<>(recipe, this::shouldRecheckAllRecipeErrors,
              InputHelper.getGasInputHandler(inputTank, RecipeError.NOT_ENOUGH_INPUT, () -> false),
              InputHelper.getGasInputHandler(uuTank, RecipeError.NOT_ENOUGH_SECONDARY_INPUT),
              OutputHelper.getGasOutputHandler(outputTank, RecipeError.NOT_ENOUGH_OUTPUT_SPACE),
              () -> recipe.getInput().input, () -> recipe.getInput().uu,
              (input, uu) -> input != null && input.isGasEqual(recipe.getInput().input)
                    && uu != null && uu.isGasEqual(recipe.getInput().uu),
              (input, uu) -> recipe.getOutput().output.copy(),
              input -> input == null || input.amount <= 0,
              uu -> uu == null || uu.amount <= 0,
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

    private double getReplicatorEnergyPerTick(ReplicatorGasStackRecipe recipe) {
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
    public Object[] getManagedTanks() {
        return new Object[]{inputTank, uuTank, outputTank};
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
