package fr.sparta.box;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/** Une box : son fichier crates/&lt;id&gt;.yml, ses emplacements et ses recompenses. */
public final class Crate {

    private final String id;
    private final File file;

    private String name;
    private boolean hologram;
    private double hologramHeight;
    private List<String> hologramLines;
    private boolean animation;
    private long cooldownSeconds;
    private String permission;
    private boolean pushBack;
    private String particleName;
    private Particle particle;

    private String keyMaterial;
    private String keyName;
    private List<String> keyLore;

    private final List<BlockPos> locations = new ArrayList<>();
    private final Map<String, Reward> rewards = new LinkedHashMap<>();

    private Crate(String id, File file) {
        this.id = id;
        this.file = file;
    }

    /** Box vierge, telle que creee par /box create. */
    public static Crate create(String id, File file) {
        Crate crate = new Crate(id, file);
        String title = Character.toUpperCase(id.charAt(0)) + id.substring(1);
        crate.name = "<yellow>Box " + title;
        crate.hologram = true;
        crate.hologramHeight = 1.25;
        crate.hologramLines = new ArrayList<>(List.of(
                "<yellow><bold>Box " + title,
                "<gray>Clic droit : <white>ouvrir",
                "<gray>Clic gauche : <white>apercu"));
        crate.animation = true;
        crate.cooldownSeconds = 0;
        crate.permission = "";
        crate.pushBack = true;
        crate.particleName = "";
        crate.keyMaterial = "TRIPWIRE_HOOK";
        crate.keyName = "<yellow>Cle <white>" + title;
        crate.keyLore = new ArrayList<>(List.of("<gray>Clic droit sur une <yellow>Box " + title, "<gray>pour l'ouvrir."));
        return crate;
    }

    public static Crate load(String id, File file, Logger logger) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        Crate crate = new Crate(id, file);
        crate.name = yaml.getString("name", id);
        crate.hologram = yaml.getBoolean("hologram.enabled", true);
        crate.hologramHeight = yaml.getDouble("hologram.height", 1.25);
        crate.hologramLines = new ArrayList<>(yaml.getStringList("hologram.lines"));
        crate.animation = yaml.getBoolean("animation", true);
        crate.cooldownSeconds = Math.max(0, yaml.getLong("cooldown", 0));
        crate.permission = yaml.getString("permission", "");
        crate.pushBack = yaml.getBoolean("push-back", true);
        crate.particleName = yaml.getString("particle", "");
        crate.keyMaterial = yaml.getString("key.material", "TRIPWIRE_HOOK");
        crate.keyName = yaml.getString("key.name", "<yellow>Cle " + id);
        crate.keyLore = new ArrayList<>(yaml.getStringList("key.lore"));

        for (String raw : yaml.getStringList("locations")) {
            BlockPos pos = BlockPos.parse(raw);
            if (pos == null) {
                logger.warning("Box '" + id + "' : emplacement invalide ignore : " + raw);
            } else if (!crate.locations.contains(pos)) {
                crate.locations.add(pos);
            }
        }
        ConfigurationSection rewardSection = yaml.getConfigurationSection("rewards");
        if (rewardSection != null) {
            for (String rewardId : rewardSection.getKeys(false)) {
                ConfigurationSection section = rewardSection.getConfigurationSection(rewardId);
                if (section != null) {
                    crate.rewards.put(rewardId, Reward.load(rewardId, section, logger));
                }
            }
        }
        crate.resolveParticle(logger);
        return crate;
    }

    private void resolveParticle(Logger logger) {
        particle = null;
        if (particleName == null || particleName.isBlank()) {
            return;
        }
        NamespacedKey key = NamespacedKey.fromString(particleName.trim().toLowerCase(Locale.ROOT));
        Particle found = key == null ? null
                : RegistryAccess.registryAccess().getRegistry(RegistryKey.PARTICLE_TYPE).get(key);
        if (found == null || found.getDataType() != Void.class) {
            logger.warning("Box '" + id + "' : particule '" + particleName
                    + "' inconnue ou necessitant des donnees, ignoree.");
            return;
        }
        particle = found;
    }

    public void save() throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("name", name);
        yaml.set("hologram.enabled", hologram);
        yaml.set("hologram.height", hologramHeight);
        yaml.set("hologram.lines", hologramLines);
        yaml.set("animation", animation);
        yaml.set("cooldown", cooldownSeconds);
        yaml.set("permission", permission);
        yaml.set("push-back", pushBack);
        yaml.set("particle", particleName);
        yaml.set("key.material", keyMaterial);
        yaml.set("key.name", keyName);
        yaml.set("key.lore", keyLore);
        yaml.set("locations", locations.stream().map(BlockPos::serialize).toList());
        ConfigurationSection rewardSection = yaml.createSection("rewards");
        for (Reward reward : rewards.values()) {
            reward.save(rewardSection.createSection(reward.id()));
        }
        yaml.save(file);
    }

    /** Tire une recompense au hasard selon les poids, ou null s'il n'y en a aucune. */
    public Reward roll() {
        double total = totalWeight();
        if (total <= 0) {
            return null;
        }
        double pick = ThreadLocalRandom.current().nextDouble(total);
        Reward last = null;
        for (Reward reward : rewards.values()) {
            if (reward.weight() <= 0) {
                continue;
            }
            last = reward;
            pick -= reward.weight();
            if (pick < 0) {
                return reward;
            }
        }
        return last;
    }

    public double totalWeight() {
        double total = 0;
        for (Reward reward : rewards.values()) {
            total += Math.max(0, reward.weight());
        }
        return total;
    }

    /** Chance en pourcentage. */
    public double chance(Reward reward) {
        double total = totalWeight();
        return total <= 0 ? 0 : Math.max(0, reward.weight()) * 100.0 / total;
    }

    public ItemStack keyItem(NamespacedKey tag, int amount) {
        Material type = Material.matchMaterial(keyMaterial);
        if (type == null || !type.isItem() || type.isAir()) {
            type = Material.TRIPWIRE_HOOK;
        }
        ItemStack stack = new ItemStack(type, Math.max(1, amount));
        stack.editMeta(meta -> {
            meta.displayName(Text.item(keyName));
            if (!keyLore.isEmpty()) {
                meta.lore(Text.items(keyLore));
            }
            meta.setEnchantmentGlintOverride(true);
            meta.getPersistentDataContainer().set(tag, PersistentDataType.STRING, id);
        });
        return stack;
    }

    public String nextRewardId() {
        int i = rewards.size() + 1;
        while (rewards.containsKey("r" + i)) {
            i++;
        }
        return "r" + i;
    }

    public String id() {
        return id;
    }

    public File file() {
        return file;
    }

    public String rawName() {
        return name;
    }

    public Component name() {
        return Text.parse(name);
    }

    public boolean hologram() {
        return hologram && !hologramLines.isEmpty();
    }

    public double hologramHeight() {
        return hologramHeight;
    }

    public List<String> hologramLines() {
        return hologramLines;
    }

    public boolean animation() {
        return animation;
    }

    public long cooldownSeconds() {
        return cooldownSeconds;
    }

    public String permission() {
        return permission;
    }

    public boolean pushBack() {
        return pushBack;
    }

    public Particle particle() {
        return particle;
    }

    public List<BlockPos> locations() {
        return locations;
    }

    public Collection<Reward> rewards() {
        return rewards.values();
    }

    public void addReward(Reward reward) {
        rewards.put(reward.id(), reward);
    }

    public void removeReward(Reward reward) {
        rewards.remove(reward.id());
    }
}
