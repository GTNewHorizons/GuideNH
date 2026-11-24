from pathlib import Path

from PIL import Image, ImageDraw


NAMES = [
    "save", "capture_all", "crop", "add", "undo", "redo", "settings", "world",
    "snap", "refresh", "record", "stop", "play", "pause", "timer", "close",
    "delete", "folder", "lock", "link", "up", "down", "check", "copy",
]


def glyph(name):
    mask = Image.new("1", (16, 16))
    draw = ImageDraw.Draw(mask)
    if name == "save":
        draw.rectangle((3, 2, 12, 12), outline=1)
        draw.rectangle((5, 2, 10, 5), outline=1)
        draw.rectangle((5, 8, 10, 12), outline=1)
    elif name == "capture_all":
        draw.rectangle((2, 2, 10, 10), outline=1)
        draw.rectangle((5, 5, 13, 13), outline=1)
        draw.rectangle((7, 7, 11, 11), outline=1)
    elif name == "crop":
        draw.line((4, 1, 4, 11, 14, 11), fill=1, width=2)
        draw.line((1, 4, 11, 4, 11, 14), fill=1, width=2)
    elif name == "add":
        draw.rectangle((2, 2, 12, 12), outline=1)
        draw.line((7, 4, 7, 10), fill=1, width=2)
        draw.line((4, 7, 10, 7), fill=1, width=2)
    elif name in ("undo", "redo"):
        draw.line((3, 5, 9, 5, 12, 8, 12, 11, 7, 11), fill=1, width=2)
        draw.polygon([(1, 5), (5, 1), (5, 9)], fill=1)
        if name == "redo":
            mask = mask.transpose(Image.Transpose.FLIP_LEFT_RIGHT)
    elif name == "settings":
        for x, y in ((4, 5), (8, 10), (12, 4)):
            draw.line((x, 2, x, 12), fill=1)
            draw.rectangle((x - 1, y - 1, x + 1, y + 1), outline=1)
    elif name == "world":
        draw.ellipse((2, 1, 13, 12), outline=1)
        draw.ellipse((5, 1, 10, 12), outline=1)
        draw.line((2, 6, 13, 6), fill=1)
        draw.line((4, 3, 11, 3), fill=1)
        draw.line((4, 10, 11, 10), fill=1)
    elif name == "snap":
        draw.rectangle((3, 2, 5, 8), fill=1)
        draw.rectangle((10, 2, 12, 8), fill=1)
        draw.arc((3, 5, 12, 13), 0, 180, fill=1, width=3)
        draw.rectangle((3, 2, 5, 4), fill=0)
        draw.rectangle((10, 2, 12, 4), fill=0)
        draw.line((3, 2, 5, 2), fill=1)
        draw.line((10, 2, 12, 2), fill=1)
    elif name == "refresh":
        draw.arc((2, 2, 12, 12), 30, 315, fill=1, width=2)
        draw.polygon([(13, 1), (13, 6), (8, 3)], fill=1)
    elif name == "record":
        draw.ellipse((3, 2, 12, 11), fill=1)
    elif name == "stop":
        draw.rectangle((3, 2, 12, 11), fill=1)
    elif name == "play":
        draw.polygon([(4, 2), (12, 7), (4, 12)], fill=1)
    elif name == "pause":
        draw.rectangle((4, 2, 6, 12), fill=1)
        draw.rectangle((9, 2, 11, 12), fill=1)
    elif name == "timer":
        draw.ellipse((3, 3, 12, 12), outline=1)
        draw.rectangle((6, 1, 9, 2), fill=1)
        draw.line((8, 5, 8, 8, 11, 8), fill=1)
    elif name == "close":
        draw.line((4, 3, 11, 10), fill=1, width=2)
        draw.line((11, 3, 4, 10), fill=1, width=2)
    elif name == "delete":
        draw.line((3, 4, 12, 4), fill=1)
        draw.rectangle((4, 5, 11, 12), outline=1)
        draw.rectangle((6, 2, 9, 3), outline=1)
        draw.line((6, 7, 6, 10), fill=1)
        draw.line((9, 7, 9, 10), fill=1)
    elif name == "folder":
        draw.line((2, 12, 2, 3, 6, 3, 8, 5, 13, 5, 13, 12, 2, 12), fill=1)
        draw.line((3, 7, 12, 7), fill=1)
    elif name == "lock":
        draw.arc((5, 1, 10, 8), 180, 360, fill=1, width=2)
        draw.rectangle((3, 6, 12, 12), outline=1)
        draw.line((8, 8, 8, 10), fill=1)
    elif name == "link":
        draw.rounded_rectangle((2, 2, 9, 8), radius=2, outline=1)
        draw.rounded_rectangle((6, 6, 13, 12), radius=2, outline=1)
        draw.line((6, 6, 9, 8), fill=1, width=2)
    elif name in ("up", "down"):
        draw.line((3, 9, 7, 5, 11, 9), fill=1, width=2)
        if name == "down":
            mask = mask.transpose(Image.Transpose.FLIP_TOP_BOTTOM)
    elif name == "check":
        draw.line((2, 7, 6, 11, 13, 3), fill=1, width=2)
    elif name == "copy":
        draw.rectangle((2, 2, 9, 9), outline=1)
        draw.rectangle((5, 5, 12, 12), outline=1)
    return mask


def main():
    atlas = Image.new("RGBA", (256, 256))
    for index, name in enumerate(NAMES):
        mask = glyph(name)
        icon = Image.new("RGBA", (16, 16))
        shadow = Image.new("1", (16, 16))
        shadow.paste(mask, (0, 1))
        icon.paste((0, 0, 0, 255), (0, 0), shadow)
        icon.paste((255, 255, 255, 255), (0, 0), mask)
        atlas.paste(icon, ((index % 16) * 16, (index // 16) * 16))
    target = Path(__file__).resolve().parents[2] / "src/main/resources/assets/guidenh/textures/capture/icons.png"
    target.parent.mkdir(parents=True, exist_ok=True)
    atlas.save(target, format="PNG")
    print(target)


if __name__ == "__main__":
    main()
