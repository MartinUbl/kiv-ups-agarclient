package cz.zcu.kiv.ups.agarclient.main;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.Timer;

import cz.zcu.kiv.ups.agarclient.enums.Opcodes;
import cz.zcu.kiv.ups.agarclient.network.GamePacket;
import cz.zcu.kiv.ups.agarclient.network.Networking;

/**
 * Main game canvas, where all magic happens
 *
 * @author martin.ubl
 */
public strictfp class GameCanvas extends JPanel implements ActionListener
{
    private static final long serialVersionUID = 1L;

    /** Update timer */
    private Timer timer;
    /** Update timer delay */
    private final int DELAY = 10;
    /** Last update time to determine change multiplier */
    private long lastUpdateTime = 0;
    /** Last update of movement to server */
    private long lastHeartbeatTime = 0;
    /** Flag for changing movement angle */
    private boolean movementAngleChanged = false;

    /** Coefficient for server-side position values to convert them to drawable units */
    private static final float DRAW_UNIT_COEF = 30.0f; // 30.0f
    /** Base movement coefficient */
    private static final float MOVE_MS_COEF = 0.0065f; // 0.0065f
    /** Time delay between two movement updates */
    private static final long HEARTBEAT_TIME_DELAY = 500;
    /** Player size coefficient */
    private static final float PLAYER_SIZE_COEF = 0.3f;

    /** Movement flags - up, left, down, right */
    private static boolean moveDirFlags[] = { false, false, false, false };

    /** Flag for "we have been eaten" */
    private boolean weAreDead = false;

    /** parent frame */
    private GameWindow parentFrame = null;

    /**
     * Updates local player movement angle
     */
    private void updateMoveAngle()
    {
        if (weAreDead)
            return;

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
                // send start move packet
                sendMoveState(true, pl);
                pl.moving = true;
            }

            float oldAngle = pl.moveAngle;

            // calculate angle
            pl.moveAngle = (float) Math.atan(vy / vx);
            // deal with symmetric tangent values
            if (Math.abs(vx) > 0.001f && vx < 0)
                pl.moveAngle = (float) (- Math.PI + pl.moveAngle);

            if (Math.abs(oldAngle - pl.moveAngle) > 0.01f)
                movementAngleChanged = true;
        }
        // stop moving, if needed
        else if (pl.moving)
        {
            // send stop move packet
            sendMoveState(false, pl);
            pl.moving = false;
        }
    }

    /**
     * Sets dead flag
     * @param state dead state
     */
    public void setWeAreDead(boolean state)
    {
        weAreDead = state;
    }

    /**
     * Retrieves dead state
     * @return dead state
     */
    public boolean getWeAreDead()
    {
        return weAreDead;
    }

    /**
     * Sends move state to server
     * @param moving is player moving?
     * @param pl player we are talking about
     */
    private void sendMoveState(boolean moving, LocalPlayer pl)
    {
        if (weAreDead)
            return;

        GamePacket gp;
        if (moving)
            gp = new GamePacket(Opcodes.CP_MOVE_START.val());
        else
            gp = new GamePacket(Opcodes.CP_MOVE_STOP.val());

        gp.putFloat(pl.positionX);
        gp.putFloat(pl.positionY);
        gp.putFloat(pl.moveAngle);

        Networking.getInstance().sendPacket(gp);
    }

    /**
     * Sends move heartbeat to server
     * @param pl local player
     */
    private void sendMoveHeartbeat(LocalPlayer pl)
    {
        if (weAreDead)
            return;

        GamePacket gp = new GamePacket(Opcodes.CP_MOVE_HEARTBEAT.val());

        gp.putFloat(pl.positionX);
        gp.putFloat(pl.positionY);

        Networking.getInstance().sendPacket(gp);
    }

    /**
     * Sends information about move direction change
     * @param pl local player
     */
    private void sendMoveDirection(LocalPlayer pl)
    {
        if (weAreDead)
            return;

        GamePacket gp = new GamePacket(Opcodes.CP_MOVE_DIRECTION.val());

        gp.putFloat(pl.moveAngle);

        Networking.getInstance().sendPacket(gp);
    }

    /**
     * Initializes game canvas
     * @param fr parent frame
     */
    public void initCanvas(GameWindow fr)
    {
        parentFrame = fr;

        // create key adapter to be used
        KeyAdapter kap = new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_W)
                    moveDirFlags[0] = false;
                else if (e.getKeyCode() == KeyEvent.VK_A)
                    moveDirFlags[1] = false;
                else if (e.getKeyCode() == KeyEvent.VK_S)
                    moveDirFlags[2] = false;
                else if (e.getKeyCode() == KeyEvent.VK_D)
                    moveDirFlags[3] = false;

                updateMoveAngle();
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_W)
                    moveDirFlags[0] = true;
                else if (e.getKeyCode() == KeyEvent.VK_A)
                    moveDirFlags[1] = true;
                else if (e.getKeyCode() == KeyEvent.VK_S)
                    moveDirFlags[2] = true;
                else if (e.getKeyCode() == KeyEvent.VK_D)
                    moveDirFlags[3] = true;

                updateMoveAngle();

                // when we are dead, use space to restart the game
                if (weAreDead && e.getKeyCode() == KeyEvent.VK_SPACE)
                {
                    // this will send "new world" request, just as when the game starts
                    parentFrame.initGame();
                }
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
        synchronized (Networking.getInstance())
        {
            doPaint((Graphics2D)g);
        }

        // synchronize buffers
        Toolkit.getDefaultToolkit().sync();
    }

    /**
     * Internal method for painting everything we need
     * @param g2 2D graphics object
     */
    private void doPaint(Graphics2D g2)
    {
        // turn antialiasing on
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // when we are dead, just display, what we can do
        if (weAreDead)
        {
            g2.setColor(Color.GRAY);
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(Color.BLACK);
            String toDraw = "Game Over :-(";
            g2.drawString(toDraw, (getWidth() - g2.getFontMetrics().stringWidth(toDraw)) / 2, getHeight() / 2);

            toDraw = "Mezerník - opakovat hru";
            g2.drawString(toDraw, (getWidth() - g2.getFontMetrics().stringWidth(toDraw)) / 2, getHeight() / 2 + 30);
            toDraw = "Escape - zpátky do lobby";
            g2.drawString(toDraw, (getWidth() - g2.getFontMetrics().stringWidth(toDraw)) / 2, getHeight() / 2 + 45);
            return;
        }

        GameStorage gsInst = GameStorage.getInstance();

        // retrieve everything we need to be drawn
        List<WorldObject> wobjs = gsInst.getVisibleObjects();
        List<PlayerObject> plrs = gsInst.getVisiblePlayers();
        LocalPlayer pl = gsInst.getLocalPlayer();

        // clear background
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, getWidth(), getHeight());

        int plsize;

        // get reference points
        float refX = pl.positionX - (getWidth() / 2) / DRAW_UNIT_COEF;
        float refY = pl.positionY - (getHeight() / 2) / DRAW_UNIT_COEF;

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

        // draw all players
        for (PlayerObject plr : plrs)
        {
            g2.setColor(new Color(plr.param));
            plsize = (int)(plr.size * PLAYER_SIZE_COEF);
            g2.fillOval((int)((plr.positionX - refX)*DRAW_UNIT_COEF) - plsize / 2, (int)((plr.positionY - refY)*DRAW_UNIT_COEF) - plsize / 2, plsize, plsize);

            g2.drawString(plr.name, (int)((plr.positionX - refX)*DRAW_UNIT_COEF) - g2.getFontMetrics().stringWidth(plr.name) / 2, (int)((plr.positionY - refY)*DRAW_UNIT_COEF) - plsize / 2 - 4);
        }

        // paint our player
        plsize = (int)(pl.size * PLAYER_SIZE_COEF);
        g2.setColor(new Color(pl.param));
        g2.fillOval((getWidth() - plsize) / 2, (getHeight() - plsize) / 2, plsize, plsize);
        g2.drawString(pl.name, (getWidth() - g2.getFontMetrics().stringWidth(pl.name)) / 2, (getHeight() - plsize) / 2 - 4);
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

            if (pl.positionX < 0)
                pl.positionX = 0;
            if (pl.positionY < 0)
                pl.positionY = 0;

            if (pl.positionX > Main.getMapWidth())
                pl.positionX = Main.getMapWidth();
            if (pl.positionY > Main.getMapHeight())
                pl.positionY = Main.getMapHeight();

            // update movement angle if needed
            if (movementAngleChanged)
            {
                sendMoveDirection(pl);
                movementAngleChanged = false;
            }

            // send movement update if needed
            if (System.currentTimeMillis() - lastHeartbeatTime > HEARTBEAT_TIME_DELAY)
            {
                sendMoveHeartbeat(pl);
                lastHeartbeatTime = System.currentTimeMillis();
            }
        }

        // other players moving
        List<PlayerObject> plrs = gsInst.getVisiblePlayers();

        for (PlayerObject plr : plrs)
        {
            if (plr.moving)
            {
                float nx, ny;

                // determine change using angle, time diff and coefficients
                nx = (float) (Math.cos(plr.moveAngle)*MOVE_MS_COEF*diff);
                ny = (float) (Math.sin(plr.moveAngle)*MOVE_MS_COEF*diff);

                plr.positionX += nx;
                plr.positionY += ny;

                if (plr.positionX < 0)
                    plr.positionX = 0;
                if (plr.positionY < 0)
                    plr.positionY = 0;

                if (plr.positionX > Main.getMapWidth())
                    plr.positionX = Main.getMapWidth();
                if (plr.positionY > Main.getMapHeight())
                    plr.positionY = Main.getMapHeight();
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
        synchronized (Networking.getInstance())
        {
            if (!weAreDead)
                updateMovement();
        }

        // paint has its own lock
        repaint();

        lastUpdateTime = System.currentTimeMillis();
    }

}
