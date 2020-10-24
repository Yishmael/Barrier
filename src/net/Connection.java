package net;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.UUID;

import cards.CardDatabase;
import cards.types.Card;
import net.packets.Packet;
import net.packets.Packet.PacketType;

public class Connection {
    private Socket socket;
    private long lastSentTime;
    @SuppressWarnings("unused")
    private int bytesSent, bytesReceived;

    public Connection() {
        String hostname = "localhost";
        try {
            socket = new Socket(hostname, Server.PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean sending;

    public void enqueue(Packet newPacket) throws IOException {
        Packet packet = newPacket;
        if (packet.type == PacketType.CARDS_UPDATED && !packet.cards.isEmpty()) {
            packet = new Packet(newPacket.type);
            for (Card card: newPacket.cards) {
                packet.cards.add((Card) Utils.deepCopy(card));
            }
        }

        System.out.println("Enqueuing " + packet);

        while (sending) {
            System.out.println("sending...");
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // System.out.println("Enqueued " + packet + " at " + System.currentTimeMillis());
        // packetsData.add(Utils.serialize(packet));
        for(Packet p : packets) {
            if (p.type == packet.type) {
                if (p.getCreationTime() < packet.getCreationTime()) {
                    // check the lists and only keep the most recent version of the card in it

//                    return;
                }
            }
        }
        packets.add(packet);
    }

    // private ArrayList<byte[]> packetsData = new ArrayList<>();
    private ArrayList<Packet> packets = new ArrayList<>();

    public void sendAll() throws IOException {
        if (packets.isEmpty()) {
            return;
        }
        if (System.currentTimeMillis() - lastSentTime < 100) {
            return;
        }
        System.out.println("Sending " + packets.size() + " packet(s).");
        sending = true;
        for (Packet packet: packets) {
            if (packet.type == PacketType.CARDS_UPDATED) {
                System.out.println("Sending " + packet.cards.get(0) + " " + packet.cards.get(0).destroyed);
            } // for (Iterator<Packet> iter = packets.iterator(); iter.hasNext();) {
            // Packet packet = iter.next();
            //
            // }
            // byte[] data = Utils.serialize(packet);
            try {
                // waiting at least 100ms between sends
                Thread.sleep(Math.max(0, System.currentTimeMillis() - lastSentTime < 100 ? 100 : 0));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            byte[] data = Utils.serialize(packet);
            socket.getOutputStream().write(data);
            lastSentTime = System.currentTimeMillis();
            bytesSent += data.length;
        }
        //
        packets.clear();
        sending = false;
        // System.out.println(String.format("Total sent: %d. Total received: %d", bytesSent,
        // bytesReceived));
    }

    public Packet receive() throws IOException {
        return receive(20);
    }

    public Packet receive(int timeoutMillis) throws IOException {
        Packet packet = null;
        socket.setSoTimeout(timeoutMillis);
        byte[] data = new byte[Utils.BUFFER_SIZE];
        try {
            int size = socket.getInputStream().read(data);
            packet = (Packet) Utils.deserialize(data);
            bytesReceived += size;
            // System.out.println("rec: " + size + ": " + packet);
        } catch (SocketTimeoutException e) {
            // pass
        }
        return packet;
    }

}
