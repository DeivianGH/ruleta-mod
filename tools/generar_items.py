"""Genera iconos pixel art editables para los objetos del mod (RGBA, 16x16)."""
from pathlib import Path
import struct
import zlib
import json

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/ruleta/textures/item"
ROOT.mkdir(parents=True, exist_ok=True)
MODELS = ROOT.parent.parent / "models/item"
MODELS.mkdir(parents=True, exist_ok=True)

def png(path, pixels):
    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xffffffff)
    raw = b"".join(b"\0" + bytes(sum((list(px) for px in row), [])) for row in pixels)
    data = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">2I5B", 16, len(pixels), 8, 6, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
    path.write_bytes(data)

def grid(): return [[(0, 0, 0, 0) for _ in range(16)] for _ in range(16)]
def rect(g, x1, y1, x2, y2, color):
    for y in range(y1, y2):
        for x in range(x1, x2): g[y][x] = color

# Heart silhouette with a shaded edge and a small highlight.
g = grid(); red=(205,48,68,255); dark=(116,25,42,255); light=(255,137,145,255)
for y, span in [(3,(3,6)),(4,(2,7)),(5,(2,7)),(6,(3,6)),(7,(4,5)),(8,(5,5))]: rect(g,span[0],y,span[1]+1,y+1,red)
for y in range(3,8):
    for x in range(2,8):
        if g[y][x][3]:
            if x in (2,7) or y==8: g[y][x]=dark
rect(g,3,4,5,5,light)
png(ROOT/"corazon.png", g)

# Original stylized mask used by the brief death-card animation.
g = grid(); charcoal=(33,38,48,255); red=(195,42,58,255); pale=(224,214,194,255); black=(18,20,27,255)
rect(g,5,1,11,3,charcoal); rect(g,3,3,13,10,charcoal); rect(g,4,3,12,9,pale)
rect(g,3,4,5,8,red); rect(g,11,4,13,8,red)
rect(g,5,5,7,7,black); rect(g,9,5,11,7,black); rect(g,7,6,9,8,red)
rect(g,5,9,11,11,charcoal); rect(g,6,9,7,11,black); rect(g,9,9,10,11,black)
rect(g,7,12,9,14,red); rect(g,5,14,11,15,charcoal)
png(ROOT/"death_face.png",g)

palettes=[(133,92,194),(81,156,91),(78,175,177),(113,113,124),(93,145,220),(241,190,52),(223,174,55),(247,205,70),(222,189,96),(185,201,225)]
for i, gold in enumerate(palettes):
    g=grid(); edge=(80,57,35,255); main=(*gold,255); shine=tuple(min(255,c+65) for c in gold)+(255,)
    if i >= 6: # Four original dolphin silhouettes with gold bodies and jewel details.
        rect(g,3,4,11,7,edge); rect(g,4,3,10,7,main); rect(g,6,2,9,4,shine)
        rect(g,8,4,12,6,main); rect(g,10,5,14,6,edge); rect(g,12,4,15,5,shine)
        rect(g,5,7,9,9,main); rect(g,4,8,8,10,edge); rect(g,4,9,7,11,main)
        rect(g,7,10,10,13,main); rect(g,8,12,11,14,edge); rect(g,9,12,12,13,shine)
        rect(g,5,5,6,6,(38,115,161,255)); rect(g,3,5,4,6,edge)
    else: # Totem idol, with a distinct palette for every variant.
        rect(g,5,2,11,4,edge); rect(g,6,1,10,3,shine); rect(g,4,4,12,11,edge)
        rect(g,5,4,11,10,main); rect(g,3,6,5,9,edge); rect(g,11,6,13,9,edge)
        rect(g,4,7,5,9,main); rect(g,11,7,12,9,main); rect(g,6,5,7,6,(31,29,34,255)); rect(g,9,5,10,6,(31,29,34,255))
        rect(g,7,8,9,9,shine); rect(g,6,11,10,13,edge); rect(g,7,11,9,12,main); rect(g,5,13,11,14,edge)
    if i >= 6:
        frames=[]
        for frame in range(4):
            f=[row[:] for row in g]
            for yy in range(4,7):
                for xx in range(10,16): f[yy][xx]=(0,0,0,0)
            fx,fy=[(12,5),(13,5),(12,4),(13,4)][frame]
            rect(f,fx,fy,min(16,fx+3),fy+1,main)
            if fx+2 < 16: f[fy][fx+2]=edge
            frames.extend(f)
        png(ROOT/f"totem_{i+1}.png",frames)
        (ROOT/f"totem_{i+1}.png.mcmeta").write_text(json.dumps({"animation": {"frametime": 6, "frames": [0, 1, 2, 3]}}, indent=2)+"\n", encoding="utf-8")
    else:
        png(ROOT/f"totem_{i+1}.png",g)

for item in ["corazon"] + [f"totem_{i}" for i in range(1, 11)]:
    if item.startswith("totem_") and int(item.split("_")[1]) >= 7:
        elements=[]
        for a,b in [([5,4,3],[11,8,13]),([4,5,12],[9,7,16]),([7,4,0],[9,8,4]),([6,3,8],[10,4,11]),([6,7,7],[8,9,10])]:
            faces={face:{"texture":"#all"} for face in ("north","south","east","west","up","down")}
            elements.append({"from":a,"to":b,"faces":faces})
        model={"textures":{"all":f"ruleta:item/{item}"},"elements":elements,
               "display":{"gui":{"rotation":[25,35,-12],"scale":[0.85,0.85,0.85]},"ground":{"scale":[0.5,0.5,0.5]},"thirdperson_righthand":{"rotation":[0,90,0],"translation":[0,2,1],"scale":[0.55,0.55,0.55]}}}
    else:
        model={"parent":"item/generated","textures":{"layer0":f"ruleta:item/{item}"},
               "display":{"gui":{"rotation":[0,0,-12],"scale":[1.15,1.15,1.15]}}}
    (MODELS/f"{item}.json").write_text(json.dumps(model, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

es = {"item.ruleta.ruleta_admin": "Varita de la Ruleta", "item.ruleta.corazon": "Corazón consumible"}
en = {"item.ruleta.ruleta_admin": "Roulette Wand", "item.ruleta.corazon": "Consumable Heart"}
for i, name in enumerate(["Amatista", "Bosque", "Coral", "Ceniza", "Tempestad", "Sol", "Delfín Dorado", "Delfín Solar", "Delfín Marino", "Delfín Abisal"], 1):
    es[f"item.ruleta.totem_{i}"] = f"Tótem de {name}"
    en[f"item.ruleta.totem_{i}"] = f"{name} Totem"
lang = ROOT.parent.parent / "lang"
(lang/"es_es.json").write_text(json.dumps(es, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
(lang/"en_us.json").write_text(json.dumps(en, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
