package mekceumoremachine.common.tile.machine.TierOxidizer;

import io.netty.buffer.ByteBuf;
import mekanism.api.IConfigCardAccess.ISpecialConfigData;
import mekanism.api.IContentsListener;
import mekanism.api.TileNetworkList;
import mekanism.api.gas.*;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.Mekanism;
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
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.cache.CachedRecipe;
import mekanism.common.recipe.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.common.recipe.cache.IRecipeLookupHandler;
import mekanism.common.recipe.cache.OneInputCachedRecipe;
import mekanism.common.recipe.cache.RecipeCacheLookupMonitor;
import mekanism.common.recipe.cache.inputs.InputHelper;
import mekanism.common.recipe.cache.outputs.OutputHelper;
import mekanism.common.recipe.inputs.ItemStackInput;
import mekanism.common.recipe.machines.OxidationRecipe;
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
import mekceumoremachine.common.tile.interfaces.INeedRepeatTierUpgrade;
import mekceumoremachine.common.tile.interfaces.ITierSorting;
import mekceumoremachine.common.tile.machine.TierProcessInputSorter;
import mekceumoremachine.common.upgrade.LargeMachineUpgradeDataApplier;
import mekceumoremachine.common.upgrade.RepeatedChemicalOxidizerUpgradeData;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TileEntityTierChemicalOxidizer extends TileEntityMachine implements ISustainedData, ITankManager, ISpecialConfigData, IComparatorSupport, ISideConfiguration, INeedRepeatTierUpgrade<MachineTier>, ITierSorting, IRecipeLookupHandler<OxidationRecipe>, TierProcessInputSorter.Context {

    public static final int MAX_GAS = 10000;
    public ResizableGasTank outputTank1;
    public ResizableGasTank outputTank2;
    public ResizableGasTank outputTank3;
    public ResizableGasTank outputTank4;
    public ResizableGasTank outputTank5;
    public ResizableGasTank outputTank6;
    public ResizableGasTank outputTank7;
    public ResizableGasTank outputTank8;
    public ResizableGasTank outputTank9;
    public int[] progress;
    public OxidationRecipe[] cachedRecipe;
    private final RecipeCacheLookupMonitor<OxidationRecipe>[] recipeCacheLookupMonitors;
    private final boolean[] activeProcesses;
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
    public static final int BASE_TICKS_REQUIRED = 100;
    public int ticksRequired = BASE_TICKS_REQUIRED;
    public boolean upgraded;
    private final List<IInventorySlot> inputSlots = new ArrayList<>();
    private EnergyInventorySlot energySlot;

    public TileEntityTierChemicalOxidizer(MachineTier type) {
        super("oxidizer", "TierChemicalOxidizer", type.processes * MachineType.CHEMICAL_OXIDIZER.getStorage(), MachineType.CHEMICAL_OXIDIZER.getUsage(), 1);
        this.tier = type;
        progress = new int[tier.processes];
        isActive = false;
        cachedRecipe = new OxidationRecipe[tier.processes];
        activeProcesses = new boolean[tier.processes];
        recipeCacheLookupMonitors = createRecipeCacheLookupMonitors();
        outPutTanks = new ResizableGasTank[9];
        upgradeComponent.getSupportedTypes().forEach(this::recalculateUpgradables);

        configComponent = new TileComponentConfig(this, TransmissionType.ITEM, TransmissionType.GAS, TransmissionType.ENERGY);
        initializeInventorySlots();
        configComponent.addItemSlotInfo(DataType.ENERGY, energySlot);
        configComponent.addItemSlotInfo(DataType.INPUT, inputSlots);
        configComponent.addItemSlotInfo(DataType.INPUT_ENHANCED, inputSlots);
        configComponent.setConfig(TransmissionType.ITEM, DataType.INPUT, DataType.ENERGY, DataType.ENERGY, DataType.INPUT, DataType.INPUT, DataType.NONE);
        configComponent.setCanEject(TransmissionType.ITEM, false);

        configComponent.addGasSlotInfo(DataType.OUTPUT, getOutputTanks());
        configComponent.fillConfig(TransmissionType.GAS, DataType.OUTPUT);
        configComponent.setEjecting(TransmissionType.GAS, true);

        configComponent.setInputConfig(TransmissionType.ENERGY);

        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(configComponent, TransmissionType.GAS);
    }

    @SuppressWarnings("unchecked")
    private RecipeCacheLookupMonitor<OxidationRecipe>[] createRecipeCacheLookupMonitors() {
        RecipeCacheLookupMonitor<OxidationRecipe>[] monitors = new RecipeCacheLookupMonitor[tier.processes];
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
        energySlot = builder.addSlot(EnergyInventorySlot.fillOrConvert(getMainEnergyContainer(), this::getWorld, listener, 8, 13));
        for (int process = 0; process < 9; process++) {
            int processIndex = process;
            InputInventorySlot inputSlot = builder.addSlot(InputInventorySlot.at(stack -> RecipeHandler.getOxidizerRecipe(new ItemStackInput(stack)) != null,
                  getRecipeCacheChangeListener(listener, process), getProcessSlotX(process), 13));
            inputSlot.setEnabledSupplier(() -> isProcessIndex(processIndex));
            inputSlots.add(inputSlot);
        }
        return builder.build();
    }

    @Override
    protected IGasTankHolder getInitialGasTanks(IContentsListener listener) {
        GasTankHelper builder = createGasTankHelper();
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
            for (RecipeCacheLookupMonitor<OxidationRecipe> monitor : recipeCacheLookupMonitors) {
                if (monitor != null) {
                    monitor.unpause();
                }
            }
        }
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
        float targetScale = (float) (outputTank1.getGas() != null ? outputTank1.getGas().amount : 0) / outputTank1.getMaxGas();
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
        OxidationRecipe recipe = getRecipeForInput(process, fallbackInput, updateCache);
        return recipe != null && recipeOutputFits(process, recipe);
    }

    private boolean recipeOutputFits(int process, OxidationRecipe recipe) {
        return outPutTanks[process] == null || recipe.getOutput().applyOutputs(outPutTanks[process], false, 1);
    }

    @Override
    public int getSorterNeededInput(int process, ItemStack inputStack) {
        OxidationRecipe recipe = getRecipeForInput(process, inputStack, true);
        return recipe == null ? 1 : Math.max(1, recipe.recipeInput.ingredient.getCount());
    }

    @Override
    public void onSorterChanged() {
        markNoUpdateSync();
    }

    private boolean isProcessIndex(int process) {
        return process >= 0 && process < tier.processes;
    }

    private boolean processRecipe(int process) {
        RecipeCacheLookupMonitor<OxidationRecipe> monitor = recipeCacheLookupMonitors[process];
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

    protected void onCachedRecipeFinish() {
        markNoUpdateSync();
    }

    @Override
    public int getSavedOperatingTicks(int cacheIndex) {
        return isProcessIndex(cacheIndex) ? progress[cacheIndex] : 0;
    }

    @Override
    public OxidationRecipe getRecipe(int cacheIndex) {
        if (!isProcessIndex(cacheIndex)) {
            return null;
        }
        IInventorySlot slot = getProcessInputSlot(cacheIndex);
        if (slot == null || slot.getStack().isEmpty()) {
            return null;
        }
        ItemStackInput input = new ItemStackInput(slot.getStack());
        if (cachedRecipe[cacheIndex] == null || !input.testEquality(cachedRecipe[cacheIndex].getInput())) {
            cachedRecipe[cacheIndex] = RecipeHandler.getOxidizerRecipe(input);
        }
        return cachedRecipe[cacheIndex];
    }

    @Override
    public CachedRecipe<OxidationRecipe> createNewCachedRecipe(OxidationRecipe recipe, int cacheIndex) {
        IInventorySlot inputSlot = getProcessInputSlot(cacheIndex);
        return new OneInputCachedRecipe<>(recipe, () -> false,
              InputHelper.getInputHandler(inputSlot, RecipeError.NOT_ENOUGH_INPUT),
              OutputHelper.getGasOutputHandler(outPutTanks[cacheIndex], RecipeError.NOT_ENOUGH_OUTPUT_SPACE),
              () -> recipe.getInput().ingredient,
              input -> mekanism.common.recipe.inputs.MachineInput.inputContains(input, recipe.getInput().ingredient),
              input -> recipe.getOutput().output.copy(), ItemStack::isEmpty, output -> output == null || output.amount <= 0)
              .setCanHolderFunction(() -> MekanismUtils.canFunction(this))
              .setActive(active -> setProcessActive(cacheIndex, active))
              .setEnergyRequirements(() -> energyPerTick, getMainEnergyContainer())
              .setRequiredTicks(() -> ticksRequired)
              .setBaselineMaxOperations(this::getMaxOperationsPerTick)
              .setOperatingTicksChanged(ticks -> progress[cacheIndex] = ticks)
              .setOnFinish(this::onCachedRecipeFinish);
    }

    @Override
    public void onCachedRecipeChanged(CachedRecipe<OxidationRecipe> recipeCache, int cacheIndex) {
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

    public OxidationRecipe getOxidationRecipe(ItemStack input) {
        return RecipeHandler.getOxidizerRecipe(new ItemStackInput(input));
    }

    public boolean inputProducesOutput(int process, ItemStack fallbackInput, boolean updateCache) {
        return getRecipeForInput(process, fallbackInput, updateCache) != null;
    }

    public OxidationRecipe getRecipeForInput(int process, ItemStack fallbackInput, boolean updateCache) {
        if (!isProcessIndex(process)) {
            return null;
        }
        OxidationRecipe cached = cachedRecipe[process];
        if (cached == null) {
            cached = getOxidationRecipe(fallbackInput);
            if (updateCache) {
                cachedRecipe[process] = cached;
            }
        } else {
            ItemStack recipeInput = cached.recipeInput.ingredient;
            if (recipeInput.isEmpty() || !ItemStack.areItemsEqual(recipeInput, fallbackInput)) {
                cached = getOxidationRecipe(fallbackInput);
                if (updateCache) {
                    cachedRecipe[process] = cached;
                }
            }
        }
        return cached;
    }

    public double getScaledProgress(int process) {
        return Math.max(Math.min((double) progress[process] / ticksRequired, 1.0D), 0.0D);
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
        }
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
        }
        for (int i = 0; i < outPutTanks.length; i++) {
            nbtTags.setTag("outputTank" + i, outPutTanks[i].write(new NBTTagCompound()));
        }
    }

    @Nonnull
    @Override
    public String getName() {
        return LangUtils.localize("tile.TierChemicalOxidizer." + tier.getBaseTier().getSimpleName() + ".name");
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
        } else if (upgrade == Upgrade.SPEED) {
            ticksRequired = MekanismUtils.getTicks(this, BASE_TICKS_REQUIRED);
            energyPerTick = MekanismUtils.getEnergyPerTick(this, BASE_ENERGY_PER_TICK);
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
            case BASIC -> new Object[]{outputTank1, outputTank2, outputTank3};
            case ADVANCED -> new Object[]{outputTank1, outputTank2, outputTank3, outputTank4, outputTank5};
            case ELITE ->
                    new Object[]{outputTank1, outputTank2, outputTank3, outputTank4, outputTank5, outputTank6, outputTank7};
            case ULTIMATE ->
                    new Object[]{outputTank1, outputTank2, outputTank3, outputTank4, outputTank5, outputTank6, outputTank7, outputTank8, outputTank9};
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
        for (int i = 0; i < outPutTanks.length; i++) {
            GasTank tank = outPutTanks[i];
            if (tank.getGas() != null) {
                ItemDataUtils.setCompound(itemStack, "outputTank" + i, tank.getGas().write(new NBTTagCompound()));
            }
        }
    }

    @Override
    public void readSustainedData(ItemStack itemStack) {
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
        return tier.canUpgradeTo(upgradeTier) ? MEKCeuMoreMachineBlocks.TierChemicalOxidizer.getStateFromMeta(MachineTier.get(upgradeTier).ordinal()) : null;
    }

    @Override
    public void prepareForUpgrade() {
        isUpgrade = false;
    }

    @Override
    public IUpgradeData getUpgradeData(BaseTier upgradeTier) {
        return tier.canUpgradeTo(upgradeTier) ? new RepeatedChemicalOxidizerUpgradeData(upgradeTier, this) : null;
    }

    @Override
    public boolean parseUpgradeData(IUpgradeData upgradeData) {
        if (upgradeData instanceof RepeatedChemicalOxidizerUpgradeData data && data.getUpgradeTier() == tier.getBaseTier()) {
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

    private boolean applyUpgradeData(RepeatedChemicalOxidizerUpgradeData data) {
        LargeMachineUpgradeDataApplier.applyCommon(this, data, upgradeComponent, securityComponent);
        prevEnergy = data.prevEnergy;
        System.arraycopy(data.progress, 0, progress, 0, Math.min(data.progress.length, progress.length));
        sorting = data.sorting;
        markSortingNeeded();
        configComponent.read(data.configComponentData.copy());
        ejectorComponent.read(data.ejectorComponentData.copy());
        ejectorComponent.setOutputData(configComponent, TransmissionType.GAS);

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
    public int getBlockGuiID(Block block, int i) {
        return 12;
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

