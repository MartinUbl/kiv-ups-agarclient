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

    /** Flag for game init */
    private boolean gameInitialized = false;

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
        // TODO: rework this conditional madness

        // received packet about new world
        if (packet.getOpcode() == Opcodes.SP_NEW_WORLD.val() || packet.getOpcode() == Opcodes.SP_UPDATE_WORLD.val())
        {
            int id, size, param;
            float x, y, angle;
            byte type;
            boolean moving;
            String name;

            // "our details" retrieval is received only on new world packet
            if (packet.getOpcode() == Opcodes.SP_NEW_WORLD.val())
            {
                float sizeX, sizeY;

                sizeX = packet.getFloat();
                sizeY = packet.getFloat();
                Main.setMapSize(sizeX, sizeY);

                // at first, retrieve our details
                id = packet.getInt();
                name = packet.getString();
                size = packet.getInt();
                x = packet.getFloat();
                y = packet.getFloat();
                param = packet.getInt(); // color
                moving = (packet.getByte() == 1);
                angle = packet.getFloat();

                // store local player
                Main.setPlayerId(id);
                GameStorage.getInstance().setLocalPlayer(new LocalPlayer(id, x, y, (byte) 0, param, size, name, moving, angle));
            }

            // retrieve count of players present in updatepacket
            int plcount = packet.getInt();

            synchronized (Networking.getInstance())
            {
                // resolve all players
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
                    if (id != Main.getPlayerId())
                        GameStorage.getInstance().addPlayerObject(new PlayerObject(id, x, y, (byte) 0, param, size, name, moving, angle));
                }
            }

            // retrieve object count present in updatepacket
            int objcount = packet.getInt();

            synchronized (Networking.getInstance())
            {
                for (int i = 0; i < objcount; i++)
                {
                    id = packet.getInt();
                    x = packet.getFloat();
                    y = packet.getFloat();
                    type = packet.getByte();
                    param = packet.getInt();

                    //System.out.println("Adding "+id+" "+x+" "+y+", "+type+" "+param);

                    // create object
                    GameStorage.getInstance().addWorldObject(new WorldObject(id, x, y, type, param));
                }
            }

            // init canvas to be drawn (again just when new world is obtained)
            if (packet.getOpcode() == Opcodes.SP_NEW_WORLD.val() && !gameInitialized)
            {
                canvas = new GameCanvas();
                canvas.setSize(getPreferredSize());
                add(canvas, BorderLayout.CENTER);

                canvas.initCanvas(this);
            }
            canvas.setWeAreDead(false);

            gameInitialized = true;
        }
        // move heartbeat packet
        else if (packet.getOpcode() == Opcodes.SP_MOVE_HEARTBEAT.val())
        {
            // get player ID and position
            int id = packet.getInt();
            float x = packet.getFloat();
            float y = packet.getFloat();

            synchronized (Networking.getInstance())
            {
                // find player and set position
                PlayerObject plr = GameStorage.getInstance().findPlayer(id);
                if (plr != null && id != Main.getPlayerId())
                {
                    plr.positionX = x;
                    plr.positionY = y;
                }
            }
        }
        // move direction packet
        else if (packet.getOpcode() == Opcodes.SP_MOVE_DIRECTION.val())
        {
            // get player ID and move angle
            int id = packet.getInt();
            float angle = packet.getFloat();

            synchronized (Networking.getInstance())
            {
                // find player and set move angle
                PlayerObject plr = GameStorage.getInstance().findPlayer(id);
                if (plr != null && id != Main.getPlayerId())
                {
                    plr.moveAngle = angle;
                }
            }
        }
        // move start packet
        else if (packet.getOpcode() == Opcodes.SP_MOVE_START.val())
        {
            // get player ID and move angle
            int id = packet.getInt();
            float angle = packet.getFloat();

            // find player, set move angle and moving flag
            synchronized (Networking.getInstance())
            {
                PlayerObject plr = GameStorage.getInstance().findPlayer(id);
                if (plr != null && id != Main.getPlayerId())
                {
                    plr.moveAngle = angle;
                    plr.moving = true;
                }
            }
        }
        // move stop packet
        else if (packet.getOpcode() == Opcodes.SP_MOVE_STOP.val())
        {
            // get player ID and position
            int id = packet.getInt();
            float x = packet.getFloat();
            float y = packet.getFloat();

            synchronized (Networking.getInstance())
            {
                // find player, set position and unset moving flag
                PlayerObject plr = GameStorage.getInstance().findPlayer(id);
                if (plr != null && id != Main.getPlayerId())
                {
                    plr.positionX = x;
                    plr.positionY = y;
                    plr.moving = false;
                }
            }
        }
        // new player packet
        else if (packet.getOpcode() == Opcodes.SP_NEW_PLAYER.val())
        {
            // standard player create block
            int id, size, param;
            float x, y, angle;
            boolean moving;
            String name;

            id = packet.getInt();
            name = packet.getString();
            size = packet.getInt();
            x = packet.getFloat();
            y = packet.getFloat();
            param = packet.getInt(); // color
            moving = (packet.getByte() == 1);
            angle = packet.getFloat();

            // create player, if it's not us
            if (id != Main.getPlayerId())
            {
                synchronized (Networking.getInstance())
                {
                    System.out.println("Creating player "+name+" at "+x+" ; "+y);
                    GameStorage.getInstance().addPlayerObject(new PlayerObject(id, x, y, (byte) 0, param, size, name, moving, angle));
                }
            }
        }
        // object eaten packet
        else if (packet.getOpcode() == Opcodes.SP_OBJECT_EATEN.val())
        {
            int objectId = packet.getInt();
            int eaterId = packet.getInt();
            int sizeChange = packet.getInt();

            /*WorldObject obj = GameStorage.getInstance().findObject(objectId);
            if (obj != null)
            {
                GameStorage.getInstance().removeWorldObject(obj);
            }*/

            synchronized (Networking.getInstance())
            {
                if (eaterId == Main.getPlayerId())
                {
                    GameStorage.getInstance().getLocalPlayer().size += sizeChange;
                    System.out.println("Changing my size to "+GameStorage.getInstance().getLocalPlayer().size);
                }
                else
                {
                    PlayerObject plr = GameStorage.getInstance().findPlayer(eaterId);
                    if (plr != null)
                        plr.size += sizeChange;
                }
            }
        }
        // player eaten packet
        else if (packet.getOpcode() == Opcodes.SP_PLAYER_EATEN.val())
        {
            int subjectId = packet.getInt();
            int eaterId = packet.getInt();
            int sizeChange = packet.getInt();

            if (subjectId == Main.getPlayerId())
            {
                // we had been eaten :-(
                canvas.setWeAreDead(true);
            }
            else
            {
                synchronized (Networking.getInstance())
                {
                    PlayerObject plr = GameStorage.getInstance().findPlayer(subjectId);
                    if (plr != null)
                        GameStorage.getInstance().removePlayerObject(plr);

                    if (eaterId == Main.getPlayerId())
                        GameStorage.getInstance().getLocalPlayer().size += sizeChange;
                    else
                    {
                        plr = GameStorage.getInstance().findPlayer(subjectId);
                        if (plr != null)
                            plr.size += sizeChange;
                    }
                }
            }
        }
        // object destroy packet
        else if (packet.getOpcode() == Opcodes.SP_DESTROY_OBJECT.val())
        {
            int objectId = packet.getInt();
            byte reason = packet.getByte();

            synchronized (Networking.getInstance())
            {
                WorldObject obj = GameStorage.getInstance().findObject(objectId);
                if (obj != null)
                {
                    GameStorage.getInstance().removeWorldObject(obj);
                    // TODO: animation?
                }
            }
        }
    }

    @Override
    public void OnConnectionStateChanged(ConnectionState state)
    {
        //
    }

}
