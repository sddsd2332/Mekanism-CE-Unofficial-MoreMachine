package mekceumoremachine.client.gui.element.tab;

import mekanism.client.SpecialColors;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.tab.window.GuiWindowCreatorTab;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.common.inventory.container.SelectedWindowData;
import mekanism.common.util.MekanismUtils;
import mekceumoremachine.client.gui.GuiWirelessConnectionWindow;
import mekceumoremachine.common.tile.machine.TileEntityWirelessChargingEnergy;
import mekceumoremachine.common.ui.MoreMachineLang;
import mekceumoremachine.common.ui.MoreMachineWindowTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class GuiWirelessEnergyEnable extends GuiWindowCreatorTab<TileEntityWirelessChargingEnergy, GuiWirelessEnergyEnable> {

    private static final ResourceLocation ENERGY = MekanismUtils.getResource(MekanismUtils.ResourceType.GUI, "energy.png");
    private boolean windowOpen;

    public GuiWirelessEnergyEnable(IGuiWrapper gui, TileEntityWirelessChargingEnergy tile, Supplier<GuiWirelessEnergyEnable> elementSupplier) {
        super(ENERGY, gui, tile, -26, 62, 26, 18, true, elementSupplier);
    }

    @Override
    protected void disableTab() {
        windowOpen = true;
        super.disableTab();
    }

    @Override
    public void tick() {
        super.tick();
        active = !windowOpen;
    }

    @Override
    protected Consumer<GuiWindow> getCloseListener() {
        return window -> {
            GuiWirelessEnergyEnable tab = getElementSupplier().get();
            tab.windowOpen = false;
            tab.active = true;
        };
    }

    @Override
    protected Consumer<GuiWindow> getReAttachListener() {
        return window -> {
            GuiWirelessEnergyEnable tab = getElementSupplier().get();
            tab.windowOpen = true;
            tab.disableTab();
        };
    }

    @Override
    @Nullable
    protected Integer getTabColor() {
        return SpecialColors.TAB_ENERGY_CONFIG.argb();
    }

    @Override
    protected ITextComponent getTooltipText() {
        return MoreMachineLang.WIRELESS_CONNECTIONS.translate();
    }

    @Override
    protected GuiWindow createWindow(SelectedWindowData windowData) {
        return new GuiWirelessConnectionWindow(gui(), (getGuiWidth() - GuiWirelessConnectionWindow.WIDTH) / 2, 18, dataSource, windowData);
    }

    @Override
    protected SelectedWindowData getNextWindowData() {
        return new SelectedWindowData(MoreMachineWindowTypes.WIRELESS_CONNECTION_CONFIG);
    }
}
