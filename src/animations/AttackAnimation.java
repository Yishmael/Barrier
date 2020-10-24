package animations;

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.Serializable;

import cards.types.Card;
import cards.types.Unit;
import misc.Game;
import misc.LoadedImages;

public class AttackAnimation implements Serializable {
    private static final long serialVersionUID = 1L;
    private Card source, target;
    private int x, y, sx, sy, dx, dy;
    public boolean finished;
    private int imageWidth, imageHeight;

    public AttackAnimation(Card source, Card target) {
        // maybe have the animation keep UUIDs instead of cards
        this.source = source;
        this.target = target;
        updateDirection();
    }

    private long hitTime;

    public void updateDirection() {
        sx = source.getX() + Game.TILE_WIDTH / 2;
        sy = source.getY() + Game.TILE_HEIGHT / 2;
        dx = target.getX() + Game.TILE_WIDTH / 2;
        dy = target.getY() + Game.TILE_HEIGHT / 2;

        imageWidth = LoadedImages.projImage.getWidth(null);
        imageHeight = LoadedImages.projImage.getHeight(null);

        x = sx;
        y = sy;
    }

    public void draw(Graphics2D g) {
        Color color = g.getColor();
        if (Math.hypot(x - dx, y - dy) < 2) {
            if (hitTime == 0) {
                hitTime = System.currentTimeMillis();
            }
            if (System.currentTimeMillis() - hitTime < 200) {
                g.setColor(new Color(1f, 0.6f, 0.6f, 0.8f));
                g.fillRect(target.getX(), target.getY(), Game.TILE_WIDTH, Game.TILE_HEIGHT);
            } else {
                finished = true;
            }
        } else {
            int speed = 5;
            if (y > dy) {
                y -= speed;
            } else if (y < dy) {
                y += speed;
            }
            if (x > dx) {
                x -= speed;
            } else if (x < dx) {
                x += speed;
            }
            int projSize = 50;
            g.drawImage(LoadedImages.projImage, x - projSize / 2, y - projSize / 2, x + projSize / 2, y + projSize / 2,
                    0, 0, imageWidth, imageHeight, null);
            g.setColor(color);
        }
    }

    public Card getSource() {
        return source;
    }

    public Card getTarget() {
        return target;
    }

}
