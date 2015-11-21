package cz.zcu.kiv.ups.agarclient.main;

import java.util.LinkedList;
import java.util.List;

/**
 * Game storage - for storing runtime data about players, objects, etc.
 *
 * @author martin.ubl
 */
public class GameStorage
{
    /** Only one singleton instance */
    private static GameStorage INSTANCE = null;

    /** Local player object */
    private LocalPlayer localPlayer = null;
    /** All visible world objects */
    private List<WorldObject> worldObjects = new LinkedList<WorldObject>();
    /** All visible player objects */
    private List<PlayerObject> playerObjects = new LinkedList<PlayerObject>();

    /**
     * Retrueves GameStorage singleton instance
     * @return GameStorage instance
     */
    public static GameStorage getInstance()
    {
        if (INSTANCE == null)
            INSTANCE = new GameStorage();

        return INSTANCE;
    }

    /**
     * Adds new worldobject to world
     * @param obj object to be added
     */
    public void addWorldObject(WorldObject obj)
    {
        worldObjects.add(obj);
    }

    /**
     * Retrieves all visible objects
     * @return list of visible objects
     */
    public List<WorldObject> getVisibleObjects()
    {
        return worldObjects;
    }

    /**
     * Adds new player to world
     * @param obj player to be added
     */
    public void addPlayerObject(PlayerObject obj)
    {
        playerObjects.add(obj);
    }

    /**
     * Retrieves all visible players
     * @return list of visible players
     */
    public List<PlayerObject> getVisiblePlayers()
    {
        return playerObjects;
    }

    /**
     * Sets local player object
     * @param pl new local player object
     */
    public void setLocalPlayer(LocalPlayer pl)
    {
        localPlayer = pl;
    }

    /**
     * Retrieves local player object
     * @return local player object
     */
    public LocalPlayer getLocalPlayer()
    {
        return localPlayer;
    }

}
