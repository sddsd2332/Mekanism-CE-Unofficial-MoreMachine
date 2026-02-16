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

public class GuiWirelessEnergyEnable extends GuiTileEntityElement<TileEntityWirelessChargingEnergy> {

    public GuiWirelessEnergyEnable(IGuiWrapper gui,TileEntityWirelessChargingEnergy tile, ResourceLocation def) {
        super(gui, def, tile, -26, 58, 26, 35, -21, 62, 18, 18);
    }

    @Override
    public void renderBackground(int xAxis, int yAxis, int guiWidth, int guiHeight) {
        super.renderBackground(xAxis, yAxis, guiWidth, guiHeight);
        mc.renderEngine.bindTexture(MekanismUtils.getResource(MekanismUtils.ResourceType.BUTTON_TAB, "button_tab_icon.png"));
        guiObj.drawTexturedRect(guiWidth - 21, guiHeight + 62, 72, 0, 18, 18);
        mc.getTextureManager().bindTexture(MekanismUtils.getResource(MekanismUtils.ResourceType.GUI, "State.png"));
        guiObj.drawTexturedRect(guiWidth - 22, guiHeight + 81, 6, 6, 8, 8);
        guiObj.drawTexturedRect(guiWidth - 21, guiHeight + 82, tileEntity.enableEmit ? 0 : 6, 0, 6, 6);
        mc.renderEngine.bindTexture(defaultLocation);
    }

    @Override
    public void renderForeground(int xAxis, int yAxis) {
        if (inBounds(xAxis, yAxis)) {
            displayTooltip(LangUtils.localize("gui.WirelessChargingEnergy.enable") + ":" + LangUtils.transOnOffcap(tileEntity.enableEmit), xAxis, yAxis);
        }
        mc.renderEngine.bindTexture(defaultLocation);
    }

    @Override
    public void preMouseClicked(int xAxis, int yAxis, int button) {
    }

    @Override
    public void mouseClicked(int xAxis, int yAxis, int button) {
        if (button == 0 && inBounds(xAxis, yAxis)) {
            TileNetworkList data = TileNetworkList.withContents(0);
            Mekanism.packetHandler.sendToServer(new PacketTileEntity.TileEntityMessage(tileEntity, data));
            SoundHandler.playSound(SoundEvents.UI_BUTTON_CLICK);
        }
    }

}
