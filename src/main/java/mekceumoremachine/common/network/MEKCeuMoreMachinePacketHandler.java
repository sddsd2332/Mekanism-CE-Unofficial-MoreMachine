package mekceumoremachine.common.network;

import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.network.PacketWirelessConnection.WirelessConnectionMessage;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class MEKCeuMoreMachinePacketHandler {

    private int packetId;
    private boolean initialized;
    private final SimpleNetworkWrapper netHandler = NetworkRegistry.INSTANCE.newSimpleChannel(MEKCeuMoreMachine.MODID);

    public void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        int wirelessConnectionPacketId = packetId++;
        netHandler.registerMessage(PacketWirelessConnection.class, WirelessConnectionMessage.class, wirelessConnectionPacketId, Side.SERVER);
        netHandler.registerMessage(PacketWirelessConnection.class, WirelessConnectionMessage.class, wirelessConnectionPacketId, Side.CLIENT);
    }

    public void sendTo(IMessage message, EntityPlayerMP player) {
        netHandler.sendTo(message, player);
    }

    public void sendToServer(IMessage message) {
        netHandler.sendToServer(message);
    }
}
