package fr.sparta.box;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

/** Position d'un bloc, independante du chargement du monde. */
public record BlockPos(String worldName, int x, int y, int z) {

    public static BlockPos of(Block block) {
        return new BlockPos(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    /** Format "monde;x;y;z", ou null si invalide. */
    public static BlockPos parse(String raw) {
        if (raw == null) {
            return null;
        }
        String[] parts = raw.split(";");
        if (parts.length != 4) {
            return null;
        }
        try {
            return new BlockPos(parts[0], Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim()), Integer.parseInt(parts[3].trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String serialize() {
        return worldName + ";" + x + ";" + y + ";" + z;
    }

    public World world() {
        return Bukkit.getWorld(worldName);
    }

    /** Vrai si le monde est charge et le chunk du bloc aussi (sans le charger). */
    public boolean isLoaded() {
        World world = world();
        return world != null && world.isChunkLoaded(x >> 4, z >> 4);
    }

    /** Centre du bloc, ou null si le monde n'est pas charge. */
    public Location center() {
        World world = world();
        return world == null ? null : new Location(world, x + 0.5, y + 0.5, z + 0.5);
    }

    @Override
    public String toString() {
        return worldName + " " + x + " " + y + " " + z;
    }
}
