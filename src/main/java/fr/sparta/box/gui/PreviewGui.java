package fr.sparta.box.gui;

import fr.sparta.box.BoxPlugin;
import fr.sparta.box.Crate;
import fr.sparta.box.Reward;
import fr.sparta.box.Text;
import java.util.List;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Apercu des recompenses d'une box et de leurs chances. */
public final class PreviewGui extends RewardListGui {

    public PreviewGui(BoxPlugin plugin, Crate crate, Player viewer) {
        super(plugin, crate, viewer, Text.parse(plugin.text("gui.preview-title"),
                Placeholder.component("box", crate.name())));
    }

    @Override
    protected ItemStack decorate(Reward reward) {
        return withLore(reward, List.of(Text.item(plugin.text("gui.chance-line"),
                Placeholder.unparsed("chance", chance(reward)))));
    }

    @Override
    protected ItemStack centerItem() {
        return Text.icon(plugin.keys().physical(crate, 1).getType(),
                Text.item(plugin.text("gui.keys-info")),
                Text.items(plugin.textList("gui.keys-info-lore"),
                        Placeholder.unparsed("virtual", String.valueOf(plugin.keys().virtual(viewer.getUniqueId(), crate))),
                        Placeholder.unparsed("physical", String.valueOf(plugin.keys().physicalCount(viewer, crate)))));
    }
}
