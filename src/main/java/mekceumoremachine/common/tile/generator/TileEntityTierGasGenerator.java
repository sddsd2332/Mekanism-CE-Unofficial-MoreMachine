package mekceumoremachine.common.tile.generator;

import io.netty.buffer.ByteBuf;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.EnumColor;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.api.TileNetworkList;
import mekanism.api.gas.*;
import mekanism.common.Mekanism;
import mekanism.common.base.IBoundingBlock;
import mekanism.common.base.IComparatorSupport;
import mekanism.common.base.ISpecialSelectionWireframeTile;
import mekanism.common.base.ISustainedData;
import mekanism.common.capabilities.holder.gas.GasTankHelper;
import mekanism.common.capabilities.holder.gas.IGasTankHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.config.MekanismConfig;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.inventory.slot.gas.GasInventorySlot;
import mekanism.common.recipe.GasStackFuelToEnergyRecipe;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.inputs.GasInput;
import mekanism.common.tier.BaseTier;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.upgrade.TierUpgradeData;
import mekanism.common.util.*;
import mekanism.generators.common.tile.TileEntityGenerator;
import mekanism.multiblockmachine.common.registries.MultiblockMachineBlocks;
import mekceumoremachine.common.capability.ResizableGasTank;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.interfaces.ILargeMachine;
import mekceumoremachine.common.tile.interfaces.ITierMachine;
import mekceumoremachine.common.upgrade.FirstGasGeneratorUpgradeData;
import mekceumoremachine.common.upgrade.LargeMachineUpgradeDataApplier;
import mekceumoremachine.common.upgrade.LargeGasGeneratorUpgradeData;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Optional.Interface;
import net.minecraftforge.fml.common.Optional.InterfaceList;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;

@InterfaceList({
        @Interface(iface = "mekceumoremachine.common.tile.interfaces.ILargeMachine", modid = "mekanismmultiblockmachine"),
})
public class TileEntityTierGasGenerator extends TileEntityGenerator implements ISustainedData, IComparatorSupport, ITierMachine<MachineTier>, ILargeMachine, ISpecialSelectionWireframeTile {

    private static final String[] methods = new String[]{"getEnergy", "getOutput", "getMaxEnergy", "getEnergyNeeded", "getGas", "getGasNeeded"};
    /**
     * The maximum amount of gas this block can store.
     */
    public static final int MAX_GAS = 18000;
    /**
     * The tank this block is storing fuel in.
     */
    public ResizableGasTank fuelTank;
    public int burnTicks = 0;
    public int maxBurnTicks;
    public double generationRate = 0;
    public double clientUsed;
    private int currentRedstoneLevel;
    public GasStackFuelToEnergyRecipe cachedRecipe;
    private int cachedRecipeVersion = -1;
    private GasInventorySlot fuelSlot;
    private EnergyInventorySlot energySlot;

    public MachineTier tier = MachineTier.BASIC;

    public TileEntityTierGasGenerator() {
        super("gas", "TierGasGenerator", 0, 0);
        initializeInventorySlots();
    }

    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = createInventorySlotHelper();
        fuelSlot = builder.addSlot(GasInventorySlot.fill(fuelTank, listener, 17, 35),
              RelativeSide.LEFT, RelativeSide.BACK, RelativeSide.TOP, RelativeSide.BOTTOM);
        fuelSlot.setSlotOverlay(SlotOverlay.MINUS);
        energySlot = builder.addSlot(EnergyInventorySlot.drain(getMainEnergyContainer(listener), listener, 143, 35), RelativeSide.RIGHT);
        energySlot.setSlotOverlay(SlotOverlay.PLUS);
        return builder.build();
    }

    @Override
    protected IGasTankHolder getInitialGasTanks(IContentsListener listener) {
        GasTankHelper builder = createGasTankHelper();
        builder.addTank(getOrCreateFuelTank(listener), RelativeSide.LEFT, RelativeSide.BACK, RelativeSide.TOP, RelativeSide.BOTTOM);
        return builder.build();
    }

    private ResizableGasTank getOrCreateFuelTank(IContentsListener listener) {
        if (fuelTank == null) {
            fuelTank = new FuelTank(listener);
        }
        return fuelTank;
    }

    @Override
    public void onAsyncUpdateServer() {
        super.onAsyncUpdateServer();
        energySlot.drainContainer();
        fuelSlot.fillTank();
        GasStackFuelToEnergyRecipe recipe = getRecipe();
        boolean operate = recipe != null && canOperate();
        if (operate && getEnergyContainer().insert(generationRate, Action.SIMULATE, AutomationType.INTERNAL) == 0) {
            setActive(true);
            if (!fuelTank.isEmpty()) {
                maxBurnTicks = Math.max(1, recipe.getInput().ingredient.amount);
                generationRate = recipe.getOutput().energyOutput;
            }

            int toUse = getToUse();

            int total = burnTicks + fuelTank.getStored() * maxBurnTicks;
            total -= toUse;
            getEnergyContainer().insert(generationRate * toUse, Action.EXECUTE, AutomationType.INTERNAL);

            if (!fuelTank.isEmpty()) {
                fuelTank.setStackSize(total / maxBurnTicks, Action.EXECUTE);
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
        output = MekanismConfig.current().general.FROM_H2.val() * 2 * tier.processes;
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
            return EnergyInventorySlot.drainExtractCheck(getMainEnergyContainer(), itemstack);
        } else if (slotID == 0) {
            return GasInventorySlot.fillExtractCheck(fuelTank, itemstack);
        }
        return false;
    }

    @Override
    public boolean canOperate() {
        return (fuelTank.getStored() > 0 || burnTicks > 0) && getRecipe() != null && MekanismUtils.canFunction(this);
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
            tier = MachineTier.byIndex(dataStream.readInt());
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
    public void readCustomNBT(NBTTagCompound nbtTags) {
        super.readCustomNBT(nbtTags);
        tier = MachineTier.byIndex(nbtTags.getInteger("tier"));
        fuelTank.setMaxGas(tier.processes * MAX_GAS);
        fuelTank.read(nbtTags.getCompoundTag("fuelTank"));
        sanitizeFuelTank();
        updateOutputForStoredFuel();
    }

    @Override
    public void writeCustomNBT(NBTTagCompound nbtTags) {
        super.writeCustomNBT(nbtTags);
        nbtTags.setInteger("tier", tier.ordinal());
        nbtTags.setTag("fuelTank", fuelTank.write(new NBTTagCompound()));
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
            sanitizeFuelTank();
            updateOutputForStoredFuel();
        }
    }

    private void sanitizeFuelTank() {
        GasStack stored = fuelTank.getGas();
        if (stored != null && (stored.amount <= 0 || stored.getGas() == null)) {
            fuelTank.setEmpty();
        } else if (stored != null) {
            fuelTank.setStackSize(stored.amount, Action.EXECUTE);
        }
    }

    private void updateOutputForStoredFuel() {
        GasStackFuelToEnergyRecipe recipe = RecipeHandler.getGasStackFuelToEnergyRecipe(fuelTank.getGas());
        if (recipe != null) {
            generationRate = recipe.getOutput().energyOutput;
            output = generationRate * 2 * tier.processes;
        }
    }

    @Override
    public int getRedstoneLevel() {
        return MekanismUtils.redstoneLevelFromContents(fuelTank.getStored(), fuelTank.getMaxGas());
    }

    public GasStackFuelToEnergyRecipe getRecipe() {
        int recipeVersion = RecipeHandler.Recipe.GAS_FUEL_TO_ENERGY_RECIPE.getRecipeVersion();
        if (cachedRecipeVersion != recipeVersion) {
            cachedRecipe = null;
            cachedRecipeVersion = recipeVersion;
        }
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
    public boolean applyTierUpgrade(BaseTier upgradeTier) {
        if (!tier.canUpgradeTo(upgradeTier)) {
            return false;
        }
        tier = MachineTier.get(upgradeTier);
        fuelTank.setMaxGas(tier.processes * MAX_GAS);
        updateOutputForStoredFuel();
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

    private class FuelTank extends ResizableGasTank {

        private FuelTank(IContentsListener listener) {
            super(MAX_GAS * tier.processes, NOT_EXTERNAL, ALWAYS_TRUE_BI,
                  stack -> stack != null && stack.getGas() != null && RecipeHandler.Recipe.GAS_FUEL_TO_ENERGY_RECIPE.containsRecipe(stack.getGas()), listener);
        }

        @Override
        public void setStack(GasStack stack) {
            boolean wasEmpty = isEmpty();
            super.setStack(stack);
            recheckOutput(stack, wasEmpty);
        }

        @Override
        public void setStackUnchecked(GasStack stack) {
            boolean wasEmpty = isEmpty();
            super.setStackUnchecked(stack);
            recheckOutput(stack, wasEmpty);
        }

        private void recheckOutput(GasStack stack, boolean wasEmpty) {
            if (wasEmpty && stack != null && stack.amount > 0) {
                GasStackFuelToEnergyRecipe recipe = RecipeHandler.getGasStackFuelToEnergyRecipe(stack);
                if (recipe != null) {
                    generationRate = recipe.getOutput().energyOutput;
                    output = generationRate * 2 * tier.processes;
                }
            }
        }
    }

    public boolean isUpgrade = true;

    @Override
    public IUpgradeData getUpgradeData(BaseTier upgradeTier) {
        if (upgradeTier == BaseTier.ULTIMATE && tier == MachineTier.ULTIMATE) {
            return new LargeGasGeneratorUpgradeData(upgradeTier, this);
        }
        return ITierMachine.super.getUpgradeData(upgradeTier);
    }

    @Override
    public IBlockState getUpgradeResult(BaseTier upgradeTier) {
        return upgradeTier == BaseTier.ULTIMATE && tier == MachineTier.ULTIMATE ? MultiblockMachineBlocks.LargeGasGenerator.getDefaultState() : null;
    }

    @Override
    public void prepareForUpgrade() {
        isUpgrade = false;
    }

    @Override
    public boolean parseUpgradeData(IUpgradeData upgradeData) {
        if (upgradeData instanceof FirstGasGeneratorUpgradeData data && data.getUpgradeTier() == tier.getBaseTier()) {
            LargeMachineUpgradeDataApplier.applyCommon(this, data, null, securityComponent);
            burnTicks = data.burnTicks;
            maxBurnTicks = data.maxBurnTicks;
            generationRate = data.generationRate;
            clientUsed = data.clientUsed;
            fuelTank.setGas(data.fuel == null ? null : data.fuel.copy());
            isUpgrade = true;
            LargeMachineUpgradeDataApplier.finish(this, null);
            return true;
        }
        if (upgradeData instanceof TierUpgradeData data && data.getUpgradeTier() == BaseTier.ULTIMATE && tier == MachineTier.ULTIMATE) {
            return applyLargeMachineUpgrade();
        }
        return ITierMachine.super.parseUpgradeData(upgradeData);
    }

    @Override
    public boolean canLargeMachineUpgrade(EntityPlayer player) {
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
        return true;
    }

    @Override
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

    @Override
    @SideOnly(Side.CLIENT)
    public Class<?> getSelectionWireframeModelClass() {
        return mekceumoremachine.client.model.generator.ModelTierGasGenerator.class;
    }


}
