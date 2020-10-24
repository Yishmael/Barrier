package misc;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

import javax.swing.JFrame;

import animations.Animator;
import animations.AttackAnimation;
import cards.CardAction;
import cards.CardDatabase;
import cards.DetailCard;
import cards.Status;
import cards.piles.Pile;
import cards.types.Ability;
import cards.types.Card;
import cards.types.CardType.AbilityType.AbilityTag;
import cards.types.CardType.EnvironmentType;
import cards.types.CardType.UnitType.UnitTag;
import cards.types.Environment;
import cards.types.Unit;
import misc.GameEvent.GameEventType;
import misc.Slot.SlotPosition;
import net.Connection;
import net.Server;
import net.Utils;
import net.packets.Packet;
import net.packets.Packet.PacketType;

public class Game extends Canvas implements Runnable {
    private static final long serialVersionUID = 1L;
    public static final int WIDTH = 800, HEIGHT = 600;
    // public static final int WIDTH = 1280, HEIGHT = 800;
    public static final int TILE_WIDTH = 80, TILE_HEIGHT = 100;
    public static final int CARD_WIDTH = TILE_WIDTH, CARD_HEIGHT = TILE_HEIGHT;

    public Grid grid;
    public Player localPlayer, remotePlayer;
    public int turnNumber;
    public boolean localTurn;
    public ArrayList<GameEvent> gameEvents = new ArrayList<>();

    private JFrame frame;
    private BufferStrategy bs;
    private Graphics2D g;
    private boolean running = true;
    private DetailCard detailCard;
    private InputHandler inputHandler;
    private Server server;
    public boolean hosting;
    private Connection connection;
    private ArrayList<Card> placedCards = new ArrayList<>();
    public static CardDatabase cdb; // for Packet to map UUID to Card
    private Animator animator;

    public Game(boolean hosting) throws IOException, InterruptedException {
        this.hosting = hosting;
        setMinimumSize(new Dimension(WIDTH, HEIGHT));
        setMaximumSize(new Dimension(WIDTH, HEIGHT));
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        frame = new JFrame("The Barrier v0.0.1");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(this, BorderLayout.CENTER);
        // frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        // frame.setUndecorated(true);
        frame.pack();
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        if (hosting) {
            frame.setTitle("SERVER");
            frame.setLocation(-60, 20);
        } else {
            frame.setLocation(-60, 430);
        }
        // backImage = ImageIO.read(Game.class.getResource("/back.png"));
        Utils.loadImages();
        animator = new Animator();
    }

    public synchronized void start() throws UnknownHostException, IOException {
        localPlayer = new Player(this, Side.BOTTOM);
        remotePlayer = new Player(this, Side.TOP);

        grid = new Grid(this);
        inputHandler = new InputHandler(this);
        addKeyListener(inputHandler);
        addMouseListener(inputHandler);
        addMouseMotionListener(inputHandler);
        detailCard = new DetailCard(null);

        if (hosting) {
            frame.requestFocus();
        }
        connectToServer();
        running = true;
        new Thread(this).start();
    }

    @SuppressWarnings("incomplete-switch")
    private void loadPacket() throws IOException {
        Packet packet = connection.receive();
        if (packet == null) {
            return;
        }
        System.out.println(String.format("(%d)Parsing %s", localPlayer.getPlayerId(), packet));
        switch (packet.type) {
        case INFO:
            System.out.println("Server says: '" + packet.message + "'");
            break;
        case PLAYER_IDS:
            localPlayer.setPlayerId(packet.localPlayerId);
            remotePlayer.setPlayerId(packet.remotePlayerId);
            break;
        case LOGGED_IN:
            cdb = packet.cdb;
            System.out.println("Size of CDB: " + Utils.serialize(cdb).length);
            for (UUID uuid: packet.uuids) {
                Card card = cdb.getCardByUUID(uuid);
                if (card.getPlayerId() == localPlayer.getPlayerId()) {
                    localPlayer.drawPile.add(card);
                    if (hosting) {
                        System.out.println("got card: " + card);
                    }
                } else {
                    remotePlayer.drawPile.add(card);
                }
            }
            // the remote player is already up-to-date
            localPlayer.drawPile.updated = false;
            localPlayer.discardPile.updated = false;
            remotePlayer.drawPile.updated = false;
            remotePlayer.discardPile.updated = false;
            localTurn = packet.firstPlayerId == localPlayer.getPlayerId();
            startGame();
            break;
        case CARDS_PLACED: {
            cdb = packet.cdb;
            for (UUID uuid: packet.uuids) {
                Card card = cdb.getCardByUUID(uuid);
                getSlotAt(card.getSlotPosition()).setCard(card);
                card.updated = false;
            }
            break;
        }
        case CARDS_UPDATED: {
            cdb = packet.cdb;
            for (UUID uuid: packet.uuids) {
                for (Card fieldCard: getAllGameCards()) {
                    if (fieldCard.getUUID().equals(uuid)) {
                        Card remoteCard = cdb.getCardByUUID(uuid);
                        fieldCard.updateWith(remoteCard);
                        fieldCard.updated = false;
                        System.out.println(
                                "UPDATING " + fieldCard + " " + fieldCard.destroyed + " " + fieldCard.getPlayerId());
                        System.out.println(remoteCard.location);
                        if (remoteCard.location.equals("DISCARD PILE")) {
                            // moveFromFieldToDiscardPile(fieldCard.getUUID());
                        }
                    }
                }
            }
            break;
        }
        case INNERCARD_CHANGED:
            cdb = packet.cdb;
            Card innerCard = cdb.getCardByUUID(packet.innerCardUUID);
            System.out.println("INNERCARD: " + innerCard.getName());
            Ability updatedCard = (Ability) getSlotAt(packet.cards.get(0).getSlotPosition()).getCard();
            updatedCard.setAffectedCard(innerCard);
            break;
        case HAND_UPDATED:
            ArrayList<Card> handCards = new ArrayList<>();
            cdb = packet.cdb;
            for (UUID uuid: packet.uuids) {
                handCards.add(cdb.getCardByUUID(uuid));
            }
            remotePlayer.setHandCards(handCards.toArray(new Card[handCards.size()]));
            break;
        case DRAW_PILE_UPDATED:
        case DISCARD_PILE_UPDATED: {
            cdb = packet.cdb;
            Player owner = getPlayer(packet.pileOwnerId);
            Pile pile = packet.type == PacketType.DRAW_PILE_UPDATED ? owner.drawPile : owner.discardPile;
            System.out.println(localPlayer.getPlayerId() + ":" + owner.getPlayerId() + " updating disc pile");
            pile.clear();
            for (UUID uuid: packet.uuids) {
                Card card = cdb.getCardByUUID(uuid);
                pile.add(card);
                System.out.println(localPlayer.getPlayerId() + " ADDING CARD " + card + " " + card.getPlayerId());
            }
            pile.updated = false; // to make sure this isn't sent back to the sender
            break;
        }
        case START_ANIMATION:
            animator.attackAnimation = new AttackAnimation(cdb.getCardByUUID(packet.animationSource),
                    cdb.getCardByUUID(packet.animationTarget));
            System.err.println("source: " + animator.attackAnimation.getSource());
            break;
        case START_TURN:
            startTurn(packet.turnNumber);
            break;
        }

    }

    private void broadcastEvent(GameEvent event) {
        for (Card card: getAllFieldCards()) {
            card.onEvent(event);
            if (event.getType() == GameEventType.BURNING_TICK) {
                for (Card fieldCard: getAllFieldCards()) {
                    if (fieldCard != card) {
                        if (fieldCard instanceof Unit) {
                            if (Grid.distanceBetween(card.getSlotPosition(), fieldCard.getSlotPosition()) <= 1) {
                                if (!((Unit) fieldCard).getType().hasTag(UnitTag.BURNING)) {
                                    System.out.println(card + " " + fieldCard);
                                    int amount = environmentActive(EnvironmentType.INFERNO) ? 2 : 1;
                                    ((Unit) fieldCard).deltaHealth(card, -amount);
                                    System.out.println(
                                            String.format("%s took %d burning damage.", fieldCard.getName(), amount));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean environmentActive(EnvironmentType type) {
        for (Environment environ: getAllFieldEnvironments()) {
            if (environ.getType() == type) {
                return true;
            }
        }
        return false;
    }

    public void tick() throws IOException {
        if (connection != null) {
            loadPacket();
        }
        for (Card card: getAllFieldCards()) {
            if (card instanceof Unit) {
                if (((Unit) card).lastEvent != null) {
                    gameEvents.add(card.lastEvent);
                    ((Unit) card).lastEvent = null;
                }
            }
        }
        for (GameEvent info: gameEvents) {
            broadcastEvent(info);
        }
        gameEvents.clear();
        if (!placedCards.isEmpty()) {
            Packet packet = new Packet(PacketType.CARDS_PLACED);
            for (Card card: placedCards) {
                broadcastEvent(new GameEvent(GameEventType.CARD_PLAYED, null, card));
                if (card.getPlayerId() != localPlayer.getPlayerId()) {
                    // don't send updates on remote placed cards
                    continue;
                }
                packet.cards.add(card);
            }
            connection.enqueue(packet);
            placedCards.clear();
        }
        if (!localPlayer.environmentSlot.isBlank()) {
            if (localPlayer.environmentSlot.getEnvironment().ready) {
                CardAction triggeredEvent = localPlayer.environmentSlot.getEnvironment().getTriggeredEvent();
                for (Card card: getAllFieldCards()) {
                    if (triggeredEvent != null && triggeredEvent.isValidTarget(card)) {
                        triggeredEvent.applyOn(card);
                        if (triggeredEvent.getSourceCard() instanceof Ability) {
                            Packet packet = new Packet(PacketType.INNERCARD_CHANGED);
                            packet.cards.add(triggeredEvent.getSourceCard());
                            packet.innerCardUUID = card.getUUID();
                            connection.enqueue(packet);
                        }
                    }
                }
                localPlayer.environmentSlot.getEnvironment().reset();
            }
        }
        enqueuePileUpdates();
        enqueueCardUpdates();
        for (Card card: getAllFieldCards()) {
            if (card.destroyed) {
                moveFromFieldToDiscardPile(card.getUUID());
            }
        }
        // if (connection != null) {
        // loadPacket();
        // }
        connection.sendAll();
    }

    public void startAttackAnimation(Unit attacker, Unit target) throws IOException {
        animator.attackAnimation = new AttackAnimation(attacker, target);
        Packet packet = new Packet(PacketType.START_ANIMATION);
        packet.animationSource = attacker.getUUID();
        packet.animationTarget = target.getUUID();
        connection.enqueue(packet);
    }

    public void render() {
        bs = getBufferStrategy();
        if (bs == null) {
            createBufferStrategy(2);
            return;
        }
        g = (Graphics2D) bs.getDrawGraphics();
        if (!getAllFieldEnvironments().isEmpty()) {
            Environment environ = getAllFieldEnvironments().get(0);
            environ.drawBackgroundImage(g);
        } else {
            g.setColor(Color.DARK_GRAY);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        g.setColor(Color.GREEN);
        Player player = localTurn ? localPlayer : remotePlayer;
        // g.drawString((localTurn ? "Your" : "Opponent's") + " " + turnNumber + "th turn", WIDTH -
        // 120, HEIGHT - 210);
        g.drawString("Turn number " + turnNumber, WIDTH - 120, HEIGHT - 210);
        // g.drawString("FPS: " + TimeKeeper.fps, WIDTH - 55, HEIGHT - 5);
        grid.draw(g);
        localPlayer.draw(g);
        remotePlayer.draw(g);
        detailCard.draw(g);
        g.drawString("Remote ID:" + remotePlayer.getPlayerId(), 80, 130);
        g.drawString("Local ID:" + localPlayer.getPlayerId(), 80, 430);
        if (player.cardAction != null) {
            g.drawImage(LoadedImages.reticleImage, inputHandler.getMouseX() - 20, inputHandler.getMouseY() - 20, 40, 40,
                    null);
        }
        g.setFont(new Font("Arial", Font.PLAIN, 50));
        if (connection == null) {
            g.setColor(Color.RED);
            g.drawString("NOT CONNECTED", 20, 60);
        } else if (!localTurn) {
            g.setColor(Color.CYAN);
            g.setFont(new Font("Arial", Font.PLAIN, 30));
            g.drawString("Waiting for the opponent...", 170, 310);
        }
        if (discarding) {
            g.drawString("Discard " + discardCount + " cards", 220, 380);
            g.setColor(new Color(0, 0, 0, 0.3f));
            g.fillRect(0, 0, WIDTH, HEIGHT);
        }
        animator.draw(g);
        g.dispose();
        bs.show();
    }

    private void startGame() throws IOException {
        System.out.println("Game started!");
        localPlayer.drawCards(5);
        if (localTurn) {
            System.out.println("You go first.");
            startTurn(1);
        }
    }

    private void startTurn(int turnNumber) throws IOException {
        this.turnNumber = turnNumber;
        localPlayer.setPhase(Phase.DRAW);
        localPlayer.drawCards(2);
        localPlayer.setPhase(Phase.ATTACK);
        localTurn = true;
    }

    public void endTurn() throws IOException {
        localPlayer.setPhase(Phase.END);
        for (Unit unit: localPlayer.getAllFieldUnits()) {
            unit.attackedThisTurn = false;
        }
        localTurn = false;
        localPlayer.drawPile.showCards = false;
        localPlayer.discardPile.showCards = false;
        // localPlayer.discardHand();
        Packet packet = new Packet(PacketType.START_TURN);
        packet.turnNumber = turnNumber + 1;
        connection.enqueue(packet);
    }

    // called when any of the phases started
    public void onPhase(Phase phase) throws IOException {
        for (Player player: new Player[] { localPlayer, remotePlayer }) {
            for (Slot slot: player.unitSlots) {
                if (!slot.isBlank()) {
                    Unit unit = slot.getUnit();
                    unit.onPhase(phase);
                }
            }
            for (Slot slot: player.abilitySlots) {
                if (!slot.isBlank()) {
                    slot.getAbility().onPhase(phase);
                }
            }

            Slot slot = player.environmentSlot; //
            if (!slot.isBlank()) {
                slot.getEnvironment().onPhase(phase);
            }
        }
    }

    public boolean discarding;
    private int discardCount;

    public void activateDiscard(int count) {
        discardCount = Math.min(count, localPlayer.getHandCount());
        discarding = discardCount > 0;
    }

    public void moveFromFieldToDiscardPile(UUID uuid) {
        Card card = cdb.getCardByUUID(uuid);
        System.out.println(localPlayer.getPlayerId() + " Trying to remove " + card + " " + card.getPlayerId());
        Player owner = getPlayer(card.getPlayerId());
        getSlotAt(card.getSlotPosition()).clear();
        // resetting runs on the original reference
        card.reset();
        owner.discardPile.add(card);
    }

    public void grantMassBlock(int amount, Player owner) throws IOException {
        for (Slot slot: owner.unitSlots) {
            if (!slot.isBlank()) {
                Unit unit = slot.getUnit();
                unit.addStatus(new Status(amount));
            }
        }
    }

    public void damageAllUnits(Card source, int amount) throws IOException {
        damageUnits(source, amount, localPlayer);
        damageUnits(source, amount, remotePlayer);
    }

    private void damageUnits(Card source, int amount, Player owner) throws IOException {
        for (Iterator<Unit> iter = owner.getUnitsOnField().iterator(); iter.hasNext();) {
            Unit unit = iter.next();
            unit.deltaHealth(source, -amount);
        }
    }

    public void placeUnit(Unit unit, SlotPosition position) throws IOException {
        Slot slot = getSlotAt(position);
        slot.setCard(unit);
        placedCards.add(unit);
        cdb.updateCard(unit);
    }

    public void placeAbility(Ability ability, SlotPosition position) throws IOException {
        Slot slot = getSlotAt(position);
        slot.setCard(ability);
        for (Environment environ: getAllFieldEnvironments()) {
            environ.onEvent("ABILITYPLACED");
        }
        placedCards.add(ability);

        if (!ability.getType().hasTag(AbilityTag.EQUIP)) {
            ability.destroyed = true;
        }
        cdb.updateCard(ability);
    }

    public void placeEnvironment(Environment environ, SlotPosition position) throws IOException {
        Slot slot = getSlotAt(position);
        if (!localPlayer.environmentSlot.isBlank()) {
            Card card = localPlayer.environmentSlot.getCard();
            card.destroyed = true;
            moveFromFieldToDiscardPile(card.getUUID());
            Packet packet = new Packet(PacketType.CARDS_UPDATED);
            packet.cards.add(card);
            connection.enqueue(packet);
        }
        if (!remotePlayer.environmentSlot.isBlank()) {
            Card card = remotePlayer.environmentSlot.getCard();
            card.destroyed = true;
            moveFromFieldToDiscardPile(card.getUUID());
            Packet packet = new Packet(PacketType.CARDS_UPDATED);
            packet.cards.add(card);
            connection.enqueue(packet);
        }
        environ.addAffectedSlots(localPlayer.unitSlots);
        environ.addAffectedSlots(remotePlayer.unitSlots);
        slot.setCard(environ);
        placedCards.add(environ);
        cdb.updateCard(environ);
    }

    private ArrayList<Card> getAllGameCards() {
        ArrayList<Card> cards = new ArrayList<>();
        for (Player player: new Player[] { localPlayer, remotePlayer }) {
            cards.addAll(player.getAllCards());
        }

        return cards;
    }

    private ArrayList<Card> getAllFieldCards() {
        ArrayList<Card> cards = new ArrayList<>();
        for (SlotPosition pos: SlotPosition.values()) {
            Slot slot = getSlotAt(pos);
            if (slot != null && !slot.isBlank()) { // slot = null for piles
                cards.add(slot.getCard());
            }
        }
        return cards;
    }

    private ArrayList<Environment> getAllFieldEnvironments() {
        ArrayList<Environment> cards = new ArrayList<>();
        for (SlotPosition pos: SlotPosition.values()) {
            if (pos.name().contains("ENVIRON")) {
                Slot slot = getSlotAt(pos);
                if (!slot.isBlank()) { // slot = null for piles
                    cards.add(slot.getEnvironment());
                }
            }
        }
        return cards;
    }

    private Slot getSlotAt(SlotPosition position) {
        for (Slot slot: localPlayer.unitSlots) {
            if (slot.getPosition() == position) {
                return slot;
            }
        }
        for (Slot slot: localPlayer.abilitySlots) {
            if (slot.getPosition() == position) {
                return slot;
            }
        }
        if (localPlayer.environmentSlot.getPosition() == position) {
            return localPlayer.environmentSlot;
        }
        for (Slot slot: remotePlayer.unitSlots) {
            if (slot.getPosition() == position) {
                return slot;
            }
        }
        for (Slot slot: remotePlayer.abilitySlots) {
            if (slot.getPosition() == position) {
                return slot;
            }
        }
        if (remotePlayer.environmentSlot.getPosition() == position) {
            return remotePlayer.environmentSlot;
        }
        return null;
    }

    public void hoveredOver(int mouseX, int mouseY) {
        for (Player player: new Player[] { localPlayer, remotePlayer }) {
            for (Slot slot: player.unitSlots) {
                if (!slot.isBlank()) {
                    Unit unit = slot.getUnit();
                    if (slot.contains(mouseX, mouseY)) {
                        detailCard = new DetailCard(unit);
                        return;
                    }
                }
            }
            for (Slot slot: player.abilitySlots) {
                if (!slot.isBlank()) {
                    Ability ability = slot.getAbility();
                    if (slot.contains(mouseX, mouseY)) {
                        detailCard = new DetailCard(ability);
                        return;
                    }
                }
            }
        }
        Card card = localPlayer.getHandCardAt(mouseX, mouseY);
        if (card != null) {
            detailCard = new DetailCard(card);
            return;
        }

        Slot slot = grid.getSlotAt(mouseX, mouseY);
        if (slot != null && !slot.isBlank()) {
            detailCard = new DetailCard(slot.getCard());
            return;
        }
    }

    public void releaseCardToField() throws IOException {
        if (localPlayer.activeCard == null) {
            return;
        }
        Slot slot = grid.getSlotAt(inputHandler.getMouseX(), inputHandler.getMouseY());
        if (slot == null) {
            return;
        }
        if (slot.getPlayerId() != localPlayer.getPlayerId()) {
            return;
        }
        if (!(slot.getPosition() == SlotPosition.ENVIRON_0 || slot.getPosition() == SlotPosition.ENVIRON_1)
                && !slot.isBlank()) {
            // placing an environment on top of another one replace it
            return;
        }
        if (slot.getPosition().name().contains("UNIT") && localPlayer.activeCard instanceof Unit) {
            localPlayer.placeCardOnField(localPlayer.activeCard, slot.getPosition());
        } else if (slot.getPosition().name().contains("ABILITY") && localPlayer.activeCard instanceof Ability) {
            localPlayer.placeCardOnField(localPlayer.activeCard, slot.getPosition());
        } else if (slot.getPosition().name().contains("ENVIRON") && localPlayer.activeCard instanceof Environment) {
            localPlayer.placeCardOnField(localPlayer.activeCard, slot.getPosition());
        }
    }

    public void handleClickAt(int button, int mouseX, int mouseY) throws IOException {
        if (button == MouseEvent.BUTTON3) {
            localPlayer.updateCardPositions();
            localPlayer.activeCard = null;
            grid.clickAt(button, mouseX, mouseY);
            return;
        }
        if (discarding) {
            if (localPlayer.contains(mouseX, mouseY)) {
                localPlayer.onClick(mouseX, mouseY);
                if (localPlayer.getDiscardQueueSize() == discardCount) {
                    discarding = false;
                    localPlayer.executeDiscard();
                }
            }
            return;
        }
        if (localPlayer.cardAction != null) {
            grid.clickAt(button, mouseX, mouseY);
            return;
        } else if (localPlayer.contains(mouseX, mouseY)) {
            localPlayer.onClick(mouseX, mouseY);
            return;
        } else if (localPlayer.drawPile.contains(mouseX, mouseY)) {
            localPlayer.drawPile.onClick(mouseX, mouseY);
            return;
        } else if (localPlayer.discardPile.contains(mouseX, mouseY)) {
            localPlayer.discardPile.onClick(mouseX, mouseY);
            return;
        } else if (remotePlayer.discardPile.contains(mouseX, mouseY)) {
            remotePlayer.discardPile.onClick(mouseX, mouseY);
            return;
        }
        grid.clickAt(button, mouseX, mouseY);
    }

    private void enqueuePileUpdates() throws IOException {
        if (localPlayer.drawPile.updated) {
            Packet packet = new Packet(PacketType.DRAW_PILE_UPDATED);
            packet.pileOwnerId = localPlayer.getPlayerId();
            for (Card card: localPlayer.drawPile.getCards()) {
                packet.uuids.add(card.getUUID());
            }
            connection.enqueue(packet);
            localPlayer.drawPile.updated = false;
        }
        if (localPlayer.discardPile.updated) {
            Packet packet = new Packet(PacketType.DISCARD_PILE_UPDATED);
            packet.pileOwnerId = localPlayer.getPlayerId();
            for (Card card: localPlayer.discardPile.getCards()) {
                packet.uuids.add(card.getUUID());
            }
            connection.enqueue(packet);
            localPlayer.discardPile.updated = false;
        }
        // if (remotePlayer.drawPile.updated) {
        // Packet packet = new Packet(PacketType.DRAW_PILE_UPDATED);
        // packet.pileOwnerId = remotePlayer.getPlayerId();
        // for (Card card: remotePlayer.drawPile.getCards()) {
        // packet.cardUUIDs2.add(card.getUUID());
        // }
        // connection.enqueue(packet);
        // remotePlayer.drawPile.updated = false;
        // }
        // if (remotePlayer.discardPile.updated) {
        // Packet packet = new Packet(PacketType.DISCARD_PILE_UPDATED);
        // packet.pileOwnerId = remotePlayer.getPlayerId();
        // for (Card card: remotePlayer.discardPile.getCards()) {
        // packet.cardUUIDs2.add(card.getUUID());
        // }
        // connection.enqueue(packet);
        // remotePlayer.discardPile.updated = false;
        // }
    }

    private void enqueueCardUpdates() throws IOException {
        ArrayList<Card> updatedCards = new ArrayList<>();
        for (Card card: getAllFieldCards()) {
            if (card.getPlayerId() != localPlayer.getPlayerId()) {
                // continue; // don't send updates on remote cards
                // updating remote after all because of cards that can affect the remote player's
                // cards
            }
            if (card.destroyed || card.updated) {
                updatedCards.add(card);
            }
            card.updated = false;
        }
        if (!updatedCards.isEmpty()) {
            Packet packet = new Packet(PacketType.CARDS_UPDATED);
            for (Card card: updatedCards) {
                packet.cards.add(card);
            }
            connection.enqueue(packet);
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public int getMouseX() {
        return inputHandler.getMouseX();
    }

    public int getMouseY() {
        return inputHandler.getMouseY();
    }

    public Player getPlayer(int playerId) {
        if (localPlayer.getPlayerId() == playerId) {
            return localPlayer;
        } else if (remotePlayer.getPlayerId() == playerId) {
            return remotePlayer;
        } else {
            return null;
        }
    }

    public void connectToServer() throws IOException {
        if (hosting && server == null) {
            server = new Server();
            Thread thread = new Thread(server);
            thread.start();
        }
        if (connection == null) {
            System.out.println("Connecting to server...");
            connection = new Connection();
            Packet packet = new Packet(PacketType.LOGIN_REQUEST);
            connection.enqueue(packet);
        }
    }

    public enum Side {
        TOP, BOTTOM;
    }

    @Override
    public void run() {
        while (running) {
            TimeKeeper.update();
            try {
                tick();
            } catch (IOException e) {
                e.printStackTrace();
            }
            render();
        }
        frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
    }

    public synchronized void stop() {
        running = false;
    }

    public static void main(String argv[]) throws IOException, InterruptedException {
        Game host = new Game(true);
        host.start();
        Game client = new Game(false);
        client.start();
    }

}
