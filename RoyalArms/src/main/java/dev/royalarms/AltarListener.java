package dev.royalarms;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public final class AltarListener implements Listener {

    private final RoyalArms plugin;

    public AltarListener(RoyalArms plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onAltarUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block core = e.getClickedBlock();
        if (core == null || core.getType() != Material.LODESTONE) return;
        if (!plugin.altars().isAltar(core)) return;

        e.setCancelled(true);
        Player p = e.getPlayer();
        FileConfiguration cfg = plugin.getConfig();

        long leftMs = plugin.altars().cooldownLeftMs(core);
        if (leftMs > 0) {
            long s = (leftMs + 999) / 1000;
            p.sendActionBar(Component.text(
                    String.format("Altar recharging: %02d:%02d", s / 60, s % 60), NamedTextColor.RED));
            return;
        }

        Material offer = Material.matchMaterial(cfg.getString("altar.offering-material", "NETHERITE_INGOT"));
        if (offer == null) offer = Material.NETHERITE_INGOT;
        int amount = Math.max(1, cfg.getInt("altar.offering-amount", 4));

        if (!p.getInventory().containsAtLeast(new ItemStack(offer), amount)) {
            p.sendActionBar(Component.text("Offer " + amount + " "
                    + offer.name().toLowerCase().replace('_', ' '), NamedTextColor.YELLOW));
            return;
        }

        p.getInventory().removeItem(new ItemStack(offer, amount));

        Weapon[] all = Weapon.values();
        Weapon w = all[ThreadLocalRandom.current().nextInt(all.length)];
        ItemStack reward = plugin.createItem(w);
        p.getInventory().addItem(reward).values()
                .forEach(left -> p.getWorld().dropItemNaturally(p.getLocation(), left));

        plugin.altars().startCooldown(core, cfg.getInt("altar.cooldown-seconds", 600));

        World world = core.getWorld();
        var spot = core.getLocation().add(0.5, 1.2, 0.5);
        world.strikeLightningEffect(spot);
        world.spawnParticle(Particle.END_ROD, spot, 60, 0.6, 0.8, 0.6, 0.05);
        world.playSound(spot, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1f);
        Bukkit.getServer().sendMessage(Component.text(
                p.getName() + " claimed the " + w.displayName + " from an altar!", w.color));
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (e.getBlock().getType() == Material.LODESTONE) {
            plugin.altars().unregister(e.getBlock());
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        if (!e.isNewChunk()) return;
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.getBoolean("structures.enabled", true)) return;

        World world = e.getWorld();
        if (world.getEnvironment() != World.Environment.NORMAL) return;
        List<String> worlds = cfg.getStringList("structures.worlds");
        if (!worlds.isEmpty() && !worlds.contains(world.getName())) return;
        if (ThreadLocalRandom.current().nextDouble() > cfg.getDouble("structures.chance", 0.004)) return;

        Chunk chunk = e.getChunk();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        int x = chunkX * 16 + 8;
        int z = chunkZ * 16 + 8;

        // place one tick later; never modify blocks inside the load event itself
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!world.isChunkLoaded(chunkX, chunkZ)) return;
            Block ground = plugin.structures().findSurface(world, x, z);
            if (ground == null) return;
            plugin.structures().buildShrine(ground, new Random());
            plugin.getLogger().info("Spawned Ruined Shrine at " + x + ", " + ground.getY() + ", " + z);
        });
    }
}
