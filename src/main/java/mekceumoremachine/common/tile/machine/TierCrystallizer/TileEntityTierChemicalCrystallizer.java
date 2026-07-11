package mekceumoremachine.common.tile.machine.TierCrystallizer;

import io.netty.buffer.ByteBuf;
import mekanism.api.IConfigCardAccess.ISpecialConfigData;
import mekanism.api.IContentsListener;
import mekanism.api.TileNetworkList;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTank;
import mekanism.api.gas.IExtendedGasTank;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.Mekanism;
import mekanism.common.CommonWorldTickHandler;
import mekanism.common.Upgrade;
import mekanism.common.base.IComparatorSupport;
import mekanism.common.base.IGuiProvider;
import mekanism.common.base.ISideConfiguration;
import mekanism.common.base.ISpecialSelectionWireframeTile;
import mekanism.common.base.ISustainedData;
import mekanism.common.base.ITankManager;
import mekanism.common.block.states.BlockStateMachine.MachineType;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.gas.GasTankHelper;
import mekanism.common.capabilities.holder.gas.IGasTankHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.config.MekanismConfig;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.RecipeHandler.Recipe;
import mekanism.common.recipe.cache.CachedRecipe;
import mekanism.common.recipe.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.common.recipe.cache.FactoryRecipeCacheLookupMonitor;
import mekanism.common.recipe.cache.IRecipeLookupHandler;
import mekanism.common.recipe.cache.OneInputCachedRecipe;
import mekanism.common.recipe.cache.inputs.InputHelper;
import mekanism.common.recipe.cache.outputs.OutputHelper;
import mekanism.common.recipe.inputs.GasInput;
import mekanism.common.recipe.machines.CrystallizerRecipe;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.prefab.TileEntityMachine;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.util.ItemDataUtils;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.TileUtils;
import mekanism.common.util.UpgradeUtils;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.capability.ResizableGasTank;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.machine.TierGasInputSorter;
import mekceumoremachine.common.tile.interfaces.INeedRepeatTierUpgrade;
import mekceumoremachine.common.tile.interfaces.ITierSorting;
import mekceumoremachine.common.upgrade.FirstChemicalCrystallizerUpgradeData;
import mekceumoremachine.common.upgrade.LargeMachineUpgradeData;
import mekceumoremachine.common.upgrade.LargeMachineUpgradeDataApplier;
import mekceumoremachine.common.upgrade.RepeatedChemicalCrystallizerUpgradeData;
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
import java.util.Collections;
import java.util.List;

public class TileEntityTierChemicalCrystallizer extends TileEntityMachine implements ISustainedData, ITankManager, ISpecialConfigData,
      IComparatorSupport, ISideConfiguration, ISpecialSelectionWireframeTile, INeedRepeatTierUpgrade<MachineTier>, IRecipeLookupHandler<CrystallizerRecipe>,
      ITierSorting, TierGasInputSorter.Context {

    public static final int MAX_GAS = 10000;
    public static final int BASE_TICKS_REQUIRED = 200;

    public ResizableGasTank inputTank1;
    public ResizableGasTank inputTank2;
    public ResizableGasTank inputTank3;
    public ResizableGasTank inputTank4;
    public ResizableGasTank inputTank5;
    public ResizableGasTank inputTank6;
    public ResizableGasTank inputTank7;
    public ResizableGasTank inputTank8;
    public ResizableGasTank inputTank9;

    public int[] progress;
    public CrystallizerRecipe[] cachedRecipe;
    private final FactoryRecipeCacheLookupMonitor<CrystallizerRecipe>[] recipeCacheLookupMonitors;
    private final boolean[] activeProcesses;
    public ResizableGasTank[] inputTanks;
    public MachineTier tier;
    public TileComponentEjector ejectorComponent;
    public TileComponentConfig configComponent;
    public float prevScale;
    public int updateDelay;
    public boolean needsPacket;
    public int ticksRequired = BASE_TICKS_REQUIRED;
    public boolean upgraded;
    public boolean isUpgrade = true;
    public boolean sorting;
    private boolean sortingNeeded = true;
    private int observedRecipeVersion = RecipeHandler.getGlobalRecipeVersion();
    private boolean recipeCachesInvalid;
    private final TierGasInputSorter gasSorter = new TierGasInputSorter(this);

    private final List<IInventorySlot> outputSlots = new ArrayList<>();
    private EnergyInventorySlot energySlot;

    public TileEntityTierChemicalCrystallizer(MachineTier type) {
        super("crystallizer", "TierChemicalCrystallizer", type.processes * MachineType.CHEMICAL_CRYSTALLIZER.getStorage(),
              MachineType.CHEMICAL_CRYSTALLIZER.getUsage(), 3);
        tier = type;
        progress = new int[tier.processes];
        cachedRecipe = new CrystallizerRecipe[tier.processes];
        activeProcesses = new boolean[tier.processes];
        recipeCacheLookupMonitors = createRecipeCacheLookupMonitors();
        inputTanks = new ResizableGasTank[9];
        isActive = false;
        upgradeComponent.getSupportedTypes().forEach(this::recalculateUpgradables);

        configComponent = new TileComponentConfig(this, TransmissionType.ITEM, TransmissionType.GAS, TransmissionType.ENERGY);
        initializeInventorySlots();
        configComponent.setupItemIOConfig(Collections.emptyList(), outputSlots, energySlot, false);
        configComponent.setConfig(TransmissionType.ITEM, DataType.INPUT, DataType.INPUT, DataType.INPUT, DataType.ENERGY, DataType.INPUT, DataType.OUTPUT);

        configComponent.addGasSlotInfo(DataType.INPUT, getInputTanks());
        configComponent.fillConfig(TransmissionType.GAS, DataType.INPUT);
        configComponent.setCanEject(TransmissionType.GAS, false);

        configComponent.setInputConfig(TransmissionType.ENERGY);

        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(configComponent, TransmissionType.ITEM);
    }

    @SuppressWarnings("unchecked")
    private FactoryRecipeCacheLookupMonitor<CrystallizerRecipe>[] createRecipeCacheLookupMonitors() {
        FactoryRecipeCacheLookupMonitor<CrystallizerRecipe>[] monitors = new FactoryRecipeCacheLookupMonitor[tier.processes];
        for (int process = 0; process < tier.processes; process++) {
            monitors[process] = new FactoryRecipeCacheLookupMonitor<>(this, process, this::markSortingNeeded);
        }
        return monitors;
    }

    private IContentsListener getInputTankListener(IContentsListener listener, int process) {
        return () -> {
            listener.onContentsChanged();
            if (isProcessIndex(process)) {
                recipeCacheLookupMonitors[process].onChange();
            }
        };
    }

    private IContentsListener getOutputSlotListener(IContentsListener listener, int process) {
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
        outputSlots.clear();
        energySlot = builder.addSlot(EnergyInventorySlot.fillOrConvert(getMainEnergyContainer(), this::getWorld, listener, 8, 13));
        for (int process = 0; process < 9; process++) {
            int processIndex = process;
            OutputInventorySlot outputSlot = builder.addSlot(OutputInventorySlot.at(getOutputSlotListener(listener, process), getProcessSlotX(process), 71));
            outputSlot.setEnabledSupplier(() -> isProcessIndex(processIndex));
            outputSlots.add(outputSlot);
        }
        return builder.build();
    }

    @Override
    protected IGasTankHolder getInitialGasTanks(IContentsListener listener) {
        GasTankHelper builder = createGasTankHelper();
        for (int process = 0; process < inputTanks.length; process++) {
            ResizableGasTank tank = getOrCreateInputTank(process, listener);
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
            for (FactoryRecipeCacheLookupMonitor<CrystallizerRecipe> monitor : recipeCacheLookupMonitors) {
                if (monitor != null) {
                    monitor.unpause();
                }
            }
        }
    }

    private ResizableGasTank getOrCreateInputTank(int process, IContentsListener listener) {
        ResizableGasTank tank = switch (process) {
            case 0 -> inputTank1 == null ? inputTank1 = ResizableGasTank.input(MAX_GAS, this::isValidGas, getInputTankListener(listener, process)) : inputTank1;
            case 1 -> inputTank2 == null ? inputTank2 = ResizableGasTank.input(MAX_GAS, this::isValidGas, getInputTankListener(listener, process)) : inputTank2;
            case 2 -> inputTank3 == null ? inputTank3 = ResizableGasTank.input(MAX_GAS, this::isValidGas, getInputTankListener(listener, process)) : inputTank3;
            case 3 -> inputTank4 == null ? inputTank4 = ResizableGasTank.input(MAX_GAS, this::isValidGas, getInputTankListener(listener, process)) : inputTank4;
            case 4 -> inputTank5 == null ? inputTank5 = ResizableGasTank.input(MAX_GAS, this::isValidGas, getInputTankListener(listener, process)) : inputTank5;
            case 5 -> inputTank6 == null ? inputTank6 = ResizableGasTank.input(MAX_GAS, this::isValidGas, getInputTankListener(listener, process)) : inputTank6;
            case 6 -> inputTank7 == null ? inputTank7 = ResizableGasTank.input(MAX_GAS, this::isValidGas, getInputTankListener(listener, process)) : inputTank7;
            case 7 -> inputTank8 == null ? inputTank8 = ResizableGasTank.input(MAX_GAS, this::isValidGas, getInputTankListener(listener, process)) : inputTank8;
            case 8 -> inputTank9 == null ? inputTank9 = ResizableGasTank.input(MAX_GAS, this::isValidGas, getInputTankListener(listener, process)) : inputTank9;
            default -> throw new IllegalArgumentException("Invalid process " + process);
        };
        inputTanks[process] = tank;
        return tank;
    }

    private boolean isValidGas(Gas gas) {
        return Recipe.CHEMICAL_CRYSTALLIZER.containsRecipe(gas);
    }

    void sortInputGasTanks() {
        gasSorter.sort();
    }

    @Override
    public boolean isGasSorting() {
        return isSorting();
    }

    @Override
    public int getGasSorterProcessCount() {
        return tier.processes;
    }

    @Override
    public IExtendedGasTank getGasSorterInputTank(int process) {
        return isProcessIndex(process) ? inputTanks[process] : null;
    }

    @Override
    public boolean gasSorterInputProducesOutput(int process, Gas gas, boolean updateCache) {
        return inputProducesOutput(process, gas, updateCache);
    }

    @Override
    public int getGasSorterNeededInput(Gas gas) {
        return getMinGasPerTank(gas);
    }

    @Override
    public boolean isGasSorterProcessLocked(int process) {
        return isGasProcessLockedForSorting(process);
    }

    @Override
    public void onGasSorterChanged() {
        needsPacket = true;
        markNoUpdateSync();
    }

    private int getMinGasPerTank(Gas gas) {
        int min = Integer.MAX_VALUE;
        for (CrystallizerRecipe recipe : Recipe.CHEMICAL_CRYSTALLIZER.get().values()) {
            GasStack input = recipe.getInput().ingredient;
            if (input != null && input.getGas() == gas) {
                min = Math.min(min, Math.max(1, input.amount));
            }
        }
        return min == Integer.MAX_VALUE ? 1 : min;
    }

    private boolean isGasProcessLockedForSorting(int process) {
        return isProcessIndex(process) && progress[process] > 0;
    }

    private boolean inputProducesOutput(int process, Gas gas, boolean updateCache) {
        if (!isProcessIndex(process) || gas == null) {
            return false;
        }
        IInventorySlot outputSlot = getProcessOutputSlot(process);
        CrystallizerRecipe recipe = getRecipeForGas(gas);
        if (outputSlot == null || recipe == null || !recipe.getOutput().applyOutputs(outputSlot, false)) {
            return false;
        }
        if (updateCache) {
            cachedRecipe[process] = recipe;
            recipeCacheLookupMonitors[process].updateCachedRecipe(recipe);
        }
        return true;
    }

    private CrystallizerRecipe getRecipeForGas(Gas gas) {
        return RecipeHandler.getChemicalCrystallizerRecipe(new GasInput(new GasStack(gas, 1)));
    }

    private List<IExtendedGasTank> getInputTanks() {
        List<IExtendedGasTank> tanks = new ArrayList<>(tier.processes);
        for (int process = 0; process < tier.processes; process++) {
            tanks.add(inputTanks[process]);
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
        float targetScale = (float) (inputTank1.getGas() != null ? inputTank1.getGas().amount : 0) / inputTank1.getMaxGas();
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
        if (shouldSortGasTanks()) {
            markSortingNotNeeded();
            sortInputGasTanks();
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

    private boolean shouldSortGasTanks() {
        return sortingNeeded && isSorting() && hasInputGas();
    }

    private boolean hasInputGas() {
        for (int process = 0; process < tier.processes; process++) {
            ResizableGasTank inputTank = inputTanks[process];
            GasStack stack = inputTank == null ? null : inputTank.getGas();
            if (stack != null && stack.amount > 0) {
                return true;
            }
        }
        return false;
    }

    public boolean isSorting() {
        return sorting;
    }

    private boolean areRecipeCachesInvalid() {
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

    private IInventorySlot getProcessOutputSlot(int process) {
        return isProcessIndex(process) && process < outputSlots.size() ? outputSlots.get(process) : null;
    }

    private boolean isProcessIndex(int process) {
        return process >= 0 && process < tier.processes;
    }

    private boolean processRecipe(int process) {
        FactoryRecipeCacheLookupMonitor<CrystallizerRecipe> monitor = recipeCacheLookupMonitors[process];
        monitor.unpause();
        return monitor.updateAndProcess();
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
    public CrystallizerRecipe getRecipe(int cacheIndex) {
        if (!isProcessIndex(cacheIndex)) {
            return null;
        }
        GasStack gas = inputTanks[cacheIndex].getGas();
        if (gas == null || gas.amount <= 0) {
            return null;
        }
        GasInput input = new GasInput(gas);
        if (cachedRecipe[cacheIndex] == null || !input.testEquality(cachedRecipe[cacheIndex].getInput())) {
            cachedRecipe[cacheIndex] = RecipeHandler.getChemicalCrystallizerRecipe(input);
        }
        return cachedRecipe[cacheIndex];
    }

    @Override
    public CachedRecipe<CrystallizerRecipe> createNewCachedRecipe(CrystallizerRecipe recipe, int cacheIndex) {
        IInventorySlot outputSlot = getProcessOutputSlot(cacheIndex);
        return new OneInputCachedRecipe<>(recipe, () -> false,
              InputHelper.getGasInputHandler(inputTanks[cacheIndex], RecipeError.NOT_ENOUGH_INPUT),
              OutputHelper.getOutputHandler(outputSlot, RecipeError.NOT_ENOUGH_OUTPUT_SPACE),
              () -> recipe.getInput().ingredient,
              input -> input != null && input.isGasEqual(recipe.getInput().ingredient),
              input -> recipe.getOutput().output.copy(),
              input -> input == null || input.amount <= 0,
              ItemStack::isEmpty)
              .setCanHolderFunction(() -> MekanismUtils.canFunction(this))
              .setActive(active -> setProcessActive(cacheIndex, active))
              .setEnergyRequirements(() -> energyPerTick, getMainEnergyContainer())
              .setRequiredTicks(() -> ticksRequired)
              .setBaselineMaxOperations(this::getMaxOperationsPerTick)
              .setOperatingTicksChanged(ticks -> progress[cacheIndex] = ticks)
              .setOnFinish(this::onCachedRecipeFinish);
    }

    @Override
    public void onCachedRecipeChanged(CachedRecipe<CrystallizerRecipe> recipeCache, int cacheIndex) {
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

    public double getScaledProgress(int process) {
        return Math.max(Math.min((double) progress[process] / ticksRequired, 1.0D), 0.0D);
    }

    @Override
    public boolean sideIsConsumer(EnumFacing side) {
        return configComponent.hasSideForData(TransmissionType.ENERGY, facing, DataType.ENERGY, side);
    }

    @Override
    public void handlePacketData(ByteBuf dataStream) {
        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            int type = dataStream.readInt();
            if (type == 0) {
                sorting = !sorting;
                markSortingNeeded();
                needsPacket = true;
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
            for (GasTank inputTank : inputTanks) {
                TileUtils.readTankData(dataStream, inputTank);
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
        for (GasTank inputTank : inputTanks) {
            TileUtils.addTankData(data, inputTank);
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
        for (int i = 0; i < inputTanks.length; i++) {
            inputTanks[i].read(nbtTags.getCompoundTag("inputTank" + i));
        }
    }

    @Override
    public void writeCustomNBT(NBTTagCompound nbtTags) {
        super.writeCustomNBT(nbtTags);
        nbtTags.setBoolean("sorting", sorting);
        for (int i = 0; i < tier.processes; i++) {
            nbtTags.setInteger("progress" + i, progress[i]);
        }
        for (int i = 0; i < inputTanks.length; i++) {
            nbtTags.setTag("inputTank" + i, inputTanks[i].write(new NBTTagCompound()));
        }
    }

    @Nonnull
    @Override
    public String getName() {
        return LangUtils.localize("tile.TierChemicalCrystallizer." + tier.getBaseTier().getSimpleName() + ".name");
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
        return capability == Capabilities.CONFIG_CARD_CAPABILITY ||
              capability == Capabilities.SPECIAL_CONFIG_DATA_CAPABILITY || super.hasCapability(capability, side);
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
            energyPerTick = MekanismUtils.getEnergyPerTick(this, BASE_ENERGY_PER_TICK);
        } else if (upgrade == Upgrade.SPEED) {
            ticksRequired = MekanismUtils.getTicks(this, BASE_TICKS_REQUIRED);
            energyPerTick = MekanismUtils.getEnergyPerTick(this, BASE_ENERGY_PER_TICK);
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
        Object[] tanks = new Object[tier.processes];
        System.arraycopy(inputTanks, 0, tanks, 0, tier.processes);
        return tanks;
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
        for (int i = 0; i < inputTanks.length; i++) {
            GasTank tank = inputTanks[i];
            if (tank.getGas() != null) {
                ItemDataUtils.setCompound(itemStack, "inputTank" + i, tank.getGas().write(new NBTTagCompound()));
            }
        }
    }

    @Override
    public void readSustainedData(ItemStack itemStack) {
        for (int i = 0; i < inputTanks.length; i++) {
            inputTanks[i].setGas(GasStack.readFromNBT(ItemDataUtils.getCompound(itemStack, "inputTank" + i)));
        }
    }

    @Override
    public int getRedstoneLevel() {
        int stored = 0;
        int capacity = 0;
        for (int process = 0; process < tier.processes; process++) {
            stored += inputTanks[process].getStored();
            capacity += inputTanks[process].getMaxGas();
        }
        return capacity == 0 ? Container.calcRedstoneFromInventory(this) : MekanismUtils.redstoneLevelFromContents(stored, capacity);
    }

    @Override
    public IBlockState getUpgradeResult(BaseTier upgradeTier) {
        return tier.canUpgradeTo(upgradeTier) ? MEKCeuMoreMachineBlocks.TierChemicalCrystallizer.getStateFromMeta(MachineTier.get(upgradeTier).ordinal()) : null;
    }

    @Override
    public void prepareForUpgrade() {
        isUpgrade = false;
    }

    @Override
    public IUpgradeData getUpgradeData(BaseTier upgradeTier) {
        return tier.canUpgradeTo(upgradeTier) ? new RepeatedChemicalCrystallizerUpgradeData(upgradeTier, this) : null;
    }

    @Override
    public boolean parseUpgradeData(IUpgradeData upgradeData) {
        if (upgradeData instanceof FirstChemicalCrystallizerUpgradeData data && data.getUpgradeTier() == tier.getBaseTier()) {
            return applyFirstUpgradeSnapshot(data);
        }
        if (upgradeData instanceof RepeatedChemicalCrystallizerUpgradeData data && data.getUpgradeTier() == tier.getBaseTier()) {
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

    private boolean applyFirstUpgradeSnapshot(FirstChemicalCrystallizerUpgradeData data) {
        LargeMachineUpgradeDataApplier.applyCommonWithoutInventory(this, data, upgradeComponent, securityComponent);
        prevEnergy = data.prevEnergy;
        progress[0] = data.operatingTicks;
        configComponent.read(data.configComponentData.copy());
        ejectorComponent.read(data.ejectorComponentData.copy());
        ejectorComponent.setOutputData(configComponent, TransmissionType.ITEM);
        setUpgradeSlot(0, data.energySlot);
        clearOutputSlots();
        setUpgradeSlot(1, data.outputSlot);
        inputTank1.setGas(data.inputGas == null ? null : data.inputGas.copy());
        LargeMachineUpgradeDataApplier.returnUnmappedStack(this, data.unmappedInputGasSlot);
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

    private void clearOutputSlots() {
        for (IInventorySlot outputSlot : outputSlots) {
            outputSlot.setStack(ItemStack.EMPTY);
        }
    }

    private boolean applyUpgradeData(RepeatedChemicalCrystallizerUpgradeData data) {
        LargeMachineUpgradeDataApplier.applyCommon(this, data, upgradeComponent, securityComponent);
        prevEnergy = data.prevEnergy;
        System.arraycopy(data.progress, 0, progress, 0, Math.min(data.progress.length, progress.length));
        configComponent.read(data.configComponentData.copy());
        ejectorComponent.read(data.ejectorComponentData.copy());
        ejectorComponent.setOutputData(configComponent, TransmissionType.ITEM);
        sorting = data.sorting;

        int tankCount = Math.min(data.inputGases.length, inputTanks.length);
        for (int i = 0; i < tankCount; i++) {
            inputTanks[i].setGas(data.inputGases[i] == null ? null : data.inputGases[i].copy());
        }
        upgraded = true;
        isUpgrade = true;
        LargeMachineUpgradeDataApplier.finish(this, upgradeComponent);
        return true;
    }

    @Override
    public boolean shouldDumpRadiation() {
        return isUpgrade;
    }

    @Override
    public int getBlockGuiID(Block block, int i) {
        return 21;
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
