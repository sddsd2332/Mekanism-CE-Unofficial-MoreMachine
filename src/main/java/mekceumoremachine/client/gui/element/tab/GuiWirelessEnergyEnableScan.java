package mekceumoremachine.client.gui.element.tab;

import mekanism.api.TileNetworkList;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiTileEntityElement;
import mekanism.client.sound.SoundHandler;
import mekanism.common.Mekanism;
import mekanism.common.network.PacketTileEntity;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekceumoremachine.common.tile.machine.TileEntityWirelessChargingEnergy;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class GuiWirelessEnergyEnableScan extends GuiTileEntityElement<TileEntityWirelessChargingEnergy> {

    public GuiWirelessEnergyEnableScan(IGuiWrapper gui, TileEntityWirelessChargingEnergy tile, ResourceLocation def) {
        super(gui, def, tile, 176, 32, 26, 26, 179, 36, 18, 18);
    }

    @Override
    public void renderForeground(int xAxis, int yAxis) {
        if (inBounds(xAxis, yAxis)) {
            List<String> list = new ArrayList<>();
            list.add(LangUtils.localize("gui.WirelessChargingEnergy.scan"));
            list.add(LangUtils.localize("gui.WirelessChargingEnergy.quantity") + ":" + tileEntity.getScanMachineCount());
            if (tileEntity.getScanMachineCount() >= tileEntity.getMaxLinks()) {
                list.add(LangUtils.localize("gui.WirelessChargingEnergy.MaxLinks"));
            }
            this.displayTooltips(list, xAxis, yAxis);
        }
        mc.renderEngine.bindTexture(defaultLocation);
    }

    @Override
    public void mouseClicked(int xAxis, int yAxis, int button) {
        if (button == 0 && inBounds(xAxis, yAxis)) {
            TileNetworkList data = TileNetworkList.withContents(1);
            Mekanism.packetHandler.sendToServer(new PacketTileEntity.TileEntityMessage(tileEntity, data));
            SoundHandler.playSound(SoundEvents.UI_BUTTON_CLICK);
        }
    }


    @Override
    public void renderBackground(int xAxis, int yAxis, int guiWidth, int guiHeight) {
        super.renderBackground(xAxis, yAxis, guiWidth, guiHeight);
        mc.renderEngine.bindTexture(MekanismUtils.getResource(MekanismUtils.ResourceType.BUTTON, "button_icon.png"));
        guiObj.drawTexturedRect(guiWidth + 179, guiHeight + 36, 54, 36, 18, 18);
    }


}
