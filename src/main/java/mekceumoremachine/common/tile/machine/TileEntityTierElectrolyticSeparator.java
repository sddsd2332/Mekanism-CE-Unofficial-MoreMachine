package mekceumoremachine.common.tile.machine;

import io.netty.buffer.ByteBuf;
import mekanism.api.EnumColor;
import mekanism.api.IConfigCardAccess;
import mekanism.api.IContentsListener;
import mekanism.api.TileNetworkList;
import mekanism.api.gas.*;
import mekanism.api.math.MathUtils;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.Mekanism;
import mekanism.common.Upgrade;
import mekanism.common.Upgrade.IUpgradeInfoHandler;
import mekanism.common.base.*;
import mekanism.common.block.states.BlockStateMachine.MachineType;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.holder.fluid.FluidTankHelper;
import mekanism.common.capabilities.holder.fluid.IFluidTankHolder;
import mekanism.common.capabilities.holder.gas.GasTankHelper;
import mekanism.common.capabilities.holder.gas.IGasTankHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.config.MekanismConfig;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.inventory.slot.FluidInventorySlot;
import mekanism.common.inventory.slot.gas.GasInventorySlot;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.RecipeHandler.Recipe;
import mekanism.common.recipe.cache.CachedRecipe;
import mekanism.common.recipe.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.common.recipe.cache.OneInputCachedRecipe;
import mekanism.common.recipe.cache.inputs.InputHelper;
import mekanism.common.recipe.cache.outputs.OutputHelper;
import mekanism.common.recipe.inputs.FluidInput;
import mekanism.common.recipe.machines.SeparatorRecipe;
import mekanism.common.recipe.outputs.ChemicalPairOutput;
import mekanism.common.tier.BaseTier;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.upgrade.TierUpgradeData;
import mekanism.common.tile.TileEntityGasTank.GasMode;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.prefab.TileEntityBasicMachine;
import mekanism.common.util.*;
import mekceumoremachine.common.capability.ResizableFluidTank;
import mekceumoremachine.common.capability.ResizableGasTank;
import mekanism.multiblockmachine.common.registries.MultiblockMachineBlocks;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.interfaces.ILargeMachine;
import mekceumoremachine.common.tile.interfaces.ITierMachine;
import mekceumoremachine.common.upgrade.FirstElectrolyticSeparatorUpgradeData;
import mekceumoremachine.common.upgrade.LargeElectrolyticSeparatorUpgradeData;
import mekceumoremachine.common.upgrade.LargeMachineUpgradeDataApplier;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Optional.Interface;
import net.minecraftforge.fml.common.Optional.InterfaceList;
import net.minecraftforge.fml.common.Optional.Method;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

@InterfaceList({
        @Interface(iface = "mekceumoremachine.common.tile.interfaces.ILargeMachine", modid = "mekanismmultiblockmachine"),
})
public class TileEntityTierElectrolyticSeparator extends TileEntityBasicMachine<FluidInput, ChemicalPairOutput, SeparatorRecipe>
        implements ISustainedData, IUpgradeInfoHandler, ITankManager, IConfigCardAccess.ISpecialConfigData, ITierMachine<MachineTier>, ILargeMachine {


    private static final String[] methods = new String[]{"getEnergy", "getOutput", "getMaxEnergy", "getEnergyNeeded", "getWater", "getWaterNeeded", "getHydrogen", "getHydrogenNeeded", "getOxygen", "getOxygenNeeded"};
    private static final GasMode[] GAS_MODES = GasMode.values();
    private final EjectSpeedController gasSpeedController = new EjectSpeedController();


    public ResizableFluidTank fluidTank;

    public static int MAX_GAS = 2400;

    public ResizableGasTank leftTank;

    public ResizableGasTank rightTank;

    public MachineTier tier = MachineTier.BASIC;

    public GasMode dumpLeft = GasMode.IDLE;
    public GasMode dumpRight = GasMode.IDLE;
    public SeparatorRecipe cachedRecipe;
    public double clientEnergyUsed;
    private int currentRedstoneLevel;
    private FluidInventorySlot inputSlot;
    private GasInventorySlot leftSlot;
    private GasInventorySlot rightSlot;
    private EnergyInventorySlot energySlot;

    public TileEntityTierElectrolyticSeparator() {
        super("electrolyticseparator", "TierElectrolyticSeparator", 0, MachineType.ELECTROLYTIC_SEPARATOR.getUsage(), 4, 1);
        configComponent = new TileComponentConfig(this, TransmissionType.ITEM, TransmissionType.ENERGY, TransmissionType.GAS, TransmissionType.FLUID);
        initializeInventorySlots();
        configComponent.setupItemInputDualOutputConfig(inputSlot, leftSlot, rightSlot, energySlot);
        configComponent.setConfig(TransmissionType.ITEM, DataType.NONE, DataType.NONE, DataType.INPUT, DataType.ENERGY, DataType.OUTPUT_1, DataType.OUTPUT_2);
        configComponent.setCanEject(TransmissionType.ITEM, false);

        configComponent.setupFluidInputConfig(fluidTank);

        configComponent.setupGasDualOutputConfig(leftTank, rightTank);
        configComponent.setConfig(TransmissionType.GAS, DataType.NONE, DataType.NONE, DataType.NONE, DataType.NONE, DataType.OUTPUT_1, DataType.OUTPUT_2);

        configComponent.setInputConfig(TransmissionType.ENERGY);
        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(configComponent, TransmissionType.ITEM, TransmissionType.GAS)
              .setCanTankEject(tank -> {
                  if (tank == leftTank) {
                      return dumpLeft != GasMode.DUMPING;
                  } else if (tank == rightTank) {
                      return dumpRight != GasMode.DUMPING;
                  }
                  return true;
              });
    }

    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = createInventorySlotHelper();
        inputSlot = builder.addSlot(FluidInventorySlot.fill(fluidTank, listener, 26, 35));
        leftSlot = builder.addSlot(GasInventorySlot.drain(leftTank, listener, 59, 52));
        rightSlot = builder.addSlot(GasInventorySlot.drain(rightTank, listener, 101, 52));
        energySlot = builder.addSlot(EnergyInventorySlot.fillOrConvert(getMainEnergyContainer(), this::getWorld, listener, 143, 35));
        inputSlot.setSlotType(ContainerSlotType.INPUT);
        leftSlot.setSlotType(ContainerSlotType.OUTPUT);
        rightSlot.setSlotType(ContainerSlotType.OUTPUT);
        return builder.build();
    }

    @Override
    protected IFluidTankHolder getInitialFluidTanks(IContentsListener listener) {
        FluidTankHelper builder = createFluidTankHelper();
        builder.addTank(getOrCreateFluidTank(listener));
        return builder.build();
    }

    @Override
    protected IGasTankHolder getInitialGasTanks(IContentsListener listener) {
        GasTankHelper builder = createGasTankHelper();
        builder.addTank(getOrCreateLeftTank(listener));
        builder.addTank(getOrCreateRightTank(listener));
        return builder.build();
    }

    private ResizableFluidTank getOrCreateFluidTank(IContentsListener listener) {
        if (fluidTank == null) {
            fluidTank = ResizableFluidTank.input(MAX_GAS * 10 * tier.processes,
                  fluid -> fluid != null && Recipe.ELECTROLYTIC_SEPARATOR.containsRecipe(fluid.getFluid()), listener);
        }
        return fluidTank;
    }

    private ResizableGasTank getOrCreateLeftTank(IContentsListener listener) {
        if (leftTank == null) {
            leftTank = ResizableGasTank.output(MAX_GAS * tier.processes, listener);
        }
        return leftTank;
    }

    private ResizableGasTank getOrCreateRightTank(IContentsListener listener) {
        if (rightTank == null) {
            rightTank = ResizableGasTank.output(MAX_GAS * tier.processes, listener);
        }
        return rightTank;
    }


    @Override
    public void onCachedRecipeChanged(CachedRecipe<SeparatorRecipe> cachedRecipe, int cacheIndex) {
        super.onCachedRecipeChanged(cachedRecipe, cacheIndex);
        double energyUsage = cachedRecipe == null ? MachineType.ELECTROLYTIC_SEPARATOR.getUsage() : cachedRecipe.getRecipe().energyUsage;
        boolean update = BASE_ENERGY_PER_TICK != energyUsage;
        BASE_ENERGY_PER_TICK = energyUsage;
        if (update) {
            recalculateUpgradables(Upgrade.ENERGY);
        }
    }

    public int dumpAmount;

    @Override
    public void onAsyncUpdateServer() {
        super.onAsyncUpdateServer();
        energySlot.fillContainerOrConvert();
        inputSlot.fillTank();
        leftSlot.drainTank();
        rightSlot.drainTank();
        clientEnergyUsed = processRecipe(getMainEnergyContainer());
        prevEnergy = getEnergy();
        dumpAmount = 8 * Math.min((int) Math.pow(2, upgradeComponent.getUpgrades(Upgrade.SPEED)), MekanismConfig.current().mekce.MAXspeedmachines.val());
        dumpAmount *= tier.processes;
    }

    @Override
    public void addTileSyncTask() {
        this.gasSpeedController.ensureSize(2, () -> Arrays.asList(new TankProvider.Gas(leftTank), new TankProvider.Gas(rightTank)));
        handleTank(leftTank, dumpLeft, configComponent.getSidesForData(TransmissionType.GAS, facing, DataType.OUTPUT_1), dumpAmount, 0);
        handleTank(rightTank, dumpRight, configComponent.getSidesForData(TransmissionType.GAS, facing, DataType.OUTPUT_2), dumpAmount, 1);
        int newRedstoneLevel = getRedstoneLevel();
        if (newRedstoneLevel != currentRedstoneLevel) {
            updateComparatorOutputLevelSync();
            currentRedstoneLevel = newRedstoneLevel;
        }
    }

    private void handleTank(GasTank tank, GasMode mode, Set<EnumFacing> side, int dumpAmount, int tankidx) {
        if (tank.getGas() != null) {
            if (mode != GasMode.DUMPING) {
                if (configComponent.isEjecting(TransmissionType.GAS)) {
                    ejectGas(side, tank, this.gasSpeedController, tankidx);
                }
            } else {
                tank.draw(dumpAmount, true);
            }
            if (mode == GasMode.DUMPING_EXCESS) {
                int target = getDumpingExcessTarget(tank);
                int stored = tank.getStored();
                if (target < stored) {
                    tank.draw(Math.min(stored - target, tier.getGasTankTier().getBaseOutput()), true);
                }
            }
        }
    }

    private int getDumpingExcessTarget(GasTank tank) {
        return MathUtils.clampToInt(tank.getMaxGas() * MekanismConfig.current().general.dumpExcessKeepRatio.val());
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

    public int getUpgradedUsage(SeparatorRecipe recipe) {
        int possibleProcess;
        if (leftTank.getGasType() == recipe.recipeOutput.leftGas.getGas()) {
            possibleProcess = leftTank.getNeeded() / recipe.recipeOutput.leftGas.amount;
            possibleProcess = Math.min(rightTank.getNeeded() / recipe.recipeOutput.rightGas.amount, possibleProcess);
        } else {
            possibleProcess = leftTank.getNeeded() / recipe.recipeOutput.rightGas.amount;
            possibleProcess = Math.min(rightTank.getNeeded() / recipe.recipeOutput.leftGas.amount, possibleProcess);
        }
        possibleProcess = Math.min(Math.min((int) Math.pow(2, upgradeComponent.getUpgrades(Upgrade.SPEED)), MekanismConfig.current().mekce.MAXspeedmachines.val()) * tier.processes, possibleProcess);
        possibleProcess = Math.min((int) (getEnergy() / energyPerTick), possibleProcess);
        possibleProcess = Math.max(possibleProcess, 1);
        return Math.min(fluidTank.getFluidAmount() / recipe.recipeInput.ingredient.amount, possibleProcess);
    }

    public SeparatorRecipe getRecipe() {
        refreshRecipeLookupCache();
        FluidInput input = getInput();
        if (cachedRecipe == null || !input.testEquality(cachedRecipe.getInput())) {
            cachedRecipe = RecipeHandler.getElectrolyticSeparatorRecipe(getInput());
        }
        return cachedRecipe;
    }

    @Override
    protected void clearRecipeLookupCache() {
        super.clearRecipeLookupCache();
        cachedRecipe = null;
    }

    @Override
    public SeparatorRecipe getRecipe(int cacheIndex) {
        return getRecipe();
    }

    public FluidInput getInput() {
        return new FluidInput(fluidTank.getFluid());
    }

    public boolean canOperate(SeparatorRecipe recipe) {
        return recipe != null && recipe.canOperate(fluidTank, leftTank, rightTank);
    }

    @Override
    public CachedRecipe<SeparatorRecipe> createNewCachedRecipe(SeparatorRecipe recipe, int cacheIndex) {
        return new OneInputCachedRecipe<>(recipe, this::shouldRecheckAllRecipeErrors,
              InputHelper.getFluidInputHandler(fluidTank, RecipeError.NOT_ENOUGH_INPUT),
              OutputHelper.getChemicalPairOutputHandler(leftTank, rightTank, RecipeError.NOT_ENOUGH_OUTPUT_SPACE),
              () -> recipe.getInput().ingredient,
              input -> input != null && input.isFluidEqual(recipe.getInput().ingredient),
              input -> recipe.getOutput().copy(), input -> input == null || input.amount <= 0, output -> output == null || !output.isValid())
              .setCanHolderFunction(() -> MekanismUtils.canFunction(this))
              .setActive(active -> {
                  if (active || prevEnergy >= getEnergy()) {
                      setActive(active);
                  }
              })
              .setEnergyRequirements(() -> energyPerTick, getMainEnergyContainer())
              .setRequiredTicks(() -> ticksRequired)
              .setBaselineMaxOperations(() -> getUpgradedUsage(recipe))
              .setOperatingTicksChanged(ticks -> operatingTicks = ticks)
              .setErrorsChanged(this::onRecipeErrorsChanged)
              .setOnFinish(this::onCachedRecipeFinish);
    }

    @Override
    public Map<FluidInput, SeparatorRecipe> getRecipes() {
        return Recipe.ELECTROLYTIC_SEPARATOR.get();
    }

    @Override
    public void handlePacketData(ByteBuf dataStream) {
        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            byte type = dataStream.readByte();
            if (type == 0) {
                dumpLeft = nextGasMode(dumpLeft);
            } else if (type == 1) {
                dumpRight = nextGasMode(dumpRight);
            }
            return;
        }

        super.handlePacketData(dataStream);

        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            MachineTier prevTier = tier;
            tier = MachineTier.byIndex(dataStream.readInt());
            fluidTank.setCapacity(MAX_GAS * 10 * tier.processes);
            leftTank.setMaxGas(MAX_GAS * tier.processes);
            rightTank.setMaxGas(MAX_GAS * tier.processes);
            TileUtils.readTankData(dataStream, fluidTank);
            TileUtils.readTankData(dataStream, leftTank);
            TileUtils.readTankData(dataStream, rightTank);
            dumpLeft = getGasMode(dataStream.readInt(), dumpLeft);
            dumpRight = getGasMode(dataStream.readInt(), dumpRight);
            clientEnergyUsed = dataStream.readDouble();
            if (prevTier != tier) {
                MekanismUtils.updateBlock(world, getPos());
            }
        }
    }

    @Override
    public TileNetworkList getNetworkedData(TileNetworkList data) {
        super.getNetworkedData(data);
        data.add(tier.ordinal());
        TileUtils.addTankData(data, fluidTank);
        TileUtils.addTankData(data, leftTank);
        TileUtils.addTankData(data, rightTank);
        data.add(dumpLeft.ordinal());
        data.add(dumpRight.ordinal());
        data.add(clientEnergyUsed);
        return data;
    }

    @Override
    public void readCustomNBT(NBTTagCompound nbtTags) {
        super.readCustomNBT(nbtTags);
        tier = MachineTier.byIndex(nbtTags.getInteger("tier"));
        fluidTank.setCapacity(MAX_GAS * 10 * tier.processes);
        leftTank.setMaxGas(MAX_GAS * tier.processes);
        rightTank.setMaxGas(MAX_GAS * tier.processes);
        fluidTank.readFromNBT(nbtTags.getCompoundTag("fluidTank"));
        leftTank.read(nbtTags.getCompoundTag("leftTank"));
        rightTank.read(nbtTags.getCompoundTag("rightTank"));
        dumpLeft = getGasMode(nbtTags.getInteger("dumpLeft"), dumpLeft);
        dumpRight = getGasMode(nbtTags.getInteger("dumpRight"), dumpRight);
    }


    @Override
    public void writeCustomNBT(NBTTagCompound nbtTags) {
        super.writeCustomNBT(nbtTags);
        nbtTags.setInteger("tier", tier.ordinal());
        nbtTags.setTag("fluidTank", fluidTank.writeToNBT(new NBTTagCompound()));
        nbtTags.setTag("leftTank", leftTank.write(new NBTTagCompound()));
        nbtTags.setTag("rightTank", rightTank.write(new NBTTagCompound()));
        nbtTags.setInteger("dumpLeft", dumpLeft.ordinal());
        nbtTags.setInteger("dumpRight", dumpRight.ordinal());
    }

    @Override
    public String[] getMethods() {
        return methods;
    }

    @Override
    public Object[] invoke(int method, Object[] arguments) throws NoSuchMethodException {
        return switch (method) {
            case 0 -> new Object[]{electricityStored};
            case 1 -> new Object[]{tier.getGasTankTier().getBaseOutput()};
            case 2 -> new Object[]{BASE_MAX_ENERGY};
            case 3 -> new Object[]{BASE_MAX_ENERGY - electricityStored.get()};
            case 4 -> new Object[]{fluidTank.getFluid() != null ? fluidTank.getFluid().amount : 0};
            case 5 ->
                    new Object[]{fluidTank.getFluid() != null ? (fluidTank.getCapacity() - fluidTank.getFluid().amount) : 0};
            case 6 -> new Object[]{leftTank.getStored()};
            case 7 -> new Object[]{leftTank.getNeeded()};
            case 8 -> new Object[]{rightTank.getStored()};
            case 9 -> new Object[]{rightTank.getNeeded()};
            default -> throw new NoSuchMethodException();
        };
    }

    @Override
    public void writeSustainedData(ItemStack itemStack) {
        if (fluidTank.getFluid() != null) {
            ItemDataUtils.setCompound(itemStack, "fluidTank", fluidTank.getFluid().writeToNBT(new NBTTagCompound()));
        }
        if (leftTank.getGas() != null) {
            ItemDataUtils.setCompound(itemStack, "leftTank", leftTank.getGas().write(new NBTTagCompound()));
        }
        if (rightTank.getGas() != null) {
            ItemDataUtils.setCompound(itemStack, "rightTank", rightTank.getGas().write(new NBTTagCompound()));
        }
    }


    @Override
    public void readSustainedData(ItemStack itemStack) {
        fluidTank.setFluid(FluidStack.loadFluidStackFromNBT(ItemDataUtils.getCompound(itemStack, "fluidTank")));
        leftTank.setGas(GasStack.readFromNBT(ItemDataUtils.getCompound(itemStack, "leftTank")));
        rightTank.setGas(GasStack.readFromNBT(ItemDataUtils.getCompound(itemStack, "rightTank")));
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, EnumFacing side) {
        if (isCapabilityDisabled(capability, side)) {
            return false;
        }
        return capability == Capabilities.SPECIAL_CONFIG_DATA_CAPABILITY || super.hasCapability(capability, side);
    }

    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, EnumFacing side) {
        if (isCapabilityDisabled(capability, side)) {
            return null;
        } else if (capability == Capabilities.SPECIAL_CONFIG_DATA_CAPABILITY) {
            return Capabilities.SPECIAL_CONFIG_DATA_CAPABILITY.cast(this);
        }
        return super.getCapability(capability, side);
    }

    @Override
    public List<String> getInfo(Upgrade upgrade) {
        return upgrade == Upgrade.SPEED ? upgrade.getExpScaledInfo(this) : upgrade.getMultScaledInfo(this);
    }

    @Override
    public Object[] getManagedTanks() {
        return new Object[]{fluidTank, leftTank, rightTank};
    }

    @Override
    public void recalculateUpgradables(Upgrade upgrade) {
        super.recalculateUpgradables(upgrade);
        if (upgrade == Upgrade.ENERGY) {
            energyPerTick = MachineType.ELECTROLYTIC_SEPARATOR.getUsage();
            setEnergy(Math.min(getMaxEnergy(), getEnergy()));
        }
    }


    @Override
    public double getMaxEnergy() {
        return upgradeComponent.isUpgradeInstalled(Upgrade.ENERGY) ? MekanismUtils.getMaxEnergy(this, getTierEnergy()) : getTierEnergy();
    }


    public double getTierEnergy() {
        return MachineType.ELECTROLYTIC_SEPARATOR.getStorage() * tier.processes;
    }

    @Override
    public int getRedstoneLevel() {
        return MekanismUtils.redstoneLevelFromContents(fluidTank.getFluidAmount(), fluidTank.getCapacity());
    }

    @Override
    public int getBlockGuiID(Block block, int metadata) {
        return 4;
    }

    @Override
    public IGuiProvider guiProvider() {
        return MEKCeuMoreMachine.proxy;
    }


    @Override
    public boolean applyTierUpgrade(BaseTier upgradeTier) {
        if (!tier.canUpgradeTo(upgradeTier)) {
            return false;
        }
        tier = MachineTier.get(upgradeTier);
        fluidTank.setCapacity(tier.processes * 10 * MAX_GAS);
        leftTank.setMaxGas(tier.processes * MAX_GAS);
        rightTank.setMaxGas(tier.processes * MAX_GAS);
        upgradeComponent.getSupportedTypes().forEach(this::recalculateUpgradables);
        Mekanism.packetHandler.sendUpdatePacket(this);
        markNoUpdateSync();
        return true;
    }

    @Nonnull
    @Override
    public String getName() {
        return LangUtils.localize("tile.TierElectrolyticSeparator." + tier.getBaseTier().getSimpleName() + ".name");
    }

    @Override
    public MachineTier getTier() {
        return tier;
    }

    @Override
    public NBTTagCompound getConfigurationData(NBTTagCompound nbtTags) {
        nbtTags.setInteger("dumpLeft", dumpLeft.ordinal());
        nbtTags.setInteger("dumpRight", dumpRight.ordinal());
        return nbtTags;
    }

    @Override
    public void setConfigurationData(NBTTagCompound nbtTags) {
        dumpLeft = getGasMode(nbtTags.getInteger("dumpLeft"), dumpLeft);
        dumpRight = getGasMode(nbtTags.getInteger("dumpRight"), dumpRight);
    }

    @Override
    public String getDataType() {
        return getName();
    }

    private static GasMode nextGasMode(GasMode mode) {
        return GAS_MODES[(mode.ordinal() + 1) % GAS_MODES.length];
    }

    private static GasMode getGasMode(int index, GasMode fallback) {
        return index >= 0 && index < GAS_MODES.length ? GAS_MODES[index] : fallback;
    }

    public boolean isUpgrade = true;

    @Override
    public IUpgradeData getUpgradeData(BaseTier upgradeTier) {
        if (upgradeTier == BaseTier.ULTIMATE && tier == MachineTier.ULTIMATE) {
            return new LargeElectrolyticSeparatorUpgradeData(upgradeTier, this);
        }
        return ITierMachine.super.getUpgradeData(upgradeTier);
    }

    @Override
    public IBlockState getUpgradeResult(BaseTier upgradeTier) {
        return upgradeTier == BaseTier.ULTIMATE && tier == MachineTier.ULTIMATE ? MultiblockMachineBlocks.LargeElectrolyticSeparator.getDefaultState() : null;
    }

    @Override
    public void prepareForUpgrade() {
        isUpgrade = false;
    }

    @Override
    public boolean parseUpgradeData(IUpgradeData upgradeData) {
        if (upgradeData instanceof FirstElectrolyticSeparatorUpgradeData data && data.getUpgradeTier() == tier.getBaseTier()) {
            LargeMachineUpgradeDataApplier.applyCommon(this, data, upgradeComponent, securityComponent);
            clientEnergyUsed = data.clientEnergyUsed;
            prevEnergy = data.prevEnergy;
            configComponent.read(data.configComponentData.copy());
            ejectorComponent.read(data.ejectorComponentData.copy());
            ejectorComponent.setOutputData(configComponent, TransmissionType.ITEM, TransmissionType.GAS);
            fluidTank.setFluid(data.fluid == null ? null : data.fluid.copy());
            leftTank.setGas(data.leftGas == null ? null : data.leftGas.copy());
            rightTank.setGas(data.rightGas == null ? null : data.rightGas.copy());
            dumpLeft = data.dumpLeft;
            dumpRight = data.dumpRight;
            isUpgrade = true;
            LargeMachineUpgradeDataApplier.finish(this, upgradeComponent);
            return true;
        }
        if (upgradeData instanceof TierUpgradeData data && data.getUpgradeTier() == BaseTier.ULTIMATE && tier == MachineTier.ULTIMATE) {
            return applyLargeMachineUpgrade();
        }
        return ITierMachine.super.parseUpgradeData(upgradeData);
    }

    @Override
    @Method(modid = "mekanismmultiblockmachine")
    public boolean canLargeMachineUpgrade(EntityPlayer player) {
        if (tier != MachineTier.ULTIMATE) {
            return false;
        }
        //检查范围内是否能摆放
        BlockPos.MutableBlockPos testPos = new BlockPos.MutableBlockPos();
        for (int yPos = 0; yPos <= 1; yPos++) {
            for (int xPos = -1; xPos <= 1; xPos++) {
                for (int zPos = -1; zPos <= 1; zPos++) {
                    //跳过自己
                    if (yPos == 0 && xPos == 0 && zPos == 0) {
                        continue;
                    }
                    testPos.setPos(pos.getX() + xPos, pos.getY() + yPos, pos.getZ() + zPos);
                    Block b = world.getBlockState(testPos).getBlock();
                    if (!world.isValid(testPos) || !world.isBlockLoaded(testPos, false) || !b.isReplaceable(world, testPos)) {
                        player.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + Mekanism.LOG_TAG + EnumColor.GREY + " " + LangUtils.localize("tooltip.largeMachineUpgrade.pos") + ": " + "X " + testPos.getX() + " " + "Y " + testPos.getY() + " " + "Z " + testPos.getZ()));
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    @Method(modid = "mekanismmultiblockmachine")
    public boolean applyLargeMachineUpgrade() {
        return ILargeMachine.super.applyLargeMachineUpgrade();
    }

    /**
     * @author sddsd2332
     * @reason 取消在升级的时候进行辐射判断
     */
    @Override
    public boolean shouldDumpRadiation() {
        return isUpgrade;
    }
}
