package misc;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.io.Serializable;

import cards.types.Ability;
import cards.types.Card;
import cards.types.Environment;
import cards.types.Unit;

public class Slot implements Serializable {
    public enum SlotPosition {
        ENVIRON_0(Game.TILE_WIDTH * 2 - 3, 250),
        ENVIRON_1(Game.TILE_WIDTH * 7 + 3, 250),
        ABILITY_0(Game.TILE_WIDTH * 3 - Game.TILE_WIDTH / 2, Game.TILE_HEIGHT),
        ABILITY_1(Game.TILE_WIDTH * 4 - Game.TILE_WIDTH / 2, Game.TILE_HEIGHT),
        ABILITY_2(Game.TILE_WIDTH * 5 - Game.TILE_WIDTH / 2, Game.TILE_HEIGHT),
        ABILITY_3(Game.TILE_WIDTH * 6 - Game.TILE_WIDTH / 2, Game.TILE_HEIGHT),
        ABILITY_4(Game.TILE_WIDTH * 7 - Game.TILE_WIDTH / 2, Game.TILE_HEIGHT),
        ABILITY_5(Game.TILE_WIDTH * 3 - Game.TILE_WIDTH / 2, Game.TILE_HEIGHT * 4),
        ABILITY_6(Game.TILE_WIDTH * 4 - Game.TILE_WIDTH / 2, Game.TILE_HEIGHT * 4),
        ABILITY_7(Game.TILE_WIDTH * 5 - Game.TILE_WIDTH / 2, Game.TILE_HEIGHT * 4),
        ABILITY_8(Game.TILE_WIDTH * 6 - Game.TILE_WIDTH / 2, Game.TILE_HEIGHT * 4),
        ABILITY_9(Game.TILE_WIDTH * 7 - Game.TILE_WIDTH / 2, Game.TILE_HEIGHT * 4),
        UNIT_0(Game.TILE_WIDTH * 3, Game.TILE_HEIGHT * 2),
        UNIT_1(Game.TILE_WIDTH * 4, Game.TILE_HEIGHT * 2),
        UNIT_2(Game.TILE_WIDTH * 5, Game.TILE_HEIGHT * 2),
        UNIT_3(Game.TILE_WIDTH * 6, Game.TILE_HEIGHT * 2),
        UNIT_4(Game.TILE_WIDTH * 3, Game.TILE_HEIGHT * 3),
        UNIT_5(Game.TILE_WIDTH * 4, Game.TILE_HEIGHT * 3),
        UNIT_6(Game.TILE_WIDTH * 5, Game.TILE_HEIGHT * 3),
        UNIT_7(Game.TILE_WIDTH * 6, Game.TILE_HEIGHT * 3),
        DRAW_0(Game.TILE_WIDTH * 8, 0),
        DRAW_1(Game.TILE_WIDTH, Game.TILE_HEIGHT * 5),
        DISCARD_0(Game.TILE_WIDTH, 0),
        DISCARD_1(Game.TILE_WIDTH * 8, Game.TILE_HEIGHT * 5);

        private int x, y;

        private SlotPosition(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }

    private static final long serialVersionUID = 1L;
    public Rectangle rect;
    private Card card;
    private SlotPosition position;
    private boolean flipped;

    private int playerId = -1;

    public Slot(SlotPosition position, boolean flipped) {
        this(null, position, flipped);
    }

    public Slot(Card card, SlotPosition position, boolean flipped) {
        this.flipped = flipped;
        this.card = card;
        this.position = position;
        rect = new Rectangle(0, 0, Game.TILE_WIDTH, Game.TILE_HEIGHT);
        if (position != null) {
            setCoordinates();
        }
    }

    public void clear() {
        card = null;
    }

    public boolean contains(int x, int y) {
        return rect.contains(x, y);
    }

    public void draw(Graphics2D g) {
        drawAtAngle(g, 0);
    }

    public void drawAtAngle(Graphics2D g, int angle) {
        Color color = g.getColor();
        Stroke stroke = g.getStroke();
        g.setStroke(new BasicStroke(3));
        if (position.name().contains("UNIT")) {
            g.setColor(new Color(0xc18855));
        } else if (position.name().contains("ABILITY")) {
            g.setColor(new Color(0x80ff00));
        } else if (position.name().contains("ENVIRON")) {
            g.setColor(new Color(0x80ffee));
        }
        g.drawString("PID:" + playerId, rect.x + 10, rect.y + 40);
        g.draw(rect);
        // g.fillRect(rect.x + 5, rect.y + 5, rect.width - 5, rect.height - 5);
        if (card != null) {
            card.drawAtAngle(g, angle);
        }
        g.setStroke(stroke);
        g.setColor(color);
    }

    public Card getCard() {
        return card;
    }

    public Unit getUnit() {
        return (Unit) card;
    }

    public Ability getAbility() {
        return (Ability) card;
    }

    public Environment getEnvironment() {
        return (Environment) card;
    }

    public int getPlayerId() {
        return playerId;
    }

    public SlotPosition getPosition() {
        return position;
    }

    public boolean isBlank() {
        return card == null;
    }

    public void setCard(Card card) {
        this.card = card;
        this.card.setSlotPosition(position);
        this.card.setX(rect.x);
        this.card.setY(rect.y);
    }

    public void setCoordinates() {
        rect.x = position.x;
        rect.y = position.y;
        if (flipped) {
            if (position.name().contains("ENVIRON")) {
                int index = Integer.parseInt(position.name().substring(8));
                SlotPosition opposite = SlotPosition.valueOf("ENVIRON_" + (1 - index));
                rect.x = opposite.x;
                rect.y = opposite.y;
            } else if (position.name().contains("UNIT")) {
                int index = Integer.parseInt(position.name().substring(5));
                SlotPosition opposite = SlotPosition.valueOf("UNIT_" + (7 - index));
                rect.x = opposite.x;
                rect.y = opposite.y;
            } else if (position.name().contains("ABILITY")) {
                int index = Integer.parseInt(position.name().substring(8));
                SlotPosition opposite = SlotPosition.valueOf("ABILITY_" + (9 - index));
                rect.x = opposite.x;
                rect.y = opposite.y;
            } else if (position.name().contains("DISCARD")) {
                int index = Integer.parseInt(position.name().substring(8));
                SlotPosition opposite = SlotPosition.valueOf("DISCARD_" + (1 - index));
                rect.x = opposite.x;
                rect.y = opposite.y;
                System.err.println("FLIPPING DISCARD PILE" + position);
            }
        }
        if (card != null) {
            card.setX(rect.x);
            card.setY(rect.y);
        }
    }

    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }

    @Override
    public String toString() {
        return String.format("Slot(%s)", position);
    }

}
