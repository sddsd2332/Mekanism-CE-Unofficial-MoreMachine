package mekceumoremachine.common.tile.machine;

import io.netty.buffer.ByteBuf;
import mekanism.api.EnumColor;
import mekanism.api.IContentsListener;
import mekanism.api.TileNetworkList;
import mekanism.api.gas.*;
import mekanism.api.text.TextComponentGroup;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.Mekanism;
import mekanism.common.Upgrade;
import mekanism.common.base.IBoundingBlock;
import mekanism.common.base.IGuiProvider;
import mekanism.common.base.ISustainedData;
import mekanism.common.base.ITankManager;
import mekanism.common.block.states.BlockStateMachine;
import mekanism.common.capabilities.holder.gas.GasTankHelper;
import mekanism.common.capabilities.holder.gas.IGasTankHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.config.MekanismConfig;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.inventory.slot.gas.GasInventorySlot;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.cache.CachedRecipe;
import mekanism.common.recipe.cache.ChemicalPairCachedRecipe;
import mekanism.common.recipe.cache.inputs.InputHelper;
import mekanism.common.recipe.cache.outputs.OutputHelper;
import mekanism.common.recipe.inputs.ChemicalPairInput;
import mekanism.common.recipe.machines.ChemicalInfuserRecipe;
import mekanism.common.recipe.outputs.GasOutput;
import mekanism.common.tier.BaseTier;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.upgrade.TierUpgradeData;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.prefab.TileEntityBasicMachine;
import mekanism.common.util.*;
import mekceumoremachine.common.capability.ResizableGasTank;
import mekanism.multiblockmachine.common.registries.MultiblockMachineBlocks;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.interfaces.ILargeMachine;
import mekceumoremachine.common.tile.interfaces.ITierMachine;
import mekceumoremachine.common.upgrade.FirstChemicalInfuserUpgradeData;
import mekceumoremachine.common.upgrade.LargeChemicalInfuserUpgradeData;
import mekceumoremachine.common.upgrade.LargeMachineUpgradeDataApplier;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
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
public class TileEntityTierChemicalInfuser extends TileEntityBasicMachine<ChemicalPairInput, GasOutput, ChemicalInfuserRecipe> implements ISustainedData, Upgrade.IUpgradeInfoHandler,
        ITankManager, ITierMachine<MachineTier>, ILargeMachine {


    public static final int MAX_GAS = 10000;
    public ResizableGasTank leftTank;
    public ResizableGasTank rightTank;
    public ResizableGasTank centerTank;

    public ChemicalInfuserRecipe cachedRecipe;

    public double clientEnergyUsed;

    public MachineTier tier = MachineTier.BASIC;
    private GasInventorySlot leftSlot;
    private GasInventorySlot rightSlot;
    private GasInventorySlot centerSlot;
    private EnergyInventorySlot energySlot;

    public TileEntityTierChemicalInfuser() {
        super("cheminfuser", "TierChemicalInfuser", 0, BlockStateMachine.MachineType.CHEMICAL_INFUSER.getUsage(), 4, 1);
        configComponent = new TileComponentConfig(this, TransmissionType.ITEM, TransmissionType.GAS, TransmissionType.ENERGY);
        initializeInventorySlots();
        configComponent.setupItemDualInputOutputConfig(leftSlot, rightSlot, centerSlot, energySlot);
        configComponent.setConfig(TransmissionType.ITEM, DataType.NONE, DataType.NONE, DataType.OUTPUT, DataType.ENERGY, DataType.INPUT_1, DataType.INPUT_2);
        configComponent.setCanEject(TransmissionType.ITEM, false);

        configComponent.setupGasDualInputOutputConfig(leftTank, rightTank, centerTank);
        configComponent.setConfig(TransmissionType.GAS, DataType.NONE, DataType.NONE, DataType.OUTPUT, DataType.NONE, DataType.INPUT_1, DataType.INPUT_2);

        configComponent.setInputConfig(TransmissionType.ENERGY);

        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(configComponent, TransmissionType.GAS);
    }

    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = createInventorySlotHelper();
        leftSlot = builder.addSlot(GasInventorySlot.fill(leftTank, listener, 6, 56));
        rightSlot = builder.addSlot(GasInventorySlot.fill(rightTank, listener, 154, 56));
        centerSlot = builder.addSlot(GasInventorySlot.drain(centerTank, listener, 80, 65));
        energySlot = builder.addSlot(EnergyInventorySlot.fillOrConvert(getMainEnergyContainer(), this::getWorld, listener, 154, 14));
        leftSlot.setSlotType(ContainerSlotType.INPUT);
        leftSlot.setSlotOverlay(SlotOverlay.MINUS);
        rightSlot.setSlotType(ContainerSlotType.INPUT);
        rightSlot.setSlotOverlay(SlotOverlay.MINUS);
        centerSlot.setSlotType(ContainerSlotType.OUTPUT);
        centerSlot.setSlotOverlay(SlotOverlay.PLUS);
        return builder.build();
    }

    @Override
    protected IGasTankHolder getInitialGasTanks(IContentsListener listener) {
        GasTankHelper builder = createGasTankHelper();
        builder.addTank(getOrCreateLeftTank(listener));
        builder.addTank(getOrCreateRightTank(listener));
        builder.addTank(getOrCreateCenterTank(listener));
        return builder.build();
    }

    private ResizableGasTank getOrCreateLeftTank(IContentsListener listener) {
        if (leftTank == null) {
            leftTank = ResizableGasTank.input(MAX_GAS * tier.processes, this::isValidLeftGas, listener);
        }
        return leftTank;
    }

    private ResizableGasTank getOrCreateRightTank(IContentsListener listener) {
        if (rightTank == null) {
            rightTank = ResizableGasTank.input(MAX_GAS * tier.processes, this::isValidRightGas, listener);
        }
        return rightTank;
    }

    private ResizableGasTank getOrCreateCenterTank(IContentsListener listener) {
        if (centerTank == null) {
            centerTank = ResizableGasTank.output(MAX_GAS * tier.processes, listener);
        }
        return centerTank;
    }

    @Override
    public void onAsyncUpdateServer() {
        super.onAsyncUpdateServer();
        energySlot.fillContainerOrConvert();
        leftSlot.fillTank();
        rightSlot.fillTank();
        centerSlot.drainTank();

        clientEnergyUsed = processRecipe(getMainEnergyContainer());
        prevEnergy = getEnergy();
    }

    public int getUpgradedUsage(ChemicalInfuserRecipe recipe) {
        int possibleProcess = Math.min((int) Math.pow(2, upgradeComponent.getUpgrades(Upgrade.SPEED)), MekanismConfig.current().mekce.MAXspeedmachines.val());
        possibleProcess *= tier.processes;
        if (leftTank.getGasType() == recipe.recipeInput.leftGas.getGas()) {
            possibleProcess = Math.min(leftTank.getStored() / recipe.recipeInput.leftGas.amount, possibleProcess);
            possibleProcess = Math.min(rightTank.getStored() / recipe.recipeInput.rightGas.amount, possibleProcess);
        } else {
            possibleProcess = Math.min(leftTank.getStored() / recipe.recipeInput.rightGas.amount, possibleProcess);
            possibleProcess = Math.min(rightTank.getStored() / recipe.recipeInput.leftGas.amount, possibleProcess);
        }
        possibleProcess = Math.min(centerTank.getNeeded() / recipe.recipeOutput.output.amount, possibleProcess);
        possibleProcess = Math.min((int) (getEnergy() / energyPerTick), possibleProcess);
        possibleProcess = Math.max(possibleProcess, 1);
        return possibleProcess;
    }


    @Override
    public ChemicalPairInput getInput() {
        return new ChemicalPairInput(leftTank.getGas(), rightTank.getGas());
    }

    @Override
    public ChemicalInfuserRecipe getRecipe() {
        refreshRecipeLookupCache();
        ChemicalPairInput input = getInput();
        if (cachedRecipe == null || !input.testEquality(cachedRecipe.getInput())) {
            cachedRecipe = RecipeHandler.getChemicalInfuserRecipe(getInput());
        }
        return cachedRecipe;
    }

    @Override
    protected void clearRecipeLookupCache() {
        super.clearRecipeLookupCache();
        cachedRecipe = null;
    }

    @Override
    public ChemicalInfuserRecipe getRecipe(int cacheIndex) {
        return getRecipe();
    }

    @Override
    public boolean canOperate(ChemicalInfuserRecipe recipe) {
        return recipe != null && recipe.canOperate(leftTank, rightTank, centerTank);
    }

    @Override
    public CachedRecipe<ChemicalInfuserRecipe> createNewCachedRecipe(ChemicalInfuserRecipe recipe, int cacheIndex) {
        return new ChemicalPairCachedRecipe<>(recipe, this::shouldRecheckAllRecipeErrors,
              InputHelper.getGasInputHandler(leftTank, CachedRecipe.OperationTracker.RecipeError.NOT_ENOUGH_LEFT_INPUT),
              InputHelper.getGasInputHandler(rightTank, CachedRecipe.OperationTracker.RecipeError.NOT_ENOUGH_RIGHT_INPUT),
              OutputHelper.getGasOutputHandler(centerTank, CachedRecipe.OperationTracker.RecipeError.NOT_ENOUGH_OUTPUT_SPACE), recipe.getInput(), recipe.getOutput())
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
    public Map<ChemicalPairInput, ChemicalInfuserRecipe> getRecipes() {
        return RecipeHandler.Recipe.CHEMICAL_INFUSER.get();
    }

    private boolean isValidLeftGas(Gas gas) {
        return isValidGas(gas, rightTank);
    }

    private boolean isValidRightGas(Gas gas) {
        return isValidGas(gas, leftTank);
    }

    private boolean isValidGas(Gas gas, GasTank otherTank) {
        if (gas == null) {
            return false;
        }
        Gas otherGas = otherTank == null ? null : otherTank.getGasType();
        for (ChemicalPairInput input : getRecipes().keySet()) {
            if (otherGas == null && (input.leftGas.getGas() == gas || input.rightGas.getGas() == gas)) {
                return true;
            } else if (otherGas != null && ((input.leftGas.getGas() == gas && input.rightGas.getGas() == otherGas) ||
                  (input.rightGas.getGas() == gas && input.leftGas.getGas() == otherGas))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void handlePacketData(ByteBuf dataStream) {
        super.handlePacketData(dataStream);
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            MachineTier prevTier = tier;
            tier = MachineTier.byIndex(dataStream.readInt());
            leftTank.setMaxGas(tier.processes * MAX_GAS);
            rightTank.setMaxGas(tier.processes * MAX_GAS);
            centerTank.setMaxGas(tier.processes * MAX_GAS);
            clientEnergyUsed = dataStream.readDouble();
            TileUtils.readTankData(dataStream, leftTank);
            TileUtils.readTankData(dataStream, rightTank);
            TileUtils.readTankData(dataStream, centerTank);
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
        TileUtils.addTankData(data, leftTank);
        TileUtils.addTankData(data, rightTank);
        TileUtils.addTankData(data, centerTank);
        return data;
    }

    @Override
    public void readCustomNBT(NBTTagCompound nbtTags) {
        super.readCustomNBT(nbtTags);
        tier = MachineTier.byIndex(nbtTags.getInteger("tier"));
        leftTank.setMaxGas(tier.processes * MAX_GAS);
        rightTank.setMaxGas(tier.processes * MAX_GAS);
        centerTank.setMaxGas(tier.processes * MAX_GAS);
        leftTank.read(nbtTags.getCompoundTag("leftTank"));
        rightTank.read(nbtTags.getCompoundTag("rightTank"));
        centerTank.read(nbtTags.getCompoundTag("centerTank"));
    }

    @Override
    public void writeCustomNBT(NBTTagCompound nbtTags) {
        super.writeCustomNBT(nbtTags);
        nbtTags.setInteger("tier", tier.ordinal());
        nbtTags.setTag("leftTank", leftTank.write(new NBTTagCompound()));
        nbtTags.setTag("rightTank", rightTank.write(new NBTTagCompound()));
        nbtTags.setTag("centerTank", centerTank.write(new NBTTagCompound()));
    }

    @Override
    public void writeSustainedData(ItemStack itemStack) {
        if (leftTank.getGas() != null) {
            ItemDataUtils.setCompound(itemStack, "leftTank", leftTank.getGas().write(new NBTTagCompound()));
        }
        if (rightTank.getGas() != null) {
            ItemDataUtils.setCompound(itemStack, "rightTank", rightTank.getGas().write(new NBTTagCompound()));
        }
        if (centerTank.getGas() != null) {
            ItemDataUtils.setCompound(itemStack, "centerTank", centerTank.getGas().write(new NBTTagCompound()));
        }
    }

    @Override
    public void readSustainedData(ItemStack itemStack) {
        leftTank.setGas(GasStack.readFromNBT(ItemDataUtils.getCompound(itemStack, "leftTank")));
        rightTank.setGas(GasStack.readFromNBT(ItemDataUtils.getCompound(itemStack, "rightTank")));
        centerTank.setGas(GasStack.readFromNBT(ItemDataUtils.getCompound(itemStack, "centerTank")));
    }

    @Override
    public List<String> getInfo(Upgrade upgrade) {
        return upgrade == Upgrade.SPEED ? upgrade.getExpScaledInfo(this) : upgrade.getMultScaledInfo(this);
    }


    @Override
    public Object[] getManagedTanks() {
        return new Object[]{leftTank, rightTank, centerTank};
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
    public boolean applyTierUpgrade(BaseTier upgradeTier) {
        if (!tier.canUpgradeTo(upgradeTier)) {
            return false;
        }
        tier = MachineTier.get(upgradeTier);
        leftTank.setMaxGas(tier.processes * MAX_GAS);
        rightTank.setMaxGas(tier.processes * MAX_GAS);
        centerTank.setMaxGas(tier.processes * MAX_GAS);
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
        return LangUtils.localize("tile.TierChemicalInfuser." + tier.getBaseTier().getSimpleName() + ".name");
    }

    @Override
    public int getBlockGuiID(Block block, int metadata) {
        return 6;
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
        return BlockStateMachine.MachineType.CHEMICAL_INFUSER.getStorage() * tier.processes;
    }


    public boolean isUpgrade = true;

    @Override
    public IUpgradeData getUpgradeData(BaseTier upgradeTier) {
        if (upgradeTier == BaseTier.ULTIMATE && tier == MachineTier.ULTIMATE) {
            return new LargeChemicalInfuserUpgradeData(upgradeTier, this);
        }
        return ITierMachine.super.getUpgradeData(upgradeTier);
    }

    @Override
    public IBlockState getUpgradeResult(BaseTier upgradeTier) {
        return upgradeTier == BaseTier.ULTIMATE && tier == MachineTier.ULTIMATE ? MultiblockMachineBlocks.LargeChemicalInfuser.getDefaultState() : null;
    }

    @Override
    public void prepareForUpgrade() {
        isUpgrade = false;
    }

    @Override
    public boolean parseUpgradeData(IUpgradeData upgradeData) {
        if (upgradeData instanceof FirstChemicalInfuserUpgradeData data && data.getUpgradeTier() == tier.getBaseTier()) {
            LargeMachineUpgradeDataApplier.applyCommon(this, data, upgradeComponent, securityComponent);
            clientEnergyUsed = data.clientEnergyUsed;
            prevEnergy = data.prevEnergy;
            configComponent.read(data.configComponentData.copy());
            ejectorComponent.read(data.ejectorComponentData.copy());
            ejectorComponent.setOutputData(configComponent, TransmissionType.GAS);
            leftTank.setGas(data.leftGas == null ? null : data.leftGas.copy());
            rightTank.setGas(data.rightGas == null ? null : data.rightGas.copy());
            centerTank.setGas(data.centerGas == null ? null : data.centerGas.copy());
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
                    if (yPos ==0 && xPos ==0 && zPos ==0){
                        continue;
                    }
                    testPos.setPos(pos.getX() + xPos, pos.getY() + yPos, pos.getZ() + zPos);
                    Block b = world.getBlockState(testPos).getBlock();
                    if (!world.isValid(testPos) || !world.isBlockLoaded(testPos, false) || !b.isReplaceable(world, testPos)) {
                        player.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + Mekanism.LOG_TAG + EnumColor.GREY + " " + LangUtils.localize("tooltip.largeMachineUpgrade.pos")+ ": " +  "X " + testPos.getX() + " " + "Y " +  testPos.getY() +" " + "Z " +  testPos.getZ()));
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


