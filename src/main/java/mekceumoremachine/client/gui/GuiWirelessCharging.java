package mekceumoremachine.client.gui;

import mekanism.api.TileNetworkList;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.button.GuiDisableableButton;
import mekanism.client.gui.element.*;
import mekanism.client.gui.element.gauge.GuiEnergyGauge;
import mekanism.client.gui.element.slot.GuiInputSlot;
import mekanism.client.gui.element.slot.GuiOutputSlot;
import mekanism.client.gui.element.tab.GuiSecurityTab;
import mekanism.client.gui.element.tab.GuiSideConfigurationTab;
import mekanism.client.gui.element.tab.GuiTransporterConfigTab;
import mekanism.client.gui.element.tab.GuiVisualsTab;
import mekanism.common.Mekanism;
import mekanism.common.network.PacketTileEntity;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekceumoremachine.common.inventory.container.ContainerWirelessCharging;
import mekceumoremachine.common.tile.machine.TileEntityWirelessChargingStation;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.util.Arrays;

@SideOnly(Side.CLIENT)
public class GuiWirelessCharging extends GuiMekanismTile<TileEntityWirelessChargingStation> {

    public static final int CHARGE_ROBIT_BUTTON_ID = 0;
    public static final int PLAYER_ARMOR_BUTTON_ID = 1;
    public static final int PLAYER_INVENTORY_BUTTON_ID = 2;

    public GuiWirelessCharging(InventoryPlayer inventory, TileEntityWirelessChargingStation tile) {
        super(tile, new ContainerWirelessCharging(inventory, tile));
        ySize += 4;
        ResourceLocation resource = getGuiLocation();
        addGuiElement(new GuiRedstoneControl(this, tileEntity, resource));
        addGuiElement(new GuiSecurityTab(this, tileEntity, resource, -26));
        addGuiElement(new GuiSideConfigurationTab(this, tileEntity, resource));
        addGuiElement(new GuiTransporterConfigTab(this, 34, tileEntity, resource));
        addGuiElement(new GuiEnergyGauge(() -> tileEntity, GuiEnergyGauge.Type.STANDARD, this, resource, 47, 13 + 2));
        addGuiElement(new GuiEnergyInfo(() -> Arrays.asList(LangUtils.localize("gui.storing") + ": " + MekanismUtils.getEnergyDisplay(tileEntity.getEnergy(), tileEntity.getMaxEnergy()), LangUtils.localize("gui.maxOutput") + ": " + MekanismUtils.getEnergyDisplay(tileEntity.getMaxOutput()) + "/t"), this, resource));
        addGuiElement(new GuiInputSlot(this, resource, 25, 13 + 2, tileEntity).with(GuiSlot.SlotOverlay.MINUS));
        addGuiElement(new GuiOutputSlot(this, resource, 25, 55 + 2, tileEntity).with(GuiSlot.SlotOverlay.PLUS));
        addGuiElement(new GuiPlayerSlot(this, resource,7,83+ 2));
        addGuiElement(new GuiPlayerArmmorSlot(this, resource, 176, 37, false));
        addGuiElement(new GuiSlot(GuiSlot.SlotType.STATE_HOLDER, this, resource, 67, 15));
        addGuiElement(new GuiSlot(GuiSlot.SlotType.STATE_HOLDER, this, resource, 67, 37));
        addGuiElement(new GuiSlot(GuiSlot.SlotType.STATE_HOLDER, this, resource, 67, 59));
        addGuiElement(new GuiVisualsTab(this, tileEntity, resource, 62));
    }


    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(tileEntity.getName(), (xSize / 2) - (fontRenderer.getStringWidth(tileEntity.getName()) / 2), 4, 0x404040);
        fontRenderer.drawString(LangUtils.localize("container.inventory"), 8, ySize - 96 + 2, 0x404040);
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
    }


    @Override
    protected void drawGuiContainerBackgroundLayer(int xAxis, int yAxis) {
        super.drawGuiContainerBackgroundLayer(xAxis, yAxis);
        mc.getTextureManager().bindTexture(MekanismUtils.getResource(MekanismUtils.ResourceType.SLOT, "Slot_Icon.png"));
        drawTexturedModalRect(guiLeft + 67 + 2, guiTop + 17, tileEntity.chargeRobit ? 12 : 0, 88, 12, 12);
        drawTexturedModalRect(guiLeft + 67 + 2, guiTop + 39, tileEntity.playerArmor ?  12 : 0, 88,12, 12);
        drawTexturedModalRect(guiLeft + 67 + 2, guiTop + 61, tileEntity.playerInventory ?  12 : 0, 88, 12, 12);
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.clear();
        buttonList.add(new GuiDisableableButton(CHARGE_ROBIT_BUTTON_ID, guiLeft + 86, guiTop + 15, 80, 16, LangUtils.localize("gui.mekceumoremachine.chargeRobit")));
        buttonList.add(new GuiDisableableButton(PLAYER_ARMOR_BUTTON_ID, guiLeft + 86, guiTop + 37, 80, 16, LangUtils.localize("gui.mekceumoremachine.playerArmor")));
        buttonList.add(new GuiDisableableButton(PLAYER_INVENTORY_BUTTON_ID, guiLeft + 86, guiTop + 59, 80, 16, LangUtils.localize("gui.mekceumoremachine.playerInventory")));
    }

    @Override
    protected void actionPerformed(GuiButton guibutton) throws IOException {
        super.actionPerformed(guibutton);
        switch (guibutton.id) {
            case CHARGE_ROBIT_BUTTON_ID ->
                    Mekanism.packetHandler.sendToServer(new PacketTileEntity.TileEntityMessage(tileEntity, TileNetworkList.withContents(0)));
            case PLAYER_ARMOR_BUTTON_ID ->
                    Mekanism.packetHandler.sendToServer(new PacketTileEntity.TileEntityMessage(tileEntity, TileNetworkList.withContents(1)));
            case PLAYER_INVENTORY_BUTTON_ID ->
                    Mekanism.packetHandler.sendToServer(new PacketTileEntity.TileEntityMessage(tileEntity, TileNetworkList.withContents(2)));
        }
    }

}
