package cz.zcu.kiv.ups.agarclient.enums;

public enum ObjectTypeId
{
    OBJECT_TYPE_NONE(0),
    OBJECT_TYPE_PLAYER(1),
    OBJECT_TYPE_IDLEFOOD(2),
    OBJECT_TYPE_BONUSFOOD(3),
    OBJECT_TYPE_TRAP(4),

    PACKET_OBJECT_TYPE_PLAYER(0),
    PACKET_OBJECT_TYPE_WORLDOBJECT(1);

    /** opcode value */
    private int typeId;

    /**
     * Constructor of enum value
     * @param tid object typeid value
     */
    private ObjectTypeId(int tid)
    {
        typeId = tid;
    }

    /**
     * Retrieves value of typeid enum member
     * @return typeid value
     */
    public int val()
    {
        return typeId;
    }
}
