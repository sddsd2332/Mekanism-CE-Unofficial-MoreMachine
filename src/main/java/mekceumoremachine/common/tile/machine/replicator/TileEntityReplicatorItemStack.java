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
import mekanism.common.capabilities.holder.gas.GasTankHelper;
import mekanism.common.capabilities.holder.gas.IGasTankHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.cache.CachedRecipe;
import mekanism.common.recipe.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.common.recipe.cache.TwoInputCachedRecipe;
import mekanism.common.recipe.cache.inputs.InputHelper;
import mekanism.common.recipe.cache.outputs.OutputHelper;
import mekanism.common.recipe.inputs.NucleosynthesizerInput;
import mekanism.common.recipe.machines.ReplicatorItemStackRecipe;
import mekanism.common.recipe.outputs.ItemStackOutput;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.prefab.TileEntityBasicMachine;
import mekanism.common.util.*;
import mekceumoremachine.common.capability.ResizableGasTank;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.config.MoreMachineConfig;
import mekceumoremachine.common.recipe.cache.inputs.TemplateInputHelper;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.Map;

public class TileEntityReplicatorItemStack extends TileEntityBasicMachine<NucleosynthesizerInput, ItemStackOutput, ReplicatorItemStackRecipe> implements ISustainedData, ISpecialSelectionWireframeTile {

    private static final String[] methods = new String[]{"getEnergy", "getProgress", "isActive", "facing", "canOperate", "getMaxEnergy", "getEnergyNeeded", "getGasStored"};
    public ResizableGasTank inputGasTank;
    private BasicInventorySlot inputSlot;
    private EnergyInventorySlot energySlot;
    private OutputInventorySlot outputSlot;

    public TileEntityReplicatorItemStack() {
        super("prc", "ReplicatorItemStack", MoreMachineConfig.current().config.ReplicatorItemStackEnergyStorge.val(), MoreMachineConfig.current().config.ReplicatorItemStackEnergyUsage.val(), 3, 200);

        configComponent = new TileComponentConfig(this, TransmissionType.ITEM, TransmissionType.ENERGY, TransmissionType.GAS);

        initializeInventorySlots();
        configComponent.setupItemIOConfig(inputSlot, outputSlot, energySlot);
        configComponent.setConfig(TransmissionType.ITEM, DataType.ENERGY, DataType.INPUT, DataType.NONE, DataType.NONE, DataType.NONE, DataType.OUTPUT);

        configComponent.setupInputConfig(TransmissionType.GAS, inputGasTank);
        configComponent.setInputConfig(TransmissionType.ENERGY);

        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(configComponent, TransmissionType.ITEM);
    }

    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = createInventorySlotHelper();
        inputSlot = builder.addSlot(BasicInventorySlot.at(TileEntityReplicatorItemStack::isInRecipe, getRecipeCacheListener(), 53, 34));
        energySlot = builder.addSlot(EnergyInventorySlot.fillOrConvert(getMainEnergyContainer(), this::getWorld, listener, 140, 34));
        outputSlot = builder.addSlot(OutputInventorySlot.at(getRecipeCacheChangeListener(listener), 115, 34));
        return builder.build();
    }

    @Override
    protected IGasTankHolder getInitialGasTanks(IContentsListener listener) {
        GasTankHelper builder = createGasTankHelper();
        builder.addTank(getOrCreateInputGasTank(listener));
        return builder.build();
    }

    private ResizableGasTank getOrCreateInputGasTank(IContentsListener listener) {
        if (inputGasTank == null) {
            inputGasTank = ResizableGasTank.input(10000, gas -> true, getRecipeCacheListener());
        }
        return inputGasTank;
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
    public void onCachedRecipeChanged(CachedRecipe<ReplicatorItemStackRecipe> cachedRecipe, int cacheIndex) {
        super.onCachedRecipeChanged(cachedRecipe, cacheIndex);
        int ticks = cachedRecipe == null ? 200 : cachedRecipe.getRecipe().ticks;
        boolean update = BASE_TICKS_REQUIRED != ticks;
        BASE_TICKS_REQUIRED = ticks;
        if (update) {
            recalculateUpgradables(Upgrade.SPEED);
        }
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
        refreshRecipeLookupCache();
        NucleosynthesizerInput input = getInput();
        if (cachedRecipe == null || !input.testEquality(cachedRecipe.getInput())) {
            cachedRecipe = RecipeHandler.getReplicatorItemStackRecipe(input);
        }
        return cachedRecipe;
    }

    @Override
    protected void clearRecipeLookupCache() {
        super.clearRecipeLookupCache();
        cachedRecipe = null;
    }

    @Override
    public NucleosynthesizerInput getInput() {
        return new NucleosynthesizerInput(inputSlot.getStack(), inputGasTank.getGas());
    }

    @Override
    public boolean canOperate(ReplicatorItemStackRecipe recipe) {
        return recipe != null && recipe.canOperate(inputSlot, outputSlot, inputGasTank);
    }

    @Override
    public ReplicatorItemStackRecipe getRecipe(int cacheIndex) {
        return getRecipe();
    }

    @Override
    public CachedRecipe<ReplicatorItemStackRecipe> createNewCachedRecipe(ReplicatorItemStackRecipe recipe, int cacheIndex) {
        return new TwoInputCachedRecipe<>(recipe, this::shouldRecheckAllRecipeErrors,
              TemplateInputHelper.getItemTemplateInputHandler(inputSlot, RecipeError.NOT_ENOUGH_INPUT),
              InputHelper.getGasInputHandler(inputGasTank, RecipeError.NOT_ENOUGH_SECONDARY_INPUT),
              OutputHelper.getOutputHandler(outputSlot, RecipeError.NOT_ENOUGH_OUTPUT_SPACE),
              () -> recipe.getInput().getSolid(), () -> recipe.getInput().getGas(),
              (item, gas) -> recipe.getInput().meets(new NucleosynthesizerInput(item, gas)),
              (item, gas) -> recipe.getOutput().output.copy(),
              ItemStack::isEmpty, gas -> gas == null || gas.amount <= 0, ItemStack::isEmpty)
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

    private double getReplicatorEnergyPerTick(ReplicatorItemStackRecipe recipe) {
        return MekanismUtils.getEnergyPerTick(this, BASE_ENERGY_PER_TICK + recipe.extraEnergy);
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
