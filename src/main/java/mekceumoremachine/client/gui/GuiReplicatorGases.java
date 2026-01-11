package mekceumoremachine.client.gui;

import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.IJeiNoShowRecipe;
import mekanism.client.gui.element.GuiEnergyInfo;
import mekanism.client.gui.element.GuiPlayerSlot;
import mekanism.client.gui.element.GuiProgress;
import mekanism.client.gui.element.GuiRedstoneControl;
import mekanism.client.gui.element.gauge.GuiEnergyGauge;
import mekanism.client.gui.element.gauge.GuiGasGauge;
import mekanism.client.gui.element.gauge.GuiGauge;
import mekanism.client.gui.element.slot.GuiEnergySlot;
import mekanism.client.gui.element.tab.GuiSecurityTab;
import mekanism.client.gui.element.tab.GuiSideConfigurationTab;
import mekanism.client.gui.element.tab.GuiUpgradeTab;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekceumoremachine.common.inventory.container.ContainerReplicatorGases;
import mekceumoremachine.common.tile.machine.replicator.TileEntityReplicatorGases;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

import java.util.Arrays;

public class GuiReplicatorGases extends GuiMekanismTile<TileEntityReplicatorGases> implements IJeiNoShowRecipe {

    public GuiReplicatorGases(InventoryPlayer inventory, TileEntityReplicatorGases tile) {
        super(tile, new ContainerReplicatorGases(inventory, tile));
        ResourceLocation resource = getGuiLocation();
        addGuiElement(new GuiSecurityTab(this, tileEntity, resource));
        addGuiElement(new GuiRedstoneControl(this, tileEntity, resource));
        addGuiElement(new GuiUpgradeTab(this, tileEntity, resource));
        addGuiElement(new GuiSideConfigurationTab(this, tileEntity, resource));
        addGuiElement(new GuiEnergyGauge(() -> tileEntity, GuiEnergyGauge.Type.SMALL, this, resource, 154, 43));
        addGuiElement(new GuiEnergyInfo(() -> {
            double extra = tileEntity.getRecipe() != null ? tileEntity.getRecipe().extraEnergy : 0;
            String multiplier = MekanismUtils.getEnergyDisplay(MekanismUtils.getEnergyPerTick(tileEntity, tileEntity.BASE_ENERGY_PER_TICK + extra));
            return Arrays.asList(LangUtils.localize("gui.using") + ": " + multiplier + "/t", LangUtils.localize("gui.needed") + ": " + MekanismUtils.getEnergyDisplay(tileEntity.getNeedEnergy()));
        }, this, resource));
        addGuiElement(new GuiGasGauge(() -> tileEntity.inputTank, GuiGauge.Type.STANDARD, this, resource, 5, 13).withColor(GuiGauge.TypeColor.RED));
        addGuiElement(new GuiGasGauge(() -> tileEntity.uuTank, GuiGauge.Type.STANDARD, this, resource, 26, 13).withColor(GuiGauge.TypeColor.ORANGE));
        addGuiElement(new GuiGasGauge(() -> tileEntity.outputTank, GuiGauge.Type.STANDARD, this, resource, 133, 13).withColor(GuiGauge.TypeColor.BLUE));
        addGuiElement(new GuiEnergySlot(this, resource, 154, 13, tileEntity));
        addGuiElement(new GuiProgress(new GuiProgress.IProgressInfoHandler() {
            @Override
            public double getProgress() {
                return tileEntity.getScaledProgress();
            }
        }, GuiProgress.ProgressBar.LARGE_RIGHT, this, resource, 62, 38));
        addGuiElement(new GuiPlayerSlot(this, resource));
    }


    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(tileEntity.getName(), (xSize / 2) - (fontRenderer.getStringWidth(tileEntity.getName()) / 2), 6, 0x404040);
        fontRenderer.drawString(LangUtils.localize("container.inventory"), 8, (ySize - 96) + 4, 0x404040);
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
    }


}
