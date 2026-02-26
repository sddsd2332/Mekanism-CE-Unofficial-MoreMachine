package mekceumoremachine.common.tile.machine;

import io.netty.buffer.ByteBuf;
import mekanism.api.EnumColor;
import mekanism.api.TileNetworkList;
import mekanism.api.gas.*;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.Mekanism;
import mekanism.common.SideData;
import mekanism.common.Upgrade;
import mekanism.common.base.*;
import mekanism.common.block.states.BlockStateMachine.MachineType;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.config.MekanismConfig;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.inputs.GasAndFluidInput;
import mekanism.common.recipe.machines.WasherRecipe;
import mekanism.common.recipe.outputs.GasOutput;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.prefab.TileEntityBasicMachine;
import mekanism.common.util.*;
import mekanism.multiblockmachine.common.registries.MultiblockMachineBlocks;
import mekanism.multiblockmachine.common.tile.machine.TileEntityLargeChemicalWasher;
import mekceumoremachine.common.MEKCeuMoreMachine;
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
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Optional.Interface;
import net.minecraftforge.fml.common.Optional.InterfaceList;
import net.minecraftforge.fml.common.Optional.Method;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

@InterfaceList({
        @Interface(iface = "mekceumoremachine.common.tile.interfaces.ILargeMachine", modid = "mekanismmultiblockmachine"),
})
public class TileEntityTierChemicalWasher extends TileEntityBasicMachine<GasAndFluidInput, GasOutput, WasherRecipe> implements IGasHandler, IFluidHandlerWrapper, ISustainedData, Upgrade.IUpgradeInfoHandler, ITankManager, ITierMachine<MachineTier>, ILargeMachine {


    public static final int MAX_GAS = 10000;
    public static final int MAX_FLUID = 10000;
    //TODO
    public static int WATER_USAGE = 5;

    public FluidTank fluidTank;
    public GasTank inputTank;
    public GasTank outputTank;

    public WasherRecipe cachedRecipe;
    public double clientEnergyUsed;
    private int currentRedstoneLevel;
    public MachineTier tier = MachineTier.BASIC;

    public TileEntityTierChemicalWasher() {
        super("washer", "TierChemicalWasher", 0, MachineType.CHEMICAL_WASHER.getUsage(), 4, 1);
        fluidTank = new FluidTankSync(MAX_FLUID * tier.processes);
        inputTank = new GasTank(MAX_GAS * tier.processes);
        outputTank = new GasTank(MAX_GAS * tier.processes);

        configComponent = new TileComponentConfig(this, TransmissionType.ITEM, TransmissionType.ENERGY, TransmissionType.FLUID, TransmissionType.GAS);

        configComponent.addOutput(TransmissionType.ITEM, new SideData(DataType.NONE, InventoryUtils.EMPTY));
        configComponent.addOutput(TransmissionType.ITEM, new SideData(DataType.INPUT, new int[]{0}));
        configComponent.addOutput(TransmissionType.ITEM, new SideData(DataType.OUTPUT, new int[]{1}));
        configComponent.addOutput(TransmissionType.ITEM, new SideData(DataType.GAS, new int[]{2}));
        configComponent.addOutput(TransmissionType.ITEM, new SideData(DataType.ENERGY, new int[]{3}));
        configComponent.setConfig(TransmissionType.ITEM, new byte[]{1, 1, 1, 4, 1, 2});
        configComponent.setCanEject(TransmissionType.ITEM, false);

        configComponent.setInputConfig(TransmissionType.FLUID);

        configComponent.addOutput(TransmissionType.GAS, new SideData(DataType.NONE, InventoryUtils.EMPTY));
        configComponent.addOutput(TransmissionType.GAS, new SideData(DataType.INPUT, new int[]{1}));
        configComponent.addOutput(TransmissionType.GAS, new SideData(DataType.OUTPUT, new int[]{2}));
        configComponent.addOutput(TransmissionType.GAS, new SideData(new int[]{1, 2}, new boolean[]{false, true}));
        configComponent.setConfig(TransmissionType.GAS, new byte[]{1, 1, 1, 1, 1, 2});

        configComponent.setInputConfig(TransmissionType.ENERGY);

        inventory = NonNullListSynchronized.withSize(5, ItemStack.EMPTY);

        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(TransmissionType.GAS, configComponent.getOutputs(TransmissionType.GAS).get(2));
        ejectorComponent.setInputOutputData(TransmissionType.GAS, configComponent.getOutputs(TransmissionType.GAS).get(3));
    }


    @Override
    public void onAsyncUpdateServer() {
        super.onAsyncUpdateServer();
        ChargeUtils.discharge(3, this);
        manageBuckets();
        TileUtils.drawGas(inventory.get(2), outputTank);
        WasherRecipe recipe = getRecipe();
        getProcess(recipe, true, energyPerTick, true, false);
        prevEnergy = getEnergy();
        int newRedstoneLevel = getRedstoneLevel();
        if (newRedstoneLevel != currentRedstoneLevel) {
            updateComparatorOutputLevelSync();
            currentRedstoneLevel = newRedstoneLevel;
        }
    }

    @Override
    protected void setUpOtherActions() {
        double prev = getEnergy();
        setEnergy(getEnergy() - energyPerTick * getUpgradedUsage());
        clientEnergyUsed = prev - getEnergy();
    }

    @Override
    public WasherRecipe getRecipe() {
        GasAndFluidInput input = getInput();
        if (cachedRecipe == null || !input.testEquality(cachedRecipe.getInput())) {
            cachedRecipe = RecipeHandler.getChemicalWasherRecipe(getInput());
        }
        return cachedRecipe;
    }

    @Override
    public GasAndFluidInput getInput() {
        return new GasAndFluidInput(inputTank.getGas(), fluidTank.getFluid());
    }

    @Override
    public boolean canOperate(WasherRecipe recipe) {
        return recipe != null && recipe.canOperate(inputTank, fluidTank, outputTank);
    }

    @Override
    public void operate(WasherRecipe recipe) {
        recipe.operate(inputTank, fluidTank, outputTank, getUpgradedUsage());
    }

    @Override
    public Map<GasAndFluidInput, WasherRecipe> getRecipes() {
        return RecipeHandler.Recipe.CHEMICAL_WASHER.get();
    }

    private void manageBuckets() {
        if (FluidContainerUtils.isFluidContainer(inventory.get(0)) && fluidTank.getFluidAmount() != fluidTank.getCapacity()) {
            FluidContainerUtils.handleContainerItemEmpty(this, fluidTank, 0, 1, FluidContainerUtils.FluidChecker.check(FluidRegistry.WATER));
        }
    }

    public int getUpgradedUsage() {
        int possibleProcess = Math.min((int) Math.pow(2, upgradeComponent.getUpgrades(Upgrade.SPEED)), MekanismConfig.current().mekce.MAXspeedmachines.val());
        possibleProcess *= tier.processes;
        possibleProcess = Math.min(Math.min(inputTank.getStored(), outputTank.getNeeded()), possibleProcess);
        possibleProcess = Math.min((int) (getEnergy() / energyPerTick), possibleProcess);
        possibleProcess = Math.max(possibleProcess, 1);
        return Math.min(fluidTank.getFluidAmount() / WATER_USAGE, possibleProcess);
    }

    @Override
    public void handlePacketData(ByteBuf dataStream) {
        super.handlePacketData(dataStream);
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            MachineTier prevTier = tier;
            tier = MachineTier.values()[dataStream.readInt()];
            fluidTank.setCapacity(tier.processes * MAX_FLUID);
            inputTank.setMaxGas(tier.processes * MAX_GAS);
            outputTank.setMaxGas(tier.processes * MAX_GAS);
            clientEnergyUsed = dataStream.readDouble();
            TileUtils.readTankData(dataStream, fluidTank);
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
        data.add(clientEnergyUsed);
        TileUtils.addTankData(data, fluidTank);
        TileUtils.addTankData(data, inputTank);
        TileUtils.addTankData(data, outputTank);
        return data;
    }

    @Override
    public void readCustomNBT(NBTTagCompound nbtTags) {
        super.readCustomNBT(nbtTags);
        tier = MachineTier.values()[nbtTags.getInteger("tier")];
        fluidTank.readFromNBT(nbtTags.getCompoundTag("leftTank"));
        inputTank.read(nbtTags.getCompoundTag("rightTank"));
        outputTank.read(nbtTags.getCompoundTag("centerTank"));
    }

    @Override
    public void writeCustomNBT(NBTTagCompound nbtTags) {
        super.writeCustomNBT(nbtTags);
        nbtTags.setInteger("tier", tier.ordinal());
        nbtTags.setTag("leftTank", fluidTank.writeToNBT(new NBTTagCompound()));
        nbtTags.setTag("rightTank", inputTank.write(new NBTTagCompound()));
        nbtTags.setTag("centerTank", outputTank.write(new NBTTagCompound()));
    }

    @Override
    public boolean canReceiveGas(EnumFacing side, Gas type) {
        return configComponent.getOutput(TransmissionType.GAS, side, facing).hasSlot(1) && inputTank.canReceive(type) && RecipeHandler.Recipe.CHEMICAL_WASHER.containsRecipe(type);
    }


    @Override
    public int receiveGas(EnumFacing side, GasStack stack, boolean doTransfer) {
        if (canReceiveGas(side, stack.getGas())) {
            return inputTank.receive(stack, doTransfer);
        }
        return 0;
    }

    @Override
    public GasStack drawGas(EnumFacing side, int amount, boolean doTransfer) {
        if (canDrawGas(side, null)) {
            return outputTank.draw(amount, doTransfer);
        }
        return null;
    }

    @Override
    public boolean canDrawGas(EnumFacing side, Gas type) {
        return configComponent.getOutput(TransmissionType.GAS, side, facing).hasSlot(2) && outputTank.canDraw(type);
    }

    @Nonnull
    @Override
    public GasTankInfo[] getTankInfo() {
        return new GasTankInfo[]{inputTank, outputTank};
    }

    @Override
    public boolean isItemValidForSlot(int slotID, @Nonnull ItemStack itemstack) {
        if (slotID == 0) {
            return FluidUtil.getFluidContained(itemstack) != null && FluidUtil.getFluidContained(itemstack).getFluid() == FluidRegistry.WATER;
        } else if (slotID == 2) {
            return ChargeUtils.canBeDischarged(itemstack);
        }
        return false;
    }

    @Override
    public boolean canExtractItem(int slotID, @Nonnull ItemStack itemstack, @Nonnull EnumFacing side) {
        if (slotID == 1) {
            return !itemstack.isEmpty() && itemstack.getItem() instanceof IGasItem gasItem && gasItem.canProvideGas(itemstack, null);
        } else if (slotID == 2) {
            return ChargeUtils.canBeOutputted(itemstack, false);
        }
        return false;
    }


    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, EnumFacing side) {
        if (isCapabilityDisabled(capability, side)) {
            return false;
        }
        return capability == Capabilities.GAS_HANDLER_CAPABILITY || capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, side);
    }

    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, EnumFacing side) {
        if (isCapabilityDisabled(capability, side)) {
            return null;
        } else if (capability == Capabilities.GAS_HANDLER_CAPABILITY) {
            return Capabilities.GAS_HANDLER_CAPABILITY.cast(this);
        } else if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(new FluidHandlerWrapper(this, side));
        }
        return super.getCapability(capability, side);
    }


    @Override
    public int fill(EnumFacing from, @Nonnull FluidStack resource, boolean doFill) {
        return fluidTank.fill(resource, doFill);
    }

    @Override
    public boolean canFill(EnumFacing from, @Nonnull FluidStack fluid) {
        if (configComponent.getOutput(TransmissionType.FLUID, from, facing).ioState == SideData.IOState.INPUT) {
            return FluidContainerUtils.canFill(fluidTank.getFluid(), fluid) && fluid.getFluid().equals(FluidRegistry.WATER);
        }
        return false;
    }

    @Override
    public FluidTankInfo[] getTankInfo(EnumFacing from) {
        if (configComponent.getOutput(TransmissionType.FLUID, from, facing).ioState != SideData.IOState.OFF) {
            return new FluidTankInfo[]{fluidTank.getInfo()};
        }
        return PipeUtils.EMPTY;
    }

    @Override
    public FluidTankInfo[] getAllTanks() {
        return new FluidTankInfo[]{fluidTank.getInfo()};
    }

    @Override
    public void writeSustainedData(ItemStack itemStack) {
        if (fluidTank.getFluid() != null) {
            ItemDataUtils.setCompound(itemStack, "fluidTank", fluidTank.getFluid().writeToNBT(new NBTTagCompound()));
        }
        if (inputTank.getGas() != null) {
            ItemDataUtils.setCompound(itemStack, "inputTank", inputTank.getGas().write(new NBTTagCompound()));
        }
        if (outputTank.getGas() != null) {
            ItemDataUtils.setCompound(itemStack, "outputTank", outputTank.getGas().write(new NBTTagCompound()));
        }
    }

    @Override
    public void readSustainedData(ItemStack itemStack) {
        fluidTank.setFluid(FluidStack.loadFluidStackFromNBT(ItemDataUtils.getCompound(itemStack, "fluidTank")));
        inputTank.setGas(GasStack.readFromNBT(ItemDataUtils.getCompound(itemStack, "inputTank")));
        outputTank.setGas(GasStack.readFromNBT(ItemDataUtils.getCompound(itemStack, "outputTank")));
    }

    @Override
    public List<String> getInfo(Upgrade upgrade) {
        return upgrade == Upgrade.SPEED ? upgrade.getExpScaledInfo(this) : upgrade.getMultScaledInfo(this);
    }

    @Override
    public Object[] getTanks() {
        return new Object[]{fluidTank, inputTank, outputTank};
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
    public boolean getEnergySlot() {
        return inventory.get(3).isEmpty();
    }

    @Override
    public boolean getInputSlot() {
        return false;
    }

    @Override
    public boolean getOuputSlot() {
        return false;
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
        fluidTank.setCapacity(tier.processes * MAX_FLUID);
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
        return LangUtils.localize("tile.TierChemicalWasher." + tier.getBaseTier().getSimpleName() + ".name");
    }

    @Override
    public int getBlockGuiID(Block block, int metadata) {
        return 8;
    }

    @Override
    public IGuiProvider guiProvider() {
        return MEKCeuMoreMachine.proxy;
    }


    @Override
    public double getMaxEnergy() {
        return upgradeComponent.isUpgradeInstalled(Upgrade.ENERGY) ? MekanismUtils.getMaxEnergy(this, getTierEnergy()) : getTierEnergy();
    }

    public double getTierEnergy() {
        return MachineType.CHEMICAL_WASHER.getStorage() * tier.processes;
    }

    public boolean isUpgrade = true;

    @Override
    @Method(modid = "mekanismmultiblockmachine")
    public boolean largeMachineUpgrade(EntityPlayer player) {
        if (tier != MachineTier.ULTIMATE) {
            return false;
        }
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
        isUpgrade = false;
        if (world.getTileEntity(getPos()) instanceof IBoundingBlock block) {
            block.onBreak();
        } else {
            world.setBlockToAir(getPos());
        }

        world.setBlockState(getPos(), MultiblockMachineBlocks.LargeChemicalWasher.getDefaultState(), 3);
        if (world.getTileEntity(getPos()) instanceof TileEntityLargeChemicalWasher tile) {
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
            tile.clientEnergyUsed = clientEnergyUsed;


            //Machine
            tile.setActive(isActive);
            tile.setControlType(getControlType());
            tile.prevEnergy = prevEnergy;
            tile.upgradeComponent.readFrom(upgradeComponent);
            tile.upgradeComponent.setUpgradeSlot(upgradeComponent.getUpgradeSlot());
            tile.upgradeComponent.setSupported(Upgrade.THREAD);//升级完后需要添加支持线程升级
            tile.securityComponent.readFrom(securityComponent);
            for (int i = 0; i < inventory.size(); i++) {
                tile.inventory.set(i, inventory.get(i));
            }
            tile.inputTank.setGas(inputTank.getGas());
            tile.outputTank.setGas(outputTank.getGas());
            tile.fluidTank.setFluid(fluidTank.getFluid());
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
        return isUpgrade;
    }
}
