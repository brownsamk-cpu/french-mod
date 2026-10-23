# French Mod

A Fabric mod for **Minecraft 26.3** made by Sam Brown and his daughter.
It adds new French-themed things, one step at a time.

## Step 1: Steel

Steel looks like iron, with the same pixels, but a bit darker and a cooler gray. It's a little better than iron.

### How to make steel
1. **Steel Blend** (shapeless): 1 Iron Ingot + 1 Coal *or* Charcoal.
2. Cook the blend in a **Furnace** (10 s) or a **Blast Furnace** (5 s) to get a **Steel Ingot** (0.7 xp).

Other recipes:
* 9 Steel Ingots ↔ 1 **Block of Steel**
* 9 Steel Nuggets ↔ 1 Steel Ingot
* Steel sword, pickaxe, axe, shovel and hoe (same shapes as iron, with sticks)
* Steel helmet, chestplate, leggings and boots
* Smelt or blast any steel tool or armor piece back into a Steel Nugget

### Stats
| | Iron | **Steel** | Diamond |
|---|---|---|---|
| Tool durability | 250 | **500** | 1561 |
| Mining speed | 6 | **7** | 8 |
| Mines | up to diamond ore | **up to diamond ore** | obsidian too |
| Sword damage / speed | 6 / 1.6 | **6 / 1.6** | 7 / 1.6 |
| Axe damage / speed | 9 / 0.9 | **9 / 0.9** | 9 / 1.0 |
| Pickaxe / Shovel / Hoe damage | 4 / 4.5 / 1 | **4 / 4.5 / 1** | 5 / 5.5 / 1 |
| Full armor points | 15 | **17** (2/7/6/2) | 20 |
| Armor toughness | 0 | **1** | 2 |
| Armor durability (helmet/chest/legs/boots) | 165/240/225/195 | **264/384/360/312** | 363/528/495/429 |
| Enchantability | 14 (tools), 9 (armor) | **12** | 10 |

Steel tools hit exactly as hard and as fast as iron. They last twice as long and mine a bit faster.
Steel tools and armor are repaired with Steel Ingots.

### Using steel as iron
* **28 iron recipes also take steel**, with the same shape and the same result. You can't mix iron and steel in one recipe. They are: bucket, shears, flint and steel, compass, iron bars, chain, lantern, soul lantern, iron door, iron trapdoor, heavy pressure plate, tripwire hook, rail, activator rail, detector rail, minecart, cauldron, hopper, anvil, piston, crafter, crossbow, shield, saddle, stonecutter, smithing table, blast furnace and lodestone.
* Steel Ingots **repair iron armor** in an anvil.
* Steel Ingots can **pay a beacon**, and Blocks of Steel can be part of a **beacon base**.
* Common `c:` tags: `c:ingots/steel`, `c:nuggets/steel` and `c:storage_blocks/steel`, so other mods can use the steel.
* On purpose, steel does **not** count as iron for iron tools or in `c:ingots/iron`. Otherwise iron-tool recipes would turn out the steel tools by mistake.

French Mod works together with Cherry Mod. They use different mod ids and only add to tags.

## Building
Use JDK 25.
```
./gradlew build                                 # builds the jar and runs the game tests
./gradlew runClientGameTest -PclientShots       # takes real in-game screenshots (needs a display)
```
The jar ends up in `build/libs/frenchmod-0.1.0.jar`.

Textures, models, recipes, tags and translations (English + French) come from `tools/steel.py`.
It recolors the vanilla iron textures, so you need to run Gradle once first:
```
python3 tools/steel.py
```

## License
CC0
