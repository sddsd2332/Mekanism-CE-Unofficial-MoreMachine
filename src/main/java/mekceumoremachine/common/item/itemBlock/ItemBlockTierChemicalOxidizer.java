package mekceumoremachine.common.item.itemBlock;

import cofh.redstoneflux.api.IEnergyContainerItem;
import ic2.api.item.IElectricItemManager;
import ic2.api.item.ISpecialElectricItem;
import mekanism.api.energy.IEnergizedItem;
import mekanism.common.base.*;
import mekanism.common.block.states.BlockStateMachine;
import mekanism.common.capabilities.ItemCapabilityWrapper;
import mekanism.common.config.MekanismConfig;
import mekanism.common.integration.MekanismHooks;
import mekanism.common.integration.forgeenergy.ForgeEnergyItemWrapper;
import mekanism.common.integration.ic2.IC2ItemManager;
import mekanism.common.integration.redstoneflux.RFIntegration;
import mekanism.common.integration.tesla.TeslaItemWrapper;
import mekanism.common.security.ISecurityItem;
import mekanism.common.security.ISecurityTile;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.prefab.TileEntityBasicBlock;
import mekanism.common.tile.prefab.TileEntityElectricBlock;
import mekanism.common.util.ItemDataUtils;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekceumoremachine.common.block.states.BlockStateTierChemicalOxidizer.MachineType;
import mekceumoremachine.common.item.interfaces.IItemTipName;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.util.MEKCeuMoreMachineUtils;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.Optional.Interface;
import net.minecraftforge.fml.common.Optional.InterfaceList;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;

@InterfaceList({
        @Interface(iface = "cofh.redstoneflux.api.IEnergyContainerItem", modid = MekanismHooks.REDSTONEFLUX_MOD_ID),
        @Interface(iface = "ic2.api.item.ISpecialElectricItem", modid = MekanismHooks.IC2_MOD_ID)
})

public class ItemBlockTierChemicalOxidizer extends ItemBlockMekceuMoreMachine implements IEnergizedItem, ISpecialElectricItem, IEnergyContainerItem, ISustainedInventory, ISecurityItem , IItemTipName {


    public Block metaBlock;

    public ItemBlockTierChemicalOxidizer(Block block) {
        super(block);
        metaBlock = block;
        setHasSubtypes(true);
        setNoRepair();
    }

    @Override
    public int getMetadata(int i) {
        return i;
    }

    @Nonnull
    @Override
    public String getTranslationKey(ItemStack itemstack) {
        if (MachineType.get(itemstack) != null) {
            MachineType type = MachineType.get(itemstack);
            BaseTier tier = type.tier.getBaseTier();
            return "tile.TierChemicalOxidizer." + tier.getSimpleName() + ".name";
        }
        return "null";
    }

    @Nonnull
    @Override
    public String getItemStackDisplayName(@Nonnull ItemStack itemstack) {
        if (MachineType.get(itemstack) != null) {
            MachineType type = MachineType.get(itemstack);
            BaseTier tier = type.tier.getBaseTier();
            return tier.getColor() + LangUtils.localize("tile.TierChemicalOxidizer." + tier.getSimpleName() + ".name");
        }
        return super.getItemStackDisplayName(itemstack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(@Nonnull ItemStack itemstack, World world, @Nonnull List<String> list, @Nonnull ITooltipFlag flag) {
        MachineType type = MachineType.get(itemstack);
        if (type != null) {
            super.addInformation(itemstack, world, list, flag);
        }
    }


    @Override
    public boolean placeBlockAt(@Nonnull ItemStack stack, @Nonnull EntityPlayer player, World world, @Nonnull BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, @Nonnull IBlockState state) {
        if (stack.getCount() > 1 && MekanismConfig.current().mekce.StackingPlacementLimits.val()) {
            return false;
        }
        boolean place = true;
        MachineType type = MachineType.get(stack);
        if (MekanismConfig.current().general.destroyDisabledBlocks.val()) {
            if (type != null && !type.isEnabled()) {
                return false;
            }
        }
        if (place && super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, state)) {
            if (world.getTileEntity(pos) instanceof TileEntityBasicBlock tileEntity) {
                if (tileEntity instanceof ISecurityTile security) {
                    security.getSecurity().setOwnerUUID(getOwnerUUID(stack));
                    if (hasSecurity(stack)) {
                        security.getSecurity().setMode(getSecurity(stack));
                    }
                    if (getOwnerUUID(stack) == null) {
                        security.getSecurity().setOwnerUUID(player.getUniqueID());
                    }
                }
                if (tileEntity instanceof IUpgradeTile upgradeTile) {
                    if (ItemDataUtils.hasData(stack, "upgrades")) {
                        upgradeTile.getComponent().read(ItemDataUtils.getDataMap(stack));
                    }
                }
                if (tileEntity instanceof ISideConfiguration config) {
                    if (ItemDataUtils.hasData(stack, "sideDataStored")) {
                        config.getConfig().read(ItemDataUtils.getDataMap(stack));
                        config.getEjector().read(ItemDataUtils.getDataMap(stack));
                    }
                }
                if (tileEntity instanceof ISustainedData data) {
                    if (stack.getTagCompound() != null) {
                        data.readSustainedData(stack);
                    }
                }
                if (tileEntity instanceof IRedstoneControl redstoneControl) {
                    if (ItemDataUtils.hasData(stack, "controlType")) {
                        redstoneControl.setControlType(IRedstoneControl.RedstoneControl.values()[ItemDataUtils.getInt(stack, "controlType")]);
                    }
                }
                if (tileEntity instanceof ISustainedInventory inventory) {
                    inventory.setInventory(getInventory(stack));
                }
                if (tileEntity instanceof TileEntityElectricBlock tile) {
                    tile.electricityStored.set(getEnergy(stack));
                }
            }
            return true;
        }
        return false;
    }


    @Override
    public void setInventory(NBTTagList nbtTags, Object... data) {
        if (data[0] instanceof ItemStack stack) {
            ItemDataUtils.setList(stack, "Items", nbtTags);
        }
    }

    @Override
    public NBTTagList getInventory(Object... data) {
        if (data[0] instanceof ItemStack stack) {
            return ItemDataUtils.getList(stack, "Items");
        }
        return null;
    }


    @Override
    public UUID getOwnerUUID(ItemStack stack) {
        if (ItemDataUtils.hasData(stack, "ownerUUID")) {
            return UUID.fromString(ItemDataUtils.getString(stack, "ownerUUID"));
        }
        return null;
    }

    @Override
    public void setOwnerUUID(ItemStack stack, UUID owner) {
        if (owner == null) {
            ItemDataUtils.removeData(stack, "ownerUUID");
        } else {
            ItemDataUtils.setString(stack, "ownerUUID", owner.toString());
        }
    }

    @Override
    public ISecurityTile.SecurityMode getSecurity(ItemStack stack) {
        if (!MekanismConfig.current().general.allowProtection.val()) {
            return ISecurityTile.SecurityMode.PUBLIC;
        }
        return ISecurityTile.SecurityMode.values()[ItemDataUtils.getInt(stack, "security")];
    }

    @Override
    public void setSecurity(ItemStack stack, ISecurityTile.SecurityMode mode) {
        if (getOwnerUUID(stack) == null) {
            ItemDataUtils.removeData(stack, "security");
        } else {
            ItemDataUtils.setInt(stack, "security", mode.ordinal());
        }
    }

    @Override
    public boolean hasSecurity(ItemStack stack) {
        return true;
    }

    @Override
    public boolean hasOwner(ItemStack stack) {
        return hasSecurity(stack);
    }

    @Override
    public double getEnergy(ItemStack itemStack) {
        if (itemStack.getCount() > 1) {
            return 0;
        }
        if (!itemStack.hasTagCompound()) {
            return 0;
        }
        return ItemDataUtils.getDouble(itemStack, "energyStored");
    }


    @Override
    public void setEnergy(ItemStack itemStack, double amount) {
        if (itemStack.getCount() > 1) {
            return;
        }
        if (amount == 0) {
            NBTTagCompound dataMap = ItemDataUtils.getDataMap(itemStack);
            dataMap.removeTag("energyStored");
            if (dataMap.isEmpty() && itemStack.getTagCompound() != null) {
                itemStack.getTagCompound().removeTag(ItemDataUtils.DATA_ID);
            }
        } else {
            ItemDataUtils.setDouble(itemStack, "energyStored", Math.max(Math.min(amount, getMaxEnergy(itemStack)), 0));
        }
    }

    @Override
    public double getMaxEnergy(ItemStack itemStack) {
        if (itemStack.getCount() > 1) {
            return 0;
        }
        if (MachineType.get(itemStack) == null) {
            return 0;
        }
        MachineType type = MachineType.get(itemStack);
        MachineTier tier = type.tier;
        double storage = getMachineStorage() * tier.processes;
        return ItemDataUtils.hasData(itemStack, "upgrades") ? MekanismUtils.getMaxEnergy(itemStack, storage) : storage;
    }


    public double getMachineStorage() {
        return BlockStateMachine.MachineType.NUTRITIONAL_LIQUIFIER.getStorage();
    }

    @Override
    public double getMaxTransfer(ItemStack itemStack) {
        if (itemStack.getCount() > 1) {
            return 0;
        }
        return getMaxEnergy(itemStack) * 0.005;
    }

    @Override
    public boolean canReceive(ItemStack itemStack) {
        return itemStack.getCount() <= 1;
    }

    @Override
    public boolean canSend(ItemStack itemStack) {
        return false;
    }

    @Override
    @Optional.Method(modid = MekanismHooks.REDSTONEFLUX_MOD_ID)
    public int receiveEnergy(ItemStack theItem, int energy, boolean simulate) {
        if (theItem.getCount() > 1) {
            return 0;
        }
        if (canReceive(theItem)) {
            double energyNeeded = getMaxEnergy(theItem) - getEnergy(theItem);
            double toReceive = Math.min(RFIntegration.fromRF(energy), energyNeeded);
            if (!simulate) {
                setEnergy(theItem, getEnergy(theItem) + toReceive);
            }
            return RFIntegration.toRF(toReceive);
        }
        return 0;
    }

    @Override
    @Optional.Method(modid = MekanismHooks.REDSTONEFLUX_MOD_ID)
    public int extractEnergy(ItemStack theItem, int energy, boolean simulate) {
        if (theItem.getCount() > 1) {
            return 0;
        }
        if (canSend(theItem)) {
            double energyRemaining = getEnergy(theItem);
            double toSend = Math.min(RFIntegration.fromRF(energy), energyRemaining);
            if (!simulate) {
                setEnergy(theItem, getEnergy(theItem) - toSend);
            }
            return RFIntegration.toRF(toSend);
        }
        return 0;
    }

    @Override
    @Optional.Method(modid = MekanismHooks.REDSTONEFLUX_MOD_ID)
    public int getEnergyStored(ItemStack theItem) {
        if (theItem.getCount() > 1) {
            return 0;
        }
        return RFIntegration.toRF(getEnergy(theItem));
    }

    @Override
    @Optional.Method(modid = MekanismHooks.REDSTONEFLUX_MOD_ID)
    public int getMaxEnergyStored(ItemStack theItem) {
        if (theItem.getCount() > 1) {
            return 0;
        }
        return RFIntegration.toRF(getMaxEnergy(theItem));
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return getEnergy(stack) > 0;
    }


    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return 1D - (getEnergy(stack) / getMaxEnergy(stack));
    }

    @Override
    @Optional.Method(modid = MekanismHooks.IC2_MOD_ID)
    public IElectricItemManager getManager(ItemStack itemStack) {
        return IC2ItemManager.getManager(this);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt) {
        return new ItemCapabilityWrapper(stack, new TeslaItemWrapper(), new ForgeEnergyItemWrapper());
    }

    @Override
    public String getItemName() {
        return "TierChemicalOxidizer";
    }
}
