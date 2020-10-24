package cards.types;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;

import cards.CardAction;
import cards.CardAction.ActionType;
import cards.types.CardType.EnvironmentType;
import misc.Game;
import misc.GameEvent;
import misc.Phase;
import misc.Slot;

public class Environment extends Card {
    private static final long serialVersionUID = 1L;
    private final EnvironmentType type;

    public Environment(EnvironmentType type) {
        super(CardType.ENVIRONMENT);
        this.type = type;
    }

    @Override
    public void draw(Graphics2D g, int x, int y, float scale, float angle) {
        Color color = g.getColor();
        g.drawString("PID:" + playerId, x + 10, y + 40);
        g.drawString("" + counter, x + 30, y);
        AffineTransform oldTx = g.getTransform();
        g.rotate(Math.toRadians(angle), x + (Game.CARD_WIDTH / 2) * scale, y + (Game.CARD_HEIGHT / 2) * scale);
        g.drawImage(type.getImage(), x, y, (int) (x + Game.CARD_WIDTH * scale), (int) (y + Game.CARD_HEIGHT * scale), 0,
                0, type.getImage().getWidth(null), type.getImage().getHeight(null), null);
        // g.drawString("PID: " + playerId, x + Game.CARD_WIDTH / 4, y + Game.CARD_HEIGHT / 2);
        g.setTransform(oldTx);
        g.setColor(color);
    }

    public void drawBackgroundImage(Graphics2D g) {
        g.drawImage(getBackgroundImage(), 0, 0, Game.WIDTH, Game.HEIGHT, 0, 0, getBackgroundImage().getWidth(null),
                getBackgroundImage().getHeight(null), null);
    }

    @Override
    public Image getImage() {
        return type.getImage();
    }

    public Image getBackgroundImage() {
        return type.getBackgroundImage();
    }

    @Override
    public String getDescription() {
        return type.getDescription();
    }

    @Override
    public String[] getDescriptionLines() {
        return type.getDescriptionLines();
    }

    @Override
    public boolean isPlayable(Game game) {
        return true;
    }

    @Override
    public void reset() {
        // slotPosition = null;
        counter = 0;
        ready = false;
        updated = true;
        destroyed = false;
    }

    @Override
    public String getName() {
        return type.getName();
    }

    @Override
    public Phase getPhase() {
        return Phase.NEUTRAL;
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", type.getName(), slotPosition);
    }

    public EnvironmentType getType() {
        return type;
    }

    @Override
    public void updateWith(Card other) {
        if (!(other instanceof Environment)) {
            return;
        }
        Environment environ = (Environment) other;
        counter = environ.counter;
        ready = environ.ready;

        slotPosition = other.slotPosition;
        destroyed = other.destroyed;

        updated = true;
    }

    private int counter;
    public boolean ready;

    public void onEvent(String string) {
        if (string.equals("ABILITYPLACED")) {
            counter++;
            updated = true;
        }

        if (counter >= 2) {
            ready = true;
            updated = true;
        }
    }

    public CardAction getTriggeredEvent() {
        CardAction action = null;
        if (type == EnvironmentType.POWER_TOTEM) {
            action = new CardAction(null, null, CardType.UNIT, ActionType.DAMAGE, 5);
        }
        return action;
    }

    private ArrayList<Slot> slots = new ArrayList<>();

    public void addAffectedSlots(Slot[] slots) {
        // TODO pass the list of field slots to the card when creating it since they're final anyway
        for (Slot slot: slots) {
            this.slots.add(slot);
        }
        updated = true;
    }

    @Override
    public void onPhase(Phase phase) {
        if (type == EnvironmentType.HEALING_FOREST) {
            if (phase == Phase.END) {
                for (Slot slot: slots) {
                    if (!slot.isBlank()) {
                        Unit unit = slot.getUnit();
                        unit.deltaHealth(this, 1);
                    }
                }
            }
        }
    }

    @Override
    public void onEvent(GameEvent event) {
        // TODO get the list of events it listens to from the enum

    }

}
