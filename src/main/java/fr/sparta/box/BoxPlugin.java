package fr.sparta.box;

import fr.sparta.box.gui.Gui;
import fr.sparta.box.gui.GuiListener;
import java.util.List;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class BoxPlugin extends JavaPlugin {

    private CrateManager crates;
    private KeyManager keys;
    private HologramManager holograms;
    private CrateOpener opener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        crates = new CrateManager(this);
        keys = new KeyManager(this);
        holograms = new HologramManager(this);
        opener = new CrateOpener(this);

        crates.load();
        keys.load();
        holograms.start();

        getServer().getPluginManager().registerEvents(new CrateListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(), this);

        PluginCommand command = getCommand("box");
        if (command != null) {
            BoxCommand executor = new BoxCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
    }

    @Override
    public void onDisable() {
        closeMenus();
        if (holograms != null) {
            holograms.stop();
        }
        if (keys != null) {
            keys.save();
        }
    }

    /** Recharge config.yml et les box. */
    public void reload() {
        closeMenus();
        reloadConfig();
        holograms.stop();
        crates.load();
        keys.load();
        holograms.start();
    }

    /** Ferme les menus ouverts : une roulette en cours donne alors sa recompense. */
    private void closeMenus() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder(false) instanceof Gui) {
                player.closeInventory();
            }
        }
    }

    public CrateManager crates() {
        return crates;
    }

    public KeyManager keys() {
        return keys;
    }

    public HologramManager holograms() {
        return holograms;
    }

    public CrateOpener opener() {
        return opener;
    }

    /** Texte brut de config.yml (chemin complet, ex : "gui.next"). */
    public String text(String path) {
        return getConfig().getString(path, "");
    }

    public List<String> textList(String path) {
        return getConfig().getStringList(path);
    }

    /** Message de chat "messages.&lt;key&gt;", precede du prefixe. */
    public void send(CommandSender to, String key, TagResolver... resolvers) {
        to.sendMessage(Text.parse(text("prefix") + text("messages." + key), resolvers));
    }
}
