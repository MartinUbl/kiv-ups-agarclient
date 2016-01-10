package cz.zcu.kiv.ups.agarclient.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

import cz.zcu.kiv.ups.agarclient.enums.Opcodes;
import cz.zcu.kiv.ups.agarclient.main.Main;
import cz.zcu.kiv.ups.agarclient.main.NetworkStateReceiver;

/**
 * Networking class and thread
 *
 * @author martin.ubl
 */
public class Networking extends Thread
{
    /** socket i/o timeout in milliseconds */
    private static final int SOCKET_IO_TIMEOUT = 5000;

    /** packet limit without response */
    private static final int SOCKET_NORESPONSE_PKT_LIMIT = 30;

    /** Only one networking class instance (singleton) */
    private static Networking INSTANCE = null;

    /** client socket */
    private Socket s;
    /** socket input stream */
    private InputStream istream;
    /** socket output stream */
    private OutputStream ostream;
    /** remote port used */
    private int port;
    /** remote host address */
    private String host;
    /** is connected? */
    private boolean isConnected = false;

    /** flag for socket shutdown */
    private boolean isShuttingDown = false;

    /** number of packets without response */
    private int noresponsePacketCount = 0;

    /** State of our connection to server */
    private ConnectionState connectionState = ConnectionState.IDLE;

    /** packet send queue */
    private Queue<GamePacket> sendQueue = new LinkedList<GamePacket>();

    /** registered state receiver */
    private NetworkStateReceiver stateReceiver = null;
    /** generic state receiver */
    private NetworkStateReceiver genericReceiver = new GenericPacketHandler();

    /**
     * Implicit constructor, just have to be private due to singleton pattern used
     */
    private Networking()
    {
    }

    /**
     * Retrieve Networking singleton instance
     * @return networking class only instance
     */
    public static Networking getInstance()
    {
        if (INSTANCE == null)
            INSTANCE = new Networking();

        return INSTANCE;
    }

    /**
     * Sets host info to be connected to
     * @param server_host host address
     * @param server_port remote port
     */
    public void setHostInfo(String server_host, int server_port)
    {
        host = server_host;
        port = server_port;
    }

    /**
     * Increases noresponse packet count
     */
    private void increaseNoresponsePackets()
    {
        noresponsePacketCount++;
    }

    /**
     * Clears noresponse packet count
     */
    private void clearNoresponsePackets()
    {
        noresponsePacketCount = 0;
    }

    /**
     * Register network state receiver
     * @param receiver registered receiver
     */
    public synchronized void registerStateReceiver(NetworkStateReceiver receiver)
    {
        stateReceiver = receiver;
    }

    /**
     * Retrieves state receiver
     * @return state receiver
     */
    public synchronized NetworkStateReceiver getStateReceiver()
    {
        return stateReceiver;
    }

    /**
     * Performs connection to remote host
     * @return was attempt successful?
     */
    private boolean connectToServer()
    {
        try
        {
            s = new Socket(host, port);

            // sets socket i/o timeout
            //s.setSoTimeout(SOCKET_IO_TIMEOUT);
        }
        catch (IOException e)
        {
            System.out.println("Connection to " + host + ":" + port + " refused");
            return false;
        }
        catch (IllegalArgumentException e)
        {
            System.out.println("Illegal port - not in allowed range 0 - 65535");
            return false;
        }
        catch (NullPointerException e)
        {
            System.out.println("Hostname not supplied");
            return false;
        }

        try
        {
            istream = s.getInputStream();
            ostream = s.getOutputStream();

            // Set receive buffer size to maximum (unsigned) short value
            // this will allow us to wait for whole packets instead of reading by fragments
            s.setReceiveBufferSize(65535);
        }
        catch (Exception e)
        {
            return false;
        }

        return true;
    }

    /**
     * Sends packet - internal thread-safe method
     * @param msg packet to be sent
     */
    private void _sendPacket(GamePacket msg)
    {
        increaseNoresponsePackets();

        try
        {
            System.out.println("Sending: "+msg.getOpcode());
            // write raw data and flush to be sent
            ostream.write(msg.getRaw());
            ostream.flush();

            if (noresponsePacketCount > SOCKET_NORESPONSE_PKT_LIMIT)
            {
                System.out.println("Limit of non-responded packets reached, closing");
                s.close();
                isConnected = false;
                _sendConnectionStateChange(ConnectionState.DISCONNECTED_RETRY);
            }
        }
        catch (Exception e)
        {
            System.err.println("Write error: "+e.toString());

            isConnected = false;
            _sendConnectionStateChange(ConnectionState.DISCONNECTED_RETRY);

            return;
        }
    }

    /**
     * Enqueues packet for sending
     * @param msg packet to be sent
     */
    public void sendPacket(GamePacket msg)
    {
        if (isConnected)
            _sendPacket(msg);
        else
        {
            synchronized (this)
            {
                sendQueue.add(msg);
            }
        }
    }

    /**
     * Retrieves packet to be sent from head of packet queue
     * @return packet to be sent
     */
    private synchronized GamePacket getPacketToSend()
    {
        return sendQueue.poll();
    }

    /**
     * Retrieves emptiness state of packet queue
     * @return is packet queue empty?
     */
    public synchronized boolean isPacketQueueEmpty()
    {
        return sendQueue.isEmpty();
    }

    /**
     * Reads packet from socket - warning, this method should be called only if we
     * found out, that there's something waiting on input
     *
     * @return read packet
     */
    private GamePacket _readPacket()
    {
        int opcode = 0, size = 0;
        byte[] header, data;
        try
        {
            // allocate space for header
            header = new byte[4];
            // read header
            istream.read(header, 0, 4);

            // fix endianity
            ByteBuffer bb = ByteBuffer.allocate(header.length);
            bb.put(header);
            bb.rewind();

            // read header contents
            opcode = bb.getShort();
            bb.rewind();
            size = bb.getInt() & 0xFFFF; // there is always a way, how to get unsigned short range

            // wait for needed bytes count
            while (istream.available() < size)
                Thread.yield();

            // read data (blocking call)
            data = new byte[size];
            istream.read(data, 0, size);

            clearNoresponsePackets();

            // build packet and return it
            return new GamePacket((short)opcode, (short)size, data);
        }
        catch (SocketException e)
        {
            if (isConnected)
            {
                System.err.println("Read error "+e.toString());

                isConnected = false;
                _sendConnectionStateChange(ConnectionState.DISCONNECTED_RETRY);
            }
        }
        catch (IOException e)
        {
            if (isConnected)
            {
                System.err.println("Read error "+e.toString());

                isConnected = false;
                _sendConnectionStateChange(ConnectionState.DISCONNECTED_RETRY);
            }
        }
        catch (Exception e)
        {
            System.err.println("Generic read error: "+e.toString());
        }

        return null;
    }

    /**
     * Reads packet from network socket and passes it by to hooked receiver
     */
    private void _readAndDispatchPacket()
    {
        GamePacket pkt = _readPacket();

        if (pkt == null)
            return;

        synchronized (this)
        {
            // at first, try to handle by generic receiver. If successful, do not handle further
            if (genericReceiver.OnPacketReceived(pkt))
                return;

            if (stateReceiver != null)
                stateReceiver.OnPacketReceived(pkt);
        }
    }

    /**
     * Sends connection state change to hooked state receiver
     * @param state state to be sent
     */
    private synchronized void _sendConnectionStateChange(ConnectionState state)
    {
        clearNoresponsePackets();

        if (!genericReceiver.OnConnectionStateChanged(state) && stateReceiver != null)
            stateReceiver.OnConnectionStateChanged(state);

        connectionState = state;
    }

    /**
     * Starts networking thread if not started yet
     */
    public void startUp()
    {
        isShuttingDown = false;

        if (!isAlive())
            start();
        else
        {
            synchronized (this)
            {
                notify();
            }
        }
    }

    /**
     * Marks networking to be shut down
     */
    public void shutDown()
    {
        isShuttingDown = true;
    }

    /**
     * Disconnects client from server
     */
    public void disconnect()
    {
        // set disconnected state
        _sendConnectionStateChange(ConnectionState.DISCONNECTED);
        isConnected = false;

        try
        {
            s.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void simulateSessionTimeout()
    {
        try {
            s.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        isConnected = false;
        _sendConnectionStateChange(ConnectionState.DISCONNECTED_RETRY);
    }

    private void sendRestoreSession()
    {
        GamePacket gp = new GamePacket(Opcodes.CP_RESTORE_SESSION.val());
        gp.putString(Main.getSessionKey());
        sendPacket(gp);
    }

    @Override
    public void run()
    {
        while (!isShuttingDown)
        {
            // at first, attempt to connect to remote host
            if (!connectToServer())
            {
                // if unsuccessful, send failed state (if not retrying)
                if (connectionState != ConnectionState.DISCONNECTED_RETRY)
                {
                    _sendConnectionStateChange(ConnectionState.CONNECTION_FAILED);

                    try
                    {
                        synchronized (this)
                        {
                            wait();
                        }
                    }
                    catch (InterruptedException e)
                    {
                        //
                    }
                }
                else // if retrying, just sleep for a few seconds and try again later
                {
                    try
                    {
                        System.out.println("Connection to "+s.getInetAddress()+":"+s.getPort()+" failed, retrying in 3s");
                        Thread.sleep(3000);
                    }
                    catch (InterruptedException e)
                    {
                        //
                    }
                }

                continue;
            }

            System.out.println("Connected to "+s.getInetAddress()+":"+s.getPort());

            if (connectionState == ConnectionState.DISCONNECTED_RETRY)
            {
                System.out.println("Attempting to restore old session");
                sendRestoreSession();
            }

            _sendConnectionStateChange(ConnectionState.CONNECTED);

            isConnected = true;

            // while there's some packets to be sent, send them
            while (!isPacketQueueEmpty())
                _sendPacket(getPacketToSend());

            // this loop will be repeated until there's a chance something will need to be sent/received to/from network
            while (!s.isClosed() && !isShuttingDown && isConnected)
            {
                // while there's something waiting on socket, read it and dispatch it
                _readAndDispatchPacket();
            }

            // if we were disconnected by external signal (i.e. kicked by server), wait for another user-supplied signal
            if (!isShuttingDown && !isConnected && connectionState == ConnectionState.DISCONNECTED)
            {
                try
                {
                    synchronized (this)
                    {
                        wait();
                    }
                }
                catch (InterruptedException e)
                {
                    //
                }
            }
        }

        // finally, close everything
        try
        {
            if (s != null)
                s.close();
        }
        catch (IOException e)
        {
            //
        }

        // and send state change
        _sendConnectionStateChange(ConnectionState.DISCONNECTED);
    }

    protected void finalize()
    {
        //
    }
}
