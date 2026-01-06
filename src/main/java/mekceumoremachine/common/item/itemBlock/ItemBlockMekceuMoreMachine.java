package mekceumoremachine.common.item.itemBlock;

import mekanism.api.EnumColor;
import mekanism.api.energy.IEnergizedItem;
import mekanism.client.MekKeyHandler;
import mekanism.client.MekanismClient;
import mekanism.client.MekanismKeyHandler;
import mekanism.common.Upgrade;
import mekanism.common.base.ISustainedInventory;
import mekanism.common.base.ISustainedTank;
import mekanism.common.base.IUpgradeTile;
import mekanism.common.security.ISecurityItem;
import mekanism.common.util.ItemDataUtils;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.SecurityUtils;
import mekceumoremachine.common.item.interfaces.IItemTipName;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.List;

public class ItemBlockMekceuMoreMachine extends ItemBlock{

    public ItemBlockMekceuMoreMachine(Block block) {
        super(block);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(@Nonnull ItemStack itemstack, World world, @Nonnull List<String> list, @Nonnull ITooltipFlag flag) {
        if (!MekKeyHandler.getIsKeyPressed(MekanismKeyHandler.sneakKey)) {
            list.add(LangUtils.localize("tooltip.hold") + " " + EnumColor.INDIGO + GameSettings.getKeyDisplayString(MekanismKeyHandler.sneakKey.getKeyCode()) +
                    EnumColor.GREY + " " + LangUtils.localize("tooltip.forDetails") + ".");
            list.add(LangUtils.localize("tooltip.hold") + " " + EnumColor.AQUA + GameSettings.getKeyDisplayString(MekanismKeyHandler.sneakKey.getKeyCode()) +
                    EnumColor.GREY + " " + LangUtils.localize("tooltip.and") + " " + EnumColor.AQUA +
                    GameSettings.getKeyDisplayString(MekanismKeyHandler.handModeSwitchKey.getKeyCode()) + EnumColor.GREY + " " + LangUtils.localize("tooltip.forDesc") + ".");
        } else if (!MekKeyHandler.getIsKeyPressed(MekanismKeyHandler.handModeSwitchKey)) {
            if (itemstack.getItem() instanceof ISecurityItem iSecurityItem) {
                if (iSecurityItem.hasSecurity(itemstack)) {
                    list.add(SecurityUtils.getOwnerDisplay(Minecraft.getMinecraft().player, MekanismClient.clientUUIDMap.get(iSecurityItem.getOwnerUUID(itemstack))));
                    list.add(EnumColor.GREY + LangUtils.localize("gui.security") + ": " + SecurityUtils.getSecurityDisplay(itemstack, Side.CLIENT));
                    if (SecurityUtils.isOverridden(itemstack, Side.CLIENT)) {
                        list.add(EnumColor.RED + "(" + LangUtils.localize("gui.overridden") + ")");
                    }
                }
            }
            if (itemstack.getItem() instanceof IEnergizedItem energizedItem && itemstack.getCount() <= 1) {
                list.add(EnumColor.BRIGHT_GREEN + LangUtils.localize("tooltip.storedEnergy") + ": " + EnumColor.GREY + MekanismUtils.getEnergyDisplay(energizedItem.getEnergy(itemstack), energizedItem.getMaxEnergy(itemstack)));
            }
            if (itemstack.getItem() instanceof ISustainedTank tank && itemstack.getCount() <= 1) {
                FluidStack fluidStack = tank.getFluidStack(itemstack);
                if (fluidStack != null && itemstack.getCount() <= 1) {
                    list.add(EnumColor.PINK + LangUtils.localizeFluidStack(fluidStack) + ": " + EnumColor.GREY + tank.getFluidStack(itemstack).amount + "mB");
                }
            }
            if (itemstack.getItem() instanceof ISustainedInventory inventory) {
                list.add(EnumColor.AQUA + LangUtils.localize("tooltip.inventory") + ": " + EnumColor.GREY + LangUtils.transYesNo(inventory.getInventory(itemstack) != null && inventory.getInventory(itemstack).tagCount() != 0));
            }
            if (ItemDataUtils.hasData(itemstack, "upgrades")) {
                Upgrade.buildMap(ItemDataUtils.getDataMap(itemstack)).forEach((key, value) -> list.add(key.getColor() + "- " + key.getName() + (key.canMultiply() ? ": " + EnumColor.GREY + "x" + value : "")));
            }
        } else {
            if (itemstack.getItem() instanceof IItemTipName machine) {
                String getDescription = LangUtils.localize("tooltip." + machine.getItemName());
                list.addAll(MekanismUtils.splitTooltip(getDescription, itemstack));
            }
        }
    }
}
