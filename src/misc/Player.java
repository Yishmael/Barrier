package misc;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Stroke;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import cards.CardAction;
import cards.CardAction.ActionType;
import cards.piles.DiscardPile;
import cards.piles.DrawPile;
import cards.types.Ability;
import cards.types.Card;
import cards.types.CardType;
import cards.types.CardType.AbilityType;
import cards.types.CardType.UnitType;
import cards.types.Environment;
import cards.types.Unit;
import misc.Game.Side;
import misc.Slot.SlotPosition;
import net.packets.Packet;
import net.packets.Packet.PacketType;

public class Player {

    public final Slot[] unitSlots = new Slot[4];
    public final Slot[] abilitySlots = new Slot[5];
    public Slot environmentSlot;
    public DrawPile drawPile;
    public DiscardPile discardPile;
    public Card activeCard = null;

    private Phase phase;
    private int playerId = -1;
    private Game game;
    private Side side;
    private ArrayList<Card> handCards = new ArrayList<>();
    public CardAction cardAction;
    private int handOffsetX = 100, handOffsetY = 0;

    public Player(Game game, Side side) {
        this.game = game;
        this.side = side;
        if (this.side == Side.BOTTOM) {
            handOffsetY = Game.HEIGHT - 70;
        } else {
            handOffsetY = 5;
        }
        initSlots();
    }

    public void placeCardOnField(Card card, SlotPosition position) throws IOException {
        boolean played = false;
        if (phase == Phase.DEFEND && card.getPhase() == Phase.ATTACK) {
            return;
        }

        if (card instanceof Unit) {
            Unit unit = (Unit) card;
            UnitType unitType = unit.getType();
            game.placeUnit(unit, position);
            switch (unitType) {
            case PROTECTOR:
                break;
            case ANTLION:
                break;
            }
            setPhase(unitType.getPhase());
            played = true;
        } else if (card instanceof Ability) {
            Ability ability = (Ability) card;
            AbilityType abilityType = ((Ability) card).getType();
            switch (abilityType) {
            case EARTHQUAKE:
                game.damageAllUnits(ability, 2);
                break;
            case BLOCK:
                game.grantMassBlock(2, this);
                break;
            case HEALING_HAND:
                cardAction = new CardAction(ability, this, CardType.UNIT, ActionType.HEALING, 2);
                break;
            case AGGRESSION:
                setPhase(Phase.ATTACK);
                break;
            case ENTANGLING_VINES:
                cardAction = new CardAction(ability, null, CardType.UNIT, ActionType.DAMAGE, 1);
                break;
            case EXECUTOR_AXE:
                cardAction = new CardAction(ability, this, CardType.UNIT, ActionType.ATTACK_MOD, 3);
                break;
            }
            game.placeAbility(ability, position);
            played = true;
        } else if (card instanceof Environment) {
            Environment environment = (Environment) card;
            game.placeEnvironment(environment, position);
            played = true;
        }
        if (played) {
            if (card.getPhase() != Phase.NEUTRAL) {
                setPhase(card.getPhase());
            }
            System.out.println("Playing " + card);
            if (cardAction != null) {
                System.out.println("Select your target for " + card);
            }
            activeCard = null;
            removeCard(card);
        }
    }

    public void drawCards(int count) throws IOException {
        for (int i = 0; i < count; i++) {
            if (isHandFull()) {
                break;
            }
            if (drawPile.isEmpty()) {
                reloadDrawPile();
            }
            if (drawPile.isEmpty()) {
                break;
            }
            Card topCard = drawPile.getTopCard();
            drawPile.removeCard(topCard);
            addToHand(topCard, false);
        }
        if (count > 0) {
            Packet packet = new Packet(PacketType.HAND_UPDATED);
            for (Card card: handCards) {
                packet.uuids.add(card.getUUID());
            }
            game.getConnection().enqueue(packet);
        }
    }

    public ArrayList<Unit> getUnitsOnField() {
        ArrayList<Unit> units = new ArrayList<>();
        for (Slot slot: unitSlots) {
            if (!slot.isBlank()) {
                units.add(slot.getUnit());
            }
        }
        return units;
    }

    public ArrayList<Ability> getAbilitiesOnField() {
        ArrayList<Ability> abilities = new ArrayList<>();
        for (Slot slot: abilitySlots) {
            if (!slot.isBlank()) {
                abilities.add(slot.getAbility());
            }
        }
        return abilities;
    }

    private void reloadDrawPile() {
        for (Card card: discardPile.getCards()) {
            drawPile.add(card);
        }
        drawPile.shuffle();
        discardPile.clear();
    }

    public void setHandCards(Card[] cards) {
        handCards.clear();
        // no notification because this is used to updating remote hand
        for (Card card: cards) {
            handCards.add(card);
        }
        updateCardPositions();
    }

    public void setPlayerId(int playerId) {
        this.playerId = playerId;
        environmentSlot.setPlayerId(playerId);
        for (Slot slot: unitSlots) {
            slot.setPlayerId(playerId);
        }
        for (Slot slot: abilitySlots) {
            slot.setPlayerId(playerId);
        }
    }

    public int getHandCount() {
        return handCards.size();
    }

    public boolean isHandFull() {
        return handCards.size() >= 7;

    }

    public boolean isHandEmpty() {
        return handCards.isEmpty();
    }

    public void addToHand(Card card, boolean notify) throws IOException {
        handCards.add(card);
        updateCardPositions();
        if (notify) {
            Packet packet = new Packet(PacketType.HAND_UPDATED);
            for (Card c: handCards) {
                packet.uuids.add(c.getUUID());
            }
            game.getConnection().enqueue(packet);
        }
    }

    public boolean contains(int mouseX, int mouseY) {
        return getHandCardAt(mouseX, mouseY) != null;
    }

    public int getDiscardQueueSize() {
        return discardQueue.size();
    }

    private ArrayList<Card> discardQueue = new ArrayList<>();

    public void onClick(int mouseX, int mouseY) {
        Card card = getHandCardAt(mouseX, mouseY);
        if (card != null) {
            if (game.discarding) {
                if (!discardQueue.contains(card)) {
                    card.setY(card.getY() - 20);
                    discardQueue.add(card);
                } else {
                    card.setY(card.getY() + 20);
                    discardQueue.remove(card);
                }
            } else {
                if (card != activeCard) {
                    if (card.isPlayable(game)) {
                        // System.out.println("Using " + card);
                        activeCard = card;
                        updateCardPositions();
                    } else {
                        System.out.println("Cannot use " + card);
                    }
                }
            }
        }
    }

    public void executeDiscard() throws IOException {
        for (Card card: discardQueue) {
            discard(card);
        }
        discardQueue.clear();
    }

    public Card getHandCardAt(int mouseX, int mouseY) {
        for (Card card: handCards) {
            if (card.contains(mouseX, mouseY)) {
                return card;
            }
        }
        return null;
    }

    public void draw(Graphics2D g) {
        if (phase != null) {
            g.drawImage(phase.getImage(), Game.WIDTH - 200, Game.HEIGHT - 240, Game.WIDTH - 130, Game.HEIGHT - 170, 0,
                    0, 32, 32, null);
            g.drawString(phase + " PHASE", Game.WIDTH - 200 - 10, Game.HEIGHT - 235);
        }

        drawPile.draw(g);
        discardPile.draw(g);
        drawHand(g, false);
    }

    public void drawHand(Graphics2D g, boolean hidden) {
        Color color = g.getColor();
        Stroke stroke = g.getStroke();
        for (int i = 0; i < handCards.size(); i++) {
            Card card = handCards.get(i);
            int x = card.getX();
            int y = card.getY();
            g.setColor(Color.WHITE);
            g.drawString("" + (i + 1), x + Game.CARD_WIDTH / 2, y - 10);
            if (hidden) {
                g.drawImage(LoadedImages.backImage, x, y, x + Game.CARD_WIDTH, y + Game.CARD_HEIGHT, 0, 0,
                        LoadedImages.backImage.getWidth(null), LoadedImages.backImage.getHeight(null), null);
            } else {
                Image image = card.getImage();
                g.drawImage(image, x, y, x + Game.CARD_WIDTH, y + Game.CARD_HEIGHT, 0, 0, image.getWidth(null),
                        image.getHeight(null), null);
                g.drawImage(card.getPhase().getImage(), x + Game.CARD_WIDTH / 3, y + Game.CARD_HEIGHT / 3, 50, 50,
                        null);
                g.drawString("PID:" + card.getPlayerId(), x + 10, y + 40);
                if (!card.isPlayable(game)) {
                    g.setStroke(new BasicStroke(3));
                    g.setColor(Color.RED);
                    g.drawLine(x, y, x + Game.CARD_WIDTH, y + Game.CARD_HEIGHT);
                    g.drawLine(x + Game.CARD_WIDTH, y, x, y + Game.CARD_HEIGHT);
                }
            }
        }
        g.setStroke(stroke);
        g.setColor(color);
    }

    public void discard(Card card) throws IOException {
        removeCard(card);
        Packet packet = new Packet(PacketType.HAND_UPDATED);
        for (Card c: handCards) {
            packet.uuids.add(c.getUUID());
        }
        game.getConnection().enqueue(packet);
        discardPile.add(card);
    }

    private void removeCard(Card card) {
        handCards.remove(card);
        updateCardPositions();
    }

    public void updateCardPositions() {
        for (int i = 0; i < handCards.size(); i++) {
            int x = handOffsetX + Game.CARD_WIDTH * (i + 1) + 5 * i;
            int y = handOffsetY;
            handCards.get(i).setX(x);
            handCards.get(i).setY(y);
        }
    }

    public Side getSide() {
        return side;
    }

    public int getPlayerId() {
        return playerId;
    }

    public Phase getPhase() {
        return phase;
    }

    public ArrayList<Card> getAllCards() {
        // TODO change this to get the list of cards in the deck
        ArrayList<Card> cards = new ArrayList<>();
        for (Slot slot: unitSlots) {
            if (!slot.isBlank()) {
                cards.add(slot.getCard());
            }
        }
        for (Slot slot: abilitySlots) {
            if (!slot.isBlank()) {
                cards.add(slot.getCard());
            }
        }
        if (!environmentSlot.isBlank()) {
            cards.add(environmentSlot.getCard());
        }
        for (Card card: handCards) {
            cards.add(card);
        }
        for (Card card: drawPile.getCards()) {
            cards.add(card);
        }
        for (Card card: discardPile.getCards()) {
            cards.add(card);
        }

        return cards;
    }

    public ArrayList<Unit> getAllFieldUnits() {
        ArrayList<Unit> units = new ArrayList<>();
        for (Slot slot: unitSlots) {
            if (!slot.isBlank()) {
                units.add(slot.getUnit());
            }
        }
        return units;
    }

    public void setPhase(Phase phase) throws IOException {
        if (this.phase != phase) {
            this.phase = phase;
            game.onPhase(phase);
        }
    }

    private void initSlots() {
        if (game.hosting) {
            if (side == Side.TOP) {
                environmentSlot = new Slot(SlotPosition.ENVIRON_1, !game.hosting);
                drawPile = new DrawPile(SlotPosition.DRAW_0, !game.hosting);
                discardPile = new DiscardPile(SlotPosition.DISCARD_0, !game.hosting);
                unitSlots[0] = new Slot(SlotPosition.UNIT_0, !game.hosting);
                unitSlots[1] = new Slot(SlotPosition.UNIT_1, !game.hosting);
                unitSlots[2] = new Slot(SlotPosition.UNIT_2, !game.hosting);
                unitSlots[3] = new Slot(SlotPosition.UNIT_3, !game.hosting);
                abilitySlots[0] = new Slot(SlotPosition.ABILITY_0, !game.hosting);
                abilitySlots[1] = new Slot(SlotPosition.ABILITY_1, !game.hosting);
                abilitySlots[2] = new Slot(SlotPosition.ABILITY_2, !game.hosting);
                abilitySlots[3] = new Slot(SlotPosition.ABILITY_3, !game.hosting);
                abilitySlots[4] = new Slot(SlotPosition.ABILITY_4, !game.hosting);
            } else if (side == Side.BOTTOM) {
                environmentSlot = new Slot(SlotPosition.ENVIRON_0, !game.hosting);
                drawPile = new DrawPile(SlotPosition.DRAW_1, !game.hosting);
                discardPile = new DiscardPile(SlotPosition.DISCARD_1, !game.hosting);
                unitSlots[0] = new Slot(SlotPosition.UNIT_4, !game.hosting);
                unitSlots[1] = new Slot(SlotPosition.UNIT_5, !game.hosting);
                unitSlots[2] = new Slot(SlotPosition.UNIT_6, !game.hosting);
                unitSlots[3] = new Slot(SlotPosition.UNIT_7, !game.hosting);
                abilitySlots[0] = new Slot(SlotPosition.ABILITY_5, !game.hosting);
                abilitySlots[1] = new Slot(SlotPosition.ABILITY_6, !game.hosting);
                abilitySlots[2] = new Slot(SlotPosition.ABILITY_7, !game.hosting);
                abilitySlots[3] = new Slot(SlotPosition.ABILITY_8, !game.hosting);
                abilitySlots[4] = new Slot(SlotPosition.ABILITY_9, !game.hosting);
            }
        } else {
            if (side == Side.TOP) {
                environmentSlot = new Slot(SlotPosition.ENVIRON_0, !game.hosting);
                drawPile = new DrawPile(SlotPosition.DRAW_1, !game.hosting);
                discardPile = new DiscardPile(SlotPosition.DISCARD_1, !game.hosting);
                unitSlots[0] = new Slot(SlotPosition.UNIT_4, !game.hosting);
                unitSlots[1] = new Slot(SlotPosition.UNIT_5, !game.hosting);
                unitSlots[2] = new Slot(SlotPosition.UNIT_6, !game.hosting);
                unitSlots[3] = new Slot(SlotPosition.UNIT_7, !game.hosting);
                abilitySlots[0] = new Slot(SlotPosition.ABILITY_5, !game.hosting);
                abilitySlots[1] = new Slot(SlotPosition.ABILITY_6, !game.hosting);
                abilitySlots[2] = new Slot(SlotPosition.ABILITY_7, !game.hosting);
                abilitySlots[3] = new Slot(SlotPosition.ABILITY_8, !game.hosting);
                abilitySlots[4] = new Slot(SlotPosition.ABILITY_9, !game.hosting);
            } else if (side == Side.BOTTOM) {
                environmentSlot = new Slot(SlotPosition.ENVIRON_1, !game.hosting);
                drawPile = new DrawPile(SlotPosition.DRAW_0, !game.hosting);
                discardPile = new DiscardPile(SlotPosition.DISCARD_0, !game.hosting);
                unitSlots[0] = new Slot(SlotPosition.UNIT_0, !game.hosting);
                unitSlots[1] = new Slot(SlotPosition.UNIT_1, !game.hosting);
                unitSlots[2] = new Slot(SlotPosition.UNIT_2, !game.hosting);
                unitSlots[3] = new Slot(SlotPosition.UNIT_3, !game.hosting);
                abilitySlots[0] = new Slot(SlotPosition.ABILITY_0, !game.hosting);
                abilitySlots[1] = new Slot(SlotPosition.ABILITY_1, !game.hosting);
                abilitySlots[2] = new Slot(SlotPosition.ABILITY_2, !game.hosting);
                abilitySlots[3] = new Slot(SlotPosition.ABILITY_3, !game.hosting);
                abilitySlots[4] = new Slot(SlotPosition.ABILITY_4, !game.hosting);
            }

        }
    }

}
