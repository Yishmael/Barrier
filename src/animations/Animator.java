package animations;

import java.awt.Graphics2D;

public class Animator {

    public AttackAnimation attackAnimation;

    public void draw(Graphics2D g) {
        if (attackAnimation != null) {
            attackAnimation.draw(g);
            if (attackAnimation.finished) {
                attackAnimation = null;
            }
        }
    }
}
