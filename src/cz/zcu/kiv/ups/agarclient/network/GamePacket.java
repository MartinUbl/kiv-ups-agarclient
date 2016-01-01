package cz.zcu.kiv.ups.agarclient.network;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Game packet class
 *
 * @author martin.ubl
 */
public strictfp class GamePacket
{
    /** packet opcode */
    private short opcode = 0;
    /** packet contents size */
    private short size = 0;
    /** packet read pos */
    private int readPos = 0;

    /** write buffer for packet building */
    private ByteArrayOutputStream writeBuffer;
    /** write buffer "gateway" which i.e. formats to proper network endianity */
    private DataOutputStream writeBufferGate;

    /** read buffer used when receiving packet */
    private ByteBuffer readBuffer;

    /**
     * Constructor for immediate parsed data
     * @param opcode opcode
     * @param size size of contents (data)
     * @param data contents
     */
    public GamePacket(short opcode, short size, byte[] data)
    {
        this.opcode = opcode;
        this.size = size;

        readBuffer = ByteBuffer.allocate(data.length).put(data);
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
        if (size > 0)
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
     * Retrieves packet real size
     * @return packet real size
     */
    public int getRealSize()
    {
        return readBuffer.capacity();
    }

    /**
     * Retrieves packet read position
     * @return packet read position
     */
    public int getReadPos()
    {
        return readPos;
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
            writeBufferGate.write(str.getBytes("UTF-8"));
            writeBufferGate.write(0); // make sure the string is null terminated
            size += str.getBytes("UTF-8").length + 1;
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
     * Writes float value to packet
     * @param val value to be written
     */
    public void putFloat(float val)
    {
        try {
            writeBufferGate.writeFloat(val);
        } catch (IOException e) {
            e.printStackTrace();
        }
        size += 4;
    }

    /**
     * Retrieves string from packet
     * @return string read
     */
    public String getString()
    {
        byte c;
        List<Byte> lbs = new ArrayList<Byte>();

        // read, until we reach zero terminator
        while (true)
        {
            c = (byte)((readBuffer.get()) & 0xFF);

            if (c == '\0')
                break;

            lbs.add((byte)c);
        }

        // convert list of Bytes to array of primitive bytes
        byte[] barr = new byte[lbs.size()];
        int i = 0;
        for (Byte b : lbs)
            barr[i++] = b;

        try {
            return new String(barr, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // just try at least some encoding
            return new String(barr);
        }
    }

    /**
     * Read integer from packet
     * @return integer read
     */
    public int getInt()
    {
        readPos += 4;
        return readBuffer.getInt();
    }

    /**
     * Read short value from packet
     * @return short value read
     */
    public short getShort()
    {
        readPos += 2;
        return readBuffer.getShort();
    }

    /**
     * Read single byte from packet
     * @return single byte read
     */
    public byte getByte()
    {
        readPos += 1;
        return readBuffer.get();
    }

    /**
     * Read floating point number from packet
     * @return floating point number read
     */
    public float getFloat()
    {
        readPos += 4;
        return readBuffer.getFloat();
    }

}
