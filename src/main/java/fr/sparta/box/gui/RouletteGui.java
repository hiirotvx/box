package fr.sparta.box.gui;

import fr.sparta.box.BoxPlugin;
import fr.sparta.box.Crate;
import fr.sparta.box.Reward;
import fr.sparta.box.Text;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

/**
 * Animation roulette : la ligne du milieu defile en ralentissant et s'arrete
 * sur la recompense, tiree au sort AVANT l'animation. Fermer le menu donne
 * la recompense immediatement.
 */
public final class RouletteGui extends Gui {

    private static final int FIRST = 9;
    private static final int CENTER = 13;
    private static final Sound TICK = Sound.sound(Key.key("block.note_block.hat"), Sound.Source.MASTER, 0.8f, 1.6f);

    private final BoxPlugin plugin;
    private final Crate crate;
    private final Player player;
    private final Reward winner;
    private final List<Reward> strip = new ArrayList<>();
    private final int steps;
    private int step;
    private boolean finished;
    private BukkitTask task;

    public RouletteGui(BoxPlugin plugin, Crate crate, Player player, Reward winner) {
        this.plugin = plugin;
        this.crate = crate;
        this.player = player;
        this.winner = winner;
        this.steps = Math.max(10, plugin.getConfig().getInt("roulette-steps", 40));
        this.inventory = Bukkit.createInventory(this, 27, crate.name());

        // Bande de recompenses : au dernier pas, la case du centre (index steps + 4) est le gagnant.
        for (int i = 0; i < steps + 9; i++) {
            Reward random = crate.roll();
            strip.add(random == null ? winner : random);
        }
        strip.set(steps + 4, winner);

        ItemStack frame = Text.filler(Material.GRAY_STAINED_GLASS_PANE);
        ItemStack pointer = Text.filler(Material.LIME_STAINED_GLASS_PANE);
        for (int slot = 0; slot < 9; slot++) {
            inventory.setItem(slot, frame);
            inventory.setItem(slot + 18, frame);
        }
        inventory.setItem(4, pointer);
        inventory.setItem(22, pointer);
    }

    @Override
    public void open(Player viewer) {
        draw();
        super.open(viewer);
        schedule();
    }

    private void draw() {
        for (int i = 0; i < 9; i++) {
            inventory.setItem(FIRST + i, strip.get(step + i).icon());
        }
    }

    /** Delai avant le pas suivant : rapide au debut, de plus en plus lent a la fin. */
    private long delay() {
        double progress = (double) step / steps;
        if (progress < 0.5) {
            return 1;
        }
        if (progress < 0.75) {
            return 2;
        }
        if (progress < 0.9) {
            return 4;
        }
        return 7;
    }

    private void schedule() {
        task = Bukkit.getScheduler().runTaskLater(plugin, this::advance, delay());
    }

    private void advance() {
        if (finished) {
            return;
        }
        step++;
        draw();
        player.playSound(TICK);
        if (step >= steps) {
            finish();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.getOpenInventory().getTopInventory().getHolder(false) == this) {
                    player.closeInventory();
                }
            }, 40L);
        } else {
            schedule();
        }
    }

    private void finish() {
        if (finished) {
            return;
        }
        finished = true;
        if (task != null) {
            task.cancel();
        }
        ItemStack highlight = inventory.getItem(CENTER);
        for (int slot = FIRST; slot < FIRST + 9; slot++) {
            if (slot != CENTER) {
                inventory.setItem(slot, Text.filler(Material.GRAY_STAINED_GLASS_PANE));
            }
        }
        inventory.setItem(CENTER, highlight);
        plugin.opener().deliver(player, crate, winner);
    }

    @Override
    public void close(InventoryCloseEvent event) {
        finish();
    }
}
