package mekceumoremachine.common.tile.machine;

import io.netty.buffer.ByteBuf;
import mekanism.api.Coord4D;
import mekanism.api.EnumColor;
import mekanism.api.IConfigCardAccess;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.api.TileNetworkList;
import mekanism.api.gas.*;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.Mekanism;
import mekanism.common.Upgrade;
import mekanism.common.base.*;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.holder.gas.GasTankHelper;
import mekanism.common.capabilities.holder.gas.IGasTankHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.config.MekanismConfig;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.inventory.slot.gas.GasInventorySlot;
import mekanism.common.integration.computer.IComputerIntegration;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.cache.CachedRecipe;
import mekanism.common.recipe.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.common.recipe.cache.IRecipeLookupHandler;
import mekanism.common.recipe.cache.OneInputCachedRecipe;
import mekanism.common.recipe.cache.RecipeCacheLookupMonitor;
import mekanism.common.recipe.cache.inputs.InputHelper;
import mekanism.common.recipe.cache.outputs.OutputHelper;
import mekanism.common.recipe.inputs.GasInput;
import mekanism.common.recipe.machines.SolarNeutronRecipe;
import mekanism.common.recipe.outputs.GasOutput;
import mekanism.common.security.ISecurityTile;
import mekanism.common.tier.BaseTier;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.upgrade.TierUpgradeData;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.TileComponentSecurity;
import mekanism.common.tile.component.TileComponentUpgrade;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.prefab.TileEntityContainerBlock;
import mekanism.common.util.*;
import mekceumoremachine.common.capability.ResizableGasTank;
import mekanism.multiblockmachine.common.registries.MultiblockMachineBlocks;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.interfaces.ILargeMachine;
import mekceumoremachine.common.tile.interfaces.ITierMachine;
import mekceumoremachine.common.upgrade.FirstSolarNeutronActivatorUpgradeData;
import mekceumoremachine.common.upgrade.LargeSolarNeutronActivatorUpgradeData;
import mekceumoremachine.common.upgrade.LargeMachineUpgradeDataApplier;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Optional.InterfaceList({
        @Optional.Interface(iface = "mekceumoremachine.common.tile.interfaces.ILargeMachine", modid = "mekanismmultiblockmachine"),
})
public class TileEntityTierSolarNeutronActivator extends TileEntityContainerBlock implements IUpgradeTile, IRedstoneControl, ISecurityTile, IComputerIntegration, ISideConfiguration, IConfigCardAccess,
        IBoundingBlock, ISustainedData, ITankManager, Upgrade.IUpgradeInfoHandler, IComparatorSupport, IActiveState, ITierMachine<MachineTier>, ILargeMachine, ISpecialSelectionWireframeTile, IRecipeLookupHandler<SolarNeutronRecipe> {


    public static final int MAX_GAS = 10000;
    private final RecipeCacheLookupMonitor<SolarNeutronRecipe> recipeCacheLookupMonitor = new RecipeCacheLookupMonitor<>(this);
    public ResizableGasTank inputTank;
    public ResizableGasTank outputTank;
    private SolarNeutronRecipe cachedRecipe;
    private int cachedRecipeVersion = -1;
    private int currentRedstoneLevel;
    private boolean isActive;
    private long lastActive = -1;
    private boolean needsRainCheck;
    private boolean seesSunThisTick;
    public TileComponentEjector ejectorComponent;
    public TileComponentConfig configComponent;
    public int operatingTicks;

    public int BASE_TICKS_REQUIRED;

    public int ticksRequired;
    private final int RECENT_THRESHOLD = 100;
    public TileComponentUpgrade upgradeComponent;
    public TileComponentSecurity securityComponent = new TileComponentSecurity(this);
    private RedstoneControl controlType = RedstoneControl.DISABLED;
    private GasInventorySlot inputSlot;
    private GasInventorySlot outputSlot;

    public MachineTier tier = MachineTier.BASIC;

    public TileEntityTierSolarNeutronActivator() {
        this(1);
    }

    public TileEntityTierSolarNeutronActivator(int baseTicksRequired) {
        super("TierSolarNeutronActivator");
        ticksRequired = BASE_TICKS_REQUIRED = baseTicksRequired;
        upgradeComponent = new TileComponentUpgrade(this, Upgrade.SPEED);
        configComponent = new TileComponentConfig(this, TransmissionType.ITEM, TransmissionType.GAS);
        initializeInventorySlots();
        configComponent.setupItemIOConfig(inputSlot, outputSlot);
        configComponent.setConfig(TransmissionType.ITEM, DataType.INPUT, DataType.EMPTY, DataType.OUTPUT, DataType.INPUT, DataType.INPUT, DataType.INPUT);
        configComponent.setCanEject(TransmissionType.ITEM, false);

        configComponent.setupIOConfig(TransmissionType.GAS, inputTank, outputTank, RelativeSide.FRONT, false, true);
        configComponent.setConfig(TransmissionType.GAS, DataType.INPUT, DataType.EMPTY, DataType.OUTPUT, DataType.INPUT, DataType.INPUT, DataType.INPUT);

        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(configComponent, TransmissionType.GAS);
    }

    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = createInventorySlotHelper();
        inputSlot = builder.addSlot(GasInventorySlot.fill(inputTank, listener, 5, 56));
        outputSlot = builder.addSlot(GasInventorySlot.drain(outputTank, listener, 155, 56));
        inputSlot.setSlotType(ContainerSlotType.INPUT);
        inputSlot.setSlotOverlay(SlotOverlay.MINUS);
        outputSlot.setSlotType(ContainerSlotType.OUTPUT);
        outputSlot.setSlotOverlay(SlotOverlay.PLUS);
        return builder.build();
    }

    @Override
    protected IGasTankHolder getInitialGasTanks(IContentsListener listener) {
        GasTankHelper builder = createGasTankHelper();
        builder.addTank(getOrCreateInputTank(listener));
        builder.addTank(getOrCreateOutputTank(listener));
        return builder.build();
    }

    private ResizableGasTank getOrCreateInputTank(IContentsListener listener) {
        if (inputTank == null) {
            inputTank = ResizableGasTank.input(tier.processes * MAX_GAS, gas -> RecipeHandler.Recipe.SOLAR_NEUTRON_ACTIVATOR.containsRecipe(gas), listener);
        }
        return inputTank;
    }

    private ResizableGasTank getOrCreateOutputTank(IContentsListener listener) {
        if (outputTank == null) {
            outputTank = ResizableGasTank.output(tier.processes * MAX_GAS, listener);
        }
        return outputTank;
    }

    @Override
    public void validate() {
        super.validate();
        // Cache the flag to know if rain matters where this block is placed
        needsRainCheck = world.provider.getBiomeForCoords(getPos()).canRain();
    }

    @Override
    public void onUpdateClient() {
        super.onUpdateClient();
        if (!isActive && lastActive > 0) {
            long updateDiff = world.getTotalWorldTime() - lastActive;
            if (updateDiff > RECENT_THRESHOLD) {
                MekanismUtils.updateBlock(world, getPos());
                lastActive = -1;
            }
        }
    }


    @Override
    public void onAsyncUpdateServer() {
        super.onAsyncUpdateServer();
        inputSlot.fillTank();
        outputSlot.drainTank();
        // TODO: Ideally the neutron activator should use the sky brightness to determine throughput; but
        // changing this would dramatically affect a lot of setups with Fusion reactors which can take
        // a long time to relight. I don't want to be chased by a mob right now, so just doing basic
        // rain checks.
        boolean seesSun = world.isDaytime() && world.canSeeSky(getPos().up()) && !world.provider.isNether();
        if (needsRainCheck) {
            seesSun &= !(world.isRaining() || world.isThundering());
        }

        seesSunThisTick = seesSun;
        if (!recipeCacheLookupMonitor.updateAndProcess()) {
            setActive(false);
        }

        // Every 20 ticks (once a second), send update to client. Note that this is a 50% reduction in network
        // traffic from previous implementation that send the update every 10 ticks.
        if (world.getTotalWorldTime() % 20 == 0) {
            Mekanism.packetHandler.sendUpdatePacket(this);
        }

        int newRedstoneLevel = getRedstoneLevel();
        if (newRedstoneLevel != currentRedstoneLevel) {
            updateComparatorOutputLevelSync();
            currentRedstoneLevel = newRedstoneLevel;
        }
    }

    public int getUpgradedUsage(SolarNeutronRecipe recipe) {
        int possibleProcess = Math.min((int) Math.pow(2, upgradeComponent.getUpgrades(Upgrade.SPEED)), MekanismConfig.current().mekce.MAXspeedmachines.val());
        possibleProcess *= tier.processes;
        return Math.max(possibleProcess, 1);
    }

    public SolarNeutronRecipe getRecipe() {
        refreshRecipeLookupCache();
        GasInput input = getInput();
        if (cachedRecipe == null || !input.testEquality(cachedRecipe.getInput())) {
            cachedRecipe = RecipeHandler.getSolarNeutronRecipe(getInput());
        }
        return cachedRecipe;
    }

    private void refreshRecipeLookupCache() {
        int recipeVersion = RecipeHandler.getGlobalRecipeVersion();
        if (cachedRecipeVersion != recipeVersion) {
            cachedRecipe = null;
            cachedRecipeVersion = recipeVersion;
        }
    }

    @Override
    public void onRecipeCacheInvalidated(int cacheIndex) {
        cachedRecipe = null;
        cachedRecipeVersion = RecipeHandler.getGlobalRecipeVersion();
    }

    public GasInput getInput() {
        return new GasInput(inputTank.getGas());
    }

    public boolean canOperate(SolarNeutronRecipe recipe) {
        return recipe != null && recipe.canOperate(inputTank, outputTank);
    }

    @Override
    public SolarNeutronRecipe getRecipe(int cacheIndex) {
        return getRecipe();
    }

    @Override
    public CachedRecipe<SolarNeutronRecipe> createNewCachedRecipe(SolarNeutronRecipe recipe, int cacheIndex) {
        return new OneInputCachedRecipe<>(recipe, () -> false,
              InputHelper.getGasInputHandler(inputTank, RecipeError.NOT_ENOUGH_INPUT),
              OutputHelper.getGasOutputHandler(outputTank, RecipeError.NOT_ENOUGH_OUTPUT_SPACE),
              () -> recipe.getInput().ingredient,
              input -> input != null && input.isGasEqual(recipe.getInput().ingredient),
              input -> recipe.getOutput().output.copy(),
              input -> input == null || input.amount <= 0,
              output -> output == null || output.amount <= 0)
              .setCanHolderFunction(() -> seesSunThisTick && MekanismUtils.canFunction(this))
              .setActive(this::setActive)
              .setRequiredTicks(() -> 1)
              .setBaselineMaxOperations(() -> getUpgradedUsage(recipe));
    }

    public Map<GasInput, SolarNeutronRecipe> getRecipes() {
        return RecipeHandler.Recipe.SOLAR_NEUTRON_ACTIVATOR.get();
    }

    @Override
    public void handlePacketData(ByteBuf dataStream) {
        super.handlePacketData(dataStream);
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            MachineTier prevTier = tier;
            tier = MachineTier.byIndex(dataStream.readInt());
            inputTank.setMaxGas(tier.processes * MAX_GAS);
            outputTank.setMaxGas(tier.processes * MAX_GAS);
            boolean newActive = dataStream.readBoolean();
            boolean stateChange = newActive != isActive;
            isActive = newActive;
            if (stateChange && !isActive) {
                // Switched off; note the time
                lastActive = world.getTotalWorldTime();
            } else if (stateChange && isActive) {
                // Switching on; if lastActive is not currently set, trigger a lighting update
                // and make sure lastActive is clear
                if (lastActive == -1) {
                    MekanismUtils.updateBlock(world, getPos());
                }
                lastActive = -1;
            }
            controlType = MekanismUtils.getByIndex(RedstoneControl.values(), dataStream.readInt(), controlType);
            operatingTicks = dataStream.readInt();
            ticksRequired = dataStream.readInt();
            TileUtils.readTankData(dataStream, inputTank);
            TileUtils.readTankData(dataStream, outputTank);
            if (prevTier != tier) {
                MekanismUtils.updateBlock(world, getPos());
            }
        }
    }

    @Override
    public TileNetworkList getNetworkedData(TileNetworkList data) {
        super.getNetworkedData(data);
        data.add(tier.ordinal());
        data.add(isActive);
        data.add(controlType.ordinal());
        data.add(operatingTicks);
        data.add(ticksRequired);
        TileUtils.addTankData(data, inputTank);
        TileUtils.addTankData(data, outputTank);
        return data;
    }

    @Override
    public void readCustomNBT(NBTTagCompound nbtTags) {
        super.readCustomNBT(nbtTags);
        tier = MachineTier.byIndex(nbtTags.getInteger("tier"));
        inputTank.setMaxGas(tier.processes * MAX_GAS);
        outputTank.setMaxGas(tier.processes * MAX_GAS);
        isActive = nbtTags.getBoolean("isActive");
        controlType = MekanismUtils.getByIndex(RedstoneControl.values(), nbtTags.getInteger("controlType"), controlType);
        operatingTicks = nbtTags.getInteger("operatingTicks");
        inputTank.read(nbtTags.getCompoundTag("inputTank"));
        outputTank.read(nbtTags.getCompoundTag("outputTank"));
    }

    @Override
    public void writeCustomNBT(NBTTagCompound nbtTags) {
        super.writeCustomNBT(nbtTags);
        nbtTags.setInteger("tier", tier.ordinal());
        nbtTags.setBoolean("isActive", isActive);
        nbtTags.setInteger("controlType", controlType.ordinal());
        nbtTags.setInteger("operatingTicks", operatingTicks);
        nbtTags.setTag("inputTank", inputTank.write(new NBTTagCompound()));
        nbtTags.setTag("outputTank", outputTank.write(new NBTTagCompound()));
    }

    @Override
    public void onPlace() {
        MekanismUtils.makeBoundingBlock(world, getPos().up(), Coord4D.get(this));
    }

    @Override
    public void onBreak() {
        world.setBlockToAir(getPos().up());
        world.setBlockToAir(getPos());
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, EnumFacing side) {
        if (isCapabilityDisabled(capability, side)) {
            return false;
        }
        return capability == Capabilities.CONFIG_CARD_CAPABILITY || super.hasCapability(capability, side);
    }

    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, EnumFacing side) {
        if (isCapabilityDisabled(capability, side)) {
            return null;
        } else if (capability == Capabilities.CONFIG_CARD_CAPABILITY) {
            return Capabilities.CONFIG_CARD_CAPABILITY.cast(this);
        }
        return super.getCapability(capability, side);
    }

    @Override
    public boolean isCapabilityDisabled(@Nonnull Capability<?> capability, EnumFacing side) {
        return configComponent.isCapabilityDisabled(capability, side, facing) || super.isCapabilityDisabled(capability, side);
    }


    @Override
    public void writeSustainedData(ItemStack itemStack) {
        if (inputTank.getGas() != null) {
            ItemDataUtils.setCompound(itemStack, "inputTank", inputTank.getGas().write(new NBTTagCompound()));
        }
        if (outputTank.getGas() != null) {
            ItemDataUtils.setCompound(itemStack, "outputTank", outputTank.getGas().write(new NBTTagCompound()));
        }
    }

    @Override
    public void readSustainedData(ItemStack itemStack) {
        inputTank.setGas(GasStack.readFromNBT(ItemDataUtils.getCompound(itemStack, "inputTank")));
        outputTank.setGas(GasStack.readFromNBT(ItemDataUtils.getCompound(itemStack, "outputTank")));
    }

    @Override
    public boolean canSetFacing(@Nonnull EnumFacing facing) {
        return facing != EnumFacing.DOWN && facing != EnumFacing.UP;
    }

    @Override
    public boolean renderUpdate() {
        return false;
    }

    @Override
    public boolean lightUpdate() {
        return false;
    }

    @Override
    public Object[] getManagedTanks() {
        return new Object[]{inputTank, outputTank};
    }


    @Override
    public List<String> getInfo(Upgrade upgrade) {
        return upgrade == Upgrade.SPEED ? upgrade.getExpScaledInfo(this) : upgrade.getMultScaledInfo(this);
    }

    @Nonnull
    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }

    @Override
    public int getRedstoneLevel() {
        return MekanismUtils.redstoneLevelFromContents(inputTank.getStored(), inputTank.getMaxGas());
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
    public RedstoneControl getControlType() {
        return controlType;
    }

    @Override
    public void setControlType(RedstoneControl type) {
        controlType = Objects.requireNonNull(type);
        MekanismUtils.saveChunk(this);
    }

    @Override
    public boolean canPulse() {
        return false;
    }

    @Override
    public TileComponentUpgrade getComponent() {
        return upgradeComponent;
    }

    @Override
    public TileComponentSecurity getSecurity() {
        return securityComponent;
    }

    @Override
    public boolean getActive() {
        return isActive;
    }

    @Override
    public void setActive(boolean active) {
        boolean stateChange = isActive != active;
        if (stateChange) {
            isActive = active;
            Mekanism.packetHandler.sendUpdatePacket(this);
        }
    }

    @Override
    public boolean wasActiveRecently() {
        // If the machine is currently active or it flipped off within our threshold,
        // we'll consider it recently active.
        return isActive || (lastActive > 0 && (world.getTotalWorldTime() - lastActive) < RECENT_THRESHOLD);
    }

    @Override
    public int getBlockGuiID(Block block, int metadata) {
        return 5;
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
        inputTank.setMaxGas(tier.processes * MAX_GAS);
        outputTank.setMaxGas(tier.processes * MAX_GAS);
        upgradeComponent.getSupportedTypes().forEach(this::recalculateUpgradables);
        Mekanism.packetHandler.sendUpdatePacket(this);
        markNoUpdateSync();
        return true;
    }

    @Override
    public MachineTier getTier() {
        return tier;
    }

    @Nonnull
    @Override
    public String getName() {
        return LangUtils.localize("tile.TierSolarNeutronActivator." + tier.getBaseTier().getSimpleName() + ".name");
    }

    public boolean isUpgrade = true;

    @Override
    public IUpgradeData getUpgradeData(BaseTier upgradeTier) {
        if (upgradeTier == BaseTier.ULTIMATE && tier == MachineTier.ULTIMATE) {
            return new LargeSolarNeutronActivatorUpgradeData(upgradeTier, this);
        }
        return ITierMachine.super.getUpgradeData(upgradeTier);
    }

    @Override
    public IBlockState getUpgradeResult(BaseTier upgradeTier) {
        return upgradeTier == BaseTier.ULTIMATE && tier == MachineTier.ULTIMATE ? MultiblockMachineBlocks.LargeSolarNeutronActivator.getDefaultState() : null;
    }

    @Override
    public void prepareForUpgrade() {
        isUpgrade = false;
    }

    @Override
    public boolean parseUpgradeData(IUpgradeData upgradeData) {
        if (upgradeData instanceof FirstSolarNeutronActivatorUpgradeData data && data.getUpgradeTier() == tier.getBaseTier()) {
            onPlace();
            LargeMachineUpgradeDataApplier.applyCommon(this, data, upgradeComponent, securityComponent);
            operatingTicks = data.operatingTicks;
            configComponent.read(data.configComponentData.copy());
            ejectorComponent.read(data.ejectorComponentData.copy());
            ejectorComponent.setOutputData(configComponent, TransmissionType.GAS);
            inputTank.setGas(data.inputGas == null ? null : data.inputGas.copy());
            outputTank.setGas(data.outputGas == null ? null : data.outputGas.copy());
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
    @Optional.Method(modid = "mekanismmultiblockmachine")
    public boolean canLargeMachineUpgrade(EntityPlayer player) {
        if (tier != MachineTier.ULTIMATE) {
            return false;
        }
        //检查范围内是否能摆放
        BlockPos.MutableBlockPos testPos = new BlockPos.MutableBlockPos();
        for (int yPos = 0; yPos <= 2; yPos++) {
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
    @Optional.Method(modid = "mekanismmultiblockmachine")
    public boolean applyLargeMachineUpgrade() {
        return ILargeMachine.super.applyLargeMachineUpgrade();
    }

    @Override
    public boolean shouldDumpRadiation(){
        return isUpgrade;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Class<?> getSelectionWireframeModelClass() {
        return mekceumoremachine.client.model.machine.ModelTierSolarNeutronActivator.class;
    }

    @Override
    public String[] getSelectionWireframeIgnoredRendererFieldNames() {
        return new String[]{"laserBeamToggle"};
    }

}
