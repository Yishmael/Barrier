package cards.types;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import javax.imageio.ImageIO;

import misc.Phase;
import net.Utils;

public enum CardType implements Serializable {
    ABILITY, UNIT, ENVIRONMENT;

    public enum UnitType {
        PROTECTOR(5, 10, 2, new UnitTag[] {UnitTag.BURNING}, "A heavily armored unit.", "protector.png"),
        ANTLION(3, 6, 2, new UnitTag[] {}, "A larva with a robust fusiform body.", "antlion.png"),;

        public enum UnitTag {
            RANGED, BURNING,;
        }

        private int health, attack, block;
        private String description;
        private UnitTag[] tags;
        private Image image;

        UnitType(int health, int attack, int block, UnitTag[] tags, String description, String path) {
            this.health = health;
            this.attack = attack;
            this.block = block;
            this.tags = tags;
            this.description = description;
            if (path != null) {
                try {
                    this.image = ImageIO.read(new File("res/" + path));
                    // this.image = ImageIO.read(Game.class.getResource(path));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public boolean hasTag(UnitTag tag) {
            for (UnitTag t: tags) {
                if (tag == t) {
                    return true;
                }
            }
            return false;
        }

        public Unit instantiate() {
            return new Unit(this);
        }

        public Phase getPhase() {
            return Phase.DEFEND;
        }

        public Image getImage() {
            return image;
        }

        public int getHealth() {
            return health;
        }

        public int getAttack() {
            return attack;
        }

        public int getBlock() {
            return block;
        }

        public String getName() {
            return name().toCharArray()[0] + name().substring(1).toLowerCase();
        }

        public String getDescription() {
            return description;
        }

        public String[] getDescriptionLines() {
            return Utils.splitText(description, 23);
        }

        public CardType getCardType() {
            return CardType.UNIT;
        }
    }

    public enum AbilityType {
        BLOCK(Phase.DEFEND, new AbilityTag[] {}, "Increase block of all allied units by 2.", "block.png"),
        EARTHQUAKE(Phase.ATTACK, new AbilityTag[] {}, "Deal 2 damage to all summoned units.", "earthquake.png"),
        HEALING_HAND(
                Phase.DEFEND,
                new AbilityTag[] { AbilityTag.EQUIP },
                "Restore 2 HP to the target unit.",
                "healing_hand.png"),
        AGGRESSION(Phase.NEUTRAL, new AbilityTag[] {}, "Enter attack phase.", "aggression.png"),
        ENTANGLING_VINES(
                Phase.NEUTRAL,
                new AbilityTag[] { AbilityTag.EQUIP },
                "Equipped unit cannot attack and loses 1 HP each turn.",
                "entangling_vines.png"),
        EXECUTOR_AXE(
                Phase.NEUTRAL,
                new AbilityTag[] { AbilityTag.EQUIP },
                "Increase the unit's attack by 3, and whenever it kills a unit, it gains an additional 1 attack.",
                "executor_axe.png"),;

        private Phase phase;
        private AbilityTag[] tags;
        private String description;
        private Image image;

        AbilityType(Phase phase, AbilityTag[] tags, String description, String path) {
            this.phase = phase;
            this.tags = tags;
            this.description = description;
            if (path != null) {
                try {
                    this.image = ImageIO.read(new File("res/" + path));
                    // this.image = ImageIO.read(Game.class.getResource(path));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public boolean hasTag(AbilityTag tag) {
            for (AbilityTag t: tags) {
                if (tag == t) {
                    return true;
                }
            }
            return false;
        }

        public enum AbilityTag {
            EQUIP,
        }

        public Ability instantiate() {
            return new Ability(this);
        }

        public String getName() {
            return name().toCharArray()[0] + name().substring(1).toLowerCase();
        }

        public String getDescription() {
            return description;
        }

        public String[] getDescriptionLines() {
            return Utils.splitText(description, 23);
        }

        public Image getImage() {
            return image;
        }

        public Phase getPhase() {
            return phase;
        }

        public CardType getCardType() {
            return CardType.ABILITY;
        }
    }

    public enum EnvironmentType {
        HEALING_FOREST(
                "All units restore 1 HP at the end of their owner's turn.",
                "healing_forest.png",
                "healing_forest.png"),
        INFERNO("Burning is twice as effective.", "inferno.png", "inferno.png"),
        POWER_TOTEM(
                "Whenever a Spell is played, this card gains 1 charge. Upon reaching 5 charges, it explodes, releasing all charges and dealing 5 normal damage to all units.",
                "power_totem.png",
                "power_totem.png"),;

        private String description;
        private Image image, bgImage;

        EnvironmentType(String description, String path, String bgImagePath) {
            this.description = description;
            if (path != null) {
                try {
                    image = ImageIO.read(new File("res/" + path));
                    // this.image = ImageIO.read(Game.class.getResource(path));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bgImagePath != null) {
                try {
                    bgImage = ImageIO.read(new File("res/fields/" + bgImagePath));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public Environment instantiate() {
            return new Environment(this);
        }

        public String getName() {
            return name().toCharArray()[0] + name().substring(1).toLowerCase();
        }

        public String getDescription() {
            return description;
        }

        public String[] getDescriptionLines() {
            return Utils.splitText(description, 23);
        }

        public Image getImage() {
            return image;
        }

        public Image getBackgroundImage() {
            return bgImage;
        }

        public CardType getCardType() {
            return CardType.ENVIRONMENT;
        }
    }
}
