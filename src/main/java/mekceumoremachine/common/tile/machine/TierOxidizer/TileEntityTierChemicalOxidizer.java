package mekceumoremachine.common.tile.machine.TierOxidizer;

import io.netty.buffer.ByteBuf;
import mekanism.api.IConfigCardAccess.ISpecialConfigData;
import mekanism.api.TileNetworkList;
import mekanism.api.gas.*;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.Mekanism;
import mekanism.common.SideData;
import mekanism.common.Upgrade;
import mekanism.common.base.*;
import mekanism.common.block.states.BlockStateMachine.MachineType;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.inputs.ItemStackInput;
import mekanism.common.recipe.machines.OxidationRecipe;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.component.SideConfig;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.factory.TileEntityFactory;
import mekanism.common.tile.prefab.TileEntityMachine;
import mekanism.common.util.*;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.interfaces.INeedRepeatTierUpgrade;
import mekceumoremachine.common.tile.interfaces.ITierSorting;
import net.minecraft.block.Block;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class TileEntityTierChemicalOxidizer extends TileEntityMachine implements IGasHandler, ISustainedData, ITankManager, ISpecialConfigData, IComparatorSupport, ISideConfiguration, INeedRepeatTierUpgrade<MachineTier>, ITierSorting {

    public static final int MAX_GAS = 10000;
    public GasTank outputTank1 = new GasTank(MAX_GAS);
    public GasTank outputTank2 = new GasTank(MAX_GAS);
    public GasTank outputTank3 = new GasTank(MAX_GAS);
    public GasTank outputTank4 = new GasTank(MAX_GAS);
    public GasTank outputTank5 = new GasTank(MAX_GAS);
    public GasTank outputTank6 = new GasTank(MAX_GAS);
    public GasTank outputTank7 = new GasTank(MAX_GAS);
    public GasTank outputTank8 = new GasTank(MAX_GAS);
    public GasTank outputTank9 = new GasTank(MAX_GAS);
    public int[] progress;
    public OxidationRecipe[] cachedRecipe;
    public GasTank[] outPutTanks = new GasTank[]{outputTank1, outputTank2, outputTank3, outputTank4, outputTank5, outputTank6, outputTank7, outputTank8, outputTank9};
    public MachineTier tier;
    public TileComponentEjector ejectorComponent;
    public TileComponentConfig configComponent;
    public boolean sorting;
    public boolean oldSorting;
    private final InvSorter inventorySorter = new InvSorter(this);
    private final EjectSpeedController gasSpeedController = new EjectSpeedController();
    public float prevScale;
    public int updateDelay;
    public boolean needsPacket;
    protected int successCounter = 0;
    protected boolean inventoryChanged = false;
    public static final int BASE_TICKS_REQUIRED = 100;
    public int ticksRequired = BASE_TICKS_REQUIRED;
    public boolean upgraded;

    public TileEntityTierChemicalOxidizer(MachineTier type) {
        super("oxidizer", "TierChemicalOxidizer", 0, MachineType.CHEMICAL_OXIDIZER.getUsage(), 1);
        this.tier = type;
        progress = new int[tier.processes];
        isActive = false;
        cachedRecipe = new OxidationRecipe[tier.processes];
        BASE_MAX_ENERGY = maxEnergy = tier.processes * MachineType.CHEMICAL_OXIDIZER.getStorage();
        BASE_ENERGY_PER_TICK = energyPerTick = tier.processes * MachineType.CHEMICAL_OXIDIZER.getUsage();
        upgradeComponent.getSupportedTypes().forEach(this::recalculateUpgradables);

        configComponent = new TileComponentConfig(this, TransmissionType.ITEM, TransmissionType.GAS, TransmissionType.ENERGY);
        configComponent.addOutput(TransmissionType.ITEM, new SideData(DataType.NONE, InventoryUtils.EMPTY));
        configComponent.addOutput(TransmissionType.ITEM, new SideData(DataType.INPUT, getInputSlotsWithTier(tier)));
        configComponent.addOutput(TransmissionType.ITEM, new SideData(DataType.ENERGY, new int[]{0}));
        configComponent.addOutput(TransmissionType.ITEM, new SideData(DataType.INPUT_ENHANCED, getInputSlotsWithTier(tier)));
        configComponent.setConfig(TransmissionType.ITEM, new byte[]{1, 2, 2, 2, 2, 0});

        configComponent.addOutput(TransmissionType.GAS, new SideData(DataType.NONE, InventoryUtils.EMPTY));
        configComponent.addOutput(TransmissionType.GAS, new SideData(DataType.OUTPUT, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8}));
        configComponent.fillConfig(TransmissionType.GAS, 1);

        configComponent.setInputConfig(TransmissionType.ENERGY);


        inventory = NonNullListSynchronized.withSize(11, ItemStack.EMPTY);

        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(TransmissionType.GAS, configComponent.getOutputs(TransmissionType.GAS).get(1));
    }

    public static int[] getInputSlotsWithTier(MachineTier tier) {
        return switch (tier) {
            case BASIC -> new int[]{2, 3, 4};
            case ADVANCED -> new int[]{2, 3, 4, 5, 6};
            case ELITE -> new int[]{2, 3, 4, 5, 6, 7, 8};
            case ULTIMATE -> new int[]{2, 3, 4, 5, 6, 7, 8, 9, 10};
        };
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

        ChargeUtils.discharge(0, this);

        if (sorting) {
            if (oldSorting) {
                sortInventory(); //Keeping the old sort prevents some problems
            } else {
                inventorySorter.sort();
            }
        }

        for (int process = 0; process < tier.processes; process++) {
            if (MekanismUtils.canFunction(this) && canOperate(getInputSlot(process)) && getEnergy() >= energyPerTick) {
                setActive(true);
                electricityStored.addAndGet(-energyPerTick);
                if ((progress[process] + 1) < ticksRequired) {
                    progress[process]++;
                } else if ((progress[process] + 1) >= ticksRequired) {
                    operate(getInputSlot(process));
                    progress[process] = 0;
                }
            }
            if (!canOperate(getInputSlot(process))) {
                progress[process] = 0;
            }
        }

        boolean hasOperation = false;


        for (int i = 0; i < tier.processes; i++) {
            if (canOperate(getInputSlot(i))) {
                hasOperation = true;
                break;
            }
        }

        if (MekanismUtils.canFunction(this) && hasOperation && getEnergy() >= energyPerTick) {
            setActive(true);
        } else if (prevEnergy >= getEnergy()) {
            setActive(false);
        }

        prevEnergy = getEnergy();
        if (needsPacket) {
            Mekanism.packetHandler.sendUpdatePacket(this);
        }
        needsPacket = false;
    }

    @Override
    public void addTileSyncTask() {
        AutomaticallyExtractItems(3);
        this.gasSpeedController.ensureSize(9, () -> Arrays.asList(new TankProvider.Gas(outputTank1), new TankProvider.Gas(outputTank2), new TankProvider.Gas(outputTank3), new TankProvider.Gas(outputTank4), new TankProvider.Gas(outputTank5), new TankProvider.Gas(outputTank6), new TankProvider.Gas(outputTank7), new TankProvider.Gas(outputTank8), new TankProvider.Gas(outputTank9)));
        for (int process = 0; process < tier.processes; process++) {
            handleTank(outPutTanks[process], configComponent.getSidesForData(TransmissionType.GAS, facing, 1), process);
        }
    }


    protected boolean canWork(int minWorkDelay, int maxWorkDelay) {
        if (inventoryChanged) {
            inventoryChanged = false;
            return true;
        }

        if (successCounter <= 0) {
            return ticksExisted % maxWorkDelay == 0;
        }
        int workDelay = Math.max(minWorkDelay, maxWorkDelay - (successCounter * 5));
        return ticksExisted % workDelay == 0;
    }

    protected void AutomaticallyExtractItems(int dataIndex) {
        if (getWorld().isRemote || !canWork(5, 60)) {
            return;
        }
        InputItems(dataIndex);
    }


    private void InputItems(int dataIndex) {
        SideConfig config = configComponent.getConfig(TransmissionType.ITEM);
        EnumFacing[] translatedFacings = MekanismUtils.getBaseOrientations(facing);
        for (EnumFacing facing : EnumFacing.VALUES) {
            if (config.get(translatedFacings[facing.ordinal()]) == dataIndex) {
                BlockPos offset = getPos().offset(facing);
                TileEntity te = getWorld().getTileEntity(offset);
                if (te == null) {
                    continue;
                }
                IItemHandler itemHandler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing);
                if (itemHandler == null) {
                    continue;
                }
                inputFromExternal(itemHandler);
            }
        }
    }


    private synchronized void inputFromExternal(IItemHandler external) {
        boolean successAtLeastOnce = false;

        external:
        for (int externalSlotId = 0; externalSlotId < external.getSlots(); externalSlotId++) {
            ItemStack externalStack = external.getStackInSlot(externalSlotId);
            if (externalStack.isEmpty()) {
                continue;
            }

            for (int internalSlotId : getInputSlotsWithTier(tier)) {
                ItemStack internalStack = inventory.get(internalSlotId);
                int maxCanExtract = Math.min(externalStack.getCount(), externalStack.getMaxStackSize());
                if (internalStack.isEmpty()) {
                    // Extract external item and insert to internal.
                    if (!isItemValidForSlot(internalSlotId, externalStack)) {
                        continue;
                    }
                    ItemStack extracted = external.extractItem(externalSlotId, maxCanExtract, false);
                    inventory.set(internalSlotId, extracted);
                    successAtLeastOnce = true;
                    // If there are no more items in the current slot, check the next external slot.
                    if (external.getStackInSlot(externalSlotId).isEmpty()) {
                        continue external;
                    }
                    continue;
                }

                if (internalStack.getCount() >= internalStack.getMaxStackSize() || !matchStacks(internalStack, externalStack)) {
                    continue;
                }

                int extractAmt = Math.min(
                        internalStack.getMaxStackSize() - internalStack.getCount(),
                        maxCanExtract);

                // Extract external item and insert to internal.
                ItemStack extracted = external.extractItem(externalSlotId, extractAmt, false);
                inventory.set(internalSlotId, copyStackWithSize(extracted, internalStack.getCount() + extracted.getCount()));
                successAtLeastOnce = true;
                // If there are no more items in the current slot, check the next external slot.
                if (external.getStackInSlot(externalSlotId).isEmpty()) {
                    continue external;
                }
            }
        }

        if (successAtLeastOnce) {
            incrementSuccessCounter(60, 5);
            markNoUpdate();
        } else {
            decrementSuccessCounter();
        }
    }

    protected void decrementSuccessCounter() {
        if (successCounter > 0) {
            successCounter--;
        }
    }

    protected void incrementSuccessCounter(int maxWorkDelay, int minWorkDelay) {
        int max = (maxWorkDelay - minWorkDelay) / 5;
        if (successCounter < max) {
            successCounter++;
        }
    }

    private void handleTank(GasTank tank, Set<EnumFacing> side, int tankidx) {
        if (tank.getGas() != null) {
            if (configComponent.isEjecting(TransmissionType.GAS)) {
                ejectGas(side, tank, this.gasSpeedController, tankidx);
            }
        }
    }

    private void ejectGas(Set<EnumFacing> outputSides, GasTank tank, EjectSpeedController speedController, int tankIdx) {
        speedController.record(tankIdx);
        if (tank.getGas() == null || tank.getStored() <= 0 || tank.getGas().getGas() == null) {
            return;
        }
        if (!speedController.canEject(tankIdx)) {
            return;
        }
        GasStack toEmit = tank.getGas().copy().withAmount(Math.min(tank.getMaxGas(), tank.getStored()));
        int emitted = GasUtils.emit(toEmit, this, outputSides);
        speedController.eject(tankIdx, emitted);
        if (emitted <= 0) {
            return;
        }
        tank.draw(emitted, true);
    }

    public static boolean matchStacks(@Nonnull ItemStack stack, @Nonnull ItemStack other) {
        if (!ItemStack.areItemsEqual(stack, other)) return false;
        return ItemStack.areItemStackTagsEqual(stack, other);
    }

    public static ItemStack copyStackWithSize(ItemStack stack, int amount) {
        if (stack.isEmpty() || amount <= 0) return ItemStack.EMPTY;
        ItemStack s = stack.copy();
        s.setCount(amount);
        return s;
    }

    public int getInputSlot(int operation) {
        return 2 + operation;
    }


    private int getOperation(int inputSlot) {
        return inputSlot - 2;
    }

    public boolean canOperate(int inputSlot) {
        if (inventory.get(inputSlot).isEmpty()) {
            return false;
        }
        int process = getOperation(inputSlot);
        if (cachedRecipe[process] != null && cachedRecipe[process].getInput().useItemStackFromInventory(inventory, inputSlot, false)) {
            return cachedRecipe[process].canOperate(inventory, inputSlot, outPutTanks[process]);
        }
        ItemStackInput input = new ItemStackInput(inventory.get(inputSlot));
        OxidationRecipe recipe = RecipeHandler.getOxidizerRecipe(input);
        cachedRecipe[process] = recipe;
        if (recipe == null) {
            return false;
        }
        return recipe.canOperate(inventory, inputSlot, outPutTanks[process]);
    }

    public void operate(int inputSlot) {
        if (!canOperate(inputSlot)) {
            return;
        }
        int process = getOperation(inputSlot);
        if (cachedRecipe[process] == null) {//should never happen, but cant be too sure.
            Mekanism.logger.debug("cachedRecipe was null, but we were asked to operate anyway?! {} @ {}", this, this.pos);
            return;
        }
        cachedRecipe[process].operate(inventory, inputSlot, outPutTanks[process]);
        markNoUpdateSync();
    }

    @Override
    public boolean sideIsConsumer(EnumFacing side) {
        return configComponent.hasSideForData(TransmissionType.ENERGY, facing, 1, side);
    }

    public OxidationRecipe getOxidationRecipe(ItemStack input) {
        return RecipeHandler.getOxidizerRecipe(new ItemStackInput(input));
    }

    public boolean inputProducesOutput(int slotID, ItemStack fallbackInput, boolean updateCache) {
        int process = getOperation(slotID);
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
        return true;
    }

    public OxidationRecipe getSlotRecipe(int slotID, ItemStack fallbackInput, boolean updateCache) {
        int process = getOperation(slotID);
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

    @Override
    public boolean canExtractItem(int slotID, @Nonnull ItemStack itemstack, @Nonnull EnumFacing side) {
        if (slotID == 0) {
            return ChargeUtils.canBeOutputted(itemstack, false);
        }
        return false;
    }

    @Override
    public boolean canInsertItem(int slotID, @Nonnull ItemStack itemstack, @Nonnull EnumFacing side) {
        if (slotID == 0) {
            return ChargeUtils.canBeDischarged(itemstack);
        } else if (isInputSlot(slotID)) {
            return inputProducesOutput(slotID, itemstack, false);
        }
        //TODO: Only allow inserting into extra slot if it can go in
        return super.canInsertItem(slotID, itemstack, side);
    }

    private boolean isInputSlot(int slotID) {
        return slotID >= 2 && (tier == MachineTier.BASIC ? slotID <= 4 : tier == MachineTier.ADVANCED ? slotID <= 6 : tier == MachineTier.ELITE ? slotID <= 8 : tier == MachineTier.ULTIMATE && slotID <= 10);
    }

    @Override
    public boolean isItemValidForSlot(int slotID, @Nonnull ItemStack itemstack) {
        if (isInputSlot(slotID)) {
            return RecipeHandler.getOxidizerRecipe(new ItemStackInput(itemstack)) != null;
        } else if (slotID == 0) {
            return ChargeUtils.canBeDischarged(itemstack);
        }
        return false;
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
            } else if (type == 1) {
                oldSorting = !oldSorting;
            }
            return;
        }
        super.handlePacketData(dataStream);
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            sorting = dataStream.readBoolean();
            oldSorting = dataStream.readBoolean();
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
        data.add(oldSorting);
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
        oldSorting = nbtTags.getBoolean("oldSorting");
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
        nbtTags.setBoolean("oldSorting", oldSorting);
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

    @Nonnull
    @Override
    public int[] getSlotsForFace(@Nonnull EnumFacing side) {
        return configComponent.getOutput(TransmissionType.ITEM, side, facing).availableSlots;
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
    public int receiveGas(EnumFacing side, GasStack stack, boolean doTransfer) {
        return 0;
    }

    @Override
    public GasStack drawGas(EnumFacing side, int amount, boolean doTransfer) {
        return null;
    }

    @Override
    public boolean canReceiveGas(EnumFacing side, Gas type) {
        return false;
    }

    @Override
    public boolean canDrawGas(EnumFacing side, Gas type) {
        return false;
    }


    @Nonnull
    @Override
    public GasTankInfo[] getTankInfo() {
        return switch (tier) {
            case BASIC -> new GasTankInfo[]{outputTank1, outputTank2, outputTank3};
            case ADVANCED -> new GasTankInfo[]{outputTank1, outputTank2, outputTank3, outputTank4, outputTank5};
            case ELITE ->
                    new GasTankInfo[]{outputTank1, outputTank2, outputTank3, outputTank4, outputTank5, outputTank6, outputTank7};
            case ULTIMATE ->
                    new GasTankInfo[]{outputTank1, outputTank2, outputTank3, outputTank4, outputTank5, outputTank6, outputTank7, outputTank8, outputTank9};
        };
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, EnumFacing side) {
        if (isCapabilityDisabled(capability, side)) {
            return false;
        }
        return capability == Capabilities.GAS_HANDLER_CAPABILITY || capability == Capabilities.CONFIG_CARD_CAPABILITY || capability == Capabilities.SPECIAL_CONFIG_DATA_CAPABILITY || super.hasCapability(capability, side);
    }

    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, EnumFacing side) {
        if (isCapabilityDisabled(capability, side)) {
            return null;
        } else if (capability == Capabilities.GAS_HANDLER_CAPABILITY) {
            return Capabilities.GAS_HANDLER_CAPABILITY.cast(this);
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
        switch (upgrade) {
            case ENERGY ->
                    energyPerTick = MekanismUtils.getEnergyPerTick(this, BASE_ENERGY_PER_TICK); // incorporate speed upgrades
            case SPEED -> {
                ticksRequired = MekanismUtils.getTicks(this, BASE_TICKS_REQUIRED);
                energyPerTick = MekanismUtils.getEnergyPerTick(this, BASE_ENERGY_PER_TICK);
            }
            default -> {
            }
        }
    }

    @Override
    public Object[] getTanks() {
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
        nbtTags.setBoolean("oldSorting", oldSorting);
        return nbtTags;
    }

    @Override
    public void setConfigurationData(NBTTagCompound nbtTags) {
        sorting = nbtTags.getBoolean("sorting");
        oldSorting = nbtTags.getBoolean("oldSorting");
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
    public boolean upgrade(BaseTier upgradeTier) {
        if (upgradeTier.ordinal() != tier.ordinal() + 1) {
            return false;
        }
        //如果升级的是创造,跳过
        if (upgradeTier == BaseTier.CREATIVE) {
            return false;
        }
        isUpgrade = false;
        if (world.getTileEntity(getPos()) instanceof IBoundingBlock block) {
            block.onBreak();
        } else {
            world.setBlockToAir(getPos());
        }
        world.setBlockState(getPos(), MEKCeuMoreMachineBlocks.TierChemicalOxidizer.getStateFromMeta(tier.ordinal() + 1), 3);

        if (world.getTileEntity(getPos()) instanceof TileEntityTierChemicalOxidizer tile) {
            //Basic
            tile.facing = facing;
            tile.clientFacing = clientFacing;
            tile.ticker = ticker;
            tile.redstone = redstone;
            tile.redstoneLastTick = redstoneLastTick;
            tile.doAutoSync = doAutoSync;

            //Electric
            tile.electricityStored.set(electricityStored.get());
            tile.prevEnergy = prevEnergy;

            //Factory
            System.arraycopy(progress, 0, tile.progress, 0, tier.processes);

            //Machine
            tile.setActive(isActive);
            tile.sorting = sorting;
            tile.oldSorting = oldSorting;
            tile.setControlType(getControlType());

            tile.upgradeComponent.readFrom(upgradeComponent);
            tile.upgradeComponent.setUpgradeSlot(upgradeComponent.getUpgradeSlot());

            tile.ejectorComponent.readFrom(ejectorComponent);
            tile.ejectorComponent.setOutputData(TransmissionType.GAS, tile.configComponent.getOutputs(TransmissionType.GAS).get(1));

            tile.securityComponent.readFrom(securityComponent);
            configComponent.getTransmissions().forEach(transmission -> {
                tile.configComponent.setConfig(transmission, configComponent.getConfig(transmission).asByteArray());
                tile.configComponent.setEjecting(transmission, configComponent.isEjecting(transmission));
            });
            for (int i = 0; i < inventory.size(); i++) {
                tile.inventory.set(i, inventory.get(i));
            }
            for (int i = 0; i < outPutTanks.length; i++) {
                tile.outPutTanks[i].setGas(outPutTanks[i].getGas());
            }
            tile.upgradeComponent.getSupportedTypes().forEach(tile::recalculateUpgradables);
            tile.upgraded = true;
            tile.markNoUpdateSync();
            Mekanism.packetHandler.sendUpdatePacket(tile);
            markNoUpdateSync();
            return true;
        }
        return false;
    }

    /**
     * @author sddsd2332
     * @reason 取消在升级的时候进行辐射判断
     */
    @Override
    public boolean shouldDumpRadiation() {
        return isUpgrade && super.shouldDumpRadiation();
    }

    @Override
    public int getBlockGuiID(Block block, int i) {
        return 12;
    }

    @Override
    public IGuiProvider guiProvider() {
        return MEKCeuMoreMachine.proxy;
    }

    public void sortInventory() { //old
        int[] inputSlots = getInputSlotsWithTier(tier);
        for (int i = 0; i < inputSlots.length; i++) {
            int slotID = inputSlots[i];
            ItemStack stack = inventory.get(slotID);
            int count = stack.getCount();
            ItemStack output = inventory.get(tier.processes + slotID);
            for (int j = i + 1; j < inputSlots.length; j++) {
                int checkSlotID = inputSlots[j];
                ItemStack checkStack = inventory.get(checkSlotID);
                if (Math.abs(count - checkStack.getCount()) < 2 ||
                        !InventoryUtils.areItemsStackable(stack, checkStack)) {
                    continue;
                }
                //Output/Input will not match
                // Only check if the input spot is empty otherwise assume it works
                if (stack.isEmpty() && !inputProducesOutput(checkSlotID, checkStack, true) ||
                        checkStack.isEmpty() && !inputProducesOutput(slotID, stack, true)) {
                    continue;
                }
                //Balance the two slots
                int total = count + checkStack.getCount();
                ItemStack newStack = stack.isEmpty() ? checkStack : stack;
                inventory.set(slotID, StackUtils.size(newStack, (total + 1) / 2));
                inventory.set(checkSlotID, StackUtils.size(newStack, total / 2));
                markNoUpdateSync();
                return;
            }
        }
    }

    @Override
    public MachineTier getNowTier() {
        return tier;
    }

    @Override
    public boolean getsorting() {
        return sorting;
    }

    @Override
    public boolean getoldSorting() {
        return oldSorting;
    }

    public static class InvSorter {
        private final TileEntityTierChemicalOxidizer factory;

        // Reusable List
        private final List<Tuple<OxidationRecipe, ItemStack>> vaildRecipeItemStackList = new ArrayList<>();
        // Reusable List
        private final List<ItemStack> invaildRecipeItemStackList = new ArrayList<>();
        // Reusable List
        private final List<ItemStack> sorted = new ArrayList<>();

        public InvSorter(TileEntityTierChemicalOxidizer factory) {
            this.factory = factory;
        }

        private static void addItemStackToList(ItemStack willBeAdded, List<ItemStack> stackList) {
            boolean isAdded = false;
            for (ItemStack stack : stackList) {
                int maxStackSize = stack.getMaxStackSize();
                int invStackCount = willBeAdded.getCount();

                if (!matchStacks(stack, willBeAdded)) {
                    continue;
                }
                if (stack.getCount() >= maxStackSize) {
                    continue;
                }
                if (stack.getCount() + invStackCount > maxStackSize) {
                    int added = maxStackSize - stack.getCount();
                    stack.setCount(maxStackSize);
                    willBeAdded.setCount(invStackCount - added);
                    continue;
                }
                stack.setCount(stack.getCount() + invStackCount);
                isAdded = true;
            }
            if (!isAdded) {
                stackList.add(willBeAdded);
            }
        }

        private static boolean addItemStackToTupleList(ItemStack willBeAdded, List<Tuple<OxidationRecipe, ItemStack>> tupleList) {
            for (Tuple<OxidationRecipe, ItemStack> collected : tupleList) {
                ItemStack stack = collected.getSecond();
                int maxStackSize = stack.getMaxStackSize();
                int invStackCount = willBeAdded.getCount();

                if (!matchStacks(stack, willBeAdded)) {
                    continue;
                }
                if (stack.getCount() >= maxStackSize) {
                    continue;
                }
                if (stack.getCount() + invStackCount > maxStackSize) {
                    int added = maxStackSize - stack.getCount();
                    stack.setCount(maxStackSize);
                    willBeAdded.setCount(invStackCount - added);
                    continue;
                }
                stack.setCount(stack.getCount() + invStackCount);
                return true;
            }
            return false;
        }


        public void sort() {
            if (!factory.sorting || factory.getWorld().getTotalWorldTime() % 20 != 0) {
                return;
            }
            int[] slotIds = getInputSlotsWithTier(factory.tier);
            if (slotIds == null || !hasItem(slotIds)) {
                return;
            }

            vaildRecipeItemStackList.clear();
            invaildRecipeItemStackList.clear();
            sorted.clear();

            collectInvToList(slotIds);

            if (vaildRecipeItemStackList.size() + invaildRecipeItemStackList.size() >= slotIds.length) {
                //The collection size is bigger than equals slotIds size, end sort.
                return;
            }

            doSort(slotIds.length - (vaildRecipeItemStackList.size() + invaildRecipeItemStackList.size()));
            applyResult(sorted, slotIds);
        }

        private boolean hasItem(int[] slotIds) {
            for (int slotId : slotIds) {
                if (factory.inventory.get(slotId) != ItemStack.EMPTY) {
                    return true;
                }
            }
            return false;
        }

        private void applyResult(List<ItemStack> sorted, int[] slotIds) {
            if (sorted.isEmpty()) {
                return;
            }

            int index = 0;
            for (int slotId : slotIds) {
                factory.inventory.set(slotId, ItemStack.EMPTY);
                if (index >= sorted.size()) {
                    continue;
                }
                factory.inventory.set(slotId, sorted.get(index));
                index++;
            }
            sorted.clear();
            factory.markNoUpdateSync();
        }

        private void doSort(int emptySlotAmount) {
            int availableEmptySlotAmount = emptySlotAmount;
            for (Tuple<OxidationRecipe, ItemStack> recipeAndInput : vaildRecipeItemStackList) {
                OxidationRecipe recipe = recipeAndInput.getFirst();
                ItemStack invStack = recipeAndInput.getSecond();
                ItemStack recipeInput = TileEntityFactory.getRecipeInput(recipe);

                int invCount = invStack.getCount();
                int minCount = recipeInput.getCount();
                if (invCount <= minCount) {
                    sorted.add(invStack);
                    continue;
                }

                int splitCount = Math.min(availableEmptySlotAmount + 1, invCount / minCount);
                int countAfterSplit = invCount / splitCount;
                int extra = invCount % splitCount;

                sorted.add(copyStackWithSize(invStack, countAfterSplit + extra));

                while (splitCount > 1) {
                    sorted.add(copyStackWithSize(invStack, countAfterSplit));
                    availableEmptySlotAmount--;
                    splitCount--;
                }
            }
            sorted.addAll(invaildRecipeItemStackList);
        }

        private void collectInvToList(int[] slotIds) {
            for (int slotId : slotIds) {
                ItemStack invTmp = factory.inventory.get(slotId);
                if (invTmp == ItemStack.EMPTY) {
                    continue;
                }
                ItemStack invStack = invTmp.copy();

                if (addItemStackToTupleList(invStack, vaildRecipeItemStackList)) {
                    continue;
                }

                OxidationRecipe recipe = factory.getSlotRecipe(slotId, invStack, true);
                if (recipe != null) {
                    vaildRecipeItemStackList.add(new Tuple<>(recipe, invStack));
                } else {
                    addItemStackToList(invStack, invaildRecipeItemStackList);
                }
            }
        }
    }


}

