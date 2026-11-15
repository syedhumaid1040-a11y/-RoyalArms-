package dev.royalarms;

import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class AltarManager {

    private final RoyalArms plugin;
    private final File file;
    private final Set<String> altars = new HashSet<>();
    private final Map<String, Long> cooldowns = new HashMap<>();

    public AltarManager(RoyalArms plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "altars.yml");
        load();
    }

    private static String key(Block b) {
        return b.getWorld().getName() + "," + b.getX() + "," + b.getY() + "," + b.getZ();
    }

    private void load() {
        if (!file.exists()) return;
        altars.addAll(YamlConfiguration.loadConfiguration(file).getStringList("altars"));
    }

    private void save() {
        try {
            plugin.getDataFolder().mkdirs();
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set("altars", new ArrayList<>(altars));
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not save altars.yml: " + ex.getMessage());
        }
    }

    public void register(Block core) {
        if (altars.add(key(core))) save();
    }

    public void unregister(Block core) {
        if (altars.remove(key(core))) {
            cooldowns.remove(key(core));
            save();
        }
    }

    public boolean isAltar(Block core) {
        return altars.contains(key(core));
    }

    public long cooldownLeftMs(Block core) {
        Long until = cooldowns.get(key(core));
        return until == null ? 0 : Math.max(0, until - System.currentTimeMillis());
    }

    public void startCooldown(Block core, int seconds) {
        cooldowns.put(key(core), System.currentTimeMillis() + seconds * 1000L);
    }
}
