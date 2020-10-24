package misc;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.io.IOException;

import cards.types.Ability;
import cards.types.Card;
import cards.types.CardType.UnitType.UnitTag;
import cards.types.Unit;
import misc.GameEvent.GameEventType;
import misc.Slot.SlotPosition;
import net.packets.Packet;
import net.packets.Packet.PacketType;

public class Grid {
    public final Player localPlayer, remotePlayer;
    public boolean selected = false;

    private final Game game;
    private Unit attacker;

    public Grid(Game game) {
        this.game = game;
        this.localPlayer = game.localPlayer;
        this.remotePlayer = game.remotePlayer;
    }

    private void drawAttackLine(Graphics2D g) {
        Color color = g.getColor();
        Stroke stroke = g.getStroke();
        g.setStroke(new BasicStroke(5));
        g.setColor(Color.YELLOW);
        int originX = attacker.getX() + Game.CARD_WIDTH / 2;
        int originY = attacker.getY() + Game.CARD_HEIGHT / 2;
        int attackerId = attacker.getPlayerId();
        int attackerPositionIndex = -1;

        Player attackerPlayer = attackerId == localPlayer.getPlayerId() ? localPlayer : remotePlayer;
        Player attackedPlayer = attackerPlayer == localPlayer ? remotePlayer : localPlayer;

        for (int i = 0; i < attackerPlayer.unitSlots.length; i++) {
            Slot slot = attackerPlayer.unitSlots[i];
            Unit unit = slot.getUnit();
            if (unit == attacker) {
                attackerPositionIndex = i;
                break;
            }
        }
        // draw a line to all enemies
        for (int i = 0; i < attackedPlayer.unitSlots.length; i++) {
            Slot slot = attackedPlayer.unitSlots[i];
            if (slot.isBlank()) {
                continue;
            }
            Unit unit = slot.getUnit();
            if (Math.abs(attackerPositionIndex - i) > 1) {
                if (!attacker.getType().hasTag(UnitTag.RANGED)) {
                    continue;
                }
            }
            g.setColor(new Color(1, 0, 0, 0.5f));
            g.drawLine(originX, originY, unit.getX() + Game.TILE_WIDTH / 2, unit.getY() + Game.TILE_HEIGHT / 2);
        }
        g.setColor(Color.YELLOW);
        Slot slot = getSlotAt(game.getMouseX(), game.getMouseY());
        if (slot != null && !slot.isBlank()) {
            Card card = slot.getCard();
            g.drawLine(originX, originY, card.getX() + Game.TILE_WIDTH / 2, card.getY() + Game.TILE_HEIGHT / 2);
        } else {
            g.setColor(new Color(1, 0, 0, 0.7f));
            g.drawLine(originX, originY, game.getMouseX(), game.getMouseY());
        }
        g.setColor(Color.WHITE);
        g.setStroke(stroke);
        g.setColor(color);
    }

    private void drawGrid(Graphics2D g) {
        localPlayer.environmentSlot.drawAtAngle(g, 0);
        for (Slot slot: localPlayer.unitSlots) {
            slot.draw(g);
        }
        for (Slot slot: localPlayer.abilitySlots) {
            slot.draw(g);
        }
        remotePlayer.environmentSlot.drawAtAngle(g, 180);
        for (Slot slot: remotePlayer.unitSlots) {
            slot.draw(g);
        }
        for (Slot slot: remotePlayer.abilitySlots) {
            slot.draw(g);
        }
    }

    public void draw(Graphics2D g) {
        drawGrid(g);
        if (selected) {
            drawAttackLine(g);
        }
    }

    public Slot getSlotAt(int x, int y) {
        for (Player player: new Player[] { localPlayer, remotePlayer }) {
            for (Slot slot: player.unitSlots) {
                if (slot.contains(x, y)) {
                    return slot;
                }
            }
            for (Slot slot: player.abilitySlots) {
                if (slot.contains(x, y)) {
                    return slot;
                }
            }
            if (player.environmentSlot.contains(x, y)) {
                return player.environmentSlot;
            }
            if (player.discardPile.contains(x, y)) {
                Card card = player.discardPile.getTopCard();
                if (card != null) {
                    return new Slot(card, player.discardPile.getSlotPosition(), !game.hosting);
                }
            }
            if (player.drawPile.contains(x, y)) {
                Card card = player.drawPile.getTopCard();
                if (card != null) {
                    return new Slot(card, player.drawPile.getSlotPosition(), !game.hosting);
                }
            }
        }
        return null;
    }

    public void clickAt(int button, int x, int y) throws IOException {
        // handles targeting, placing a card, and attacking
        if (button == MouseEvent.BUTTON3) { // right click cancels the current action
            selected = false;
            return;
        }
        Slot slot = getSlotAt(x, y);
        if (slot == null || slot.isBlank()) {
            return;
        }
        Card card = slot.getCard();
        if (game.localPlayer.cardAction != null) {
            Card targetCard = card;
            if (game.localPlayer.cardAction.isValidTarget(targetCard)) {
                game.localPlayer.cardAction.applyOn(targetCard);
                if (game.localPlayer.cardAction.getSourceCard() instanceof Ability) {
                    Packet packet = new Packet(PacketType.INNERCARD_CHANGED);
                    packet.cards.add(game.localPlayer.cardAction.getSourceCard());
                    packet.innerCardUUID = targetCard.getUUID();
                    game.getConnection().enqueue(packet);
                }
                game.localPlayer.cardAction = null;
            }
            return;
        }
        System.out.println("Info: " + card);
        if (card.getPlayerId() != game.localPlayer.getPlayerId()) {
            // cannot attack with other player's units
            if (!selected) {
                return;
            }
        } else {
            // cannot attack own units
            if (selected) {
                return;
            }
        }
        if (game.localPlayer.getPhase() != Phase.ATTACK) {
            return;
        }
        if (!selected) {
            if (card instanceof Unit) {
                attacker = (Unit) card;
                if (!attacker.attackedThisTurn && attacker.canAttack) {
                    selected = true;
                    System.out.println("Attacking with " + card);
                }
            }
        } else {
            Unit target = (Unit) card;
            if (!attacker.canReach(target)) {
                return;
            }
            selected = false;
            target.onAttack(attacker);
            game.gameEvents.add(new GameEvent(GameEventType.UNIT_ATTACKED, attacker, target));
            attacker.attackedThisTurn = true;
            System.out.println(String.format("%s attacked %s.", attacker, target));
            game.startAttackAnimation(attacker, target);
        }
    }

    public static int distanceBetween(SlotPosition a, SlotPosition b) {
        if (a == null || b == null) {
            return 99;
        }
        if (a.name().contains("UNIT") && b.name().contains("UNIT")) {
            int offsetA =  Integer.parseInt(a.name().substring(5));
            int offsetB = Integer.parseInt(b.name().substring(5));
            return Math.abs(offsetA%4 - offsetB%4);
        }
        return 99;
    }

}
