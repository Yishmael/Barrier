package misc;

import java.awt.Image;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public enum Phase {
    ATTACK("attack.png"), DEFEND("defend.png"),
    DRAW(null), END(null), NEUTRAL(null);

    private Image image;

    Phase(String path) {
        if (path != null) {
            try {
                this.image = ImageIO.read(new File("res/" +path));
                // this.image = ImageIO.read(Game.class.getResource(path));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Image getImage() {
        return image;
    }
}