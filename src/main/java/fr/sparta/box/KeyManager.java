package fr.sparta.box;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

/**
 * Cles physiques (objets marques par un tag PDC contenant l'id de la box) et
 * cles virtuelles (compteur par joueur, sauvegarde dans keys.yml).
 */
public final class KeyManager {

    private final BoxPlugin plugin;
    private final NamespacedKey tag;
    private final File file;
    private final Map<UUID, Map<String, Integer>> virtualKeys = new HashMap<>();

    public KeyManager(BoxPlugin plugin) {
        this.plugin = plugin;
        this.tag = new NamespacedKey(plugin, "crate_key");
        this.file = new File(plugin.getDataFolder(), "keys.yml");
    }

    public void load() {
        virtualKeys.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String rawUuid : yaml.getKeys(false)) {
            ConfigurationSection section = yaml.getConfigurationSection(rawUuid);
            if (section == null) {
                continue;
            }
            try {
                UUID uuid = UUID.fromString(rawUuid);
                Map<String, Integer> keys = new HashMap<>();
                for (String crateId : section.getKeys(false)) {
                    int amount = section.getInt(crateId);
                    if (amount > 0) {
                        keys.put(crateId, amount);
                    }
                }
                virtualKeys.put(uuid, keys);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("keys.yml : UUID invalide ignore : " + rawUuid);
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        virtualKeys.forEach((uuid, keys) -> keys.forEach((crateId, amount) -> {
            if (amount > 0) {
                yaml.set(uuid + "." + crateId, amount);
            }
        }));
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Impossible de sauvegarder keys.yml", e);
        }
    }

    public int virtual(UUID player, Crate crate) {
        return virtualKeys.getOrDefault(player, Map.of()).getOrDefault(crate.id(), 0);
    }

    public Map<String, Integer> virtualKeys(UUID player) {
        return virtualKeys.getOrDefault(player, Map.of());
    }

    /** Ajoute (ou retire si negatif) des cles virtuelles, jamais en dessous de zero. */
    public void addVirtual(UUID player, Crate crate, int amount) {
        Map<String, Integer> keys = virtualKeys.computeIfAbsent(player, uuid -> new HashMap<>());
        int value = Math.max(0, keys.getOrDefault(crate.id(), 0) + amount);
        if (value == 0) {
            keys.remove(crate.id());
        } else {
            keys.put(crate.id(), value);
        }
        save();
    }

    public ItemStack physical(Crate crate, int amount) {
        return crate.keyItem(tag, amount);
    }

    /** Id de la box ouverte par cet objet, ou null si ce n'est pas une cle. */
    public String keyId(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasItemMeta()) {
            return null;
        }
        return stack.getItemMeta().getPersistentDataContainer().get(tag, PersistentDataType.STRING);
    }

    public boolean isKey(ItemStack stack) {
        return keyId(stack) != null;
    }

    public int physicalCount(Player player, Crate crate) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (crate.id().equals(keyId(stack))) {
                count += stack.getAmount();
            }
        }
        return count;
    }

    public boolean hasKey(Player player, Crate crate) {
        return virtual(player.getUniqueId(), crate) > 0 || physicalCount(player, crate) > 0;
    }

    /** Consomme une cle : virtuelle d'abord, puis celle en main, puis n'importe ou dans l'inventaire. */
    public boolean takeKey(Player player, Crate crate) {
        if (virtual(player.getUniqueId(), crate) > 0) {
            addVirtual(player.getUniqueId(), crate, -1);
            return true;
        }
        PlayerInventory inventory = player.getInventory();
        int hand = inventory.getHeldItemSlot();
        if (consume(inventory, hand, crate)) {
            return true;
        }
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (slot != hand && consume(inventory, slot, crate)) {
                return true;
            }
        }
        return false;
    }

    private boolean consume(PlayerInventory inventory, int slot, Crate crate) {
        ItemStack stack = inventory.getItem(slot);
        if (!crate.id().equals(keyId(stack))) {
            return false;
        }
        if (stack.getAmount() <= 1) {
            inventory.setItem(slot, null);
        } else {
            stack.setAmount(stack.getAmount() - 1);
        }
        return true;
    }
}
