package net;

import java.io.IOException;
import java.net.Socket;

import cards.DeckBuilder.DeckType;
import cards.types.Ability;
import cards.types.Card;
import cards.types.CardType.AbilityType;
import cards.types.CardType.EnvironmentType;
import cards.types.CardType.UnitType;
import cards.types.Environment;
import cards.types.Unit;
import net.packets.Packet;
import net.packets.Packet.PacketType;

public class ClientWorker implements Runnable {
    private Socket socket;
    private Server server;
    public int clientId;

    public ClientWorker(Server server, Socket socket) throws IOException {
        this.clientId = -1;
        this.server = server;
        this.socket = socket;
    }

    @Override
    public void run() {
        while (socket.isConnected()) {
            byte[] data = new byte[Utils.BUFFER_SIZE];
            try {
                socket.setSoTimeout(10);

                @SuppressWarnings("unused")
                int size = socket.getInputStream().read(data);
                Packet packet = (Packet) Utils.deserialize(data);
                // System.out.println("(S)" + size + ":" + packet);
                parsePacket(packet);
            } catch (IOException e) {
                if (e.getMessage().equals("Connection reset")) {
                    break;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(String.format("(S)Player %d disconnected.", clientId));
    }

    private boolean loggedIn;

    @SuppressWarnings("null")
    private void parsePacket(Packet packet) throws IOException, InterruptedException {
        if (server.getWorkerCount() >= 2) {
            // System.out.println(
            // String.format("(S)%d -> %d: %s", clientId, server.getOtherWorker(clientId).clientId,
            // packet));
        }
        Packet response = null;
        ClientWorker otherWorker = null;
        if (server.getWorkerCount() >= 2) {
            otherWorker = server.getOtherWorker(clientId);
        }
        switch (packet.type) {
        case LOGIN_REQUEST:
            if (loggedIn) {
                return;
            }
            clientId = server.getWorkerCount();
            System.out.println("(S)NEW USER: ID " + clientId);
            if (server.getWorkerCount() < 2) {
                return;
            }
            response = new Packet(PacketType.PLAYER_IDS);
            response.localPlayerId = clientId;
            response.remotePlayerId = otherWorker.clientId;
            socket.getOutputStream().write(Utils.serialize(response));
            response.localPlayerId = otherWorker.clientId;
            response.remotePlayerId = clientId;
            otherWorker.getSocket().getOutputStream().write(Utils.serialize(response));

            Thread.sleep(150);
            response = new Packet(PacketType.LOGGED_IN);
            response.message = "Login successful!";
            response.cdb = server.getCardDatabase();
            for (UnitType type: DeckType.ONE.getUnits()) {
                Unit unit = type.instantiate();
                unit.setPlayerId(clientId);
                server.getCardDatabase().addCard(unit);
                response.uuids.add(unit.getUUID());
            }
            for (UnitType type: DeckType.TWO.getUnits()) {
                Unit unit = type.instantiate();
                unit.setPlayerId(otherWorker.clientId);
                server.getCardDatabase().addCard(unit);
                response.uuids.add(unit.getUUID());
            }
            for (AbilityType type: DeckType.ONE.getAbilities()) {
                Ability ability = type.instantiate();
                ability.setPlayerId(clientId);
                server.getCardDatabase().addCard(ability);
                response.uuids.add(ability.getUUID());
            }
            for (AbilityType type: DeckType.TWO.getAbilities()) {
                Ability ability = type.instantiate();
                ability.setPlayerId(otherWorker.clientId);
                server.getCardDatabase().addCard(ability);
                response.uuids.add(ability.getUUID());
            }
            for (EnvironmentType type: DeckType.ONE.getEnvirons()) {
                Environment environ = type.instantiate();
                environ.setPlayerId(clientId);
                server.getCardDatabase().addCard(environ);
                response.uuids.add(environ.getUUID());
            }
            for (EnvironmentType type: DeckType.TWO.getEnvirons()) {
                Environment environ = type.instantiate();
                environ.setPlayerId(otherWorker.clientId);
                server.getCardDatabase().addCard(environ);
                response.uuids.add(environ.getUUID());
            }
            response.firstPlayerId = 1;
            socket.getOutputStream().write(Utils.serialize(response));
            otherWorker.getSocket().getOutputStream().write(Utils.serialize(response));
            System.out.println("(S)" + response);
            loggedIn = true;
            break;
        case CARDS_PLACED:
            response = new Packet(PacketType.CARDS_PLACED);
            for (Card card: packet.cards) {
                server.getCardDatabase().updateCard(card);
                response.uuids.add(card.getUUID());
            }
            response.cdb = server.getCardDatabase();
            otherWorker.getSocket().getOutputStream().write(Utils.serialize(response));
            break;
        case CARDS_UPDATED:
            // for (UUID uuid: packet.cardUUIDs1) {
            // System.out.println(
            // String.format("(S)Updated %s",
            // server.getCardDatabase().getCardByUUID(uuid).getName()));
            // }
            response = new Packet(PacketType.CARDS_UPDATED);
            for (Card card: packet.cards) {
                server.getCardDatabase().updateCard(card);
                response.uuids.add(card.getUUID());
            }
            response.cdb = server.getCardDatabase();
            otherWorker.getSocket().getOutputStream().write(Utils.serialize(response));
            break;
        case HAND_UPDATED:
            response = new Packet(PacketType.HAND_UPDATED);
            for (Card card: packet.cards) {
                response.uuids.add(card.getUUID());
            }
            response.cdb = server.getCardDatabase();
            otherWorker.getSocket().getOutputStream().write(Utils.serialize(response));
            break;
        case INNERCARD_CHANGED:
            response = new Packet(PacketType.INNERCARD_CHANGED);
            for (Card card: packet.cards) {
                response.cards.add(card);
                response.innerCardUUID = packet.innerCardUUID;
                break; // breaking since there's only one card
            }
            response.cdb = server.getCardDatabase();
            otherWorker.getSocket().getOutputStream().write(Utils.serialize(response));
            break;
        case START_TURN:
            // Thread.sleep(300);
            response = new Packet(PacketType.START_TURN);
            response.turnNumber = packet.turnNumber;
            otherWorker.getSocket().getOutputStream().write(Utils.serialize(response));
            break;
        case DRAW_PILE_UPDATED:
        case DISCARD_PILE_UPDATED:
            response = new Packet(packet.type);
            response.cdb = server.getCardDatabase();
            response.pileOwnerId = packet.pileOwnerId;
            response.uuids = packet.uuids;
            otherWorker.getSocket().getOutputStream().write(Utils.serialize(response));
            break;
        case START_ANIMATION:
            response = new Packet(PacketType.START_ANIMATION);
            response.animationSource = packet.animationSource;
            response.animationTarget = packet.animationTarget;
            otherWorker.getSocket().getOutputStream().write(Utils.serialize(response));
            break;
        default:
            break;
        }
    }

    public Socket getSocket() {
        return socket;
    }

}
