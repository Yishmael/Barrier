package misc;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;

public class InputHandler implements KeyListener, MouseListener, MouseMotionListener {
    private final Game game;
    private int mouseX, mouseY;

    public InputHandler(Game game) {
        this.game = game;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (!game.localTurn) {
            return;
        }
        if (e.getButton() == MouseEvent.BUTTON3) {
            System.out.println(e.getX() + ":" + e.getY());
        }
        try {
            game.handleClickAt(e.getButton(), mouseX, mouseY);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        if (game.localPlayer.activeCard != null) {
            game.localPlayer.activeCard.setX(mouseX);
            game.localPlayer.activeCard.setY(mouseY);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (!game.localTurn) {
            return;
        }
        try {
            game.releaseCardToField();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!game.localTurn) {
            return;
        }
        if (game.localPlayer.activeCard != null) {
            game.localPlayer.activeCard.setX(mouseX);
            game.localPlayer.activeCard.setY(mouseY);
        }
        mouseX = e.getX();
        mouseY = e.getY();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        if (game.localPlayer.activeCard != null) {
            game.localPlayer.activeCard.setX(mouseX);
            game.localPlayer.activeCard.setY(mouseY);
        }
        game.hoveredOver(mouseX, mouseY);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            game.stop();
        }
        if (!game.localTurn) {
            return;
        }
        if (e.getKeyChar() == ' ') {
            try {
                if (!game.grid.selected && game.localPlayer.activeCard == null && game.localPlayer.cardAction == null) {
                    game.endTurn();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        try {
            if (e.getKeyChar() == '.') {
                game.localPlayer.drawCards(1);
            } else if (e.getKeyChar() == '`') {
                game.damageAllUnits(null, 3); // damage
            } else if (e.getKeyChar() == '1') {
                game.damageAllUnits(null, -3); // heal
            } else if (e.getKeyChar() == 'd') {
                game.activateDiscard(2); // trigger discard
            } else if (e.getKeyChar() == 'a') {

            } else if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
                game.localPlayer.setPhase(Phase.ATTACK);
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public int getMouseX() {
        return mouseX;
    }

    public int getMouseY() {
        return mouseY;
    }
}
