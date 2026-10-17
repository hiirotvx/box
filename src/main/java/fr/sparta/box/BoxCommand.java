package fr.sparta.box;

import fr.sparta.box.gui.EditorGui;
import fr.sparta.box.gui.PreviewGui;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class BoxCommand implements TabExecutor {

    private static final List<String> ADMIN = List.of("create", "delete", "list", "set", "remove", "edit",
            "addreward", "preview", "open", "give", "take", "keys", "reload", "help");

    private final BoxPlugin plugin;

    public BoxCommand(BoxPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("keys") && args.length <= 1) {
            keys(sender, args);
            return true;
        }
        if (!sender.hasPermission("box.admin")) {
            plugin.send(sender, "no-permission");
            return true;
        }
        switch (sub) {
            case "create" -> create(sender, args);
            case "delete" -> delete(sender, args);
            case "list" -> list(sender);
            case "set" -> set(sender, args);
            case "remove" -> remove(sender);
            case "edit" -> edit(sender, args);
            case "addreward" -> addReward(sender, args);
            case "preview" -> preview(sender, args);
            case "open" -> open(sender, args);
            case "give" -> give(sender, args);
            case "take" -> take(sender, args);
            case "keys" -> keys(sender, args);
            case "reload" -> {
                plugin.reload();
                plugin.send(sender, "reloaded");
            }
            default -> help(sender);
        }
        return true;
    }

    private void help(CommandSender sender) {
        for (String line : plugin.textList("messages.help")) {
            sender.sendMessage(Text.parse(line));
        }
    }

    private void create(CommandSender sender, String[] args) {
        if (args.length < 2) {
            help(sender);
            return;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        if (!id.matches("[a-z0-9_-]{1,32}")) {
            plugin.send(sender, "box-invalid-id");
            return;
        }
        if (plugin.crates().get(id) != null) {
            plugin.send(sender, "box-exists", Placeholder.unparsed("box", id));
            return;
        }
        plugin.crates().create(id);
        plugin.send(sender, "box-created", Placeholder.unparsed("box", id));
    }

    private void delete(CommandSender sender, String[] args) {
        Crate crate = crateArg(sender, args, 1);
        if (crate == null) {
            return;
        }
        plugin.holograms().refresh(crate);
        plugin.crates().delete(crate);
        plugin.send(sender, "box-deleted", Placeholder.unparsed("box", crate.id()));
    }

    private void list(CommandSender sender) {
        Collection<Crate> all = plugin.crates().all();
        plugin.send(sender, "box-list-header", Placeholder.unparsed("count", String.valueOf(all.size())));
        for (Crate crate : all) {
            sender.sendMessage(Text.parse(plugin.text("messages.box-list-entry"),
                    Placeholder.unparsed("box", crate.id()),
                    Placeholder.component("name", crate.name()),
                    Placeholder.unparsed("rewards", String.valueOf(crate.rewards().size())),
                    Placeholder.unparsed("locations", String.valueOf(crate.locations().size()))));
        }
    }

    private void set(CommandSender sender, String[] args) {
        Player player = player(sender);
        if (player == null) {
            return;
        }
        Crate crate = crateArg(sender, args, 1);
        if (crate == null) {
            return;
        }
        Block block = target(player);
        if (block == null) {
            return;
        }
        Crate existing = plugin.crates().at(block);
        if (existing != null) {
            plugin.send(sender, "block-already-box", Placeholder.unparsed("box", existing.id()));
            return;
        }
        plugin.crates().addLocation(crate, BlockPos.of(block));
        plugin.send(sender, "block-set", Placeholder.unparsed("box", crate.id()));
    }

    private void remove(CommandSender sender) {
        Player player = player(sender);
        if (player == null) {
            return;
        }
        Block block = target(player);
        if (block == null) {
            return;
        }
        Crate crate = plugin.crates().at(block);
        if (crate == null) {
            plugin.send(sender, "block-not-box");
            return;
        }
        BlockPos pos = BlockPos.of(block);
        plugin.holograms().remove(pos);
        plugin.crates().removeLocation(crate, pos);
        plugin.send(sender, "block-removed", Placeholder.unparsed("box", crate.id()));
    }

    private void edit(CommandSender sender, String[] args) {
        Player player = player(sender);
        Crate crate = player == null ? null : crateArg(sender, args, 1);
        if (crate != null) {
            new EditorGui(plugin, crate, player).open(player);
        }
    }

    private void addReward(CommandSender sender, String[] args) {
        Player player = player(sender);
        Crate crate = player == null ? null : crateArg(sender, args, 1);
        if (crate == null) {
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.isEmpty()) {
            plugin.send(sender, "reward-hand-empty");
            return;
        }
        double weight = 10;
        if (args.length >= 3) {
            Double parsed = parseDouble(sender, args[2]);
            if (parsed == null) {
                return;
            }
            weight = parsed;
        }
        crate.addReward(Reward.fromItem(crate.nextRewardId(), hand, weight));
        plugin.crates().save(crate);
        plugin.send(sender, "reward-added", Placeholder.unparsed("box", crate.id()),
                Placeholder.unparsed("weight", args.length >= 3 ? args[2] : "10"));
    }

    private void preview(CommandSender sender, String[] args) {
        Crate crate = crateArg(sender, args, 1);
        Player target = crate == null ? null : targetPlayer(sender, args, 2);
        if (target != null) {
            new PreviewGui(plugin, crate, target).open(target);
        }
    }

    private void open(CommandSender sender, String[] args) {
        Crate crate = crateArg(sender, args, 1);
        Player target = crate == null ? null : targetPlayer(sender, args, 2);
        if (target != null) {
            plugin.opener().start(target, crate, false);
        }
    }

    /** /box give <joueur|*> <box> [quantite] [virtuelle] */
    private void give(CommandSender sender, String[] args) {
        if (args.length < 3) {
            help(sender);
            return;
        }
        Crate crate = crateArg(sender, args, 2);
        if (crate == null) {
            return;
        }
        int amount = 1;
        if (args.length >= 4) {
            Integer parsed = parseInt(sender, args[3]);
            if (parsed == null) {
                return;
            }
            amount = parsed;
        }
        boolean virtual = args.length >= 5 && args[4].toLowerCase(Locale.ROOT).startsWith("v");
        String type = virtual ? "virtuelle" : "physique";

        if (args[1].equals("*")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                giveKeys(player, crate, amount, virtual);
            }
            plugin.send(sender, "key-given", keyPlaceholders(crate, amount, "*", type));
            return;
        }
        Player online = Bukkit.getPlayerExact(args[1]);
        if (online != null) {
            giveKeys(online, crate, amount, virtual);
            plugin.send(sender, "key-given", keyPlaceholders(crate, amount, online.getName(), type));
            return;
        }
        // Joueur hors ligne : seulement des cles virtuelles.
        OfflinePlayer offline = virtual ? Bukkit.getOfflinePlayerIfCached(args[1]) : null;
        if (offline == null) {
            plugin.send(sender, "player-not-found", Placeholder.unparsed("player", args[1]));
            return;
        }
        plugin.keys().addVirtual(offline.getUniqueId(), crate, amount);
        plugin.send(sender, "key-given", keyPlaceholders(crate, amount, args[1], type));
    }

    private void giveKeys(Player player, Crate crate, int amount, boolean virtual) {
        if (virtual) {
            plugin.keys().addVirtual(player.getUniqueId(), crate, amount);
        } else {
            for (ItemStack leftover : player.getInventory().addItem(plugin.keys().physical(crate, amount)).values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }
        plugin.send(player, "key-received", Placeholder.unparsed("amount", String.valueOf(amount)),
                Placeholder.component("box", crate.name()));
    }

    private TagResolver[] keyPlaceholders(Crate crate, int amount, String player, String type) {
        return new TagResolver[] {
                Placeholder.unparsed("amount", String.valueOf(amount)),
                Placeholder.unparsed("box", crate.id()),
                Placeholder.unparsed("player", player),
                Placeholder.unparsed("type", type)};
    }

    /** /box take <joueur> <box> [quantite] (cles virtuelles) */
    private void take(CommandSender sender, String[] args) {
        if (args.length < 3) {
            help(sender);
            return;
        }
        OfflinePlayer target = offline(args[1]);
        if (target == null) {
            plugin.send(sender, "player-not-found", Placeholder.unparsed("player", args[1]));
            return;
        }
        Crate crate = crateArg(sender, args, 2);
        if (crate == null) {
            return;
        }
        int amount = 1;
        if (args.length >= 4) {
            Integer parsed = parseInt(sender, args[3]);
            if (parsed == null) {
                return;
            }
            amount = parsed;
        }
        plugin.keys().addVirtual(target.getUniqueId(), crate, -amount);
        plugin.send(sender, "key-taken", keyPlaceholders(crate, amount, args[1], "virtuelle"));
    }

    /** /box keys [joueur] */
    private void keys(CommandSender sender, String[] args) {
        UUID uuid;
        String name;
        if (args.length >= 2) {
            if (!sender.hasPermission("box.admin")) {
                plugin.send(sender, "no-permission");
                return;
            }
            OfflinePlayer target = offline(args[1]);
            if (target == null) {
                plugin.send(sender, "player-not-found", Placeholder.unparsed("player", args[1]));
                return;
            }
            uuid = target.getUniqueId();
            name = args[1];
        } else {
            Player player = player(sender);
            if (player == null) {
                return;
            }
            if (!sender.hasPermission("box.keys")) {
                plugin.send(sender, "no-permission");
                return;
            }
            uuid = player.getUniqueId();
            name = player.getName();
        }
        plugin.send(sender, "keys-header", Placeholder.unparsed("player", name));
        Map<String, Integer> keys = plugin.keys().virtualKeys(uuid);
        if (keys.isEmpty()) {
            sender.sendMessage(Text.parse(plugin.text("messages.keys-none")));
            return;
        }
        keys.forEach((crateId, amount) -> {
            Crate crate = plugin.crates().get(crateId);
            sender.sendMessage(Text.parse(plugin.text("messages.keys-entry"),
                    crate == null ? Placeholder.unparsed("name", crateId) : Placeholder.component("name", crate.name()),
                    Placeholder.unparsed("amount", String.valueOf(amount))));
        });
    }

    // ---------------------------------------------------------------- utilitaires

    private Player player(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        plugin.send(sender, "player-only");
        return null;
    }

    private Block target(Player player) {
        int distance = Math.max(1, plugin.getConfig().getInt("target-distance", 6));
        Block block = player.getTargetBlockExact(distance);
        if (block == null || block.getType().isAir()) {
            plugin.send(player, "no-target-block", Placeholder.unparsed("distance", String.valueOf(distance)));
            return null;
        }
        return block;
    }

    private Crate crateArg(CommandSender sender, String[] args, int index) {
        if (args.length <= index) {
            help(sender);
            return null;
        }
        Crate crate = plugin.crates().get(args[index]);
        if (crate == null) {
            plugin.send(sender, "unknown-box", Placeholder.unparsed("box", args[index]));
        }
        return crate;
    }

    /** Joueur en ligne a l'index donne, ou l'emetteur lui-meme si absent. */
    private Player targetPlayer(CommandSender sender, String[] args, int index) {
        if (args.length <= index) {
            return player(sender);
        }
        Player target = Bukkit.getPlayerExact(args[index]);
        if (target == null) {
            plugin.send(sender, "player-not-found", Placeholder.unparsed("player", args[index]));
        }
        return target;
    }

    private OfflinePlayer offline(String name) {
        Player online = Bukkit.getPlayerExact(name);
        return online != null ? online : Bukkit.getOfflinePlayerIfCached(name);
    }

    private Integer parseInt(CommandSender sender, String raw) {
        try {
            int value = Integer.parseInt(raw);
            if (value > 0) {
                return value;
            }
        } catch (NumberFormatException ignored) {
            // message ci-dessous
        }
        plugin.send(sender, "invalid-number", Placeholder.unparsed("value", raw));
        return null;
    }

    private Double parseDouble(CommandSender sender, String raw) {
        try {
            double value = Double.parseDouble(raw.replace(',', '.'));
            if (value > 0 && Double.isFinite(value)) {
                return value;
            }
        } catch (NumberFormatException ignored) {
            // message ci-dessous
        }
        plugin.send(sender, "invalid-number", Placeholder.unparsed("value", raw));
        return null;
    }

    // ---------------------------------------------------------------- completion

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        boolean admin = sender.hasPermission("box.admin");
        if (args.length == 1) {
            return filter(admin ? ADMIN : List.of("keys"), args[0]);
        }
        if (!admin) {
            return List.of();
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        List<String> boxes = new ArrayList<>(plugin.crates().ids());
        return switch (sub) {
            case "delete", "set", "edit", "addreward" -> args.length == 2 ? filter(boxes, args[1]) : List.of();
            case "preview", "open" -> switch (args.length) {
                case 2 -> filter(boxes, args[1]);
                case 3 -> filter(onlineNames(), args[2]);
                default -> List.of();
            };
            case "give", "take" -> switch (args.length) {
                case 2 -> {
                    List<String> names = onlineNames();
                    if (sub.equals("give")) {
                        names.add(0, "*");
                    }
                    yield filter(names, args[1]);
                }
                case 3 -> filter(boxes, args[2]);
                case 4 -> filter(List.of("1", "5", "10", "64"), args[3]);
                case 5 -> sub.equals("give") ? filter(List.of("physique", "virtuelle"), args[4]) : List.of();
                default -> List.of();
            };
            case "keys" -> args.length == 2 ? filter(onlineNames(), args[1]) : List.of();
            default -> List.of();
        };
    }

    private static List<String> onlineNames() {
        List<String> names = new ArrayList<>();
        Bukkit.getOnlinePlayers().forEach(player -> names.add(player.getName()));
        return names;
    }

    private static List<String> filter(Collection<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(option);
            }
        }
        return out;
    }
}
