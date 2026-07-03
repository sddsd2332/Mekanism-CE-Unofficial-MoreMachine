package mekceumoremachine.client.gui;

import java.util.Arrays;
import mekanism.api.TileNetworkList;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.GuiSideHolder;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.gauge.GuiEnergyGauge;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.common.Mekanism;
import mekanism.common.network.PacketTileEntity.TileEntityMessage;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekceumoremachine.client.gui.element.tab.GuiOffsetVisualsTab;
import mekceumoremachine.common.inventory.container.ContainerWirelessCharging;
import mekceumoremachine.common.tile.machine.TileEntityWirelessChargingStation;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiWirelessCharging extends GuiConfigurableTile<TileEntityWirelessChargingStation, ContainerWirelessCharging> {

    private static final int BUTTON_X = 86;
    private static final int BUTTON_WIDTH = 80;

    public GuiWirelessCharging(InventoryPlayer inventory, TileEntityWirelessChargingStation tile) {
        super(tile, new ContainerWirelessCharging(inventory, tile));
        ySize += 4;
        dynamicSlots = true;
    }

    @Override
    protected void addSecurityTab() {
        addSecurityTab((net.minecraft.tileentity.TileEntity & mekanism.common.security.ISecurityTile) tileEntity, 6);
    }

    @Override
    protected void addGuiElements() {
        addButton(GuiSideHolder.rightArmorHolder(this));
        super.addGuiElements();
        addButton(new GuiEnergyGauge(this, tileEntity, GuiEnergyGauge.Type.STANDARD, 47, 15));
        addButton(new GuiEnergyTab(this, () -> Arrays.asList(
              new TextComponentString(LangUtils.localize("gui.storing") + ": " + MekanismUtils.getEnergyDisplay(tileEntity.getEnergy(), tileEntity.getMaxEnergy())),
              new TextComponentString(LangUtils.localize("gui.maxOutput") + ": " + MekanismUtils.getEnergyDisplay(tileEntity.getMaxOutput()) + "/t"))));
        addButton(new MekanismButton(this, BUTTON_X, 15, BUTTON_WIDTH, 16,
              new TextComponentString(LangUtils.localize("gui.mekceumoremachine.chargeRobit")), () -> sendPacket(0), null));
        addButton(new MekanismButton(this, BUTTON_X, 37, BUTTON_WIDTH, 16,
              new TextComponentString(LangUtils.localize("gui.mekceumoremachine.playerArmor")), () -> sendPacket(1), null));
        addButton(new MekanismButton(this, BUTTON_X, 59, BUTTON_WIDTH, 16,
              new TextComponentString(LangUtils.localize("gui.mekceumoremachine.playerInventory")), () -> sendPacket(2), null));
        addButton(new GuiOffsetVisualsTab<>(this, tileEntity, 62));
    }

    @Override
    protected void drawForegroundText(int mouseX, int mouseY) {
        drawTitleText(new TextComponentString(tileEntity.getName()), 4);
        renderInventoryText(8, ySize - 94, getXSize());
        super.drawForegroundText(mouseX, mouseY);
    }

    private void sendPacket(int type) {
        Mekanism.packetHandler.sendToServer(new TileEntityMessage(tileEntity, TileNetworkList.withContents(type)));
    }
}
