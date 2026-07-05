#!/usr/bin/env python3
"""
从 sy.png 精确裁剪图标 → 图生图增强 → 保存为透明 PNG
硬编码 API Key，不需要问老板
"""
import subprocess, json, os, time, base64
from PIL import Image

# 硬编码 API Key
KEY = "sk-dcYJaww0fLjfB8tPQ9GJgf3T7tO9P7fjxowxPDt7LzHdTTsY"
DIR = "/home/openclaw/.openclaw/workspace/yishell/app/src/main/res/drawable-xxhdpi"
SY = "/home/openclaw/.openclaw/workspace/yishell/sy.png"

sy = Image.open(SY).convert("RGBA")
W, H = sy.size
print(f"sy.png: {W}x{H}")

# 图标坐标（sy.png 853x1844 的绝对坐标）
icons = {
    "logo_paper_plane": (49, 51, 138, 91),
    "ic_glass_server_blue": (90, 175, 265, 265),
    "ic_glass_server_green": (90, 293, 265, 382),
    "ic_glass_server_yellow": (90, 468, 160, 505),
    "ic_glass_server_purple": (90, 525, 160, 562),
    "ic_glass_server_cyan": (90, 582, 160, 619),
    "ic_glass_server_blue_fav": (90, 700, 160, 737),
    "ic_glass_server_blue_plus": (154, 872, 300, 948),
}

def img2img_enhance(icon_name, crop):
    """图生图增强：保持原始比例，只提升清晰度"""
    tmp_path = f"/tmp/icon_{icon_name}.png"
    crop.save(tmp_path)
    
    with open(tmp_path, "rb") as f:
        b64 = base64.b64encode(f.read()).decode()
    
    # 关键：用原始宽高比作为 size
    w, h = crop.size
    size_str = f"{w}x{h}"
    
    payload = {
        "model": "agnes-image-2.1-flash",
        "prompt": "Keep the exact same shape, composition, colors, and aspect ratio. Just enhance resolution and clarity. Make it sharper and higher quality. Do NOT change proportions. Remove white background, make it fully transparent.",
        "size": size_str,
        "extra_body": {
            "image": [f"data:image/png;base64,{b64}"],
            "response_format": "url"
        }
    }
    
    with open("/tmp/img2img.json", "w") as f:
        json.dump(payload, f)
    
    cmd = [
        "curl", "-s", "https://apihub.agnes-ai.com/v1/images/generations",
        "-H", f"Authorization: Bearer {KEY}",
        "-H", "Content-Type: application/json",
        "--data-binary", "@/tmp/img2img.json"
    ]
    result = subprocess.run(cmd, capture_output=True, text=True, timeout=300)
    resp = json.loads(result.stdout)
    
    if resp.get("data"):
        url = resp["data"][0]["url"]
        filepath = os.path.join(DIR, f"{icon_name}.png")
        subprocess.run(["curl", "-sL", "-o", filepath, url])
        
        # 去白底
        img = Image.open(filepath).convert("RGBA")
        pixels = img.load()
        for y in range(img.size[1]):
            for x in range(img.size[0]):
                r, g, b, a = pixels[x, y]
                if r > 230 and g > 230 and b > 230:
                    pixels[x, y] = (r, g, b, 0)
        img.save(filepath, "PNG", optimize=True)
        
        size_kb = os.path.getsize(filepath) / 1024
        print(f"  ✓ {icon_name}: {crop.size} -> {img.size} ({size_kb:.0f} KB)")
        return True
    else:
        print(f"  ✗ {icon_name}: {resp}")
        return False

# 裁剪 + 图生图增强
for name, (x1, y1, x2, y2) in icons.items():
    crop = sy.crop((x1, y1, x2, y2))
    print(f"\n{name}: crop {crop.size}")
    img2img_enhance(name, crop)
    time.sleep(3)

print("\n=== Done ===")
for f in sorted(os.listdir(DIR)):
    if f.endswith(".png") and ("ic_" in f or "logo" in f):
        size_kb = os.path.getsize(os.path.join(DIR, f)) / 1024
        print(f"  {f}: {size_kb:.0f} KB")
