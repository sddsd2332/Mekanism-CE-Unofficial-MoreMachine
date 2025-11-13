package mekceumoremachine.common.tile.generator;

import io.netty.buffer.ByteBuf;
import mekanism.api.EnumColor;
import mekanism.api.TileNetworkList;
import mekanism.api.gas.*;
import mekanism.common.Mekanism;
import mekanism.common.Upgrade;
import mekanism.common.base.IBoundingBlock;
import mekanism.common.base.IComparatorSupport;
import mekanism.common.base.IMachineSlotTip;
import mekanism.common.base.ISustainedData;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.config.MekanismConfig;
import mekanism.common.recipe.GasStackFuelToEnergyRecipe;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.inputs.GasInput;
import mekanism.common.tier.BaseTier;
import mekanism.common.util.*;
import mekanism.generators.common.tile.TileEntityGenerator;
import mekanism.multiblockmachine.common.registries.MultiblockMachineBlocks;
import mekanism.multiblockmachine.common.tile.generator.TileEntityLargeGasGenerator;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.interfaces.ILargeMachine;
import mekceumoremachine.common.tile.interfaces.ITierMachine;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Optional.Interface;
import net.minecraftforge.fml.common.Optional.InterfaceList;

import javax.annotation.Nonnull;

@InterfaceList({
        @Interface(iface = "mekceumoremachine.common.tile.interfaces.ILargeMachine", modid = "mekanismmultiblockmachine"),
})
public class TileEntityTierGasGenerator extends TileEntityGenerator implements IGasHandler, ISustainedData, IComparatorSupport, IMachineSlotTip, ITierMachine<MachineTier>, ILargeMachine {

    private static final String[] methods = new String[]{"getEnergy", "getOutput", "getMaxEnergy", "getEnergyNeeded", "getGas", "getGasNeeded"};
    /**
     * The maximum amount of gas this block can store.
     */
    public static final int MAX_GAS = 18000;
    /**
     * The tank this block is storing fuel in.
     */
    public GasTank fuelTank;
    public int burnTicks = 0;
    public int maxBurnTicks;
    public double generationRate = 0;
    public double clientUsed;
    private int currentRedstoneLevel;
    public GasStackFuelToEnergyRecipe cachedRecipe;

    public MachineTier tier = MachineTier.BASIC;

    public TileEntityTierGasGenerator() {
        super("gas", "TierGasGenerator", 0, 0);
        inventory = NonNullListSynchronized.withSize(2, ItemStack.EMPTY);
        fuelTank = new GasTank(MAX_GAS * tier.processes);
    }

    @Override
    public void onAsyncUpdateServer() {
        super.onAsyncUpdateServer();
        ChargeUtils.charge(1, this);
        if (!inventory.get(0).isEmpty() && fuelTank.getStored() < fuelTank.getMaxGas()) {
            Gas gasType = null;
            if (fuelTank.getGas() != null) {
                gasType = fuelTank.getGas().getGas();
            } else if (!inventory.get(0).isEmpty() && inventory.get(0).getItem() instanceof IGasItem gasItem) {
                if (gasItem.getGas(inventory.get(0)) != null) {
                    gasType = gasItem.getGas(inventory.get(0)).getGas();
                }
            }
            if (gasType != null && RecipeHandler.Recipe.GAS_FUEL_TO_ENERGY_RECIPE.containsRecipe(gasType)) {
                GasStack removed = GasUtils.removeGas(inventory.get(0), gasType, fuelTank.getNeeded());
                fuelTank.receive(removed, true);
            }
        }

        boolean operate = canOperate();
        GasStackFuelToEnergyRecipe recipe = getRecipe();
        if (operate && getEnergy() + generationRate < getMaxEnergy()) {
            setActive(true);
            if (fuelTank.getStored() != 0) {
                maxBurnTicks = recipe.getInput().ingredient.amount;
                generationRate = recipe.getOutput().energyOutput;
            }

            int toUse = getToUse();

            int total = burnTicks + fuelTank.getStored() * maxBurnTicks;
            total -= toUse;
            setEnergy(getEnergy() + generationRate * toUse);

            if (fuelTank.getStored() > 0) {
                fuelTank.setGas(new GasStack(fuelTank.getGasType(), total / maxBurnTicks));
            }
            burnTicks = total % maxBurnTicks;
            clientUsed = toUse / (double) maxBurnTicks;
        } else {
            if (!operate) {
                reset();
            }
            clientUsed = 0;
            setActive(false);
        }
        int newRedstoneLevel = getRedstoneLevel();
        if (newRedstoneLevel != currentRedstoneLevel) {
            updateComparatorOutputLevelSync();
            currentRedstoneLevel = newRedstoneLevel;
        }
    }

    public void reset() {
        burnTicks = 0;
        maxBurnTicks = 0;
        generationRate = 0;
    }

    public int getToUse() {
        if (generationRate == 0 || fuelTank.getGas() == null) {
            return 0;
        }
        int max = (int) Math.ceil(((float) fuelTank.getStored() / (float) fuelTank.getMaxGas()) * 256F);
        max *= tier.processes;
        max = Math.min((fuelTank.getStored() * maxBurnTicks) + burnTicks, max);
        max = (int) Math.min((getMaxEnergy() - getEnergy()) / generationRate, max);
        return max;
    }

    @Override
    public boolean canExtractItem(int slotID, @Nonnull ItemStack itemstack, @Nonnull EnumFacing side) {
        if (slotID == 1) {
            return ChargeUtils.canBeOutputted(itemstack, true);
        } else if (slotID == 0) {
            return itemstack.getItem() instanceof IGasItem gasItem && gasItem.getGas(itemstack) == null;
        }
        return false;
    }

    @Override
    public boolean isItemValidForSlot(int slotID, @Nonnull ItemStack itemstack) {
        if (slotID == 0) {
            return itemstack.getItem() instanceof IGasItem gasItem && gasItem.getGas(itemstack) != null &&
                    RecipeHandler.Recipe.GAS_FUEL_TO_ENERGY_RECIPE.containsRecipe(gasItem.getGas(itemstack).getGas());
        } else if (slotID == 1) {
            return ChargeUtils.canBeCharged(itemstack);
        }
        return true;
    }

    @Nonnull
    @Override
    public int[] getSlotsForFace(@Nonnull EnumFacing side) {
        return side == MekanismUtils.getRight(facing) ? new int[]{1} : new int[]{0};
    }

    @Override
    public boolean canOperate() {
        return (fuelTank.getStored() > 0 || burnTicks > 0) && MekanismUtils.canFunction(this);
    }

    @Override
    public String[] getMethods() {
        return methods;
    }

    @Override
    public Object[] invoke(int method, Object[] arguments) throws NoSuchMethodException {
        return switch (method) {
            case 0 -> new Object[]{getEnergy()};
            case 1 -> new Object[]{getMaxOutput()};
            case 2 -> new Object[]{getMaxEnergy()};
            case 3 -> new Object[]{getNeedEnergy()};
            case 4 -> new Object[]{fuelTank.getStored()};
            case 5 -> new Object[]{fuelTank.getNeeded()};
            default -> throw new NoSuchMethodException();
        };
    }

    @Override
    public void handlePacketData(ByteBuf dataStream) {
        super.handlePacketData(dataStream);
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            MachineTier prevTier = tier;
            tier = MachineTier.values()[dataStream.readInt()];
            fuelTank.setMaxGas(tier.processes * MAX_GAS);

            generationRate = dataStream.readDouble();
            clientUsed = dataStream.readDouble();
            TileUtils.readTankData(dataStream, fuelTank);
            maxBurnTicks = dataStream.readInt();
            if (prevTier != tier) {
                MekanismUtils.updateBlock(world, getPos());
            }
        }
    }

    @Override
    public TileNetworkList getNetworkedData(TileNetworkList data) {
        super.getNetworkedData(data);
        data.add(tier.ordinal());
        data.add(generationRate);
        data.add(clientUsed);
        TileUtils.addTankData(data, fuelTank);
        data.add(maxBurnTicks);
        return data;
    }

    @Override
    public int receiveGas(EnumFacing side, GasStack stack, boolean doTransfer) {
        boolean isTankEmpty = fuelTank.getGas() == null;
        if (canReceiveGas(side, stack.getGas()) && (isTankEmpty || fuelTank.getGas().isGasEqual(stack))) {
            return fuelTank.receive(stack, doTransfer);
        }
        return 0;
    }


    @Nonnull
    @Override
    public GasTankInfo[] getTankInfo() {
        return new GasTankInfo[]{fuelTank};
    }


    @Override
    public void readCustomNBT(NBTTagCompound nbtTags) {
        super.readCustomNBT(nbtTags);
        tier = MachineTier.values()[nbtTags.getInteger("tier")];
        fuelTank.read(nbtTags.getCompoundTag("fuelTank"));
    }

    @Override
    public void writeCustomNBT(NBTTagCompound nbtTags) {
        super.writeCustomNBT(nbtTags);
        nbtTags.setInteger("tier", tier.ordinal());
        nbtTags.setTag("fuelTank", fuelTank.write(new NBTTagCompound()));
    }

    @Override
    public boolean canReceiveGas(EnumFacing side, Gas type) {
        return RecipeHandler.Recipe.GAS_FUEL_TO_ENERGY_RECIPE.containsRecipe(type) && side != facing;
    }

    @Override
    public GasStack drawGas(EnumFacing side, int amount, boolean doTransfer) {
        return null;
    }

    @Override
    public boolean canDrawGas(EnumFacing side, Gas type) {
        return false;
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, EnumFacing side) {
        if (isCapabilityDisabled(capability, side)) {
            return false;
        }
        return capability == Capabilities.GAS_HANDLER_CAPABILITY || super.hasCapability(capability, side);
    }

    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, EnumFacing side) {
        if (isCapabilityDisabled(capability, side)) {
            return null;
        } else if (capability == Capabilities.GAS_HANDLER_CAPABILITY) {
            return Capabilities.GAS_HANDLER_CAPABILITY.cast(this);
        }
        return super.getCapability(capability, side);
    }

    @Override
    public boolean isCapabilityDisabled(@Nonnull Capability<?> capability, EnumFacing side) {
        if (capability == Capabilities.GAS_HANDLER_CAPABILITY) {
            return side == facing;
        }
        return super.isCapabilityDisabled(capability, side);
    }

    @Override
    public void writeSustainedData(ItemStack itemStack) {
        if (fuelTank != null) {
            ItemDataUtils.setCompound(itemStack, "fuelTank", fuelTank.write(new NBTTagCompound()));
        }
    }

    @Override
    public void readSustainedData(ItemStack itemStack) {
        if (ItemDataUtils.hasData(itemStack, "fuelTank")) {
            fuelTank.read(ItemDataUtils.getCompound(itemStack, "fuelTank"));
        }
    }

    @Override
    public int getRedstoneLevel() {
        return MekanismUtils.redstoneLevelFromContents(fuelTank.getStored(), fuelTank.getMaxGas());
    }

    @Override
    public boolean getEnergySlot() {
        return inventory.get(1).isEmpty();
    }

    @Override
    public boolean getInputSlot() {
        return false;
    }

    @Override
    public boolean getOuputSlot() {
        return false;
    }

    public GasStackFuelToEnergyRecipe getRecipe() {
        GasInput input = getInput();
        if (cachedRecipe == null || !input.testEquality(cachedRecipe.getInput())) {
            cachedRecipe = RecipeHandler.getGasStackFuelToEnergyRecipe(getInput());
        }
        return cachedRecipe;
    }

    public GasInput getInput() {
        return new GasInput(fuelTank.getGas());
    }


    public double getUsed() {
        return Math.round(clientUsed * 100) / 100D;
    }

    public int getMaxBurnTicks() {
        return maxBurnTicks;
    }

    @Override
    public MachineTier getTier() {
        return tier;
    }

    @Override
    public boolean upgrade(BaseTier upgradeTier) {
        if (upgradeTier.ordinal() != tier.ordinal() + 1) {
            return false;
        }
        if (upgradeTier == BaseTier.CREATIVE){
            return false;
        }
        tier = MachineTier.values()[upgradeTier.ordinal()];
        fuelTank.setMaxGas(tier.processes * MAX_GAS);
        Mekanism.packetHandler.sendUpdatePacket(this);
        markNoUpdateSync();
        return true;
    }

    @Nonnull
    @Override
    public String getName() {
        return LangUtils.localize("tile.TierGasGenerator." + tier.getBaseTier().getSimpleName() + ".name");
    }

    @Override
    public double getMaxEnergy() {
        return MekanismConfig.current().general.FROM_H2.val() * 1000 * tier.processes;
    }

    @Override
    public double getMaxOutput() {
        return MekanismConfig.current().general.FROM_H2.val() * 1000 * tier.processes * 2;
    }

    public boolean isUpgrade = true;

    @Override
    public boolean largeMachineUpgrade(EntityPlayer player) {
        if (tier != MachineTier.ULTIMATE) {
            return false;
        }
        BlockPos.MutableBlockPos testPos = new BlockPos.MutableBlockPos();
        boolean isCanPlace = false;
        outer:
        for (int yPos = 0; yPos <= 2; yPos++) {
            for (int xPos = -1; xPos <= 1; xPos++) {
                for (int zPos = -1; zPos <= 1; zPos++) {
                    if (yPos == 0 && xPos == 0 && zPos == 0) {
                        continue;
                    }
                    testPos.setPos(pos.getX() + xPos, pos.getY() + yPos, pos.getZ() + zPos);
                    Block b = world.getBlockState(testPos).getBlock();
                    if (!world.isValid(testPos) || !world.isBlockLoaded(testPos, false) || !b.isReplaceable(world, testPos)) {
                        isCanPlace = true;
                        player.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + Mekanism.LOG_TAG + EnumColor.GREY + " " + LangUtils.localize("tooltip.canPlace.pos") + ": " + "X " + testPos.getX() + " " + "Y " + testPos.getY() + " " + "Z " + testPos.getZ()));
                        break outer;
                    }
                }
            }
        }
        if (isCanPlace) {
            return false;
        }
        isUpgrade = false;
        if (world.getTileEntity(getPos()) instanceof IBoundingBlock block) {
            block.onBreak();
        } else {
            world.setBlockToAir(getPos());
        }

        world.setBlockState(getPos(), MultiblockMachineBlocks.LargeGasGenerator.getDefaultState(), 3);
        if (world.getTileEntity(getPos()) instanceof TileEntityLargeGasGenerator tile) {
            tile.onPlace();
            //Basic
            tile.facing = facing;
            tile.clientFacing = clientFacing;
            tile.ticker = ticker;
            tile.redstone = redstone;
            tile.redstoneLastTick = redstoneLastTick;
            tile.doAutoSync = doAutoSync;

            //Electric
            tile.electricityStored.set(electricityStored.get());
            tile.clientUsed = clientUsed;
            tile.maxBurnTicks = maxBurnTicks;
            tile.generationRate = generationRate;

            //Machine
            tile.setActive(isActive);
            tile.setControlType(getControlType());
            tile.upgradeComponent.setUpgradeSlot(2);
            tile.upgradeComponent.setSupported(Upgrade.ENERGY);
            tile.upgradeComponent.setSupported(Upgrade.THREAD);//升级完后需要添加支持线程升级
            tile.securityComponent.readFrom(securityComponent);
            for (int i = 0; i < inventory.size(); i++) {
                tile.inventory.set(i, inventory.get(i));
            }
            tile.fuelTank.setGas(fuelTank.getGas());
            tile.upgradeComponent.getSupportedTypes().forEach(tile::recalculateUpgradables);
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


}
