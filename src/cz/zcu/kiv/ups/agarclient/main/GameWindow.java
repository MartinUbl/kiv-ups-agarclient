package cz.zcu.kiv.ups.agarclient.main;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

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

    /** parent lobby window we will return to after leaving room */
    private LobbyWindow parentWindow = null;

    /**
     * Public constructor - retaining parent window
     * @param parent
     */
    public GameWindow(LobbyWindow parent)
    {
        parentWindow = parent;
    }

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

    public void returnToLobby()
    {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        Networking.getInstance().registerStateReceiver(parentWindow);

        GameStorage.getInstance().wipeAll();
        GameStorage.getInstance().setLocalPlayer(null);

        parentWindow.setVisible(true);
        parentWindow.initRoomList();

        setVisible(false);
        dispose();
    }

    /**
     * Inits game - tries to retrieve world from server
     * @param reinit
     */
    public void initGame(boolean reinit)
    {
        GamePacket gp = new GamePacket(Opcodes.CP_WORLD_REQUEST.val());
        gp.putByte(reinit ? 1 : 0);
        Networking.getInstance().sendPacket(gp);
    }

    /**
     * Inits game - tries to retrieve world from server
     */
    public void initGame()
    {
        initGame(false);
    }

    @Override
    public boolean OnPacketReceived(GamePacket packet)
    {
        GameStorage gsInst = GameStorage.getInstance();
        // TODO: rework this conditional madness

        // received packet about new world
        if (packet.getOpcode() == Opcodes.SP_NEW_WORLD.val() || packet.getOpcode() == Opcodes.SP_UPDATE_WORLD.val())
        {
            int id, size, param;
            float x, y, angle;
            byte type;
            boolean moving, dead, localDead;
            String name;

            localDead = false;

            // "our details" retrieval is received only on new world packet
            if (packet.getOpcode() == Opcodes.SP_NEW_WORLD.val())
            {
                float sizeX, sizeY;

                sizeX = packet.getFloat();
                sizeY = packet.getFloat();
                gsInst.setMapSize(sizeX, sizeY);

                // at first, retrieve our details
                id = packet.getInt();
                name = packet.getString();
                size = packet.getInt();
                x = packet.getFloat();
                y = packet.getFloat();
                param = packet.getInt(); // color
                moving = (packet.getByte() == 1);
                localDead = (packet.getByte() == 1);
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
                    dead = (packet.getByte() == 1); // maybe won't be used at all here
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

            canvas.setWeAreDead(localDead);

            canvas.setConnectionLostState(0);

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
            boolean moving, dead;
            String name;

            id = packet.getInt();
            name = packet.getString();
            size = packet.getInt();
            x = packet.getFloat();
            y = packet.getFloat();
            param = packet.getInt(); // color
            moving = (packet.getByte() == 1);
            dead = (packet.getByte() == 1); // also won't be used here, probably
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
        // player exit packet
        else if (packet.getOpcode() == Opcodes.SP_PLAYER_EXIT.val())
        {
            int playerId = packet.getInt();
            byte reason = packet.getByte();

            PlayerObject obj = GameStorage.getInstance().findPlayer(playerId);
            if (obj != null)
            {
                GameStorage.getInstance().removePlayerObject(obj);
                // TODO: chat message about player leave
            }
        }
        // session restore response packet
        else if (packet.getOpcode() == Opcodes.SP_RESTORE_SESSION_RESPONSE.val())
        {
            int statusCode = packet.getByte();
            if (statusCode != 0)
            {
                Networking.getInstance().disconnect();

                JOptionPane.showMessageDialog(null, "Přihlášení vypršelo, prosím, přihlašte se znovu!", "Nelze obnovit spojení", JOptionPane.ERROR_MESSAGE);
            }
            else
            {
                int roomId = packet.getInt();
                if (roomId != 0)
                {
                    int chatChannel = packet.getInt();
                }

                initGame(true);
            }
        }

        return true;
    }

    @Override
    public boolean OnConnectionStateChanged(ConnectionState state)
    {
        if (state == ConnectionState.DISCONNECTED_RETRY)
        {
            if (canvas != null)
                canvas.setConnectionLostState(1);
            GameStorage.getInstance().wipeAll();
        }
        else if (state == ConnectionState.CONNECTED)
        {
            if (canvas != null)
                canvas.setConnectionLostState(2);
        }
        else if (state == ConnectionState.DISCONNECTED)
        {
            LoginWindow lw = new LoginWindow();
            lw.initComponents();
            Networking.getInstance().registerStateReceiver(lw);
            setVisible(false);
            lw.setVisible(true);
        }

        return true;
    }

}
