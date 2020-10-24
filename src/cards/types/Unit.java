package cards.types;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Iterator;

import cards.Status;
import cards.types.CardType.UnitType;
import cards.types.CardType.UnitType.UnitTag;
import misc.Game;
import misc.GameEvent;
import misc.GameEvent.GameEventType;
import misc.Grid;
import misc.Phase;

public class Unit extends Card {
    private static final long serialVersionUID = 1L;
    private final UnitType type;
    public boolean attackedThisTurn = false;

    private int health, maxHealth, attack, block;
    private final ArrayList<Status> statuses = new ArrayList<>();
    public boolean canAttack = true;

    public Unit(UnitType type) {
        super(CardType.UNIT);
        this.type = type;
        reset();
    }

    @Override
    public void draw(Graphics2D g, int x, int y, float scale, float angle) {
        Color color = g.getColor();
        Font font = g.getFont();
        AffineTransform oldTx = g.getTransform();
        g.rotate(Math.toRadians(angle), x + (Game.CARD_WIDTH / 2) * scale, y + (Game.CARD_HEIGHT / 2) * scale);
        g.drawImage(type.getImage(), x, y, (int) (x + Game.CARD_WIDTH * scale), (int) (y + Game.CARD_HEIGHT * scale), 0,
                0, type.getImage().getWidth(null), type.getImage().getHeight(null), null);
        g.setTransform(oldTx);
        g.setColor(Color.CYAN);
        g.setFont(new Font("Arial", Font.BOLD, 17));
        g.drawString(String.format("%d/%d HP", health, maxHealth), x + 5, y + Game.CARD_HEIGHT - 35);
        g.setColor(Color.YELLOW);
        // g.drawString("PID: " + playerId, x + Game.CARD_WIDTH / 4, y + Game.CARD_HEIGHT / 2);
        g.drawString(attack + "/" + getBlockText(), x + 5, y + Game.CARD_HEIGHT - 10);
        if (!statuses.isEmpty()) {
            g.drawString("Status: " + statuses.get(0).duration, x, y + 25);
        }
        g.setFont(font);
        g.setColor(color);
    }

    public int getBaseBlock() {
        return block;
    }

    public int getTotalBlock() {
        int totalBlock = block;
        for (Status status: statuses) {
            totalBlock += status.block;
        }
        return Math.max(0, totalBlock);
    }

    public String getBlockText() {
        int bonusBlock = 0;
        for (Status status: statuses) {
            bonusBlock += status.block;
        }

        if (bonusBlock > 0) {
            return block + "+" + bonusBlock;
        } else if (bonusBlock < 0) {
            if (bonusBlock < -block) {
                bonusBlock = -block;
            }
            return block + "" + bonusBlock;
        } else {
            return "" + block;
        }
    }

    @Override
    public void reset() {
        // slotPosition = null;
        health = type.getHealth();
        maxHealth = health;
        attack = type.getAttack();
        block = type.getBlock();
        statuses.clear();
        canAttack = true;
        attackedThisTurn = false;
        updated = true;
        destroyed = false;
    }

    public UnitType getType() {
        return type;
    }

    @Override
    public Image getImage() {
        return type.getImage();
    }

    @Override
    public String getName() {
        return type.getName();
    }

    @Override
    public String getDescription() {
        return type.getDescription();
    }

    @Override
    public String[] getDescriptionLines() {
        return type.getDescriptionLines();
    }

    @Override
    public Phase getPhase() {
        return Phase.DEFEND;
    }

    @Override
    public String toString() {
        // return String.format("%s (%d HP / %d ATK / %d BLK)", type.getName(), health, attack,
        // getTotalBlock());
        return String.format("%s (%s)", type.getName(), slotPosition);
    }

    public void onReflectedAttack(Card source, int amount) {
        takeDamage(source, amount);
    }

    public void onAttack(Unit attacker) {
        int attack = attacker.getAttack();
        takeDamage(attacker, attack);

        if (Math.random() < 0) {
            // if this unit has Thorns, damage the attacker back
            attacker.onReflectedAttack(this, 1);
        }

        updated = true;
    }

    private void takeDamage(Card source, int attack) {
        if (getTotalBlock() > 0) {
            if (attack >= 2 * getTotalBlock()) {
                deltaHealth(source, -attack + getTotalBlock());
            } else if (attack > getTotalBlock()) {
                attack = getTotalBlock();
            }
            addStatus(new Status(-attack));
        } else {
            deltaHealth(source, -attack);
        }
    }

    public void deltaHealth(Card source, int delta) {
        health = Math.min(Math.max(health + delta, -1), maxHealth);
        if (health < 0) {
            destroyed = true;
            lastEvent = new GameEvent(GameEventType.UNIT_DESTROYED, source, this);
        }
        updated = true;
    }

    public int getAttack() {
        return attack;
    }

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public ArrayList<Status> getStatuses() {
        return statuses;
    }

    public void addStatus(Status status) {
        // merging all statuses because they're all block
        if (!statuses.isEmpty()) {
            statuses.get(0).block += status.block;
        } else {
            statuses.add(status);
        }
        updated = true;
    }

    @Override
    public boolean isPlayable(Game game) {
        Phase currentPhase = game.localPlayer.getPhase();
        return !(currentPhase == Phase.DEFEND && getPhase() == Phase.ATTACK);
    }

    @Override
    public void onPhase(Phase phase) {
        if (phase == Phase.DRAW) {
            for (Iterator<Status> iter = statuses.iterator(); iter.hasNext();) {
                Status status = iter.next();
                if (status.duration == 1) {
                    iter.remove();
                } else {
                    status.duration -= 1;
                }
                updated = true;
            }
            if (type.hasTag(UnitTag.BURNING)) {
                lastEvent = new GameEvent(GameEventType.BURNING_TICK, this, null);
            }
        }
    }

    @Override
    public void updateWith(Card other) {
        if (!(other instanceof Unit)) {
            return;
        }
        Unit unit = (Unit) other;
        health = unit.getHealth();
        maxHealth = unit.getMaxHealth();
        attack = unit.getAttack();
        block = unit.getBaseBlock();
        statuses.clear();
        for (Status status: unit.getStatuses()) {
            statuses.add(status);
        }
        canAttack = unit.canAttack;
        slotPosition = unit.slotPosition;
        destroyed = unit.destroyed;
        updated = true;
    }

    public boolean canReach(Unit target) {
        if (type.hasTag(UnitTag.RANGED)) {
            return true;
        }
        int attackRange = 1;
        if (Grid.distanceBetween(slotPosition, target.slotPosition) <= attackRange) {
            return true;
        }
        return false;
    }

    @Override
    public void onEvent(GameEvent event) {

    }

    public void deltaAttack(int amount) {
        // TODO separate base attack from attack modifiers
        this.attack += amount;
        updated = true;
    }
}
