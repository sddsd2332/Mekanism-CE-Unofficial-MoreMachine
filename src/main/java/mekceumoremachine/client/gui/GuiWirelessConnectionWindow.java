package mekceumoremachine.client.gui;

import mekanism.api.Coord4D;
import mekanism.client.gui.GuiUtils;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.GuiElement;
import mekanism.client.gui.element.button.TranslationButton;
import mekanism.client.gui.element.scroll.GuiScrollList;
import mekanism.client.gui.element.text.BackgroundType;
import mekanism.client.gui.element.text.GuiTextField;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.common.inventory.container.SelectedWindowData;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.config.WirelessConnectionClientCache;
import mekceumoremachine.common.config.WirelessConnectionSnapshot;
import mekceumoremachine.common.config.WirelessConnectionSnapshot.MachineGroup;
import mekceumoremachine.common.config.WirelessConnectionSnapshot.Target;
import mekceumoremachine.client.render.WirelessConnectionHighlightHandler;
import mekceumoremachine.common.network.PacketWirelessConnection.ConnectionPacket;
import mekceumoremachine.common.network.PacketWirelessConnection.WirelessConnectionMessage;
import mekceumoremachine.common.tile.machine.TileEntityWirelessChargingEnergy;
import mekceumoremachine.common.ui.MoreMachineLang;
import mekceumoremachine.common.ui.MoreMachineWindowTypes;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GuiWirelessConnectionWindow extends GuiWindow {

    public static final int WIDTH = 336;
    private static final int HEIGHT = 220;
    private static final int TYPE_LIST_WIDTH = 154;
    private static final int TARGET_LIST_X = 170;
    private static final int TARGET_LIST_WIDTH = 160;
    private static final int LIST_Y = 38;
    private static final int LIST_HEIGHT = 144;
    private static final int ROW_HEIGHT = 20;
    private static final int TYPE_SEARCH_X = 62;
    private static final int TYPE_SEARCH_WIDTH = 98;
    private static final int TARGET_SEARCH_X = 234;
    private static final int TARGET_SEARCH_WIDTH = 50;
    private static final int FILTER_X = 288;
    private static final int FILTER_WIDTH = 42;
    private static final int BOTTOM_Y = 202;
    private static final int MODE_BUTTON_Y = BOTTOM_Y - 16;
    private static final int SNAPSHOT_REQUEST_INTERVAL = 20;
    private static final int STATUS_REQUEST_INTERVAL = 40;
    private static final int INDICATOR_SIZE = 6;
    private static final int INDICATOR_ENABLED = 0xFF57C78B;
    private static final int INDICATOR_PARTIAL = 0xFFE0C05A;
    private static final int INDICATOR_DISABLED = 0xFFC75656;
    private static final int ROW_HOVER_COLOR = 0x503A4B5F;
    private static final int ROW_SELECTED_COLOR = 0x80608CC1;
    private static final int ROW_FOCUSED_SELECTED_COLOR = 0xB04E9FDB;
    private static final int ROW_SELECTED_OUTLINE_COLOR = 0xFF8FC7FF;
    private static final int ROW_FOCUSED_SELECTED_OUTLINE_COLOR = 0xFFDDF2FF;
    private static final int ROW_UNLOADED_INNER_COLOR = 0x70C79F2F;
    private static final int ROW_UNLOADED_INNER_OUTLINE_COLOR = 0xFFE0C05A;
    private static final int ROW_ALL_UNLOADED_INNER_COLOR = 0x70C72F2F;
    private static final int ROW_ALL_UNLOADED_INNER_OUTLINE_COLOR = 0xFFE05A5A;
    private static final int UNLOADED_TARGET_TEXT_COLOR = 0xFFE0C05A;
    private static final double SCROLL_PIXELS_PER_SECOND = 12.0D;
    private static final double MIN_SCROLL_EDGE_PAUSE = 0.5D;

    private final TileEntityWirelessChargingEnergy tile;
    private final Coord4D station;
    private final MachineTypeList machineTypeList;
    private final TargetList targetList;
    private final GuiTextField machineSearchField;
    private final GuiTextField targetSearchField;
    private final TranslationButton statusFilterButton;
    private final TranslationButton allButton;
    private final TranslationButton dynamicChargingButton;
    private final TranslationButton deleteButton;
    private final TranslationButton selectedButton;
    private final TranslationButton upButton;
    private final TranslationButton downButton;

    private WirelessConnectionSnapshot snapshot = WirelessConnectionSnapshot.EMPTY;
    private StatusFilter statusFilter = StatusFilter.ALL;
    private SelectionFocus selectionFocus = SelectionFocus.MACHINE_TYPE;
    private String machineSearch = "";
    private String targetSearch = "";
    @Nullable
    private String selectedMachineType;
    @Nullable
    private Coord4D selectedTarget;
    private boolean closed;
    private boolean closeScheduled;
    private int snapshotRequestCooldown;
    private int statusRequestCooldown;

    private void drawSmoothScrollingString(TextComponentString text, int x, int y, int width, int height, int color) {
        int textWidth = getFont().getStringWidth(text.getFormattedText());
        if (textWidth <= 0 || width <= 0) {
            return;
        }
        boolean scrolling = textWidth > width;
        float drawX = x;
        if (scrolling) {
            enableGuiScissor(x, y, x + width, y + height);
            drawX -= getScrollingOffset(textWidth, width, getTimeOpened(), !getFont().getBidiFlag());
        }
        float drawY = y + (height - getFont().FONT_HEIGHT) / 2F;
        getFont().drawString(text.getFormattedText(), drawX, drawY, color, false);
        if (scrolling) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    private float getScrollingOffset(double contentWidth, double areaWidth, long msVisible, boolean leftToRight) {
        double overflowWidth = contentWidth - areaWidth;
        if (overflowWidth <= 0) {
            return 0;
        }
        long visibleDuration = Math.max(0, GuiElement.getMillis() - msVisible);
        double seconds = visibleDuration / 1_000D;
        double travelTime = overflowWidth / SCROLL_PIXELS_PER_SECOND;
        double cycleTime = MIN_SCROLL_EDGE_PAUSE * 2 + travelTime * 2;
        double cyclePosition = seconds % cycleTime;
        if (cyclePosition < MIN_SCROLL_EDGE_PAUSE) {
            return leftToRight ? 0 : (float) overflowWidth;
        }
        cyclePosition -= MIN_SCROLL_EDGE_PAUSE;
        double offset;
        if (cyclePosition < travelTime) {
            offset = cyclePosition * SCROLL_PIXELS_PER_SECOND;
        } else if (cyclePosition < travelTime + MIN_SCROLL_EDGE_PAUSE) {
            offset = overflowWidth;
        } else {
            cyclePosition -= travelTime + MIN_SCROLL_EDGE_PAUSE;
            offset = overflowWidth - cyclePosition * SCROLL_PIXELS_PER_SECOND;
        }
        return (float) (leftToRight ? offset : overflowWidth - offset);
    }

    private void enableGuiScissor(int minX, int minY, int maxX, int maxY) {
        double scaleX = minecraft.displayWidth / (double) minecraft.currentScreen.width;
        double scaleY = minecraft.displayHeight / (double) minecraft.currentScreen.height;
        int scissorX = (int) Math.floor((getGuiLeft() + minX) * scaleX);
        int scissorY = (int) Math.floor(minecraft.displayHeight - (getGuiTop() + maxY) * scaleY);
        int scissorWidth = Math.max(0, (int) Math.ceil((maxX - minX) * scaleX));
        int scissorHeight = Math.max(0, (int) Math.ceil((maxY - minY) * scaleY));
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(scissorX, scissorY, scissorWidth, scissorHeight);
    }

    public GuiWirelessConnectionWindow(IGuiWrapper gui, int x, int y, TileEntityWirelessChargingEnergy tile, SelectedWindowData windowData) {
        super(gui, x, y, WIDTH, HEIGHT, windowData);
        if (windowData.type != MoreMachineWindowTypes.WIRELESS_CONNECTION_CONFIG) {
            throw new IllegalArgumentException("Wireless connection windows must use the wireless connection window type");
        }
        this.tile = tile;
        station = Coord4D.get(tile);
        interactionStrategy = InteractionStrategy.ALL;

        machineSearchField = addChild(new GuiTextField(gui, relativeX + TYPE_SEARCH_X, relativeY + 22, TYPE_SEARCH_WIDTH, 12)
              .setBackground(BackgroundType.DIGITAL)
              .setTextColor(screenTextColor())
              .setMaxLength(64));
        targetSearchField = addChild(new GuiTextField(gui, relativeX + TARGET_SEARCH_X, relativeY + 22, TARGET_SEARCH_WIDTH, 12)
              .setBackground(BackgroundType.DIGITAL)
              .setTextColor(screenTextColor())
              .setMaxLength(64));
        statusFilterButton = addChild(new TranslationButton(gui, relativeX + FILTER_X, relativeY + 22, FILTER_WIDTH, 12,
              statusFilter.getLabel(), () -> cycleStatusFilter(), getOnHover(MoreMachineLang.STATUS_FILTER_TOOLTIP)));
        machineTypeList = addChild(new MachineTypeList(gui, relativeX + 6, relativeY + LIST_Y, TYPE_LIST_WIDTH, LIST_HEIGHT));
        targetList = addChild(new TargetList(gui, relativeX + TARGET_LIST_X, relativeY + LIST_Y, TARGET_LIST_WIDTH, LIST_HEIGHT));

        dynamicChargingButton = addChild(new TranslationButton(gui, relativeX + 6, relativeY + MODE_BUTTON_Y, 92, 12,
              tile.isDynamicWirelessCharging() ? MoreMachineLang.DYNAMIC_CHARGING_MODE : MoreMachineLang.ALL_CHARGING_MODE,
              this::toggleDynamicCharging, getOnHover(MoreMachineLang.CHARGING_MODE_TOOLTIP)));
        deleteButton = addChild(new TranslationButton(gui, relativeX + 102, relativeY + MODE_BUTTON_Y, 84, 12,
              MoreMachineLang.DELETE_SELECTION, this::deleteSelected, getOnHover(MoreMachineLang.DELETE_BUTTON_TOOLTIP)));
        allButton = addChild(new TranslationButton(gui, relativeX + 6, relativeY + BOTTOM_Y, 92, 12, MoreMachineLang.ENABLE_ALL,
              () -> sendAllEnabled(!GuiScreen.isShiftKeyDown()), getOnHover(MoreMachineLang.ALL_BUTTON_TOOLTIP)));
        selectedButton = addChild(new TranslationButton(gui, relativeX + 102, relativeY + BOTTOM_Y, 84, 12, MoreMachineLang.ENABLE_MACHINE,
              () -> sendSelectedEnabled(!GuiScreen.isShiftKeyDown()), getOnHover(MoreMachineLang.SELECTED_BUTTON_TOOLTIP)));
        upButton = addChild(new TranslationButton(gui, relativeX + 190, relativeY + BOTTOM_Y, 68, 12, MoreMachineLang.MOVE_UP,
              () -> sendMove(-1, GuiScreen.isShiftKeyDown()), getOnHover(MoreMachineLang.MOVE_UP_BUTTON_TOOLTIP)));
        downButton = addChild(new TranslationButton(gui, relativeX + 262, relativeY + BOTTOM_Y, 68, 12, MoreMachineLang.MOVE_DOWN,
              () -> sendMove(1, GuiScreen.isShiftKeyDown()), getOnHover(MoreMachineLang.MOVE_DOWN_BUTTON_TOOLTIP)));

        machineSearchField.setResponder(text -> {
            machineSearch = normalize(text);
            machineTypeList.resetScroll();
            targetList.resetScroll();
            validateSelection();
        });
        targetSearchField.setResponder(text -> {
            targetSearch = normalize(text);
            targetList.resetScroll();
            validateSelection();
        });

        WirelessConnectionClientCache.clearConfig(station);
        requestSnapshot();
        updateButtons();
    }

    @Override
    public void tick() {
        super.tick();
        if (tile.isInvalid() || tile.getWorld() == null || tile.getWorld().getTileEntity(tile.getPos()) != tile) {
            scheduleClose();
            return;
        }
        WirelessConnectionSnapshot cached = WirelessConnectionClientCache.getConfigSnapshot(station);
        if (cached != null) {
            if (cached != snapshot) {
                snapshot = cached;
                validateSelection();
            }
            if (--statusRequestCooldown <= 0) {
                requestStatusSnapshot();
            }
        } else if (--snapshotRequestCooldown <= 0) {
            requestSnapshot();
        }
        updateButtons();
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        super.close();
        WirelessConnectionClientCache.clearConfig(station);
    }

    @Override
    public void renderForeground(int mouseX, int mouseY) {
        super.renderForeground(mouseX, mouseY);
        drawTitleText(MoreMachineLang.WIRELESS_CONNECTIONS.translate(), 5);
        drawScaledScrollingString(MoreMachineLang.MACHINE_TYPES.translate(), 8, 24, TextAlignment.LEFT, titleTextColor(), 50, 0,
              false, 0.82F, getTimeOpened());
        drawScaledScrollingString(MoreMachineLang.COORDINATES_STATUS.translate(), TARGET_LIST_X + 2, 24, TextAlignment.LEFT, titleTextColor(), 62, 0,
              false, 0.78F, getTimeOpened());
        drawSearchPlaceholder(machineSearchField, TYPE_SEARCH_X, TYPE_SEARCH_WIDTH);
        drawSearchPlaceholder(targetSearchField, TARGET_SEARCH_X, TARGET_SEARCH_WIDTH);
        if (snapshot.isEmpty()) {
            drawScaledScrollingString(MoreMachineLang.EMPTY.translate(), 6, 104, TextAlignment.CENTER, screenTextColor(), WIDTH - 12, 0,
                  false, 1, getTimeOpened());
        } else if (getFilteredMachineGroups().isEmpty()) {
            drawScaledScrollingString(MoreMachineLang.NO_MATCH.translate(), 6, 104, TextAlignment.CENTER, screenTextColor(), TYPE_LIST_WIDTH, 0,
                  false, 0.9F, getTimeOpened());
        } else if (getFilteredTargets().isEmpty()) {
            drawScaledScrollingString(MoreMachineLang.NO_MATCH.translate(), TARGET_LIST_X, 104, TextAlignment.CENTER, screenTextColor(), TARGET_LIST_WIDTH, 0,
                  false, 0.9F, getTimeOpened());
        }
    }

    @Override
    public void renderToolTip(int mouseX, int mouseY) {
        super.renderToolTip(mouseX, mouseY);
        if (machineSearchField.isMouseOver(mouseX, mouseY)) {
            displayTooltip(MoreMachineLang.MACHINE_SEARCH_TOOLTIP.translate(), mouseX, mouseY);
        } else if (targetSearchField.isMouseOver(mouseX, mouseY)) {
            displayTooltip(MoreMachineLang.TARGET_SEARCH_TOOLTIP.translate(), mouseX, mouseY);
        }
    }

    private void drawSearchPlaceholder(GuiTextField field, int x, int width) {
        if (field.isEmpty() && !field.isFocused()) {
            drawScaledScrollingString(MoreMachineLang.SEARCH.translate(), x + 4, 24, TextAlignment.LEFT, 0x707070, width - 6, 0,
                  false, 0.78F, getTimeOpened());
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return targetList.mouseScrolled(mouseX, mouseY, delta) || machineTypeList.mouseScrolled(mouseX, mouseY, delta) ||
              super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean hasPersistentData() {
        return true;
    }

    private void requestSnapshot() {
        MEKCeuMoreMachine.packetHandler.sendToServer(new WirelessConnectionMessage(station, ConnectionPacket.REQUEST_CONFIG));
        snapshotRequestCooldown = SNAPSHOT_REQUEST_INTERVAL;
    }

    private void requestStatusSnapshot() {
        MEKCeuMoreMachine.packetHandler.sendToServer(new WirelessConnectionMessage(station, ConnectionPacket.REQUEST_STATUS));
        statusRequestCooldown = STATUS_REQUEST_INTERVAL;
    }

    private void closeForHighlight() {
        if (minecraft.player != null) {
            minecraft.player.closeScreen();
        } else {
            minecraft.displayGuiScreen(null);
        }
    }

    private void scheduleClose() {
        if (!closeScheduled) {
            closeScheduled = true;
            if (gui() instanceof GuiMekanism<?> mekanismGui) {
                mekanismGui.queueWindowClose(this);
            } else {
                close();
            }
        }
    }

    private void sendAllEnabled(boolean enabled) {
        send(new WirelessConnectionMessage(station, ConnectionPacket.SET_ALL_ENABLED, "", null, 0, enabled));
    }

    private void toggleDynamicCharging() {
        send(new WirelessConnectionMessage(station, ConnectionPacket.SET_DYNAMIC_CHARGING, "", null, 0,
              !tile.isDynamicWirelessCharging()));
    }

    private void sendSelectedEnabled(boolean enabled) {
        if (selectionFocus == SelectionFocus.MACHINE_TYPE) {
            if (selectedMachineType != null) {
                send(new WirelessConnectionMessage(station, ConnectionPacket.SET_TYPE_ENABLED, selectedMachineType, null, 0, enabled));
            }
        } else if (selectedMachineType != null && selectedTarget != null) {
            send(new WirelessConnectionMessage(station, ConnectionPacket.SET_TARGET_ENABLED, selectedMachineType, selectedTarget, 0, enabled));
        }
    }

    private void deleteSelected() {
        if (GuiScreen.isShiftKeyDown() && GuiScreen.isCtrlKeyDown()) {
            if (selectionFocus == SelectionFocus.MACHINE_TYPE) {
                send(new WirelessConnectionMessage(station, ConnectionPacket.DELETE_ALL, "", null, 0, false));
            } else if (selectedMachineType != null) {
                send(new WirelessConnectionMessage(station, ConnectionPacket.DELETE_TYPE, selectedMachineType, null, 0, false));
            }
            return;
        }
        if (selectionFocus == SelectionFocus.MACHINE_TYPE) {
            if (selectedMachineType != null) {
                send(new WirelessConnectionMessage(station, ConnectionPacket.DELETE_TYPE, selectedMachineType, null, 0, false));
            }
        } else if (selectedMachineType != null && selectedTarget != null) {
            send(new WirelessConnectionMessage(station, ConnectionPacket.DELETE_TARGET, selectedMachineType, selectedTarget, 0, false));
        }
    }

    private void toggleMachineGroup(MachineGroup group) {
        selectionFocus = SelectionFocus.MACHINE_TYPE;
        selectedMachineType = group.getKey();
        selectedTarget = group.getTargets().isEmpty() ? null : group.getTargets().get(0).getCoord();
        targetList.resetScroll();
        validateSelection();
        send(new WirelessConnectionMessage(station, ConnectionPacket.SET_TYPE_ENABLED, group.getKey(), null, 0,
              group.getEnabledCount() != group.getTargets().size()));
    }

    private void toggleTarget(Target target) {
        selectionFocus = SelectionFocus.TARGET;
        selectedTarget = target.getCoord();
        updateButtons();
        send(new WirelessConnectionMessage(station, ConnectionPacket.SET_TARGET_ENABLED, selectedMachineType, target.getCoord(), 0,
              !target.isEnabled()));
    }

    private void sendMove(int direction, boolean toEdge) {
        if (selectionFocus == SelectionFocus.MACHINE_TYPE) {
            if (selectedMachineType != null) {
                send(new WirelessConnectionMessage(station, toEdge ? ConnectionPacket.MOVE_TYPE_TO_EDGE : ConnectionPacket.MOVE_TYPE,
                      selectedMachineType, null, direction, false));
            }
        } else if (selectedMachineType != null && selectedTarget != null) {
            send(new WirelessConnectionMessage(station, toEdge ? ConnectionPacket.MOVE_TARGET_TO_EDGE : ConnectionPacket.MOVE_TARGET,
                  selectedMachineType, selectedTarget, direction, false));
        }
    }

    private void send(WirelessConnectionMessage message) {
        if (snapshot.isEditable()) {
            MEKCeuMoreMachine.packetHandler.sendToServer(message);
        }
    }

    private void cycleStatusFilter() {
        statusFilter = statusFilter.next();
        statusFilterButton.setMessage(statusFilter.getLabel().translate());
        targetList.resetScroll();
        validateSelection();
    }

    private void updateButtons() {
        boolean shift = GuiScreen.isShiftKeyDown();
        boolean deleteList = shift && GuiScreen.isCtrlKeyDown();
        allButton.setMessage((shift ? MoreMachineLang.DISABLE_ALL : MoreMachineLang.ENABLE_ALL).translate());
        selectedButton.setMessage((shift ? MoreMachineLang.DISABLE_MACHINE : MoreMachineLang.ENABLE_MACHINE).translate());
        upButton.setMessage((shift ? MoreMachineLang.MOVE_TOP : MoreMachineLang.MOVE_UP).translate());
        downButton.setMessage((shift ? MoreMachineLang.MOVE_BOTTOM : MoreMachineLang.MOVE_DOWN).translate());
        deleteButton.setMessage((deleteList ? MoreMachineLang.DELETE_LIST : MoreMachineLang.DELETE_SELECTION).translate());
        boolean editable = snapshot.isEditable();
        dynamicChargingButton.setMessage((tile.isDynamicWirelessCharging() ? MoreMachineLang.DYNAMIC_CHARGING_MODE :
              MoreMachineLang.ALL_CHARGING_MODE).translate());
        dynamicChargingButton.active = editable;
        allButton.active = editable && !snapshot.isEmpty();
        selectedButton.active = editable && (selectionFocus == SelectionFocus.MACHINE_TYPE ? getSelectedMachineGroup() != null : getSelectedTarget() != null);
        deleteButton.active = deleteList ? editable && (selectionFocus == SelectionFocus.MACHINE_TYPE ? !snapshot.isEmpty() :
              getSelectedMachineGroup() != null) : selectedButton.active;
        upButton.active = selectedButton.active;
        downButton.active = selectedButton.active;
    }

    private void validateSelection() {
        List<MachineGroup> groups = getFilteredMachineGroups();
        MachineGroup selectedGroup = getSelectedMachineGroup();
        if (selectedGroup == null || !groups.contains(selectedGroup)) {
            selectedMachineType = groups.isEmpty() ? null : groups.get(0).getKey();
            selectedTarget = null;
        }
        List<Target> targets = getFilteredTargets();
        Target target = getSelectedTarget();
        if (target == null || !targets.contains(target)) {
            selectedTarget = targets.isEmpty() ? null : targets.get(0).getCoord();
        }
        updateButtons();
    }

    private List<MachineGroup> getFilteredMachineGroups() {
        List<MachineGroup> groups = new ArrayList<>();
        for (MachineGroup group : snapshot.getMachineGroups()) {
            if (machineSearch.isEmpty() || normalize(getLocalizedMachineName(group)).contains(machineSearch) ||
                normalize(group.getNameKey()).contains(machineSearch) || normalize(group.getKey()).contains(machineSearch)) {
                groups.add(group);
            }
        }
        return groups;
    }

    private List<Target> getFilteredTargets() {
        MachineGroup group = getSelectedMachineGroup();
        List<Target> targets = new ArrayList<>();
        if (group == null) {
            return targets;
        }
        for (Target target : group.getTargets()) {
            if (statusFilter.matches(target) && (targetSearch.isEmpty() || normalize(target.getCoordinateText()).contains(targetSearch))) {
                targets.add(target);
            }
        }
        return targets;
    }

    @Nullable
    private MachineGroup getSelectedMachineGroup() {
        if (selectedMachineType == null) {
            return null;
        }
        for (MachineGroup group : snapshot.getMachineGroups()) {
            if (group.getKey().equals(selectedMachineType)) {
                return group;
            }
        }
        return null;
    }

    @Nullable
    private Target getSelectedTarget() {
        MachineGroup group = getSelectedMachineGroup();
        if (group == null || selectedTarget == null) {
            return null;
        }
        for (Target target : group.getTargets()) {
            if (target.getCoord().equals(selectedTarget)) {
                return target;
            }
        }
        return null;
    }

    private static String normalize(String text) {
        String stripped = TextFormatting.getTextWithoutFormattingCodes(text);
        return (stripped == null ? text : stripped).trim().toLowerCase(Locale.ROOT);
    }

    private static String getLocalizedMachineName(MachineGroup group) {
        if (!group.getMachineStack().isEmpty()) {
            String displayName = group.getMachineStack().getDisplayName();
            if (!displayName.isEmpty()) {
                return displayName;
            }
        }
        String nameKey = group.getNameKey();
        if (!nameKey.isEmpty()) {
            if (I18n.hasKey(nameKey)) {
                return I18n.format(nameKey);
            }
            String standardNameKey = nameKey.endsWith(".name") ? nameKey : nameKey + ".name";
            if (I18n.hasKey(standardNameKey)) {
                return I18n.format(standardNameKey);
            }
        }
        return nameKey;
    }

    private void drawRowSelection(int x, int y, int width, int height, boolean selected, boolean focused, boolean hovered) {
        if (selected) {
            GuiUtils.fill(x, y, x + width, y + height, focused ? ROW_FOCUSED_SELECTED_COLOR : ROW_SELECTED_COLOR);
            GuiUtils.drawOutline(x, y, width, height, focused ? ROW_FOCUSED_SELECTED_OUTLINE_COLOR : ROW_SELECTED_OUTLINE_COLOR);
        } else if (hovered) {
            GuiUtils.fill(x, y, x + width, y + height, ROW_HOVER_COLOR);
        }
    }

    private void drawLoadStateInner(int x, int y, int width, int height, boolean allUnloaded) {
        int fillColor = allUnloaded ? ROW_ALL_UNLOADED_INNER_COLOR : ROW_UNLOADED_INNER_COLOR;
        int outlineColor = allUnloaded ? ROW_ALL_UNLOADED_INNER_OUTLINE_COLOR : ROW_UNLOADED_INNER_OUTLINE_COLOR;
        GuiUtils.fill(x + 2, y + 2, x + width - 2, y + height - 2, fillColor);
        GuiUtils.drawOutline(x + 2, y + 2, width - 4, height - 4, outlineColor);
    }

    private class MachineTypeList extends GuiScrollList {

        private MachineTypeList(IGuiWrapper gui, int x, int y, int width, int height) {
            super(gui, x, y, width, height, ROW_HEIGHT, GuiInnerScreen.SCREEN, GuiInnerScreen.SCREEN_SIZE);
        }

        private void resetScroll() {
            scroll = 0;
        }

        @Override
        protected int getMaxElements() {
            return getFilteredMachineGroups().size();
        }

        @Override
        public boolean hasSelection() {
            return getSelectedMachineGroup() != null;
        }

        @Override
        protected void setSelected(int index) {
            List<MachineGroup> groups = getFilteredMachineGroups();
            if (index >= 0 && index < groups.size()) {
                MachineGroup group = groups.get(index);
                selectionFocus = SelectionFocus.MACHINE_TYPE;
                selectedMachineType = group.getKey();
                selectedTarget = group.getTargets().isEmpty() ? null : group.getTargets().get(0).getCoord();
                targetList.resetScroll();
                validateSelection();
            }
        }

        @Override
        public void clearSelection() {
            selectedMachineType = null;
            selectedTarget = null;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            MachineGroup group = getHoveredGroup((int) mouseY);
            if (button == 1 && GuiScreen.isCtrlKeyDown() && group != null && isMouseOverRows(mouseX, mouseY)) {
                List<Coord4D> locations = new ArrayList<>(group.getTargets().size());
                for (Target target : group.getTargets()) {
                    locations.add(target.getCoord());
                }
                WirelessConnectionHighlightHandler.INSTANCE.showLocations(locations);
                closeForHighlight();
                return true;
            }
            if (button == 1 && GuiScreen.isShiftKeyDown() && snapshot.isEditable() && group != null && isMouseOverRows(mouseX, mouseY)) {
                toggleMachineGroup(group);
                return true;
            }
            int indicatorX = getX() + barXShift - 10;
            if (button == 0 && snapshot.isEditable() && group != null && mouseX >= indicatorX && mouseX < indicatorX + INDICATOR_SIZE) {
                toggleMachineGroup(group);
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        protected void renderElements(int mouseX, int mouseY, float partialTicks) {
            List<MachineGroup> groups = getFilteredMachineGroups();
            int start = getCurrentSelection();
            int max = Math.max(0, Math.min(getFocusedElements(), groups.size() - start));
            for (int i = 0; i < max; i++) {
                MachineGroup group = groups.get(start + i);
                int y = relativeY + 1 + i * elementHeight;
                boolean selected = group.getKey().equals(selectedMachineType);
                boolean hovered = mouseX >= getX() + 1 && mouseX < getX() + barXShift - 1 &&
                      mouseY >= getY() + 1 + i * elementHeight && mouseY < getY() + 1 + (i + 1) * elementHeight;
                drawRowSelection(relativeX + 1, y, barXShift - 2, elementHeight, selected,
                      selectionFocus == SelectionFocus.MACHINE_TYPE, hovered);
                int loaded = group.getLoadedCount();
                if (loaded < group.getTargets().size()) {
                    drawLoadStateInner(relativeX + 1, y, barXShift - 2, elementHeight, loaded == 0);
                }
                int enabled = group.getEnabledCount();
                int color = enabled == 0 ? INDICATOR_DISABLED : enabled == group.getTargets().size() ? INDICATOR_ENABLED : INDICATOR_PARTIAL;
                GuiUtils.fill(relativeX + barXShift - 10, y + 7, relativeX + barXShift - 4, y + 13, color);
            }
        }

        @Override
        public void renderForeground(int mouseX, int mouseY) {
            super.renderForeground(mouseX, mouseY);
            List<MachineGroup> groups = getFilteredMachineGroups();
            int start = getCurrentSelection();
            int max = Math.max(0, Math.min(getFocusedElements(), groups.size() - start));
            for (int i = 0; i < max; i++) {
                MachineGroup group = groups.get(start + i);
                int rowY = relativeY + 2 + i * elementHeight;
                ItemStack machineStack = group.getMachineStack();
                if (!machineStack.isEmpty()) {
                    gui().renderItemWithOverlay(machineStack, relativeX + 3, rowY, 1F, null);
                }
                drawSmoothScrollingString(new TextComponentString(getLocalizedMachineName(group)), relativeX + 23, rowY, barXShift - 67, 16,
                      screenTextColor());
                int y = 3 + i * elementHeight;
                drawScaledScrollingString(new TextComponentString(group.getEnabledCount() + "/" + group.getTargets().size()),
                      barXShift - 42, y, TextAlignment.RIGHT, screenTextColor(), 28, 14, 0, false, 0.78F, getTimeOpened());
            }
        }

        @Override
        public void renderToolTip(int mouseX, int mouseY) {
            super.renderToolTip(mouseX, mouseY);
            MachineGroup group = getHoveredGroup(mouseY);
            int visibleIndex = getHoveredVisibleIndex(mouseY);
            if (group == null || visibleIndex < 0 || !isMouseOverRows(mouseX, mouseY)) {
                return;
            }
            int iconX = getX() + 3;
            int iconY = getY() + 2 + visibleIndex * elementHeight;
            if (!group.getMachineStack().isEmpty() && mouseX >= iconX && mouseX < iconX + 16 && mouseY >= iconY && mouseY < iconY + 16) {
                gui().renderItemTooltip(group.getMachineStack(), mouseX, mouseY);
                return;
            }
            List<String> tooltips = new ArrayList<>();
            int loaded = group.getLoadedCount();
            if (loaded < group.getTargets().size()) {
                tooltips.add(MoreMachineLang.LOADED_COUNT.translate(loaded, group.getTargets().size()).getFormattedText());
            }
            tooltips.add(MoreMachineLang.SELECT_TYPE_TOOLTIP.translate().getFormattedText());
            tooltips.add(MoreMachineLang.TYPE_TOGGLE_TOOLTIP.translate().getFormattedText());
            tooltips.add(MoreMachineLang.TYPE_HIGHLIGHT_TOOLTIP.translate().getFormattedText());
            displayTooltips(tooltips, mouseX, mouseY);
        }

        @Nullable
        private MachineGroup getHoveredGroup(int mouseY) {
            int relativeMouseY = mouseY - getY() - 1;
            if (relativeMouseY < 0 || relativeMouseY >= height - 2) {
                return null;
            }
            int index = getCurrentSelection() + relativeMouseY / elementHeight;
            List<MachineGroup> groups = getFilteredMachineGroups();
            return index >= 0 && index < groups.size() ? groups.get(index) : null;
        }

        private int getHoveredVisibleIndex(int mouseY) {
            int relativeMouseY = mouseY - getY() - 1;
            if (relativeMouseY < 0 || relativeMouseY >= height - 2) {
                return -1;
            }
            int visibleIndex = relativeMouseY / elementHeight;
            return visibleIndex >= 0 && visibleIndex < getFocusedElements() ? visibleIndex : -1;
        }

        private boolean isMouseOverRows(double mouseX, double mouseY) {
            return mouseX >= getX() + 1 && mouseX < getX() + barXShift - 1 &&
                   mouseY >= getY() + 1 && mouseY < getY() + height - 1;
        }
    }

    private class TargetList extends GuiScrollList {

        private TargetList(IGuiWrapper gui, int x, int y, int width, int height) {
            super(gui, x, y, width, height, ROW_HEIGHT, GuiInnerScreen.SCREEN, GuiInnerScreen.SCREEN_SIZE);
        }

        private void resetScroll() {
            scroll = 0;
        }

        @Override
        protected int getMaxElements() {
            return getFilteredTargets().size();
        }

        @Override
        public boolean hasSelection() {
            return getSelectedTarget() != null;
        }

        @Override
        protected void setSelected(int index) {
            List<Target> targets = getFilteredTargets();
            if (index >= 0 && index < targets.size()) {
                selectionFocus = SelectionFocus.TARGET;
                selectedTarget = targets.get(index).getCoord();
                updateButtons();
            }
        }

        @Override
        public void clearSelection() {
            selectedTarget = null;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            Target target = getHoveredTarget((int) mouseY);
            if (button == 1 && GuiScreen.isCtrlKeyDown() && target != null && isMouseOverRows(mouseX, mouseY)) {
                WirelessConnectionHighlightHandler.INSTANCE.showLocation(target.getCoord());
                closeForHighlight();
                return true;
            }
            if (button == 1 && GuiScreen.isShiftKeyDown() && snapshot.isEditable() && target != null && isMouseOverRows(mouseX, mouseY)) {
                toggleTarget(target);
                return true;
            }
            int indicatorX = getX() + barXShift - 10;
            if (button == 0 && snapshot.isEditable() && target != null && mouseX >= indicatorX && mouseX < indicatorX + INDICATOR_SIZE) {
                toggleTarget(target);
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        protected void renderElements(int mouseX, int mouseY, float partialTicks) {
            List<Target> targets = getFilteredTargets();
            int start = getCurrentSelection();
            int max = Math.max(0, Math.min(getFocusedElements(), targets.size() - start));
            for (int i = 0; i < max; i++) {
                Target target = targets.get(start + i);
                int y = relativeY + 1 + i * elementHeight;
                boolean selected = target.getCoord().equals(selectedTarget);
                boolean hovered = mouseX >= getX() + 1 && mouseX < getX() + barXShift - 1 &&
                      mouseY >= getY() + 1 + i * elementHeight && mouseY < getY() + 1 + (i + 1) * elementHeight;
                drawRowSelection(relativeX + 1, y, barXShift - 2, elementHeight, selected, selectionFocus == SelectionFocus.TARGET, hovered);
                if (!target.isLoaded()) {
                    drawLoadStateInner(relativeX + 1, y, barXShift - 2, elementHeight, false);
                }
                GuiUtils.fill(relativeX + barXShift - 10, y + 7, relativeX + barXShift - 4, y + 13,
                      target.isEnabled() ? INDICATOR_ENABLED : INDICATOR_DISABLED);
            }
        }

        @Override
        public void renderForeground(int mouseX, int mouseY) {
            super.renderForeground(mouseX, mouseY);
            List<Target> targets = getFilteredTargets();
            int start = getCurrentSelection();
            int max = Math.max(0, Math.min(getFocusedElements(), targets.size() - start));
            for (int i = 0; i < max; i++) {
                Target target = targets.get(start + i);
                int rowY = relativeY + 2 + i * elementHeight;
                int y = 3 + i * elementHeight;
                drawSmoothScrollingString(new TextComponentString(target.getCoordinateText()), relativeX + 4, rowY, barXShift - 60, 16,
                      target.isLoaded() ? screenTextColor() : UNLOADED_TARGET_TEXT_COLOR);
                drawScaledScrollingString((target.isEnabled() ? MoreMachineLang.ENABLED : MoreMachineLang.DISABLED).translate(),
                      barXShift - 54, y, TextAlignment.RIGHT, screenTextColor(), 40, 14, 0, false, 0.72F, getTimeOpened());
            }
        }

        @Override
        public void renderToolTip(int mouseX, int mouseY) {
            super.renderToolTip(mouseX, mouseY);
            Target target = getHoveredTarget(mouseY);
            if (target == null || !isMouseOverRows(mouseX, mouseY)) {
                return;
            }
            List<String> tooltips = new ArrayList<>();
            if (!target.isLoaded()) {
                tooltips.add(MoreMachineLang.UNLOADED.translate().getFormattedText());
            }
            tooltips.add(MoreMachineLang.SELECT_TARGET_TOOLTIP.translate().getFormattedText());
            tooltips.add(MoreMachineLang.TARGET_TOGGLE_TOOLTIP.translate().getFormattedText());
            tooltips.add(MoreMachineLang.TARGET_HIGHLIGHT_TOOLTIP.translate().getFormattedText());
            displayTooltips(tooltips, mouseX, mouseY);
        }

        @Nullable
        private Target getHoveredTarget(int mouseY) {
            int relativeMouseY = mouseY - getY() - 1;
            if (relativeMouseY < 0 || relativeMouseY >= height - 2) {
                return null;
            }
            int index = getCurrentSelection() + relativeMouseY / elementHeight;
            List<Target> targets = getFilteredTargets();
            return index >= 0 && index < targets.size() ? targets.get(index) : null;
        }

        private boolean isMouseOverRows(double mouseX, double mouseY) {
            return mouseX >= getX() + 1 && mouseX < getX() + barXShift - 1 &&
                   mouseY >= getY() + 1 && mouseY < getY() + height - 1;
        }
    }

    private enum StatusFilter {
        ALL(MoreMachineLang.ALL_STATUS) {
            @Override
            boolean matches(Target target) {
                return true;
            }
        },
        ENABLED(MoreMachineLang.ENABLED_STATUS) {
            @Override
            boolean matches(Target target) {
                return target.isEnabled();
            }
        },
        DISABLED(MoreMachineLang.DISABLED_STATUS) {
            @Override
            boolean matches(Target target) {
                return !target.isEnabled();
            }
        };

        private final MoreMachineLang label;

        StatusFilter(MoreMachineLang label) {
            this.label = label;
        }

        private MoreMachineLang getLabel() {
            return label;
        }

        private StatusFilter next() {
            StatusFilter[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        abstract boolean matches(Target target);
    }

    private enum SelectionFocus {
        MACHINE_TYPE,
        TARGET
    }
}
