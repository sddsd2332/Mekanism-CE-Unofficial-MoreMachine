package mekceumoremachine.client.gui;

import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.slot.GuiVirtualSlot;
import mekanism.client.gui.element.slot.SlotType;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.common.inventory.container.SelectedWindowData;
import mekceumoremachine.common.inventory.container.ContainerTierOrganicFarm;
import mekceumoremachine.common.ui.MoreMachineWindowTypes;
import net.minecraft.util.text.TextComponentTranslation;

/** Movable shared 8x8 Organic Farm output inventory. */
public class GuiOrganicFarmOutputWindow extends GuiWindow {

    private static final int COLUMNS = 8;

    public GuiOrganicFarmOutputWindow(IGuiWrapper gui, int x, int y, ContainerTierOrganicFarm container) {
        super(gui, x, y, 160, 172, new SelectedWindowData(MoreMachineWindowTypes.ORGANIC_FARM_OUTPUT));
        interactionStrategy = InteractionStrategy.ALL;
        for (int slot = 0; slot < 64; slot++) {
            int slotX = relativeX + 8 + slot % COLUMNS * 18;
            int slotY = relativeY + 20 + slot / COLUMNS * 18;
            addChild(new GuiVirtualSlot(this, SlotType.NORMAL, gui, slotX, slotY, container.getOutputSlot(slot)));
        }
    }

    @Override
    public void renderForeground(int mouseX, int mouseY) {
        super.renderForeground(mouseX, mouseY);
        drawTitleText(new TextComponentTranslation("gui.mekceumoremachine.organic_farm_output"), 5);
    }
}
