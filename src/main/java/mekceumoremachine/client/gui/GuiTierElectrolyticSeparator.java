package mekceumoremachine.client.gui;

import java.util.Arrays;
import mekanism.api.TileNetworkList;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.GuiElement;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import mekanism.client.gui.element.button.GuiGasMode;
import mekanism.client.gui.element.gauge.GuiFluidGauge;
import mekanism.client.gui.element.gauge.GuiGasGauge;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.client.recipe_viewer.type.RecipeViewerRecipeType;
import mekanism.client.sound.SoundHandler;
import mekanism.common.Mekanism;
import mekanism.common.network.PacketTileEntity.TileEntityMessage;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekceumoremachine.common.inventory.container.ContainerTierElectrolyticSeparator;
import mekceumoremachine.common.tile.machine.TileEntityTierElectrolyticSeparator;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.text.TextComponentString;

public class GuiTierElectrolyticSeparator extends GuiConfigurableTile<TileEntityTierElectrolyticSeparator, ContainerTierElectrolyticSeparator> {

    private GuiElement fluidGauge;

    public GuiTierElectrolyticSeparator(InventoryPlayer inventory, TileEntityTierElectrolyticSeparator tile) {
        super(tile, new ContainerTierElectrolyticSeparator(inventory, tile));
        dynamicSlots = true;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        addButton(new GuiEnergyTab(this, () -> Arrays.asList(
              new TextComponentString(LangUtils.localize("gui.using") + ": " + MekanismUtils.getEnergyDisplay(tileEntity.clientEnergyUsed) + "/t"),
              new TextComponentString(LangUtils.localize("gui.needed") + ": " + MekanismUtils.getEnergyDisplay(tileEntity.getNeedEnergy())))));
        fluidGauge = addButton(new GuiFluidGauge(this, tileEntity.fluidTank, GuiFluidGauge.Type.STANDARD, 5, 10)
              .withColor(GuiFluidGauge.GaugeColor.RED));
        addButton(new GuiGasGauge(this, tileEntity.leftTank, GuiGasGauge.Type.SMALL, 58, 18).withColor(GuiGasGauge.GaugeColor.BLUE));
        addButton(new GuiGasGauge(this, tileEntity.rightTank, GuiGasGauge.Type.SMALL, 100, 18).withColor(GuiGasGauge.GaugeColor.AQUA));
        addButton(new GuiVerticalPowerBar(this, tileEntity, 164, 15));
        addButton(new GuiProgress(() -> tileEntity.getActive() ? 1 : 0, ProgressType.BI, this, 80, 30)
              .recipeViewerCategories(RecipeViewerRecipeType.SEPARATING));
        addButton(new GuiGasMode(this, 7, 72, false, () -> tileEntity.dumpLeft, () -> sendModePacket((byte) 0)));
        addButton(new GuiGasMode(this, 159, 72, true, () -> tileEntity.dumpRight, () -> sendModePacket((byte) 1)));
    }

    @Override
    protected void drawForegroundText(int mouseX, int mouseY) {
        drawTitleTextWithOffset(new TextComponentString(tileEntity.getName()),
              fluidGauge == null ? 4 : fluidGauge.getRelativeRight(), 4, getXSize());
        super.drawForegroundText(mouseX, mouseY);
    }

    private void sendModePacket(byte tank) {
        Mekanism.packetHandler.sendToServer(new TileEntityMessage(tileEntity, TileNetworkList.withContents(tank)));
        SoundHandler.playSound(SoundEvents.UI_BUTTON_CLICK);
    }
}
