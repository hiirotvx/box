package fr.sparta.box.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Menu du plugin. Tous les clics y sont annules ; les sous-classes reagissent. */
public abstract class Gui implements InventoryHolder {

    protected Inventory inventory;

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    /** Clic dans le menu. */
    public void click(InventoryClickEvent event) {
    }

    /** Clic dans l'inventaire du joueur pendant que le menu est ouvert. */
    public void bottomClick(InventoryClickEvent event) {
    }

    public void close(InventoryCloseEvent event) {
    }
}
