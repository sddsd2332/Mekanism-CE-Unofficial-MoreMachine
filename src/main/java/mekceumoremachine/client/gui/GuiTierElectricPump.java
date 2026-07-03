package mekceumoremachine.client.gui;

import java.util.Arrays;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import mekanism.client.gui.element.gauge.GuiFluidGauge;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekceumoremachine.common.inventory.container.ContainerTierElectricPump;
import mekceumoremachine.common.tile.machine.TileEntityTierElectricPump;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.TextComponentString;

public class GuiTierElectricPump extends GuiMekanismTile<TileEntityTierElectricPump, ContainerTierElectricPump> {

    public GuiTierElectricPump(InventoryPlayer inventory, TileEntityTierElectricPump tile) {
        super(tile, new ContainerTierElectricPump(inventory, tile));
        dynamicSlots = true;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        addButton(new GuiVerticalPowerBar(this, tileEntity, 164, 15));
        addButton(new GuiFluidGauge(this, tileEntity.fluidTank, GuiFluidGauge.Type.STANDARD, 6, 13));
        addButton(new GuiEnergyTab(this, () -> Arrays.asList(
              new TextComponentString(LangUtils.localize("gui.using") + ": " + MekanismUtils.getEnergyDisplay(tileEntity.energyPerTick) + "/t"),
              new TextComponentString(LangUtils.localize("gui.needed") + ": " + MekanismUtils.getEnergyDisplay(tileEntity.getMaxEnergy() - tileEntity.getEnergy())))));
        addButton(new GuiInnerScreen(this, 48, 23, 80, 41));
    }

    @Override
    protected void drawForegroundText(int mouseX, int mouseY) {
        drawTitleText(new TextComponentString(tileEntity.getName()), 4);
        renderInventoryText(8, ySize - 92, getXSize());
        fontRenderer.drawString(MekanismUtils.getEnergyDisplay(tileEntity.getEnergy(), tileEntity.getMaxEnergy()), 51, 26, screenTextColor());
        String text = tileEntity.fluidTank.getFluid() != null ? LangUtils.localizeFluidStack(tileEntity.fluidTank.getFluid()) + ": " + tileEntity.fluidTank.getFluid().amount
              : LangUtils.localize("gui.noFluid");
        drawTextScaledBound(new TextComponentString(text), 51, 35, screenTextColor(), 74);
        super.drawForegroundText(mouseX, mouseY);
    }
}
