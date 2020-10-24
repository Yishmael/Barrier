package net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;

import cards.CardDatabase;
import net.packets.Packet;

public class Server implements Runnable {
    public static final int PORT = 55555;
    private final ServerSocket ss;
    private final ArrayList<ClientWorker> workers = new ArrayList<>();
    private final CardDatabase cdb = new CardDatabase();

    public Server() throws IOException {
        ss = new ServerSocket(PORT);
        ss.setReuseAddress(true);
    }

    public void start() throws IOException, InterruptedException {
        System.out.println(String.format("(S)Listening on %d...", PORT));
        while (true) {
            System.out.println(String.format("(S)Waiting for a new client...", PORT));
            Socket socket = ss.accept();
            System.out.println("(S)New connection from " + socket.getPort());
            socket.getOutputStream().write(Utils.serialize(new Packet("Welcome to the server!")));
            Thread.sleep(100);
            ClientWorker worker = new ClientWorker(this, socket);
            workers.add(worker);
            Thread thread = new Thread(worker);
            thread.start();

            for (Iterator<ClientWorker> iter = workers.iterator(); iter.hasNext();) {
                ClientWorker clientWorker = iter.next();
                if (clientWorker.getSocket().isClosed()) {
                    iter.remove();
                }
            }
            System.out.println("(S)" + workers.size() + " workers active");
            if (workers.size() == 2) {
                break;
            }
        }
        ss.close();
        System.out.println(String.format("(S)All players connected! (%d)", workers.size()));
    }

    @Override
    public void run() {
        try {
            start();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public ClientWorker getOtherWorker(int clientId) {
        for (ClientWorker worker: workers) {
            if (worker.clientId != clientId) {
                return worker;
            }
        }
        return null;
    }

    public int getWorkerCount() {
        return workers.size();
    }

    public CardDatabase getCardDatabase() {
        return cdb;
    }

    public static void main(String argv[]) throws IOException, InterruptedException {
        Server server = new Server();
        Thread thread = new Thread(server);
        thread.start();
    }

};
