package cz.zcu.kiv.ups.agarclient.network;

/**
 * Connection state enumerator
 *
 * @author martin.ubl
 */
public enum ConnectionState
{
    IDLE,
    CONNECTED,
    DISCONNECTED,
    DISCONNECTED_RETRY,
    CONNECTION_FAILED,
    CONNECTION_FAILED_SERVER_BAD
}
