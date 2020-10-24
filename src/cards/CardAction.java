package cards;

import java.io.Serializable;

import cards.types.Ability;
import cards.types.Card;
import cards.types.CardType;
import cards.types.CardType.AbilityType;
import cards.types.Unit;
import misc.Player;

public class CardAction implements Serializable{
    private static final long serialVersionUID = 1L;
    private final Card sourceCard;
    private final Player player;
    private final CardType affectedType;
    private final ActionType actionType;
    private final int amount;

    public CardAction(Card sourceCard, Player player, CardType affectedType, ActionType actionType, int amount) {
        this.sourceCard = sourceCard;
        this.player = player;
        this.affectedType = affectedType;
        this.actionType = actionType;
        this.amount = amount;
    }

    public enum ActionType {
        HEALING, DAMAGE, ATTACK_MOD,;
    }

    public boolean isValidTarget(Card card) {
        // player = null means all players
        if (player != null && player.getPlayerId() != card.getPlayerId()) {
            return false;
        }
        if (affectedType == CardType.UNIT && !card.isUnit()) {
            return false;
        }
        if (affectedType == CardType.ABILITY && !card.isAbility()) {
            return false;
        }
        if (affectedType == CardType.ENVIRONMENT && !card.isEnvironment()) {
            return false;
        }
        return true;
    }

    public void applyOn(Card card) {
        if (actionType == ActionType.HEALING) {
            System.out.println("Healing " + card.getName());
            ((Unit) card).deltaHealth(sourceCard, amount);
        } else if (actionType == ActionType.DAMAGE) {
            System.out.println("Damaging " + card.getName());
            ((Unit) card).deltaHealth(sourceCard, -amount);
        } else if (actionType == ActionType.ATTACK_MOD) {
            System.out.println("Modding attack of " + card.getName());
            ((Unit)card).deltaAttack(amount);
        }
        if (sourceCard instanceof Ability) {
            ((Ability) sourceCard).setAffectedCard(card);
            if (((Ability) sourceCard).getType() == AbilityType.ENTANGLING_VINES) {
                ((Unit) card).canAttack = false;
            }
        }
        sourceCard.updated = true;
    }

    public Card getSourceCard() {
        return sourceCard;
    }

}
