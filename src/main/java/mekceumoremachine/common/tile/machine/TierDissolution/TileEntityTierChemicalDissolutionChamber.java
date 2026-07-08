package mekceumoremachine.common.tile.machine.TierDissolution;

import io.netty.buffer.ByteBuf;
import mekanism.api.IConfigCardAccess.ISpecialConfigData;
import mekanism.api.IContentsListener;
import mekanism.api.TileNetworkList;
import mekanism.api.gas.*;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.math.MathUtils;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.Mekanism;
import mekanism.common.MekanismFluids;
import mekanism.common.CommonWorldTickHandler;
import mekanism.common.Upgrade;
import mekanism.common.base.*;
import mekanism.common.block.states.BlockStateMachine.MachineType;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.gas.GasTankHelper;
import mekanism.common.capabilities.holder.gas.IGasTankHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.config.MekanismConfig;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.inventory.slot.gas.GasInventorySlot;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.cache.CachedRecipe;
import mekanism.common.recipe.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.common.recipe.cache.IRecipeLookupHandler;
import mekanism.common.recipe.cache.ItemStackConstantGasCachedRecipe;
import mekanism.common.recipe.cache.RecipeCacheLookupMonitor;
import mekanism.common.recipe.cache.inputs.InputHelper;
import mekanism.common.recipe.cache.outputs.OutputHelper;
import mekanism.common.recipe.inputs.ItemStackInput;
import mekanism.common.recipe.machines.DissolutionRecipe;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.prefab.TileEntityMachine;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.util.*;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.capability.ResizableGasTank;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.machine.TierProcessInputSorter;
import mekceumoremachine.common.tile.interfaces.INeedRepeatTierUpgrade;
import mekceumoremachine.common.tile.interfaces.ITierSorting;
import mekceumoremachine.common.upgrade.FirstChemicalDissolutionChamberUpgradeData;
import mekceumoremachine.common.upgrade.LargeMachineUpgradeData;
import mekceumoremachine.common.upgrade.LargeMachineUpgradeDataApplier;
import mekceumoremachine.common.upgrade.RepeatedChemicalDissolutionChamberUpgradeData;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TileEntityTierChemicalDissolutionChamber extends TileEntityMachine implements ISustainedData, ITankManager, ISpecialConfigData, IComparatorSupport, ISideConfiguration, INeedRepeatTierUpgrade<MachineTier>, ITierSorting, ISpecialSelectionWireframeTile, IRecipeLookupHandler<DissolutionRecipe>, IRecipeLookupHandler.ConstantUsageRecipeLookupHandler, TierProcessInputSorter.Context {

    public static final int MAX_GAS = 10000;
    public static final int BASE_INJECT_USAGE = 1;
    public static final int BASE_TICKS_REQUIRED = 100;
    public final double BASE_ENERGY_USAGE = MachineType.CHEMICAL_DISSOLUTION_CHAMBER.getUsage();
    public ResizableGasTank injectTank;
    public ResizableGasTank outputTank1;
    public ResizableGasTank outputTank2;
    public ResizableGasTank outputTank3;
    public ResizableGasTank outputTank4;
    public ResizableGasTank outputTank5;
    public ResizableGasTank outputTank6;
    public ResizableGasTank outputTank7;
    public ResizableGasTank outputTank8;
    public ResizableGasTank outputTank9;
    public double injectUsage = BASE_INJECT_USAGE;
    public int injectUsageThisTick;
    public int[] progress;
    public long[] usedSoFar;
    public int ticksRequired = BASE_TICKS_REQUIRED;
    public DissolutionRecipe[] cachedRecipe;
    private final RecipeCacheLookupMonitor<DissolutionRecipe>[] recipeCacheLookupMonitors;
    private final boolean[] activeProcesses;
    private long baseTotalUsage = BASE_TICKS_REQUIRED;
    public ResizableGasTank[] outPutTanks;
    public MachineTier tier;
    public TileComponentEjector ejectorComponent;
    public TileComponentConfig configComponent;
    public boolean sorting;
    private final TierProcessInputSorter inventorySorter = new TierProcessInputSorter(this);
    private boolean sortingNeeded = true;
    private int observedRecipeVersion = RecipeHandler.getGlobalRecipeVersion();
    private boolean recipeCachesInvalid;
    public float prevScale;
    public int updateDelay;
    public boolean needsPacket;
    public boolean upgraded;
    private final List<IInventorySlot> inputSlots = new ArrayList<>();
    private GasInventorySlot injectSlot;
    private EnergyInventorySlot energySlot;

    public TileEntityTierChemicalDissolutionChamber(MachineTier type) {
        super("dissolution", "TierChemicalDissolutionChamber", type.processes * MachineType.CHEMICAL_DISSOLUTION_CHAMBER.getStorage(), MachineType.CHEMICAL_DISSOLUTION_CHAMBER.getUsage(), 2);
        this.tier = type;
        progress = new int[tier.processes];
        isActive = false;
        cachedRecipe = new DissolutionRecipe[tier.processes];
        usedSoFar = new long[tier.processes];
        activeProcesses = new boolean[tier.processes];
        recipeCacheLookupMonitors = createRecipeCacheLookupMonitors();
        outPutTanks = new ResizableGasTank[9];

        upgradeComponent.setSupported(Upgrade.GAS);
        upgradeComponent.getSupportedTypes().forEach(this::recalculateUpgradables);

        configComponent = new TileComponentConfig(this, TransmissionType.ITEM, TransmissionType.GAS, TransmissionType.ENERGY);
        initializeInventorySlots();
        configComponent.addItemSlotInfo(DataType.EXTRA, injectSlot);
        configComponent.addItemSlotInfo(DataType.ENERGY, energySlot);
        configComponent.addItemSlotInfo(DataType.INPUT, inputSlots);
        configComponent.addItemSlotInfo(DataType.INPUT_ENHANCED, inputSlots);
        configComponent.setConfig(TransmissionType.ITEM, DataType.EXTRA, DataType.INPUT, DataType.INPUT, DataType.ENERGY, DataType.INPUT, DataType.NONE);
        configComponent.setCanEject(TransmissionType.ITEM, false);

        configComponent.addGasSlotInfo(DataType.INPUT, injectTank);
        configComponent.addGasSlotInfo(DataType.OUTPUT, getOutputTanks());
        configComponent.addGasSlotInfo(DataType.INPUT_OUTPUT, getAllGasTanks(), getOutputTanks());
        configComponent.setConfig(TransmissionType.GAS, DataType.INPUT, DataType.INPUT, DataType.INPUT, DataType.INPUT, DataType.INPUT, DataType.OUTPUT);
        configComponent.setEjecting(TransmissionType.GAS, true);

        configComponent.setInputConfig(TransmissionType.ENERGY);

        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(configComponent, TransmissionType.GAS)
              .setCanTankEject(tank -> tank != injectTank);

    }

    @SuppressWarnings("unchecked")
    private RecipeCacheLookupMonitor<DissolutionRecipe>[] createRecipeCacheLookupMonitors() {
        RecipeCacheLookupMonitor<DissolutionRecipe>[] monitors = new RecipeCacheLookupMonitor[tier.processes];
        for (int process = 0; process < tier.processes; process++) {
            monitors[process] = new RecipeCacheLookupMonitor<>(this, process);
        }
        return monitors;
    }

    private IContentsListener getRecipeCacheChangeListener(IContentsListener listener, int process) {
        return () -> {
            listener.onContentsChanged();
            if (isProcessIndex(process)) {
                markSortingNeeded();
                recipeCacheLookupMonitors[process].onChange();
            }
        };
    }

    private IContentsListener getSharedTankListener(IContentsListener listener) {
        return () -> {
            listener.onContentsChanged();
            for (RecipeCacheLookupMonitor<DissolutionRecipe> monitor : recipeCacheLookupMonitors) {
                monitor.onChange();
            }
        };
    }

    private IContentsListener getOutputTankListener(IContentsListener listener, int process) {
        return () -> {
            listener.onContentsChanged();
            if (isProcessIndex(process)) {
                recipeCacheLookupMonitors[process].onChange();
            }
        };
    }

    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = createInventorySlotHelper();
        inputSlots.clear();
        injectSlot = builder.addSlot(GasInventorySlot.fill(injectTank, listener, 8, 65));
        injectSlot.setSlotType(ContainerSlotType.EXTRA);
        injectSlot.setSlotOverlay(SlotOverlay.MINUS);
        energySlot = builder.addSlot(EnergyInventorySlot.fillOrConvert(getMainEnergyContainer(), this::getWorld, listener, 8, 13));
        for (int process = 0; process < 9; process++) {
            int processIndex = process;
            InputInventorySlot inputSlot = builder.addSlot(InputInventorySlot.at(stack -> RecipeHandler.getDissolutionRecipe(new ItemStackInput(stack)) != null,
                  getRecipeCacheChangeListener(listener, process), getProcessSlotX(process), 13));
            inputSlot.setEnabledSupplier(() -> isProcessIndex(processIndex));
            inputSlots.add(inputSlot);
        }
        return builder.build();
    }

    @Override
    protected IGasTankHolder getInitialGasTanks(IContentsListener listener) {
        GasTankHelper builder = createGasTankHelper();
        builder.addTank(getOrCreateInjectTank(listener));
        for (int process = 0; process < outPutTanks.length; process++) {
            ResizableGasTank tank = getOrCreateOutputTank(process, listener);
            if (isProcessIndex(process)) {
                builder.addTank(tank);
            }
        }
        return builder.build();
    }

    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        return super.getInitialEnergyContainers(() -> {
            listener.onContentsChanged();
            unpauseRecipeCaches();
        });
    }

    private void unpauseRecipeCaches() {
        if (recipeCacheLookupMonitors != null) {
            for (RecipeCacheLookupMonitor<DissolutionRecipe> monitor : recipeCacheLookupMonitors) {
                if (monitor != null) {
                    monitor.unpause();
                }
            }
        }
    }

    private ResizableGasTank getOrCreateInjectTank(IContentsListener listener) {
        if (injectTank == null) {
            injectTank = ResizableGasTank.input(MAX_GAS * tier.processes, this::isValidGas, getSharedTankListener(listener));
        }
        return injectTank;
    }

    private ResizableGasTank getOrCreateOutputTank(int process, IContentsListener listener) {
        ResizableGasTank tank = switch (process) {
            case 0 -> outputTank1 == null ? outputTank1 = ResizableGasTank.output(MAX_GAS, getOutputTankListener(listener, process)) : outputTank1;
            case 1 -> outputTank2 == null ? outputTank2 = ResizableGasTank.output(MAX_GAS, getOutputTankListener(listener, process)) : outputTank2;
            case 2 -> outputTank3 == null ? outputTank3 = ResizableGasTank.output(MAX_GAS, getOutputTankListener(listener, process)) : outputTank3;
            case 3 -> outputTank4 == null ? outputTank4 = ResizableGasTank.output(MAX_GAS, getOutputTankListener(listener, process)) : outputTank4;
            case 4 -> outputTank5 == null ? outputTank5 = ResizableGasTank.output(MAX_GAS, getOutputTankListener(listener, process)) : outputTank5;
            case 5 -> outputTank6 == null ? outputTank6 = ResizableGasTank.output(MAX_GAS, getOutputTankListener(listener, process)) : outputTank6;
            case 6 -> outputTank7 == null ? outputTank7 = ResizableGasTank.output(MAX_GAS, getOutputTankListener(listener, process)) : outputTank7;
            case 7 -> outputTank8 == null ? outputTank8 = ResizableGasTank.output(MAX_GAS, getOutputTankListener(listener, process)) : outputTank8;
            case 8 -> outputTank9 == null ? outputTank9 = ResizableGasTank.output(MAX_GAS, getOutputTankListener(listener, process)) : outputTank9;
            default -> throw new IllegalArgumentException("Invalid process " + process);
        };
        outPutTanks[process] = tank;
        return tank;
    }

    private List<IExtendedGasTank> getOutputTanks() {
        List<IExtendedGasTank> tanks = new ArrayList<>(tier.processes);
        for (int process = 0; process < tier.processes; process++) {
            tanks.add(outPutTanks[process]);
        }
        return tanks;
    }

    private List<IExtendedGasTank> getAllGasTanks() {
        List<IExtendedGasTank> tanks = new ArrayList<>();
        tanks.add(injectTank);
        tanks.addAll(getOutputTanks());
        return tanks;
    }

    private int getProcessSlotX(int process) {
        int slotLocation = tier == MachineTier.BASIC ? 55 : tier == MachineTier.ADVANCED ? 35 : tier == MachineTier.ELITE ? 29 : 27;
        int xDistance = tier == MachineTier.BASIC ? 38 : tier == MachineTier.ADVANCED ? 26 : 19;
        return slotLocation + process * xDistance;
    }

    @Override
    public void onUpdateClient() {
        super.onUpdateClient();
        if (updateDelay > 0) {
            updateDelay--;
            if (updateDelay == 0) {
                MekanismUtils.updateBlock(world, getPos());
            }
        }
        float targetScale = (float) (injectTank.getGas() != null ? injectTank.getGas().amount : 0) / injectTank.getMaxGas();
        if (Math.abs(prevScale - targetScale) > 0.01) {
            prevScale = (9 * prevScale + targetScale) / 10;
        }
    }

    @Override
    public void setActive(boolean active) {
        super.setActive(active);
        if (updateDelay == 0) {
            Mekanism.packetHandler.sendUpdatePacket(this);
            updateDelay = 10;
        }
    }

    @Override
    public void onAsyncUpdateServer() {
        super.onAsyncUpdateServer();
        if (updateDelay > 0) {
            updateDelay--;
            if (updateDelay == 0) {
                needsPacket = true;
            }
        }
        energySlot.fillContainerOrConvert();
        injectSlot.fillTank();
        injectUsageThisTick = Math.max(BASE_INJECT_USAGE, StatUtils.inversePoisson(injectUsage));
     //   injectUsageThisTick *= tier.processes;
        if (shouldSortInventory()) {
            markSortingNotNeeded();
            sortInventory();
        } else if (!sortingNeeded && areRecipeCachesInvalid()) {
            markSortingNeeded();
        }

        Arrays.fill(activeProcesses, false);
        for (int process = 0; process < tier.processes; process++) {
            if (!processRecipe(process)) {
                progress[process] = 0;
            }
        }
        markRecipeCachesObserved();
        updateActiveState();
        prevEnergy = getEnergy();
        if (needsPacket) {
            Mekanism.packetHandler.sendUpdatePacket(this);
        }
        needsPacket = false;
    }


    private boolean isProcessIndex(int process) {
        return process >= 0 && process < tier.processes;
    }

    private boolean processRecipe(int process) {
        RecipeCacheLookupMonitor<DissolutionRecipe> monitor = recipeCacheLookupMonitors[process];
        monitor.unpause();
        return monitor.updateAndProcess();
    }

    private boolean shouldSortInventory() {
        return sortingNeeded && isSorting() && hasItemInput();
    }

    private boolean hasItemInput() {
        for (int process = 0; process < tier.processes; process++) {
            IInventorySlot inputSlot = getProcessInputSlot(process);
            if (inputSlot != null && !inputSlot.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void sortInventory() {
        inventorySorter.sort();
    }

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

    private void markRecipeCachesObserved() {
        observedRecipeVersion = RecipeHandler.getGlobalRecipeVersion();
        recipeCachesInvalid = false;
    }

    private void markSortingNeeded() {
        sortingNeeded = true;
    }

    private void markSortingNotNeeded() {
        sortingNeeded = false;
    }

    private void setProcessActive(int process, boolean active) {
        activeProcesses[process] = active;
    }

    private void updateActiveState() {
        boolean active = false;
        for (boolean processActive : activeProcesses) {
            if (processActive) {
                active = true;
                break;
            }
        }
        if (active || prevEnergy >= getEnergy()) {
            setActive(active);
        }
    }

    private int getMaxOperationsPerTick() {
        int maxOperations = Math.min((int) Math.pow(2, upgradeComponent.getUpgrades(Upgrade.SPEED)), MekanismConfig.current().mekce.MAXspeedmachines.val());
        return Math.max(maxOperations, 1);
    }

    private long getGasUsage(long usedSoFar, int operatingTicks) {
        long baseRemaining = baseTotalUsage - usedSoFar;
        int remainingTicks = ticksRequired - operatingTicks;
        if (remainingTicks <= 0) {
            return Math.max(baseRemaining, 0);
        } else if (baseRemaining < remainingTicks) {
            return 0;
        } else if (baseRemaining == remainingTicks) {
            return 1;
        }
        return Math.max(MathUtils.clampToLong(baseRemaining / (double) remainingTicks), 0);
    }

    protected void onCachedRecipeFinish() {
        markNoUpdateSync();
    }

    @Override
    public int getSavedOperatingTicks(int cacheIndex) {
        return isProcessIndex(cacheIndex) ? progress[cacheIndex] : 0;
    }

    @Override
    public long getSavedUsedSoFar(int cacheIndex) {
        return isProcessIndex(cacheIndex) ? usedSoFar[cacheIndex] : 0;
    }

    @Override
    public DissolutionRecipe getRecipe(int cacheIndex) {
        if (!isProcessIndex(cacheIndex)) {
            return null;
        }
        IInventorySlot slot = getProcessInputSlot(cacheIndex);
        if (slot == null || slot.getStack().isEmpty()) {
            return null;
        }
        ItemStackInput input = new ItemStackInput(slot.getStack());
        if (cachedRecipe[cacheIndex] == null || !input.testEquality(cachedRecipe[cacheIndex].getInput())) {
            cachedRecipe[cacheIndex] = RecipeHandler.getDissolutionRecipe(input);
        }
        return cachedRecipe[cacheIndex];
    }

    @Override
    public CachedRecipe<DissolutionRecipe> createNewCachedRecipe(DissolutionRecipe recipe, int cacheIndex) {
        IInventorySlot inputSlot = getProcessInputSlot(cacheIndex);
        return new ItemStackConstantGasCachedRecipe<>(recipe, () -> false,
              InputHelper.getInputHandler(inputSlot, RecipeError.NOT_ENOUGH_INPUT),
              InputHelper.getConstantGasInputHandler(injectTank, RecipeError.NOT_ENOUGH_SECONDARY_INPUT, false),
              OutputHelper.getGasOutputHandler(outPutTanks[cacheIndex], RecipeError.NOT_ENOUGH_OUTPUT_SPACE),
              this::getGasUsage, used -> usedSoFar[cacheIndex] = used)
              .setCanHolderFunction(() -> MekanismUtils.canFunction(this))
              .setActive(active -> setProcessActive(cacheIndex, active))
              .setEnergyRequirements(() -> energyPerTick, getMainEnergyContainer())
              .setRequiredTicks(() -> ticksRequired)
              .setBaselineMaxOperations(this::getMaxOperationsPerTick)
              .setOperatingTicksChanged(ticks -> progress[cacheIndex] = ticks)
              .setOnFinish(this::onCachedRecipeFinish);
    }

    @Override
    public void onCachedRecipeChanged(CachedRecipe<DissolutionRecipe> recipeCache, int cacheIndex) {
        if (isProcessIndex(cacheIndex)) {
            cachedRecipe[cacheIndex] = recipeCache == null ? null : recipeCache.getRecipe();
        }
    }

    @Override
    public void onRecipeCacheInvalidated(int cacheIndex) {
        if (isProcessIndex(cacheIndex)) {
            cachedRecipe[cacheIndex] = null;
        }
    }


    @Override
    public boolean sideIsConsumer(EnumFacing side) {
        return configComponent.hasSideForData(TransmissionType.ENERGY, facing, DataType.ENERGY, side);
    }


    public DissolutionRecipe getDissolutionRecipe(ItemStack input) {
        return RecipeHandler.getDissolutionRecipe(new ItemStackInput(input));
    }

    public boolean inputProducesOutput(int process, ItemStack fallbackInput, boolean updateCache) {
        return getRecipeForInput(process, fallbackInput, updateCache) != null;
    }

    public DissolutionRecipe getRecipeForInput(int process, ItemStack fallbackInput, boolean updateCache) {
        if (!isProcessIndex(process)) {
            return null;
        }
        DissolutionRecipe cached = cachedRecipe[process];
        if (cached == null) {
            cached = getDissolutionRecipe(fallbackInput);
            if (updateCache) {
                cachedRecipe[process] = cached;
            }
        } else {
            ItemStack recipeInput = cached.recipeInput.ingredient;
            if (recipeInput.isEmpty() || !ItemStack.areItemsEqual(recipeInput, fallbackInput)) {
                cached = getDissolutionRecipe(fallbackInput);
                if (updateCache) {
                    cachedRecipe[process] = cached;
                }
            }
        }
        return cached;
    }

    public double getScaledProgress(int process) {
        return Math.max(Math.min( (double)progress[process] / ticksRequired, 1.0D),0.0D);
    }

    @Override
    public void handlePacketData(ByteBuf dataStream) {
        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            int type = dataStream.readInt();
            if (type == 0) {
                sorting = !sorting;
                markSortingNeeded();
            }
            return;
        }

        super.handlePacketData(dataStream);
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            sorting = dataStream.readBoolean();
            upgraded = dataStream.readBoolean();
            for (int i = 0; i < tier.processes; i++) {
                progress[i] = dataStream.readInt();
            }
            injectTank.setMaxGas(MAX_GAS * tier.processes);
            TileUtils.readTankData(dataStream, injectTank);
            for (GasTank outPutTank : outPutTanks) {
                TileUtils.readTankData(dataStream, outPutTank);
            }
            if (upgraded) {
                markNoUpdateSync();
                MekanismUtils.updateBlock(world, getPos());
                upgraded = false;
            }
        }
    }

    @Override
    public TileNetworkList getNetworkedData(TileNetworkList data) {
        super.getNetworkedData(data);
        data.add(sorting);
        data.add(upgraded);
        data.add(progress);
        TileUtils.addTankData(data, injectTank);
        for (GasTank outPutTank : outPutTanks) {
            TileUtils.addTankData(data, outPutTank);
        }
        upgraded = false;
        return data;
    }


    @Override
    public void readCustomNBT(NBTTagCompound nbtTags) {
        super.readCustomNBT(nbtTags);
        sorting = nbtTags.getBoolean("sorting");
        for (int i = 0; i < tier.processes; i++) {
            progress[i] = nbtTags.getInteger("progress" + i);
            usedSoFar[i] = nbtTags.getLong("usedSoFar" + i);
        }
        injectTank.read(nbtTags.getCompoundTag("injectTank"));
        for (int i = 0; i < outPutTanks.length; i++) {
            outPutTanks[i].read(nbtTags.getCompoundTag("outputTank" + i));
        }
    }

    @Override
    public void writeCustomNBT(NBTTagCompound nbtTags) {
        super.writeCustomNBT(nbtTags);
        nbtTags.setBoolean("sorting", sorting);
        for (int i = 0; i < tier.processes; i++) {
            nbtTags.setInteger("progress" + i, progress[i]);
            nbtTags.setLong("usedSoFar" + i, usedSoFar[i]);
        }
        nbtTags.setTag("injectTank", injectTank.write(new NBTTagCompound()));
        for (int i = 0; i < outPutTanks.length; i++) {
            nbtTags.setTag("outputTank" + i, outPutTanks[i].write(new NBTTagCompound()));
        }
    }


    private IInventorySlot getProcessInputSlot(int process) {
        return isProcessIndex(process) && process < inputSlots.size() ? inputSlots.get(process) : null;
    }

    @Override
    public int getSorterProcessCount() {
        return tier.processes;
    }

    @Override
    public IInventorySlot getSorterInputSlot(int process) {
        return getProcessInputSlot(process);
    }

    @Override
    public boolean sorterInputProducesOutput(int process, ItemStack fallbackInput, boolean updateCache) {
        DissolutionRecipe recipe = getRecipeForInput(process, fallbackInput, updateCache);
        return recipe != null && recipeOutputFits(process, recipe);
    }

    private boolean recipeOutputFits(int process, DissolutionRecipe recipe) {
        return outPutTanks[process] == null || recipe.getOutput().applyOutputs(outPutTanks[process], false, 1);
    }

    @Override
    public int getSorterNeededInput(int process, ItemStack inputStack) {
        DissolutionRecipe recipe = getRecipeForInput(process, inputStack, true);
        return recipe == null ? 1 : Math.max(1, recipe.recipeInput.ingredient.getCount());
    }

    @Override
    public boolean isSorterProcessLocked(int process) {
        return isProcessIndex(process) && (progress[process] > 0 || usedSoFar[process] > 0);
    }

    @Override
    public void onSorterChanged() {
        markNoUpdateSync();
    }

    @Nonnull
    @Override
    public String getName() {
        return LangUtils.localize("tile.TierChemicalDissolutionChamber." + tier.getBaseTier().getSimpleName() + ".name");
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

    private boolean isValidGas(Gas gas) {
        return gas == MekanismFluids.SulfuricAcid;
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, EnumFacing side) {
        if (isCapabilityDisabled(capability, side)) {
            return false;
        }
        return capability == Capabilities.CONFIG_CARD_CAPABILITY || capability == Capabilities.SPECIAL_CONFIG_DATA_CAPABILITY || super.hasCapability(capability, side);
    }


    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, EnumFacing side) {
        if (isCapabilityDisabled(capability, side)) {
            return null;
        } else if (capability == Capabilities.CONFIG_CARD_CAPABILITY) {
            return Capabilities.CONFIG_CARD_CAPABILITY.cast(this);
        } else if (capability == Capabilities.SPECIAL_CONFIG_DATA_CAPABILITY) {
            return Capabilities.SPECIAL_CONFIG_DATA_CAPABILITY.cast(this);
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void recalculateUpgradables(Upgrade upgrade) {
        super.recalculateUpgradables(upgrade);
        if (upgrade == Upgrade.ENERGY) {
            energyPerTick = MekanismUtils.getEnergyPerTick(this, BASE_ENERGY_PER_TICK); // incorporate speed upgrades
        } else if (upgrade == Upgrade.GAS) {
            injectUsage = MekanismUtils.getSecondaryEnergyPerTickMean(this, BASE_INJECT_USAGE);
            baseTotalUsage = MekanismUtils.getBaseUsage(this, BASE_INJECT_USAGE);
        } else if (upgrade == Upgrade.SPEED) {
            ticksRequired = MekanismUtils.getTicks(this, BASE_TICKS_REQUIRED);
            energyPerTick = MekanismUtils.getEnergyPerTick(this, BASE_ENERGY_USAGE);
            injectUsage = MekanismUtils.getSecondaryEnergyPerTickMean(this, BASE_INJECT_USAGE);
            baseTotalUsage = MekanismUtils.getBaseUsage(this, BASE_INJECT_USAGE);
        }
        if (world != null && !world.isRemote) {
            unpauseRecipeCaches();
        }
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
    public Object[] getManagedTanks() {
        return switch (tier) {
            case BASIC -> new Object[]{injectTank, outputTank1, outputTank2, outputTank3};
            case ADVANCED -> new Object[]{injectTank, outputTank1, outputTank2, outputTank3, outputTank4, outputTank5};
            case ELITE ->
                    new Object[]{injectTank, outputTank1, outputTank2, outputTank3, outputTank4, outputTank5, outputTank6, outputTank7};
            case ULTIMATE ->
                    new Object[]{injectTank, outputTank1, outputTank2, outputTank3, outputTank4, outputTank5, outputTank6, outputTank7, outputTank8, outputTank9};
        };
    }

    @Override
    public NBTTagCompound getConfigurationData(NBTTagCompound nbtTags) {
        nbtTags.setBoolean("sorting", sorting);
        return nbtTags;
    }

    @Override
    public void setConfigurationData(NBTTagCompound nbtTags) {
        sorting = nbtTags.getBoolean("sorting");
        markSortingNeeded();
    }

    @Override
    public String getDataType() {
        return getName();
    }

    @Override
    public void writeSustainedData(ItemStack itemStack) {
        if (injectTank.getGas() != null) {
            ItemDataUtils.setCompound(itemStack, "injectTank", injectTank.getGas().write(new NBTTagCompound()));
        }
        for (int i = 0; i < outPutTanks.length; i++) {
            GasTank tank = outPutTanks[i];
            if (tank.getGas() != null) {
                ItemDataUtils.setCompound(itemStack, "outputTank" + i, tank.getGas().write(new NBTTagCompound()));
            }
        }
    }


    @Override
    public void readSustainedData(ItemStack itemStack) {
        injectTank.setGas(GasStack.readFromNBT(ItemDataUtils.getCompound(itemStack, "injectTank")));
        for (int i = 0; i < outPutTanks.length; i++) {
            outPutTanks[i].setGas(GasStack.readFromNBT(ItemDataUtils.getCompound(itemStack, "outputTank" + i)));
        }
    }

    @Override
    public int getRedstoneLevel() {
        return Container.calcRedstoneFromInventory(this);
    }

    public boolean isUpgrade = true;

    @Override
    public IBlockState getUpgradeResult(BaseTier upgradeTier) {
        return tier.canUpgradeTo(upgradeTier) ? MEKCeuMoreMachineBlocks.TierChemicalDissolutionChamber.getStateFromMeta(MachineTier.get(upgradeTier).ordinal()) : null;
    }

    @Override
    public void prepareForUpgrade() {
        isUpgrade = false;
    }

    @Override
    public IUpgradeData getUpgradeData(BaseTier upgradeTier) {
        return tier.canUpgradeTo(upgradeTier) ? new RepeatedChemicalDissolutionChamberUpgradeData(upgradeTier, this) : null;
    }

    @Override
    public boolean parseUpgradeData(IUpgradeData upgradeData) {
        if (upgradeData instanceof FirstChemicalDissolutionChamberUpgradeData data && data.getUpgradeTier() == tier.getBaseTier()) {
            return applyFirstUpgradeSnapshot(data);
        }
        if (upgradeData instanceof RepeatedChemicalDissolutionChamberUpgradeData data && data.getUpgradeTier() == tier.getBaseTier()) {
            return applyUpgradeData(data);
        }
        return false;
    }

    @Override
    public boolean applyRepeatedTierUpgrade(BaseTier upgradeTier) {
        IUpgradeData upgradeData = getUpgradeData(upgradeTier);
        IBlockState upgradeResult = getUpgradeResult(upgradeTier);
        return upgradeData != null && upgradeResult != null && UpgradeUtils.replaceTileForUpgrade(this, upgradeResult, upgradeData);
    }

    private boolean applyFirstUpgradeSnapshot(FirstChemicalDissolutionChamberUpgradeData data) {
        LargeMachineUpgradeDataApplier.applyCommon(this, data, upgradeComponent, securityComponent);
        prevEnergy = data.prevEnergy;
        progress[0] = data.operatingTicks;
        configComponent.read(data.configComponentData.copy());
        ejectorComponent.read(data.ejectorComponentData.copy());
        ejectorComponent.setOutputData(configComponent, TransmissionType.GAS)
              .setCanTankEject(tank -> tank != injectTank);
        setUpgradeSlot(0, data.targetSlot0);
        setUpgradeSlot(1, data.targetSlot1);
        setUpgradeSlot(2, data.targetSlot2);
        setUpgradeSlot(3, data.targetSlot3);
        injectTank.setGas(data.injectGas == null ? null : data.injectGas.copy());
        outputTank1.setGas(data.outputGas == null ? null : data.outputGas.copy());
        upgraded = true;
        isUpgrade = true;
        LargeMachineUpgradeDataApplier.finish(this, upgradeComponent);
        return true;
    }

    private void setUpgradeSlot(int slot, ItemStack stack) {
        List<IInventorySlot> slots = getInventorySlots(null);
        if (slot < slots.size()) {
            slots.get(slot).setStack(LargeMachineUpgradeData.copyStack(stack));
        }
    }

    private boolean applyUpgradeData(RepeatedChemicalDissolutionChamberUpgradeData data) {
        LargeMachineUpgradeDataApplier.applyCommon(this, data, upgradeComponent, securityComponent);
        prevEnergy = data.prevEnergy;
        System.arraycopy(data.progress, 0, progress, 0, Math.min(data.progress.length, progress.length));
        System.arraycopy(data.usedSoFar, 0, usedSoFar, 0, Math.min(data.usedSoFar.length, usedSoFar.length));
        sorting = data.sorting;
        markSortingNeeded();
        configComponent.read(data.configComponentData.copy());
        ejectorComponent.read(data.ejectorComponentData.copy());
        ejectorComponent.setOutputData(configComponent, TransmissionType.GAS)
              .setCanTankEject(tank -> tank != injectTank);

        injectTank.setGas(data.injectGas == null ? null : data.injectGas.copy());
        int tankCount = Math.min(data.outputGases.length, outPutTanks.length);
        for (int i = 0; i < tankCount; i++) {
            outPutTanks[i].setGas(data.outputGases[i] == null ? null : data.outputGases[i].copy());
        }
        upgraded = true;
        isUpgrade = true;
        LargeMachineUpgradeDataApplier.finish(this, upgradeComponent);
        return true;
    }

    /**
     * @author sddsd2332
     * @reason 取消在升级的时候进行辐射判断
     */
    @Override
    public boolean shouldDumpRadiation() {
        return isUpgrade;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Class<?> getSelectionWireframeModelClass() {
        return mekceumoremachine.client.model.machine.ModelTierChemicalDissolutionChamber.class;
    }

    @Override
    public int getBlockGuiID(Block block, int i) {
        return 10;
    }

    @Override
    public IGuiProvider guiProvider() {
        return MEKCeuMoreMachine.proxy;
    }

    @Override
    public MachineTier getNowTier() {
        return tier;
    }

    @Override
    public boolean getsorting() {
        return sorting;
    }

}
