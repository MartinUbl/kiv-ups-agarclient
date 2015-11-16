package cz.zcu.kiv.ups.agarclient.enums;

/**
 * Opcodes enumerator
 *
 * @author martin.ubl
 */
public enum Opcodes
{
    OPCODE_NONE(0x00),
    CP_LOGIN(0x01),
    SP_LOGIN_RESPONSE(0x02),
    CP_REGISTER(0x03),
    SP_REGISTER_RESPONSE(0x04),
    CP_ROOM_LIST(0x05),
    SP_ROOM_LIST_RESPONSE(0x06),
    CP_JOIN_ROOM(0x07),
    SP_JOIN_ROOM_RESPONSE(0x08),
    CP_CREATE_ROOM(0x09),
    SP_CREATE_ROOM_RESPONSE(0x0A),
    SP_NEW_PLAYER(0x0B),
    SP_NEW_WORLD(0x0C),
    CP_MOVE_DIRECTION(0x0D),
    CP_MOVE_START(0x0E),
    CP_MOVE_STOP(0x0F),
    CP_MOVE_HEARTBEAT(0x10),
    SP_MOVE_DIRECTION(0x11),
    SP_MOVE_START(0x12),
    SP_MOVE_STOP(0x13),
    SP_MOVE_HEARTBEAT(0x14),
    SP_OBJECT_EATEN(0x15),
    SP_PLAYER_EATEN(0x16),
    CP_USE_BONUS(0x17),
    SP_USE_BONUS_FAILED(0x18),
    SP_USE_BONUS(0x19),
    SP_CANCEL_BONUS(0x1A),
    SP_NEW_OBJECT(0x1B),
    CP_PLAYER_EXIT(0x1C),
    SP_PLAYER_EXIT(0x1D),
    CP_STATS(0x1E),
    SP_STATS_RESPONSE(0x1F),
    CP_CHAT_MSG(0x20),
    SP_CHAT_MSG(0x21);

    /** opcode value */
    private int opcode;

    /**
     * Constructor of enum value
     * @param op opcode value
     */
    private Opcodes(int op)
    {
        opcode = op;
    }

    /**
     * Retrieves value of opcode enum member
     * @return opcode value
     */
    public int val()
    {
        return opcode;
    }
}
