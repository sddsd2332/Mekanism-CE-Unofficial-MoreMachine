package mekceumoremachine.client.gui;

import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.IJeiNoShowRecipe;
import mekanism.client.gui.element.*;
import mekanism.client.gui.element.gauge.GuiGasGauge;
import mekanism.client.gui.element.gauge.GuiGauge;
import mekanism.client.gui.element.slot.GuiEnergySlot;
import mekanism.client.gui.element.slot.GuiInputSlot;
import mekanism.client.gui.element.slot.GuiOutputSlot;
import mekanism.client.gui.element.tab.GuiSecurityTab;
import mekanism.client.gui.element.tab.GuiSideConfigurationTab;
import mekanism.client.gui.element.tab.GuiTransporterConfigTab;
import mekanism.client.gui.element.tab.GuiUpgradeTab;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekceumoremachine.common.inventory.container.ContainerReplicatorItemStack;
import mekceumoremachine.common.tile.machine.replicator.TileEntityReplicatorItemStack;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Arrays;

@SideOnly(Side.CLIENT)
public class GuiReplicatorItemStack extends GuiMekanismTile<TileEntityReplicatorItemStack> implements IJeiNoShowRecipe {

    public GuiReplicatorItemStack(InventoryPlayer inventory, TileEntityReplicatorItemStack tile) {
        super(tile, new ContainerReplicatorItemStack(inventory, tile));
        ResourceLocation resource = getGuiLocation();
        addGuiElement(new GuiRedstoneControl(this, tileEntity, resource));
        addGuiElement(new GuiSecurityTab(this, tileEntity, resource));
        addGuiElement(new GuiSideConfigurationTab(this, tileEntity, resource));
        addGuiElement(new GuiTransporterConfigTab(this, tileEntity, resource));
        addGuiElement(new GuiUpgradeTab(this, tileEntity, resource));
        addGuiElement(new GuiEnergyInfo(() -> {
            double extra = tileEntity.getRecipe() != null ? tileEntity.getRecipe().extraEnergy : 0;
            String multiplier = MekanismUtils.getEnergyDisplay(MekanismUtils.getEnergyPerTick(tileEntity, tileEntity.BASE_ENERGY_PER_TICK + extra));
            return Arrays.asList(LangUtils.localize("gui.using") + ": " + multiplier + "/t", LangUtils.localize("gui.needed") + ": " + MekanismUtils.getEnergyDisplay(tileEntity.getNeedEnergy()));
        }, this, resource));
        addGuiElement(new GuiGasGauge(() -> tileEntity.inputGasTank, GuiGauge.Type.STANDARD, this, resource, 28, 10).withColor(GuiGauge.TypeColor.RED));
        addGuiElement(new GuiPowerBar(this, tileEntity, resource, 164, 15));
        addGuiElement(new GuiInputSlot(this, resource, 53, 34, tileEntity));
        addGuiElement(new GuiEnergySlot(this, resource, 140, 34, tileEntity));
        addGuiElement(new GuiOutputSlot(this, resource, 115, 34, tileEntity));
        addGuiElement(new GuiProgress(new GuiProgress.IProgressInfoHandler() {
            @Override
            public double getProgress() {
                return tileEntity.getScaledProgress();
            }
        }, GuiProgress.ProgressBar.MEDIUM, this, resource, 75, 37));
        addGuiElement(new GuiPlayerSlot(this, resource));
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(tileEntity.getName(), (xSize / 2) - (fontRenderer.getStringWidth(tileEntity.getName()) / 2), 6, 0x404040);
        fontRenderer.drawString(LangUtils.localize("container.inventory"), 8, (ySize - 96) + 2, 0x404040);
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
    }
}
