package fr.sparta.box.gui;

import fr.sparta.box.BoxPlugin;
import fr.sparta.box.Crate;
import fr.sparta.box.Reward;
import fr.sparta.box.Text;
import java.util.Locale;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/** Editeur des recompenses : poids, diffusion, suppression, ajout depuis l'inventaire. */
public final class EditorGui extends RewardListGui {

    public EditorGui(BoxPlugin plugin, Crate crate, Player viewer) {
        super(plugin, crate, viewer, Text.parse(plugin.text("gui.editor-title"),
                Placeholder.component("box", crate.name())));
    }

    @Override
    protected ItemStack decorate(Reward reward) {
        return withLore(reward, Text.items(plugin.textList("gui.editor-lore"),
                Placeholder.unparsed("weight", formatWeight(reward.weight())),
                Placeholder.unparsed("chance", chance(reward)),
                Placeholder.unparsed("broadcast", reward.broadcast() ? "oui" : "non")));
    }

    @Override
    protected ItemStack centerItem() {
        return Text.icon(Material.BOOK, Text.item(plugin.text("gui.editor-help")),
                Text.items(plugin.textList("gui.editor-help-lore")));
    }

    @Override
    protected void rewardClicked(Reward reward, InventoryClickEvent event) {
        switch (event.getClick()) {
            case LEFT -> reward.weight(reward.weight() + 1);
            case SHIFT_LEFT -> reward.weight(reward.weight() + 10);
            case RIGHT -> reward.weight(reward.weight() - 1);
            case SHIFT_RIGHT -> reward.weight(reward.weight() - 10);
            case MIDDLE -> reward.broadcast(!reward.broadcast());
            case DROP, CONTROL_DROP -> crate.removeReward(reward);
            default -> {
                return;
            }
        }
        changed();
    }

    @Override
    public void bottomClick(InventoryClickEvent event) {
        if (!event.isShiftClick()) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.isEmpty()) {
            return;
        }
        crate.addReward(Reward.fromItem(crate.nextRewardId(), clicked, 10));
        changed();
    }

    private void changed() {
        plugin.crates().save(crate);
        render();
    }

    private static String formatWeight(double weight) {
        return weight == Math.floor(weight) ? String.valueOf((long) weight) : String.format(Locale.FRANCE, "%.2f", weight);
    }
}
