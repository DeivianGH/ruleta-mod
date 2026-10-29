"""Genera la animacion de la ruleta: 1 hoja PNG por color (48 cuadros en cuadricula 8x6), 8 colores.
La rueda gira frenando y se detiene con el color elegido arriba, bajo la flecha.
Uso:  python3 tools/generar_frames.py   (necesita Pillow)
El orden de COLORS debe ser igual al de RuletaColor.java."""
import math, os
from PIL import Image, ImageDraw

COLORS = [("roja", (229, 57, 53)), ("naranja", (251, 140, 0)), ("amarilla", (253, 216, 53)),
          ("verde", (67, 160, 71)), ("azul", (30, 136, 229)), ("cian", (0, 172, 193)),
          ("morada", (142, 36, 170)), ("rosada", (236, 64, 122))]
FRAMES, OUT, SS = 48, 128, 4          # SS = supersampling para bordes suaves
N = len(COLORS); SEG = 360 / N
root = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/ruleta/textures/ruleta")

def draw_frame(rot, target):
    S = OUT * SS
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx = cy = S // 2
    R = int(S * 0.42)
    box = [cx - R, cy - R, cx + R, cy + R]
    d.ellipse([cx - R - 10 * SS // 2, cy - R - 10 * SS // 2, cx + R + 10 * SS // 2, cy + R + 10 * SS // 2], fill=(25, 25, 25, 255))
    for i, (_, rgb) in enumerate(COLORS):
        a0 = i * SEG + rot
        d.pieslice(box, a0, a0 + SEG, fill=rgb + (255,), outline=(255, 255, 255, 255), width=SS)
    d.ellipse([cx - R // 6, cy - R // 6, cx + R // 6, cy + R // 6], fill=(30, 30, 30, 255), outline=(255, 255, 255, 255), width=SS)
    # flecha fija arriba
    tip = (cx, cy - R + int(R * 0.18)); l = (cx - int(R * 0.12), cy - R - int(R * 0.12)); r = (cx + int(R * 0.12), cy - R - int(R * 0.12))
    d.polygon([tip, l, r], fill=(255, 255, 255, 255), outline=(0, 0, 0, 255))
    return img.resize((OUT, OUT), Image.LANCZOS)

COLS, ROWS = 8, 6
os.makedirs(root, exist_ok=True)
for ci, (name, _) in enumerate(COLORS):
    sheet = Image.new("RGBA", (COLS * OUT, ROWS * OUT), (0, 0, 0, 0))
    rot_final = 270 - ci * SEG - SEG / 2          # centro del segmento elegido queda arriba
    for f in range(FRAMES):
        t = f / (FRAMES - 1)
        rot = rot_final + 1080 * (1 - t) ** 3     # frena al final
        sheet.paste(draw_frame(rot, ci), ((f % COLS) * OUT, (f // COLS) * OUT))
    sheet.save(os.path.join(root, f"{name}.png"))
print("listo")
