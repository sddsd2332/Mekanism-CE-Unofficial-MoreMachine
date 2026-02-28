package mekceumoremachine.client.gui;

import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.element.GuiEnergyInfo;
import mekanism.client.gui.element.GuiPlayerSlot;
import mekanism.client.gui.element.GuiProgress;
import mekanism.client.gui.element.GuiRedstoneControl;
import mekanism.client.gui.element.gauge.GuiEnergyGauge;
import mekanism.client.gui.element.slot.GuiEnergySlot;
import mekanism.client.gui.element.slot.GuiOutputSlot;
import mekanism.client.gui.element.tab.GuiSecurityTab;
import mekanism.client.gui.element.tab.GuiSideConfigurationTab;
import mekanism.client.gui.element.tab.GuiTransporterConfigTab;
import mekanism.client.gui.element.tab.GuiUpgradeTab;
import mekanism.common.util.LangUtils;
import mekceumoremachine.common.inventory.container.ContainerVoidMineralGenerator;
import mekceumoremachine.common.tile.machine.TileEntityVoidMineralGenerator;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

public class GuiVoidMineralGenerator extends GuiMekanismTile<TileEntityVoidMineralGenerator> {

    public GuiVoidMineralGenerator(InventoryPlayer inventory, TileEntityVoidMineralGenerator tile) {
        super(tile, new ContainerVoidMineralGenerator(inventory, tile));
        ResourceLocation resource = getGuiLocation();
        addGuiElement(new GuiSecurityTab(this, tileEntity, resource,80,0));
        addGuiElement(new GuiRedstoneControl(this, tileEntity, resource,80,0));
        addGuiElement(new GuiUpgradeTab(this, tileEntity, resource, 80, 0));
        addGuiElement(new GuiSideConfigurationTab(this, tileEntity, resource));
        addGuiElement(new GuiTransporterConfigTab(this, 34, tileEntity, resource));
        addGuiElement(new GuiEnergyInfo(tileEntity, this, resource));
        addGuiElement(new GuiEnergyGauge(() -> tileEntity, GuiEnergyGauge.Type.MEDIUM, this, resource, 6, 78));
        addGuiElement(new GuiEnergySlot(this, resource, 14, 56, tileEntity));
        addGuiElement(new GuiProgress(new GuiProgress.IProgressInfoHandler() {
            @Override
            public double getProgress() {
                return tileEntity.getScaledProgress();
            }
        }, GuiProgress.ProgressBar.SMALL_RIGHT, this, resource, 44, 103));
        for (int slotY = 0; slotY < 9; slotY++) {
            for (int slotX = 0; slotX < 9; slotX++) {
                addGuiElement(new GuiOutputSlot(this, resource, 80 + slotX * 18, 13 + slotY * 18, tileEntity));
            }
        }
        addGuiElement(new GuiPlayerSlot(this, resource, 47, 185));
        xSize += 80;
        ySize += 102;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(tileEntity.getName(), (xSize / 2) - (fontRenderer.getStringWidth(tileEntity.getName()) / 2), 4, 0x404040);
        fontRenderer.drawString(LangUtils.localize("container.inventory"), 47, (ySize - 96) + 2, 0x404040);
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
    }
}
