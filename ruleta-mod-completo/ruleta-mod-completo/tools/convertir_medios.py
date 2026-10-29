"""Rebuild the provided clips as transparent Minecraft-ready frame atlases.

Run once with Pillow and FFmpeg installed. The MP4 files are only inputs; the
game reads the generated PNG atlases and OGG audio from its resource folder.
"""
from __future__ import annotations

import argparse
import math
import shutil
import struct
import subprocess
import wave
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/ruleta"


def build_wheel_atlas(path: Path) -> dict[str, int]:
    frame = 256
    columns, rows, count = 8, 5, 40
    scale = 3
    high = frame * scale
    center = high // 2
    radius = 76 * scale
    palette = [
        (139, 35, 184, 255), (208, 42, 158, 255), (232, 37, 65, 255),
        (241, 68, 4, 255), (255, 132, 0, 255), (250, 211, 0, 255),
        (181, 224, 7, 255), (83, 209, 100, 255), (71, 193, 213, 255),
        (56, 112, 211, 255), (67, 50, 188, 255), (125, 38, 184, 255),
    ]
    sheet = Image.new("RGBA", (frame * columns, frame * rows), (0, 0, 0, 0))
    bounds = (center-radius, center-radius, center+radius, center+radius)
    for frame_id in range(count):
        layer = Image.new("RGBA", (high, high), (0, 0, 0, 0))
        shadow = Image.new("RGBA", (high, high), (0, 0, 0, 0))
        ImageDraw.Draw(shadow).ellipse((center-radius+6, center-radius+8, center+radius+6, center+radius+8), fill=(4, 6, 14, 115))
        layer.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(7*scale)))
        draw = ImageDraw.Draw(layer)
        rotation = frame_id * 360.0 / count
        for index, color in enumerate(palette):
            start = -90 + rotation + index * 360 / len(palette)
            end = -90 + rotation + (index+1) * 360 / len(palette)
            draw.pieslice(bounds, start=start, end=end, fill=color)
        for index in range(len(palette)):
            angle = math.radians(-90 + rotation + index * 360 / len(palette))
            px = round(center + math.cos(angle) * radius)
            py = round(center + math.sin(angle) * radius)
            draw.line((center, center, px, py), fill=(29, 26, 39, 230), width=2*scale)
        draw.ellipse(bounds, outline=(35, 31, 45, 255), width=3*scale)
        hub = 12 * scale
        draw.ellipse((center-hub-2, center-hub-2, center+hub+2, center+hub+2), fill=(22, 24, 36, 255), outline=(240, 198, 75, 255), width=2*scale)
        draw.ellipse((center-4*scale, center-4*scale, center+4*scale, center+4*scale), fill=(255, 234, 161, 255))
        small = layer.resize((frame, frame), Image.Resampling.LANCZOS)
        sheet.alpha_composite(small, ((frame_id % columns) * frame, (frame_id // columns) * frame))
    path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(path, optimize=True, compress_level=8)
    return {"columns": columns, "rows": rows, "frames": count, "frameWidth": frame, "frameHeight": frame}


def read_exact(stream, size: int) -> bytes:
    parts = bytearray()
    while len(parts) < size:
        part = stream.read(size-len(parts))
        if not part:
            break
        parts.extend(part)
    return bytes(parts)


def build_death_atlas(ffmpeg: str, video: Path, output: Path) -> dict[str, int]:
    width, height = 160, 249
    columns, rows = 16, 12
    # El clip lleva un rótulo/placa roja opaca en sus primeros fotogramas.
    # Recortamos 0.35 s de cabecera y retiramos el negro del resto como alpha.
    filter_graph = "fps=20,trim=start=0.35,setpts=PTS-STARTPTS,crop=720:1120:0:0,scale=160:249:flags=lanczos"
    command = [ffmpeg, "-hide_banner", "-loglevel", "error", "-an", "-i", str(video),
               "-vf", filter_graph, "-f", "rawvideo", "-pix_fmt", "rgba", "pipe:1"]
    process = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    assert process.stdout is not None
    sheet = Image.new("RGBA", (width*columns, height*rows), (0, 0, 0, 0))
    frame_bytes = width * height * 4
    count = 0
    while True:
        data = read_exact(process.stdout, frame_bytes)
        if len(data) != frame_bytes:
            break
        image = Image.frombytes("RGBA", (width, height), data)
        r, g, b, _ = image.split()
        brightness = ImageChops.lighter(ImageChops.lighter(r, g), b)
        alpha = brightness.point(lambda value: max(0, min(255, round((value-14) * 255 / 64))))
        image.putalpha(alpha)
        col, row = count % columns, count // columns
        if row >= rows:
            process.kill()
            raise RuntimeError("Death clip produced more frames than the configured atlas can hold")
        sheet.alpha_composite(image, (col*width, row*height))
        count += 1
    process.stdout.close()
    stderr = process.stderr.read().decode("utf-8", "replace") if process.stderr else ""
    result = process.wait()
    if result != 0:
        raise RuntimeError(f"FFmpeg could not decode the death video: {stderr}")
    if not 1 <= count <= columns * rows:
        raise RuntimeError(f"Unexpected frame count from death video: {count}")
    output.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(output, optimize=True, compress_level=8)
    return {"columns": columns, "rows": rows, "frames": count, "frameWidth": width, "frameHeight": height}


def build_ambience_wav(path: Path, seconds: int = 15, rate: int = 22050) -> None:
    bell_notes = [(2.1, 293.66), (4.6, 220.00), (7.4, 246.94), (10.5, 196.00), (12.8, 261.63)]
    with wave.open(str(path), "wb") as wav:
        wav.setnchannels(2)
        wav.setsampwidth(2)
        wav.setframerate(rate)
        chunk = bytearray()
        for sample in range(seconds*rate):
            t = sample/rate
            fade_in = min(1.0, t/1.1)
            fade_out = min(1.0, max(0.0, (seconds-t)/1.25))
            envelope = min(fade_in, fade_out) * (0.75 + 0.25*math.sin(2*math.pi*0.12*t))
            drone = (0.27*math.sin(2*math.pi*49.0*t)
                     + 0.16*math.sin(2*math.pi*73.42*t + 0.4)
                     + 0.10*math.sin(2*math.pi*98.0*t + 1.0)
                     + 0.075*math.sin(2*math.pi*146.83*t + 0.8))
            pulse = 0.065*math.sin(2*math.pi*42.0*t) * (0.5 + 0.5*math.sin(2*math.pi*0.24*t))
            bell = 0.0
            for start, frequency in bell_notes:
                age = t-start
                if 0 <= age < 1.7:
                    bell += 0.10*math.exp(-2.4*age)*math.sin(2*math.pi*frequency*age)
            left = max(-0.90, min(0.90, (drone+pulse+bell)*envelope*0.63))
            right = max(-0.90, min(0.90, (drone+pulse+bell*0.82)*envelope*0.63))
            chunk.extend(struct.pack("<hh", int(left*32767), int(right*32767)))
            if len(chunk) >= 65536:
                wav.writeframesraw(chunk)
                chunk.clear()
        if chunk:
            wav.writeframesraw(chunk)


def encode_music(ffmpeg: str, output: Path) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    temp_wav = ROOT / "dist/deivil-ambience.wav"
    temp_wav.parent.mkdir(parents=True, exist_ok=True)
    build_ambience_wav(temp_wav)
    command = [ffmpeg, "-hide_banner", "-loglevel", "error", "-y", "-i", str(temp_wav), "-c:a", "vorbis", "-strict", "-2", "-q:a", "4", str(output)]
    result = subprocess.run(command, capture_output=True, text=True)
    temp_wav.unlink(missing_ok=True)
    if result.returncode:
        raise RuntimeError(f"FFmpeg Vorbis encoding failed: {result.stderr}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--ffmpeg", default=shutil.which("ffmpeg") or r"C:\Program Files\BlueStacks_nxt\ffmpeg.exe")
    parser.add_argument("--death-video", required=True, type=Path)
    parser.add_argument("--wheel-video", required=True, type=Path, help="Reference only: its watermarked wheel is recreated as a transparent atlas.")
    args = parser.parse_args()
    if not Path(args.ffmpeg).is_file():
        raise SystemExit(f"FFmpeg was not found: {args.ffmpeg}")
    if not args.death_video.is_file() or not args.wheel_video.is_file():
        raise SystemExit("Could not read one of the supplied MP4 files")

    wheel = ASSETS / "textures/animation/ruleta_wheel.png"
    death = ASSETS / "textures/animation/muerte.png"
    wheel_meta = build_wheel_atlas(wheel)
    death_meta = build_death_atlas(args.ffmpeg, args.death_video, death)
    (ASSETS / "textures/animation").mkdir(parents=True, exist_ok=True)
    import json
    (ASSETS / "textures/animation/ruleta_wheel.json").write_text(json.dumps(wheel_meta, indent=2)+"\n", encoding="utf-8")
    (ASSETS / "textures/animation/muerte.json").write_text(json.dumps(death_meta, indent=2)+"\n", encoding="utf-8")
    encode_music(args.ffmpeg, ASSETS / "sounds/momento_deivil.ogg")
    print(f"Wheel atlas: {wheel_meta}; death atlas: {death_meta}")


if __name__ == "__main__":
    main()
