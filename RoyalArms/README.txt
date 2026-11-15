RoyalArms 2.0 - for Paper 26.2 (Java 25)

BUILD
1. Install JDK 25 and Gradle (or open the folder in IntelliJ IDEA).
2. In this folder run:  gradle build
3. Jar: build/libs/RoyalArms-2.0.0.jar  ->  put it in your server's plugins folder.
   If the server complains about api-version, change '26.2' in plugin.yml.

HOW IT WORKS
- Land melee hits to fill power bars (max 3, shown in the action bar).
  A mace smash (falling hit) fills a full bar.
- Right-click = ability 1, sneak + right-click = ability 2.
  /ability1 and /ability2 do the same without sneaking (good for Bedrock).
- Crafted weapons start locked. Unique player kills unlock them:
  1 kill = passive, 2 = ability 1, 3 = ability 2. (unlock.enabled in config.yml)

WEAPONS
 Shockwave (mace)             A1 Surge (1): launch up + stun nearby players
                              A2 Shockwave (3): next smash chains lightning
                              Passive Blitz: repeated smashes boost Surge
 Lifestealer (sword)          A1 Lifesteal (1): heal from melee for 10s
                              A2 Bloodthirsty (3): hits steal hearts as absorption
                              Passive Venom: chance on hit for a bad effect
 Warden Battleaxe (axe)       A1 Sensor (1): nearby players glow 10s
                              A2 Ground Pound (2): armor-piercing sonic boom beam
                              Passive: Wardens ignore you
 Primal Spear (spear)         A1 Throw (1): damage + Poison II
                              A2 Vines (2): root a target and pull them to you
                              Passive Agility: Speed II while carried + forward lunge on hits
 Afterburner (sword)          A1 Spitfire (1): homing magma orb
                              A2 Blazing Shuriken (2): spinning blade, returns on a miss
                              Passive Fireborn: Fire Resistance while carried
 Void Hammer (mace)           A1 Hammer Toss (1): throw and teleport to it
                              A2 Peekaboo (3): pick a player, teleport above them
                              Passive Afloat: chance on hit for Levitation

RECIPES (shaped, original - edit in RoyalArms.java if you want different ones)
 Shockwave:        2 Netherite Ingot, Heavy Core, 2 Breeze Rod
 Lifestealer:      Wither Skeleton Skull, 2 Netherite Ingot, Netherite Sword, Enchanted Golden Apple
 Warden Battleaxe: 2 Echo Shard, Sculk Catalyst, Netherite Axe, Netherite Ingot
 Primal Spear:     2 Pufferfish, Weeping Vines, Netherite Spear, Netherite Ingot
 Afterburner:      2 Blaze Rod, Ghast Tear, Netherite Sword, Netherite Ingot
 Void Hammer:      2 Ender Eye, Dragon's Breath, Mace, Nether Star
Open /arsenal to see them in-game.

COMMANDS (OP)
/arms give <weapon> [player] [locked]   (unlocked by default)
/arms power <0-3>      set power on the held weapon (testing)
/arms timer <seconds> <title>   /arms timer stop
/arms altar    /arms shrine    /arms gui
Everyone: /arsenal (/weapons), /ability1, /ability2

ARTIFACTS (right-click, or /ability1 - works on Bedrock too)
 Cloak of Shadows  invisible for 15s, attacking breaks it (90s)   4 Phantom Membrane, Echo Shard, Nether Star
 Jumbo Rocket      blast high into the sky + slow falling (60s)   4 Firework Rocket, 4 TNT, Gunpowder or Creeper Head (center)
 Phoenix Feather   passive: cancels one killing blow, then burns up
                                                                  Blaze Rod, 2 Feather, Totem of Undying, Netherite Ingot
 Gale Rod          dash forward + knock back nearby players (20s) 2 Wind Charge, 2 Breeze Rod, Netherite Ingot
 Chrono Clock      rewind ~5 seconds of position/health (120s)    3 Echo Shard, Clock, Nether Star
 Tracker Compass   nearest player distance + direction (30s)      3 Ender Eye, Recovery Compass, Diamond
 Bucket of Malk    full hunger + saturation + cleanse (60s)       Milk Bucket, 4 Golden Apple, 4 Sugar
 Bucket of Greed   pull nearby items to you + Fire Resistance (60s)  Lava Bucket, 4 Gold Block, 4 Emerald Block
 /arms give <artifact id> [player] also works (cloak_of_shadows, jumbo_rocket,
 phoenix_feather, gale_rod, chrono_clock, tracker_compass).

BEDROCK (Geyser): everything uses chest menus, action bar text, vanilla items and
commands, so it works for Bedrock players. If tapping to use an item is unreliable
on a device, /ability1 and /ability2 do the same thing.

ALTARS & SHRINES: right-click an altar's lodestone with the offering
(default 4 netherite ingots) for a random locked weapon. Ruined Shrines
spawn in newly generated chunks and their loot chest has a 35% chance of
holding a random artifact (structures.artifact-chance in config.yml). See config.yml.

ENCHANTS: RoyalArms never touches enchants. Weapons are real vanilla items
with hidden data, so enchanting, anvils, mending and renaming keep working.
