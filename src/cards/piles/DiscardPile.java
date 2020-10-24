package cards.piles;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import cards.types.Card;
import misc.Game;
import misc.Slot.SlotPosition;

public class DiscardPile extends Pile {

    public DiscardPile(SlotPosition position, boolean flipped) {
        super(position, flipped);
    }

    @Override
    public void draw(Graphics2D g) {
        Color color = g.getColor();
        g.setColor(Color.WHITE);
        g.drawRect(rect.x, rect.y, rect.width + 2, rect.height + 2);
        for (int i = 0; i < cards.size(); i++) {
            Card topCard = cards.get(0);
            int yOffset = -Math.min(20, i * 1);
            g.drawImage(topCard.getImage(), rect.x, rect.y + yOffset, rect.x + Game.TILE_WIDTH,
                    rect.y + yOffset + Game.TILE_HEIGHT, 0, 0, topCard.getImage().getWidth(null),
                    topCard.getImage().getHeight(null), null);
        }
        super.drawCards(g);
        g.drawString("Discard pile: " + cards.size(), rect.x + 5, rect.y + Game.TILE_HEIGHT - 15);
        g.setColor(color);
    }
}
