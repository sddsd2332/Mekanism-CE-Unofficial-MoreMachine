package mekceumoremachine.common.upgrade;

import mekanism.api.inventory.IInventorySlot;
import mekanism.common.base.IActiveState;
import mekanism.common.base.IRedstoneControl;
import mekanism.common.base.IUpgradeTile;
import mekanism.common.security.ISecurityTile;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.component.TileComponentSecurity;
import mekanism.common.tile.prefab.TileEntityContainerBlock;
import mekanism.common.tile.prefab.TileEntityElectricBlock;
import mekanism.common.upgrade.TierUpgradeData;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nonnull;
import java.util.List;

public class LargeMachineUpgradeData extends TierUpgradeData {

    public final EnumFacing facing;
    public final EnumFacing clientFacing;
    public final int ticker;
    public final boolean redstone;
    public final boolean redstoneLastTick;
    public final boolean doAutoSync;
    public final double energy;
    public final boolean active;
    public final IRedstoneControl.RedstoneControl controlType;
    public final NBTTagCompound upgradeComponentData;
    public final NBTTagCompound securityComponentData;
    public final ItemStack[] inventory;

    public LargeMachineUpgradeData(@Nonnull BaseTier upgradeTier, @Nonnull TileEntityContainerBlock source) {
        super(upgradeTier);
        facing = source.facing;
        clientFacing = source.clientFacing;
        ticker = source.ticker;
        redstone = source.redstone;
        redstoneLastTick = source.redstoneLastTick;
        doAutoSync = source.doAutoSync;
        energy = source instanceof TileEntityElectricBlock electric ? electric.getEnergy() : 0;
        active = source instanceof IActiveState activeState && activeState.getActive();
        controlType = source instanceof IRedstoneControl redstoneControl ? redstoneControl.getControlType() : IRedstoneControl.RedstoneControl.DISABLED;
        upgradeComponentData = writeUpgradeComponent(source);
        securityComponentData = writeSecurityComponent(source);
        inventory = copyInventory(source);
    }

    private static NBTTagCompound writeUpgradeComponent(TileEntityContainerBlock source) {
        NBTTagCompound data = new NBTTagCompound();
        if (source instanceof IUpgradeTile upgradeTile) {
            upgradeTile.writeUpgrades(data);
        }
        return data;
    }

    private static NBTTagCompound writeSecurityComponent(TileEntityContainerBlock source) {
        NBTTagCompound data = new NBTTagCompound();
        if (source instanceof ISecurityTile securityTile) {
            TileComponentSecurity component = securityTile.getSecurity();
            if (component != null) {
                component.write(data);
            }
        }
        return data;
    }

    private static ItemStack[] copyInventory(TileEntityContainerBlock source) {
        List<IInventorySlot> slots = source.getInventorySlots(null);
        ItemStack[] stacks = new ItemStack[slots.size()];
        for (int i = 0; i < stacks.length; i++) {
            stacks[i] = copyStack(slots.get(i).getStack());
        }
        return stacks;
    }

    public static ItemStack copyStack(ItemStack stack) {
        return stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }
}
