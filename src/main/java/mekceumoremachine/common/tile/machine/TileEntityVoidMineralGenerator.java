package mekceumoremachine.common.tile.machine;

import io.netty.buffer.ByteBuf;
import mekanism.api.IConfigCardAccess;
import mekanism.api.TileNetworkList;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.Mekanism;
import mekanism.common.SideData;
import mekanism.common.Upgrade;
import mekanism.common.base.IGuiProvider;
import mekanism.common.base.ISideConfiguration;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.config.MekanismConfig;
import mekanism.common.item.ItemUpgrade;
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
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class TileEntityVoidMineralGenerator extends TileEntityOperationalMachine implements ISideConfiguration, IConfigCardAccess, ITierMachine<MachineTier> {

    public TileComponentEjector ejectorComponent;
    public TileComponentConfig configComponent;

    public int delayTicks;

    private static final int[] INV_SLOTS = IntStream.range(2, 83).toArray();

    public MachineTier tier = MachineTier.BASIC;
    private int currentRedstoneLevel;

    public TileEntityVoidMineralGenerator() {
        super("machine.smelter", "VoidMineralGenerator", MoreMachineConfig.current().config.VoidMineralGeneratorEnergyStorge.val(), MoreMachineConfig.current().config.VoidMineralGeneratorEnergyUsage.val(), 0, MoreMachineConfig.current().config.VoidMineralGeneratorTick.val());
        configComponent = new TileComponentConfig(this, TransmissionType.ITEM, TransmissionType.ENERGY);
        upgradeComponent.setSupported(Upgrade.THREAD);
        configComponent.addOutput(TransmissionType.ITEM, new SideData(DataType.NONE, InventoryUtils.EMPTY));
        configComponent.addOutput(TransmissionType.ITEM, new SideData(DataType.OUTPUT, INV_SLOTS));
        configComponent.addOutput(TransmissionType.ITEM, new SideData(DataType.OUTPUT_ENHANCED, INV_SLOTS));
        configComponent.setConfig(TransmissionType.ITEM, new byte[]{1, -1, 1, 1, 1, 1});
        configComponent.setIOConfig(TransmissionType.ENERGY);

        inventory = NonNullListSynchronized.withSize(83, ItemStack.EMPTY);

        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(TransmissionType.ITEM, configComponent.getOutputs(TransmissionType.ITEM).get(1));
    }

    @Override
    public void onAsyncUpdateServer() {
        super.onAsyncUpdateServer();
        ChargeUtils.discharge(1, this);
        //判断所有INV_SLOTS插槽是否都满了，如果满了就不继续生成了
        boolean hasInventory = IntStream.of(INV_SLOTS).allMatch(i -> {
            ItemStack s = inventory.get(i);
            return !s.isEmpty() && s.getCount() >= Math.min(s.getMaxStackSize(), getInventoryStackLimit());
        });
        List<ItemStack> available = VoidMineralGeneratorUitls.getCanOre();
        if (!hasInventory && MekanismUtils.canFunction(this) && getEnergy() >= energyPerTick && !available.isEmpty()) {
            setActive(true);
            operatingTicks++;
            electricityStored.addAndGet(-energyPerTick);
            if (operatingTicks >= ticksRequired) {
                //生成物品到库存
                GenerateItem(available);
                operatingTicks = 0;
            }
        } else if (prevEnergy >= getEnergy()) {
            setActive(false);
        }
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
            // Try to insert the entire incoming stack across multiple slots.
            int remaining = stack.getCount();
            // Single-slot limit is the minimum of the item's max stack size and the container's inventory stack limit
            int slotLimit = Math.min(stack.getMaxStackSize(), getInventoryStackLimit());
            // Iterate through all internal slots and try to place as much as possible.
            for (int i : INV_SLOTS) {
                if (remaining <= 0) break;
                ItemStack currentStack = inventory.get(i);
                if (currentStack.isEmpty()) {
                    // Place up to slotLimit into this empty slot
                    int toPlace = Math.min(remaining, slotLimit);
                    inventory.set(i, StackUtils.size(stack, toPlace));
                    remaining -= toPlace;
                    // try to place any remaining into later slots (loop will continue naturally)
                } else if (ItemHandlerHelper.canItemStacksStack(currentStack, stack)) {
                    int space = slotLimit - currentStack.getCount();
                    if (space <= 0) continue;
                    int toMove = Math.min(space, remaining);
                    currentStack.grow(toMove);
                    remaining -= toMove;
                }
            }
            // After attempting all slots, any remaining amount is discarded per requirement.
        }
    }


    @Override
    public void addTileSyncTask() {
        BetterEjectingItem();
    }

    private void BetterEjectingItem() {
        if (delayTicks == 0 || MekanismConfig.current().mekce.ItemsEjectWithoutDelay.val()) {
            outputItems(2);
            if (!MekanismConfig.current().mekce.ItemsEjectWithoutDelay.val()) {
                delayTicks = MekanismConfig.current().mekce.ItemEjectionDelay.val();
            }
        } else {
            delayTicks--;
        }
    }


    private void outputItems(int dataIndex) {
        if (!configComponent.isEjecting(TransmissionType.ITEM)) {
            return;
        }
        for (EnumFacing facing : configComponent.getSidesForData(TransmissionType.ITEM, facing, dataIndex)) {
            BlockPos offset = getPos().offset(facing);
            TileEntity te = getWorld().getTileEntity(offset);
            if (!InventoryUtils.isItemHandler(te, facing.getOpposite())) {
                continue;
            }
            IItemHandler itemHandler = InventoryUtils.getItemHandler(te, facing.getOpposite());
            if (itemHandler == null) {
                continue;
            }
            try {
                outputToExternal(itemHandler);
            } catch (Exception e) {
                Mekanism.logger.error("Exception when insert item: ", e);
            }
        }
    }


    private synchronized void outputToExternal(IItemHandler external) {
        for (int externalSlotId = 0; externalSlotId < external.getSlots(); externalSlotId++) {
            ItemStack externalStack = external.getStackInSlot(externalSlotId);
            int slotLimit = external.getSlotLimit(externalSlotId);
            if (!externalStack.isEmpty() && externalStack.getCount() >= slotLimit) {
                continue;
            }
            for (int internalSlotId : INV_SLOTS) {
                ItemStack internalStack = inventory.get(internalSlotId);
                if (internalStack.isEmpty()) {
                    continue;
                }
                if (externalStack.isEmpty()) {
                    ItemStack notInserted = external.insertItem(externalSlotId, internalStack, false);
                    // Safeguard against Storage Drawers virtual slot
                    if (notInserted.getCount() == internalStack.getCount()) {
                        break;
                    }
                    inventory.set(internalSlotId, notInserted);
                    if (notInserted.isEmpty()) {
                        break;
                    }
                    continue;
                }
                if (!matchStacks(internalStack, externalStack)) {
                    continue;
                }
                // Extract internal item to external.
                ItemStack notInserted = external.insertItem(externalSlotId, internalStack, false);
                inventory.set(internalSlotId, notInserted);
                if (notInserted.isEmpty()) {
                    break;
                }
            }
        }

    }

    public static boolean matchStacks(@Nonnull ItemStack stack, @Nonnull ItemStack other) {
        if (!ItemStack.areItemsEqual(stack, other)) return false;
        return ItemStack.areItemStackTagsEqual(stack, other);
    }


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

    /*
    @Override
    public void onPlace() {
        Coord4D current = Coord4D.get(this);
        MekanismUtils.makeBoundingBlock(world, getPos().up(), current);
    }

    @Override
    public void onBreak() {
        world.setBlockToAir(getPos().up());
        world.setBlockToAir(getPos());
    }

     */

    @Override
    public boolean isItemValidForSlot(int slotID, @Nonnull ItemStack stack) {
        if (slotID == 0) {
            return stack.getItem() instanceof ItemUpgrade;
        } else if (slotID == 1) {
            return ChargeUtils.canBeDischarged(stack);
        }
        return false;
    }

    @Override
    public boolean canInsertItem(int slotID, @Nonnull ItemStack itemstack, @Nonnull EnumFacing side) {
        if (slotID == 1) {
            return ChargeUtils.canBeDischarged(itemstack);
        }
        return false;
    }

    @Override
    public boolean canExtractItem(int slotID, @Nonnull ItemStack itemstack, @Nonnull EnumFacing side) {
        if (slotID == 1) {
            return !ChargeUtils.canBeDischarged(itemstack);
        }
        return slotID >= 2 && slotID < 84;
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
    public boolean upgrade(BaseTier upgradeTier) {
        if (upgradeTier.ordinal() != tier.ordinal() + 1) {
            return false;
        }
        if (upgradeTier == BaseTier.CREATIVE) {
            return false;
        }
        tier = MachineTier.values()[upgradeTier.ordinal()];
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
            tier = MachineTier.values()[dataStream.readInt()];
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
        tier = MachineTier.values()[nbtTags.getInteger("tier")];
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
        return configComponent.hasSideForData(TransmissionType.ENERGY, facing, 1, side);
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


}
