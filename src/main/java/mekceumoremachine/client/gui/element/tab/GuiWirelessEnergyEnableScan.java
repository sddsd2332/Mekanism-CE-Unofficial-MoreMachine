package mekceumoremachine.client.gui.element.tab;

import mekanism.api.TileNetworkList;
import mekanism.client.SpecialColors;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiInsetElement;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.Mekanism;
import mekanism.common.network.PacketTileEntity;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekceumoremachine.common.tile.machine.TileEntityWirelessChargingEnergy;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class GuiWirelessEnergyEnableScan extends GuiInsetElement<TileEntityWirelessChargingEnergy> {

    private static final ResourceLocation CONFIGURATION = MekanismUtils.getResource(MekanismUtils.ResourceType.GUI, "configuration.png");

    public GuiWirelessEnergyEnableScan(IGuiWrapper gui, TileEntityWirelessChargingEnergy tile) {
        super(CONFIGURATION, gui, tile, gui.getWidth(), 6, 26, 18, false);
    }

    @Override
    protected void colorTab() {
        MekanismRenderer.color(SpecialColors.TAB_CONFIGURATION.argb());
    }

    @Override
    public void renderToolTip(int mouseX, int mouseY) {
        super.renderToolTip(mouseX, mouseY);
        List<String> list = new ArrayList<>();
        list.add(LangUtils.localize("gui.WirelessChargingEnergy.scan"));
        list.add(LangUtils.localize("gui.WirelessChargingEnergy.quantity") + ":" + dataSource.getScanMachineCount());
        if (dataSource.getScanMachineCount() >= dataSource.getMaxLinks()) {
            list.add(LangUtils.localize("gui.WirelessChargingEnergy.MaxLinks"));
        }
        displayTooltips(list, mouseX, mouseY);
    }

    @Override
    public void onClick(double mouseX, double mouseY, int button) {
        Mekanism.packetHandler.sendToServer(new PacketTileEntity.TileEntityMessage(dataSource, TileNetworkList.withContents(1)));
    }
}
