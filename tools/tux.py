#!/usr/bin/env python3
"""French Mod: Tux, the family's real cat. A fluffy long-haired black cat with a big fluffy white chest (bib) and a white chin/muzzle.

Data trick: Tux's fur is the vanilla cat skin layout (64x32, the vanilla cat model), painted from the data below:
- FUR: the black coat colours, with soft 'fluff' streaks (FLUFF) so the long hair looks fluffy.
- WHITE: rectangles (x, y, w, h) on the skin that are white fur: the bib, the belly stripe, the chin and the muzzle.
- FRINGE: single white pixels that poke out of the bib edge, so the white looks fluffy instead of a straight line.
- EYES / NOSE: little details.
The spawn egg is drawn from EGG rows. Starts from the vanilla tuxedo cat skin (cat_black.png) only for the transparent areas.

Needs the Minecraft 26.3 client jar (Loom downloads it): set MC_CLIENT_JAR, or it is found in ~/.gradle/caches/fabric-loom/26.3/.
Run from the repo root, after tools/steel.py (which rewrites the language files): python3 tools/tux.py
"""
import glob
import io
import os
import json
import zipfile

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'frenchmod')

BLACK = (24, 22, 30, 255)
FLUFF = (48, 44, 58, 255)      # long-hair shine streaks
DEEP = (14, 12, 18, 255)       # shadowy undercoat
WHITE = (246, 244, 240, 255)
CREAM = (226, 222, 214, 255)   # white fur shadow, so the bib looks soft
EYE = (150, 196, 60, 255)      # green eyes
PUPIL = (20, 30, 10, 255)
NOSE = (232, 140, 150, 255)    # pink nose

# White fur on the skin (x, y, w, h). The body box is at (20,0): its top face (26..30, 0..6) is the chest, and its
# north face (26..30, 6..22) is the belly after the model turns it. The head box is at (0,0): its bottom face is
# (10..15, 0..5) and its front face is (5..10, 5..9). The nose/muzzle box is at (0,24).
WHITE_RECTS = [
    (26, 0, 4, 6),     # big white chest
    (26, 6, 4, 16),    # white belly stripe
    (25, 6, 1, 7),     # bib wraps a little around both sides (fluffy and big)
    (30, 6, 1, 7),
    (10, 0, 5, 5),     # under the chin
    (6, 8, 3, 1),      # white chin on the face
    (0, 24, 8, 4),     # white muzzle
    (40, 8, 8, 4),     # white front paws (the bottom of the front legs)
]
CREAM_PIXELS = [(26, 5), (29, 5), (27, 21), (28, 21), (26, 12), (29, 12), (11, 4), (13, 4)]
FRINGE = [(24, 7), (24, 9), (31, 8), (31, 10), (25, 13), (30, 14), (25, 15), (5, 8), (9, 8), (26, 22) ]
# Fluffy streaks in the black coat: (x, y) every few pixels.
FLUFF_PIXELS = [(21, 7), (22, 10), (21, 14), (23, 17), (22, 20), (32, 7), (34, 11), (33, 15), (35, 18), (32, 21),
                (37, 8), (38, 12), (37, 16), (39, 19), (1, 6), (3, 7), (16, 6), (18, 7), (6, 1), (8, 3), (12, 6),
                (0, 16), (1, 19), (4, 17), (5, 21), (9, 15), (10, 18), (21, 2), (23, 4), (33, 2), (35, 4)]
DEEP_PIXELS = [(20, 21), (35, 21), (8, 18), (14, 8), (19, 8)]
EYE_PIXELS = [(5, 6), (8, 6)]
PUPIL_PIXELS = [(6, 6), (9, 6)]
NOSE_PIXELS = [(3, 26)]

# Spawn egg: '.' clear, 'k' black, 'f' fluff, 'w' white, 'e' green eye, 'p' pink nose.
EGG = [
    '................',
    '......kkkk......',
    '.....kfkkfk.....',
    '....kkkkkkkk....',
    '....kekkkkek....',
    '...kkkkkkkkkk...',
    '...kkkwpwkkkk...',
    '...kkwwwwwkkk...',
    '..kfkwwwwwkfkk..',
    '..kkwwwwwwwkkk..',
    '..kkwwwwwwwwkk..',
    '..kfkwwwwwwkfk..',
    '...kkkwwwwkkk...',
    '...kkkkkkkkkk...',
    '.....kkkkkk.....',
    '................',
]
EGG_PALETTE = {'k': BLACK, 'f': FLUFF, 'w': WHITE, 'e': EYE, 'p': NOSE}


def client_jar():
    if os.environ.get('MC_CLIENT_JAR'):
        return os.environ['MC_CLIENT_JAR']
    return glob.glob(os.path.expanduser('~/.gradle/caches/fabric-loom/26.3/minecraft-client.jar'))[0]


def main():
    with zipfile.ZipFile(client_jar()) as z:
        base = Image.open(io.BytesIO(z.read('assets/minecraft/textures/entity/cat/cat_black.png'))).convert('RGBA')
    img = Image.new('RGBA', base.size, (0, 0, 0, 0))
    for y in range(base.height):
        for x in range(base.width):
            if base.getpixel((x, y))[3] > 0:
                img.putpixel((x, y), BLACK)
    for x, y, w, h in WHITE_RECTS:
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                if base.getpixel((xx, yy))[3] > 0:
                    img.putpixel((xx, yy), WHITE)
    for pixels, colour in ((FRINGE, WHITE), (CREAM_PIXELS, CREAM), (FLUFF_PIXELS, FLUFF), (DEEP_PIXELS, DEEP), (EYE_PIXELS, EYE),
                           (PUPIL_PIXELS, PUPIL), (NOSE_PIXELS, NOSE)):
        for p in pixels:
            if base.getpixel(p)[3] > 0:
                img.putpixel(p, colour)
    os.makedirs(os.path.join(ASSETS, 'textures', 'entity'), exist_ok=True)
    img.save(os.path.join(ASSETS, 'textures', 'entity', 'tux.png'))

    egg = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(EGG):
        for x, ch in enumerate(row):
            if ch != '.':
                egg.putpixel((x, y), EGG_PALETTE[ch])
    os.makedirs(os.path.join(ASSETS, 'textures', 'item'), exist_ok=True)
    egg.save(os.path.join(ASSETS, 'textures', 'item', 'tux_spawn_egg.png'))
    # Spawn egg item model, and names (merged into the language files).
    def write_json(path, obj):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, 'w') as f:
            json.dump(obj, f, indent=2, ensure_ascii=False)
            f.write('\n')
    write_json(os.path.join(ASSETS, 'models', 'item', 'tux_spawn_egg.json'),
               {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'frenchmod:item/tux_spawn_egg'}})
    write_json(os.path.join(ASSETS, 'items', 'tux_spawn_egg.json'), {'model': {'type': 'minecraft:model', 'model': 'frenchmod:item/tux_spawn_egg'}})
    for lang, names in (('en_us', ('Tux', 'Tux Spawn Egg')), ('fr_fr', ('Tux', 'Œuf d\'apparition de Tux'))):
        p = os.path.join(ASSETS, 'lang', lang + '.json')
        d = json.load(open(p))
        d['entity.frenchmod.tux'], d['item.frenchmod.tux_spawn_egg'] = names
        write_json(p, d)
    print('wrote tux.png and tux_spawn_egg.png')


if __name__ == '__main__':
    main()
