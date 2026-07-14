package mekceumoremachine.common.config;

import mekanism.api.Coord4D;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public final class WirelessConnectionClientCache {

    private static final Map<Coord4D, WirelessConnectionSnapshot> CONFIG_SNAPSHOTS = new HashMap<>();
    private static final Map<Coord4D, WirelessPreviewSnapshot> PREVIEW_SNAPSHOTS = new HashMap<>();

    private WirelessConnectionClientCache() {
    }

    public static void setConfigSnapshot(Coord4D station, WirelessConnectionSnapshot snapshot) {
        CONFIG_SNAPSHOTS.put(station, snapshot);
    }

    @Nullable
    public static WirelessConnectionSnapshot getConfigSnapshot(Coord4D station) {
        return CONFIG_SNAPSHOTS.get(station);
    }

    public static void applyStatusSnapshot(Coord4D station, WirelessConnectionStatusSnapshot status) {
        WirelessConnectionSnapshot current = CONFIG_SNAPSHOTS.get(station);
        if (current != null) {
            CONFIG_SNAPSHOTS.put(station, current.withLoadedStates(status));
        }
    }

    public static void clearConfig(Coord4D station) {
        CONFIG_SNAPSHOTS.remove(station);
    }

    public static void setPreviewSnapshot(Coord4D station, WirelessPreviewSnapshot snapshot) {
        PREVIEW_SNAPSHOTS.put(station, snapshot);
    }

    @Nullable
    public static WirelessPreviewSnapshot getPreviewSnapshot(Coord4D station) {
        return PREVIEW_SNAPSHOTS.get(station);
    }

    public static void clearPreview(Coord4D station) {
        PREVIEW_SNAPSHOTS.remove(station);
    }
}
