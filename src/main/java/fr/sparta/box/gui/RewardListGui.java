package fr.sparta.box.gui;

import fr.sparta.box.BoxPlugin;
import fr.sparta.box.Crate;
import fr.sparta.box.Reward;
import fr.sparta.box.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/** Menu de 6 lignes listant les recompenses d'une box, avec pagination sur la derniere ligne. */
public abstract class RewardListGui extends Gui {

    protected static final int PER_PAGE = 45;
    protected static final int PREVIOUS = 45;
    protected static final int CENTER = 49;
    protected static final int NEXT = 53;

    protected final BoxPlugin plugin;
    protected final Crate crate;
    protected final Player viewer;
    protected final List<Reward> shown = new ArrayList<>();
    protected int page;

    protected RewardListGui(BoxPlugin plugin, Crate crate, Player viewer, Component title) {
        this.plugin = plugin;
        this.crate = crate;
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(this, 54, title);
    }

    protected abstract ItemStack decorate(Reward reward);

    protected abstract ItemStack centerItem();

    protected void rewardClicked(Reward reward, InventoryClickEvent event) {
    }

    public void render() {
        inventory.clear();
        shown.clear();
        List<Reward> rewards = new ArrayList<>(crate.rewards());
        int pages = Math.max(1, (rewards.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.max(0, Math.min(page, pages - 1));
        for (int i = 0; i < PER_PAGE; i++) {
            int index = page * PER_PAGE + i;
            if (index >= rewards.size()) {
                break;
            }
            Reward reward = rewards.get(index);
            shown.add(reward);
            inventory.setItem(i, decorate(reward));
        }
        ItemStack filler = Text.filler(Material.BLACK_STAINED_GLASS_PANE);
        for (int slot = PER_PAGE; slot < 54; slot++) {
            inventory.setItem(slot, filler);
        }
        if (page > 0) {
            inventory.setItem(PREVIOUS, Text.icon(Material.ARROW, Text.item(plugin.text("gui.previous")), null));
        }
        if (page < pages - 1) {
            inventory.setItem(NEXT, Text.icon(Material.ARROW, Text.item(plugin.text("gui.next")), null));
        }
        ItemStack center = centerItem();
        if (center != null) {
            inventory.setItem(CENTER, center);
        }
    }

    @Override
    public void open(Player player) {
        render();
        super.open(player);
    }

    @Override
    public void click(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == PREVIOUS && page > 0) {
            page--;
            render();
        } else if (slot == NEXT) {
            page++;
            render();
        } else if (slot < PER_PAGE && slot < shown.size()) {
            rewardClicked(shown.get(slot), event);
        }
    }

    /** Copie de l'icone de la recompense, avec des lignes ajoutees a sa lore. */
    protected ItemStack withLore(Reward reward, List<Component> extra) {
        ItemStack icon = reward.icon();
        icon.editMeta(meta -> {
            List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            if (!lore.isEmpty()) {
                lore.add(Component.empty());
            }
            lore.addAll(extra);
            meta.lore(lore);
            if (!meta.hasDisplayName()) {
                meta.displayName(Text.item("").append(reward.displayName()));
            }
        });
        return icon;
    }

    protected String chance(Reward reward) {
        return String.format(Locale.FRANCE, "%.2f", crate.chance(reward));
    }
}
