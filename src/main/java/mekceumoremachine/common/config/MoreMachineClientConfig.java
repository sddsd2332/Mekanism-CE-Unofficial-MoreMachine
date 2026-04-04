package mekceumoremachine.common.config;

import io.netty.buffer.ByteBuf;
import mekanism.common.config.BaseConfig;
import mekanism.common.config.options.IntOption;

public class MoreMachineClientConfig extends BaseConfig {

    public final IntOption ConnectorPreviewNearestLineLimit = new IntOption(
            this,
            "ConnectorPreviewNearestLineLimit",
            10,
            "When connector preview mode is set to nearest, the maximum number of nearest target links to render.",
            1,
            2048
    );

    @Override
    public void write(ByteBuf config) {
        throw new UnsupportedOperationException("Client config shouldn't be synced");
    }

    @Override
    public void read(ByteBuf config) {
        throw new UnsupportedOperationException("Client config shouldn't be synced");
    }

    @Override
    public String getCategory() {
        return "client";
    }
}
