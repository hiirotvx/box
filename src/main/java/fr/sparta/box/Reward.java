package fr.sparta.box;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

/**
 * Une recompense de box. L'objet peut etre decrit a la main dans le fichier
 * (material, amount, item-name, lore, glint) ou stocke complet en Base64 (cle "item").
 * Le format Base64 est celui de Paper ({@link ItemStack#serializeAsBytes()}) : il
 * passe par le DataFixer de Minecraft, donc les objets survivent aux mises a jour du serveur.
 */
public final class Reward {

    private final String id;
    private String name;
    private double weight;
    private boolean broadcast;
    private boolean giveItem;
    private List<String> commands;
    private ItemStack item;

    // Description manuelle, conservee telle quelle a la sauvegarde.
    private boolean encoded;
    private String material;
    private int amount;
    private String itemName;
    private List<String> lore;
    private boolean glint;

    private Reward(String id) {
        this.id = id;
    }

    /** Nouvelle recompense a partir d'un objet reel (editeur, /box addreward). */
    public static Reward fromItem(String id, ItemStack stack, double weight) {
        Reward reward = new Reward(id);
        reward.weight = weight;
        reward.giveItem = true;
        reward.commands = new ArrayList<>();
        reward.encoded = true;
        reward.item = stack.clone();
        return reward;
    }

    public static Reward load(String id, ConfigurationSection section, Logger logger) {
        Reward reward = new Reward(id);
        reward.name = section.getString("name");
        reward.weight = section.getDouble("weight", 1);
        reward.broadcast = section.getBoolean("broadcast", false);
        reward.giveItem = section.getBoolean("give-item", true);
        reward.commands = new ArrayList<>(section.getStringList("commands"));

        String encodedItem = section.getString("item");
        if (encodedItem != null && !encodedItem.isBlank()) {
            try {
                reward.item = ItemStack.deserializeBytes(Base64.getDecoder().decode(encodedItem));
                reward.encoded = true;
            } catch (RuntimeException e) {
                logger.warning("Recompense '" + id + "' : objet encode illisible (" + e.getMessage() + ").");
            }
        }
        if (reward.item == null) {
            reward.encoded = false;
            reward.material = section.getString("material", "STONE");
            reward.amount = Math.max(1, section.getInt("amount", 1));
            reward.itemName = section.getString("item-name");
            reward.lore = new ArrayList<>(section.getStringList("lore"));
            reward.glint = section.getBoolean("glint", false);
            reward.item = reward.buildManualItem(logger);
        }
        return reward;
    }

    private ItemStack buildManualItem(Logger logger) {
        Material type = Material.matchMaterial(material);
        if (type == null || !type.isItem() || type.isAir()) {
            logger.warning("Recompense '" + id + "' : materiau inconnu '" + material + "', remplace par BARRIER.");
            type = Material.BARRIER;
        }
        // La quantite peut depasser une pile : addItem la repartit au moment du don.
        ItemStack stack = new ItemStack(type, amount);
        stack.editMeta(meta -> {
            if (itemName != null && !itemName.isBlank()) {
                meta.displayName(Text.item(itemName));
            }
            if (!lore.isEmpty()) {
                meta.lore(Text.items(lore));
            }
            if (glint) {
                meta.setEnchantmentGlintOverride(true);
            }
        });
        return stack;
    }

    public void save(ConfigurationSection section) {
        section.set("name", name);
        section.set("weight", weight);
        section.set("broadcast", broadcast ? true : null);
        section.set("give-item", giveItem ? null : false);
        section.set("commands", commands.isEmpty() ? null : commands);
        if (encoded) {
            section.set("item", Base64.getEncoder().encodeToString(item.serializeAsBytes()));
        } else {
            section.set("material", material);
            section.set("amount", amount);
            section.set("item-name", itemName);
            section.set("lore", lore.isEmpty() ? null : lore);
            section.set("glint", glint ? true : null);
        }
    }

    /** Nom affiche dans les messages : "name" si defini, sinon le nom de l'objet. */
    public Component displayName() {
        if (name != null && !name.isBlank()) {
            return Text.parse(name);
        }
        Component itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                ? item.getItemMeta().displayName()
                : Component.translatable(item.getType().translationKey());
        return item.getAmount() > 1 ? Component.text(item.getAmount() + "x ").append(itemName) : itemName;
    }

    /** Objet a donner au joueur (copie), ou null si la recompense ne donne pas d'objet. */
    public ItemStack giftItem() {
        return giveItem ? item.clone() : null;
    }

    /** Icone pour les menus (quantite ramenee a une pile). */
    public ItemStack icon() {
        ItemStack icon = item.clone();
        icon.setAmount(Math.max(1, Math.min(icon.getAmount(), icon.getMaxStackSize())));
        return icon;
    }

    public String id() {
        return id;
    }

    public double weight() {
        return weight;
    }

    public void weight(double weight) {
        this.weight = Math.max(0, weight);
    }

    public boolean broadcast() {
        return broadcast;
    }

    public void broadcast(boolean broadcast) {
        this.broadcast = broadcast;
    }

    public List<String> commands() {
        return commands;
    }
}
