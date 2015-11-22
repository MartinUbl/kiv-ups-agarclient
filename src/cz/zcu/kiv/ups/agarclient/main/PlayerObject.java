package cz.zcu.kiv.ups.agarclient.main;

/**
 * Object storing generic player details
 *
 * @author martin.ubl
 */
public class PlayerObject extends WorldObject
{
    /** player name */
    protected String name;
    /** is player moving? */
    protected boolean moving;
    /** direction of movement */
    protected float moveAngle;
    /** player size */
    protected int size;

    /**
     * Constructor of player object
     * @param id player id
     * @param posX position X
     * @param posY position Y
     * @param typeId type id
     * @param param color
     * @param size player size
     * @param name name
     * @param moving is moving?
     * @param moveAngle movement direction
     */
    public PlayerObject(int id, float posX, float posY, byte typeId, int param, int size, String name, boolean moving, float moveAngle)
    {
        super(id, posX, posY, typeId, param);

        this.size = size;
        this.name = name;
        this.moving = moving;
        this.moveAngle = moveAngle;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof PlayerObject)
            return ((PlayerObject) obj).id == this.id;
        else
            return super.equals(obj);
    }

}
