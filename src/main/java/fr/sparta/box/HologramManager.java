package fr.sparta.box;

import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

/**
 * Hologrammes (entites TextDisplay non sauvegardees) et particules au-dessus des box.
 * Les entites disparaissent avec leur chunk et sont recreees quand il est recharge.
 */
public final class HologramManager {

    private final BoxPlugin plugin;
    private final NamespacedKey tag;
    private final Map<BlockPos, TextDisplay> displays = new HashMap<>();
    private BukkitTask task;
    private int ticks;

    public HologramManager(BoxPlugin plugin) {
        this.plugin = plugin;
        this.tag = new NamespacedKey(plugin, "hologram");
    }

    public void start() {
        stop();
        // Hologrammes oublies par un arret brutal.
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntitiesByClass(TextDisplay.class)) {
                if (entity.getPersistentDataContainer().has(tag)) {
                    entity.remove();
                }
            }
        }
        ticks = 0;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 10L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        removeAll();
    }

    private void tick() {
        boolean refresh = ticks++ % 4 == 0;
        for (Crate crate : plugin.crates().all()) {
            for (BlockPos pos : crate.locations()) {
                if (!pos.isLoaded()) {
                    displays.remove(pos);
                    continue;
                }
                if (refresh) {
                    ensureHologram(crate, pos);
                }
                if (crate.particle() != null) {
                    Location center = pos.center();
                    center.getWorld().spawnParticle(crate.particle(), center, 4, 0.45, 0.45, 0.45, 0.01);
                }
            }
        }
    }

    private void ensureHologram(Crate crate, BlockPos pos) {
        TextDisplay current = displays.get(pos);
        if (!crate.hologram()) {
            if (current != null) {
                current.remove();
                displays.remove(pos);
            }
            return;
        }
        if (current != null && current.isValid()) {
            return;
        }
        Location location = pos.center().add(0, crate.hologramHeight() - 0.5, 0);
        Component text = Component.join(JoinConfiguration.newlines(),
                crate.hologramLines().stream().map(Text::parse).toList());
        TextDisplay display = location.getWorld().spawn(location, TextDisplay.class, entity -> {
            entity.setPersistent(false);
            entity.text(text);
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setShadowed(true);
            entity.setAlignment(TextDisplay.TextAlignment.CENTER);
            entity.getPersistentDataContainer().set(tag, PersistentDataType.BYTE, (byte) 1);
        });
        displays.put(pos, display);
    }

    /** Supprime les hologrammes d'une box (ils seront recrees au prochain passage si besoin). */
    public void refresh(Crate crate) {
        crate.locations().forEach(this::remove);
    }

    public void remove(BlockPos pos) {
        TextDisplay display = displays.remove(pos);
        if (display != null) {
            display.remove();
        }
    }

    public void removeAll() {
        displays.values().forEach(Entity::remove);
        displays.clear();
    }
}
