package cards.types;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;

import javax.net.ssl.SSLEngineResult.HandshakeStatus;

import cards.CardAction;
import cards.CardAction.ActionType;
import cards.types.CardType.AbilityType;
import cards.types.CardType.AbilityType.AbilityTag;
import misc.Game;
import misc.GameEvent;
import misc.GameEvent.GameEventType;
import misc.Phase;

public class Ability extends Card {
    private static final long serialVersionUID = 1L;
    private final AbilityType type;

    public Ability(AbilityType type) {
        super(CardType.ABILITY);
        this.type = type;
    }

    @Override
    public void draw(Graphics2D g, int x, int y, float scale, float angle) {
        Color color = g.getColor();
        AffineTransform oldTx = g.getTransform();
        g.rotate(Math.toRadians(angle), x + (Game.CARD_WIDTH / 2) * scale, y + (Game.CARD_HEIGHT / 2) * scale);
        g.drawImage(type.getImage(), x, y, (int) (x + Game.CARD_WIDTH * scale), (int) (y + Game.CARD_HEIGHT * scale), 0,
                0, type.getImage().getWidth(null), type.getImage().getHeight(null), null);
        // g.drawString("PID: " + playerId, x + Game.CARD_WIDTH / 4, y + Game.CARD_HEIGHT / 2);
        g.setTransform(oldTx);
        g.setColor(color);
    }

    public AbilityType getType() {
        return type;
    }

    @Override
    public Image getImage() {
        return type.getImage();
    }

    @Override
    public String getName() {
        return type.getName();
    }

    @Override
    public Phase getPhase() {
        return type.getPhase();
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
    public String toString() {
        return String.format("%s (%s)", type.getName(), slotPosition);
    }

    @Override
    public void reset() {
        updated = true;
        destroyed = false;
    }

    @Override
    public boolean isPlayable(Game game) {
        Phase currentPhase = game.localPlayer.getPhase();
        if (type.hasTag(AbilityTag.EQUIP)) {
            if (game.localPlayer.getUnitsOnField().isEmpty()) {
                return false;
            }
        }
        return !(currentPhase == Phase.DEFEND && getPhase() == Phase.ATTACK);
    }

    @Override
    public void updateWith(Card other) {
        if (!(other instanceof Ability)) {
            return;
        }
        slotPosition = other.slotPosition;
        destroyed = other.destroyed;

        updated = true;
    }

    private Card affectedCard;

    public void setAffectedCard(Card card) {
        affectedCard = card;
        // if (type == AbilityType.EXECUTOR_AXE) {
        // affectedCard.triggers.add(GameEventType.UNIT_DESTROYED);
        // affectedCard.triggerAction = new CardAction(this, null, CardType.UNIT,
        // ActionType.ATTACK_MOD, 1);
        // }
    }

    @Override
    public void onPhase(Phase phase) {
        if (type == AbilityType.ENTANGLING_VINES) {
            if (phase == Phase.DRAW) {
                ((Unit) affectedCard).deltaHealth(this, -1);
                ((Unit) affectedCard).canAttack = false;
            }
            if (((Unit) affectedCard).getHealth() < 0) {
                destroyed = true;
            }
        }
    }

    public Card getAffectedCard() {
        return affectedCard;
    }

    @Override
    public void onEvent(GameEvent event) {
        if (type.hasTag(AbilityTag.EQUIP)) {
            if (event.getType() == GameEventType.UNIT_DESTROYED) {
                if (event.getTarget() != null) {
                    if (event.getTarget().getUUID().equals(affectedCard.getUUID())) {
                        // equipped card is destroyed
                        destroyed = true;
                        return;
                    }
                }
            }
            if (type == AbilityType.EXECUTOR_AXE) {
                if (event.getType() == GameEventType.UNIT_DESTROYED) {
                    if (event.getSource() == null) {
                        return;
                    }
                    if (event.getSource().getUUID().equals(affectedCard.getUUID())) {
                        // unit destroyed by the equipped card
                        ((Unit) affectedCard).deltaAttack(1);
                    }
                }
            }
        }

    }

}
