package cards;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

import cards.DeckBuilder.DeckType;
import cards.types.Card;
import cards.types.Unit;

public class CardDatabase implements Serializable {
    private static final long serialVersionUID = 1L;
    private ArrayList<Card> cards = new ArrayList<>();

    public void addCard(Card card) {
        // TODO make it static and add the cards to this list in Card constructor
        cards.add(card);
    }

    public Card getCardByUUID(UUID uuid) {
        for (Card card : cards) {
            if (card.getUUID().equals(uuid)) {
                return card;
            }
        }
        return null;
    }

    public void updateCard(Card card) {
        Card oldCard = getCardByUUID(card.getUUID());
        oldCard.updateWith(card);
    }
}
