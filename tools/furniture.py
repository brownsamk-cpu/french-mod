#!/usr/bin/env python3
"""French Mod step 3: cozy French-cottage furniture. Writes every furniture texture, model, blockstate, loot table, recipe, tag and name.

Her data trick: every picture is a 16x16 grid of letters (the rows below), and each letter is a colour in PALETTE.
Style: cream / white painted wood, soft blue fabric, little gold accents, and a lamp with a warm glow.
Every model is drawn facing north (its front toward the player who placed it); the blockstates turn it with y rotations.

Run from the repo root, after tools/steel.py (which rewrites the language files): python3 tools/furniture.py
"""
import json
import os

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, 'src', 'main', 'resources')
ASSETS = os.path.join(RES, 'assets', 'frenchmod')
DATA = os.path.join(RES, 'data')
MOD = 'frenchmod'

# ---------------------------------------------------------------- colours (the data trick)

PALETTE = {
    # cream painted wood
    'c': (240, 231, 210), 'l': (251, 246, 232), 'd': (224, 211, 184), 's': (201, 186, 156),
    # soft French blue fabric
    'b': (152, 188, 224), 'B': (126, 164, 206), 'w': (184, 212, 238), 'n': (104, 140, 186),
    # gold
    'y': (226, 186, 78), 'Y': (252, 222, 122), 'o': (184, 144, 52),
    # white linen
    'W': (250, 250, 247), 'g': (232, 232, 226), 'G': (214, 214, 208),
    # sugar pink (for the Golden Trim Table)
    'p': (248, 200, 214), 'P': (255, 222, 232), 'q': (232, 176, 196), 'V': (255, 252, 252),
    # lamp shade, glowing (warm) and off
    'h': (255, 232, 160), 'H': (255, 246, 206), 'a': (246, 204, 116),
    'f': (226, 216, 192), 'F': (238, 230, 210), 'e': (206, 194, 168),
}

TEXTURES = {
    # Painted planks: four boards with darker seams and a little grain.
    'cream_wood': [
        'lllllllllcllllll', 'ccccdccccccccdcc', 'cccccccdcccccccc', 'ssssssssssssssss',
        'lllcllllllllllll', 'cccccccccdcccccc', 'ccdcccccccccccdc', 'ssssssssssssssss',
        'llllllllllclllll', 'ccccccdccccccccc', 'cccccccccccdcccc', 'ssssssssssssssss',
        'llllllcllllllllc', 'cdcccccccccccccc', 'ccccccccdccccccc', 'ssssssssssssssss',
    ],
    # Quilted blue cushion fabric with little gold buttons.
    'blue_fabric': [
        'wbbbbbbBbbbbbbbw', 'bwbbbbBnBbbbbbwb', 'bbwbbBbbbBbbbwbb', 'bbbwBbbbbbBbwbbb',
        'bbbByBbbbbbByBbb', 'bbBbwbBbbbBbwbBb', 'bBbbbwbBbBbwbbbB', 'Bnbbbbbyybbbbbnb',
        'bBbbbbBbBwbbbbBb', 'bbBbbBbbbbwbbBbb', 'bbbByBbbbbbByBbb', 'bbbwBbbbbbBbwbbb',
        'bbwbbBbbbBbbbwbb', 'bwbbbbBnBbbbbbwb', 'wbbbbbbBbbbbbbbw', 'bbbbbbbbbbbbbbbb',
    ],
    # Shiny gold trim.
    'gold_trim': [
        'YYYYYYYYYYYYYYYY', 'YyyyyyyyyyyyyyyY', 'yyYyyyyyyyyYyyyy', 'yyyyyyyyyyyyyyyy',
        'yyyyyoyyyyyyyoyy', 'oyyyyyyyyyyyyyyo', 'yyyYyyyyyYyyyyyy', 'yyyyyyyyyyyyyyyy',
        'yyyyyyyyyyyyyyyy', 'yyyyyoyyyyyyyoyy', 'yYyyyyyyyyyyyyYy', 'yyyyyyyyyyyyyyyy',
        'yyyyyyyyyyyyyyyy', 'oyyyyyyYyyyyyyyo', 'yyyyyyyyyyyyyyyy', 'oooooooooooooooo',
    ],
    # Crisp white linen sheets and pillow.
    'white_linen': [
        'WWWWWWWWWWWWWWWW', 'WWWWWWWWgWWWWWWW', 'WWWgWWWWWWWWWWWW', 'WWWWWWWWWWWWgWWW',
        'gggggggggggggggg', 'WWWWWWWWWWWWWWWW', 'WWWWWgWWWWWWWWWW', 'WWWWWWWWWWWWWWgW',
        'WWWWWWWWWWWWWWWW', 'gggggggggggggggg', 'WWWWWWWWWWWWWWWW', 'WWgWWWWWWWWgWWWW',
        'WWWWWWWWWWWWWWWW', 'WWWWWWWgWWWWWWWW', 'WWWWWWWWWWWWWWWW', 'GGGGGGGGGGGGGGGG',
    ],
    # Golden Trim Table top: white painted wood with a sugar pink wash (plain, no flowers).
    'sugar_pink_wood': [
        'VVVVVVVVVPVVVVVV', 'PPPPpPPPPPPPPpPP', 'PPPPPPPpPPPPPPPP', 'qqqqqqqqqqqqqqqq',
        'VVVPVVVVVVVVVVVV', 'PPPPPPPPPpPPPPPP', 'PPpPPPPPPPPPPPpP', 'qqqqqqqqqqqqqqqq',
        'VVVVVVVVVVPVVVVV', 'PPPPPPpPPPPPPPPP', 'PPPPPPPPPPPpPPPP', 'qqqqqqqqqqqqqqqq',
        'VVVVVVPVVVVVVVVP', 'PpPPPPPPPPPPPPPP', 'PPPPPPPPpPPPPPPP', 'qqqqqqqqqqqqqqqq',
    ],
    # Lamp shade, switched on: warm glow, brightest in the middle.
    'lamp_shade_on': [
        'aaaaaaaaaaaaaaaa', 'ahhhhhhhhhhhhhha', 'hhhhhhhhhhhhhhhh', 'hhhhhHHHHHHhhhhh',
        'hhhhHHHHHHHHhhhh', 'hhhHHHHHHHHHHhhh', 'hhhHHHHHHHHHHhhh', 'hhHHHHHHHHHHHHhh',
        'hhHHHHHHHHHHHHhh', 'hhhHHHHHHHHHHhhh', 'hhhHHHHHHHHHHhhh', 'hhhhHHHHHHHHhhhh',
        'hhhhhHHHHHHhhhhh', 'hhhhhhhhhhhhhhhh', 'ahhhhhhhhhhhhhha', 'aaaaaaaaaaaaaaaa',
    ],
    # Lamp shade, switched off: plain cream cloth.
    'lamp_shade_off': [
        'eeeeeeeeeeeeeeee', 'efffffffffffffff', 'fffffFffffffffff', 'ffffffffffFfffff',
        'ffffffffffffffff', 'ffFfffffffffffff', 'ffffffffFfffffff', 'ffffffffffffffFf',
        'ffffffffffffffff', 'ffffFfffffffffff', 'ffffffffffffFfff', 'ffffffffffffffff',
        'fffffffFffffffff', 'ffffffffffffffff', 'efffffffffffffff', 'eeeeeeeeeeeeeeee',
    ],
}

# ---------------------------------------------------------------- shapes (the boxes each piece is made of)
# Each box: (from, to, texture). Drawn facing north: the front (where you sit) is north, the back is south (z=16).
CHAIR = [
    ([2, 0, 2], [4, 8, 4], 'cream_wood'), ([12, 0, 2], [14, 8, 4], 'cream_wood'),
    ([2, 0, 12], [4, 8, 14], 'cream_wood'), ([12, 0, 12], [14, 8, 14], 'cream_wood'),
    ([2, 8, 2], [14, 10, 14], 'cream_wood'),
    ([3, 10, 3], [13, 11, 12], 'blue_fabric'),
    ([2, 10, 12], [4, 22, 14], 'cream_wood'), ([12, 10, 12], [14, 22, 14], 'cream_wood'),
    ([4, 12, 12.5], [12, 20, 13.5], 'blue_fabric'),
    ([2, 22, 12], [14, 23.5, 14], 'gold_trim'),
]
TABLE = [
    ([0, 14, 0], [16, 16, 16], 'cream_wood'),
    ([1, 13, 1], [15, 14, 15], 'gold_trim'),
    ([1.5, 0, 1.5], [3.5, 13, 3.5], 'cream_wood'), ([12.5, 0, 1.5], [14.5, 13, 3.5], 'cream_wood'),
    ([1.5, 0, 12.5], [3.5, 13, 14.5], 'cream_wood'), ([12.5, 0, 12.5], [14.5, 13, 14.5], 'cream_wood'),
    ([1, 0, 1], [4, 1, 4], 'gold_trim'), ([12, 0, 1], [15, 1, 4], 'gold_trim'),
    ([1, 0, 12], [4, 1, 15], 'gold_trim'), ([12, 0, 12], [15, 1, 15], 'gold_trim'),
]
COUCH = [
    ([1, 0, 2], [3, 3, 4], 'gold_trim'), ([13, 0, 2], [15, 3, 4], 'gold_trim'),
    ([1, 0, 12], [3, 3, 14], 'gold_trim'), ([13, 0, 12], [15, 3, 14], 'gold_trim'),
    ([0, 3, 1], [16, 7, 15], 'cream_wood'),
    ([2, 7, 1], [14, 10, 12], 'blue_fabric'),
    ([0, 7, 12], [16, 19, 15], 'blue_fabric'),
    ([0, 19, 12], [16, 20, 15], 'gold_trim'),
    ([0, 7, 1], [2, 13, 12], 'cream_wood'), ([14, 7, 1], [16, 13, 12], 'cream_wood'),
    ([0, 13, 1], [2, 14, 12], 'gold_trim'), ([14, 13, 1], [16, 14, 12], 'gold_trim'),
]
# The Golden Trim Table: the same shape as the Table, white and sugar pink, with a thick gold edge around the top.
GOLDEN_TRIM_TABLE = [
    ([0, 14, 0], [16, 16, 16], 'sugar_pink_wood'),
    ([-0.01, 13.5, -0.01], [16.01, 14.5, 16.01], 'gold_trim'),
    ([1, 13, 1], [15, 13.5, 15], 'gold_trim'),
    ([1.5, 0, 1.5], [3.5, 13, 3.5], 'white_linen'), ([12.5, 0, 1.5], [14.5, 13, 3.5], 'white_linen'),
    ([1.5, 0, 12.5], [3.5, 13, 14.5], 'white_linen'), ([12.5, 0, 12.5], [14.5, 13, 14.5], 'white_linen'),
    ([1, 0, 1], [4, 1, 4], 'gold_trim'), ([12, 0, 1], [15, 1, 4], 'gold_trim'),
    ([1, 0, 12], [4, 1, 15], 'gold_trim'), ([12, 0, 12], [15, 1, 15], 'gold_trim'),
]
LAMP = lambda shade: [
    ([5, 0, 5], [11, 1, 11], 'gold_trim'),
    ([6.5, 1, 6.5], [9.5, 2, 9.5], 'gold_trim'),
    ([7.5, 2, 7.5], [8.5, 9, 8.5], 'gold_trim'),
    ([4, 9, 4], [12, 15, 12], shade),
    ([7, 15, 7], [9, 16, 9], 'gold_trim'),
]
# Fancy Bed: the head half (pillow and tall headboard at the north edge) and the foot half (blue blanket, low footboard at the south edge).
BED_HEAD = [
    ([0, 0, 0], [2, 2, 2], 'cream_wood'), ([14, 0, 0], [16, 2, 2], 'cream_wood'),
    ([0, 2, 0], [16, 4, 16], 'cream_wood'),
    ([0, 4, 2], [16, 9, 16], 'white_linen'),
    ([0, 4, 10], [16, 9.5, 16], 'blue_fabric'),
    ([2, 9, 3], [14, 11, 8], 'white_linen'),
    ([0, 2, 0], [16, 22, 2], 'cream_wood'),
    ([3, 10, 1.5], [13, 19, 2.5], 'blue_fabric'),
    ([0, 22, 0], [16, 23.5, 2], 'gold_trim'),
]
BED_FOOT = [
    ([0, 0, 14], [2, 2, 16], 'cream_wood'), ([14, 0, 14], [16, 2, 16], 'cream_wood'),
    ([0, 2, 0], [16, 4, 16], 'cream_wood'),
    ([0, 4, 0], [16, 9.5, 14], 'blue_fabric'),
    ([0, 2, 14], [16, 12, 16], 'cream_wood'),
    ([0, 12, 14], [16, 13, 16], 'gold_trim'),
]

PIECES = {'chair': CHAIR, 'table': TABLE, 'golden_trim_table': GOLDEN_TRIM_TABLE, 'couch': COUCH, 'lamp': LAMP('lamp_shade_off'), 'lamp_on': LAMP('lamp_shade_on'),
          'fancy_bed_head': BED_HEAD, 'fancy_bed_foot': BED_FOOT}
NAMES = {'chair': ('Chair', 'Chaise'), 'table': ('Table', 'Table'), 'fancy_bed': ('Fancy Bed', 'Lit Chic'),
         'couch': ('Couch', 'Canapé'), 'lamp': ('Lamp', 'Lampe'),
         'golden_trim_table': ('The Golden Trim Table', 'La Table à Bordure Dorée')}
FACING_Y = {'north': 0, 'east': 90, 'south': 180, 'west': 270}


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write('\n')


def draw(rows):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), rows
    img = Image.new('RGBA', (16, 16))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            img.putpixel((x, y), PALETTE[ch] + (255,))
    return img


def clamp_uv(a, b):
    return [max(0, min(16, a)), max(0, min(16, b))]


def element(frm, to, tex):
    """One box with all six faces; each face's UV is its own spot on the 16x16 picture (so pixels line up)."""
    (x1, y1, z1), (x2, y2, z2) = frm, to
    t = '#' + tex
    v1, v2 = clamp_uv(16 - y2, 16 - y1)
    faces = {
        'down': {'uv': [x1, z1, x2, z2], 'texture': t}, 'up': {'uv': [x1, z1, x2, z2], 'texture': t},
        'north': {'uv': [16 - x2, v1, 16 - x1, v2], 'texture': t}, 'south': {'uv': [x1, v1, x2, v2], 'texture': t},
        'west': {'uv': [z1, v1, z2, v2], 'texture': t}, 'east': {'uv': [16 - z2, v1, 16 - z1, v2], 'texture': t},
    }
    for f in faces.values():
        f['uv'] = [max(0, min(16, u)) for u in f['uv']]
    return {'from': frm, 'to': to, 'faces': faces}


def model(boxes):
    textures = sorted({b[2] for b in boxes})
    return {'parent': 'minecraft:block/block', 'ambientocclusion': False,
            'textures': {'particle': f'{MOD}:block/cream_wood', **{t: f'{MOD}:block/{t}' for t in textures}},
            'elements': [element(*b) for b in boxes]}


def main():
    for name, rows in TEXTURES.items():
        path = os.path.join(ASSETS, 'textures', 'block', name + '.png')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        draw(rows).save(path)
    for name, boxes in PIECES.items():
        write_json(os.path.join(ASSETS, 'models', 'block', name + '.json'), model(boxes))

    # Blockstates: facing-aware (the front faces whoever placed it). The lamp also has lit=true/false.
    for name in ('chair', 'table', 'couch', 'golden_trim_table'):
        write_json(os.path.join(ASSETS, 'blockstates', name + '.json'), {'variants': {
            f'facing={f}': {'model': f'{MOD}:block/{name}', **({'y': y} if y else {})} for f, y in FACING_Y.items()}})
    write_json(os.path.join(ASSETS, 'blockstates', 'lamp.json'), {'variants': {
        f'facing={f},lit={str(lit).lower()}': {'model': f'{MOD}:block/lamp' + ('_on' if lit else ''), **({'y': y} if y else {})}
        for f, y in FACING_Y.items() for lit in (False, True)}})
    write_json(os.path.join(ASSETS, 'blockstates', 'fancy_bed.json'), {'variants': {
        f'facing={f},occupied={str(o).lower()},part={p}': {'model': f'{MOD}:block/fancy_bed_{p}', **({'y': y} if y else {})}
        for f, y in FACING_Y.items() for p in ('foot', 'head') for o in (False, True)}})

    # Item models (the lamp shows switched on in your inventory; the bed shows both halves, like a vanilla bed).
    for name in ('chair', 'table', 'couch', 'golden_trim_table'):
        write_json(os.path.join(ASSETS, 'items', name + '.json'), {'model': {'type': 'minecraft:model', 'model': f'{MOD}:block/{name}'}})
    write_json(os.path.join(ASSETS, 'items', 'lamp.json'), {'model': {'type': 'minecraft:model', 'model': f'{MOD}:block/lamp_on'}})
    write_json(os.path.join(ASSETS, 'items', 'fancy_bed.json'), {'model': {'type': 'minecraft:composite', 'models': [
        {'type': 'minecraft:model', 'model': f'{MOD}:block/fancy_bed_head'},
        {'type': 'minecraft:model', 'model': f'{MOD}:block/fancy_bed_foot',
         'transformation': {'left_rotation': [0, 0, 0, 1], 'right_rotation': [0, 0, 0, 1], 'scale': [1, 1, 1], 'translation': [0, 0, 1]}}]}})
    # Bed halves use the vanilla bed's hand/inventory poses.
    for half in ('head', 'foot'):
        p = os.path.join(ASSETS, 'models', 'block', f'fancy_bed_{half}.json')
        m = json.load(open(p))
        m['parent'] = 'minecraft:block/template_bed'
        write_json(p, m)

    # Loot: each piece drops itself (the bed only from its head half, like vanilla beds, so you get one bed back).
    for name in NAMES:
        pool = {'condition': {'type': 'minecraft:survives_explosion'}, 'entries': [{'type': 'minecraft:item', 'name': f'{MOD}:{name}'}],
                'rolls': 1}
        if name == 'fancy_bed':
            pool['entries'][0]['condition'] = {'type': 'minecraft:match_block', 'blocks': f'{MOD}:fancy_bed', 'state': {'part': 'head'}}
        write_json(os.path.join(DATA, MOD, 'loot_table', 'blocks', name + '.json'),
                   {'type': 'minecraft:block', 'pools': [pool], 'random_sequence': f'{MOD}:blocks/{name}'})

    # Recipes
    P, S, BW, WW, GN = '#minecraft:planks', 'minecraft:stick', 'minecraft:light_blue_wool', 'minecraft:white_wool', 'minecraft:gold_nugget'
    recipes = {
        'chair': ({'P': P, 'W': BW, 'S': S}, ['P  ', 'PWP', 'S S']),
        'table': ({'P': P, 'G': GN, 'S': S}, ['PPP', 'SGS', 'S S']),
        'golden_trim_table': ({'P': P, 'K': 'minecraft:pink_dye', 'G': 'minecraft:gold_ingot', 'S': S}, ['PKP', 'SGS', 'S S']),
        'couch': ({'P': P, 'W': BW, 'G': GN}, ['W  ', 'WWW', 'PGP']),
        'fancy_bed': ({'P': P, 'W': BW, 'L': WW, 'G': GN}, ['GLG', 'WWW', 'PPP']),
        'lamp': ({'L': WW, 'T': 'minecraft:torch', 'G': 'minecraft:gold_ingot'}, [' L ', ' T ', ' G ']),
    }
    for name, (key, pattern) in recipes.items():
        write_json(os.path.join(DATA, MOD, 'recipe', name + '.json'), {
            'type': 'minecraft:crafting_shaped', 'category': 'building', 'group': 'french_furniture', 'key': key,
            'pattern': pattern, 'result': {'id': f'{MOD}:{name}'}})

    # Mine them fastest with an axe (they're wood).
    write_json(os.path.join(DATA, 'minecraft', 'tags', 'block', 'mineable', 'axe.json'),
               {'replace': False, 'values': [f'{MOD}:{n}' for n in NAMES]})

    # Names, merged into the existing language files.
    for lang, i in (('en_us', 0), ('fr_fr', 1)):
        p = os.path.join(ASSETS, 'lang', lang + '.json')
        d = json.load(open(p))
        for n, names in NAMES.items():
            d[f'block.{MOD}.{n}'] = names[i]
        d[f'entity.{MOD}.seat'] = 'Seat' if i == 0 else 'Siège'
        write_json(p, d)
    print('wrote', len(TEXTURES), 'textures,', len(PIECES), 'models and', len(NAMES), 'furniture pieces')


if __name__ == '__main__':
    main()
