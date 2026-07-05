#!/usr/bin/env python3
"""Generate 3D glass server cube PNG icons for YiShell."""
from PIL import Image, ImageDraw, ImageFilter
import math, os

OUT = "/home/openclaw/.openclaw/workspace/yishell/app/src/main/res/drawable-xxhdpi"
os.makedirs(OUT, exist_ok=True)

COLORS = {
    "blue":   (0, 122, 255, 255),
    "green":  (0, 214, 143, 255),
    "yellow": (255, 204, 0, 255),
    "purple": (156, 39, 176, 255),
    "cyan":   (0, 188, 212, 255),
    "red":    (239, 68, 68, 255),
}

def lerp_color(c1, c2, t):
    return tuple(int(a + (b - a) * t) for a, b in zip(c1, c2))

def draw_glass_cube(draw, cx, cy, size, base_color, alpha=0.85):
    """Draw an isometric glass cube at (cx, cy) with given size."""
    w = size
    h = size
    s = w * 0.42

    top = cy - h * 0.28
    mid = cy - h * 0.05
    bot = cy + h * 0.35

    left = cx - s
    right = cx + s
    center = cx

    r, g, b, _ = base_color

    # Shadow
    shadow_y = bot + h * 0.08
    draw.ellipse(
        [cx - s * 0.8, shadow_y - h * 0.04, cx + s * 0.8, shadow_y + h * 0.06],
        fill=(0, 0, 0, 30)
    )

    # Glass base reflection
    base_pts = [
        (cx - s * 1.1, bot + h * 0.02),
        (cx, bot + h * 0.15),
        (cx + s * 1.1, bot + h * 0.02),
    ]
    draw.polygon(base_pts, fill=(255, 255, 255, 50))

    # Left face
    left_pts = [
        (left, mid),
        (center, top),
        (center, bot),
        (left, mid + h * 0.33),
    ]
    face_l = (int(r * 0.45), int(g * 0.45), int(b * 0.45), int(alpha * 255))
    draw.polygon(left_pts, fill=face_l)

    # Right face
    right_pts = [
        (right, mid),
        (center, top),
        (center, bot),
        (right, mid + h * 0.33),
    ]
    face_r = (int(r * 0.35), int(g * 0.35), int(b * 0.35), int(alpha * 255))
    draw.polygon(right_pts, fill=face_r)

    # Top face (diamond)
    top_pts = [
        (center, top),
        (right, mid),
        (center, mid + h * 0.12),
        (left, mid),
    ]
    top_face = (min(255, r + 80), min(255, g + 80), min(255, b + 80), int(alpha * 255))
    draw.polygon(top_pts, fill=top_face)

    # Glass highlight on top face
    highlight_pts = [
        (center, top + h * 0.02),
        (right - s * 0.15, mid - h * 0.02),
        (center, mid + h * 0.08),
        (left + s * 0.15, mid - h * 0.02),
    ]
    draw.polygon(highlight_pts, fill=(255, 255, 255, 100))

    # Edge lines
    edge_c = (255, 255, 255, 120)
    lw = max(1, int(size * 0.015))

    draw.line([(center, top), (left, mid)], fill=edge_c, width=lw)
    draw.line([(center, top), (right, mid)], fill=edge_c, width=lw)
    draw.line([(center, top), (center, mid + h * 0.12)], fill=edge_c, width=lw)
    draw.line([(left, mid), (left, mid + h * 0.33)], fill=edge_c, width=lw)
    draw.line([(right, mid), (right, mid + h * 0.33)], fill=edge_c, width=lw)
    draw.line([(left, mid), (center, bot)], fill=edge_c, width=lw)
    draw.line([(right, mid), (center, bot)], fill=edge_c, width=lw)
    draw.line([(center, mid + h * 0.12), (center, bot)], fill=edge_c, width=lw)
    draw.line([(left, mid + h * 0.33), (center, bot)], fill=edge_c, width=lw)
    draw.line([(right, mid + h * 0.33), (center, bot)], fill=edge_c, width=lw)

    # Terminal symbol on right face
    sx = cx + s * 0.3
    sy = mid + h * 0.15
    sym_size = size * 0.12
    sym_color = (255, 255, 255, 200)
    sw = max(1, int(size * 0.02))

    draw.line(
        [(sx - sym_size * 0.3, sy - sym_size * 0.2),
         (sx + sym_size * 0.1, sy + sym_size * 0.1)],
        fill=sym_color, width=sw
    )
    draw.line(
        [(sx + sym_size * 0.1, sy + sym_size * 0.1),
         (sx - sym_size * 0.3, sy + sym_size * 0.4)],
        fill=sym_color, width=sw
    )
    draw.line(
        [(sx + sym_size * 0.0, sy + sym_size * 0.55),
         (sx + sym_size * 0.5, sy + sym_size * 0.55)],
        fill=sym_color, width=sw
    )

    return left, mid, right, bot, top, s, h


def draw_star(draw, cx, cy, outer_r, color):
    """Draw a 5-pointed star."""
    pts = []
    for i in range(10):
        angle = math.radians(i * 36 - 90)
        r = outer_r if i % 2 == 0 else outer_r * 0.4
        pts.append((cx + r * math.cos(angle), cy + r * math.sin(angle)))
    draw.polygon(pts, fill=color)


def draw_plus_badge(draw, cx, cy, radius):
    """Draw a blue circle with white plus sign."""
    draw.ellipse(
        [cx - radius, cy - radius, cx + radius, cy + radius],
        fill=(0, 122, 255, 255)
    )
    lw = max(1, int(radius * 0.25))
    draw.line([(cx - radius * 0.5, cy), (cx + radius * 0.5, cy)], fill=(255, 255, 255, 255), width=lw)
    draw.line([(cx, cy - radius * 0.5), (cx, cy + radius * 0.5)], fill=(255, 255, 255, 255), width=lw)


def gen_icon(color_name, base_color, size_px, show_star=False, show_plus=False, suffix=""):
    img = Image.new("RGBA", (size_px, size_px), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    cx = size_px // 2
    cy = size_px // 2 + size_px * 0.04

    draw_glass_cube(draw, cx, cy, size_px * 0.85, base_color)

    if show_star:
        star_x = cx + size_px * 0.32
        star_y = cy - size_px * 0.30
        star_r = size_px * 0.11
        draw_star(draw, star_x, star_y, star_r, (255, 204, 0, 255))

    if show_plus:
        badge_x = cx + size_px * 0.30
        badge_y = cy + size_px * 0.32
        badge_r = size_px * 0.10
        draw_plus_badge(draw, badge_x, badge_y, badge_r)

    name = f"ic_glass_server_{color_name}{suffix}"
    path = os.path.join(OUT, f"{name}.png")
    img.save(path, "PNG")
    print(f"  {name}.png ({size_px}x{size_px})")
    return path


print("Generating glass server cube icons...")

for color_name, base_color in COLORS.items():
    gen_icon(color_name, base_color, 192)
    if color_name == "blue":
        gen_icon(color_name, base_color, 192, show_plus=True, suffix="_plus")
        gen_icon(color_name, base_color, 96, show_star=True, suffix="_star")

print("Done!")
