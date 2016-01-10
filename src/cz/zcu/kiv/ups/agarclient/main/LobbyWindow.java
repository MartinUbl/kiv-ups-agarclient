package cz.zcu.kiv.ups.agarclient.main;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import cz.zcu.kiv.ups.agarclient.enums.Opcodes;
import cz.zcu.kiv.ups.agarclient.network.ConnectionState;
import cz.zcu.kiv.ups.agarclient.network.GamePacket;
import cz.zcu.kiv.ups.agarclient.network.Networking;

/**
 * Window with all lobby information - room list, etc.
 *
 * @author martin.ubl
 */
public class LobbyWindow extends JFrame implements NetworkStateReceiver, ActionListener
{
    private static final long serialVersionUID = 1L;

    /** Free for all game identifier */
    public static final int GAMETYPE_FREEFORALL = 0;
    /** Rated game identifier */
    public static final int GAMETYPE_RATED = 1;

    /** JList of all rooms retrieved from server */
    private JList<RoomListItem> roomList;
    /** Model for room list */
    private DefaultListModel<RoomListItem> roomListModel = new DefaultListModel<RoomListItem>();
    /** Button for joining room */
    private JButton joinButton, createButton;

    /**
     * Initializes contents of this frame
     */
    public void initComponents()
    {
        // set size and title
        setPreferredSize(new Dimension(600, 600));
        setSize(getPreferredSize());
        setTitle("KIV/UPS: Agar.io - Lobby");

        // use borderlayout here
        setLayout(new BorderLayout());

        // now create game type radiobutton group
        JPanel radioPanel = new JPanel();
        JRadioButton radio;

        ButtonGroup gametypes = new ButtonGroup();

        // free for all button
        radio = new JRadioButton("Free for all");
        radio.setSelected(true);
        radio.setMnemonic(KeyEvent.VK_F);
        radio.setActionCommand(""+GAMETYPE_FREEFORALL);
        gametypes.add(radio);
        radioPanel.add(radio);

        radio.addActionListener(this);

        // rated game button
        radio = new JRadioButton("Rated");
        radio.setSelected(true);
        radio.setMnemonic(KeyEvent.VK_R);
        radio.setActionCommand(""+GAMETYPE_RATED);
        gametypes.add(radio);
        radioPanel.add(radio);

        radio.addActionListener(this);

        add(radioPanel, BorderLayout.NORTH);

        // create room list component
        roomList = new JList<RoomListItem>(roomListModel);
        add(roomList, BorderLayout.CENTER);

        // button panel
        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new GridLayout(1,2));

        // and finally buttons for creating/joining room
        createButton = new JButton("Založit místnost");
        btnPanel.add(createButton);
        final JFrame parent = this;
        createButton.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e)
            {
                if (!createButton.isEnabled())
                    return;

                NewRoomDialog nrd = new NewRoomDialog(parent);
                nrd.initComponents();
                nrd.setVisible(true);
            }
        });

        joinButton = new JButton("Vstoupit");
        btnPanel.add(joinButton);
        joinButton.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e)
            {
                if (!joinButton.isEnabled())
                    return;

                RoomListItem rli = roomList.getSelectedValue();
                if (rli == null)
                {
                    JOptionPane.showMessageDialog(null, "Nejdříve vyberte místnost, do které se chcete připojit!", "Vyberte místnost", JOptionPane.ERROR_MESSAGE);
                }
                else
                {
                    joinButton.setEnabled(false);
                    createButton.setEnabled(false);
                    GamePacket gp = new GamePacket(Opcodes.CP_JOIN_ROOM.val());
                    gp.putInt(rli.getRoomId());
                    gp.putByte(0); // spectator - 0 for now, TODO: will be implemented later
                    Networking.getInstance().sendPacket(gp);
                }
            }
        });

        add(btnPanel, BorderLayout.SOUTH);

        // for now, we will exit when closing this frame
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // center window
        setLocationRelativeTo(null);
    }

    /**
     * Sends room list request to server
     * @param gametype type of game we are interested into
     */
    private void initRoomList(int gametype)
    {
        roomList.setEnabled(false);

        GamePacket gp = new GamePacket(Opcodes.CP_ROOM_LIST.val());
        gp.putByte(gametype);
        Networking.getInstance().sendPacket(gp);
    }

    /**
     * Default room list retrieving
     */
    public void initRoomList()
    {
        initRoomList(GAMETYPE_FREEFORALL);
    }

    /**
     * Switches application state to gaming
     */
    private void switchToGame()
    {
        GameWindow gw = new GameWindow(this);
        gw.initComponents();
        Networking.getInstance().registerStateReceiver(gw);
        setVisible(false);
        gw.setVisible(true);
        gw.initGame();
    }

    @Override
    public boolean OnPacketReceived(GamePacket packet)
    {
        // room list response
        if (packet.getOpcode() == Opcodes.SP_ROOM_LIST_RESPONSE.val())
        {
            // fill room list from received packet
            roomListModel.clear();
            int roomCount = packet.getInt();
            for (int i = 0; i < roomCount; i++)
            {
                roomListModel.addElement(new RoomListItem(
                    packet.getInt(),    // ID
                    packet.getByte(),   // game type
                    packet.getByte(),   // player count
                    packet.getByte(),   // capacity
                    packet.getString()  // room name
                ));
            }

            // enable roomList again
            roomList.setEnabled(true);
        }
        else if (packet.getOpcode() == Opcodes.SP_JOIN_ROOM_RESPONSE.val())
        {
            int statusCode = packet.getByte();
            int chatChannel = packet.getInt(); // TODO: chat

            joinButton.setEnabled(true);
            createButton.setEnabled(true);

            switch (statusCode)
            {
                case 0: // all OK
                    switchToGame();
                    break;
                case 1: // failed due to capacity
                    JOptionPane.showMessageDialog(null, "Tato místnost je plná!", "Nelze se připojit", JOptionPane.ERROR_MESSAGE);
                    break;
                case 2: // no spectators allowed
                    JOptionPane.showMessageDialog(null, "Tato místnost nepřijímá spektátory!", "Nelze se připojit", JOptionPane.ERROR_MESSAGE);
                    break;
                case 3: // no such room (should not happen)
                    JOptionPane.showMessageDialog(null, "Tato místnost již byla ukončena!", "Nelze se připojit", JOptionPane.ERROR_MESSAGE);
                    break;
                case 4: // already in room (should not happen)
                    break;
            }
        }
        else if (packet.getOpcode() == Opcodes.SP_CREATE_ROOM_RESPONSE.val())
        {
            int statusCode = packet.getByte();
            int chatChannel = packet.getInt(); // TODO: chat

            joinButton.setEnabled(true);
            createButton.setEnabled(true);

            switch (statusCode)
            {
                case 0: // all OK
                    switchToGame();
                    break;
                case 1: // failed due to capacity
                    JOptionPane.showMessageDialog(null, "Server již nedovoluje zakládat další místnosti!", "Nelze vytvořit", JOptionPane.ERROR_MESSAGE);
                    break;
                case 2: // invalid parameters
                    JOptionPane.showMessageDialog(null, "Byly zadány neplatné parametry!", "Nelze vytvořit", JOptionPane.ERROR_MESSAGE);
                    break;
            }
        }
        else if (packet.getOpcode() == Opcodes.SP_RESTORE_SESSION_RESPONSE.val())
        {
            int statusCode = packet.getByte();
            if (statusCode != 0)
            {
                JOptionPane.showMessageDialog(null, "Přihlášení vypršelo, prosím, přihlašte se znovu!", "Nelze obnovit spojení", JOptionPane.ERROR_MESSAGE);
                Networking.getInstance().disconnect();
            }
            else
            {
                initRoomList(GAMETYPE_FREEFORALL);
            }
        }

        return true;
    }

    @Override
    public boolean OnConnectionStateChanged(ConnectionState state)
    {
        if (state == ConnectionState.DISCONNECTED_RETRY)
        {
            // TODO: show label, disable UI

            roomList.setEnabled(false);
            joinButton.setEnabled(false);
            createButton.setEnabled(false);
        }
        else if (state == ConnectionState.CONNECTED)
        {
            // TODO: hide label
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

    @Override
    public void actionPerformed(ActionEvent e)
    {
        initRoomList(Integer.parseInt(e.getActionCommand()));
    }
}
