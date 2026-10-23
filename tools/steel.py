#!/usr/bin/env python3
"""French Mod step 1: Steel. Writes every steel texture, model, recipe, tag and name.

Her rule: "the same pixels of iron, just a little bit darker". So every steel picture is the vanilla iron picture,
pixel for pixel, with the metal colours run through STEEL() below (darker and a touch cooler/bluer). Wooden handles stay brown.
The Steel Blend is the steel ingot shape with coal specks (SPECKS), and the mod icon is drawn from the ICON rows.

Needs the Minecraft 26.3 client jar (Loom downloads it): set MC_CLIENT_JAR, or it is found in ~/.gradle/caches/fabric-loom/26.3/.
Run from the repo root: python3 tools/steel.py
"""
import glob
import io
import json
import os
import zipfile

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, 'src', 'main', 'resources')
ASSETS = os.path.join(RES, 'assets', 'frenchmod')
DATA = os.path.join(RES, 'data')
MOD = 'frenchmod'

# ---------------------------------------------------------------- colours

DARKER = 0.80          # "just a little bit darker"
COOL = (0.96, 0.99, 1.06)  # a hint of blue-gray, so steel reads as steel and not as dull iron


def is_metal(r, g, b):
    """Gray (or the iron nugget's faint blue-gray) pixels are metal; brown handle pixels are not."""
    return max(r, g, b) - min(r, g, b) <= 12 or (b >= r and max(r, g, b) - min(r, g, b) <= 45)


def STEEL(c):
    r, g, b, a = c
    if a == 0 or not is_metal(r, g, b):
        return c
    out = [min(255, max(0, round(v * DARKER * k))) for v, k in zip((r, g, b), COOL)]
    return (out[0], out[1], out[2], a)


# Steel Blend = steel ingot with coal mixed in: coal-black specks on these (x, y) pixels of the ingot.
SPECKS = [(4, 6), (7, 5), (10, 6), (5, 8), (9, 8), (12, 8), (7, 10), (3, 9), (11, 10)]
COAL = (38, 38, 42, 255)
COAL_SHINE = (78, 78, 86, 255)
SHINY_SPECKS = [(7, 5), (9, 8)]

# Mod icon: a cute Eiffel tower at sunset-blue, 16x16 drawn here, scaled up to 128x128.
ICON_PALETTE = {
    '.': None,                 # sky (gradient filled in below)
    'T': (92, 70, 58),         # tower (Eiffel bronze-brown)
    't': (140, 108, 84),       # tower highlight
    'G': (98, 172, 74),        # grass
    'g': (76, 140, 58),        # darker grass
    'C': (255, 255, 255),      # cloud
    'H': (255, 120, 160),      # little heart
    'F': (0, 85, 164),         # flag: blue
    'W': (255, 255, 255),      # flag: white
    'R': (239, 65, 53),        # flag: red
}
ICON = [
    '................',
    '.H.H...t........',
    'HHHHH..T....CC..',
    '.HHH...T...CCCC.',
    '..H...tTt.......',
    '......tTt.......',
    '......TTT.......',
    '.....TTTTT......',
    '......T.T.......',
    '.....tT.Tt..FWR.',
    '...TTTTTTTTT.WR.',
    '....tT...Tt..WR.',
    '....T.....T..WR.',
    '...tT.ttt.Tt.|..',
    '..TT.t...t.TT|..',
    'gGGGGGGGGGGGGGGg',
]

# ---------------------------------------------------------------- the steel set (id, English, French, vanilla iron picture)

ITEMS = [
    # id, English, French, iron source texture (or None = made here), model parent
    ('steel_blend', 'Steel Blend', "Mélange d'acier", None, 'generated'),
    ('steel_ingot', 'Steel Ingot', "Lingot d'acier", 'item/iron_ingot', 'generated'),
    ('steel_nugget', 'Steel Nugget', "Pépite d'acier", 'item/iron_nugget', 'generated'),
    ('steel_sword', 'Steel Sword', 'Épée en acier', 'item/iron_sword', 'handheld'),
    ('steel_shovel', 'Steel Shovel', 'Pelle en acier', 'item/iron_shovel', 'handheld'),
    ('steel_pickaxe', 'Steel Pickaxe', 'Pioche en acier', 'item/iron_pickaxe', 'handheld'),
    ('steel_axe', 'Steel Axe', 'Hache en acier', 'item/iron_axe', 'handheld'),
    ('steel_hoe', 'Steel Hoe', 'Houe en acier', 'item/iron_hoe', 'handheld'),
    ('steel_helmet', 'Steel Helmet', 'Casque en acier', 'item/iron_helmet', 'generated'),
    ('steel_chestplate', 'Steel Chestplate', 'Plastron en acier', 'item/iron_chestplate', 'generated'),
    ('steel_leggings', 'Steel Leggings', 'Jambières en acier', 'item/iron_leggings', 'generated'),
    ('steel_boots', 'Steel Boots', 'Bottes en acier', 'item/iron_boots', 'generated'),
]
BLOCK = ('steel_block', 'Block of Steel', "Bloc d'acier", 'block/iron_block')
ARMOR_LAYERS = {'humanoid': 'entity/equipment/humanoid/iron', 'humanoid_leggings': 'entity/equipment/humanoid_leggings/iron'}
TAB = ('French Mod', 'French Mod')

# Vanilla iron recipes that also get a steel version ("you could also use it as iron"):
# the same pattern with every iron ingot -> steel ingot, iron nugget -> steel nugget, block of iron -> block of steel.
# Iron tools/armor are left out (steel has its own, better set); so are the iron block/ingot/nugget/raw iron recipes.
USE_AS_IRON = ['activator_rail', 'anvil', 'blast_furnace', 'bucket', 'cauldron', 'compass', 'crafter', 'crossbow', 'detector_rail',
               'flint_and_steel', 'heavy_weighted_pressure_plate', 'hopper', 'iron_bars', 'iron_chain', 'iron_door', 'iron_trapdoor',
               'lantern', 'lodestone', 'minecart', 'piston', 'rail', 'saddle', 'shears', 'shield', 'smithing_table', 'soul_lantern',
               'stonecutter', 'tripwire_hook']
SWAP = {'minecraft:iron_ingot': f'{MOD}:steel_ingot', 'minecraft:iron_nugget': f'{MOD}:steel_nugget', 'minecraft:iron_block': f'{MOD}:steel_block'}

TOOL_PATTERNS = {
    'steel_sword': ['X', 'X', '#'], 'steel_shovel': ['X', '#', '#'], 'steel_pickaxe': ['XXX', ' # ', ' # '],
    'steel_axe': ['XX', 'X#', ' #'], 'steel_hoe': ['XX', ' #', ' #'],
}
ARMOR_PATTERNS = {
    'steel_helmet': ['XXX', 'X X'], 'steel_chestplate': ['X X', 'XXX', 'XXX'], 'steel_leggings': ['XXX', 'X X', 'X X'],
    'steel_boots': ['X X', 'X X'],
}


# ---------------------------------------------------------------- helpers

def client_jar():
    path = os.environ.get('MC_CLIENT_JAR')
    if path:
        return path
    found = glob.glob(os.path.expanduser('~/.gradle/caches/fabric-loom/26.3/minecraft-client.jar'))
    if not found:
        raise SystemExit('Minecraft 26.3 client jar not found; run ./gradlew build once or set MC_CLIENT_JAR')
    return found[0]


JAR = zipfile.ZipFile(client_jar())


def vanilla_png(path):
    return Image.open(io.BytesIO(JAR.read(f'assets/minecraft/textures/{path}.png'))).convert('RGBA')


def vanilla_recipe(name):
    return json.loads(JAR.read(f'data/minecraft/recipe/{name}.json'))


def steelify(img):
    out = img.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            px[x, y] = STEEL(px[x, y])
    return out


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write('\n')


def save_png(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def blend_texture():
    img = steelify(vanilla_png('item/iron_ingot'))
    for x, y in SPECKS:
        if img.getpixel((x, y))[3] > 0:
            img.putpixel((x, y), COAL_SHINE if (x, y) in SHINY_SPECKS else COAL)
    return img


def icon():
    img = Image.new('RGBA', (16, 16))
    for y, row in enumerate(ICON):
        sky = (int(120 + y * 6), int(180 + y * 3), 255)
        for x, ch in enumerate(row):
            if ch == '|':
                c = (60, 60, 60)  # flag pole
            else:
                c = ICON_PALETTE[ch] or sky
            img.putpixel((x, y), c + (255,))
    return img.resize((128, 128), Image.NEAREST)


def swap(value):
    if isinstance(value, str):
        return SWAP.get(value, value)
    if isinstance(value, list):
        return [swap(v) for v in value]
    if isinstance(value, dict):
        return {k: swap(v) for k, v in value.items()}
    return value


def item_def(name):
    return {'model': {'type': 'minecraft:model', 'model': f'{MOD}:item/{name}'}}


# ---------------------------------------------------------------- main

def main():
    tex = os.path.join(ASSETS, 'textures')
    en, fr = {}, {}

    # Items
    for name, english, french, source, parent in ITEMS:
        img = blend_texture() if source is None else steelify(vanilla_png(source))
        save_png(img, os.path.join(tex, 'item', name + '.png'))
        write_json(os.path.join(ASSETS, 'items', name + '.json'), item_def(name))
        write_json(os.path.join(ASSETS, 'models', 'item', name + '.json'),
                   {'parent': f'minecraft:item/{parent}', 'textures': {'layer0': f'{MOD}:item/{name}'}})
        en[f'item.{MOD}.{name}'] = english
        fr[f'item.{MOD}.{name}'] = french

    # Block of Steel
    name, english, french, source = BLOCK
    save_png(steelify(vanilla_png(source)), os.path.join(tex, 'block', name + '.png'))
    write_json(os.path.join(ASSETS, 'models', 'block', name + '.json'),
               {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'{MOD}:block/{name}'}})
    write_json(os.path.join(ASSETS, 'blockstates', name + '.json'), {'variants': {'': {'model': f'{MOD}:block/{name}'}}})
    write_json(os.path.join(ASSETS, 'items', name + '.json'),
               {'model': {'type': 'minecraft:model', 'model': f'{MOD}:block/{name}'}})
    write_json(os.path.join(DATA, MOD, 'loot_table', 'blocks', name + '.json'), {
        'type': 'minecraft:block',
        'pools': [{'condition': {'type': 'minecraft:survives_explosion'},
                   'entries': [{'type': 'minecraft:item', 'name': f'{MOD}:{name}'}], 'rolls': 1}],
        'random_sequence': f'{MOD}:blocks/{name}'})
    en[f'block.{MOD}.{name}'] = english
    fr[f'block.{MOD}.{name}'] = french

    # Worn armor (equipment asset + layer textures)
    for layer, source in ARMOR_LAYERS.items():
        save_png(steelify(vanilla_png(source)), os.path.join(tex, 'entity', 'equipment', layer, 'steel.png'))
    write_json(os.path.join(ASSETS, 'equipment', 'steel.json'),
               {'layers': {layer: [{'texture': f'{MOD}:steel'}] for layer in ARMOR_LAYERS}})

    save_png(icon(), os.path.join(ASSETS, 'icon.png'))

    en[f'itemGroup.{MOD}.french_mod'], fr[f'itemGroup.{MOD}.french_mod'] = TAB
    write_json(os.path.join(ASSETS, 'lang', 'en_us.json'), en)
    write_json(os.path.join(ASSETS, 'lang', 'fr_fr.json'), fr)

    # Recipes
    rec = os.path.join(DATA, MOD, 'recipe')
    S = f'{MOD}:steel_ingot'
    write_json(os.path.join(rec, 'steel_blend.json'), {
        'type': 'minecraft:crafting_shapeless', 'category': 'misc',
        'ingredients': ['minecraft:iron_ingot', '#minecraft:coals'], 'result': {'id': f'{MOD}:steel_blend', 'count': 1}})
    write_json(os.path.join(rec, 'steel_ingot_from_smelting.json'), {
        'type': 'minecraft:smelting', 'category': 'misc', 'cookingtime': 200, 'experience': 0.7, 'group': 'steel_ingot',
        'ingredient': f'{MOD}:steel_blend', 'result': {'id': S}})
    write_json(os.path.join(rec, 'steel_ingot_from_blasting.json'), {
        'type': 'minecraft:blasting', 'category': 'misc', 'cookingtime': 100, 'experience': 0.7, 'group': 'steel_ingot',
        'ingredient': f'{MOD}:steel_blend', 'result': {'id': S}})
    write_json(os.path.join(rec, 'steel_block.json'), {
        'type': 'minecraft:crafting_shaped', 'category': 'building', 'key': {'#': S}, 'pattern': ['###', '###', '###'],
        'result': {'id': f'{MOD}:steel_block'}})
    write_json(os.path.join(rec, 'steel_ingot_from_steel_block.json'), {
        'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'group': 'steel_ingot', 'ingredients': [f'{MOD}:steel_block'],
        'result': {'id': S, 'count': 9}})
    write_json(os.path.join(rec, 'steel_nugget.json'), {
        'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': [S], 'result': {'id': f'{MOD}:steel_nugget', 'count': 9}})
    write_json(os.path.join(rec, 'steel_ingot_from_nuggets.json'), {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'group': 'steel_ingot', 'key': {'#': f'{MOD}:steel_nugget'},
        'pattern': ['###', '###', '###'], 'result': {'id': S}})
    for name, pattern in TOOL_PATTERNS.items():
        write_json(os.path.join(rec, name + '.json'), {
            'type': 'minecraft:crafting_shaped', 'category': 'equipment',
            'key': {'#': 'minecraft:stick', 'X': f'#{MOD}:steel_tool_materials'}, 'pattern': pattern, 'result': {'id': f'{MOD}:{name}'}})
    for name, pattern in ARMOR_PATTERNS.items():
        write_json(os.path.join(rec, name + '.json'), {
            'type': 'minecraft:crafting_shaped', 'category': 'equipment', 'key': {'X': S}, 'pattern': pattern, 'result': {'id': f'{MOD}:{name}'}})
    gear = [f'{MOD}:{n}' for n in list(TOOL_PATTERNS) + list(ARMOR_PATTERNS)]
    for kind, time in (('smelting', 200), ('blasting', 100)):
        write_json(os.path.join(rec, f'steel_nugget_from_{kind}.json'), {
            'type': f'minecraft:{kind}', 'category': 'misc', 'cookingtime': time, 'experience': 0.1, 'ingredient': gear,
            'result': {'id': f'{MOD}:steel_nugget'}})
    for name in USE_AS_IRON:
        recipe = vanilla_recipe(name)
        steel = {k: (v if k == 'result' else swap(v)) for k, v in recipe.items()}
        steel.pop('group', None)
        assert steel != recipe, name
        write_json(os.path.join(rec, f'{name}_from_steel.json'), steel)

    # Tags
    tags = os.path.join(DATA)
    def tag(namespace, kind, name, values):
        write_json(os.path.join(tags, namespace, 'tags', kind, name + '.json'), {'replace': False, 'values': values})
    tag(MOD, 'item', 'steel_tool_materials', [S])
    tag(MOD, 'item', 'repairs_steel_armor', [S])
    # Steel can also patch up iron armor on an anvil, and it pays for a beacon like iron does; a Block of Steel works in a beacon base.
    tag('minecraft', 'item', 'repairs_iron_armor', [S])
    tag('minecraft', 'item', 'beacon_payment_items', [S])
    tag('minecraft', 'block', 'beacon_base_blocks', [f'{MOD}:steel_block'])
    tag('minecraft', 'block', 'mineable/pickaxe', [f'{MOD}:steel_block'])
    tag('minecraft', 'block', 'needs_stone_tool', [f'{MOD}:steel_block'])
    for vanilla, item in (('swords', 'steel_sword'), ('shovels', 'steel_shovel'), ('pickaxes', 'steel_pickaxe'), ('axes', 'steel_axe'),
                          ('hoes', 'steel_hoe'), ('head_armor', 'steel_helmet'), ('chest_armor', 'steel_chestplate'),
                          ('leg_armor', 'steel_leggings'), ('foot_armor', 'steel_boots')):
        tag('minecraft', 'item', vanilla, [f'{MOD}:{item}'])
    # Common (c:) convention tags so other mods can find steel.
    tag('c', 'item', 'ingots/steel', [S])
    tag('c', 'item', 'ingots', [S])
    tag('c', 'item', 'nuggets/steel', [f'{MOD}:steel_nugget'])
    tag('c', 'item', 'nuggets', [f'{MOD}:steel_nugget'])
    tag('c', 'item', 'storage_blocks/steel', [f'{MOD}:steel_block'])
    tag('c', 'item', 'storage_blocks', [f'{MOD}:steel_block'])
    tag('c', 'block', 'storage_blocks/steel', [f'{MOD}:steel_block'])
    tag('c', 'block', 'storage_blocks', [f'{MOD}:steel_block'])
    print('wrote', len(ITEMS) + 1, 'steel things and', len(os.listdir(rec)), 'recipes')


if __name__ == '__main__':
    main()
