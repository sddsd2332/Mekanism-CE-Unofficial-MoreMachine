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
import net.minecraft.util.text.TextComponentString;

public class GuiWirelessEnergyEnable extends GuiInsetElement<TileEntityWirelessChargingEnergy> {

    private static final ResourceLocation ENERGY = MekanismUtils.getResource(MekanismUtils.ResourceType.GUI, "energy.png");

    public GuiWirelessEnergyEnable(IGuiWrapper gui, TileEntityWirelessChargingEnergy tile) {
        super(ENERGY, gui, tile, -26, 62, 35, 18, true);
    }

    @Override
    protected void colorTab() {
        MekanismRenderer.color(SpecialColors.TAB_ENERGY_CONFIG.argb());
    }

    @Override
    public void drawBackground(int mouseX, int mouseY, float partialTicks) {
        super.drawBackground(mouseX, mouseY, partialTicks);
        drawScaledScrollingString(new TextComponentString(LangUtils.transOnOffcap(dataSource.enableEmit)), 0, 24, TextAlignment.CENTER, titleTextColor(), width, 3,
              false, 1, getMillis());
    }

    @Override
    public void renderToolTip(int mouseX, int mouseY) {
        super.renderToolTip(mouseX, mouseY);
        displayTooltip(new TextComponentString(LangUtils.localize("gui.WirelessChargingEnergy.enable") + ":" + LangUtils.transOnOffcap(dataSource.enableEmit)), mouseX, mouseY);
    }

    @Override
    public void onClick(double mouseX, double mouseY, int button) {
        Mekanism.packetHandler.sendToServer(new PacketTileEntity.TileEntityMessage(dataSource, TileNetworkList.withContents(0)));
    }
}
