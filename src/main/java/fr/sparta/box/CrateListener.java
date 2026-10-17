package fr.sparta.box;

import fr.sparta.box.gui.PreviewGui;
import java.util.List;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/** Clics sur les box et protection des blocs-box. */
public final class CrateListener implements Listener {

    private final BoxPlugin plugin;

    public CrateListener(BoxPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        Crate crate = plugin.crates().at(block);
        if (crate == null) {
            return;
        }
        // Empeche le comportement normal du bloc (coffre, porte, bouton...).
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        var player = event.getPlayer();
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            boolean instant = player.isSneaking() && plugin.getConfig().getBoolean("sneak-skips-animation", true);
            plugin.opener().open(player, crate, block.getLocation().toCenterLocation(), instant);
        } else if (event.getAction() == Action.LEFT_CLICK_BLOCK
                && plugin.getConfig().getBoolean("preview-on-left-click", true)) {
            if (player.hasPermission("box.open")) {
                new PreviewGui(plugin, crate, player).open(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (plugin.crates().at(event.getBlock()) != null) {
            event.setCancelled(true);
            plugin.send(event.getPlayer(), "block-protected");
        }
    }

    /** Les cles ne se posent pas (crochets, tetes...). */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (plugin.keys().isKey(event.getItemInHand())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> plugin.crates().at(block) != null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> plugin.crates().at(block) != null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (containsCrate(event.getBlocks())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (containsCrate(event.getBlocks())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (plugin.crates().at(event.getBlock()) != null) {
            event.setCancelled(true);
        }
    }

    private boolean containsCrate(List<Block> blocks) {
        for (Block block : blocks) {
            if (plugin.crates().at(block) != null) {
                return true;
            }
        }
        return false;
    }
}
