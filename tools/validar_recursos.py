"""Comprueba el JSON, modelos e iconos del paquete de recursos del mod."""
import json
import struct
from pathlib import Path

root = Path(__file__).resolve().parents[1]
assets = root / "src/main/resources/assets/ruleta"
json_files = list(assets.rglob("*.json")) + [root / "src/main/resources/fabric.mod.json"]
for file in json_files:
    json.loads(file.read_text(encoding="utf-8"))

models = list((assets / "models/item").glob("*.json"))
for model_file in models:
    model = json.loads(model_file.read_text(encoding="utf-8"))
    for texture in model.get("textures", {}).values():
        namespace, path = texture.split(":", 1) if ":" in texture else ("minecraft", texture)
        if namespace == "ruleta" and not (assets / f"textures/{path}.png").is_file():
            raise SystemExit(f"Falta la textura {texture}, referenciada por {model_file.name}")

for item in ["corazon"] + [f"totem_{i}" for i in range(1, 11)]:
    texture = assets / f"textures/item/{item}.png"
    if not texture.is_file():
        raise SystemExit(f"Falta {texture}")
    data = texture.read_bytes()
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise SystemExit(f"PNG invalido: {texture}")
    width, height = struct.unpack(">II", data[16:24])
    if width != 16 or height not in (16, 64):
        raise SystemExit(f"Dimensiones inesperadas ({width}x{height}): {texture}")

for i in range(7, 11):
    model = json.loads((assets / f"models/item/totem_{i}.json").read_text(encoding="utf-8"))
    animation = json.loads((assets / f"textures/item/totem_{i}.png.mcmeta").read_text(encoding="utf-8"))
    if len(model.get("elements", [])) < 3 or len(animation["animation"].get("frames", [])) != 4:
        raise SystemExit(f"Modelo o animacion incompletos: totem_{i}")

print(f"OK: {len(json_files)} JSON validos, {len(models)} modelos, 11 iconos y 4 modelos animados comprobados.")
