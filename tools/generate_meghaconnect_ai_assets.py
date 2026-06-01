from __future__ import annotations

import json
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]

DEEP_BLUE = "#0B1F5C"
BLUE = "#1E40AF"
CYAN = "#22D3EE"
PURPLE = "#8B5CF6"
WHITE = "#FFFFFF"
TAGLINE = "AI Powered Appointment & Scheme Management Platform"


def ensure(path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)


def svg_mark(width: int = 512, horizontal: bool = False, dark: bool = False) -> str:
    bg = "#071538" if dark else "none"
    text = WHITE if dark else DEEP_BLUE
    sub = "#C7D2FE" if dark else "#475569"
    mark_x = 24
    mark_size = 144 if horizontal else 464
    view_w = width
    view_h = 176 if horizontal else 512
    r = 34 if horizontal else 92
    cx = mark_x + mark_size / 2
    cy = view_h / 2
    scale = mark_size / 512

    def p(x: float, y: float) -> tuple[float, float]:
        return mark_x + x * scale, cy - mark_size / 2 + y * scale

    nodes = [(162, 160), (352, 162), (256, 256), (166, 354), (348, 354)]
    node_svg = "\n".join(
        f'<circle cx="{p(x, y)[0]:.1f}" cy="{p(x, y)[1]:.1f}" r="{12 * scale:.1f}" fill="{WHITE}" opacity="0.96"/>'
        f'<circle cx="{p(x, y)[0]:.1f}" cy="{p(x, y)[1]:.1f}" r="{22 * scale:.1f}" fill="{CYAN}" opacity="0.22"/>'
        for x, y in nodes
    )
    line_svg = "\n".join(
        f'<line x1="{p(a[0], a[1])[0]:.1f}" y1="{p(a[0], a[1])[1]:.1f}" x2="{p(b[0], b[1])[0]:.1f}" y2="{p(b[0], b[1])[1]:.1f}" stroke="{WHITE}" stroke-width="{7 * scale:.1f}" opacity="0.72" stroke-linecap="round"/>'
        for a, b in [
            (nodes[0], nodes[2]),
            (nodes[1], nodes[2]),
            (nodes[2], nodes[3]),
            (nodes[2], nodes[4]),
            (nodes[0], nodes[1]),
            (nodes[3], nodes[4]),
        ]
    )
    calendar_x, calendar_y = p(176, 184)
    calendar_w, calendar_h = 160 * scale, 154 * scale
    bars = "\n".join(
        f'<rect x="{calendar_x + (24 + i * 38) * scale:.1f}" y="{calendar_y + 72 * scale:.1f}" width="{22 * scale:.1f}" height="{18 * scale:.1f}" rx="{5 * scale:.1f}" fill="{WHITE}" opacity="{0.96 if i in (0, 3) else 0.44}"/>'
        for i in range(4)
    )
    text_block = ""
    if horizontal:
        text_block = f"""
  <g transform="translate(196 50)">
    <text x="0" y="38" font-family="Inter, Segoe UI, Arial, sans-serif" font-size="38" font-weight="850" fill="{text}">MeghaConnect AI</text>
    <text x="1" y="70" font-family="Inter, Segoe UI, Arial, sans-serif" font-size="15" font-weight="650" fill="{sub}">{TAGLINE}</text>
  </g>"""

    return f"""<svg xmlns="http://www.w3.org/2000/svg" width="{view_w}" height="{view_h}" viewBox="0 0 {view_w} {view_h}" fill="none">
  <rect width="{view_w}" height="{view_h}" rx="{r}" fill="{bg}"/>
  <defs>
    <linearGradient id="mcaiGrad" x1="44" y1="30" x2="168" y2="168" gradientUnits="userSpaceOnUse">
      <stop stop-color="{CYAN}"/>
      <stop offset="0.55" stop-color="{BLUE}"/>
      <stop offset="1" stop-color="{PURPLE}"/>
    </linearGradient>
    <filter id="softShadow" x="-20%" y="-20%" width="140%" height="140%">
      <feDropShadow dx="0" dy="14" stdDeviation="16" flood-color="#020617" flood-opacity="0.24"/>
    </filter>
  </defs>
  <g filter="url(#softShadow)">
    <rect x="{mark_x}" y="{cy - mark_size / 2:.1f}" width="{mark_size}" height="{mark_size}" rx="{r}" fill="url(#mcaiGrad)"/>
    <path d="M {p(116, 146)[0]:.1f} {p(116, 146)[1]:.1f} C {p(160, 80)[0]:.1f} {p(160, 80)[1]:.1f}, {p(334, 78)[0]:.1f} {p(334, 78)[1]:.1f}, {p(392, 148)[0]:.1f} {p(392, 148)[1]:.1f}" stroke="{WHITE}" stroke-width="{18 * scale:.1f}" stroke-linecap="round" opacity="0.42"/>
    {line_svg}
    <rect x="{calendar_x:.1f}" y="{calendar_y:.1f}" width="{calendar_w:.1f}" height="{calendar_h:.1f}" rx="{24 * scale:.1f}" fill="{WHITE}" opacity="0.24"/>
    <rect x="{calendar_x + 20 * scale:.1f}" y="{calendar_y + 36 * scale:.1f}" width="{calendar_w - 40 * scale:.1f}" height="{8 * scale:.1f}" rx="{4 * scale:.1f}" fill="{WHITE}" opacity="0.82"/>
    {bars}
    {node_svg}
    <path d="M {p(140, 412)[0]:.1f} {p(140, 412)[1]:.1f} C {p(202, 452)[0]:.1f} {p(202, 452)[1]:.1f}, {p(310, 452)[0]:.1f} {p(310, 452)[1]:.1f}, {p(372, 412)[0]:.1f} {p(372, 412)[1]:.1f}" stroke="{WHITE}" stroke-width="{16 * scale:.1f}" stroke-linecap="round" opacity="0.5"/>
  </g>
  {text_block}
</svg>
"""


def save_svg(path: Path, content: str) -> None:
    ensure(path)
    path.write_text(content, encoding="utf-8")


def draw_icon(size: int, transparent: bool = False) -> Image.Image:
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0) if transparent else (255, 255, 255, 255))
    d = ImageDraw.Draw(img)
    pad = int(size * 0.08)
    radius = int(size * 0.22)
    rect = [pad, pad, size - pad, size - pad]
    for y in range(rect[1], rect[3]):
        t = (y - rect[1]) / max(1, rect[3] - rect[1])
        r = int(34 * (1 - t) + 139 * t)
        g = int(211 * (1 - t) + 92 * t)
        b = int(238 * (1 - t) + 246 * t)
        d.line([(rect[0], y), (rect[2], y)], fill=(r, g, b, 255), width=1)
    mask = Image.new("L", (size, size), 0)
    md = ImageDraw.Draw(mask)
    md.rounded_rectangle(rect, radius=radius, fill=255)
    img.putalpha(mask if transparent else Image.new("L", (size, size), 255))
    if not transparent:
        clipped = Image.new("RGBA", (size, size), (255, 255, 255, 255))
        clipped.paste(img, (0, 0), mask)
        img = clipped
        d = ImageDraw.Draw(img)
    d.rounded_rectangle(rect, radius=radius, outline=(255, 255, 255, 90), width=max(1, size // 42))

    pts = [
        (0.32, 0.31),
        (0.68, 0.31),
        (0.50, 0.50),
        (0.32, 0.68),
        (0.68, 0.68),
    ]
    def xy(p):
        return int(size * p[0]), int(size * p[1])

    for a, b in [(0, 2), (1, 2), (2, 3), (2, 4), (0, 1), (3, 4)]:
        d.line([xy(pts[a]), xy(pts[b])], fill=(255, 255, 255, 180), width=max(2, size // 55))

    cal = [int(size * 0.34), int(size * 0.37), int(size * 0.66), int(size * 0.66)]
    d.rounded_rectangle(cal, radius=max(3, size // 28), fill=(255, 255, 255, 62))
    d.rounded_rectangle(
        [cal[0] + size // 24, cal[1] + size // 16, cal[2] - size // 24, cal[1] + size // 13],
        radius=max(1, size // 80),
        fill=(255, 255, 255, 210),
    )
    cell = max(2, size // 22)
    gap = max(2, size // 34)
    for row in range(2):
        for col in range(3):
            alpha = 235 if (row, col) in ((0, 0), (1, 2)) else 120
            x = cal[0] + int(size * 0.085) + col * (cell + gap)
            y = cal[1] + int(size * 0.13) + row * (cell + gap)
            d.rounded_rectangle([x, y, x + cell, y + cell], radius=max(1, size // 90), fill=(255, 255, 255, alpha))
    for p in pts:
        x, y = xy(p)
        r = max(3, size // 35)
        d.ellipse([x - r, y - r, x + r, y + r], fill=(255, 255, 255, 245))
        d.ellipse([x - r * 2, y - r * 2, x + r * 2, y + r * 2], outline=(34, 211, 238, 90), width=max(1, size // 90))

    d.arc([int(size * 0.25), int(size * 0.72), int(size * 0.75), int(size * 0.92)], 205, 335, fill=(255, 255, 255, 150), width=max(2, size // 38))
    return img


def save_png(path: Path, size: int, transparent: bool = False) -> None:
    ensure(path)
    draw_icon(size, transparent=transparent).save(path)


def save_ico(path: Path) -> None:
    ensure(path)
    base = draw_icon(256)
    base.save(path, sizes=[(16, 16), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)])


def main() -> None:
    asset_roots = [
        ROOT / "frontend" / "public" / "asserts",
        ROOT / "frontend" / "src" / "asserts",
        ROOT / "mobile" / "assets",
    ]
    for asset_root in asset_roots:
        save_svg(asset_root / "meghaconnect-ai-mark.svg", svg_mark(512, horizontal=False))
        save_svg(asset_root / "meghaconnect-ai-horizontal.svg", svg_mark(760, horizontal=True))
        save_svg(asset_root / "meghaconnect-ai-dark.svg", svg_mark(760, horizontal=True, dark=True))
        save_svg(asset_root / "meghaconnect-ai-light.svg", svg_mark(760, horizontal=True, dark=False))
        save_png(asset_root / "logo.png", 512, transparent=True)
        save_png(asset_root / "meghaconnect-ai-icon.png", 1024, transparent=True)
        save_png(asset_root / "meghaconnect-ai-icon-light.png", 1024, transparent=False)

    save_ico(ROOT / "frontend" / "public" / "asserts" / "favicon.ico")
    save_png(ROOT / "mobile" / "web" / "favicon.png", 64)
    for name, size in {
        "Icon-192.png": 192,
        "Icon-512.png": 512,
        "Icon-maskable-192.png": 192,
        "Icon-maskable-512.png": 512,
    }.items():
        save_png(ROOT / "mobile" / "web" / "icons" / name, size)

    for density, size in {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }.items():
        save_png(ROOT / "mobile" / "android" / "app" / "src" / "main" / "res" / density / "ic_launcher.png", size)
    save_png(
        ROOT / "mobile" / "android" / "app" / "src" / "main" / "res" / "drawable" / "ic_launcher_foreground.png",
        432,
        transparent=True,
    )

    ios_dir = ROOT / "mobile" / "ios" / "Runner" / "Assets.xcassets" / "AppIcon.appiconset"
    if ios_dir.exists():
        for png in ios_dir.glob("Icon-App-*.png"):
            stem = png.stem
            spec = stem.replace("Icon-App-", "")
            base, scale = spec.split("@")
            scale_num = int(scale.replace("x", ""))
            logical = float(base.split("x")[0])
            save_png(png, int(round(logical * scale_num)))

    manifest = ROOT / "mobile" / "web" / "manifest.json"
    if manifest.exists():
        data = json.loads(manifest.read_text(encoding="utf-8"))
        data["name"] = "MeghaConnect AI"
        data["short_name"] = "MeghaConnect AI"
        data["description"] = TAGLINE
        data["background_color"] = "#0B1F5C"
        data["theme_color"] = "#1E40AF"
        manifest.write_text(json.dumps(data, indent=4) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
