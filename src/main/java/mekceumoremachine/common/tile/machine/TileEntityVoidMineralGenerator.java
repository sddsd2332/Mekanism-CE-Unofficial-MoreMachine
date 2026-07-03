package mekceumoremachine.common.tile.machine;

import io.netty.buffer.ByteBuf;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.Coord4D;
import mekanism.api.IConfigCardAccess;
import mekanism.api.IContentsListener;
import mekanism.api.TileNetworkList;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.Mekanism;
import mekanism.common.Upgrade;
import mekanism.common.base.IBoundingBlock;
import mekanism.common.base.IGuiProvider;
import mekanism.common.base.ISideConfiguration;
import mekanism.common.base.ISpecialSelectionWireframeTile;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.prefab.TileEntityOperationalMachine;
import mekanism.common.util.*;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.config.MoreMachineConfig;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.interfaces.ITierMachine;
import mekceumoremachine.common.util.VoidMineralGeneratorUitls;
import net.minecraft.block.Block;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class TileEntityVoidMineralGenerator extends TileEntityOperationalMachine implements ISideConfiguration, IConfigCardAccess, ITierMachine<MachineTier>, IBoundingBlock, ISpecialSelectionWireframeTile {

    public TileComponentEjector ejectorComponent;
    public TileComponentConfig configComponent;

    public MachineTier tier = MachineTier.BASIC;
    private int currentRedstoneLevel;
    private final List<IInventorySlot> outputSlots = new ArrayList<>();
    private EnergyInventorySlot energySlot;

    public TileEntityVoidMineralGenerator() {
        super("machine.smelter", "VoidMineralGenerator", MoreMachineConfig.current().config.VoidMineralGeneratorEnergyStorge.val(), MoreMachineConfig.current().config.VoidMineralGeneratorEnergyUsage.val(), 0, MoreMachineConfig.current().config.VoidMineralGeneratorTick.val());
        configComponent = new TileComponentConfig(this, TransmissionType.ITEM, TransmissionType.ENERGY);
        upgradeComponent.setSupported(Upgrade.THREAD);
        initializeInventorySlots();
        configComponent.addItemSlotInfo(DataType.ENERGY, energySlot);
        configComponent.addItemSlotInfo(DataType.OUTPUT, outputSlots);
        configComponent.setConfig(TransmissionType.ITEM, DataType.OUTPUT, DataType.EMPTY, DataType.OUTPUT, DataType.OUTPUT, DataType.OUTPUT, DataType.OUTPUT);
        configComponent.setInputConfig(TransmissionType.ENERGY);
        configComponent.setConfig(TransmissionType.ENERGY, DataType.INPUT, DataType.EMPTY, DataType.INPUT, DataType.INPUT, DataType.INPUT, DataType.INPUT);

        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(configComponent, TransmissionType.ITEM);
    }

    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = createInventorySlotHelper();
        outputSlots.clear();
        energySlot = builder.addSlot(EnergyInventorySlot.fillOrConvert(getMainEnergyContainer(), this::getWorld, listener, 15, 57));
        for (int index = 0; index < 81; index++) {
            int x = 81 + (index % 9) * 18;
            int y = 14 + (index / 9) * 18;
            outputSlots.add(builder.addSlot(OutputInventorySlot.at(listener, x, y)));
        }
        return builder.build();
    }


    @Override
    public void onAsyncUpdateServer() {
        super.onAsyncUpdateServer();
        energySlot.fillContainerOrConvert();
        List<ItemStack> available = VoidMineralGeneratorUitls.getCanOre();
        if (MekanismUtils.canFunction(this) && !available.isEmpty() && canFitAnyOutput(available) &&
              Double.compare(getMainEnergyContainer().extract(energyPerTick, Action.SIMULATE, AutomationType.INTERNAL), energyPerTick) == 0) {
            setActive(true);
            operatingTicks++;
            getMainEnergyContainer().extract(energyPerTick, Action.EXECUTE, AutomationType.INTERNAL);
            if (operatingTicks >= ticksRequired) {
                //生成物品到库存
                GenerateItem(available);
                operatingTicks = 0;
            }
        } else if (prevEnergy >= getEnergy()) {
            setActive(false);
        }
        prevEnergy = getEnergy();
    }

    private boolean canFitAnyOutput(List<ItemStack> available) {
        for (ItemStack template : available) {
            if (template != null && !template.isEmpty() && canFitOutput(StackUtils.size(template, Math.min(getThread() * tier.processes, template.getMaxStackSize())))) {
                return true;
            }
        }
        return false;
    }

    private boolean canFitOutput(ItemStack stack) {
        ItemStack remainder = stack.copy();
        for (IInventorySlot outputSlot : outputSlots) {
            remainder = outputSlot.insertItem(remainder, Action.SIMULATE, AutomationType.INTERNAL);
            if (remainder.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public void GenerateItem(List<ItemStack> available) {
        int idx = getWorldNN().rand.nextInt(available.size());
        ItemStack template = available.get(idx);
        if (template == null || template.isEmpty()) return;
        List<ItemStack> stacks = new ArrayList<>();
        int count = getThread();
        count *= tier.processes;
        int max = template.getMaxStackSize();
        // Split total count into multiple stacks, each <= max
        while (count > 0) {
            int toAdd = Math.min(count, max);
            stacks.add(StackUtils.size(template, toAdd));
            count -= toAdd;
        }
        addInventory(stacks);
    }

    public int getThread() {
        int thread = 1;
        if (upgradeComponent.isUpgradeInstalled(Upgrade.THREAD)) {
            thread += upgradeComponent.getUpgrades(Upgrade.THREAD);
        }
        return thread;
    }


    public void addInventory(List<ItemStack> stacks) {
        if (stacks.isEmpty()) {
            return;
        }
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) continue;
            ItemStack remaining = stack.copy();
            for (IInventorySlot outputSlot : outputSlots) {
                if (remaining.isEmpty()) {
                    break;
                }
                remaining = outputSlot.insertItem(remaining, Action.EXECUTE, AutomationType.INTERNAL);
            }
            // After attempting all slots, any remaining amount is discarded per requirement.
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
        }
        if (capability == Capabilities.CONFIG_CARD_CAPABILITY) {
            return Capabilities.CONFIG_CARD_CAPABILITY.cast(this);
        }
        return super.getCapability(capability, side);
    }

    @Override
    public boolean isCapabilityDisabled(@Nonnull Capability<?> capability, EnumFacing side) {
        return configComponent.isCapabilityDisabled(capability, side, facing) || super.isCapabilityDisabled(capability, side);
    }

    @Override
    public boolean applyTierUpgrade(BaseTier upgradeTier) {
        if (!tier.canUpgradeTo(upgradeTier)) {
            return false;
        }
        tier = MachineTier.get(upgradeTier);
        Mekanism.packetHandler.sendUpdatePacket(this);
        markNoUpdateSync();
        return true;
    }

    @Override
    public MachineTier getTier() {
        return tier;
    }


    @Override
    public void handlePacketData(ByteBuf dataStream) {
        super.handlePacketData(dataStream);
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            MachineTier prevTier = tier;
            tier = MachineTier.byIndex(dataStream.readInt());
            if (prevTier != tier) {
                MekanismUtils.updateBlock(world, getPos());
            }
        }
    }


    @Override
    public TileNetworkList getNetworkedData(TileNetworkList data) {
        super.getNetworkedData(data);
        data.add(tier.ordinal());
        return data;
    }


    @Override
    public void readCustomNBT(NBTTagCompound nbtTags) {
        super.readCustomNBT(nbtTags);
        tier = MachineTier.byIndex(nbtTags.getInteger("tier"));
    }

    @Override
    public void writeCustomNBT(NBTTagCompound nbtTags) {
        super.writeCustomNBT(nbtTags);
        nbtTags.setInteger("tier", tier.ordinal());
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
    public int getRedstoneLevel() {
        return MekanismUtils.redstoneLevelFromContents(getEnergy(), getMaxEnergy());
    }

    @Override
    public boolean sideIsConsumer(EnumFacing side) {
        return configComponent.hasSideForData(TransmissionType.ENERGY, facing, DataType.INPUT, side);
    }


    @Override
    public boolean canSetFacing(@Nonnull EnumFacing facing) {
        return facing != EnumFacing.DOWN && facing != EnumFacing.UP;
    }


    @Override
    public int getBlockGuiID(Block block, int i) {
        return 20;
    }

    @Override
    public IGuiProvider guiProvider() {
        return MEKCeuMoreMachine.proxy;
    }

    @Nonnull
    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }

    @Nonnull
    @Override
    public String getName() {
        return LangUtils.localize("tile.VoidMineralGenerator." + tier.getBaseTier().getSimpleName() + ".name");
    }

    @Override
    public double getMaxEnergy() {
        return super.getMaxEnergy() * tier.processes;
    }

    @Nonnull
    @Override
    public BlockFaceShape getOffsetBlockFaceShape(@Nonnull EnumFacing face, @Nonnull Vec3i offset) {
        return BlockFaceShape.SOLID;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Class<?> getSelectionWireframeModelClass() {
        return mekceumoremachine.client.model.machine.ModelVoidMineralGenerator.class;
    }

}
