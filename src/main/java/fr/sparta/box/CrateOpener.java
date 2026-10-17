package fr.sparta.box;

import fr.sparta.box.gui.RouletteGui;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/** Logique d'ouverture : verifications, cle, tirage, animation et don. */
public final class CrateOpener {

    private static final Sound WIN = Sound.sound(Key.key("entity.player.levelup"), Sound.Source.MASTER, 1f, 1.2f);
    private static final Sound DENY = Sound.sound(Key.key("entity.villager.no"), Sound.Source.MASTER, 1f, 1f);

    private final BoxPlugin plugin;
    private final Set<UUID> opening = new HashSet<>();
    private final Map<String, Long> cooldowns = new HashMap<>();

    public CrateOpener(BoxPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Ouverture par un joueur sur une box posee.
     * @param block centre du bloc clique (pour le recul), ou null
     */
    public void open(Player player, Crate crate, Location block, boolean instant) {
        if (!player.hasPermission("box.open")
                || (!crate.permission().isBlank() && !player.hasPermission(crate.permission()))) {
            plugin.send(player, "no-permission");
            return;
        }
        if (opening.contains(player.getUniqueId())) {
            plugin.send(player, "already-opening");
            return;
        }
        if (crate.totalWeight() <= 0) {
            plugin.send(player, "no-rewards");
            return;
        }
        String cooldownKey = player.getUniqueId() + ":" + crate.id();
        long now = System.currentTimeMillis();
        Long until = cooldowns.get(cooldownKey);
        if (until != null && until > now && !player.hasPermission("box.admin")) {
            plugin.send(player, "cooldown", Placeholder.unparsed("time", formatDuration(until - now)));
            return;
        }
        if (!plugin.keys().takeKey(player, crate)) {
            plugin.send(player, "no-key", Placeholder.component("box", crate.name()));
            player.playSound(DENY);
            if (crate.pushBack() && block != null) {
                Vector away = player.getLocation().toVector().subtract(block.toVector()).setY(0);
                if (away.lengthSquared() > 1.0E-4) {
                    player.setVelocity(away.normalize().multiply(0.7).setY(0.35));
                }
            }
            return;
        }
        if (crate.cooldownSeconds() > 0) {
            cooldowns.put(cooldownKey, now + crate.cooldownSeconds() * 1000L);
        }
        start(player, crate, instant);
    }

    /** Ouverture sans cle ni verification (/box open). */
    public void start(Player player, Crate crate, boolean instant) {
        Reward reward = crate.roll();
        if (reward == null) {
            plugin.send(player, "no-rewards");
            return;
        }
        if (crate.animation() && !instant) {
            opening.add(player.getUniqueId());
            new RouletteGui(plugin, crate, player, reward).open(player);
        } else {
            deliver(player, crate, reward);
        }
    }

    /** Donne la recompense : objet, commandes, messages. */
    public void deliver(Player player, Crate crate, Reward reward) {
        opening.remove(player.getUniqueId());
        ItemStack gift = reward.giftItem();
        if (gift != null) {
            for (ItemStack leftover : player.getInventory().addItem(gift).values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }
        for (String command : reward.commands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
        }
        plugin.send(player, "win", Placeholder.component("reward", reward.displayName()));
        player.playSound(WIN);
        if (reward.broadcast()) {
            Bukkit.broadcast(Text.parse(plugin.text("prefix") + plugin.text("messages.broadcast"),
                    Placeholder.unparsed("player", player.getName()),
                    Placeholder.component("reward", reward.displayName()),
                    Placeholder.component("box", crate.name())));
        }
    }

    private static String formatDuration(long millis) {
        long seconds = (millis + 999) / 1000;
        long hours = seconds / 3600;
        long minutes = seconds % 3600 / 60;
        long secs = seconds % 60;
        if (hours > 0) {
            return hours + "h " + minutes + "min";
        }
        if (minutes > 0) {
            return minutes + "min " + secs + "s";
        }
        return secs + "s";
    }
}
