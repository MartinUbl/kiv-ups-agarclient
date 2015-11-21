package cz.zcu.kiv.ups.agarclient.main;

/**
 * Local player class
 *
 * @author martin.ubl
 */
public class LocalPlayer extends PlayerObject
{
    /**
     * Local player constructor
     * @param id player ID
     * @param posX position X
     * @param posY position Y
     * @param typeId type id
     * @param param color
     * @param size player size
     * @param name name
     * @param moving is moving?
     * @param moveAngle angle of movement
     */
    public LocalPlayer(int id, float posX, float posY, byte typeId, int param, int size, String name, boolean moving, float moveAngle)
    {
        super(id, posX, posY, typeId, param, size, name, moving, moveAngle);
    }
}
