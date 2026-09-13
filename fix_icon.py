from PIL import Image
import os

src = "app/src/main/res/drawable/icon.png"
if not os.path.exists(src):
    print(f"Error: {src} not found")
    exit(1)

img = Image.open(src).convert("RGBA")

# Compute 1:1 square from the source image
w, h = img.size
side = max(w, h)  # use the larger dimension as the canvas

# Create a square transparent canvas
canvas = Image.new("RGBA", (side, side), (0, 0, 0, 0))
offset_x = (side - w) // 2
offset_y = (side - h) // 2
canvas.paste(img, (offset_x, offset_y))

# Resize to standard 1024x1024
canvas = canvas.resize((1024, 1024), Image.LANCZOS)

# Output directories and sizes
sizes = {
    "mipmap-mdpi":   48,
    "mipmap-hdpi":   72,
    "mipmap-xhdpi":  96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}
for dir_name, size in sizes.items():
    out_dir = f"app/src/main/res/{dir_name}"
    os.makedirs(out_dir, exist_ok=True)
    resized = canvas.resize((size, size), Image.LANCZOS)
    resized.save(f"{out_dir}/ic_launcher.png", "PNG")
    resized.save(f"{out_dir}/ic_launcher_round.png", "PNG")
    resized.save(f"{out_dir}/ic_launcher_foreground.png", "PNG")
    print(f"Saved {size}x{size} -> {out_dir}/")

# Also save full 1024 for reference
canvas.save("app/src/main/res/drawable/ic_launcher_hires.png", "PNG")
print("Done generating mipmap assets.")
