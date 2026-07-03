package mekceumoremachine.client.gui;

import java.util.Arrays;
import mekanism.api.TileNetworkList;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.GuiDownArrow;
import mekanism.client.gui.element.bar.GuiHorizontalPowerBar;
import mekanism.client.gui.element.button.ToggleButton;
import mekanism.client.gui.element.gauge.GuiFluidGauge;
import mekanism.client.gui.element.gauge.GuiGasGauge;
import mekanism.client.gui.element.progress.IProgressInfoHandler;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.client.recipe_viewer.type.RecipeViewerRecipeType;
import mekanism.client.sound.SoundHandler;
import mekanism.common.Mekanism;
import mekanism.common.network.PacketTileEntity.TileEntityMessage;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekceumoremachine.common.inventory.container.ContainerTierRotaryCondensentrator;
import mekceumoremachine.common.tile.machine.TileEntityTierRotaryCondensentrator;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.text.TextComponentString;

public class GuiTierRotaryCondensentrator extends GuiConfigurableTile<TileEntityTierRotaryCondensentrator, ContainerTierRotaryCondensentrator> {

    public GuiTierRotaryCondensentrator(InventoryPlayer inventory, TileEntityTierRotaryCondensentrator tile) {
        super(tile, new ContainerTierRotaryCondensentrator(inventory, tile));
        dynamicSlots = true;
        titleLabelY = 4;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        addButton(new GuiDownArrow(this, 159, 44));
        addButton(new GuiHorizontalPowerBar(this, tileEntity, 113, 74));
        addButton(new GuiEnergyTab(this, () -> Arrays.asList(
              new TextComponentString(LangUtils.localize("gui.using") + ": " + MekanismUtils.getEnergyDisplay(tileEntity.clientEnergyUsed) + "/t"),
              new TextComponentString(LangUtils.localize("gui.needed") + ": " + MekanismUtils.getEnergyDisplay(tileEntity.getNeedEnergy())))));
        addButton(new GuiGasGauge(this, tileEntity.gasTank, GuiGasGauge.Type.STANDARD, 25, 13).withColor(GuiGasGauge.GaugeColor.RED));
        addButton(new GuiFluidGauge(this, tileEntity.fluidTank, GuiFluidGauge.Type.STANDARD, 133, 13).withColor(GuiFluidGauge.GaugeColor.RED));
        addButton(new GuiProgress(new IProgressInfoHandler.IBooleanProgressInfoHandler() {
            @Override
            public boolean fillProgressBar() {
                return tileEntity.getActive();
            }

            @Override
            public boolean isActive() {
                return tileEntity.mode;
            }
        }, ProgressType.LARGE_RIGHT, this, 64, 39)
              .recipeViewerCategories(RecipeViewerRecipeType.CONDENSENTRATING));
        addButton(new GuiProgress(new IProgressInfoHandler.IBooleanProgressInfoHandler() {
            @Override
            public boolean fillProgressBar() {
                return tileEntity.getActive();
            }

            @Override
            public boolean isActive() {
                return !tileEntity.mode;
            }
        }, ProgressType.LARGE_LEFT, this, 64, 39)
              .recipeViewerCategories(RecipeViewerRecipeType.DECONDENSENTRATING));
        addButton(new ToggleButton(this, 4, 4, () -> !tileEntity.mode, this::sendModePacket,
              new TextComponentString(LangUtils.localize("gui.rotaryCondensentrator.toggleOperation")),
              new TextComponentString(LangUtils.localize("gui.rotaryCondensentrator.toggleOperation"))));
    }

    @Override
    protected void drawForegroundText(int mouseX, int mouseY) {
        drawTitleText(new TextComponentString(tileEntity.getName()), titleLabelY);
        fontRenderer.drawString(tileEntity.mode ? LangUtils.localize("gui.condensentrating") : LangUtils.localize("gui.decondensentrating"), 6, ySize - 92, titleTextColor());
        super.drawForegroundText(mouseX, mouseY);
    }

    private void sendModePacket() {
        Mekanism.packetHandler.sendToServer(new TileEntityMessage(tileEntity, TileNetworkList.withContents(0)));
        SoundHandler.playSound(SoundEvents.UI_BUTTON_CLICK);
    }
}
