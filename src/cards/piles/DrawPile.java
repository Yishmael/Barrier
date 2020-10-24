package cards.piles;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import cards.types.Card;
import misc.Game;
import misc.Slot.SlotPosition;

public class DrawPile extends Pile {
    public DrawPile(SlotPosition position, boolean flipped) {
        super(position, flipped);
    }

    @Override
    public void draw(Graphics2D g) {
        Color color = g.getColor();
        g.drawRect(rect.x, rect.y, rect.width + 2, rect.height + 2);
        g.setColor(Color.WHITE);
        for (int i = 0; i < cards.size(); i++) {
            Card topCard = cards.get(0);
            int yOffset = -Math.min(20, i * 1);
            // g.drawImage(Game.backImage, rect.x, rect.y + yOffset, rect.x + Game.CARD_WIDTH,
            // rect.y + yOffset + Game.CARD_HEIGHT, 0, 0, Game.backImage.getWidth(null),
            // Game.backImage.getHeight(null), null);
            g.drawImage(topCard.getImage(), rect.x, rect.y + yOffset, rect.x + Game.TILE_WIDTH,
                    rect.y + yOffset + Game.TILE_HEIGHT, 0, 0, topCard.getImage().getWidth(null),
                    topCard.getImage().getHeight(null), null);
        }
        super.drawCards(g);
        g.drawString("Draw pile: " + cards.size(), rect.x + 5, rect.y + Game.TILE_HEIGHT - 15);
        g.setColor(color);
    }
}
