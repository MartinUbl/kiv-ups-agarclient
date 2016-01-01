package cz.zcu.kiv.ups.agarclient.main;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import cz.zcu.kiv.ups.agarclient.enums.Opcodes;
import cz.zcu.kiv.ups.agarclient.network.GamePacket;
import cz.zcu.kiv.ups.agarclient.network.Networking;

public class NewRoomDialog extends JDialog
{
    private static final long serialVersionUID = 1L;

    private JLabel resultText;
    private JTextField roomNameField;
    private JComboBox<RoomSizeItem> roomSizeField;
    private JButton cancelButton, OKButton;

    private class RoomSizeItem
    {
        private String name;
        private int size;
        public RoomSizeItem(int size, String name) { this.size = size; this.name = name; };
        public String toString() { return name; };
        public int getSize() { return size; };
    }

    public NewRoomDialog(JFrame parent)
    {
        super(parent, true);
    }

    public void initComponents()
    {
        // set size
        setPreferredSize(new Dimension(500, 500));
        setSize(getPreferredSize());
        setTitle("KIV/UPS: Agar.io - New room");

        // choose layout
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel label;

        // room name
        label = new JLabel("Název místnosti");
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.3;
        add(label, c);

        roomNameField = new JTextField("Nová místnost");
        c.weightx = 0.7;
        c.gridx = 1;
        c.gridy = 0;
        add(roomNameField, c);

        // size
        label = new JLabel("Velikost");
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0.3;
        add(label, c);

        roomSizeField = new JComboBox<RoomSizeItem>(new RoomSizeItem[]{ new RoomSizeItem(25, "Nejmenší"), new RoomSizeItem(50, "Malá"), new RoomSizeItem(100, "Střední"), new RoomSizeItem(200, "Velká"), new RoomSizeItem(500, "Obrovská") });
        c.weightx = 0.7;
        c.gridx = 1;
        c.gridy = 1;
        add(roomSizeField, c);

        // room name
        label = new JLabel("Kapacita");
        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 0.3;
        add(label, c);

        SpinnerModel model = new SpinnerNumberModel(20, 2, 50, 1);
        final JSpinner spinner = new JSpinner(model);
        c.weightx = 0.7;
        c.gridx = 1;
        c.gridy = 2;
        add(spinner, c);

        // Create button
        OKButton = new JButton("Vytvořit");
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        add(OKButton, c);

        // Cancel button
        cancelButton = new JButton("Zrušit");
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        add(cancelButton, c);

        // result text
        resultText = new JLabel("");
        resultText.setForeground(Color.RED);
        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 2;
        add(resultText, c);

        final JDialog that = this;

        // listen for mouse click on login button
        OKButton.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e)
            {
                RoomSizeItem sel = (RoomSizeItem)(roomSizeField.getSelectedItem());
                if (sel == null)
                    return;

                String roomName = roomNameField.getText();

                GamePacket gp = new GamePacket(Opcodes.CP_CREATE_ROOM.val());
                gp.putString(roomName);
                gp.putInt(Integer.parseInt(((JSpinner.DefaultEditor)spinner.getEditor()).getTextField().getText()));
                gp.putInt(sel.getSize());

                Networking.getInstance().sendPacket(gp);

                that.setVisible(false);
            };
        });

        // listen for mouse click on register button
        cancelButton.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e)
            {
                that.setVisible(false);
            };
        });

        setLocationRelativeTo(null);
    }
}
