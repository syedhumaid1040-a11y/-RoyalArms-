package dev.royalarms;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RoyalArms extends JavaPlugin {

    private WeaponItems items;
    private CombatState state;
    private Abilities abilities;
    private TimerManager timers;
    private AltarManager altars;
    private StructureBuilder structures;
    private final List<NamespacedKey> recipeKeys = new ArrayList<>();
    private final Map<String, ShapedRecipe> shapedRecipes = new HashMap<>();
    private final Map<String, Long> cooldowns = new HashMap<>();
    private ArtifactManager artifacts;
    private int tickCount;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        items = new WeaponItems(this);
        state = new CombatState();
        abilities = new Abilities(this);
        artifacts = new ArtifactManager(this);
        timers = new TimerManager(this);
        altars = new AltarManager(this);
        structures = new StructureBuilder(this, altars);
        registerRecipes();

        getServer().getPluginManager().registerEvents(new WeaponListener(this), this);
        getServer().getPluginManager().registerEvents(new AltarListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(new ArtifactListener(this), this);

        ArmsCommand cmd = new ArmsCommand(this);
        getCommand("arms").setExecutor(cmd);
        getCommand("arms").setTabCompleter(cmd);
        getCommand("arsenal").setExecutor(new ArsenalCommand(this));
        getCommand("recipes").setExecutor(new ArsenalCommand(this));
        getCommand("ability1").setExecutor(new AbilityCommand(this, 1));
        getCommand("ability2").setExecutor(new AbilityCommand(this, 2));

        Bukkit.getScheduler().runTaskTimer(this, this::tick, 5L, 5L);
    }

    @Override
    public void onDisable() {
        timers.stop();
        for (NamespacedKey k : recipeKeys) {
            Bukkit.removeRecipe(k);
        }
    }

    public WeaponItems items() {
        return items;
    }

    public CombatState state() {
        return state;
    }

    public Abilities abilities() {
        return abilities;
    }

    public ArtifactManager artifacts() {
        return artifacts;
    }

    public ShapedRecipe recipeFor(String id) {
        return shapedRecipes.get(id);
    }

    public TimerManager timers() {
        return timers;
    }

    public AltarManager altars() {
        return altars;
    }

    public StructureBuilder structures() {
        return structures;
    }

    /** A fresh weapon as crafted/rewarded: locked unless kill unlocks are off. */
    public ItemStack createItem(Weapon w) {
        return items.create(w, !items.unlockEnabled());
    }

    // ---------- cooldowns ----------

    private String ckey(Player p, Weapon w, int slot) {
        return p.getUniqueId() + ":" + w.id + ":" + slot;
    }

    public long cooldownLeftMs(Player p, Weapon w, int slot) {
        Long until = cooldowns.get(ckey(p, w, slot));
        return until == null ? 0 : Math.max(0, until - System.currentTimeMillis());
    }

    public void setCooldown(Player p, Weapon w, int slot, int seconds) {
        cooldowns.put(ckey(p, w, slot), System.currentTimeMillis() + seconds * 1000L);
    }

    public int cooldownSeconds(Weapon w, int slot) {
        int fallback = slot == 1 ? 15 : 30;
        return getConfig().getInt("weapons." + w.id + ".cooldown" + slot, fallback);
    }

    public long cooldownLeftMs(Player p, String key) {
        Long until = cooldowns.get(p.getUniqueId() + ":" + key);
        return until == null ? 0 : Math.max(0, until - System.currentTimeMillis());
    }

    public void setCooldown(Player p, String key, int seconds) {
        cooldowns.put(p.getUniqueId() + ":" + key, System.currentTimeMillis() + seconds * 1000L);
    }

    public int artifactCooldownSeconds(Artifact a) {
        return getConfig().getInt("artifacts." + a.id + ".cooldown", a.cooldownSeconds);
    }

    // ---------- periodic task: action bar + passives ----------

    private void tick() {
        tickCount++;
        for (Player p : Bukkit.getOnlinePlayers()) {
            ItemStack hand = p.getInventory().getItemInMainHand();
            Weapon held = items.weaponOf(hand);

            // Fireborn: Fire Resistance while carrying the Afterburner
            boolean fireborn = false;
            boolean agility = false;
            boolean hasChrono = false;
            for (ItemStack it : p.getInventory().getContents()) {
                if (it == null) continue;
                if (!fireborn && items.weaponOf(it) == Weapon.AFTERBURNER && items.effectiveStage(it) >= 1) {
                    fireborn = true;
                    p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 60, 0, true, false, false));
                }
                // Agility: Speed II while the Primal Spear is carried
                if (!agility && items.weaponOf(it) == Weapon.PRIMAL_SPEAR && items.effectiveStage(it) >= 1) {
                    agility = true;
                    int amp = getConfig().getInt("weapons.primal_spear.speed-amplifier", 1);
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, amp, true, false, false));
                }
                if (artifacts.artifactOf(it) == Artifact.CHRONO_CLOCK) hasChrono = true;
            }
            // Chrono Clock records your position about once a second while carried
            if (hasChrono && tickCount % 4 == 0) artifacts.record(p);

            if (held != null) {
                p.sendActionBar(statusLine(p, hand, held));
            } else {
                Artifact art = artifacts.artifactOf(hand);
                if (art != null) p.sendActionBar(artifactLine(p, art));
            }
        }
    }

    private Component statusLine(Player p, ItemStack hand, Weapon w) {
        int power = items.power(hand);
        int stage = items.effectiveStage(hand);
        StringBuilder sb = new StringBuilder(w.displayName).append("  ");
        for (int i = 0; i < 3; i++) sb.append(i < power ? "■" : "□");
        sb.append("  ");
        if (stage < 3) sb.append("Kills ").append(stage).append("/3  ");
        if (stage >= 2) sb.append(cdText("A1", cooldownLeftMs(p, w, 1)));
        if (stage >= 3) sb.append("  ").append(cdText("A2", cooldownLeftMs(p, w, 2)));
        return Component.text(sb.toString(), w.color);
    }

    private Component artifactLine(Player p, Artifact a) {
        if (a == Artifact.PHOENIX_FEATHER) return Component.text(a.displayName + "  passive", a.color);
        long left = cooldownLeftMs(p, "artifact:" + a.id);
        String text = left > 0 ? String.format("%s  %.1fs", a.displayName, left / 1000.0)
                : a.displayName + "  ready";
        return Component.text(text, a.color);
    }

    private static String cdText(String name, long leftMs) {
        return leftMs > 0 ? String.format("%s %.1fs", name, leftMs / 1000.0) : name + " ready";
    }

    // ---------- recipes ----------

    private void registerRecipes() {
        if (!getConfig().getBoolean("recipes.enabled", true)) return;

        addRecipe(Weapon.SHOCKWAVE, new String[]{"NHN", " B ", " B "}, Map.of(
                'N', Material.NETHERITE_INGOT,
                'H', Material.HEAVY_CORE,
                'B', Material.BREEZE_ROD));

        addRecipe(Weapon.LIFESTEALER, new String[]{" W ", "NSN", " H "}, Map.of(
                'W', Material.WITHER_SKELETON_SKULL,
                'N', Material.NETHERITE_INGOT,
                'S', Material.NETHERITE_SWORD,
                'H', Material.ENCHANTED_GOLDEN_APPLE));

        addRecipe(Weapon.WARDEN_BATTLEAXE, new String[]{"ECE", " A ", " N "}, Map.of(
                'E', Material.ECHO_SHARD,
                'C', Material.SCULK_CATALYST,
                'A', Material.NETHERITE_AXE,
                'N', Material.NETHERITE_INGOT));

        addRecipe(Weapon.PRIMAL_SPEAR, new String[]{"PVP", " S ", " N "}, Map.of(
                'P', Material.PUFFERFISH,
                'V', Material.WEEPING_VINES,
                'S', Weapon.PRIMAL_SPEAR.material,
                'N', Material.NETHERITE_INGOT));

        addRecipe(Weapon.AFTERBURNER, new String[]{"BGB", " S ", " N "}, Map.of(
                'B', Material.BLAZE_ROD,
                'G', Material.GHAST_TEAR,
                'S', Material.NETHERITE_SWORD,
                'N', Material.NETHERITE_INGOT));

        addRecipe(Weapon.VOID_HAMMER, new String[]{"EDE", " M ", " N "}, Map.of(
                'E', Material.ENDER_EYE,
                'D', Material.DRAGON_BREATH,
                'M', Material.MACE,
                'N', Material.NETHER_STAR));

        // ---- artifacts ----
        addRecipe(Artifact.CLOAK_OF_SHADOWS.id, artifacts.create(Artifact.CLOAK_OF_SHADOWS),
                new String[]{" P ", "PEP", " N "}, Map.of(
                'P', Material.PHANTOM_MEMBRANE,
                'E', Material.ECHO_SHARD,
                'N', Material.NETHER_STAR));

        addChoiceRecipe(Artifact.JUMBO_ROCKET.id, artifacts.create(Artifact.JUMBO_ROCKET),
                new String[]{"RTR", "TGT", "RTR"}, Map.of(
                'R', new RecipeChoice.MaterialChoice(Material.FIREWORK_ROCKET),
                'T', new RecipeChoice.MaterialChoice(Material.TNT),
                'G', new RecipeChoice.MaterialChoice(Material.GUNPOWDER, Material.CREEPER_HEAD)));

        addRecipe(Artifact.PHOENIX_FEATHER.id, artifacts.create(Artifact.PHOENIX_FEATHER),
                new String[]{" B ", "FTF", " N "}, Map.of(
                'B', Material.BLAZE_ROD,
                'F', Material.FEATHER,
                'T', Material.TOTEM_OF_UNDYING,
                'N', Material.NETHERITE_INGOT));

        addRecipe(Artifact.GALE_ROD.id, artifacts.create(Artifact.GALE_ROD),
                new String[]{"WBW", " B ", " N "}, Map.of(
                'W', Material.WIND_CHARGE,
                'B', Material.BREEZE_ROD,
                'N', Material.NETHERITE_INGOT));

        addRecipe(Artifact.CHRONO_CLOCK.id, artifacts.create(Artifact.CHRONO_CLOCK),
                new String[]{" E ", "ECE", " N "}, Map.of(
                'E', Material.ECHO_SHARD,
                'C', Material.CLOCK,
                'N', Material.NETHER_STAR));

        addRecipe(Artifact.TRACKER_COMPASS.id, artifacts.create(Artifact.TRACKER_COMPASS),
                new String[]{" E ", "ECE", " D "}, Map.of(
                'E', Material.ENDER_EYE,
                'C', Material.RECOVERY_COMPASS,
                'D', Material.DIAMOND));

        addRecipe(Artifact.BUCKET_OF_MALK.id, artifacts.create(Artifact.BUCKET_OF_MALK),
                new String[]{"SGS", "GMG", "SGS"}, Map.of(
                'S', Material.SUGAR,
                'G', Material.GOLDEN_APPLE,
                'M', Material.MILK_BUCKET));

        addRecipe(Artifact.BUCKET_OF_GREED.id, artifacts.create(Artifact.BUCKET_OF_GREED),
                new String[]{"GEG", "ELE", "GEG"}, Map.of(
                'G', Material.GOLD_BLOCK,
                'E', Material.EMERALD_BLOCK,
                'L', Material.LAVA_BUCKET));
    }

    private void addChoiceRecipe(String id, ItemStack result, String[] shape,
                                 Map<Character, RecipeChoice> ingredients) {
        NamespacedKey key = new NamespacedKey(this, id);
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(shape);
        for (Map.Entry<Character, RecipeChoice> en : ingredients.entrySet()) {
            recipe.setIngredient(en.getKey(), en.getValue());
        }
        Bukkit.addRecipe(recipe);
        recipeKeys.add(key);
        shapedRecipes.put(id, recipe);
    }

    private void addRecipe(Weapon w, String[] shape, Map<Character, Material> ingredients) {
        addRecipe(w.id, createItem(w), shape, ingredients);
    }

    private void addRecipe(String id, ItemStack result, String[] shape, Map<Character, Material> ingredients) {
        NamespacedKey key = new NamespacedKey(this, id);
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(shape);
        for (Map.Entry<Character, Material> en : ingredients.entrySet()) {
            recipe.setIngredient(en.getKey(), en.getValue());
        }
        Bukkit.addRecipe(recipe);
        recipeKeys.add(key);
        shapedRecipes.put(id, recipe);
    }
}
