package net.packets;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

import animations.AttackAnimation;
import cards.CardDatabase;
import cards.types.Card;
import cards.types.Unit;
import misc.Game;

public class Packet implements Serializable, Comparable<Packet> {
    private static final long serialVersionUID = 1L;
    public String message = "";
    public PacketType type = PacketType.INFO;
    public ArrayList<Card> cards = new ArrayList<>();
    public ArrayList<UUID> uuids = new ArrayList<>();
    public int amount;
    public int localPlayerId = -1, remotePlayerId = -1;
    public int turnNumber;
    public int firstPlayerId = -1;
    public int pileOwnerId = -1;
    public CardDatabase cdb;
    public UUID innerCardUUID;
    public UUID animationSource, animationTarget;
    private long createdTime;

    private Packet(PacketType type, String message) {
        this.type = type;
        this.message = message;
        createdTime = System.currentTimeMillis();
    }

    public Packet(PacketType type) {
        this(type, "");
    }

    public Packet(String message) {
        this(PacketType.INFO, message);
    }

    @Override
    public String toString() {
        String text = "";
        switch (type) {
        case CARDS_UPDATED:
        case CARDS_PLACED:
        case HAND_UPDATED:
        case DRAW_PILE_UPDATED:
        case DISCARD_PILE_UPDATED:
            for (UUID uuid: uuids) {
                text += Game.cdb.getCardByUUID(uuid) + " ";
            }
            break;
        case INFO:
            text = message;
            break;
        case LOGGED_IN:
            break;
        case LOGIN_REQUEST:
            break;
        case PLAYER_IDS:
            text = String.format("Local ID: %d, Remote ID: %d", localPlayerId, remotePlayerId);
            break;
        case START_TURN:
            text = "" + turnNumber;
            break;
        case INNERCARD_CHANGED:
            text = cards.get(0).getName() + "contains " + innerCardUUID;
            break;
        default:
            break;
        }
        text = text.trim();
        if (text.length() == 0) {
            return String.format("PACKET<%s>", type);
        }
        return String.format("PACKET<%s>('%s')", type, text);
    }

    public long getCreationTime() {
        return createdTime;
    }

    @Override
    public int compareTo(Packet packet) {
        return this.createdTime < packet.createdTime ? -1 : this.createdTime == packet.createdTime ? 0 : 1;
    }

    public enum PacketType {
        LOGIN_REQUEST,
        LOGGED_IN,
        PLAYER_IDS,
        INFO,
        CARDS_PLACED,
        START_TURN,
        HAND_UPDATED,
        CARDS_UPDATED,
        DRAW_PILE_UPDATED,
        DISCARD_PILE_UPDATED,
        INNERCARD_CHANGED,
        START_ANIMATION,;
    }

}
