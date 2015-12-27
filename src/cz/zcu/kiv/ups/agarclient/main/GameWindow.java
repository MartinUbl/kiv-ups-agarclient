package cz.zcu.kiv.ups.agarclient.main;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;

import cz.zcu.kiv.ups.agarclient.enums.ObjectTypeId;
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
        GameStorage gsInst = GameStorage.getInstance();
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
                GameStorage.getInstance().setMapSize(sizeX, sizeY);

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
                LocalPlayer plr = new LocalPlayer(id, x, y, (byte) 0, param, size, name, moving, angle);
                gsInst.setLocalPlayer(plr);

                // this will integrate player to grid map
                gsInst.movePlayer(plr, x, y);
                gsInst.setPlayerSize(plr, size);
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
                    {
                        PlayerObject plr = new PlayerObject(id, x, y, (byte) 0, param, size, name, moving, angle);
                        gsInst.addPlayerObject(plr);
                        gsInst.setPlayerSize(plr, size);
                        gsInst.movePlayer(plr, x, y);
                    }
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

                    // create object
                    gsInst.addWorldObject(new WorldObject(id, x, y, type, param));
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
                    GameStorage.getInstance().movePlayer(plr, x, y);
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
                    GameStorage.getInstance().movePlayer(plr, x, y);
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
                    PlayerObject obj = gsInst.findPlayer(id);
                    if (obj != null)
                        gsInst.removePlayerObject(obj);

                    PlayerObject plr = new PlayerObject(id, x, y, (byte) 0, param, size, name, moving, angle);
                    gsInst.addPlayerObject(plr);
                    gsInst.setPlayerSize(plr, size);
                    gsInst.movePlayer(plr, x, y);
                }
            }
        }
        // new object packet
        else if (packet.getOpcode() == Opcodes.SP_NEW_OBJECT.val())
        {
            // standard object create block
            int id, param;
            byte type;
            float x, y;

            id = packet.getInt();
            x = packet.getFloat();
            y = packet.getFloat();
            type = packet.getByte();
            param = packet.getInt();

            // create new object
            synchronized (Networking.getInstance())
            {
                WorldObject obj = gsInst.findObject(id);
                if (obj != null)
                    gsInst.removeWorldObject(obj);

                gsInst.addWorldObject(new WorldObject(id, x, y, type, param));
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
                    gsInst.changePlayerSize(gsInst.getLocalPlayer(), sizeChange);
                }
                else
                {
                    PlayerObject plr = GameStorage.getInstance().findPlayer(eaterId);
                    if (plr != null)
                        plr.size += sizeChange;
                    else
                        System.out.println("Could not find player "+eaterId);
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
                        gsInst.changePlayerSize(gsInst.getLocalPlayer(), sizeChange);
                    else
                    {
                        plr = GameStorage.getInstance().findPlayer(subjectId);
                        if (plr != null)
                            gsInst.changePlayerSize(plr, sizeChange);
                    }
                }
            }
        }
        // object destroy packet
        else if (packet.getOpcode() == Opcodes.SP_DESTROY_OBJECT.val())
        {
            int objectId = packet.getInt();
            byte type = packet.getByte();
            byte reason = packet.getByte();

            synchronized (Networking.getInstance())
            {
                if (type == ObjectTypeId.PACKET_OBJECT_TYPE_WORLDOBJECT.val())
                {
                    WorldObject obj = GameStorage.getInstance().findObject(objectId);
                    if (obj != null)
                    {
                        GameStorage.getInstance().removeWorldObject(obj);
                        // TODO: animation?
                    }
                }
                else if (type == ObjectTypeId.PACKET_OBJECT_TYPE_PLAYER.val())
                {
                    PlayerObject obj = GameStorage.getInstance().findPlayer(objectId);
                    if (obj != null)
                    {
                        GameStorage.getInstance().removePlayerObject(obj);
                        // TODO: animation?
                    }
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
