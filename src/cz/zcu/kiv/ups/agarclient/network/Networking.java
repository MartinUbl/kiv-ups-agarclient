package cz.zcu.kiv.ups.agarclient.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

import cz.zcu.kiv.ups.agarclient.main.NetworkStateReceiver;

/**
 * Networking class and thread
 *
 * @author martin.ubl
 */
public class Networking extends Thread
{
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

    /** flag for socket shutdown */
    private boolean isShuttingDown = false;

    /** packet send queue */
    private Queue<GamePacket> sendQueue = new LinkedList<GamePacket>();

    /** registered state receiver */
    private NetworkStateReceiver stateReceiver = null;

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

            System.out.println("Connected to "+s.getInetAddress()+":"+s.getPort());
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
        try
        {
            // write raw data and flush to be sent
            ostream.write(msg.getRaw());
            ostream.flush();
        }
        catch (Exception e)
        {
            System.err.println("Write error: "+e.getMessage());
        }
    }

    /**
     * Enqueues packet for sending
     * @param msg packet to be sent
     */
    public synchronized void sendPacket(GamePacket msg)
    {
        sendQueue.add(msg);
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
        byte[] header, data;
        try
        {
            // we need to have at least 4 bytes available (whole header)
            while (istream.available() < 4)
                ;

            // allocate space for header
            header = new byte[4];
            // read header
            istream.read(header, 0, 4);

            // fix endianity
            ByteBuffer bb = ByteBuffer.allocate(header.length);
            bb.put(header);
            bb.rewind();

            // read header contents
            short opcode = bb.getShort();
            short size = bb.getShort();

            // read data (blocking call)
            data = new byte[size];
            istream.read(data);
            // build packet and return it
            return new GamePacket(opcode, size, data);
        }
        catch (IOException e)
        {
            System.err.println("Read error");
        }

        return null;
    }

    /**
     * Retrieves input stream emptiness state
     * @return is input stream empty?
     */
    private boolean isInputStreamEmpty()
    {
        try {
            return istream.available() == 0;
        } catch (IOException e) {
            //
        }

        return true;
    }

    /**
     * Reads packet from network socket and passes it by to hooked receiver
     */
    private synchronized void _readAndDispatchPacket()
    {
        if (stateReceiver != null)
            stateReceiver.OnPacketReceived(_readPacket());
    }

    /**
     * Sends connection state change to hooked state receiver
     * @param state state to be sent
     */
    private synchronized void _sendConnectionStateChange(ConnectionState state)
    {
        if (stateReceiver != null)
            stateReceiver.OnConnectionStateChanged(state);
    }

    /**
     * Starts networking thread if not started yet
     */
    public void startUp()
    {
        isShuttingDown = false;

        if (!isAlive())
            start();
    }

    /**
     * Marks networking to be shut down
     */
    public void shutDown()
    {
        isShuttingDown = true;
    }

    @Override
    public void run()
    {
        // at first, attempt to connect to remote host
        if (!connectToServer())
        {
            // if unsuccessful, send failed state
            _sendConnectionStateChange(ConnectionState.CONNECTION_FAILED);
            return;
        }
        // otherwise send connected state
        _sendConnectionStateChange(ConnectionState.CONNECTED);

        // this loop will be repeated until there's a chance something will need to be sent/received to/from network
        while (!s.isClosed() && !isShuttingDown)
        {
            // while there's some packets to be sent, send them
            while (!isPacketQueueEmpty())
                _sendPacket(getPacketToSend());

            // while there's something waiting on socket, read it and dispatch it
            while (!isInputStreamEmpty())
                _readAndDispatchPacket();
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
