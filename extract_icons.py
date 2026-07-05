#!/usr/bin/env python3
import os
import base64
import requests
from PIL import Image
from io import BytesIO

# Design image path
DESIGN_IMAGE = "/home/openclaw/.openclaw/workspace/yishell/sy.png"
OUTPUT_DIR = "/home/openclaw/.openclaw/workspace/yishell/app/src/main/res/drawable-xxhdpi"

# API Configuration
API_URL = "https://apihub.agnes-ai.com/v1/images/generations"
API_KEY = "sk-dcY...TTsY"  # Need full key
MODEL = "agnes-image-2.1-flash"

# Icon definitions: (filename, x1, y1, x2, y2)
ICONS = [
    ("logo_paper_plane.png", 49, 51, 138, 91),
    ("ic_glass_server_blue.png", 90, 175, 265, 265),
    ("ic_glass_server_green.png", 90, 293, 265, 382),
    ("ic_glass_server_yellow.png", 90, 468, 160, 505),
    ("ic_glass_server_purple.png", 90, 525, 160, 562),
    ("ic_glass_server_cyan.png", 90, 582, 160, 619),
    ("ic_glass_server_blue_fav.png", 90, 700, 160, 737),
    ("ic_glass_server_blue_plus.png", 154, 872, 300, 948),
]

def crop_icon(img, x1, y1, x2, y2):
    """Crop icon from design image"""
    return img.crop((x1, y1, x2, y2))

def image_to_base64(img):
    """Convert PIL image to base64 string"""
    buffer = BytesIO()
    img.save(buffer, format="PNG")
    return base64.b64encode(buffer.getvalue()).decode()

def enhance_icon(base64_image, width, height):
    """Call Agnes API to enhance icon"""
    headers = {
        "Authorization": f"Bearer {API_KEY}",
        "Content-Type": "application/json"
    }
    
    payload = {
        "model": MODEL,
        "prompt": "Keep the exact same shape, composition, colors, and aspect ratio. Just enhance resolution and clarity. Make it sharper and higher quality. Do NOT change proportions. Remove white background, make it fully transparent.",
        "image": f"data:image/png;base64,{base64_image}",
        "size": f"{width}x{height}",
        "n": 1
    }
    
    response = requests.post(API_URL, json=headers, headers=headers)
    return response.json()

def remove_white_background(img, threshold=240):
    """Remove white background and make transparent"""
    img = img.convert("RGBA")
    data = img.getdata()
    
    new_data = []
    for item in data:
        # If pixel is close to white, make it transparent
        if item[0] > threshold and item[1] > threshold and item[2] > threshold:
            new_data.append((255, 255, 255, 0))
        else:
            new_data.append(item)
    
    img.putdata(new_data)
    return img

def main():
    # Create output directory if it doesn't exist
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    
    # Open design image
    design_img = Image.open(DESIGN_IMAGE)
    print(f"Design image size: {design_img.size}")
    
    for filename, x1, y1, x2, y2 in ICONS:
        print(f"\nProcessing: {filename}")
        
        # Crop icon
        icon = crop_icon(design_img, x1, y1, x2, y2)
        width, height = icon.size
        print(f"  Cropped size: {width}x{height}")
        
        # Convert to base64
        base64_img = image_to_base64(icon)
        
        # Enhance via API
        print(f"  Calling Agnes API...")
        result = enhance_icon(base64_img, width, height)
        
        if "data" in result and len(result["data"]) > 0:
            # Download enhanced image
            enhanced_url = result["data"][0]["url"]
            enhanced_response = requests.get(enhanced_url)
            enhanced_img = Image.open(BytesIO(enhanced_response.content))
            
            # Remove white background
            final_img = remove_white_background(enhanced_img)
            
            # Save to output directory
            output_path = os.path.join(OUTPUT_DIR, filename)
            final_img.save(output_path, "PNG")
            print(f"  Saved: {output_path}")
        else:
            print(f"  API Error: {result}")
    
    print("\nDone!")

if __name__ == "__main__":
    main()
