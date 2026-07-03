package mekceumoremachine.common.tile.machine;

import io.netty.buffer.ByteBuf;
import mekanism.api.AutomationType;
import mekanism.api.IConfigCardAccess;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.api.TileNetworkList;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.gas.*;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.Mekanism;
import mekanism.common.Upgrade;
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
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.inventory.slot.FluidInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.inventory.slot.gas.GasInventorySlot;
import mekanism.common.network.PacketTileEntity;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.cache.CachedRecipe;
import mekanism.common.recipe.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.common.recipe.cache.IRecipeLookupHandler;
import mekanism.common.recipe.cache.RecipeCacheLookupMonitor;
import mekanism.common.recipe.cache.RotaryCachedRecipe;
import mekanism.common.recipe.cache.inputs.InputHelper;
import mekanism.common.recipe.cache.outputs.OutputHelper;
import mekanism.common.recipe.machines.RotaryRecipe;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.prefab.TileEntityMachine;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.util.*;
import mekceumoremachine.common.capability.ResizableFluidTank;
import mekceumoremachine.common.capability.ResizableGasTank;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.interfaces.ITierMachine;
import mekceumoremachine.common.upgrade.FirstRotaryCondensentratorUpgradeData;
import mekceumoremachine.common.upgrade.LargeMachineUpgradeDataApplier;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;

public class TileEntityTierRotaryCondensentrator extends TileEntityMachine implements ISustainedData, Upgrade.IUpgradeInfoHandler, ITankManager,
        IComparatorSupport, ISideConfiguration, IConfigCardAccess.ISpecialConfigData, ITierMachine<MachineTier>, ISpecialSelectionWireframeTile,
        IRecipeLookupHandler<RotaryRecipe> {


    public static final int MAX_FLUID = 10000;
    public static final RecipeError NOT_ENOUGH_FLUID_INPUT_ERROR = RecipeError.create();
    public static final RecipeError NOT_ENOUGH_GAS_INPUT_ERROR = RecipeError.create();
    public static final RecipeError NOT_ENOUGH_SPACE_GAS_OUTPUT_ERROR = RecipeError.create();
    public static final RecipeError NOT_ENOUGH_SPACE_FLUID_OUTPUT_ERROR = RecipeError.create();
    private static final List<RecipeError> TRACKED_ERROR_TYPES = Arrays.asList(
          RecipeError.NOT_ENOUGH_ENERGY,
          RecipeError.NOT_ENOUGH_ENERGY_REDUCED_RATE,
          NOT_ENOUGH_FLUID_INPUT_ERROR,
          NOT_ENOUGH_GAS_INPUT_ERROR,
          NOT_ENOUGH_SPACE_GAS_OUTPUT_ERROR,
          NOT_ENOUGH_SPACE_FLUID_OUTPUT_ERROR,
          RecipeError.INPUT_DOESNT_PRODUCE_OUTPUT
    );
    public ResizableGasTank gasTank;
    public ResizableFluidTank fluidTank;

    /**
     * true: gas -> fluid; false: fluid -> gas
     */
    public boolean mode = true;

    public double clientEnergyUsed;

    public TileComponentEjector ejectorComponent;

    public TileComponentConfig configComponent;

    private final RecipeCacheLookupMonitor<RotaryRecipe> recipeCacheLookupMonitor = new RecipeCacheLookupMonitor<>(this);
    private final boolean[] trackedErrors = new boolean[TRACKED_ERROR_TYPES.size()];
    private int currentRedstoneLevel;
    private RotaryRecipe cachedRecipe;
    private int cachedRecipeVersion = -1;

    public MachineTier tier = MachineTier.BASIC;
    private GasInventorySlot gasOutputSlot;
    private GasInventorySlot gasInputSlot;
    private FluidInventorySlot fluidSlot;
    private OutputInventorySlot fluidContainerOutputSlot;
    private EnergyInventorySlot energySlot;

    public TileEntityTierRotaryCondensentrator() {
        super("machine.rotarycondensentrator", "TierRotaryCondensentrator", 0, MachineType.ROTARY_CONDENSENTRATOR.getUsage(), 5);

        configComponent = new TileComponentConfig(this, TransmissionType.ITEM, TransmissionType.ENERGY, TransmissionType.GAS, TransmissionType.FLUID);

        initializeInventorySlots();
        configComponent.setupItemIOConfig(Arrays.asList(gasInputSlot, fluidSlot), Arrays.asList(gasOutputSlot, fluidContainerOutputSlot), energySlot, true);
        configComponent.setConfig(TransmissionType.ITEM, DataType.INPUT, DataType.ENERGY, DataType.INPUT, DataType.INPUT, DataType.OUTPUT, DataType.OUTPUT);
        configComponent.setupIOConfig(TransmissionType.GAS, gasTank, RelativeSide.LEFT, true).setEjecting(true);
        configComponent.setupFluidIOConfig(fluidTank, RelativeSide.RIGHT, true).setEjecting(true);

        configComponent.setInputConfig(TransmissionType.ENERGY);

        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(configComponent, TransmissionType.GAS, TransmissionType.FLUID)
              .setCanEject(transmissionType -> {
                  if (transmissionType == TransmissionType.GAS) {
                      return !mode;
                  } else if (transmissionType == TransmissionType.FLUID) {
                      return mode;
                  }
                  return true;
              });
    }

    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = createInventorySlotHelper();
        BooleanSupplier modeSupplier = () -> !mode;
        gasOutputSlot = builder.addSlot(GasInventorySlot.rotaryDrain(gasTank, modeSupplier, listener, 5, 25));
        gasOutputSlot.setSlotType(ContainerSlotType.INPUT);
        gasOutputSlot.setSlotOverlay(SlotOverlay.PLUS);
        gasInputSlot = builder.addSlot(GasInventorySlot.rotaryFill(gasTank, modeSupplier, listener, 5, 56));
        gasInputSlot.setSlotType(ContainerSlotType.OUTPUT);
        gasInputSlot.setSlotOverlay(SlotOverlay.MINUS);
        fluidSlot = builder.addSlot(FluidInventorySlot.rotary(fluidTank, modeSupplier, listener, 155, 25));
        fluidSlot.setSlotType(ContainerSlotType.INPUT);
        fluidContainerOutputSlot = builder.addSlot(OutputInventorySlot.at(listener, 155, 56));
        energySlot = builder.addSlot(EnergyInventorySlot.fillOrConvert(getMainEnergyContainer(), this::getWorld, listener, 155, 5));
        return builder.build();
    }

    @Override
    protected IGasTankHolder getInitialGasTanks(IContentsListener listener) {
        GasTankHelper builder = createGasTankHelper();
        builder.addTank(getOrCreateGasTank(listener));
        return builder.build();
    }

    @Override
    protected IFluidTankHolder getInitialFluidTanks(IContentsListener listener) {
        FluidTankHelper builder = createFluidTankHelper();
        builder.addTank(getOrCreateFluidTank(listener));
        return builder.build();
    }

    private IContentsListener getRecipeCacheChangeListener(@Nullable IContentsListener listener) {
        return () -> {
            if (listener != null) {
                listener.onContentsChanged();
            }
            recipeCacheLookupMonitor.onChange();
        };
    }

    private ResizableGasTank getOrCreateGasTank(IContentsListener listener) {
        if (gasTank == null) {
            gasTank = new ResizableGasTank(MAX_FLUID * tier.processes,
                  (stack, automationType) -> automationType == AutomationType.MANUAL || automationType == AutomationType.INTERNAL || !mode,
                  (stack, automationType) -> automationType == AutomationType.INTERNAL || mode,
                  stack -> stack != null && isValidGas(stack), getRecipeCacheChangeListener(listener));
        }
        return gasTank;
    }

    private ResizableFluidTank getOrCreateFluidTank(IContentsListener listener) {
        if (fluidTank == null) {
            fluidTank = new ResizableFluidTank(MAX_FLUID * tier.processes,
                  (fluid, automationType) -> automationType == AutomationType.MANUAL || automationType == AutomationType.INTERNAL || mode,
                  (fluid, automationType) -> automationType == AutomationType.INTERNAL || !mode,
                  this::isValidFluid, getRecipeCacheChangeListener(listener));
        }
        return fluidTank;
    }

    @Override
    public void onAsyncUpdateServer() {
        super.onAsyncUpdateServer();
        energySlot.fillContainerOrConvert();
        if (mode) {
            gasInputSlot.fillTank();
            fluidSlot.drainTank(fluidContainerOutputSlot);
            if (fluidTank.getFluid() != null && fluidTank.getFluidAmount() == 0) {
                fluidTank.setEmpty();
            }
        } else {
            gasOutputSlot.drainTank();
            fluidSlot.fillTank(fluidContainerOutputSlot);
        }
        clientEnergyUsed = recipeCacheLookupMonitor.updateAndProcess(getMainEnergyContainer());
        if (recipeCacheLookupMonitor.getCachedRecipe(0) == null && prevEnergy >= getEnergy()) {
            setActive(false);
        }
        prevEnergy = getEnergy();
        int newRedstoneLevel = getRedstoneLevel();
        if (newRedstoneLevel != currentRedstoneLevel) {
            updateComparatorOutputLevelSync();
            currentRedstoneLevel = newRedstoneLevel;

        }
    }

    public int getUpgradedUsage() {
        int possibleProcess = Math.min((int) Math.pow(2, upgradeComponent.getUpgrades(Upgrade.SPEED)), MekanismConfig.current().mekce.MAXspeedmachines.val());
        return Math.max(possibleProcess, 1) * tier.processes;
    }

    public boolean isValidGas(GasStack g) {
        return RecipeHandler.isRotaryGasValid(g);
    }

    public boolean isValidFluid(@Nonnull Fluid f) {
        return RecipeHandler.isRotaryFluidValid(new FluidStack(f, 1));
    }

    public boolean isValidFluid(FluidStack f) {
        return RecipeHandler.isRotaryFluidValid(f);
    }

    public RotaryRecipe getRecipe() {
        int recipeVersion = RecipeHandler.Recipe.ROTARY_CONDENSENTRATOR.getRecipeVersion();
        if (cachedRecipeVersion != recipeVersion) {
            cachedRecipe = null;
            cachedRecipeVersion = recipeVersion;
        }
        if (mode) {
            GasStack input = gasTank.getGas();
            if (cachedRecipe == null || !cachedRecipe.test(input)) {
                cachedRecipe = RecipeHandler.getRotaryRecipe(input);
            }
        } else {
            FluidStack input = fluidTank.getFluid();
            if (cachedRecipe == null || !cachedRecipe.test(input)) {
                cachedRecipe = RecipeHandler.getRotaryRecipe(input);
            }
        }
        return cachedRecipe;
    }

    @Override
    public RotaryRecipe getRecipe(int cacheIndex) {
        return getRecipe();
    }

    @Override
    public CachedRecipe<RotaryRecipe> createNewCachedRecipe(RotaryRecipe recipe, int cacheIndex) {
        return new RotaryCachedRecipe(recipe, () -> false,
              InputHelper.getFluidInputHandler(fluidTank, NOT_ENOUGH_FLUID_INPUT_ERROR),
              InputHelper.getGasInputHandler(gasTank, NOT_ENOUGH_GAS_INPUT_ERROR),
              OutputHelper.getGasOutputHandler(gasTank, NOT_ENOUGH_SPACE_GAS_OUTPUT_ERROR),
              OutputHelper.getOutputHandler(fluidTank, NOT_ENOUGH_SPACE_FLUID_OUTPUT_ERROR),
              () -> !mode)
              .setCanHolderFunction(() -> MekanismUtils.canFunction(this))
              .setActive(active -> {
                  if (active || prevEnergy >= getEnergy()) {
                      setActive(active);
                  }
              })
              .setEnergyRequirements(() -> energyPerTick, getMainEnergyContainer())
              .setBaselineMaxOperations(this::getUpgradedUsage)
              .setErrorsChanged(errors -> {
                  for (int i = 0; i < trackedErrors.length; i++) {
                      trackedErrors[i] = errors.contains(TRACKED_ERROR_TYPES.get(i));
                  }
              })
              .setOnFinish(this::markNoUpdateSync);
    }

    @Override
    public void clearRecipeErrors(int cacheIndex) {
        Arrays.fill(trackedErrors, false);
    }

    @Override
    public void onCachedRecipeChanged(CachedRecipe<RotaryRecipe> recipeCache, int cacheIndex) {
        cachedRecipe = recipeCache == null ? null : recipeCache.getRecipe();
    }

    @Override
    public void onRecipeCacheInvalidated(int cacheIndex) {
        cachedRecipe = null;
        cachedRecipeVersion = RecipeHandler.Recipe.ROTARY_CONDENSENTRATOR.getRecipeVersion();
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.trackArray(trackedErrors);
    }

    public java.util.function.BooleanSupplier getWarningCheck(RecipeError error) {
        int errorIndex = TRACKED_ERROR_TYPES.indexOf(error);
        return errorIndex == -1 ? () -> false : () -> trackedErrors[errorIndex];
    }

    private void updateTankCapacities() {
        fluidTank.setCapacity(MAX_FLUID * tier.processes);
        gasTank.setMaxGas(MAX_FLUID * tier.processes);
    }

    @Override
    public void handlePacketData(ByteBuf dataStream) {
        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            int type = dataStream.readInt();
            if (type == 0) {
                mode = !mode;
                cachedRecipe = null;
                recipeCacheLookupMonitor.onChange();
            }
            playersUsing.forEach(player -> Mekanism.packetHandler.sendTo(new PacketTileEntity.TileEntityMessage(this), (EntityPlayerMP) player));
            return;
        }

        super.handlePacketData(dataStream);
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            MachineTier prevTier = tier;
            tier = MachineTier.byIndex(dataStream.readInt());
            updateTankCapacities();
            mode = dataStream.readBoolean();
            clientEnergyUsed = dataStream.readDouble();
            TileUtils.readTankData(dataStream, fluidTank);
            TileUtils.readTankData(dataStream, gasTank);
            if (prevTier != tier) {
                MekanismUtils.updateBlock(world, getPos());
            }
        }
    }


    @Override
    public TileNetworkList getNetworkedData(TileNetworkList data) {
        super.getNetworkedData(data);
        data.add(tier.ordinal());
        data.add(mode);
        data.add(clientEnergyUsed);
        TileUtils.addTankData(data, fluidTank);
        TileUtils.addTankData(data, gasTank);
        return data;
    }

    @Override
    public void readCustomNBT(NBTTagCompound nbtTags) {
        super.readCustomNBT(nbtTags);
        tier = MachineTier.byIndex(nbtTags.getInteger("tier"));
        updateTankCapacities();
        mode = nbtTags.getBoolean("mode");
        gasTank.read(nbtTags.getCompoundTag("gasTank"));
        if (nbtTags.hasKey("fluidTank")) {
            fluidTank.readFromNBT(nbtTags.getCompoundTag("fluidTank"));
        }
    }


    @Override
    public void writeCustomNBT(NBTTagCompound nbtTags) {
        super.writeCustomNBT(nbtTags);
        nbtTags.setInteger("tier", tier.ordinal());
        nbtTags.setBoolean("mode", mode);
        nbtTags.setTag("gasTank", gasTank.write(new NBTTagCompound()));
        nbtTags.setTag("fluidTank", fluidTank.writeToNBT(new NBTTagCompound()));
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
    public boolean isCapabilityDisabled(@Nonnull Capability<?> capability, EnumFacing side) {
        return configComponent.isCapabilityDisabled(capability, side, facing) || super.isCapabilityDisabled(capability, side);
    }

    @Override
    public void writeSustainedData(ItemStack itemStack) {
        if (fluidTank.getFluid() != null) {
            ItemDataUtils.setCompound(itemStack, "fluidTank", fluidTank.getFluid().writeToNBT(new NBTTagCompound()));
        }
        if (gasTank.getGas() != null) {
            ItemDataUtils.setCompound(itemStack, "gasTank", gasTank.getGas().write(new NBTTagCompound()));
        }
    }

    @Override
    public void readSustainedData(ItemStack itemStack) {
        fluidTank.setFluid(FluidStack.loadFluidStackFromNBT(ItemDataUtils.getCompound(itemStack, "fluidTank")));
        gasTank.setGas(GasStack.readFromNBT(ItemDataUtils.getCompound(itemStack, "gasTank")));
    }

    @Override
    public List<String> getInfo(Upgrade upgrade) {
        return upgrade == Upgrade.SPEED ? upgrade.getExpScaledInfo(this) : upgrade.getMultScaledInfo(this);
    }

    @Override
    public Object[] getManagedTanks() {
        return new Object[]{fluidTank, gasTank};
    }

    @Override
    public int getRedstoneLevel() {
        if (mode) {
            return MekanismUtils.redstoneLevelFromContents(gasTank.getStored(), gasTank.getMaxGas());
        }
        return MekanismUtils.redstoneLevelFromContents(fluidTank.getFluidAmount(), fluidTank.getCapacity());
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
    public int getBlockGuiID(Block block, int metadata) {
        return 3;
    }

    @Override
    public IGuiProvider guiProvider() {
        return MEKCeuMoreMachine.proxy;
    }


    @Override
    public NBTTagCompound getConfigurationData(NBTTagCompound nbtTags) {
        nbtTags.setBoolean("mode", mode);
        return nbtTags;
    }

    @Override
    public void setConfigurationData(NBTTagCompound nbtTags) {
        boolean newMode = nbtTags.getBoolean("mode");
        if (mode != newMode) {
            mode = newMode;
            cachedRecipe = null;
            recipeCacheLookupMonitor.onChange();
        }
    }

    @Override
    public boolean applyTierUpgrade(BaseTier upgradeTier) {
        if (!tier.canUpgradeTo(upgradeTier)) {
            return false;
        }
        tier = MachineTier.get(upgradeTier);
        updateTankCapacities();
        upgradeComponent.getSupportedTypes().forEach(this::recalculateUpgradables);
        Mekanism.packetHandler.sendUpdatePacket(this);
        markNoUpdateSync();
        return true;
    }

    @Override
    public boolean parseUpgradeData(IUpgradeData upgradeData) {
        if (upgradeData instanceof FirstRotaryCondensentratorUpgradeData data && data.getUpgradeTier() == tier.getBaseTier()) {
            LargeMachineUpgradeDataApplier.applyCommon(this, data, upgradeComponent, securityComponent);
            clientEnergyUsed = data.clientEnergyUsed;
            prevEnergy = data.prevEnergy;
            mode = data.mode;
            configComponent.read(data.configComponentData.copy());
            ejectorComponent.read(data.ejectorComponentData.copy());
            ejectorComponent.setOutputData(configComponent, TransmissionType.GAS, TransmissionType.FLUID)
                  .setCanEject(transmissionType -> {
                      if (transmissionType == TransmissionType.GAS) {
                          return !mode;
                      } else if (transmissionType == TransmissionType.FLUID) {
                          return mode;
                      }
                      return true;
                  });
            gasTank.setGas(data.gas == null ? null : data.gas.copy());
            fluidTank.setFluid(data.fluid == null ? null : data.fluid.copy());
            LargeMachineUpgradeDataApplier.finish(this, upgradeComponent);
            return true;
        }
        return ITierMachine.super.parseUpgradeData(upgradeData);
    }


    @Override
    public double getMaxEnergy() {
        return upgradeComponent.isUpgradeInstalled(Upgrade.ENERGY) ? MekanismUtils.getMaxEnergy(this, getTierEnergy()) : getTierEnergy();
    }


    public double getTierEnergy() {
        return MachineType.ROTARY_CONDENSENTRATOR.getStorage() * tier.processes;
    }


    @Nonnull
    @Override
    public String getName() {
        return LangUtils.localize("tile.TierRotaryCondensentrator." + tier.getBaseTier().getSimpleName() + ".name");
    }


    @Override
    public String getDataType() {
        return getName();
    }

    @Override
    public MachineTier getTier() {
        return tier;
    }

    @Override
    public boolean shouldDumpRadiation(){
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Class<?> getSelectionWireframeModelClass() {
        return mekanism.client.model.ModelRotaryCondensentrator.class;
    }

}
