package cz.zcu.kiv.ups.agarclient.network;

import javax.swing.JOptionPane;

import cz.zcu.kiv.ups.agarclient.main.Main;
import cz.zcu.kiv.ups.agarclient.main.NetworkStateReceiver;
import cz.zcu.kiv.ups.agarclient.enums.Opcodes;

public class GenericPacketHandler implements NetworkStateReceiver
{
    @Override
    public boolean OnPacketReceived(GamePacket packet)
    {
        if (packet.getOpcode() == Opcodes.SP_PING.val())
        {
            // just send response, server will take care of rest
            GamePacket gp = new GamePacket(Opcodes.CP_PONG.val());
            Networking.getInstance().sendPacket(gp);
        }
        else if (packet.getOpcode() == Opcodes.SP_PING_PONG.val())
        {
            int val = packet.getInt();
            Main.setClientLatency(val);
        }
        else if (packet.getOpcode() == Opcodes.SP_KICK.val())
        {
            Networking.getInstance().disconnect();

            JOptionPane.showMessageDialog(null, "Server ukončil relaci! Přihlašte se prosím znovu", "Odpojeno", JOptionPane.ERROR_MESSAGE);
        }
        else
            return false;

        return true;
    }

    @Override
    public boolean OnConnectionStateChanged(ConnectionState state)
    {
        // do not handle connection state change
        return false;
    }
}
