package cz.zcu.kiv.ups.agarclient.main;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Main game canvas, where all magic happens
 *
 * @author martin.ubl
 */
public class GameCanvas extends JPanel implements ActionListener
{
    private static final long serialVersionUID = 1L;

    /** Update timer */
    private Timer timer;
    /** Update timer delay */
    private final int DELAY = 10;
    /** Last update time to determine change multiplier */
    private long lastUpdateTime = 0;

    /** Coefficient for server-side position values to convert them to drawable units */
    private static final float DRAW_UNIT_COEF = 15.0f;
    /** Base movement coefficient */
    private static final float MOVE_MS_COEF = 0.0075f;

    /** Movement flags - up, left, down, right */
    private static boolean moveDirFlags[] = { false, false, false, false };

    /**
     * Updates local player movement angle
     */
    private void updateMoveAngle()
    {
        GameStorage gsInst = GameStorage.getInstance();
        LocalPlayer pl = gsInst.getLocalPlayer();

        float vx = 0.0f, vy = 0.0f;

        // up
        if (moveDirFlags[0])
            vy -= 1.0f;
        // down
        if (moveDirFlags[2])
            vy += 1.0f;

        // left
        if (moveDirFlags[1])
            vx -= 1.0f;
        // right
        if (moveDirFlags[3])
            vx += 1.0f;

        // if we are moving...
        if (Math.abs(vx) > 0.001f || Math.abs(vy) > 0.001f)
        {
            // switch state if needed
            if (!pl.moving)
            {
                // TODO: send start move packet
                pl.moving = true;
            }

            // calculate angle
            pl.moveAngle = (float) Math.atan(vy / vx);
            // deal with symmetric tangent values
            if (Math.abs(vx) > 0.001f && vx < 0)
                pl.moveAngle = (float) (- Math.PI + pl.moveAngle);
        }
        // stop moving, if needed
        else if (pl.moving)
        {
            // TODO: send stop move packet

            pl.moving = false;
        }
    }

    /**
     * Initializes game canvas
     * @param fr parent frame
     */
    public void initCanvas(JFrame fr)
    {
        // create key adapter to be used
        KeyAdapter kap = new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_W)
                    moveDirFlags[0] = false;
                if (e.getKeyCode() == KeyEvent.VK_A)
                    moveDirFlags[1] = false;
                if (e.getKeyCode() == KeyEvent.VK_S)
                    moveDirFlags[2] = false;
                if (e.getKeyCode() == KeyEvent.VK_D)
                    moveDirFlags[3] = false;

                updateMoveAngle();
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_W)
                    moveDirFlags[0] = true;
                if (e.getKeyCode() == KeyEvent.VK_A)
                    moveDirFlags[1] = true;
                if (e.getKeyCode() == KeyEvent.VK_S)
                    moveDirFlags[2] = true;
                if (e.getKeyCode() == KeyEvent.VK_D)
                    moveDirFlags[3] = true;

                updateMoveAngle();
            }
        };
        // adds key listener to frame
        fr.addKeyListener(kap);

        // creates timer and starts updating
        timer = new Timer(DELAY, this);
        timer.start();
    }

    @Override
    public void paint(Graphics g)
    {
        // repaint parent
        super.paint(g);

        // paint our stuff
        doPaint((Graphics2D)g);

        // synchronize buffers
        Toolkit.getDefaultToolkit().sync();
    }

    /**
     * Internal method for painting everything we need
     * @param g2 2D graphics object
     */
    private void doPaint(Graphics2D g2)
    {
        GameStorage gsInst = GameStorage.getInstance();

        // retrieve everything we need to be drawn
        List<WorldObject> wobjs = gsInst.getVisibleObjects();
        LocalPlayer pl = gsInst.getLocalPlayer();

        // clear background
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, getWidth(), getHeight());

        // paint our player
        g2.setColor(Color.RED);
        g2.fillOval(getWidth() / 2 - pl.size / 2, getHeight() / 2 - pl.size / 2, pl.size, pl.size);

        // get reference points
        float refX = pl.positionX;
        float refY = pl.positionY;

        // draw all objects
        for (WorldObject obj : wobjs)
        {
            if (obj.typeId == 2) // eatable food
                g2.setColor(Color.GREEN);
            else if (obj.typeId == 3) // bonuses
                g2.setColor(Color.BLUE);
            else if (obj.typeId == 4) // traps
                g2.setColor(Color.RED);

            g2.fillOval((int)((obj.positionX - refX)*DRAW_UNIT_COEF), (int)((obj.positionY - refY)*DRAW_UNIT_COEF), 5, 5);
        }
    }

    /**
     * Updates movement of local player and remote players
     */
    private void updateMovement()
    {
        GameStorage gsInst = GameStorage.getInstance();
        LocalPlayer pl = gsInst.getLocalPlayer();

        // calculate millisecond diff between updates
        int diff = (int)(System.currentTimeMillis() - lastUpdateTime);

        // if our player is moving, update movement
        if (pl.moving)
        {
            float nx, ny;

            // determine change using angle, time diff and coefficients
            nx = (float) (Math.cos(pl.moveAngle)*MOVE_MS_COEF*diff);
            ny = (float) (Math.sin(pl.moveAngle)*MOVE_MS_COEF*diff);

            pl.positionX += nx;
            pl.positionY += ny;
        }

        // TODO: other players moving
    }

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
        updateMovement();
        repaint();
        lastUpdateTime = System.currentTimeMillis();
    }

}
