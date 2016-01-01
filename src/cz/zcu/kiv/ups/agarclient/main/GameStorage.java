package cz.zcu.kiv.ups.agarclient.main;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import cz.zcu.kiv.ups.agarclient.misc.Pair;

/**
 * Game storage - for storing runtime data about players, objects, etc.
 *
 * @author martin.ubl
 */
public class GameStorage
{
    /** Only one singleton instance */
    private static GameStorage INSTANCE = null;

    /** Cell size */
    private static final float CELL_SIZE = 10.0f;
    /** Visible offset of cells */
    private static final int CELL_VISIBLE_COUNT = 2;

    /** Base movement coefficient */
    public static final float MOVE_MS_COEF_MAX = 0.0065f; // 0.0065f
    /** Minimum movement speed */
    public static final float MOVE_MS_COEF_MIN = 0.0015f; // 0.0025f

    /** world object list monitor */
    public static final Object worldObjectLock = new Object();
    /** player object list monitor */
    public static final Object playerObjectLock = new Object();
    /** grid map list monitor */
    public static final Object gridMapLock = new Object();

    /** Map width */
    private float mapSizeX = 0.0f;
    /** Map height */
    private float mapSizeY = 0.0f;

    /** Map grid width */
    private int mapGridSizeX = 0;
    /** Map grid height */
    private int mapGridSizeY = 0;

    /** list of active cells */
    private List<Pair<Integer>> activeCells = new LinkedList<Pair<Integer>>();

    /** Local player object */
    private LocalPlayer localPlayer = null;
    /** All visible world objects */
    private List<WorldObject> worldObjects = new LinkedList<WorldObject>();
    /** All visible player objects */
    private List<PlayerObject> playerObjects = new LinkedList<PlayerObject>();

    /** map of objects listed in grid */
    private Map<Integer, Map<Integer, List<WorldObject>>> gridMap = new HashMap<Integer, Map<Integer, List<WorldObject>>>();

    /**
     * Retrieves GameStorage singleton instance
     * @return GameStorage instance
     */
    public static GameStorage getInstance()
    {
        if (INSTANCE == null)
        {
            INSTANCE = new GameStorage();
        }

        return INSTANCE;
    }

    /**
     * Sets map size
     * @param width map width
     * @param height map height
     */
    public void setMapSize(float width, float height)
    {
        mapSizeX = width;
        mapSizeY = height;

        mapGridSizeX = (int) (width / CELL_SIZE) + 1;
        mapGridSizeY = (int) (height / CELL_SIZE) + 1;

        gridMap.clear();

        // init grid map
        for (int i = 0; i < mapGridSizeX; i++)
        {
            gridMap.put(i, new HashMap<Integer, List<WorldObject>>());

            for (int j = 0; j < mapGridSizeY; j++)
                gridMap.get(i).put(j,  new LinkedList<WorldObject>());
        }
    }

    /**
     * Retrieves cell index by real position
     * @param pos
     * @return cell index
     */
    public static int getCellIndex(float pos)
    {
        return (int) (pos / CELL_SIZE);
    }

    /**
     * Retrieves map width
     * @return map width
     */
    public float getMapWidth()
    {
        return mapSizeX;
    }

    /**
     * Retrieves map height
     * @return map height
     */
    public float getMapHeight()
    {
        return mapSizeY;
    }

    /**
     * Adds new worldobject to world
     * @param obj object to be added
     */
    public void addWorldObject(WorldObject obj)
    {
        if (!worldObjects.contains(obj))
        {
            synchronized (worldObjectLock)
            {
                worldObjects.add(obj);
            }

            synchronized (gridMapLock)
            {
                gridMap.get(getCellIndex(obj.positionX)).get(getCellIndex(obj.positionY)).add(obj);
            }
        }
    }

    /**
     * Removes world object from world
     * @param obj object to be removed
     */
    public void removeWorldObject(WorldObject obj)
    {
        synchronized (worldObjectLock)
        {
            worldObjects.remove(obj);
        }

        synchronized (gridMapLock)
        {
            gridMap.get(getCellIndex(obj.positionX)).get(getCellIndex(obj.positionY)).remove(obj);
        }
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
        if (!playerObjects.contains(obj))
        {
            synchronized (playerObjectLock)
            {
                playerObjects.add(obj);
            }

            synchronized (gridMapLock)
            {
                gridMap.get(getCellIndex(obj.positionX)).get(getCellIndex(obj.positionY)).add(obj);
            }
        }
    }

    /**
     * Removes player from world
     * @param obj player to be removed
     */
    public void removePlayerObject(PlayerObject obj)
    {
        synchronized (playerObjectLock)
        {
            playerObjects.remove(obj);
        }

        synchronized (gridMapLock)
        {
            gridMap.get(getCellIndex(obj.positionX)).get(getCellIndex(obj.positionY)).remove(obj);
        }
    }

    /**
     * Wipes cell contents
     * @param indexX X index of cell
     * @param indexY Y index of cell
     */
    private void wipeCell(int indexX, int indexY)
    {
        List<WorldObject> wobl = gridMap.get(indexX).get(indexY);
        // remove all world objects and players from that cell
        synchronized (worldObjectLock)
        {
            worldObjects.removeAll(wobl);
        }

        // player objects will be removed by packet from server
        //playerObjects.removeAll(...);

        // also clear contents of cell in cell map
        wobl.clear();
    }

    /**
     * Moves player in world, and between grid cells if necessary
     * @param obj subject
     * @param nx new x
     * @param ny new y
     */
    public void movePlayer(PlayerObject obj, float nx, float ny)
    {
        int cellX, cellY, cellXNew, cellYNew;

        cellX = getCellIndex(obj.positionX);
        cellY = getCellIndex(obj.positionY);

        cellXNew = getCellIndex(nx);
        cellYNew = getCellIndex(ny);

        synchronized (playerObjectLock)
        {
            obj.positionX = nx;
            obj.positionY = ny;
        }

        synchronized (gridMapLock)
        {
            // if player moved between cells, relocate
            if (cellX != cellXNew || cellY != cellYNew || !gridMap.get(cellX).get(cellY).contains(obj))
            {
                gridMap.get(cellX).get(cellY).remove(obj);
                gridMap.get(cellXNew).get(cellYNew).add(obj);

                // if it was our local player, delete old objects in out-of-range cells
                if (obj == localPlayer)
                {
                    List<Pair<Integer>> nowCells = new LinkedList<Pair<Integer>>();

                    // retrieve currently active cells
                    for (int i = cellXNew - CELL_VISIBLE_COUNT; i <= cellXNew + CELL_VISIBLE_COUNT; i++)
                    {
                        if (i < 0 || i >= mapGridSizeX)
                            continue;

                        for (int j = cellYNew - CELL_VISIBLE_COUNT; j <= cellYNew + CELL_VISIBLE_COUNT; j++)
                        {
                            if (j < 0 || j >= mapGridSizeY)
                                continue;

                            nowCells.add(new Pair<Integer>(i, j));
                        }
                    }

                    // get list of cells to be wiped
                    List<Pair<Integer>> torem = new LinkedList<Pair<Integer>>();
                    for (Pair<Integer> ip : activeCells)
                    {
                        if (!nowCells.contains(ip))
                        {
                            wipeCell(ip.first, ip.second);
                            torem.add(ip);
                        }
                    }

                    // remove all listed cells
                    activeCells.removeAll(torem);
                    torem = null;

                    // activate newly discovered cells
                    for (Pair<Integer> ip : nowCells)
                    {
                        if (!activeCells.contains(ip))
                            activeCells.add(ip);
                    }
                }
                else
                {
                    int iX = getCellIndex(obj.positionX);
                    int iY = getCellIndex(obj.positionY);

                    // remove out of range players
                    if (!activeCells.contains(new Pair<Integer>(iX, iY)))
                    {
                        System.out.println("Removing player from "+iX+", "+iY);
                        removePlayerObject(obj);
                    }
                }
            }
        }
    }

    /**
     * Retrieves manhattan distance between two objects
     * @param a first object
     * @param b second object
     * @return manhattan distance between two objects
     */
    public float getManhattanDistance(WorldObject a, WorldObject b)
    {
        return (Math.abs(a.positionX - b.positionX) + Math.abs(a.positionY - b.positionY));
    }

    /**
     * Retrieves exact distance between two objects
     * @param a first object
     * @param b second object
     * @return exact distance between two objects
     */
    public float getExactDistance(WorldObject a, WorldObject b)
    {
        return (float) Math.sqrt(Math.pow(a.positionX - b.positionX, 2) + Math.pow(a.positionY - b.positionY, 2));
    }

    /**
     * Retrieves closest object in near grids
     * @return closest object, if any
     */
    public WorldObject getClosestNearObject()
    {
        int cellX = getCellIndex(localPlayer.positionX);
        int cellY = getCellIndex(localPlayer.positionY);

        WorldObject closest = null;
        float closestManhattan = 120000.0f, currDist;

        synchronized (gridMapLock)
        {
            // go through +1 and -1 sorrounding of our cell
            for (int i = cellX - 1; i <= cellX + 1; i++)
            {
                // do not allow to go past borders
                if (i < 0)
                    continue;
                if (i >= mapGridSizeX)
                    continue;

                // ..sorroundings in another direction
                for (int j = cellY - 1; j <= cellY + 1; j++)
                {
                    if (j < 0)
                        continue;
                    if (j >= mapGridSizeY)
                        continue;

                    // get list reference
                    List<WorldObject> wobjlist = gridMap.get(i).get(j);

                    // check for all objects
                    for (WorldObject ob : wobjlist)
                    {
                        // exclude local player, or already locally consumed objects
                        if (ob == localPlayer || ob.localIntersect)
                            continue;

                        // if the target is player, count his size and use exact distance
                        if (ob instanceof PlayerObject)
                            currDist = getExactDistance(ob, localPlayer) - ((float)((PlayerObject) ob).size)*GameCanvas.PLAYER_SIZE_COEF / GameCanvas.DRAW_UNIT_COEF;
                        else // otherwise use manhattan distance as relevant quick metric
                            currDist = getManhattanDistance(ob, localPlayer);

                        // if we found closer object, use it
                        if (currDist < closestManhattan)
                        {
                            closestManhattan = currDist;
                            closest = ob;
                        }
                    }
                }
            }
        }

        return closest;
    }

    /**
     * Retrieves object we currently intersects
     * @return currently intersecting object
     */
    public synchronized WorldObject getCurrentIntersectionObject()
    {
        // select closest object
        WorldObject closest = getClosestNearObject();
        if (closest == null)
            return null;

        // find exact distance of closest object
        float exDist = getExactDistance(closest, localPlayer);
        // calculate minimal distance of interaction with player
        float minDist = (localPlayer.size / 2.0f) * GameCanvas.PLAYER_SIZE_COEF / GameCanvas.DRAW_UNIT_COEF;

        // if the closest object is not player, set minimal distance to 0.2 if lower
        // this is due to offering chance to smallest cells to gain size
        if (!(closest instanceof PlayerObject) && minDist < 0.2f)
            minDist = 0.2f;

        // if we are closer than minimum required distance... gotcha!
        if (exDist <= minDist)
            return closest;

        return null;
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
     * Finds player object using its ID
     * @param id player ID
     * @return player object
     */
    public PlayerObject findPlayer(int id)
    {
        synchronized (playerObjectLock)
        {
            for (PlayerObject pl : playerObjects)
            {
                if (pl.id == id)
                    return pl;
            }
        }
        return null;
    }

    /**
     * Finds world object using its ID
     * @param id object ID
     * @return world object
     */
    public WorldObject findObject(int id)
    {
        synchronized (worldObjectLock)
        {
            for (WorldObject ob : worldObjects)
            {
                if (ob.id == id)
                    return ob;
            }
        }
        return null;
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

    /**
     * Changes size of player relatively
     * @param pl player subject
     * @param delta total change (signed value)
     */
    public void changePlayerSize(PlayerObject pl, int delta)
    {
        setPlayerSize(pl, pl.size + delta);
    }

    /**
     * Sets player size, recalculates appropriate values (speed)
     * @param pl player subject
     * @param size new size
     */
    public void setPlayerSize(PlayerObject pl, int size)
    {
        synchronized (playerObjectLock)
        {
            pl.size = size;

            if (size <= 12) // 120
                pl.moveCoef = MOVE_MS_COEF_MAX;
            else if (size >= 500)
                pl.moveCoef = MOVE_MS_COEF_MIN;
            else
                pl.moveCoef = MOVE_MS_COEF_MAX - ((size - 12.0f)/(500.0f-12.0f))*(MOVE_MS_COEF_MAX - MOVE_MS_COEF_MIN);

            System.out.println("Speed: "+pl.moveCoef);
        }
    }

    /**
     * Wipes all existence from storage
     */
    public synchronized void wipeAll()
    {
        synchronized (worldObjectLock)
        {
            worldObjects.clear();
        }
        synchronized (playerObjectLock)
        {
            playerObjects.clear();
        }
        synchronized (gridMapLock)
        {
            gridMap.clear();
        }
    }

}



















