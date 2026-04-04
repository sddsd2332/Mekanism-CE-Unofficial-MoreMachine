package mekceumoremachine.common.tile.interfaces;

import mekceumoremachine.common.attachments.component.ConnectionConfig;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public interface IConnectorPreviewProvider {

    List<ConnectionConfig> getPreviewConnections();

    Vec3d getPreviewOrigin();
}
