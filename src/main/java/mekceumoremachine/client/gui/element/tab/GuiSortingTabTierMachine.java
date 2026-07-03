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
import mekceumoremachine.common.tile.interfaces.ITierSorting;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;

import java.util.ArrayList;
import java.util.List;

public class GuiSortingTabTierMachine<TILE extends TileEntity & ITierSorting> extends GuiInsetElement<TILE> {

    private static final ResourceLocation SORTING = MekanismUtils.getResource(MekanismUtils.ResourceType.GUI, "sorting.png");

    public GuiSortingTabTierMachine(IGuiWrapper gui, TILE tile) {
        super(SORTING, gui, tile, -26, 62, 35, 18, true);
    }

    @Override
    protected void colorTab() {
        MekanismRenderer.color(SpecialColors.TAB_FACTORY_SORT.argb());
    }

    @Override
    public void drawBackground(int mouseX, int mouseY, float partialTicks) {
        super.drawBackground(mouseX, mouseY, partialTicks);
        drawScaledScrollingString(new TextComponentString(LangUtils.transOnOff(dataSource.getsorting())), 0, 24, TextAlignment.CENTER, titleTextColor(), width, 3,
              false, 1, getMillis());
    }

    @Override
    public void renderToolTip(int mouseX, int mouseY) {
        super.renderToolTip(mouseX, mouseY);
        List<String> info = new ArrayList<>();
        info.add(LangUtils.localize("gui.factory.autoSort") + ":" + LangUtils.transOnOff(dataSource.getsorting()));
        displayTooltips(info, mouseX, mouseY);
    }

    @Override
    public void onClick(double mouseX, double mouseY, int button) {
        Mekanism.packetHandler.sendToServer(new PacketTileEntity.TileEntityMessage(dataSource, TileNetworkList.withContents(0)));
    }
}
