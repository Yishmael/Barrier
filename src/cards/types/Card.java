package cards.types;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

import cards.CardAction;
import misc.Game;
import misc.GameEvent;
import misc.GameEvent.GameEventType;
import misc.Phase;
import misc.Slot.SlotPosition;

public abstract class Card implements Serializable {
    private static final long serialVersionUID = 1L;
    public boolean updated, destroyed;
    protected final Rectangle rect = new Rectangle(0, 0, Game.TILE_WIDTH, Game.TILE_HEIGHT);
    protected final UUID uuid;
    protected SlotPosition slotPosition = null;
    protected int playerId = -1;
    private final CardType type;
//    protected ArrayList<GameEventType> triggers = new ArrayList<>();
//    protected CardAction triggerAction;
    public GameEvent lastEvent;
    public String location = "<LOC>"; //temp

    public Card(CardType type) {
        this.type = type;
        uuid = UUID.randomUUID();
    }

    public abstract void draw(Graphics2D g, int x, int y, float scale, float angle);

    public abstract Image getImage();

    public abstract void reset();

    public abstract Phase getPhase();

    public abstract String getName();

    public abstract String getDescription();

    public abstract String[] getDescriptionLines();

    public abstract boolean isPlayable(Game game);

    public abstract void updateWith(Card other);

    public abstract void onPhase(Phase phase);

    public abstract void onEvent(GameEvent event);

    @Override
    public String toString() {
        return type.name();
    }

    public void draw(Graphics2D g) {
        draw(g, rect.x, rect.y, 1, 0);
    }

    public void drawAtAngle(Graphics2D g, float angle) {
        draw(g, rect.x, rect.y, 1, angle);
    }

    public UUID getUUID() {
        return uuid;
    }

    public int getPlayerId() {
        return playerId;
    }

    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }

    public int getX() {
        return rect.x;
    }

    public int getY() {
        return rect.y;
    }

    public void setX(int x) {
        rect.x = x;
    }

    public void setY(int y) {
        rect.y = y;
    }

    public SlotPosition getSlotPosition() {
        return slotPosition;
    }

    public void setSlotPosition(SlotPosition position) {
        this.slotPosition = position;
    }

    public boolean contains(int x, int y) {
        return rect.contains(x, y);
    }

    public boolean isUnit() {
        return type == CardType.UNIT;
    }

    public boolean isAbility() {
        return type == CardType.ABILITY;
    }

    public boolean isEnvironment() {
        return type == CardType.ENVIRONMENT;
    }

//    public boolean triggeredBy(GameEventType eventType) {
//        return triggers.contains(eventType);
//    }

}
