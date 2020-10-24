package cards;

import java.util.ArrayList;
import java.util.Collections;

import cards.types.Card;
import cards.types.CardType;
import cards.types.CardType.AbilityType;
import cards.types.CardType.EnvironmentType;
import cards.types.CardType.UnitType;

public class DeckBuilder {

    public enum DeckType {
        ONE(
                new UnitType[] { UnitType.PROTECTOR, },
                new AbilityType[] { AbilityType.BLOCK, },
                new EnvironmentType[] { EnvironmentType.INFERNO, }),
        TWO(
                new UnitType[] { UnitType.PROTECTOR, UnitType.PROTECTOR, UnitType.ANTLION },
                new AbilityType[] { AbilityType.BLOCK, AbilityType.EARTHQUAKE, AbilityType.HEALING_HAND,
                        AbilityType.ENTANGLING_VINES, AbilityType.EXECUTOR_AXE },
                new EnvironmentType[] { EnvironmentType.HEALING_FOREST, EnvironmentType.POWER_TOTEM }),
        THREE(
                new UnitType[] { UnitType.PROTECTOR, UnitType.PROTECTOR, UnitType.PROTECTOR, UnitType.PROTECTOR,
                        UnitType.PROTECTOR, UnitType.PROTECTOR, UnitType.PROTECTOR, UnitType.PROTECTOR },
                new AbilityType[] { AbilityType.BLOCK, AbilityType.BLOCK, AbilityType.BLOCK, AbilityType.BLOCK,
                        AbilityType.BLOCK, AbilityType.BLOCK, AbilityType.BLOCK, AbilityType.BLOCK, AbilityType.BLOCK,
                        AbilityType.BLOCK, AbilityType.EARTHQUAKE, AbilityType.EARTHQUAKE, AbilityType.EARTHQUAKE,
                        AbilityType.EARTHQUAKE, AbilityType.EARTHQUAKE, AbilityType.EARTHQUAKE,
                        AbilityType.EARTHQUAKE },
                new EnvironmentType[] {}),
        EMPTY(new UnitType[] {}, new AbilityType[] {}, new EnvironmentType[] {}),;

        private UnitType[] units;
        private AbilityType[] abilities;
        private EnvironmentType[] environs;

        DeckType(UnitType[] units, AbilityType[] abilities, EnvironmentType[] environs) {
            this.units = units;
            this.abilities = abilities;
            this.environs = environs;
        }

        public UnitType[] getUnits() {
            return units;
        }

        public AbilityType[] getAbilities() {
            return abilities;
        }

        public EnvironmentType[] getEnvirons() {
            return environs;
        }
    }

//    public static Card[] getDeck(DeckType type) {
//        ArrayList<Card> cards = new ArrayList<>();
//        for (UnitType unitType: type.units) {
//            cards.add(unitType.instantiate());
//        }
//        for (AbilityType abilityType: type.abilities) {
//            cards.add(abilityType.instantiate());
//        }
//        for (EnvironmentType environType: type.environs) {
//            cards.add(environType.instantiate());
//        }
//        Collections.shuffle(cards);
//        return cards.toArray(new Card[cards.size()]);
//    }
}
