package mekceumoremachine.client.gui;

import java.util.Arrays;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.gauge.GuiEnergyGauge;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.IProgressInfoHandler;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekceumoremachine.common.inventory.container.ContainerVoidMineralGenerator;
import mekceumoremachine.common.tile.machine.TileEntityVoidMineralGenerator;
import mekceumoremachine.common.util.VoidMineralGeneratorUitls;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;

public class GuiVoidMineralGenerator extends GuiConfigurableTile<TileEntityVoidMineralGenerator, ContainerVoidMineralGenerator> {

    private int delay;
    private int supportedIndex;

    public GuiVoidMineralGenerator(InventoryPlayer inventory, TileEntityVoidMineralGenerator tile) {
        super(tile, new ContainerVoidMineralGenerator(inventory, tile));
        xSize += 80;
        ySize += 102;
        dynamicSlots = true;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        addButton(new GuiEnergyTab(this, () -> Arrays.asList(
              new TextComponentString(LangUtils.localize("gui.using") + ": " + MekanismUtils.getEnergyDisplay(tileEntity.energyPerTick) + "/t"),
              new TextComponentString(LangUtils.localize("gui.needed") + ": " + MekanismUtils.getEnergyDisplay(tileEntity.getMaxEnergy() - tileEntity.getEnergy())))));
        addButton(new GuiEnergyGauge(this, tileEntity, GuiEnergyGauge.Type.MEDIUM, 6, 78));
        addButton(new GuiProgress(new IProgressInfoHandler() {
            @Override
            public double getProgress(){
                return tileEntity.getScaledProgress();
            }
            //这里不需走显示配方的字体
            @Override
            public boolean isGuiInJei(){
                return true;
            }
        }, ProgressType.SMALL_RIGHT, this, 44, 103));
        addButton(new GuiInnerScreen(this, 176, 185, 72, 76));
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (!VoidMineralGeneratorUitls.getCanOre().isEmpty()) {
            if (delay < 40) {
                delay++;
            } else {
                delay = 0;
                supportedIndex = ++supportedIndex % VoidMineralGeneratorUitls.getCanOre().size();
            }
        }
    }

    @Override
    protected void drawForegroundText(int mouseX, int mouseY) {
        drawTitleText(new TextComponentString(tileEntity.getName()), 4);
        renderInventoryText(8, ySize - 94, getXSize());
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
        fontRenderer.drawString(LangUtils.localize("gui.state") + ":" + runningType, 179, 188, screenTextColor());
        if (!VoidMineralGeneratorUitls.getCanOre().isEmpty()) {
            fontRenderer.drawString(LangUtils.localize("gui.canOre") + ": ", 179, 197, screenTextColor());
            fontRenderer.drawString(String.valueOf(VoidMineralGeneratorUitls.getCanOre().size()), 179, 206, screenTextColor());
            ItemStack[] supported = VoidMineralGeneratorUitls.getCanOre().toArray(new ItemStack[0]);
            if (supported.length > supportedIndex) {
                GlStateManager.pushMatrix();
                MekanismRenderer.resetColor();
                renderItem(supported[supportedIndex], 231, 244);
                GlStateManager.popMatrix();
                if (xAxis >= 231 && xAxis <= 249 && yAxis >= 244 && yAxis <= 262) {
                    displayTooltip(supported[supportedIndex].getDisplayName(), xAxis, yAxis);
                }
            }
        }
        super.drawForegroundText(mouseX, mouseY);
    }
}
