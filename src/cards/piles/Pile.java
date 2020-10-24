package cards.piles;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;

import org.w3c.dom.css.Rect;

import cards.types.Card;
import misc.Game;
import misc.Slot.SlotPosition;

public abstract class Pile {
    public boolean updated;
    protected Rectangle rect;
    protected SlotPosition position;
    protected ArrayList<Card> cards = new ArrayList<>();
    protected ArrayList<Rectangle> cardRects = new ArrayList<>();
    public boolean showCards;

    public Pile(SlotPosition position, boolean flipped) {
        this.position = position;
        rect = new Rectangle(position.getX(), position.getY(), Game.TILE_WIDTH, Game.TILE_HEIGHT);
        if (flipped) {
            if (position.name().contains("DRAW")) {
                int index = Integer.parseInt(position.name().substring(5));
                SlotPosition opposite = SlotPosition.valueOf("DRAW_" + (1 - index));
                rect.x = opposite.getX();
                rect.y = opposite.getY();
            } else if (position.name().contains("DISCARD")) {
                int index = Integer.parseInt(position.name().substring(8));
                SlotPosition opposite = SlotPosition.valueOf("DISCARD_" + (1 - index));
                rect.x = opposite.getX();
                rect.y = opposite.getY();
            }
        }
    }

    public abstract void draw(Graphics2D g);

    public void onClick(int x, int y) {
        Card card = getCardAt(x, y);
        if (showCards) {
            if (card != null) {
                System.out.println(getCardAt(x, y).getName());
                return;
            }
        }
        showCards = !showCards;
        if (cards.isEmpty()) {
            showCards = false;
        } else {
            // String text = cards.stream().map(Card::getName)
            // .collect(Collectors.joining(", ", position.name() + ":[", "]"));
            // System.out.println(text);
        }
    }

    public Card getTopCard() {
        if (cards.isEmpty()) {
            return null;
        }
        return cards.get(0);
    }

    public ArrayList<Card> getCards() {
        return cards;
    }

    public SlotPosition getSlotPosition() {
        return position;
    }

    public boolean contains(int x, int y) {
        return rect.contains(x, y) || showCards && getCardAt(x, y) != null;
    }

    private Card getCardAt(int x, int y) {
        for (int i = 0; i < cardRects.size(); i++) {
            if (cardRects.get(i).contains(x, y)) {
                return cards.get(i);
            }
        }
        return null;
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }

    public void removeCard(Card card) {
        cards.remove(card);
        updated = true;
    }

    public void clear() {
        // called when updating with another pile
        cards.clear();
    }

    public void shuffle() {
        Collections.shuffle(cards);
        updated = true;
    }

    public void add(Card card) {
        cards.add(0, card);
        updated = true;
    }

    public void drawCards(Graphics2D g) {
        cardRects.clear();
        for (int i = 0; i < cards.size(); i++) { // make this happen only on list change
            int x = 200 + Game.CARD_WIDTH * i + 5 * i;
            int y = Game.HEIGHT - 250;
            Rectangle rect = new Rectangle(x, y, Game.CARD_WIDTH, Game.CARD_HEIGHT);
            cardRects.add(rect);
        }
        if (showCards) { // randomize order for draw pile cards preview
            g.setColor(new Color(0, 0, 0, 0.4f));
            g.fillRect(0, 0, Game.WIDTH, Game.HEIGHT);
            for (int i = 0; i < cards.size(); i++) {
                Card card = cards.get(i);
                Rectangle rect = cardRects.get(i);
                g.drawImage(card.getImage(), rect.x, rect.y, rect.x + Game.CARD_WIDTH, rect.y + Game.TILE_HEIGHT, 0, 0,
                        card.getImage().getWidth(null), card.getImage().getHeight(null), null);
            }
        }
    }
}
