package fr.sparta.box;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import org.bukkit.block.Block;

/** Charge les box du dossier crates/ et retrouve la box posee sur un bloc. */
public final class CrateManager {

    private final BoxPlugin plugin;
    private final File folder;
    private final Map<String, Crate> crates = new TreeMap<>();
    private final Map<BlockPos, Crate> byLocation = new HashMap<>();

    public CrateManager(BoxPlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "crates");
    }

    public void load() {
        crates.clear();
        if (!folder.exists()) {
            plugin.saveResource("crates/commune.yml", false);
        }
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String id = file.getName().substring(0, file.getName().length() - 4).toLowerCase(Locale.ROOT);
                try {
                    crates.put(id, Crate.load(id, file, plugin.getLogger()));
                } catch (RuntimeException e) {
                    plugin.getLogger().log(Level.SEVERE, "Impossible de charger la box " + file.getName(), e);
                }
            }
        }
        rebuildIndex();
        plugin.getLogger().info(crates.size() + " box chargee(s).");
    }

    private void rebuildIndex() {
        byLocation.clear();
        for (Crate crate : crates.values()) {
            for (BlockPos pos : crate.locations()) {
                Crate previous = byLocation.putIfAbsent(pos, crate);
                if (previous != null) {
                    plugin.getLogger().warning("Le bloc " + pos + " est a la fois la box " + previous.id()
                            + " et " + crate.id() + " : " + previous.id() + " est conservee.");
                }
            }
        }
    }

    public void save(Crate crate) {
        try {
            crate.save();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Impossible de sauvegarder la box " + crate.id(), e);
        }
    }

    public Crate create(String id) {
        Crate crate = Crate.create(id, new File(folder, id + ".yml"));
        folder.mkdirs();
        crates.put(id, crate);
        save(crate);
        return crate;
    }

    public void delete(Crate crate) {
        crates.remove(crate.id());
        crate.locations().forEach(byLocation::remove);
        if (crate.file().exists() && !crate.file().delete()) {
            plugin.getLogger().warning("Impossible de supprimer " + crate.file().getName());
        }
    }

    public void addLocation(Crate crate, BlockPos pos) {
        crate.locations().add(pos);
        byLocation.put(pos, crate);
        save(crate);
    }

    public void removeLocation(Crate crate, BlockPos pos) {
        crate.locations().remove(pos);
        byLocation.remove(pos);
        save(crate);
    }

    public Crate get(String id) {
        return id == null ? null : crates.get(id.toLowerCase(Locale.ROOT));
    }

    public Crate at(Block block) {
        return byLocation.get(BlockPos.of(block));
    }

    public Crate at(BlockPos pos) {
        return byLocation.get(pos);
    }

    public Collection<Crate> all() {
        return crates.values();
    }

    public Collection<String> ids() {
        return crates.keySet();
    }
}
