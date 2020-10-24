package net;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

import javax.swing.JFrame;

import misc.Game;
import net.packets.Packet;
import net.packets.Packet.PacketType;

public class ClientGame extends Canvas implements Runnable {
    private static final long serialVersionUID = 1L;
    private boolean running;
    private JFrame frame;
    private Socket socket;
    private Graphics2D g;
    private BufferStrategy bs;

    public ClientGame() throws IOException {
        setMinimumSize(new Dimension(Game.WIDTH, Game.HEIGHT));
        setMaximumSize(new Dimension(Game.WIDTH, Game.HEIGHT));
        setPreferredSize(new Dimension(Game.WIDTH, Game.HEIGHT));

        frame = new JFrame("The Barrier v0.0.1 (client window)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(this, BorderLayout.CENTER);
        frame.pack();

        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        socket = new Socket("localhost", 55555);
        Packet packet = new Packet(PacketType.LOGIN_REQUEST);
        socket.getOutputStream().write(Utils.serialize(packet));
    }

    private Packet packet = null;

    private void tick() throws IOException {
        if (socket != null) {
            byte[] data = new byte[Utils.BUFFER_SIZE];
            socket.setSoTimeout(10);
            try {
                socket.getInputStream().read(data);
                Packet packet = (Packet) Utils.deserialize(data);
                this.packet = packet;
                parsePacket();
            } catch (SocketTimeoutException e) {
            }
        }
    }

    private void parsePacket() {
        System.out.println("Parsing " + packet);
    }

    private void render() {
        bs = getBufferStrategy();
        if (bs == null) {
            createBufferStrategy(2);
            return;
        }
        g = (Graphics2D) bs.getDrawGraphics();

        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Color.GREEN);
        g.setFont(new Font("Arial", Font.PLAIN, 30));
        g.drawString(packet.message, 100, 30);

        g.dispose();
        bs.show();
    }

    public static void main(String[] argv) throws IOException {
        new ClientGame().start();
    }

    public synchronized void start() {
        running = true;
        new Thread(this).start();
    }

    public synchronized void stop() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {
            try {
                tick();
            } catch (IOException e) {
                e.printStackTrace();
            }
            render();
        }
        frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
    }
}
