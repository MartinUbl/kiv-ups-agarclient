package cz.zcu.kiv.ups.agarclient.main;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import cz.zcu.kiv.ups.agarclient.enums.Opcodes;
import cz.zcu.kiv.ups.agarclient.network.ConnectionState;
import cz.zcu.kiv.ups.agarclient.network.GamePacket;
import cz.zcu.kiv.ups.agarclient.network.Networking;

/**
 * Login window for retrieving server address and port, user name, password
 *
 * @author martin.ubl
 */
public class LoginWindow extends JFrame implements NetworkStateReceiver
{
    private static final long serialVersionUID = 1L;

    /** textbox for server address */
    private JTextField hostField;
    /** textbox for port value */
    private JTextField portField;
    /** textbox for username */
    private JTextField usernameField;
    /** textbox for password */
    private JPasswordField passwordField;
    /** login button, stored for capability of disabling/enabling */
    private JButton loginButton;
    /** register button, stored for capability of disabling/enabling */
    private JButton registerButton;

    /** result text label */
    private JLabel resultText;

    /**
     * Initializes components inside this frame
     */
    public void initComponents()
    {
        // set size
        setPreferredSize(new Dimension(500, 200));
        setSize(getPreferredSize());

        // choose layout
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel label;

        // server address row
        label = new JLabel("Server");
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.3;
        add(label, c);

        hostField = new JTextField(Main.DEFAULT_HOST);
        c.weightx = 0.7;
        c.gridx = 1;
        c.gridy = 0;
        add(hostField, c);

        // server port row
        label = new JLabel("Port");
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0.3;
        add(label, c);

        portField = new JTextField(""+Main.DEFAULT_PORT);
        c.weightx = 0.7;
        c.gridx = 1;
        c.gridy = 1;
        add(portField, c);

        // username row
        label = new JLabel("Uživatel");
        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 0.3;
        add(label, c);

        usernameField = new JTextField();
        c.weightx = 0.7;
        c.gridx = 1;
        c.gridy = 2;
        add(usernameField, c);

        // password row
        label = new JLabel("Heslo");
        c.weightx = 0.3;
        c.gridx = 0;
        c.gridy = 3;
        add(label, c);

        passwordField = new JPasswordField();
        c.weightx = 0.7;
        c.gridx = 1;
        c.gridy = 3;
        add(passwordField, c);

        // login button
        loginButton = new JButton("Přihlásit se");
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        add(loginButton, c);

        // register button
        registerButton = new JButton("Registrovat se");
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 2;
        add(registerButton, c);

        // result text
        resultText = new JLabel("");
        resultText.setForeground(Color.RED);
        c.gridx = 0;
        c.gridy = 6;
        c.gridwidth = 2;
        add(resultText, c);

        // listen for mouse click on login button
        loginButton.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e)
            {
                // clear errors, disable buttons and startup networking
                resultText.setText("");
                setButtonsEnabled(false);
                Networking.getInstance().setHostInfo(hostField.getText(), Integer.parseInt(portField.getText()));
                // connect to host
                Networking.getInstance().startUp();

                // build login packet
                GamePacket gp = new GamePacket(Opcodes.CP_LOGIN.val());

                gp.putString(usernameField.getText());
                gp.putString(new String(passwordField.getPassword()));
                gp.putInt(Main.GAME_VERSION);

                // enqueue for sending
                Networking.getInstance().sendPacket(gp);
            };
        });

        // listen for mouse click on register button
        registerButton.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e)
            {
                // clear errors, disable buttons, startup networking
                resultText.setText("");
                setButtonsEnabled(false);
                Networking.getInstance().setHostInfo(hostField.getText(), Integer.parseInt(portField.getText()));
                // connect to host
                Networking.getInstance().startUp();

                // build register packet
                GamePacket gp = new GamePacket(Opcodes.CP_REGISTER.val());

                gp.putString(usernameField.getText());
                gp.putString(new String(passwordField.getPassword()));
                gp.putInt(Main.GAME_VERSION);

                // enqueue packet for sending
                Networking.getInstance().sendPacket(gp);
            };
        });

        // for now, we will exit when closing this frame
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    /**
     * Sets enabled state of all buttons present
     * @param state enabled state
     */
    private void setButtonsEnabled(boolean state)
    {
        loginButton.setEnabled(state);
        registerButton.setEnabled(state);
    }

    @Override
    public void OnPacketReceived(GamePacket packet)
    {
        if (packet.getOpcode() == Opcodes.SP_LOGIN_RESPONSE.val())
        {
            int rescode = packet.getByte();

            switch (rescode)
            {
                case 0: // OK
                    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                    return;
                case 1: // invalid name
                    resultText.setText("Neexistující uživatel!");
                    break;
                case 2: // wrong password
                    resultText.setText("Nesprávné heslo!");
                    break;
                case 3: // version mismatch
                    resultText.setText("Špatná verze hry!");
                    break;
            }

            setButtonsEnabled(true);
        }
        else if (packet.getOpcode() == Opcodes.SP_REGISTER_RESPONSE.val())
        {
            int rescode = packet.getByte();

            switch (rescode)
            {
                case 0: // OK
                    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                    break;
                case 1: // invalid name
                    resultText.setText("Neexistující uživatel!");
                    break;
                case 2: // name too short
                    resultText.setText("Uživatelské jméno je příliš krátké!");
                    break;
                case 3: // name too long
                    resultText.setText("Uživatelské jméno je příliš dlouhé!");
                    break;
                case 4: // password too short
                    resultText.setText("Heslo je příliš krátké!");
                    break;
                case 5: // password too long
                    resultText.setText("Heslo je příliš dlouhé!");
                    break;
                case 6: // name is taken
                    resultText.setText("Uživatelské jméno již bylo registrováno!");
                    break;
                case 7: // version mismatch
                    resultText.setText("Špatná verze hry!");
                    break;

            }

            setButtonsEnabled(true);
        }
    }

    @Override
    public void OnConnectionStateChanged(ConnectionState state)
    {
        if (state == ConnectionState.CONNECTION_FAILED)
        {
            JOptionPane.showMessageDialog(null, "Spojení se serverem nemohlo být navázáno!", "Nelze se připojit", JOptionPane.ERROR_MESSAGE);
            setButtonsEnabled(true);
        }
    }
}
