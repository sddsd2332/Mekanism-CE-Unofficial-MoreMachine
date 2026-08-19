package mekceumoremachine.common.tile.machine.TierOrganicFarm;

import io.netty.buffer.ByteBuf;
import mekanism.api.Action;
import mekanism.api.Coord4D;
import mekanism.api.IContentsListener;
import mekanism.api.TileNetworkList;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.math.MathUtils;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.CommonWorldTickHandler;
import mekanism.common.Upgrade;
import mekanism.common.base.IBoundingBlock;
import mekanism.common.base.IComparatorSupport;
import mekanism.common.base.IGuiProvider;
import mekanism.common.base.ISideConfiguration;
import mekanism.common.base.ISustainedData;
import mekanism.common.base.ITankManager;
import mekanism.common.block.states.BlockStateMachine.MachineType;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.fluid.BasicFluidTank;
import mekanism.common.capabilities.gas.BasicGasTank;
import mekanism.common.capabilities.holder.fluid.FluidTankHelper;
import mekanism.common.capabilities.holder.fluid.IFluidTankHolder;
import mekanism.common.capabilities.holder.gas.GasTankHelper;
import mekanism.common.capabilities.holder.gas.IGasTankHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.capabilities.merged.MergedTank;
import mekanism.common.capabilities.merged.MergedTank.CurrentType;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.inventory.slot.HybridInventorySlot;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.inventory.slot.gas.GasInventorySlot;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.cache.CachedRecipe;
import mekanism.common.recipe.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.common.recipe.cache.IRecipeLookupHandler;
import mekanism.common.recipe.cache.ItemStackConstantFarmCachedRecipe;
import mekanism.common.recipe.cache.ItemStackConstantGasCachedRecipe.GasUsageMultiplier;
import mekanism.common.recipe.cache.RecipeCacheLookupMonitor;
import mekanism.common.recipe.cache.inputs.InputHelper;
import mekanism.common.recipe.cache.outputs.OutputHelper;
import mekanism.common.recipe.inputs.FarmInput;
import mekanism.common.recipe.machines.FarmRecipe;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.prefab.TileEntityMachine;
import mekanism.common.tier.BaseTier;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.util.ItemDataUtils;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.PoissonSampler;
import mekanism.common.util.TileUtils;
import mekanism.common.util.UpgradeUtils;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.block.states.BlockStateTierOrganicFarm;
import mekceumoremachine.common.inventory.slot.FarmOutputInventorySlot;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.interfaces.ITierMachine;
import mekceumoremachine.common.tile.interfaces.ITierSorting;
import mekceumoremachine.common.tile.machine.TierProcessInputSorter;
import mekceumoremachine.common.upgrade.LargeMachineUpgradeData;
import mekceumoremachine.common.upgrade.LargeMachineUpgradeDataApplier;
import mekceumoremachine.common.upgrade.OrganicFarmUpgradeData;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * Independent multi-lane Organic Farm implementation. Each lane owns its
 * input, progress, cache and usage counter; the merged medium tank and output
 * inventory are shared by all lanes.
 */
public class TileEntityTierOrganicFarm extends TileEntityMachine implements ITierMachine<MachineTier>,
      ISideConfiguration, ITankManager, ITierSorting, ISustainedData, IComparatorSupport, IBoundingBlock,
      IRecipeLookupHandler<FarmRecipe>, IRecipeLookupHandler.ConstantUsageRecipeLookupHandler,
      TierProcessInputSorter.Context {

    public static final int BASE_TICKS_REQUIRED = 200;
    public static final int BASE_SECONDARY_PER_TICK = 1;
    public static final int OUTPUT_SLOT_COUNT = 64;

    // Match the vanilla factory layout. The GUI renders the shared medium bar
    // below the process lanes and keeps the item slots in the standard rows.
    private static final int ENERGY_SLOT_X = 7;
    private static final int ENERGY_SLOT_Y = 13;
    private static final int MEDIUM_SLOT_X = 7;
    private static final int MEDIUM_SLOT_Y = 57;
    private static final int PROCESS_INPUT_SLOT_Y = 13;

    public final MachineTier tier;
    public final int threadCount;
    public final InputInventorySlot[] inputSlots;
    public final int[] progress;
    public final long[] usedSoFar;
    public final FarmRecipe[] cachedRecipe;
    public final boolean[] activeProcesses;
    public final boolean[] errorProcesses;
    public final List<FarmOutputInventorySlot> outputSlots = new ArrayList<>(OUTPUT_SLOT_COUNT);
    private final RecipeCacheLookupMonitor<FarmRecipe>[] recipeCacheLookupMonitors;
    private final PoissonSampler[] secondaryUsageSamplers;
    private final TierProcessInputSorter inventorySorter = new TierProcessInputSorter(this);
    public BasicGasTank gasTank;
    public BasicFluidTank fluidTank;
    public MergedTank mergedTank;
    public HybridInventorySlot mergedTankSlot;
    public EnergyInventorySlot energySlot;
    public TileComponentConfig configComponent;
    public TileComponentEjector ejectorComponent;
    public int ticksRequired = BASE_TICKS_REQUIRED;
    public boolean sorting;
    private double secondaryEnergyPerTick = BASE_SECONDARY_PER_TICK;
    private double gasPerTickMeanMultiplier = 1;
    private boolean sortingNeeded = true;
    private int observedRecipeVersion = RecipeHandler.getGlobalRecipeVersion();
    private boolean recipeCachesInvalid;

    public TileEntityTierOrganicFarm(MachineTier tier) {
        super("injection", "TierOrganicFarm", tier.processes * MachineType.ORGANIC_FARM.getStorage(),
              MachineType.ORGANIC_FARM.getUsage(), 3);
        this.tier = tier;
        this.threadCount = tier.processes;
        inputSlots = new InputInventorySlot[threadCount];
        progress = new int[threadCount];
        usedSoFar = new long[threadCount];
        cachedRecipe = new FarmRecipe[threadCount];
        activeProcesses = new boolean[threadCount];
        errorProcesses = new boolean[threadCount];
        recipeCacheLookupMonitors = createRecipeCacheLookupMonitors();
        secondaryUsageSamplers = new PoissonSampler[threadCount];
        for (int lane = 0; lane < threadCount; lane++) {
            secondaryUsageSamplers[lane] = new PoissonSampler();
        }
        setSupportedUpgrade(Upgrade.SPEED);
        setSupportedUpgrade(Upgrade.ENERGY);
        setSupportedUpgrade(Upgrade.GAS);
        configComponent = new TileComponentConfig(this, TransmissionType.ITEM, TransmissionType.FLUID,
              TransmissionType.GAS, TransmissionType.ENERGY);
        initializeInventorySlots();
        configComponent.setupItemIOConfig(Arrays.asList(inputSlots), new ArrayList<>(outputSlots), energySlot, false);
        configComponent.addItemSlotInfo(DataType.EXTRA, Collections.singletonList(mergedTankSlot));
        configComponent.addFluidSlotInfo(DataType.INPUT, mergedTank.getFluidTank());
        configComponent.addGasSlotInfo(DataType.INPUT, mergedTank.getGasTank());
        configComponent.setEjecting(TransmissionType.ITEM, true);
        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(configComponent, TransmissionType.ITEM);
    }

    @SuppressWarnings("unchecked")
    private RecipeCacheLookupMonitor<FarmRecipe>[] createRecipeCacheLookupMonitors() {
        RecipeCacheLookupMonitor<FarmRecipe>[] monitors = new RecipeCacheLookupMonitor[threadCount];
        for (int lane = 0; lane < threadCount; lane++) {
            monitors[lane] = new RecipeCacheLookupMonitor<>(this, lane);
        }
        return monitors;
    }

    private IContentsListener getLaneListener(IContentsListener listener, int lane) {
        return () -> {
            listener.onContentsChanged();
            if (isLaneIndex(lane)) {
                sortingNeeded = true;
                recipeCacheLookupMonitors[lane].onChange();
            }
        };
    }

    private IContentsListener getSharedRecipeListener(@Nullable IContentsListener listener) {
        return () -> {
            if (listener != null) {
                listener.onContentsChanged();
            }
            changeRecipeCaches();
        };
    }

    private void changeRecipeCaches() {
        if (recipeCacheLookupMonitors == null) {
            return;
        }
        for (RecipeCacheLookupMonitor<FarmRecipe> monitor : recipeCacheLookupMonitors) {
            if (monitor != null) {
                monitor.onChange();
            }
        }
    }

    private void unpauseRecipeCaches() {
        if (recipeCacheLookupMonitors == null) {
            return;
        }
        for (RecipeCacheLookupMonitor<FarmRecipe> monitor : recipeCacheLookupMonitors) {
            if (monitor != null) {
                monitor.unpause();
            }
        }
    }

    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = createInventorySlotHelper();
        energySlot = builder.addSlot(EnergyInventorySlot.fillOrConvert(getMainEnergyContainer(), this::getWorld, listener,
              ENERGY_SLOT_X, ENERGY_SLOT_Y));
        for (int lane = 0; lane < threadCount; lane++) {
            int laneIndex = lane;
            InputInventorySlot input = builder.addSlot(InputInventorySlot.at(this::isRecipeItem,
                  getLaneListener(listener, lane), getLaneX(lane), PROCESS_INPUT_SLOT_Y)
                  .setAutoPullValidator((stack, side) -> {
                      ItemStack current = inputSlots[laneIndex].getStack();
                      ItemStack simulated = current.isEmpty() ? stack.copy() : current.copy();
                      if (!current.isEmpty()) {
                          simulated.grow(stack.getCount());
                      }
                      FarmRecipe recipe = getRecipeForInput(laneIndex, simulated, false);
                      return recipe != null && OutputHelper.canFitFarmOutput(outputSlots, recipe.getOutput());
                  }));
            inputSlots[lane] = input;
        }
        mergedTankSlot = builder.addSlot(HybridInventorySlot.inputOrDrainOrConvert(getOrCreateMergedTank(), this::getWorld,
              listener, MEDIUM_SLOT_X, MEDIUM_SLOT_Y));
        outputSlots.clear();
        IContentsListener outputListener = getSharedRecipeListener(listener);
        for (int i = 0; i < OUTPUT_SLOT_COUNT; i++) {
            outputSlots.add(builder.addSlot(new FarmOutputInventorySlot(threadCount, outputListener, 0, 0,
                  new mekanism.common.inventory.container.SelectedWindowData(
                        mekceumoremachine.common.ui.MoreMachineWindowTypes.ORGANIC_FARM_OUTPUT))));
        }
        return builder.build();
    }

    public int getLaneX(int lane) {
        int base = switch (threadCount) {
            case 3 -> 55;
            case 5 -> 35;
            case 7 -> 29;
            default -> 27;
        };
        int spacing = switch (threadCount) {
            case 3 -> 38;
            case 5 -> 26;
            case 7 -> 19;
            default -> 19;
        };
        return base + lane * spacing;
    }

    private boolean isRecipeItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (FarmInput input : getRecipes().keySet()) {
            if (ItemHandlerHelper.canItemStacksStack(input.itemStack, stack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected IFluidTankHolder getInitialFluidTanks(IContentsListener listener) {
        FluidTankHelper builder = createFluidTankHelper();
        builder.addTank(getOrCreateMergedTank(listener).getFluidTank());
        return builder.build();
    }

    @Override
    protected IGasTankHolder getInitialGasTanks(IContentsListener listener) {
        GasTankHelper builder = createGasTankHelper();
        builder.addTank(getOrCreateMergedTank(listener).getGasTank());
        return builder.build();
    }

    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        IContentsListener cacheListener = () -> {
            listener.onContentsChanged();
            unpauseRecipeCaches();
        };
        getMainEnergyContainer(cacheListener);
        return super.getInitialEnergyContainers(cacheListener);
    }

    private BasicGasTank getOrCreateGasTank(IContentsListener listener) {
        if (gasTank == null) {
            gasTank = BasicGasTank.input(BASE_SECONDARY_PER_TICK * 1000 * threadCount, this::isValidGas,
                  getSharedRecipeListener(listener));
        }
        return gasTank;
    }

    private BasicFluidTank getOrCreateFluidTank(IContentsListener listener) {
        if (fluidTank == null) {
            fluidTank = BasicFluidTank.input(BASE_SECONDARY_PER_TICK * 1000 * threadCount,
                  stack -> stack != null && stack.getFluid() != null && isValidFluid(stack.getFluid()),
                  getSharedRecipeListener(listener));
        }
        return fluidTank;
    }

    private MergedTank getOrCreateMergedTank() {
        return getOrCreateMergedTank(null);
    }

    private MergedTank getOrCreateMergedTank(@Nullable IContentsListener listener) {
        if (mergedTank == null) {
            mergedTank = MergedTank.create(getOrCreateFluidTank(listener), getOrCreateGasTank(listener));
        }
        return mergedTank;
    }

    private FarmInput createInput(int lane) {
        return createInput(inputSlots[lane] == null ? ItemStack.EMPTY : inputSlots[lane].getStack());
    }

    private FarmInput createInput(ItemStack item) {
        CurrentType currentType = mergedTank.getCurrentType();
        if (currentType.isGas()) {
            return new FarmInput(item, mergedTank.getGasTank().getGas());
        }
        if (currentType == CurrentType.FLUID) {
            return new FarmInput(item, mergedTank.getFluidTank().getFluid());
        }
        return new FarmInput(item, (GasStack) null);
    }

    @Nullable
    @Override
    public FarmRecipe getRecipe(int lane) {
        if (lane < 0 || lane >= threadCount) {
            return null;
        }
        FarmInput input = createInput(lane);
        if (!input.isValid()) {
            return null;
        }
        FarmRecipe recipe = cachedRecipe[lane];
        if (recipe == null || !input.testEquality(recipe.getInput())) {
            recipe = RecipeHandler.getFarmRecipe(input, getRecipes());
            cachedRecipe[lane] = recipe;
        }
        return recipe;
    }

    public Map<FarmInput, FarmRecipe> getRecipes() {
        return RecipeHandler.Recipe.ORGANIC_FARM.get();
    }

    public int getRecipeGasUsagePerOperation() {
        double usage = 3D * Math.max(1, Math.ceil(Math.max(secondaryEnergyPerTick, 0))) * Math.max(1, ticksRequired);
        return Math.max(1, MathUtils.clampToInt(usage));
    }

    private boolean processLane(int lane) {
        return recipeCacheLookupMonitors[lane].updateAndProcess();
    }

    private void handleSecondaryFuel() {
        CurrentType currentType = mergedTank.getCurrentType();
        if (currentType == CurrentType.EMPTY || currentType == CurrentType.FLUID) {
            if (mergedTankSlot.fillTank()) {
                return;
            }
        }
        if (currentType == CurrentType.EMPTY || currentType.isGas()) {
            GasInventorySlot.fillTankOrConvert(mergedTankSlot, mergedTank.getGasTank(), this::getWorld);
        }
    }

    @Override
    public void onAsyncUpdateServer() {
        super.onAsyncUpdateServer();
        if (energySlot != null) {
            energySlot.fillContainerOrConvert();
        }
        handleSecondaryFuel();
        if (sortingNeeded && sorting && hasItemInput()) {
            sortingNeeded = false;
            inventorySorter.sort();
        } else if (!sortingNeeded && areRecipeCachesInvalid()) {
            sortingNeeded = true;
        }
        Arrays.fill(activeProcesses, false);
        for (int lane = 0; lane < threadCount; lane++) {
            if (!processLane(lane)) {
                progress[lane] = 0;
                errorProcesses[lane] = false;
            }
        }
        observedRecipeVersion = RecipeHandler.getGlobalRecipeVersion();
        recipeCachesInvalid = false;
        setActive(anyLaneActive());
        prevEnergy = getEnergy();
    }

    private boolean anyLaneActive() {
        for (boolean active : activeProcesses) {
            if (active) {
                return true;
            }
        }
        return false;
    }

    private boolean hasItemInput() {
        for (InputInventorySlot inputSlot : inputSlots) {
            if (inputSlot != null && !inputSlot.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public double getScaledProgress(int lane) {
        return lane >= 0 && lane < threadCount && ticksRequired > 0 ? (double) progress[lane] / ticksRequired : 0;
    }

    public boolean isLaneActive(int lane) {
        return lane >= 0 && lane < threadCount && activeProcesses[lane];
    }

    public boolean hasLaneError(int lane) {
        return lane >= 0 && lane < threadCount && errorProcesses[lane];
    }

    private boolean isLaneIndex(int lane) {
        return lane >= 0 && lane < threadCount;
    }

    @Override
    public int getSavedOperatingTicks(int cacheIndex) {
        return isLaneIndex(cacheIndex) ? progress[cacheIndex] : 0;
    }

    @Override
    public long getSavedUsedSoFar(int cacheIndex) {
        return isLaneIndex(cacheIndex) ? usedSoFar[cacheIndex] : 0;
    }

    @Override
    public CachedRecipe<FarmRecipe> createNewCachedRecipe(FarmRecipe recipe, int cacheIndex) {
        if (!isLaneIndex(cacheIndex)) {
            return null;
        }
        GasUsageMultiplier usage = (used, operatingTicks) ->
              secondaryUsageSamplers[cacheIndex].sample(gasPerTickMeanMultiplier);
        return new ItemStackConstantFarmCachedRecipe<>(recipe, () -> false,
              InputHelper.getInputHandler(inputSlots[cacheIndex], RecipeError.NOT_ENOUGH_INPUT),
              InputHelper.getConstantGasInputHandler(mergedTank.getGasTank(), RecipeError.NOT_ENOUGH_SECONDARY_INPUT, false),
              InputHelper.getConstantFluidInputHandler(mergedTank.getFluidTank(), RecipeError.NOT_ENOUGH_SECONDARY_INPUT, false),
              OutputHelper.getFarmOutputHandler(new ArrayList<>(outputSlots), RecipeError.NOT_ENOUGH_OUTPUT_SPACE),
              usage, used -> usedSoFar[cacheIndex] = used)
              .setCanHolderFunction(() -> MekanismUtils.canFunction(this))
              .setActive(active -> activeProcesses[cacheIndex] = active)
              .setEnergyRequirements(() -> energyPerTick, getMainEnergyContainer())
              .setRequiredTicks(() -> ticksRequired)
              .setBaselineMaxOperations(() -> 1)
              .setOperatingTicksChanged(ticks -> progress[cacheIndex] = ticks)
              .setErrorsChanged(errors -> errorProcesses[cacheIndex] = !errors.isEmpty())
              .setOnFinish(this::onCachedRecipeFinish);
    }

    private void onCachedRecipeFinish() {
        sortingNeeded = true;
        markNoUpdateSync();
    }

    @Override
    public void onCachedRecipeChanged(@Nullable CachedRecipe<FarmRecipe> recipeCache, int cacheIndex) {
        if (isLaneIndex(cacheIndex)) {
            cachedRecipe[cacheIndex] = recipeCache == null ? null : recipeCache.getRecipe();
            if (recipeCache == null) {
                errorProcesses[cacheIndex] = false;
            }
        }
    }

    @Override
    public void onRecipeCacheInvalidated(int cacheIndex) {
        if (isLaneIndex(cacheIndex)) {
            cachedRecipe[cacheIndex] = null;
            recipeCachesInvalid = true;
        }
    }

    @Override
    public void clearRecipeErrors(int cacheIndex) {
        if (isLaneIndex(cacheIndex)) {
            errorProcesses[cacheIndex] = false;
        }
    }

    @Override
    public int getSorterProcessCount() {
        return threadCount;
    }

    @Nullable
    @Override
    public IInventorySlot getSorterInputSlot(int process) {
        return isLaneIndex(process) ? inputSlots[process] : null;
    }

    @Override
    public boolean sorterInputProducesOutput(int process, ItemStack fallbackInput, boolean updateCache) {
        FarmRecipe recipe = getRecipeForInput(process, fallbackInput, updateCache);
        return recipe != null && OutputHelper.canFitFarmOutput(outputSlots, recipe.getOutput());
    }

    @Override
    public int getSorterNeededInput(int process, ItemStack inputStack) {
        FarmRecipe recipe = getRecipeForInput(process, inputStack, true);
        return recipe == null ? 1 : Math.max(1, recipe.getInput().itemStack.getCount());
    }

    private FarmRecipe getRecipeForInput(int process, ItemStack inputStack, boolean updateCache) {
        if (!isLaneIndex(process) || inputStack == null || inputStack.isEmpty()) {
            return null;
        }
        FarmInput input = createInput(inputStack);
        FarmRecipe recipe = input.isValid() ? RecipeHandler.getFarmRecipe(input, getRecipes()) : null;
        if (updateCache) {
            cachedRecipe[process] = recipe;
        }
        return recipe;
    }

    @Override
    public boolean isSorterProcessLocked(int process) {
        return isLaneIndex(process) && (progress[process] > 0 || usedSoFar[process] > 0);
    }

    @Override
    public boolean isSorting() {
        return sorting;
    }

    @Override
    public boolean areRecipeCachesInvalid() {
        int recipeVersion = RecipeHandler.getGlobalRecipeVersion();
        if (observedRecipeVersion != recipeVersion) {
            recipeCachesInvalid = true;
        }
        return recipeCachesInvalid || CommonWorldTickHandler.flushTagAndRecipeCaches;
    }

    @Override
    public void onSorterChanged() {
        markNoUpdateSync();
    }

    @Override
    public TileNetworkList getNetworkedData(TileNetworkList data) {
        super.getNetworkedData(data);
        data.add(sorting);
        for (int lane = 0; lane < threadCount; lane++) {
            data.add(progress[lane]);
            data.add(activeProcesses[lane]);
            data.add(errorProcesses[lane]);
        }
        TileUtils.addTankData(data, fluidTank);
        TileUtils.addTankData(data, gasTank);
        return data;
    }

    @Override
    public void handlePacketData(ByteBuf data) {
        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            int type = data.readInt();
            if (type == 0) {
                sorting = !sorting;
                sortingNeeded = true;
            }
            return;
        }
        super.handlePacketData(data);
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            sorting = data.readBoolean();
            for (int lane = 0; lane < threadCount; lane++) {
                progress[lane] = data.readInt();
                activeProcesses[lane] = data.readBoolean();
                errorProcesses[lane] = data.readBoolean();
            }
            TileUtils.readTankData(data, fluidTank);
            TileUtils.readTankData(data, gasTank);
        }
    }

    private boolean isValidGas(@Nullable Gas gas) {
        return gas != null && RecipeHandler.Recipe.ORGANIC_FARM.containsRecipe(gas);
    }

    private boolean isValidFluid(@Nullable Fluid fluid) {
        return fluid != null && RecipeHandler.Recipe.ORGANIC_FARM.containsRecipe(fluid);
    }

    @Override
    public void recalculateUpgradables(Upgrade upgrade) {
        super.recalculateUpgradables(upgrade);
        if (upgrade == Upgrade.SPEED) {
            ticksRequired = Math.max(1, MekanismUtils.getTicks(this, BASE_TICKS_REQUIRED));
        }
        if (upgrade == Upgrade.SPEED || upgrade == Upgrade.ENERGY) {
            energyPerTick = MekanismUtils.getEnergyPerTick(this, BASE_ENERGY_PER_TICK);
        }
        if (upgrade == Upgrade.SPEED || upgrade == Upgrade.GAS) {
            recalculateSecondaryUsage();
        }
        if (!isRecalculatingAllUpgradables()) {
            unpauseRecipeCaches();
        }
    }

    @Override
    protected void onAllUpgradablesRecalculated(Set<Upgrade> upgrades) {
        super.onAllUpgradablesRecalculated(upgrades);
        if (upgrades.contains(Upgrade.SPEED) || upgrades.contains(Upgrade.ENERGY)) {
            energyPerTick = MekanismUtils.getEnergyPerTick(this, BASE_ENERGY_PER_TICK);
        }
        if (upgrades.contains(Upgrade.SPEED) || upgrades.contains(Upgrade.GAS)) {
            recalculateSecondaryUsage();
        }
        if (!upgrades.isEmpty()) {
            unpauseRecipeCaches();
        }
    }

    private void recalculateSecondaryUsage() {
        secondaryEnergyPerTick = MekanismUtils.getSecondaryEnergyPerTickMean(this, BASE_SECONDARY_PER_TICK);
        gasPerTickMeanMultiplier = MekanismUtils.getGasPerTickMeanMultiplier(this);
    }

    @Override
    public void setEnergy(double energy) {
        double previous = getEnergy();
        super.setEnergy(energy);
        if (world != null && !world.isRemote && Double.compare(previous, getEnergy()) != 0) {
            unpauseRecipeCaches();
        }
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
    public Object[] getManagedTanks() {
        return new Object[]{mergedTank.getFluidTank(), mergedTank.getGasTank()};
    }

    @Override
    public MachineTier getTier() {
        return tier;
    }

    @Override
    public boolean getsorting() {
        return sorting;
    }

    @Override
    public IBlockState getUpgradeResult(BaseTier upgradeTier) {
        return tier.canUpgradeTo(upgradeTier) ?
              MEKCeuMoreMachineBlocks.TierOrganicFarm.getStateFromMeta(MachineTier.get(upgradeTier).ordinal()) : null;
    }

    @Override
    public IUpgradeData getUpgradeData(BaseTier upgradeTier) {
        return tier.canUpgradeTo(upgradeTier) ? new OrganicFarmUpgradeData(upgradeTier, this) : null;
    }

    @Override
    public boolean parseUpgradeData(IUpgradeData upgradeData) {
        if (!(upgradeData instanceof OrganicFarmUpgradeData data) || data.getUpgradeTier() != tier.getBaseTier()) {
            return false;
        }
        LargeMachineUpgradeDataApplier.applyCommonWithoutInventory(this, data, upgradeComponent, securityComponent);
        prevEnergy = data.prevEnergy;
        energySlot.setStack(LargeMachineUpgradeData.copyStack(data.energySlot));
        mergedTankSlot.setStack(LargeMachineUpgradeData.copyStack(data.mediumSlot));
        for (int i = 0; i < Math.min(inputSlots.length, data.inputSlots.length); i++) {
            inputSlots[i].setStack(LargeMachineUpgradeData.copyStack(data.inputSlots[i]));
        }
        for (int i = 0; i < Math.min(outputSlots.size(), data.outputSlots.length); i++) {
            outputSlots.get(i).setStack(LargeMachineUpgradeData.copyStack(data.outputSlots[i]));
        }
        System.arraycopy(data.progress, 0, progress, 0, Math.min(progress.length, data.progress.length));
        System.arraycopy(data.usedSoFar, 0, usedSoFar, 0, Math.min(usedSoFar.length, data.usedSoFar.length));
        sorting = data.sorting;
        gasTank.setGas(data.gas == null ? null : data.gas.copy());
        fluidTank.setFluid(data.fluid == null ? null : data.fluid.copy());
        sanitizeTanks();
        configComponent.read(data.configComponentData.copy());
        ejectorComponent.read(data.ejectorComponentData.copy());
        ejectorComponent.setOutputData(configComponent, TransmissionType.ITEM);
        sortingNeeded = true;
        changeRecipeCaches();
        LargeMachineUpgradeDataApplier.finish(this, upgradeComponent);
        if (world != null && !world.isRemote) {
            tryPlaceBoundingBlocks(world, Coord4D.get(this));
        }
        return true;
    }

    @Override
    public boolean applyTierUpgrade(BaseTier upgradeTier) {
        IUpgradeData data = getUpgradeData(upgradeTier);
        IBlockState state = getUpgradeResult(upgradeTier);
        return data != null && state != null && UpgradeUtils.replaceTileForUpgrade(this, state, data);
    }

    @Override
    public int getBlockGuiID(Block block, int metadata) {
        return 22;
    }

    @Override
    public IGuiProvider guiProvider() {
        return MEKCeuMoreMachine.proxy;
    }

    @Nonnull
    @Override
    public String getName() {
        return LangUtils.localize("tile.TierOrganicFarm." + tier.getBaseTier().getSimpleName() + ".name");
    }

    @Override
    public void writeCustomNBT(NBTTagCompound nbt) {
        super.writeCustomNBT(nbt);
        nbt.setBoolean("sorting", sorting);
        for (int i = 0; i < threadCount; i++) {
            nbt.setInteger("progress" + i, progress[i]);
            nbt.setLong("usedSoFar" + i, usedSoFar[i]);
        }
    }

    @Override
    public void readCustomNBT(NBTTagCompound nbt) {
        super.readCustomNBT(nbt);
        sorting = nbt.getBoolean("sorting");
        for (int i = 0; i < threadCount; i++) {
            progress[i] = Math.min(Math.max(0, nbt.getInteger("progress" + i)), ticksRequired);
            usedSoFar[i] = Math.max(0, nbt.getLong("usedSoFar" + i));
        }
        sanitizeTanks();
        sortingNeeded = true;
    }

    @Override
    public int getRedstoneLevel() {
        return Container.calcRedstoneFromInventory(this);
    }

    @Override
    public void writeSustainedData(ItemStack stack) {
        if (mergedTank.getCurrentType().isGas() && gasTank.getGas() != null) {
            ItemDataUtils.setCompound(stack, "gasTank", gasTank.getGas().write(new NBTTagCompound()));
        } else if (mergedTank.getCurrentType() == CurrentType.FLUID && fluidTank.getFluid() != null) {
            ItemDataUtils.setCompound(stack, "fluidTank", fluidTank.getFluid().writeToNBT(new NBTTagCompound()));
        }
    }

    @Override
    public void readSustainedData(ItemStack stack) {
        if (gasTank != null) {
            gasTank.setGas(GasStack.readFromNBT(ItemDataUtils.getCompound(stack, "gasTank")));
        }
        if (fluidTank != null) {
            fluidTank.setFluid(FluidStack.loadFluidStackFromNBT(ItemDataUtils.getCompound(stack, "fluidTank")));
        }
        sanitizeTanks();
    }

    private void sanitizeTanks() {
        FluidStack fluid = fluidTank == null ? null : fluidTank.getFluid();
        if (fluid != null && (fluid.getFluid() == null || fluid.amount <= 0)) {
            fluidTank.setEmpty();
        } else if (fluid != null) {
            fluidTank.setStackSize(fluid.amount, Action.EXECUTE);
        }
        GasStack gas = gasTank == null ? null : gasTank.getGas();
        if (gas != null && (gas.getGas() == null || gas.amount <= 0)) {
            gasTank.setEmpty();
        } else if (gas != null) {
            gasTank.setStackSize(gas.amount, Action.EXECUTE);
        }
        if (gasTank != null && fluidTank != null && !gasTank.isEmpty() && !fluidTank.isEmpty()) {
            fluidTank.setEmpty();
        }
    }

    @Override
    public void collectBoundingBlocks(BiConsumer<BlockPos, Boolean> consumer) {
        consumer.accept(getPos().up(), false);
    }

    @Override
    public void onPlace() {
        tryPlaceBoundingBlocks(world, Coord4D.get(this));
    }

    @Override
    public void onBreak() {
        removeBoundingBlocks(world, getPos());
    }
}
