package mekceumoremachine.common.tile.machine;

import io.netty.buffer.ByteBuf;
import mekanism.api.EnumColor;
import mekanism.api.IConfigCardAccess;
import mekanism.api.TileNetworkList;
import mekanism.api.gas.*;
import mekanism.api.math.MathUtils;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.Mekanism;
import mekanism.common.MekanismFluids;
import mekanism.common.SideData;
import mekanism.common.Upgrade;
import mekanism.common.Upgrade.IUpgradeInfoHandler;
import mekanism.common.base.*;
import mekanism.common.block.states.BlockStateMachine.MachineType;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.config.MekanismConfig;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.RecipeHandler.Recipe;
import mekanism.common.recipe.inputs.FluidInput;
import mekanism.common.recipe.machines.SeparatorRecipe;
import mekanism.common.recipe.outputs.ChemicalPairOutput;
import mekanism.common.tier.BaseTier;
import mekanism.common.tier.GasTankTier;
import mekanism.common.tile.TileEntityGasTank.GasMode;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.prefab.TileEntityBasicMachine;
import mekanism.common.util.*;
import mekanism.multiblockmachine.common.registries.MultiblockMachineBlocks;
import mekanism.multiblockmachine.common.tile.machine.TileEntityLargeElectrolyticSeparator;
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
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
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
        implements IFluidHandlerWrapper, ISustainedData, IGasHandler, IUpgradeInfoHandler, ITankManager, IConfigCardAccess.ISpecialConfigData, ITierMachine<MachineTier>, ILargeMachine {


    private static final String[] methods = new String[]{"getEnergy", "getOutput", "getMaxEnergy", "getEnergyNeeded", "getWater", "getWaterNeeded", "getHydrogen", "getHydrogenNeeded", "getOxygen", "getOxygenNeeded"};
    private final EjectSpeedController gasSpeedController = new EjectSpeedController();


    public FluidTank fluidTank;

    public static int MAX_GAS = 2400;

    public GasTank leftTank;

    public GasTank rightTank;

    public MachineTier tier = MachineTier.BASIC;

    public GasMode dumpLeft = GasMode.IDLE;
    public GasMode dumpRight = GasMode.IDLE;
    public SeparatorRecipe cachedRecipe;
    public double clientEnergyUsed;
    private int currentRedstoneLevel;

    public TileEntityTierElectrolyticSeparator() {
        super("electrolyticseparator", "TierElectrolyticSeparator", 0, MachineType.ELECTROLYTIC_SEPARATOR.getUsage(), 4, 1);
        fluidTank = new FluidTankSync(MAX_GAS * 10 * tier.processes);
        leftTank = new GasTank(MAX_GAS * tier.processes);
        rightTank = new GasTank(MAX_GAS * tier.processes);
        configComponent = new TileComponentConfig(this, TransmissionType.ITEM, TransmissionType.ENERGY, TransmissionType.GAS, TransmissionType.FLUID);
        configComponent.addOutput(TransmissionType.ITEM, new SideData(DataType.NONE, InventoryUtils.EMPTY));
        configComponent.addOutput(TransmissionType.ITEM, new SideData(DataType.INPUT, new int[]{0}));
        configComponent.addOutput(TransmissionType.ITEM, new SideData(DataType.OUTPUT_1, new int[]{1}));
        configComponent.addOutput(TransmissionType.ITEM, new SideData(DataType.OUTPUT_2, new int[]{2}));
        configComponent.addOutput(TransmissionType.ITEM, new SideData(DataType.ENERGY, new int[]{3}));
        configComponent.setConfig(TransmissionType.ITEM, new byte[]{0, 0, 1, 4, 2, 3});
        configComponent.setCanEject(TransmissionType.ITEM, false);

        configComponent.setInputConfig(TransmissionType.FLUID);

        configComponent.addOutput(TransmissionType.GAS, new SideData(DataType.NONE, InventoryUtils.EMPTY));
        configComponent.addOutput(TransmissionType.GAS, new SideData(DataType.OUTPUT_1, new int[]{1}));
        configComponent.addOutput(TransmissionType.GAS, new SideData(DataType.OUTPUT_2, new int[]{2}));
        configComponent.setConfig(TransmissionType.GAS, new byte[]{0, 0, 0, 0, 1, 2});

        configComponent.setInputConfig(TransmissionType.ENERGY);
        ejectorComponent = new TileComponentEjector(this);
        inventory = NonNullListSynchronized.withSize(5, ItemStack.EMPTY);

    }


    @Override
    public void setupVariableValues() {
        if (getRecipe() == null) {
            return;
        }
        boolean update = BASE_ENERGY_PER_TICK != getRecipe().energyUsage;
        BASE_ENERGY_PER_TICK = getRecipe().energyUsage;
        if (update) {
            recalculateUpgradables(Upgrade.ENERGY);
        }
    }

    @Override
    public void setUpOtherActions() {
        double prev = getEnergy();
        if (getRecipe() != null) {
            setEnergy(getEnergy() - energyPerTick * getUpgradedUsage(getRecipe()));
        }
        clientEnergyUsed = prev - getEnergy();
    }

    public int dumpAmount;

    @Override
    public void onAsyncUpdateServer() {
        super.onAsyncUpdateServer();
        ChargeUtils.discharge(3, this);
        if (!inventory.get(0).isEmpty()) {
            if (Recipe.ELECTROLYTIC_SEPARATOR.containsRecipe(inventory.get(0))) {
                if (FluidContainerUtils.isFluidContainer(inventory.get(0))) {
                    fluidTank.fill(FluidContainerUtils.extractFluid(fluidTank, this, 0), true);
                }
            }
        }
        if (!inventory.get(1).isEmpty() && leftTank.getStored() > 0) {
            leftTank.draw(GasUtils.addGas(inventory.get(1), leftTank.getGas()), true);
            MekanismUtils.saveChunk(this);
        }
        if (!inventory.get(2).isEmpty() && rightTank.getStored() > 0) {
            rightTank.draw(GasUtils.addGas(inventory.get(2), rightTank.getGas()), true);
            MekanismUtils.saveChunk(this);
        }
        SeparatorRecipe recipe = getRecipe();
        getProcess(recipe, true, energyPerTick, true, false);
        prevEnergy = getEnergy();
        dumpAmount = 8 * Math.min((int) Math.pow(2, upgradeComponent.getUpgrades(Upgrade.SPEED)), MekanismConfig.current().mekce.MAXspeedmachines.val());
        dumpAmount *= tier.processes;
    }

    @Override
    public void addTileSyncTask() {
        this.gasSpeedController.ensureSize(2, () -> Arrays.asList(new TankProvider.Gas(leftTank), new TankProvider.Gas(rightTank)));
        handleTank(leftTank, dumpLeft, configComponent.getSidesForData(TransmissionType.GAS, facing, 1), dumpAmount, 0);
        handleTank(rightTank, dumpRight, configComponent.getSidesForData(TransmissionType.GAS, facing, 2), dumpAmount, 1);
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
                    tank.draw(Math.min(stored - target, GasTankTier.values()[tier.ordinal()].getBaseOutput()), true);
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
        FluidInput input = getInput();
        if (cachedRecipe == null || !input.testEquality(cachedRecipe.getInput())) {
            cachedRecipe = RecipeHandler.getElectrolyticSeparatorRecipe(getInput());
        }
        return cachedRecipe;
    }

    public FluidInput getInput() {
        return new FluidInput(fluidTank.getFluid());
    }

    public boolean canOperate(SeparatorRecipe recipe) {
        return recipe != null && recipe.canOperate(fluidTank, leftTank, rightTank);
    }

    @Override
    public void operate(SeparatorRecipe recipe) {
        recipe.operate(fluidTank, leftTank, rightTank, getUpgradedUsage(recipe));
    }

    @Override
    public Map<FluidInput, SeparatorRecipe> getRecipes() {
        return Recipe.ELECTROLYTIC_SEPARATOR.get();
    }

    @Override
    public boolean canExtractItem(int slotID, @Nonnull ItemStack itemstack, @Nonnull EnumFacing side) {
        if (slotID == 3) {
            return ChargeUtils.canBeOutputted(itemstack, false);
        } else if (slotID == 0) {
            return FluidUtil.getFluidContained(itemstack) == null;
        } else if (slotID == 1 || slotID == 2) {
            return itemstack.getItem() instanceof IGasItem gasItem && gasItem.getGas(itemstack) != null
                    && gasItem.getGas(itemstack).amount == gasItem.getMaxGas(itemstack);
        }
        return false;
    }

    @Override
    public boolean isItemValidForSlot(int slotID, @Nonnull ItemStack itemstack) {
        if (slotID == 0) {
            return Recipe.ELECTROLYTIC_SEPARATOR.containsRecipe(itemstack);
        } else if (slotID == 1) {
            return itemstack.getItem() instanceof IGasItem gasItem &&
                    (gasItem.getGas(itemstack) == null || gasItem.getGas(itemstack).getGas() == MekanismFluids.Hydrogen);
        } else if (slotID == 2) {
            return itemstack.getItem() instanceof IGasItem gasItem &&
                    (gasItem.getGas(itemstack) == null || gasItem.getGas(itemstack).getGas() == MekanismFluids.Oxygen);
        } else if (slotID == 3) {
            return ChargeUtils.canBeDischarged(itemstack);
        }
        return true;
    }

    @Override
    public void handlePacketData(ByteBuf dataStream) {
        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            byte type = dataStream.readByte();
            if (type == 0) {
                dumpLeft = GasMode.values()[dumpLeft.ordinal() == GasMode.values().length - 1 ? 0 : dumpLeft.ordinal() + 1];
            } else if (type == 1) {
                dumpRight = GasMode.values()[dumpRight.ordinal() == GasMode.values().length - 1 ? 0 : dumpRight.ordinal() + 1];
            }
            return;
        }

        super.handlePacketData(dataStream);

        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            MachineTier prevTier = tier;
            tier = MachineTier.values()[dataStream.readInt()];
            fluidTank.setCapacity(MAX_GAS * 10 * tier.processes);
            leftTank.setMaxGas(MAX_GAS * tier.processes);
            rightTank.setMaxGas(MAX_GAS * tier.processes);
            TileUtils.readTankData(dataStream, fluidTank);
            TileUtils.readTankData(dataStream, leftTank);
            TileUtils.readTankData(dataStream, rightTank);
            dumpLeft = GasMode.values()[dataStream.readInt()];
            dumpRight = GasMode.values()[dataStream.readInt()];
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
        tier = MachineTier.values()[nbtTags.getInteger("tier")];
        fluidTank.readFromNBT(nbtTags.getCompoundTag("fluidTank"));
        leftTank.read(nbtTags.getCompoundTag("leftTank"));
        rightTank.read(nbtTags.getCompoundTag("rightTank"));
        dumpLeft = GasMode.values()[nbtTags.getInteger("dumpLeft")];
        dumpRight = GasMode.values()[nbtTags.getInteger("dumpRight")];
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
            case 1 -> new Object[]{GasTankTier.values()[tier.ordinal()].getBaseOutput()};
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
    public boolean canFill(EnumFacing from, @Nonnull FluidStack fluid) {
        if (configComponent.getOutput(TransmissionType.FLUID, from, facing).ioState == SideData.IOState.INPUT) {
            return FluidContainerUtils.canFill(fluidTank.getFluid(), fluid) && Recipe.ELECTROLYTIC_SEPARATOR.containsRecipe(fluid.getFluid());
        }
        return false;
    }

    @Override
    public int fill(EnumFacing from, @Nonnull FluidStack resource, boolean doFill) {
        return fluidTank.fill(resource, doFill);
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
        return getTankInfo(null);
    }

    @Override
    public int receiveGas(EnumFacing side, GasStack stack, boolean doTransfer) {
        return 0;
    }

    @Override
    public GasStack drawGas(EnumFacing side, int amount, boolean doTransfer) {
        if (configComponent.getOutput(TransmissionType.GAS, side, facing).hasSlot(1)) {
            return leftTank.draw(amount, doTransfer);
        } else if (configComponent.getOutput(TransmissionType.GAS, side, facing).hasSlot(2)) {
            return rightTank.draw(amount, doTransfer);
        }
        return null;
    }

    @Override
    public boolean canReceiveGas(EnumFacing side, Gas type) {
        return false;
    }

    @Override
    public boolean canDrawGas(EnumFacing side, Gas type) {
        if (configComponent.getOutput(TransmissionType.GAS, side, facing).hasSlot(1)) {
            return leftTank.getGas() != null && leftTank.getGas().getGas() == type;
        } else if (configComponent.getOutput(TransmissionType.GAS, side, facing).hasSlot(2)) {
            return rightTank.getGas() != null && rightTank.getGas().getGas() == type;
        }
        return false;
    }

    @Nonnull
    @Override
    public GasTankInfo[] getTankInfo() {
        return new GasTankInfo[]{leftTank, rightTank};
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, EnumFacing side) {
        if (isCapabilityDisabled(capability, side)) {
            return false;
        }
        return capability == Capabilities.GAS_HANDLER_CAPABILITY || capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || capability == Capabilities.SPECIAL_CONFIG_DATA_CAPABILITY || super.hasCapability(capability, side);
    }

    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, EnumFacing side) {
        if (isCapabilityDisabled(capability, side)) {
            return null;
        } else if (capability == Capabilities.GAS_HANDLER_CAPABILITY) {
            return Capabilities.GAS_HANDLER_CAPABILITY.cast(this);
        } else if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(new FluidHandlerWrapper(this, side));
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
    public Object[] getTanks() {
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
    public boolean getEnergySlot() {
        return inventory.get(3).isEmpty();
    }

    @Override
    public boolean getInputSlot() {
        return inventory.get(0).isEmpty();
    }

    @Override
    public boolean getOuputSlot() {
        return false;
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
    public boolean upgrade(BaseTier upgradeTier) {
        if (upgradeTier.ordinal() != tier.ordinal() + 1) {
            return false;
        }
        if (upgradeTier == BaseTier.CREATIVE){
            return false;
        }
        tier = MachineTier.values()[upgradeTier.ordinal()];
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
        dumpLeft = GasMode.values()[nbtTags.getInteger("dumpLeft")];
        dumpRight = GasMode.values()[nbtTags.getInteger("dumpRight")];
    }

    @Override
    public String getDataType() {
        return getName();
    }

    public boolean isUpgrade = true;

    @Override
    @Method(modid = "mekanismmultiblockmachine")
    public boolean largeMachineUpgrade(EntityPlayer player) {
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

        isUpgrade = false;
        if (world.getTileEntity(getPos()) instanceof IBoundingBlock block) {
            block.onBreak();
        } else {
            world.setBlockToAir(getPos());
        }

        world.setBlockState(getPos(), MultiblockMachineBlocks.LargeElectrolyticSeparator.getDefaultState(), 3);
        if (world.getTileEntity(getPos()) instanceof TileEntityLargeElectrolyticSeparator tile) {
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
            tile.leftTank.setGas(leftTank.getGas());
            tile.rightTank.setGas(rightTank.getGas());
            tile.fluidTank.setFluid(fluidTank.getFluid());
            tile.dumpLeft = dumpLeft;
            tile.dumpRight = dumpRight;
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
