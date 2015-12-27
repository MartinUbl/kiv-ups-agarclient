package cz.zcu.kiv.ups.agarclient.main;

/**
 * World object class
 *
 * @author martin.ubl
 */
public class WorldObject
{
    /** object id */
    protected int id;
    /** X position */
    protected float positionX;
    /** Y position */
    protected float positionY;
    /** Object type id */
    protected byte typeId;
    /** Object parameter */
    protected int param;
    /** Is object colliding with local player? */
    protected boolean localIntersect;

    /**
     * World object constructor
     * @param id object id
     * @param posX position X
     * @param posY position Y
     * @param typeId type id
     * @param param parameter
     */
    public WorldObject(int id, float posX, float posY, byte typeId, int param)
    {
        this.id = id;
        this.positionX = posX;
        this.positionY = posY;
        this.typeId = typeId;
        this.param = param;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof WorldObject)
            return ((WorldObject) obj).id == this.id;
        else
            return super.equals(obj);
    }

    /**
     * Is object colliding with local player?
     * @return is colliding?
     */
    public boolean isLocalIntersection()
    {
        return localIntersect;
    }

    /**
     * Sets local intersection state
     * @param state
     */
    public void setLocalIntersection(boolean state)
    {
        localIntersect = state;
    }

}
