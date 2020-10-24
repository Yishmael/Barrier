package misc;

import java.io.Serializable;

import cards.types.Card;

public class GameEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    private GameEventType type;
    private Card source, target;

    public GameEvent(GameEventType type, Card source, Card target) {
        this.type = type;
        this.source = source;
        this.target = target;
    }

    public GameEventType getType() {
        return type;
    }

    public Card getSource() {
        return source;
    }

    public Card getTarget() {
        return target;
    }

    public enum GameEventType {
        CARD_PLAYED, UNIT_ATTACKED, UNIT_DESTROYED, BURNING_TICK,;
    }
}
