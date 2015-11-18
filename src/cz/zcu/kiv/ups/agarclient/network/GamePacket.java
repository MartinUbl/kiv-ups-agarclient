package cz.zcu.kiv.ups.agarclient.network;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Game packet class
 *
 * @author martin.ubl
 */
public class GamePacket
{
    /** packet opcode */
    private short opcode;
    /** packet contents size */
    private short size;

    /** write buffer for packet building */
    private ByteArrayOutputStream writeBuffer;
    /** write buffer "gateway" which i.e. formats to proper network endianity */
    private DataOutputStream writeBufferGate;

    /** read buffer used when receiving packet */
    private ByteBuffer readBuffer;

    /**
     * Constructor for packets constructed using "receive packet from network" method
     * @param data raw data received from line
     */
    public GamePacket(byte[] data)
    {
        // parse everything needed
        ByteBuffer bb = ByteBuffer.allocate(data.length);
        bb.put(data);
        bb.rewind();

        // store opcode and size
        opcode = bb.getShort();
        size = bb.getShort();

        // create read buffer and transfer everything
        readBuffer = ByteBuffer.allocate(data.length - 4).put(bb.array(), 4, data.length - 4);
        readBuffer.rewind();
    }

    /**
     * Constructor for packets built "on fly"
     */
    public GamePacket()
    {
        writeBuffer = new ByteArrayOutputStream();
        writeBufferGate = new DataOutputStream(writeBuffer);
    }

    /**
     * Constructor for packets built "on fly", specifying opcode
     * @param opcode packet opcode
     */
    public GamePacket(int opcode)
    {
        this();
        setOpcode(opcode);
    }

    /**
     * Retrieves raw byte array of packet contents, including opcode number and size
     * @return raw packet byte array
     */
    public byte[] getRaw()
    {
        // everything needs to be converted to network endianity in order to be sent
        ByteBuffer obf = ByteBuffer.allocate(4 + size);
        obf.putShort(opcode);
        obf.putShort(size);
        obf.put(writeBuffer.toByteArray());
        return obf.array();
    }

    /**
     * Retrieves packet opcode number
     * @return opcode number
     */
    public int getOpcode()
    {
        return (int)opcode;
    }

    /**
     * Retrieves packet contents size
     * @return packet contents size
     */
    public int getSize()
    {
        return size;
    }

    /**
     * Sets opcode number
     * @param opc opcode number
     */
    public void setOpcode(int opc)
    {
        opcode = (short)opc;
    }

    /**
     * Writes string to packet
     * @param str string to be written
     */
    public void putString(String str)
    {
        try {
            writeBufferGate.write(str.getBytes());
            writeBufferGate.write(0); // make sure the string is null terminated
            size += str.length() + 1;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes integer value to packet
     * @param val value to be written
     */
    public void putInt(int val)
    {
        try {
            writeBufferGate.writeInt(val);
        } catch (IOException e) {
            e.printStackTrace();
        }
        size += 4;
    }

    /**
     * Writes short value to packet
     * @param val value to be written
     */
    public void putShort(short val)
    {
        try {
            writeBufferGate.writeShort(val);
        } catch (IOException e) {
            e.printStackTrace();
        }
        size += 2;
    }

    /**
     * Writes byte value to packet
     * @param val value to be written
     */
    public void putByte(int val)
    {
        try {
            writeBufferGate.write(val);
        } catch (IOException e) {
            e.printStackTrace();
        }
        size += 1;
    }

    /**
     * Retrieves string from packet
     * @return string read
     */
    public String getString()
    {
        String ret = new String();
        // read, until we reach zero terminator
        while (true)
        {
            char c = (char)readBuffer.get();
            if (c == '\0')
                break;
            ret += c;
        }
        return ret;
    }

    /**
     * Read integer from packet
     * @return integer read
     */
    public int getInt()
    {
        return readBuffer.getInt();
    }

    /**
     * Read short value from packet
     * @return short value read
     */
    public short getShort()
    {
        return readBuffer.getShort();
    }

    /**
     * Read single byte from packet
     * @return single byte read
     */
    public byte getByte()
    {
        return readBuffer.get();
    }

    /**
     * Read floating point number from packet
     * @return floating point number read
     */
    public float getFloat()
    {
        return readBuffer.getFloat();
    }

}
