package cz.zcu.kiv.ups.agarclient.main;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JFrame;

import cz.zcu.kiv.ups.agarclient.enums.Opcodes;
import cz.zcu.kiv.ups.agarclient.network.ConnectionState;
import cz.zcu.kiv.ups.agarclient.network.GamePacket;
import cz.zcu.kiv.ups.agarclient.network.Networking;

/**
 * Game window - this is where the panel taking care of all magic rests
 *
 * @author martin.ubl
 */
public class GameWindow extends JFrame implements NetworkStateReceiver
{
    private static final long serialVersionUID = 1L;

    /** Game canvas */
    private GameCanvas canvas;

    /**
     * Initializes layout and default stuff
     */
    public void initComponents()
    {
        // set size and title
        setPreferredSize(new Dimension(700, 700));
        setSize(getPreferredSize());
        setTitle("KIV/UPS: Agar.io - Game");

        setLayout(new BorderLayout());

        // for now, we will exit when closing this frame
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // center window
        setLocationRelativeTo(null);

        // do not allow resizing
        setResizable(false);
    }

    /**
     * Inits game - tries to retrieve world from server
     */
    public void initGame()
    {
        GamePacket gp = new GamePacket(Opcodes.CP_WORLD_REQUEST.val());
        Networking.getInstance().sendPacket(gp);
    }

    @Override
    public void OnPacketReceived(GamePacket packet)
    {
        // received packet about new world
        if (packet.getOpcode() == Opcodes.SP_NEW_WORLD.val())
        {
            int id, size, param;
            float x, y, angle;
            byte type;
            boolean moving;
            String name;

            // at first, retrieve our details
            id = packet.getInt();
            name = packet.getString();
            size = packet.getInt();
            x = packet.getFloat();
            y = packet.getFloat();
            param = packet.getInt(); // color
            moving = (packet.getByte() == 1);
            angle = packet.getFloat();

            GameStorage.getInstance().setLocalPlayer(new LocalPlayer(id, x, y, (byte) 0, param, size, name, moving, angle));
            System.out.println("Local player position: "+x+" ; "+y);

            int plcount = packet.getInt();

            for (int i = 0; i < plcount; i++)
            {
                id = packet.getInt();
                name = packet.getString();
                size = packet.getInt();
                x = packet.getFloat();
                y = packet.getFloat();
                param = packet.getInt(); // color
                moving = (packet.getByte() == 1);
                angle = packet.getFloat();

                // create player
                GameStorage.getInstance().addPlayerObject(new PlayerObject(id, x, y, (byte) 0, param, size, name, moving, angle));
            }

            int objcount = packet.getInt();

            for (int i = 0; i < objcount; i++)
            {
                id = packet.getInt();
                x = packet.getFloat();
                y = packet.getFloat();
                type = packet.getByte();
                param = packet.getInt();

                // create object
                GameStorage.getInstance().addWorldObject(new WorldObject(id, x, y, type, param));
            }

            // init canvas to be drawn
            canvas = new GameCanvas();
            canvas.setSize(getPreferredSize());
            add(canvas, BorderLayout.CENTER);

            canvas.initCanvas(this);
        }
    }

    @Override
    public void OnConnectionStateChanged(ConnectionState state)
    {
        //
    }

}
