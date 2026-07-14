package mekceumoremachine.client.gui;

import java.util.Arrays;
import mekanism.api.TileNetworkList;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.gauge.GuiEnergyGauge;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.common.Mekanism;
import mekanism.common.network.PacketTileEntity.TileEntityMessage;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekceumoremachine.client.gui.element.tab.GuiOffsetVisualsTab;
import mekceumoremachine.client.gui.element.tab.GuiWirelessEnergyEnable;
import mekceumoremachine.client.gui.element.tab.GuiWirelessEnergyEnableScan;
import mekceumoremachine.common.inventory.container.ContainerWirelessEnergy;
import mekceumoremachine.common.tile.machine.TileEntityWirelessChargingEnergy;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiWirelessEnergy extends GuiConfigurableTile<TileEntityWirelessChargingEnergy, ContainerWirelessEnergy> {

    private GuiWirelessEnergyEnable connectionWindowTab;

    public GuiWirelessEnergy(InventoryPlayer inventory, TileEntityWirelessChargingEnergy tile) {
        super(tile, new ContainerWirelessEnergy(inventory, tile));
        dynamicSlots = true;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        connectionWindowTab = addButton(new GuiWirelessEnergyEnable(this, tileEntity, () -> connectionWindowTab));
        addButton(new GuiWirelessEnergyEnableScan(this, tileEntity));
        addButton(new GuiEnergyGauge(this, tileEntity, GuiEnergyGauge.Type.WIDE, 55, 18));
        addButton(new GuiEnergyTab(this, () -> Arrays.asList(
              new TextComponentString(LangUtils.localize("gui.storing") + ": " + MekanismUtils.getEnergyDisplay(tileEntity.getEnergy(), tileEntity.getMaxEnergy())),
              new TextComponentString(LangUtils.localize("gui.maxOutput") + ": " + MekanismUtils.getEnergyDisplay(tileEntity.getMaxOutput()) + "/t"))));
        addButton(new GuiOffsetVisualsTab<>(this, tileEntity, 99));
    }

    @Override
    protected void drawForegroundText(int mouseX, int mouseY) {
        drawTitleText(new TextComponentString(tileEntity.getName()), 4);
        renderInventoryText(8, ySize - 94, getXSize());
        super.drawForegroundText(mouseX, mouseY);
    }

    protected void sendPacket(int type) {
        Mekanism.packetHandler.sendToServer(new TileEntityMessage(tileEntity, TileNetworkList.withContents(type)));
    }
}
