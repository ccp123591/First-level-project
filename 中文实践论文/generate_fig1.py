from pathlib import Path
from textwrap import wrap
import math

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parent.parent
OUT_EN = ROOT / "fitcoach-fig1-fixed.png"
OUT_ZH = Path(__file__).resolve().parent / "\u56fe1-\u8001\u5e74\u5c45\u5bb6\u5065\u8eab\u573a\u666f\u4e0b\u7684\u7cfb\u7edf\u9700\u6c42\u793a\u610f\u56fe.png"

W, H = 1800, 980


def load_font(name: str, size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(str(Path(r"C:\Windows\Fonts") / name), size)


FONT_TITLE = load_font("msyh.ttc", 42)
FONT_SUB = load_font("msyh.ttc", 24)
FONT_BOX_TITLE = load_font("msyh.ttc", 28)
FONT_BOX = load_font("msyh.ttc", 22)
FONT_CENTER = load_font("msyh.ttc", 30)
FONT_CENTER_SMALL = load_font("msyh.ttc", 22)

NAVY = "#1f3a5f"
TEAL = "#2d7d7a"
GOLD = "#c6922b"
SOFT_BLUE = "#eaf2fb"
SOFT_GREEN = "#eaf7f2"
SOFT_ORANGE = "#fff5e8"
SOFT_RED = "#fbeeee"
SOFT_GRAY = "#f5f7fa"
LINE = "#8aa3b8"
TEXT = "#25313d"
MUTED = "#5b6773"
WHITE = "#ffffff"
RED_OUTLINE = "#a85656"


ZH_TITLE = "\u56fe1 \u8001\u5e74\u5c45\u5bb6\u5065\u8eab\u573a\u666f\u4e0b\u7684\u7cfb\u7edf\u9700\u6c42\u793a\u610f\u56fe"
EN_TITLE = "Fig.1 Requirement Diagram for Elderly Home Fitness Scenario"
CENTER_TITLE = "\u5c45\u5bb6\u8001\u5e74\u7528\u6237\u8bad\u7ec3\u573a\u666f"
CENTER_SUB = "FitCoach \u5b9e\u9645\u4e3b\u94fe\u8def"

STEPS = [
    ("1", "\u6253\u5f00\u6d4f\u89c8\u5668\u8fdb\u5165\u8bad\u7ec3\u9875"),
    ("2", "\u666e\u901a\u6444\u50cf\u5934\u91c7\u96c6\u4eba\u4f53\u52a8\u4f5c"),
    ("3", "\u6d4f\u89c8\u5668\u7aef MediaPipe Pose \u8bc6\u522b\u5173\u952e\u70b9"),
    ("4", "\u672c\u5730\u5b8c\u6210\u8ba1\u6570\u3001\u8bc4\u5206\u4e0e\u8bed\u97f3\u63d0\u793a"),
    ("5", "\u8bad\u7ec3\u8bb0\u5f55\u5148\u4fdd\u5b58\u5230 IndexedDB"),
    ("6", "\u767b\u5f55\u72b6\u6001\u4e0b\u53ef\u5c1d\u8bd5\u4e0a\u4f20\u540e\u7aef"),
]


def round_box(draw: ImageDraw.ImageDraw, xy, radius, fill, outline, width=3):
    draw.rounded_rectangle(xy, radius=radius, fill=fill, outline=outline, width=width)


def centered_text(draw: ImageDraw.ImageDraw, xy, txt: str, font, fill=TEXT):
    x1, y1, x2, y2 = draw.textbbox((0, 0), txt, font=font)
    x = xy[0] - (x2 - x1) / 2
    y = xy[1] - (y2 - y1) / 2
    draw.text((x, y), txt, font=font, fill=fill)


def multiline_in_box(draw: ImageDraw.ImageDraw, box, title, bullets, fill, outline):
    x1, y1, x2, y2 = box
    round_box(draw, box, 28, fill, outline)
    draw.text((x1 + 24, y1 + 18), title, font=FONT_BOX_TITLE, fill=outline)
    y = y1 + 68
    for bullet in bullets:
        lines = wrap(bullet, width=15) or [""]
        draw.text((x1 + 28, y), "\u2022", font=FONT_BOX, fill=outline)
        yy = y
        for line in lines:
            draw.text((x1 + 58, yy), line, font=FONT_BOX, fill=TEXT)
            yy += 30
        y = yy + 10


def arrow(draw: ImageDraw.ImageDraw, p1, p2, color=LINE, width=5, head=16):
    draw.line([p1, p2], fill=color, width=width)
    ang = math.atan2(p2[1] - p1[1], p2[0] - p1[0])
    a1 = ang + math.pi * 0.85
    a2 = ang - math.pi * 0.85
    p3 = (p2[0] + head * math.cos(a1), p2[1] + head * math.sin(a1))
    p4 = (p2[0] + head * math.cos(a2), p2[1] + head * math.sin(a2))
    draw.polygon([p2, p3, p4], fill=color)


def build():
    img = Image.new("RGB", (W, H), "#ffffff")
    draw = ImageDraw.Draw(img)

    centered_text(draw, (W / 2, 48), ZH_TITLE, FONT_TITLE, fill=NAVY)
    centered_text(draw, (W / 2, 98), EN_TITLE, FONT_SUB, fill=MUTED)

    center_box = (550, 285, 1250, 845)
    round_box(draw, center_box, 40, SOFT_GRAY, NAVY, width=4)
    centered_text(draw, (900, 345), CENTER_TITLE, FONT_CENTER, fill=NAVY)
    centered_text(draw, (900, 390), CENTER_SUB, FONT_CENTER_SMALL, fill=TEAL)

    step_y = 445
    step_gap = 58
    for num, label in STEPS:
        draw.ellipse((650, step_y - 8, 694, step_y + 36), fill=TEAL, outline=TEAL)
        centered_text(draw, (672, step_y + 14), num, FONT_BOX, fill=WHITE)
        draw.text((720, step_y), label, font=FONT_BOX, fill=TEXT)
        step_y += step_gap

    left_top = (70, 180, 470, 470)
    left_bottom = (70, 560, 470, 900)
    right_top = (1330, 180, 1730, 470)
    right_bottom = (1330, 560, 1730, 900)

    multiline_in_box(
        draw,
        left_top,
        "\u9002\u8001\u5316\u4f7f\u7528\u9700\u6c42",
        [
            "\u5c3d\u91cf\u5c11\u5b89\u88c5\u3001\u5c11\u914d\u7f6e",
            "\u754c\u9762\u76f4\u89c2\uff0c\u964d\u4f4e\u8ba4\u77e5\u8d1f\u62c5",
            "\u8bed\u97f3\u63d0\u793a\u5e2e\u52a9\u7406\u89e3\u52a8\u4f5c",
            "\u5bb6\u5ead\u73af\u5883\u4e0b\u53ef\u72ec\u7acb\u5b8c\u6210\u8bad\u7ec3",
        ],
        SOFT_BLUE,
        NAVY,
    )

    multiline_in_box(
        draw,
        right_top,
        "\u8bad\u7ec3\u5b89\u5168\u4e0e\u53cd\u9988\u9700\u6c42",
        [
            "\u52a8\u4f5c\u8fc7\u7a0b\u9700\u8981\u5b9e\u65f6\u8bc6\u522b",
            "\u8ba1\u6570\u3001\u8282\u594f\u548c\u7ea0\u9519\u53cd\u9988\u6e05\u6670",
            "\u672a\u68c0\u6d4b\u5230\u4eba\u4f53\u65f6\u53ef\u81ea\u52a8\u6682\u505c",
            "\u7ed3\u679c\u8bc4\u5206\u5e94\u5c3d\u91cf\u53ef\u89e3\u91ca",
        ],
        SOFT_GREEN,
        TEAL,
    )

    multiline_in_box(
        draw,
        left_bottom,
        "\u9879\u76ee\u5df2\u5b9e\u73b0\u7684\u6838\u5fc3\u652f\u6491",
        [
            "MediaPipe Pose \u6d4f\u89c8\u5668\u7aef\u59ff\u6001\u8bc6\u522b",
            "\u52a8\u4f5c\u72b6\u6001\u673a\u8ba1\u6570\u4e0e\u591a\u7ef4\u8bc4\u5206",
            "TTS \u8bed\u97f3\u64ad\u62a5\u4e0e\u8282\u62cd\u5668",
            "\u8bad\u7ec3\u62a5\u544a\u3001\u5386\u53f2\u8bb0\u5f55\u4e0e CSV \u5bfc\u51fa",
        ],
        SOFT_ORANGE,
        GOLD,
    )

    multiline_in_box(
        draw,
        right_bottom,
        "\u5f31\u7f51\u4e0e\u6570\u636e\u4f7f\u7528\u9700\u6c42",
        [
            "\u8bad\u7ec3\u5b8c\u6210\u540e\u4f18\u5148\u672c\u5730\u4fdd\u5b58",
            "\u5f31\u7f51\u6216\u79bb\u7ebf\u65f6\u8bb0\u5f55\u4e0d\u4e22\u5931",
            "PWA \u652f\u6301\u5b89\u88c5\u4e0e\u79bb\u7ebf\u6253\u5f00",
            "\u540e\u7aef\u63d0\u4f9b\u8d26\u53f7\u3001\u8bb0\u5f55\u7b49\u57fa\u7840\u63a5\u53e3",
        ],
        SOFT_RED,
        RED_OUTLINE,
    )

    arrow(draw, (470, 325), (580, 410))
    arrow(draw, (1330, 325), (1220, 410))
    arrow(draw, (470, 720), (580, 655))
    arrow(draw, (1330, 720), (1220, 655))

    img.save(OUT_EN, quality=95)
    img.save(OUT_ZH, quality=95)


if __name__ == "__main__":
    build()
