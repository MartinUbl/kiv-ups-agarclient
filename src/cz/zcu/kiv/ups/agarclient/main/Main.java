package cz.zcu.kiv.ups.agarclient.main;

import cz.zcu.kiv.ups.agarclient.network.Networking;

public class Main
{
    /** default host prefilled in login window */
    public static final String DEFAULT_HOST = "127.0.0.1";
    /** default server port prefilled in login window */
    public static final int DEFAULT_PORT = 8969;
    /** game version expected */
    public static final int GAME_VERSION = 1;

    /** Player unique identifier */
    private static int playerId = 0;

    /**
     * Main application method
     * @param args CLI args
     */
    public static void main(String[] args)
    {
        // start with login window
        LoginWindow lw = new LoginWindow();
        lw.initComponents();
        lw.setVisible(true);
        // register login window for receiving state changes
        Networking.getInstance().registerStateReceiver(lw);
    }

    /**
     * Retrieves player ID
     * @return player ID
     */
    public static int getPlayerId()
    {
        return playerId;
    }

    /**
     * Sets player id
     * @param id player id
     */
    public static void setPlayerId(int id)
    {
        playerId = id;
    }
}
