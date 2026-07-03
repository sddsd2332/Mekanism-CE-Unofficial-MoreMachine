package mekceumoremachine.common.tile.machine;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.Coord4D;
import mekanism.api.EnumColor;
import mekanism.api.IConfigurable;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.api.TileNetworkList;
import mekanism.common.Mekanism;
import mekanism.common.MekanismFluids;
import mekanism.common.PacketHandler;
import mekanism.common.Upgrade;
import mekanism.common.base.*;
import mekanism.common.block.states.BlockStateMachine;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.fluid.FluidTankHelper;
import mekanism.common.capabilities.holder.fluid.IFluidTankHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.config.MekanismConfig;
import mekanism.common.integration.computer.IComputerIntegration;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableBoolean;
import mekanism.common.inventory.container.sync.SyncableFluidStack;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.inventory.slot.FluidInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.security.ISecurityTile;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.component.TileComponentSecurity;
import mekanism.common.tile.component.TileComponentUpgrade;
import mekanism.common.tile.prefab.TileEntityElectricBlock;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.util.*;
import mekceumoremachine.common.capability.ResizableFluidTank;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.interfaces.ITierMachine;
import mekceumoremachine.common.upgrade.FirstElectricPumpUpgradeData;
import mekceumoremachine.common.upgrade.LargeMachineUpgradeDataApplier;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class TileEntityTierElectricPump extends TileEntityElectricBlock implements ISustainedTank, IConfigurable, IRedstoneControl, IUpgradeTile,
        ITankManager, IComputerIntegration, ISecurityTile, IComparatorSupport, ITierMachine<MachineTier> {

    private static final String[] methods = new String[]{"reset"};

    public static final int MAX_FLUID = 10000;
    /**
     * This pump's tank
     */
    public ResizableFluidTank fluidTank;
    /**
     * The type of fluid this pump is pumping
     */
    public Fluid activeType;
    private boolean usedEnergy;
    public MachineTier tier = MachineTier.BASIC;

    public double BASE_ENERGY_PER_TICK  = BlockStateMachine.MachineType.ELECTRIC_PUMP.getUsage() * tier.processes;
    public double energyPerTick = BASE_ENERGY_PER_TICK;
    /**
     * How many ticks it takes to run an operation.
     */
    public int BASE_TICKS_REQUIRED = 20;
    public int ticksRequired = BASE_TICKS_REQUIRED;

    /**
     * How many ticks this machine has been operating for.
     */
    public int operatingTicks;
    /**
     * The nodes that have full sources near them or in them
     */
    public Set<Coord4D> recurringNodes = new ObjectOpenHashSet<>();
    /**
     * This machine's current RedstoneControl type.
     */
    public RedstoneControl controlType = RedstoneControl.DISABLED;
    public TileComponentUpgrade upgradeComponent = new TileComponentUpgrade(this);
    public TileComponentSecurity securityComponent = new TileComponentSecurity(this);

    private int currentRedstoneLevel;
    private FluidInventorySlot inputSlot;
    private OutputInventorySlot outputSlot;
    private EnergyInventorySlot energySlot;

    public TileEntityTierElectricPump() {
        super("TierElectricPump", 0);
        upgradeComponent.setSupported(Upgrade.FILTER);
        initializeInventorySlots();
    }

    @Override
    protected IFluidTankHolder getInitialFluidTanks(IContentsListener listener) {
        FluidTankHelper builder = FluidTankHelper.forSide(() -> facing);
        builder.addTank(getOrCreateFluidTank(listener), RelativeSide.TOP);
        return builder.build();
    }

    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        EnergyContainerHelper builder = createEnergyContainerHelper();
        builder.addContainer(this, RelativeSide.BACK);
        return builder.build();
    }

    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = InventorySlotHelper.forSide(() -> facing);
        inputSlot = builder.addSlot(FluidInventorySlot.drain(fluidTank, listener, 28, 20), RelativeSide.TOP);
        inputSlot.setSlotOverlay(SlotOverlay.INPUT);
        outputSlot = builder.addSlot(OutputInventorySlot.at(listener, 28, 51), RelativeSide.BOTTOM);
        outputSlot.setSlotOverlay(SlotOverlay.OUTPUT);
        energySlot = builder.addSlot(EnergyInventorySlot.fillOrConvert(this, this::getWorld, listener, 143, 35), RelativeSide.BACK);
        return builder.build();
    }

    private ResizableFluidTank getOrCreateFluidTank(IContentsListener listener) {
        if (fluidTank == null) {
            fluidTank = ResizableFluidTank.output(tier.processes * MAX_FLUID, listener);
        }
        return fluidTank;
    }

    @Override
    public void onUpdateServer() {
        super.onUpdateServer();
        energySlot.fillContainerOrConvert();
        inputSlot.drainTank(outputSlot);

        double clientEnergyUsed = 0;
        if (MekanismUtils.canFunction(this) && (fluidTank.isEmpty() || estimateIncrementAmount() <= fluidTank.getNeeded()) &&
              Double.compare(getMainEnergyContainer().extract(energyPerTick, Action.SIMULATE, AutomationType.INTERNAL), energyPerTick) == 0) {
            if (activeType != null) {
                clientEnergyUsed = getMainEnergyContainer().extract(energyPerTick, Action.EXECUTE, AutomationType.INTERNAL);
            }
            if ((operatingTicks + 1) < ticksRequired) {
                operatingTicks++;
            } else {
                operatingTicks = 0;
                if (suck()) {
                    if (clientEnergyUsed <= 0) {
                        clientEnergyUsed = getMainEnergyContainer().extract(energyPerTick, Action.EXECUTE, AutomationType.INTERNAL);
                    }
                } else {
                    reset();
                }
            }
        }
        usedEnergy = clientEnergyUsed > 0;

        if (!fluidTank.isEmpty()) {
            FluidUtils.emit(Collections.singleton(EnumFacing.UP), fluidTank, this,
                  Math.min(256 * tier.processes * (upgradeComponent.getUpgrades(Upgrade.SPEED) + 1), fluidTank.getFluidAmount()));
        }
        int newRedstoneLevel = getRedstoneLevel();
        if (newRedstoneLevel != currentRedstoneLevel) {
            updateComparatorOutputLevelSync();
            currentRedstoneLevel = newRedstoneLevel;
        }
    }

    @Override
    public boolean supportsAsync() {
        return false;
    }

    public boolean hasFilter() {
        return upgradeComponent.isUpgradeInstalled(Upgrade.FILTER);
    }

    public int estimateIncrementAmount() {
        Fluid fluid = fluidTank.getFluid() != null ? fluidTank.getFluid().getFluid() : activeType;
        if (fluid == MekanismFluids.HeavyWater) {
            return 10 * tier.processes;
        } else if (fluid == FluidRegistry.WATER && !MekanismConfig.current().general.pumpWaterSources.val()) {
            return Fluid.BUCKET_VOLUME * tier.processes;
        }
        return Fluid.BUCKET_VOLUME;
    }

    private boolean suck() {
        List<Coord4D> tempPumpList = Arrays.asList(recurringNodes.toArray(new Coord4D[0]));
        Collections.shuffle(tempPumpList);

        //First see if there are any fluid blocks touching the pump - if so, sucks and adds the location to the recurring list
        for (EnumFacing orientation : EnumFacing.VALUES) {
            Coord4D wrapper = Coord4D.get(this).offset(orientation);
            if (suck(wrapper, true)) {
                return true;
            }
        }

        //Finally, go over the recurring list of nodes and see if there is a fluid block available to suck - if not, will iterate around the recurring block, attempt to suck,
        //and then add the adjacent block to the recurring list
        for (Coord4D wrapper : tempPumpList) {
            if (suck(wrapper, false)) {
                return true;
            }

            //Add all the blocks surrounding this recurring node to the recurring node list
            for (EnumFacing orientation : EnumFacing.VALUES) {
                Coord4D side = wrapper.offset(orientation);
                if (Coord4D.get(this).distanceTo(side) <= MekanismConfig.current().general.maxPumpRange.val()) {
                    if (suck(side, true)) {
                        return true;
                    }
                }
            }
            recurringNodes.remove(wrapper);
        }
        return false;
    }

    private boolean suck(Coord4D wrapper, boolean addRecurring) {
        FluidStack fluid = getFluid(world, wrapper, hasFilter());
        if (validFluid(fluid)) {
            suck(fluid, wrapper, addRecurring);
            return true;
        }
        return false;
    }

    private boolean validFluid(@Nullable FluidStack fluid) {
        return fluid != null && fluid.amount > 0 && (activeType == null || fluid.getFluid() == activeType) &&
              (fluidTank.isEmpty() || fluidTank.isFluidEqual(fluid)) && fluid.amount <= fluidTank.getNeeded();
    }

    private void suck(FluidStack fluid, Coord4D coord, boolean addRecurring) {
        activeType = fluid.getFluid();
        if (addRecurring) {
            recurringNodes.add(coord);
        }
        fluidTank.insert(fluid, Action.EXECUTE, AutomationType.INTERNAL);
        if (shouldTake(fluid, coord)) {
            world.setBlockToAir(coord.getPos());
        }
    }

    public void reset() {
        activeType = null;
        recurringNodes.clear();
    }

    private boolean shouldTake(FluidStack fluid, Coord4D coord) {
        if (fluid.getFluid() == FluidRegistry.WATER || fluid.getFluid() == MekanismFluids.HeavyWater) {
            return MekanismConfig.current().general.pumpWaterSources.val();
        }
        return true;
    }


    public FluidStack getFluid(World world, Coord4D pos, boolean filter) {
        IBlockState state = pos.getBlockState(world);
        Block block = state.getBlock();
        if ((block == Blocks.WATER || block == Blocks.FLOWING_WATER) && state.getValue(BlockLiquid.LEVEL) == 0) {
            if (!filter) {
                return new FluidStack(FluidRegistry.WATER, MekanismConfig.current().general.pumpWaterSources.val() ? Fluid.BUCKET_VOLUME : Fluid.BUCKET_VOLUME * tier.processes);
            }
            return new FluidStack(MekanismFluids.HeavyWater, 10 * tier.processes);
        } else if ((block == Blocks.LAVA || block == Blocks.FLOWING_LAVA) && state.getValue(BlockLiquid.LEVEL) == 0) {
            return new FluidStack(FluidRegistry.LAVA, Fluid.BUCKET_VOLUME);
        } else if (block instanceof IFluidBlock fluid) {
            if (state.getProperties().containsKey(BlockFluidBase.LEVEL) && state.getValue(BlockFluidBase.LEVEL) == 0) {
                return fluid.drain(world, pos.getPos(), false);
            }
        }
        return null;
    }

    @Override
    public void handlePacketData(ByteBuf dataStream) {
        super.handlePacketData(dataStream);
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            MachineTier prevTier = tier;
            tier = MachineTier.byIndex(dataStream.readInt());
            updateTierDependentValues();
            TileUtils.readTankData(dataStream, fluidTank);
            controlType = MekanismUtils.getByIndex(RedstoneControl.values(), dataStream.readInt(), controlType);
            activeType = dataStream.readBoolean() ? FluidRegistry.getFluid(PacketHandler.readString(dataStream)) : null;
            usedEnergy = dataStream.readBoolean();
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
        data.add(controlType.ordinal());
        data.add(activeType != null);
        if (activeType != null) {
            data.add(FluidRegistry.getFluidName(activeType));
        }
        data.add(usedEnergy);
        return data;
    }

    @Override
    public void writeCustomNBT(NBTTagCompound nbtTags) {
        super.writeCustomNBT(nbtTags);
        nbtTags.setInteger("tier", tier.ordinal());
        nbtTags.setInteger("operatingTicks", operatingTicks);

        if (activeType != null) {
            nbtTags.setString("activeType", FluidRegistry.getFluidName(activeType));
        }

        if (fluidTank.getFluid() != null) {
            nbtTags.setTag("fluidTank", fluidTank.writeToNBT(new NBTTagCompound()));
        }

        nbtTags.setInteger("controlType", controlType.ordinal());

        NBTTagList recurringList = new NBTTagList();
        recurringNodes.forEach(wrapper -> {
            NBTTagCompound tagCompound = new NBTTagCompound();
            wrapper.write(tagCompound);
            recurringList.appendTag(tagCompound);
        });
        if (recurringList.tagCount() != 0) {
            nbtTags.setTag("recurringNodes", recurringList);
        }
    }

    @Override
    public void readCustomNBT(NBTTagCompound nbtTags) {
        super.readCustomNBT(nbtTags);
        tier = MachineTier.byIndex(nbtTags.getInteger("tier"));
        updateTierDependentValues();
        operatingTicks = nbtTags.getInteger("operatingTicks");
        if (nbtTags.hasKey("activeType")) {
            activeType = FluidRegistry.getFluid(nbtTags.getString("activeType"));
        }
        if (nbtTags.hasKey("fluidTank")) {
            fluidTank.readFromNBT(nbtTags.getCompoundTag("fluidTank"));
        }
        sanitizeAndClampTank();
        if (nbtTags.hasKey("controlType")) {
            controlType = MekanismUtils.getByIndex(RedstoneControl.values(), nbtTags.getInteger("controlType"), controlType);
        }
        if (nbtTags.hasKey("recurringNodes")) {
            NBTTagList tagList = nbtTags.getTagList("recurringNodes", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < tagList.tagCount(); i++) {
                recurringNodes.add(Coord4D.read(tagList.getCompoundTagAt(i)));
            }
        }
    }

    private void updateTierDependentValues() {
        fluidTank.setCapacity(tier.processes * MAX_FLUID);
        BASE_ENERGY_PER_TICK = BlockStateMachine.MachineType.ELECTRIC_PUMP.getUsage() * tier.processes;
        ticksRequired = MekanismUtils.getTicks(this, BASE_TICKS_REQUIRED);
        energyPerTick = MekanismUtils.getEnergyPerTick(this, BASE_ENERGY_PER_TICK);
    }

    private void sanitizeAndClampTank() {
        FluidStack stored = fluidTank.getFluid();
        if (stored == null || stored.getFluid() == null || stored.amount <= 0) {
            fluidTank.setEmpty();
        } else {
            fluidTank.setStackSize(stored.amount, Action.EXECUTE);
        }
    }


    @Override
    public void setEnergy(double energy) {
        super.setEnergy(energy);
        int newRedstoneLevel = getRedstoneLevel();
        if (newRedstoneLevel != currentRedstoneLevel) {
            markNoUpdateSync();
            currentRedstoneLevel = newRedstoneLevel;
        }
    }

    @Override
    public boolean sideIsConsumer(EnumFacing side) {
        return facing.getOpposite() == side;
    }

    @Override
    public boolean canSetFacing(@Nonnull EnumFacing facing) {
        return facing != EnumFacing.DOWN && facing != EnumFacing.UP;
    }

    public void setFluidStack(FluidStack fluidStack, Object... data) {
        fluidTank.setFluid(fluidStack);
        sanitizeAndClampTank();
    }

    public FluidStack getFluidStack(Object... data) {
        return fluidTank.getFluid();
    }

    public boolean hasTank(Object... data) {
        return true;
    }

    @Override
    public EnumActionResult onSneakRightClick(EntityPlayer player, EnumFacing side) {
        reset();
        player.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + Mekanism.LOG_TAG + " " + EnumColor.GREY + LangUtils.localize("tooltip.configurator.pumpReset")));
        return EnumActionResult.SUCCESS;
    }

    @Override
    public EnumActionResult onRightClick(EntityPlayer player, EnumFacing side) {
        return EnumActionResult.PASS;
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, EnumFacing side) {
        return capability == Capabilities.CONFIGURABLE_CAPABILITY || super.hasCapability(capability, side);
    }

    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, EnumFacing side) {
        if (capability == Capabilities.CONFIGURABLE_CAPABILITY) {
            return Capabilities.CONFIGURABLE_CAPABILITY.cast(this);
        }
        return super.getCapability(capability, side);
    }

    @Override
    public RedstoneControl getControlType() {
        return controlType;
    }

    @Override
    public void setControlType(RedstoneControl type) {
        controlType = type;
        MekanismUtils.saveChunk(this);
    }

    @Override
    public boolean canPulse() {
        return true;
    }

    @Override
    public TileComponentUpgrade getComponent() {
        return upgradeComponent;
    }

    @Override
    public Object[] getManagedTanks() {
        return new Object[]{fluidTank};
    }

    @Override
    public String[] getMethods() {
        return methods;
    }

    @Override
    public Object[] invoke(int method, Object[] arguments) throws NoSuchMethodException {
        if (method == 0) {
            reset();
            return new Object[]{"Pump calculation reset."};
        }
        throw new NoSuchMethodException();
    }

    @Override
    public TileComponentSecurity getSecurity() {
        return securityComponent;
    }

    public boolean usedEnergy() {
        return usedEnergy;
    }

    @Nullable
    public FluidStack getActiveType() {
        return activeType == null ? null : new FluidStack(activeType, estimateIncrementAmount());
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableBoolean.create(this::usedEnergy, value -> usedEnergy = value));
        container.track(SyncableFluidStack.create(this::getActiveType, value -> activeType = value == null ? null : value.getFluid()));
    }

    @Override
    public void recalculateUpgradables(Upgrade upgrade) {
        super.recalculateUpgradables(upgrade);
        BASE_ENERGY_PER_TICK = BlockStateMachine.MachineType.ELECTRIC_PUMP.getUsage() * tier.processes;
        switch (upgrade) {
            case SPEED:
                ticksRequired = MekanismUtils.getTicks(this, BASE_TICKS_REQUIRED);
            case ENERGY:
                energyPerTick = MekanismUtils.getEnergyPerTick(this, BASE_ENERGY_PER_TICK);
                maxEnergy = MekanismUtils.getMaxEnergy(this, getTierEnergy());
                setEnergy(Math.min(getMaxEnergy(), getEnergy()));
            default:
                break;
        }
    }


    @Override
    public int getRedstoneLevel() {
        return MekanismUtils.redstoneLevelFromContents(fluidTank.getFluidAmount(), fluidTank.getCapacity());
    }

    @Override
    public int getBlockGuiID(Block block, int metadata) {
        return 1;
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
        updateTierDependentValues();
        upgradeComponent.getSupportedTypes().forEach(this::recalculateUpgradables);
        Mekanism.packetHandler.sendUpdatePacket(this);
        markNoUpdateSync();
        return true;
    }

    @Override
    public boolean parseUpgradeData(IUpgradeData upgradeData) {
        if (upgradeData instanceof FirstElectricPumpUpgradeData data && data.getUpgradeTier() == tier.getBaseTier()) {
            LargeMachineUpgradeDataApplier.applyCommon(this, data, upgradeComponent, securityComponent);
            operatingTicks = data.operatingTicks;
            fluidTank.setFluid(data.fluid == null ? null : data.fluid.copy());
            activeType = data.activeType;
            recurringNodes.clear();
            recurringNodes.addAll(data.recurringNodes);
            updateTierDependentValues();
            sanitizeAndClampTank();
            LargeMachineUpgradeDataApplier.finish(this, upgradeComponent);
            return true;
        }
        return ITierMachine.super.parseUpgradeData(upgradeData);
    }

    @Nonnull
    @Override
    public String getName() {
        return LangUtils.localize("tile.TierElectricPump." + tier.getBaseTier().getSimpleName() + ".name");
    }

    @Override
    public MachineTier getTier() {
        return tier;
    }

    @Override
    public double getMaxEnergy() {
        return upgradeComponent.isUpgradeInstalled(Upgrade.ENERGY) ? MekanismUtils.getMaxEnergy(this, getTierEnergy()) : getTierEnergy();
    }

    public double getTierEnergy() {
        return BlockStateMachine.MachineType.ELECTRIC_PUMP.getStorage() * tier.processes;
    }

}
