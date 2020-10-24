package cards;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import cards.types.Card;
import cards.types.Unit;
import misc.Game;
import misc.LoadedImages;

public class DetailCard {
    public static final int x = Game.WIDTH - Game.TILE_WIDTH * 2, y = Game.TILE_HEIGHT;
    public static final int WIDTH = Game.TILE_WIDTH * 2, HEIGHT = Game.TILE_HEIGHT * 2;
    public static final int descX = x + 15, descY = y + HEIGHT - 40;
    public static final int hpX = x + 15, hpY = y + HEIGHT - 65;
    public static final int atkX = x + 20, atkY = hpY + 55;

    private Card card;
    private int health, maxHealth, attack;
    private String blockText;

    public DetailCard(Card card) {
        this.card = card;
        if (card != null && card.isUnit()) {
            health = ((Unit) card).getHealth();
            maxHealth = ((Unit) card).getMaxHealth();
            attack = ((Unit) card).getAttack();
            blockText = ((Unit) card).getBlockText();
        }
    }

    public void draw(Graphics2D g) {
        Color color = g.getColor();
        Font font = g.getFont();
        if (card == null) {
            g.drawImage(LoadedImages.backImage, x, y, x + WIDTH, y + HEIGHT, 0, 0,
                    LoadedImages.backImage.getWidth(null), LoadedImages.backImage.getHeight(null), null);
        } else {
            g.drawImage(card.getImage(), x, y, WIDTH, HEIGHT, null);
            g.setFont(new Font("Calibri", Font.PLAIN, 12));
            g.setColor(Color.YELLOW);
            // g.drawString(card.getDescription(), descX, descY);
            String[] lines = card.getDescriptionLines();
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                g.drawString(line, descX, descY + i * 15);
            }
            if (card.isUnit()) {
                g.setFont(new Font("Arial", Font.BOLD, 20));
                g.setColor(Color.YELLOW);
                g.drawString(String.format("%d/%d HP", health, maxHealth), hpX, hpY);
                g.setFont(new Font("Arial", Font.BOLD, 25));
                g.setColor(Color.BLACK);
                g.drawString(attack + "", atkX, atkY);
                g.drawString(blockText + "", atkX + WIDTH - 60, atkY);
            }
        }
        g.setColor(color);
        g.setFont(font);
    }
}
