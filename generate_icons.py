#!/usr/bin/env python3
"""Generate 3D glass server cube icons via Agnes Image API."""

import requests
import json
import time
from PIL import Image
import io
import os

API_URL = "https://apihub.agnes-ai.com/v1/images/generations"
API_KEY = "sk-dcYTTsY"
OUTPUT_DIR = "/home/openclaw/.openclaw/workspace/yishell/app/src/main/res/drawable-xxhdpi"

ICONS = [
    {
        "filename": "logo_paper_plane.png",
        "prompt": "A single 3D glass paper airplane icon, isometric perspective, translucent blue glass with light refraction and white edge highlights, clean white background, professional UI icon design, photorealistic glass rendering, centered composition, modern tech logo style"
    },
    {
        "filename": "ic_glass_server_blue.png",
        "prompt": 'A single 3D glass server cube icon, isometric perspective, translucent blue glass with light refraction and white edge highlights, subtle internal circuit board pattern visible through the glass, floating on a faint shadow base, clean white background, professional UI icon design, photorealistic glass rendering, centered composition'
    },
    {
        "filename": "ic_glass_server_green.png",
        "prompt": 'A single 3D glass server cube icon, isometric perspective, translucent green glass with light refraction and white edge highlights, subtle internal circuit board pattern visible through the glass, floating on a faint shadow base, clean white background, professional UI icon design, photorealistic glass rendering, centered composition'
    },
    {
        "filename": "ic_glass_server_yellow.png",
        "prompt": 'A single 3D glass server cube icon, isometric perspective, translucent yellow glass with light refraction and white edge highlights, subtle internal circuit board pattern visible through the glass, floating on a faint shadow base, clean white background, professional UI icon design, photorealistic glass rendering, centered composition'
    },
    {
        "filename": "ic_glass_server_purple.png",
        "prompt": 'A single 3D glass server cube icon, isometric perspective, translucent purple glass with light refraction and white edge highlights, subtle internal circuit board pattern visible through the glass, floating on a faint shadow base, clean white background, professional UI icon design, photorealistic glass rendering, centered composition'
    },
    {
        "filename": "ic_glass_server_cyan.png",
        "prompt": 'A single 3D glass server cube icon, isometric perspective, translucent cyan glass with light refraction and white edge highlights, subtle internal circuit board pattern visible through the glass, floating on a faint shadow base, clean white background, professional UI icon design, photorealistic glass rendering, centered composition'
    },
    {
        "filename": "ic_glass_server_blue_fav.png",
        "prompt": 'A single 3D glass server cube icon, isometric perspective, translucent blue glass with light refraction and white edge highlights, subtle internal circuit board pattern visible through the glass, floating on a faint shadow base, clean white background, professional UI icon design, photorealistic glass rendering, centered composition'
    },
    {
        "filename": "ic_glass_server_blue_plus.png",
        "prompt": 'A single 3D glass server cube icon, isometric perspective, translucent blue glass with light refraction and white edge highlights, subtle internal circuit board pattern visible through the glass, with a small blue circular badge with white plus sign in the bottom right corner of the cube, floating on a faint shadow base, clean white background, professional UI icon design, photorealistic glass rendering, centered composition'
    },
]


def remove_white_bg(img, threshold=240):
    """Remove white background by converting to RGBA and making near-white pixels transparent."""
    img = img.convert("RGBA")
    data = img.getdata()
    new_data = []
    for r, g, b, a in data:
        if r > threshold and g > threshold and b > threshold:
            new_data.append((255, 255, 255, 0))
        else:
            new_data.append((r, g, b, a))
    img.putdata(new_data)
    return img


def generate_icon(icon_info):
    """Generate one icon via Agnes API, download, process, and save."""
    print(f"\n{'='*60}")
    print(f"Generating: {icon_info['filename']}")
    print(f"Prompt: {icon_info['prompt'][:80]}...")

    headers = {
        "Authorization": f"Bearer {API_KEY}",
        "Content-Type": "application/json",
    }
    payload = {
        "model": "agnes-image-2.1-flash",
        "prompt": icon_info["prompt"],
        "n": 1,
        "size": "512x512",
        "extra_body": {
            "response_format": "url"
        }
    }

    try:
        resp = requests.post(API_URL, headers=headers, json=payload, timeout=120)
        resp.raise_for_status()
        result = resp.json()

        if "data" not in result or len(result["data"]) == 0:
            print(f"  ERROR: No image data in response: {json.dumps(result, indent=2)[:300]}")
            return False

        image_url = result["data"][0].get("url")
        if not image_url:
            print(f"  ERROR: No URL in response data")
            return False

        print(f"  Downloading from: {image_url[:100]}...")
        img_resp = requests.get(image_url, timeout=60)
        img_resp.raise_for_status()

        img = Image.open(io.BytesIO(img_resp.content))
        print(f"  Downloaded image: {img.size} mode={img.mode}")

        img = remove_white_bg(img, threshold=235)
        img = img.resize((256, 256), Image.LANCZOS)

        out_path = os.path.join(OUTPUT_DIR, icon_info["filename"])
        img.save(out_path, "PNG")
        print(f"  SAVED: {out_path} ({os.path.getsize(out_path)} bytes)")
        return True

    except requests.exceptions.RequestException as e:
        print(f"  REQUEST ERROR: {e}")
        return False
    except Exception as e:
        print(f"  ERROR: {e}")
        return False


def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    results = []
    for icon in ICONS:
        success = generate_icon(icon)
        results.append((icon["filename"], success))
        time.sleep(1)

    print(f"\n{'='*60}")
    print("RESULTS SUMMARY:")
    for name, ok in results:
        status = "OK" if ok else "FAIL"
        print(f"  [{status}] {name}")


if __name__ == "__main__":
    main()
