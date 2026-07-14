package mekceumoremachine.common.ui;

import mekanism.api.text.ILangEntry;

public enum MoreMachineLang implements ILangEntry {
    WIRELESS_CONNECTIONS("gui.WirelessChargingEnergy.connections"),
    MACHINE_TYPES("gui.WirelessChargingEnergy.machineTypes"),
    COORDINATES_STATUS("gui.WirelessChargingEnergy.coordinatesStatus"),
    SEARCH("gui.WirelessChargingEnergy.search"),
    ALL_STATUS("gui.WirelessChargingEnergy.statusAll"),
    ENABLED_STATUS("gui.WirelessChargingEnergy.statusEnabled"),
    DISABLED_STATUS("gui.WirelessChargingEnergy.statusDisabled"),
    ENABLE_ALL("gui.WirelessChargingEnergy.enableAll"),
    DISABLE_ALL("gui.WirelessChargingEnergy.disableAll"),
    ENABLE_MACHINE("gui.WirelessChargingEnergy.enableMachine"),
    DISABLE_MACHINE("gui.WirelessChargingEnergy.disableMachine"),
    MOVE_UP("gui.WirelessChargingEnergy.moveUp"),
    MOVE_DOWN("gui.WirelessChargingEnergy.moveDown"),
    MOVE_TOP("gui.WirelessChargingEnergy.moveTop"),
    MOVE_BOTTOM("gui.WirelessChargingEnergy.moveBottom"),
    ENABLED("gui.WirelessChargingEnergy.enabled"),
    DISABLED("gui.WirelessChargingEnergy.disabled"),
    UNLOADED("gui.WirelessChargingEnergy.unloaded"),
    EMPTY("gui.WirelessChargingEnergy.empty"),
    NO_MATCH("gui.WirelessChargingEnergy.noMatch"),
    MACHINE_SEARCH_TOOLTIP("gui.WirelessChargingEnergy.machineSearchTooltip"),
    TARGET_SEARCH_TOOLTIP("gui.WirelessChargingEnergy.targetSearchTooltip"),
    STATUS_FILTER_TOOLTIP("gui.WirelessChargingEnergy.statusFilterTooltip"),
    ALL_BUTTON_TOOLTIP("gui.WirelessChargingEnergy.allButtonTooltip"),
    SELECTED_BUTTON_TOOLTIP("gui.WirelessChargingEnergy.selectedButtonTooltip"),
    MOVE_UP_BUTTON_TOOLTIP("gui.WirelessChargingEnergy.moveUpButtonTooltip"),
    MOVE_DOWN_BUTTON_TOOLTIP("gui.WirelessChargingEnergy.moveDownButtonTooltip"),
    SELECT_TYPE_TOOLTIP("gui.WirelessChargingEnergy.selectTypeTooltip"),
    TYPE_TOGGLE_TOOLTIP("gui.WirelessChargingEnergy.typeToggleTooltip"),
    TYPE_HIGHLIGHT_TOOLTIP("gui.WirelessChargingEnergy.typeHighlightTooltip"),
    LOADED_COUNT("gui.WirelessChargingEnergy.loadedCount"),
    SELECT_TARGET_TOOLTIP("gui.WirelessChargingEnergy.selectTargetTooltip"),
    TARGET_TOGGLE_TOOLTIP("gui.WirelessChargingEnergy.targetToggleTooltip"),
    TARGET_HIGHLIGHT_TOOLTIP("gui.WirelessChargingEnergy.targetHighlightTooltip"),
    DYNAMIC_CHARGING_MODE("gui.WirelessChargingEnergy.dynamicChargingMode"),
    ALL_CHARGING_MODE("gui.WirelessChargingEnergy.allChargingMode"),
    CHARGING_MODE_TOOLTIP("gui.WirelessChargingEnergy.chargingModeTooltip"),
    DELETE_SELECTION("gui.WirelessChargingEnergy.deleteSelection"),
    DELETE_LIST("gui.WirelessChargingEnergy.deleteList"),
    DELETE_BUTTON_TOOLTIP("gui.WirelessChargingEnergy.deleteButtonTooltip");

    private final String translationKey;

    MoreMachineLang(String translationKey) {
        this.translationKey = translationKey;
    }

    @Override
    public String getTranslationKey() {
        return translationKey;
    }
}
