package dev.royalarms;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public final class StructureBuilder {

    private final RoyalArms plugin;
    private final AltarManager altars;

    public StructureBuilder(RoyalArms plugin, AltarManager altars) {
        this.plugin = plugin;
        this.altars = altars;
    }

    private static void set(World w, int x, int y, int z, Material m) {
        w.getBlockAt(x, y, z).setType(m, false);
    }

    /** Finds the top solid ground block at x,z (skips trees and plants). Returns null over water. */
    public Block findSurface(World w, int x, int z) {
        Block b = w.getHighestBlockAt(x, z);
        while (b.getY() > w.getMinHeight() + 1
                && (Tag.LEAVES.isTagged(b.getType())
                || Tag.LOGS.isTagged(b.getType())
                || (!b.getType().isSolid() && !b.isLiquid()))) {
            b = b.getRelative(BlockFace.DOWN);
        }
        if (b.isLiquid()) return null;
        return b;
    }

    /** Builds a 5x5 altar. "ground" is the block the altar floor replaces. */
    public void buildAltar(Block ground) {
        World w = ground.getWorld();
        int cx = ground.getX();
        int g = ground.getY();
        int cz = ground.getZ();

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                set(w, cx + dx, g, cz + dz, Material.POLISHED_BLACKSTONE_BRICKS);
                for (int dy = 1; dy <= 4; dy++) {
                    set(w, cx + dx, g + dy, cz + dz, Material.AIR);
                }
            }
        }

        // glowing cross around the core
        set(w, cx + 1, g, cz, Material.CRYING_OBSIDIAN);
        set(w, cx - 1, g, cz, Material.CRYING_OBSIDIAN);
        set(w, cx, g, cz + 1, Material.CRYING_OBSIDIAN);
        set(w, cx, g, cz - 1, Material.CRYING_OBSIDIAN);

        // slabs on the diagonals
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                set(w, cx + sx, g + 1, cz + sz, Material.POLISHED_BLACKSTONE_BRICK_SLAB);
            }
        }

        // corner pillars with soul lanterns
        for (int sx : new int[]{-2, 2}) {
            for (int sz : new int[]{-2, 2}) {
                set(w, cx + sx, g + 1, cz + sz, Material.CHISELED_POLISHED_BLACKSTONE);
                set(w, cx + sx, g + 2, cz + sz, Material.CHISELED_POLISHED_BLACKSTONE);
                set(w, cx + sx, g + 3, cz + sz, Material.POLISHED_BLACKSTONE);
                set(w, cx + sx, g + 4, cz + sz, Material.SOUL_LANTERN);
            }
        }

        // the core you right-click
        set(w, cx, g + 1, cz, Material.LODESTONE);
        altars.register(w.getBlockAt(cx, g + 1, cz));
    }

    /** Builds a 9x9 ruined shrine with an altar in the middle and a loot chest. */
    public void buildShrine(Block ground, Random rnd) {
        World w = ground.getWorld();
        int cx = ground.getX();
        int g = ground.getY();
        int cz = ground.getZ();

        Material[] floors = {Material.COBBLESTONE, Material.MOSSY_COBBLESTONE,
                Material.STONE_BRICKS, Material.MOSSY_STONE_BRICKS, Material.CRACKED_STONE_BRICKS};
        Material[] walls = {Material.STONE_BRICKS, Material.MOSSY_STONE_BRICKS,
                Material.CRACKED_STONE_BRICKS, Material.COBBLESTONE};

        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                for (int y = g - 4; y < g; y++) {
                    Block b = w.getBlockAt(cx + dx, y, cz + dz);
                    if (!b.getType().isSolid()) b.setType(Material.COBBLESTONE, false);
                }
                set(w, cx + dx, g, cz + dz, floors[rnd.nextInt(floors.length)]);
                for (int dy = 1; dy <= 5; dy++) {
                    set(w, cx + dx, g + dy, cz + dz, Material.AIR);
                }
            }
        }

        // crumbling perimeter walls with an entrance on the north side
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != 4) continue;
                if (dz == -4 && Math.abs(dx) <= 1) continue;
                for (int dy = 1; dy <= 3; dy++) {
                    if (rnd.nextDouble() < 1.0 - dy * 0.2) {
                        set(w, cx + dx, g + dy, cz + dz, walls[rnd.nextInt(walls.length)]);
                    }
                }
            }
        }

        buildAltar(w.getBlockAt(cx, g, cz));

        // loot chest in the south-east corner
        Block chestBlock = w.getBlockAt(cx + 3, g + 1, cz + 3);
        chestBlock.setType(Material.CHEST, false);
        if (chestBlock.getState() instanceof Chest chest) {
            Inventory inv = chest.getBlockInventory();
            inv.setItem(rnd.nextInt(27), new ItemStack(Material.IRON_INGOT, 3 + rnd.nextInt(6)));
            inv.setItem(rnd.nextInt(27), new ItemStack(Material.GOLD_INGOT, 2 + rnd.nextInt(5)));
            inv.setItem(rnd.nextInt(27), new ItemStack(Material.ARROW, 8 + rnd.nextInt(17)));
            if (rnd.nextDouble() < 0.6) {
                inv.setItem(rnd.nextInt(27), new ItemStack(Material.DIAMOND, 2 + rnd.nextInt(4)));
            }
            if (rnd.nextDouble() < 0.3) {
                inv.setItem(rnd.nextInt(27), new ItemStack(Material.GOLDEN_APPLE, 1));
            }
            // a random artifact, sometimes
            if (rnd.nextDouble() < plugin.getConfig().getDouble("structures.artifact-chance", 0.35)) {
                Artifact[] all = Artifact.values();
                inv.addItem(plugin.artifacts().create(all[rnd.nextInt(all.length)]));
            }
        }
    }
}
