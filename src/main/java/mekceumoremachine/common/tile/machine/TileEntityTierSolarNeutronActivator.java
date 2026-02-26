package mekceumoremachine.common.tile.machine;

import io.netty.buffer.ByteBuf;
import mekanism.api.Coord4D;
import mekanism.api.EnumColor;
import mekanism.api.IConfigCardAccess;
import mekanism.api.TileNetworkList;
import mekanism.api.gas.*;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.Mekanism;
import mekanism.common.SideData;
import mekanism.common.Upgrade;
import mekanism.common.base.*;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.config.MekanismConfig;
import mekanism.common.integration.computer.IComputerIntegration;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.inputs.GasInput;
import mekanism.common.recipe.machines.SolarNeutronRecipe;
import mekanism.common.recipe.outputs.GasOutput;
import mekanism.common.security.ISecurityTile;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.TileComponentSecurity;
import mekanism.common.tile.component.TileComponentUpgrade;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.prefab.TileEntityContainerBlock;
import mekanism.common.util.*;
import mekanism.multiblockmachine.common.registries.MultiblockMachineBlocks;
import mekanism.multiblockmachine.common.tile.machine.TileEntityLargeSolarNeutronActivator;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.interfaces.ILargeMachine;
import mekceumoremachine.common.tile.interfaces.ITierMachine;
import net.minecraft.block.Block;
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
public class TileEntityTierSolarNeutronActivator extends TileEntityContainerBlock implements IUpgradeTile, IRedstoneControl, ISecurityTile, IElectricMachine<GasInput, GasOutput, SolarNeutronRecipe>, IComputerIntegration, ISideConfiguration, IConfigCardAccess,
        IMachineSlotTip, IBoundingBlock, IGasHandler, ISustainedData, ITankManager, Upgrade.IUpgradeInfoHandler, IComparatorSupport, IActiveState, ITierMachine<MachineTier>, ILargeMachine {


    public static final int MAX_GAS = 10000;
    public GasTank inputTank;
    public GasTank outputTank;
    private SolarNeutronRecipe cachedRecipe;
    private int currentRedstoneLevel;
    private boolean isActive;
    private long lastActive = -1;
    private boolean needsRainCheck;
    public TileComponentEjector ejectorComponent;
    public TileComponentConfig configComponent;
    public int operatingTicks;

    public int BASE_TICKS_REQUIRED;

    public int ticksRequired;
    private final int RECENT_THRESHOLD = 100;
    public TileComponentUpgrade upgradeComponent;
    public TileComponentSecurity securityComponent = new TileComponentSecurity(this);
    private RedstoneControl controlType = RedstoneControl.DISABLED;

    public MachineTier tier = MachineTier.BASIC;

    public TileEntityTierSolarNeutronActivator() {
        this(1);
    }

    public TileEntityTierSolarNeutronActivator(int baseTicksRequired) {
        super("TierSolarNeutronActivator");
        inputTank = new GasTank(tier.processes * MAX_GAS);
        outputTank = new GasTank(tier.processes * MAX_GAS);
        ticksRequired = BASE_TICKS_REQUIRED = baseTicksRequired;
        upgradeComponent = new TileComponentUpgrade(this, 2, Upgrade.SPEED);
        configComponent = new TileComponentConfig(this, TransmissionType.ITEM, TransmissionType.GAS);
        configComponent.addOutput(TransmissionType.ITEM, new SideData(DataType.NONE, InventoryUtils.EMPTY));
        configComponent.addOutput(TransmissionType.ITEM, new SideData(DataType.INPUT, new int[]{0}));
        configComponent.addOutput(TransmissionType.ITEM, new SideData(DataType.OUTPUT, new int[]{1}));
        configComponent.setConfig(TransmissionType.ITEM, new byte[]{1, -1, 2, 1, 1, 1});
        configComponent.setCanEject(TransmissionType.ITEM, false);

        configComponent.addOutput(TransmissionType.GAS, new SideData(DataType.NONE, InventoryUtils.EMPTY));
        configComponent.addOutput(TransmissionType.GAS, new SideData(DataType.INPUT, new int[]{0}));
        configComponent.addOutput(TransmissionType.GAS, new SideData(DataType.OUTPUT, new int[]{1}));
        configComponent.addOutput(TransmissionType.GAS, new SideData(new int[]{0, 1}, new boolean[]{false, true}));
        configComponent.setConfig(TransmissionType.GAS, new byte[]{1, -1, 2, 1, 1, 1});

        inventory = NonNullListSynchronized.withSize(3, ItemStack.EMPTY);

        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(TransmissionType.GAS, configComponent.getOutputs(TransmissionType.GAS).get(2));
        ejectorComponent.setInputOutputData(TransmissionType.GAS, configComponent.getOutputs(TransmissionType.GAS).get(3));
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
        ItemStack stack = inventory.get(0);
        if (!stack.isEmpty() && stack.getItem() instanceof IGasItem item && item.getGas(stack) != null && RecipeHandler.Recipe.SOLAR_NEUTRON_ACTIVATOR.containsRecipe(item.getGas(stack).getGas())) {
            TileUtils.receiveGasItem(inventory.get(0), inputTank);
        }
        TileUtils.drawGas(inventory.get(1), outputTank);
        SolarNeutronRecipe recipe = getRecipe();

        // TODO: Ideally the neutron activator should use the sky brightness to determine throughput; but
        // changing this would dramatically affect a lot of setups with Fusion reactors which can take
        // a long time to relight. I don't want to be chased by a mob right now, so just doing basic
        // rain checks.
        boolean seesSun = world.isDaytime() && world.canSeeSky(getPos().up()) && !world.provider.isNether();
        if (needsRainCheck) {
            seesSun &= !(world.isRaining() || world.isThundering());
        }

        if (seesSun && canOperate(recipe) && MekanismUtils.canFunction(this)) {
            setActive(true);
            MultipleActions(recipe);
        } else {
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
        possibleProcess = Math.min(Math.min(inputTank.getStored(), outputTank.getNeeded()), possibleProcess);
        return Math.min(inputTank.getStored() / recipe.recipeInput.ingredient.amount, possibleProcess);
    }

    public SolarNeutronRecipe getRecipe() {
        GasInput input = getInput();
        if (cachedRecipe == null || !input.testEquality(cachedRecipe.getInput())) {
            cachedRecipe = RecipeHandler.getSolarNeutronRecipe(getInput());
        }
        return cachedRecipe;
    }

    @Override
    public GasInput getInput() {
        return new GasInput(inputTank.getGas());
    }

    @Override
    public boolean canOperate(SolarNeutronRecipe recipe) {
        return recipe != null && recipe.canOperate(inputTank, outputTank);
    }

    @Override
    public void operate(SolarNeutronRecipe recipe) {
        recipe.operate(inputTank, outputTank, getUpgradedUsage(recipe));
    }

    @Override
    public Map<GasInput, SolarNeutronRecipe> getRecipes() {
        return RecipeHandler.Recipe.SOLAR_NEUTRON_ACTIVATOR.get();
    }

    @Override
    public void handlePacketData(ByteBuf dataStream) {
        super.handlePacketData(dataStream);
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            MachineTier prevTier = tier;
            tier = MachineTier.values()[dataStream.readInt()];
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
            controlType = RedstoneControl.values()[dataStream.readInt()];
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
        tier = MachineTier.values()[nbtTags.getInteger("tier")];
        isActive = nbtTags.getBoolean("isActive");
        controlType = RedstoneControl.values()[nbtTags.getInteger("controlType")];
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
    public int receiveGas(EnumFacing side, GasStack stack, boolean doTransfer) {
        if (canReceiveGas(side, stack.getGas())) {
            int recipeAmount = RecipeHandler.Recipe.SOLAR_NEUTRON_ACTIVATOR.get().get(new GasInput(stack)).recipeInput.ingredient.amount;
            int receivable = inputTank.receive(stack, false);
            int stored = inputTank.stored != null ? inputTank.stored.amount : 0;
            int newStored = stored + receivable;
            int amount = newStored - stored - newStored % recipeAmount;
            return inputTank.receive(stack.copy().withAmount(amount), doTransfer);
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
    public boolean canReceiveGas(EnumFacing side, Gas type) {
        return configComponent.getOutput(TransmissionType.GAS, side, facing).hasSlot(0) && inputTank.canReceive(type) && RecipeHandler.Recipe.SOLAR_NEUTRON_ACTIVATOR.containsRecipe(type);
    }

    @Override
    public boolean canDrawGas(EnumFacing side, Gas type) {
        return configComponent.getOutput(TransmissionType.GAS, side, facing).hasSlot(1) && outputTank.canDraw(type);
    }

    @Nonnull
    @Override
    public GasTankInfo[] getTankInfo() {
        return new GasTankInfo[]{inputTank, outputTank};
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, EnumFacing side) {
        if (isCapabilityDisabled(capability, side)) {
            return false;
        }
        return capability == Capabilities.GAS_HANDLER_CAPABILITY || capability == Capabilities.CONFIG_CARD_CAPABILITY || super.hasCapability(capability, side);
    }

    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, EnumFacing side) {
        if (isCapabilityDisabled(capability, side)) {
            return null;
        } else if (capability == Capabilities.GAS_HANDLER_CAPABILITY) {
            return Capabilities.GAS_HANDLER_CAPABILITY.cast(this);
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
    public Object[] getTanks() {
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
    public boolean isItemValidForSlot(int slot, @Nonnull ItemStack stack) {
        return stack.getItem() instanceof IGasItem;
    }

    @Nonnull
    @Override
    public int[] getSlotsForFace(@Nonnull EnumFacing side) {
        return configComponent.getOutput(TransmissionType.ITEM, side, facing).availableSlots;
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
        return false;
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

    public void MultipleActions(SolarNeutronRecipe recipe) {
        MultipleActions(recipe, ticksRequired);
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
    public boolean upgrade(BaseTier upgradeTier) {
        if (upgradeTier.ordinal() != tier.ordinal() + 1) {
            return false;
        }
        if (upgradeTier == BaseTier.CREATIVE) {
            return false;
        }
        tier = MachineTier.values()[upgradeTier.ordinal()];
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
    @Optional.Method(modid = "mekanismmultiblockmachine")
    public boolean largeMachineUpgrade(EntityPlayer player) {
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
        isUpgrade = false;
        if (world.getTileEntity(getPos()) instanceof IBoundingBlock block) {
            block.onBreak();
        } else {
            world.setBlockToAir(getPos());
        }
        world.setBlockState(getPos(), MultiblockMachineBlocks.LargeSolarNeutronActivator.getDefaultState(), 3);
        if (world.getTileEntity(getPos()) instanceof TileEntityLargeSolarNeutronActivator tile){
            tile.onPlace();
            //Basic
            tile.facing = facing;
            tile.clientFacing = clientFacing;
            tile.ticker = ticker;
            tile.redstone = redstone;
            tile.redstoneLastTick = redstoneLastTick;
            tile.doAutoSync = doAutoSync;

            //Machine
            tile.setActive(isActive);
            tile.setControlType(getControlType());
            tile.upgradeComponent.readFrom(upgradeComponent);
            tile.upgradeComponent.setUpgradeSlot(upgradeComponent.getUpgradeSlot());
            tile.upgradeComponent.setSupported(Upgrade.THREAD);//升级完后需要添加支持线程升级
            tile.securityComponent.readFrom(securityComponent);
            for (int i = 0; i < inventory.size(); i++) {
                tile.inventory.set(i, inventory.get(i));
            }
            tile.inputTank.setGas(inputTank.getGas());
            tile.outputTank.setGas(outputTank.getGas());
            tile.upgradeComponent.getSupportedTypes().forEach(tile::recalculateUpgradables);
            tile.markNoUpdateSync();
            Mekanism.packetHandler.sendUpdatePacket(tile);
            markNoUpdateSync();
            return true;

        }
        return false;
    }

    @Override
    public boolean shouldDumpRadiation(){
        return isUpgrade;
    }

}
