package cz.zcu.kiv.ups.agarclient.main;

public class RoomListItem
{
    /** room ID */
    private int roomId;
    /** type of game */
    private int roomType;
    /** actual player count */
    private int playerCount;
    /** maximum player count */
    private int playerCapacity;
    /** room name */
    private String roomName;

    /**
     * Constructor for this container class
     * @param roomId ID of room
     * @param roomType type of game
     * @param playerCount count of players at this moment
     * @param playerCapacity player capacity
     */
    public RoomListItem(int roomId, int roomType, int playerCount, int playerCapacity, String name)
    {
        this.roomId = roomId;
        this.roomType = roomType;
        this.playerCount = playerCount;
        this.playerCapacity = playerCapacity;
        this.roomName = name;
    }

    /**
     * This appears in ListBox as output
     */
    public String toString()
    {
        return "#"+roomId+": "+roomName+" ("+playerCount+" / "+playerCapacity+")";
    }

    /**
     * Retrieves room ID
     * @return Room id
     */
    public int getRoomId()
    {
        return roomId;
    }

    /**
     * Retrieves game type
     * @return game type
     */
    public int getRoomType()
    {
        return roomType;
    }
}
