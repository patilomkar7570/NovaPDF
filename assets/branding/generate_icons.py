import os
from PIL import Image, ImageDraw

src_path = 'assets/branding/nova-logo-black.png'
src_img = Image.open(src_path).convert('RGBA')

# Android mipmap densities and sizes
densities = {
    'mipmap-mdpi': 48,
    'mipmap-hdpi': 72,
    'mipmap-xhdpi': 96,
    'mipmap-xxhdpi': 144,
    'mipmap-xxxhdpi': 192
}

def make_icon(size, is_round):
    canvas = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(canvas)
    bg = (255, 255, 255, 255)
    border = (226, 232, 240, 255)
    bw = max(1, size // 64)
    if is_round:
        draw.ellipse([1, 1, size - 2, size - 2], fill=bg, outline=border, width=bw)
    else:
        rad = int(size * 0.22)
        draw.rounded_rectangle([1, 1, size - 2, size - 2], radius=rad, fill=bg, outline=border, width=bw)
    
    mark_sz = int(size * 0.65)
    mark_resized = src_img.resize((mark_sz, mark_sz), Image.Resampling.LANCZOS)
    offset = (size - mark_sz) // 2
    canvas.paste(mark_resized, (offset, offset), mark_resized)
    return canvas

res_dir = 'app/src/main/res'
for d_name, sz in densities.items():
    target_dir = os.path.join(res_dir, d_name)
    os.makedirs(target_dir, exist_ok=True)
    icon = make_icon(sz, False)
    icon.save(os.path.join(target_dir, 'ic_launcher.png'))
    icon_round = make_icon(sz, True)
    icon_round.save(os.path.join(target_dir, 'ic_launcher_round.png'))

# Adaptive icon foreground
drawable_dir = os.path.join(res_dir, 'drawable')
os.makedirs(drawable_dir, exist_ok=True)

# 432x432 foreground with symbol in safe area (around 240px)
fg_canvas = Image.new('RGBA', (432, 432), (0, 0, 0, 0))
fg_sz = 240
fg_mark = src_img.resize((fg_sz, fg_sz), Image.Resampling.LANCZOS)
fg_offset = (432 - fg_sz) // 2
fg_canvas.paste(fg_mark, (fg_offset, fg_offset), fg_mark)
fg_canvas.save(os.path.join(drawable_dir, 'ic_launcher_foreground.png'))

# Also save standalone logo marks for in-app UI
# Black mark:
logo_128 = src_img.resize((128, 128), Image.Resampling.LANCZOS)
logo_128.save(os.path.join(drawable_dir, 'ic_nova_logo.png'))

# White mark for dark mode:
white_src = Image.open('assets/branding/nova-logo-white.png').convert('RGBA')
logo_white_128 = white_src.resize((128, 128), Image.Resampling.LANCZOS)
logo_white_128.save(os.path.join(drawable_dir, 'ic_nova_logo_white.png'))

# Adaptive icon XMLs
anydpi_dir = os.path.join(res_dir, 'mipmap-anydpi-v26')
os.makedirs(anydpi_dir, exist_ok=True)

xml_content = '<?xml version="1.0" encoding="utf-8"?>\n<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n    <background android:drawable="@color/ic_launcher_background" />\n    <foreground android:drawable="@drawable/ic_launcher_foreground" />\n</adaptive-icon>\n'

with open(os.path.join(anydpi_dir, 'ic_launcher.xml'), 'w', encoding='utf-8') as f:
    f.write(xml_content)
with open(os.path.join(anydpi_dir, 'ic_launcher_round.xml'), 'w', encoding='utf-8') as f:
    f.write(xml_content)

print('Successfully generated Android icons!')
