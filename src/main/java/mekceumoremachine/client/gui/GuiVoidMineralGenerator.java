package mekceumoremachine.client.gui;

import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.element.*;
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
import mekceumoremachine.common.util.VoidMineralGeneratorUitls;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class GuiVoidMineralGenerator extends GuiMekanismTile<TileEntityVoidMineralGenerator> {

    private int delay;
    private int supportedIndex;

    public GuiVoidMineralGenerator(InventoryPlayer inventory, TileEntityVoidMineralGenerator tile) {
        super(tile, new ContainerVoidMineralGenerator(inventory, tile));
        ResourceLocation resource = getGuiLocation();
        addGuiElement(new GuiSecurityTab(this, tileEntity, resource, 80, 0));
        addGuiElement(new GuiRedstoneControl(this, tileEntity, resource, 80, 0));
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
        addGuiElement(new GuiPlayerSlot(this, resource, 7, 185));
        addGuiElement(new GuiInnerScreen(this, resource, 176, 185, 72, 76));
        xSize += 80;
        ySize += 102;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (!VoidMineralGeneratorUitls.getCanOre().isEmpty()) {
            if (delay < 30) {
                delay++;
            } else {
                delay = 0;
                supportedIndex = ++supportedIndex % VoidMineralGeneratorUitls.getCanOre().size();
            }
        }
    }


    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(tileEntity.getName(), (xSize / 2) - (fontRenderer.getStringWidth(tileEntity.getName()) / 2), 4, 0x404040);
        fontRenderer.drawString(LangUtils.localize("container.inventory"), 8, (ySize - 96) + 2, 0x404040);
        String runningType;
        if (tileEntity.energyPerTick > tileEntity.getEnergy()) {
            runningType = LangUtils.localize("mekanism.gui.digitalMiner.lowPower");
        } else if (tileEntity.getActive()) {
            runningType = LangUtils.localize("gui.digitalMiner.running");
        } else {
            runningType = LangUtils.localize("gui.idle");
        }
        int xAxis = mouseX - guiLeft;
        int yAxis = mouseY - guiTop;
        fontRenderer.drawString(LangUtils.localize("gui.state") + ":" + runningType, 179, 188, 0xFF3CFE9A);
        if (!VoidMineralGeneratorUitls.getCanOre().isEmpty()) {
            fontRenderer.drawString(LangUtils.localize("gui.canOre") + ": ", 179, 197, 0xFF3CFE9A);
            fontRenderer.drawString(String.valueOf(VoidMineralGeneratorUitls.getCanOre().size()), 179, 206, 0xFF3CFE9A);
            ItemStack[] supported = VoidMineralGeneratorUitls.getCanOre().toArray(new ItemStack[0]);
            if (supported.length > supportedIndex) {
                renderItem(supported[supportedIndex], 231, 244);
                if (xAxis >= 231 && xAxis <= 231 + 18 && yAxis >= 244 && yAxis <= 244 + 18) {
                    displayTooltip(supported[supportedIndex].getDisplayName(), xAxis, yAxis);
                }
            }
        }
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
    }
}
