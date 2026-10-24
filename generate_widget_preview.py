"""
Generate a high-resolution PNG widget thumbnail for the Music Orbit homescreen widget.
Output: androidApp/src/main/res/drawable-nodpi/widget_preview_static.png
Canvas: 960 x 330 px (3x, represents 320x110dp)
"""

from PIL import Image, ImageDraw
import os

# ─── Colours ────────────────────────────────────────────────────────────────
BG_COLOR      = (18,  16,  31, 255)   # #12101F
ART_BG        = (42,  36,  64, 255)   # #2A2440
CTRL_BG       = (42,  36,  64, 255)   # #2A2440
PLAY_BG       = (58,  51,  88, 255)   # #3A3358
TEXT_WHITE    = (255, 255, 255, 230)  # title bar  ~#E6FFFFFF
TEXT_ARTIST   = (184, 181, 201, 128)  # artist bar  #80B8B5C9
ICON_COLOR    = (123, 120, 176, 255)  # music icon on art bg  #7B78B0
WHITE         = (255, 255, 255, 255)

W, H = 960, 330

# ─── Helper: rounded rectangle ───────────────────────────────────────────────
def rounded_rect(draw, bbox, radius, fill):
    x0, y0, x1, y1 = bbox
    draw.rounded_rectangle([x0, y0, x1, y1], radius=radius, fill=fill)

# ─── Helper: draw left-skip icon (|◀) ────────────────────────────────────────
def draw_prev_icon(draw, cx, cy, size=28):
    bw = 5
    # vertical bar (left edge)
    draw.rectangle([cx - size // 2, cy - size // 2,
                    cx - size // 2 + bw, cy + size // 2], fill=WHITE)
    # left-pointing triangle
    tx = cx - size // 2 + bw + 4
    draw.polygon([
        (cx + size // 2 - 2, cy - size // 2),
        (cx + size // 2 - 2, cy + size // 2),
        (tx, cy),
    ], fill=WHITE)

# ─── Helper: draw play icon (▶) ─────────────────────────────────────────────
def draw_play_icon(draw, cx, cy, size=32):
    draw.polygon([
        (cx - size // 2 + 4, cy - size // 2),
        (cx - size // 2 + 4, cy + size // 2),
        (cx + size // 2 + 4, cy),
    ], fill=WHITE)

# ─── Helper: draw next-skip icon (▶|) ────────────────────────────────────────
def draw_next_icon(draw, cx, cy, size=28):
    tx = cx + size // 2 - 4 - 5
    # right-pointing triangle
    draw.polygon([
        (cx - size // 2 + 2, cy - size // 2),
        (cx - size // 2 + 2, cy + size // 2),
        (tx, cy),
    ], fill=WHITE)
    bw = 5
    # vertical bar (right edge)
    draw.rectangle([tx + 4, cy - size // 2,
                    tx + 4 + bw, cy + size // 2], fill=WHITE)

# ─── Helper: draw simple music note ─────────────────────────────────────────
def draw_music_icon(draw, cx, cy):
    stem_x  = cx + 10
    note_y  = cy + 14
    head_w, head_h = 22, 16

    # Stem
    draw.rectangle([stem_x - 3, cy - 22, stem_x + 3, note_y], fill=ICON_COLOR)
    # Beam (two horizontal lines from stem top)
    draw.rectangle([stem_x - 3, cy - 22, stem_x + 30, cy - 16], fill=ICON_COLOR)
    draw.rectangle([stem_x - 3, cy - 10, stem_x + 30, cy - 4],  fill=ICON_COLOR)
    # First note head
    draw.ellipse([stem_x - 3 - head_w // 2, note_y - head_h // 2 + 4,
                  stem_x - 3 + head_w // 2, note_y + head_h // 2 + 4], fill=ICON_COLOR)
    # Second note head
    draw.ellipse([stem_x + 30 - head_w // 2, cy - 4 - head_h // 2 + 4,
                  stem_x + 30 + head_w // 2, cy - 4 + head_h // 2 + 4], fill=ICON_COLOR)

# ─── Main render ─────────────────────────────────────────────────────────────
def render():
    img  = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Card background
    rounded_rect(draw, (0, 0, W, H), radius=60, fill=BG_COLOR)

    # Album art placeholder box
    art_x0, art_y0, art_x1, art_y1 = 42, 87, 207, 243
    rounded_rect(draw, (art_x0, art_y0, art_x1, art_y1), radius=36, fill=ART_BG)
    art_cx = (art_x0 + art_x1) // 2
    art_cy = (art_y0 + art_y1) // 2
    draw_music_icon(draw, art_cx - 16, art_cy - 4)

    # Song title bar (white rounded rect)
    rounded_rect(draw, (234, 118, 564, 150), radius=8, fill=TEXT_WHITE)

    # Artist bar (semi-transparent grey rounded rect)
    rounded_rect(draw, (234, 168, 434, 192), radius=6, fill=TEXT_ARTIST)

    # Transport buttons
    btn_y = H // 2
    positions = [
        (636, btn_y, 51, CTRL_BG, "prev"),
        (771, btn_y, 63, PLAY_BG, "play"),
        (906, btn_y, 51, CTRL_BG, "next"),
    ]
    for cx, cy, r, col, kind in positions:
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=col)
        if kind == "prev":
            draw_prev_icon(draw, cx, cy, size=28)
        elif kind == "play":
            draw_play_icon(draw, cx, cy, size=32)
        else:
            draw_next_icon(draw, cx, cy, size=28)

    return img

# ─── Save ─────────────────────────────────────────────────────────────────────
out_dir  = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                        "androidApp", "src", "main", "res", "drawable-nodpi")
os.makedirs(out_dir, exist_ok=True)
out_path = os.path.join(out_dir, "widget_preview_static.png")

img = render()
img.save(out_path, "PNG", optimize=True)
print(f"Saved widget thumbnail -> {out_path}")
print(f"Size: {img.width}x{img.height} px  ({img.width//3}x{img.height//3} dp @ 3x)")
