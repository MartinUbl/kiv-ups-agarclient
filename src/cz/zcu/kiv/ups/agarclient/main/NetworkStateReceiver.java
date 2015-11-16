package cz.zcu.kiv.ups.agarclient.main;

import cz.zcu.kiv.ups.agarclient.network.ConnectionState;
import cz.zcu.kiv.ups.agarclient.network.GamePacket;

/**
 * Interface for all classes, which needs to listen for network events like receiving packet
 * or changing state of connection
 *
 * @author martin.ubl
 */
public interface NetworkStateReceiver
{
    /**
     * Called when valid packet is received
     * @param packet received, parsed packet
     */
    public void OnPacketReceived(GamePacket packet);

    /**
     * Called when connection state is changed
     * @param state new state
     */
    public void OnConnectionStateChanged(ConnectionState state);
}
