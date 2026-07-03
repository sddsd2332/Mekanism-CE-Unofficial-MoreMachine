package mekceumoremachine.common.tile.machine;

import io.netty.buffer.ByteBuf;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IConfigurable;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.api.TileNetworkList;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTankInfo;
import mekanism.common.Mekanism;
import mekanism.common.base.IActiveState;
import mekanism.common.base.IComparatorSupport;
import mekanism.common.base.ISustainedData;
import mekanism.common.base.ITankManager;
import mekanism.common.capabilities.holder.gas.GasTankHelper;
import mekanism.common.capabilities.holder.gas.IGasTankHolder;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.TileEntityRadioactiveWasteBarrel;
import mekanism.common.tile.prefab.TileEntityContainerBlock;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.util.GasUtils;
import mekanism.common.util.ItemDataUtils;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.TileUtils;
import mekceumoremachine.common.capability.ResizableGasTank;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.interfaces.ITierMachine;
import mekceumoremachine.common.upgrade.FirstRadioactiveWasteBarrelUpgradeData;
import mekceumoremachine.common.upgrade.LargeMachineUpgradeDataApplier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nonnull;
import java.util.Collections;

public class TileEntityTierRadioactiveWasteBarrel extends TileEntityContainerBlock implements ISustainedData, ITankManager,
      IComparatorSupport, IActiveState, IConfigurable, ITierMachine<MachineTier> {

    public static final int MAX_GAS = 512000;

    private long lastProcessTick;
    public ResizableGasTank gasTank;
    private int processTicks;
    public boolean isActive;
    public boolean clientActive;
    public MachineTier tier = MachineTier.BASIC;

    public TileEntityTierRadioactiveWasteBarrel() {
        super("TierRadioactiveWasteBarrel");
        initializeInventorySlots();
    }

    @Override
    protected IGasTankHolder getInitialGasTanks(IContentsListener listener) {
        GasTankHelper builder = createGasTankHelper();
        builder.addTank(getOrCreateGasTank(listener), RelativeSide.TOP, RelativeSide.BOTTOM);
        return builder.build();
    }

    private ResizableGasTank getOrCreateGasTank(IContentsListener listener) {
        if (gasTank == null) {
            gasTank = ResizableGasTank.input(getTankCapacity(), gas -> gas != null && gas.isRadiation(), listener);
        }
        updateTankCapacity();
        return gasTank;
    }

    private int getTankCapacity() {
        return MAX_GAS * tier.processes;
    }

    private void updateTankCapacity() {
        if (gasTank != null) {
            gasTank.setMaxGas(getTankCapacity());
        }
    }

    @Override
    public void onUpdateServer() {
        super.onUpdateServer();
        if (getWorld().getTotalWorldTime() > lastProcessTick) {
            lastProcessTick = getWorld().getTotalWorldTime();
            GasStack stored = gasTank.getGas();
            if (stored != null && stored.getGas() != null && stored.getGas().isRadiation() && ++processTicks >= 20) {
                processTicks = 0;
                gasTank.extract(tier.processes * 10, Action.EXECUTE, AutomationType.INTERNAL);
            }
            if (getActive()) {
                GasUtils.emit(Collections.singleton(EnumFacing.DOWN), gasTank, this, getDownOutputLimit());
            }
        }
    }

    private int getDownOutputLimit() {
        TileEntity below = MekanismUtils.getTileEntity(world, pos.down());
        if (below instanceof TileEntityTierRadioactiveWasteBarrel) {
            TileEntityTierRadioactiveWasteBarrel barrel = (TileEntityTierRadioactiveWasteBarrel) below;
            return Math.min(barrel.gasTank.getNeeded(), gasTank.getCapacity());
        } else if (below instanceof TileEntityRadioactiveWasteBarrel) {
            TileEntityRadioactiveWasteBarrel barrel = (TileEntityRadioactiveWasteBarrel) below;
            return Math.min(barrel.gasTank.getNeeded(), gasTank.getCapacity());
        }
        return gasTank.getCapacity();
    }

    @Override
    public void writeSustainedData(ItemStack itemStack) {
        writeSustainedGasTanks(itemStack);
        ItemDataUtils.setLegacyGas(itemStack, "gasStored", gasTank.getGas());
    }

    @Override
    public void readSustainedData(ItemStack itemStack) {
        if (!readSustainedGasTanks(itemStack)) {
            gasTank.setStackUnchecked(ItemDataUtils.getLegacyGas(itemStack, "gasStored"));
        }
        sanitizeAndClampGasTank();
    }

    private void sanitizeAndClampGasTank() {
        updateTankCapacity();
        GasStack stored = gasTank.getGas();
        if (stored != null && (stored.amount <= 0 || stored.getGas() == null || !stored.getGas().isRadiation())) {
            gasTank.setEmpty();
        } else if (stored != null) {
            gasTank.setStackSize(stored.amount, Action.EXECUTE);
        }
    }

    @Override
    public Object[] getManagedTanks() {
        return new Object[]{gasTank};
    }

    @Override
    public EnumActionResult onSneakRightClick(EntityPlayer player, EnumFacing side) {
        if (!isRemote()) {
            setActive(!getActive());
            world.playSound(null, getPos().getX(), getPos().getY(), getPos().getZ(), SoundEvents.UI_BUTTON_CLICK, SoundCategory.BLOCKS, 0.3F, 1);
        }
        return EnumActionResult.SUCCESS;
    }

    @Override
    public EnumActionResult onRightClick(EntityPlayer player, EnumFacing side) {
        return EnumActionResult.PASS;
    }

    @Nonnull
    @Override
    public GasTankInfo[] getTankInfo() {
        return new GasTankInfo[]{gasTank};
    }

    @Override
    public void handlePacketData(ByteBuf dataStream) {
        super.handlePacketData(dataStream);
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            MachineTier prevTier = tier;
            tier = MachineTier.byIndex(dataStream.readInt());
            updateTankCapacity();
            clientActive = isActive = dataStream.readBoolean();
            TileUtils.readTankData(dataStream, gasTank);
            sanitizeAndClampGasTank();
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
        TileUtils.addTankData(data, gasTank);
        return data;
    }

    @Override
    protected void readCustomNBTBeforeInventory(NBTTagCompound nbtTags) {
        tier = MachineTier.byIndex(nbtTags.getInteger("tier"));
        updateTankCapacity();
    }

    @Override
    public void readCustomNBT(NBTTagCompound nbtTags) {
        super.readCustomNBT(nbtTags);
        clientActive = isActive = nbtTags.getBoolean("isActive");
        if (!hasStoredGasTanks(nbtTags) && nbtTags.hasKey("gasTank")) {
            gasTank.read(nbtTags.getCompoundTag("gasTank"));
        }
        sanitizeAndClampGasTank();
    }

    @Override
    public void writeCustomNBT(NBTTagCompound nbtTags) {
        super.writeCustomNBT(nbtTags);
        nbtTags.setInteger("tier", tier.ordinal());
        nbtTags.setBoolean("isActive", isActive);
    }

    @Override
    public boolean supportsAsync() {
        return false;
    }

    @Override
    public boolean getActive() {
        return isActive;
    }

    @Override
    public void setActive(boolean active) {
        isActive = active;
        if (clientActive != active) {
            Mekanism.packetHandler.sendUpdatePacket(this);
            clientActive = active;
        }
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
    public int getRedstoneLevel() {
        return MekanismUtils.redstoneLevelFromContents(gasTank.getStored(), gasTank.getMaxGas());
    }

    @Override
    public boolean applyTierUpgrade(BaseTier upgradeTier) {
        if (!tier.canUpgradeTo(upgradeTier)) {
            return false;
        }
        tier = MachineTier.get(upgradeTier);
        sanitizeAndClampGasTank();
        Mekanism.packetHandler.sendUpdatePacket(this);
        markNoUpdateSync();
        return true;
    }

    @Override
    public boolean parseUpgradeData(IUpgradeData upgradeData) {
        if (upgradeData instanceof FirstRadioactiveWasteBarrelUpgradeData data && data.getUpgradeTier() == tier.getBaseTier()) {
            LargeMachineUpgradeDataApplier.applyCommon(this, data, null, null);
            gasTank.setGas(data.gas == null ? null : data.gas.copy());
            sanitizeAndClampGasTank();
            LargeMachineUpgradeDataApplier.finish(this, null);
            return true;
        }
        return ITierMachine.super.parseUpgradeData(upgradeData);
    }

    @Override
    public MachineTier getTier() {
        return tier;
    }

    @Nonnull
    @Override
    public String getName() {
        return LangUtils.localize("tile.TierRadioactiveWasteBarrel." + tier.getBaseTier().getSimpleName() + ".name");
    }

    @Nonnull
    @Override
    public ITextComponent getDisplayName() {
        return new TextComponentString(getName());
    }

    @Override
    protected boolean shouldDumpRadiation() {
        return true;
    }
}
