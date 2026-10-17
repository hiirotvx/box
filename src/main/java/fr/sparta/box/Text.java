package fr.sparta.box;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/** Petits utilitaires MiniMessage et objets. */
public final class Text {

    public static final MiniMessage MM = MiniMessage.miniMessage();

    private Text() {
    }

    public static Component parse(String text, TagResolver... resolvers) {
        return MM.deserialize(text == null ? "" : text, resolvers);
    }

    /** Texte pour un nom ou une lore d'objet : sans l'italique par defaut de Minecraft. */
    public static Component item(String text, TagResolver... resolvers) {
        return MM.deserialize("<!italic>" + (text == null ? "" : text), resolvers);
    }

    public static List<Component> items(List<String> lines, TagResolver... resolvers) {
        List<Component> out = new ArrayList<>(lines.size());
        for (String line : lines) {
            out.add(item(line, resolvers));
        }
        return out;
    }

    public static ItemStack icon(Material material, Component name, List<Component> lore) {
        ItemStack stack = new ItemStack(material);
        stack.editMeta(meta -> {
            meta.displayName(name);
            if (lore != null && !lore.isEmpty()) {
                meta.lore(lore);
            }
        });
        return stack;
    }

    public static ItemStack filler(Material material) {
        return icon(material, Component.empty(), null);
    }
}
