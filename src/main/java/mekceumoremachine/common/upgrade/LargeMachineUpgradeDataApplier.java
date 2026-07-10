package mekceumoremachine.common.upgrade;

import mekanism.api.inventory.IInventorySlot;
import mekanism.common.Mekanism;
import mekanism.common.base.IActiveState;
import mekanism.common.base.IRedstoneControl;
import mekanism.common.tile.component.TileComponentSecurity;
import mekanism.common.tile.component.TileComponentUpgrade;
import mekanism.common.tile.prefab.TileEntityContainerBlock;
import mekanism.common.tile.prefab.TileEntityElectricBlock;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import java.util.List;

public final class LargeMachineUpgradeDataApplier {

    private LargeMachineUpgradeDataApplier() {
    }

    public static void applyCommon(TileEntityContainerBlock tile, LargeMachineUpgradeData data, TileComponentUpgrade upgradeComponent,
          TileComponentSecurity securityComponent) {
        applyCommon(tile, data, upgradeComponent, securityComponent, true);
    }

    public static void applyCommonWithoutInventory(TileEntityContainerBlock tile, LargeMachineUpgradeData data, TileComponentUpgrade upgradeComponent,
          TileComponentSecurity securityComponent) {
        applyCommon(tile, data, upgradeComponent, securityComponent, false);
    }

    private static void applyCommon(TileEntityContainerBlock tile, LargeMachineUpgradeData data, TileComponentUpgrade upgradeComponent,
          TileComponentSecurity securityComponent, boolean applyInventory) {
        tile.facing = data.facing;
        tile.clientFacing = data.clientFacing;
        tile.ticker = data.ticker;
        tile.redstone = data.redstone;
        tile.redstoneLastTick = data.redstoneLastTick;
        tile.doAutoSync = data.doAutoSync;
        if (tile instanceof IActiveState activeState) {
            activeState.setActive(data.active);
        }
        if (tile instanceof IRedstoneControl redstoneControl) {
            redstoneControl.setControlType(data.controlType);
        }
        if (upgradeComponent != null) {
            upgradeComponent.read(data.upgradeComponentData.copy());
        }
        if (securityComponent != null) {
            securityComponent.read(data.securityComponentData.copy());
        }
        if (tile instanceof TileEntityElectricBlock electric) {
            electric.setEnergy(data.energy);
        }
        if (applyInventory) {
            applyInventory(tile, data.inventory);
        }
    }

    public static void returnUnmappedStack(TileEntityContainerBlock tile, ItemStack stack) {
        if (stack != null && !stack.isEmpty() && tile.getWorld() != null && !tile.getWorld().isRemote) {
            Block.spawnAsEntity(tile.getWorld(), tile.getPos(), LargeMachineUpgradeData.copyStack(stack));
        }
    }

    public static void finish(TileEntityContainerBlock tile, TileComponentUpgrade upgradeComponent) {
        if (upgradeComponent != null) {
            upgradeComponent.getSupportedTypes().forEach(tile::recalculateUpgradables);
        }
        tile.markNoUpdateSync();
        Mekanism.packetHandler.sendUpdatePacket(tile);
    }

    private static void applyInventory(TileEntityContainerBlock tile, ItemStack[] inventory) {
        List<IInventorySlot> targetSlots = tile.getInventorySlots(null);
        int inventorySize = Math.min(inventory.length, targetSlots.size());
        for (int i = 0; i < inventorySize; i++) {
            targetSlots.get(i).setStack(LargeMachineUpgradeData.copyStack(inventory[i]));
        }
    }
}
