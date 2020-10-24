package net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import misc.LoadedImages;

public final class Utils {
    public static final int BUFFER_SIZE = 100000;

    public static byte[] serialize(Object obj) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(out);
            oos.writeObject(obj);
            return out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object deserialize(byte[] data) {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        try {
            ObjectInputStream ois = new ObjectInputStream(in);
            return ois.readObject();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object deepCopy(Object object) {
        try {
            byte[] data = serialize(object);
            return deserialize(data);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String[] splitText(String text, int maxWidth) {
        ArrayList<String> lines = new ArrayList<>();
        for (String word: text.split(" ")) {
            if (lines.isEmpty()) {
                lines.add(word);
            } else if (lines.get(lines.size() - 1).length() + word.length() < maxWidth) {
                lines.set(lines.size() - 1, lines.get(lines.size() - 1) + " " + word);
            } else {
                lines.add(word);
            }
        }
        return lines.toArray(new String[lines.size()]);
    }

    public static void loadImages() throws IOException {
        LoadedImages.backImage = ImageIO.read(new File("res/" + "back.png"));
        // LoadedImages.reticleImage = ImageIO.read(Game.class.getResource("/reticle.png"));
        LoadedImages.reticleImage = ImageIO.read(new File("res/" + "reticle.png"));
        LoadedImages.projImage = ImageIO.read(new File("res/animations/attack_projectile.png"));
    }

}
