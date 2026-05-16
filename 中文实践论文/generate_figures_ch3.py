# -*- coding: utf-8 -*-
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont


BASE = Path(__file__).resolve().parent
FONT = r"C:\Windows\Fonts\msyh.ttc"


def f(size):
    return ImageFont.truetype(FONT, size)


FONT_TITLE = f(40)
FONT_SUB = f(24)
FONT_H = f(28)
FONT_B = f(22)
FONT_S = f(20)

NAVY = "#1f3a5f"
TEAL = "#2f7f7d"
GOLD = "#b8852b"
RED = "#b55a5a"
LINE = "#8aa3b8"
TEXT = "#25313d"
MUTED = "#5b6773"
BG1 = "#eef4fb"
BG2 = "#edf8f5"
BG3 = "#fff5e8"
BG4 = "#f9efef"
BG = "#ffffff"


def center_text(draw, xy, text, font, fill=TEXT):
    x1, y1, x2, y2 = draw.textbbox((0, 0), text, font=font)
    draw.text((xy[0] - (x2 - x1) / 2, xy[1] - (y2 - y1) / 2), text, font=font, fill=fill)


def box(draw, rect, title, lines, fill, outline):
    x1, y1, x2, y2 = rect
    draw.rounded_rectangle(rect, radius=28, fill=fill, outline=outline, width=3)
    draw.text((x1 + 24, y1 + 18), title, font=FONT_H, fill=outline)
    y = y1 + 64
    for line in lines:
        draw.text((x1 + 28, y), "•", font=FONT_B, fill=outline)
        draw.text((x1 + 58, y), line, font=FONT_B, fill=TEXT)
        y += 27


def arrow(draw, p1, p2, color=LINE, width=5, head=16):
    draw.line([p1, p2], fill=color, width=width)
    import math
    ang = math.atan2(p2[1] - p1[1], p2[0] - p1[0])
    a1 = ang + math.pi * 0.88
    a2 = ang - math.pi * 0.88
    p3 = (p2[0] + head * math.cos(a1), p2[1] + head * math.sin(a1))
    p4 = (p2[0] + head * math.cos(a2), p2[1] + head * math.sin(a2))
    draw.polygon([p2, p3, p4], fill=color)


def generate_fig2():
    img = Image.new("RGB", (1800, 1100), BG)
    draw = ImageDraw.Draw(img)

    center_text(draw, (900, 45), "图2 FitCoach一级项目总体架构图", FONT_TITLE, NAVY)
    center_text(draw, (900, 92), "Fig. 2 Overall Architecture of the FitCoach Project", FONT_SUB, MUTED)

    user_rect = (690, 130, 1110, 230)
    draw.rounded_rectangle(user_rect, radius=24, fill="#f6f8fb", outline=NAVY, width=3)
    center_text(draw, (900, 165), "居家老年用户", FONT_H, NAVY)
    center_text(draw, (900, 205), "通过浏览器或桌面快捷入口使用系统", FONT_S, TEXT)

    front_rect = (90, 300, 760, 570)
    back_rect = (1040, 300, 1710, 570)
    data_rect = (360, 660, 1440, 850)

    box(draw, front_rect, "前端训练层（Vue 3 + PWA）", [
        "训练页、记录页、设置页等页面交互",
        "MediaPipe Pose 关键点识别",
        "动作判定、计数、多维评分",
        "语音提示、训练报告、CSV 导出",
    ], BG1, NAVY)

    box(draw, back_rect, "后端服务层（Spring Boot）", [
        "用户认证与基本资料管理",
        "训练记录查询、批量同步、导出接口",
        "计划、排行榜、动态等扩展业务模块",
        "统一响应结构与权限控制",
    ], BG2, TEAL)

    box(draw, data_rect, "数据存储层", [
        "浏览器本地：IndexedDB 保存训练记录，LocalStorage 保存配置项",
        "服务端：数据库保存用户、训练记录和扩展业务数据",
    ], BG3, GOLD)

    arrow(draw, (900, 230), (425, 300))
    arrow(draw, (900, 230), (1375, 300))
    arrow(draw, (760, 435), (1040, 435))
    arrow(draw, (425, 570), (560, 660))
    arrow(draw, (1375, 570), (1240, 660))

    out = BASE / "图2-FitCoach一级项目总体架构图.png"
    img.save(out, quality=95)


def generate_fig3():
    img = Image.new("RGB", (1800, 1320), BG)
    draw = ImageDraw.Draw(img)

    center_text(draw, (900, 45), "图3 训练主链路流程图", FONT_TITLE, NAVY)
    center_text(draw, (900, 92), "Fig. 3 Main Training Workflow", FONT_SUB, MUTED)

    steps = [
        ("用户选择动作与目标次数", BG1, NAVY),
        ("前端初始化姿态识别模型", BG1, NAVY),
        ("摄像头采集视频并提取关键点", BG1, NAVY),
        ("动作判定与计数逻辑实时运行", BG2, TEAL),
        ("输出语音提示与多维评分结果", BG2, TEAL),
        ("训练结束后生成训练报告", BG3, GOLD),
        ("训练记录优先写入本地 IndexedDB", BG3, GOLD),
        ("登录状态下可尝试上传后端", BG4, RED),
    ]

    y = 170
    rects = []
    for label, fill, outline in steps:
        rect = (470, y, 1330, y + 90)
        rects.append(rect)
        draw.rounded_rectangle(rect, radius=24, fill=fill, outline=outline, width=3)
        center_text(draw, (900, y + 45), label, FONT_H if "动作" in label else FONT_B, TEXT)
        y += 130

    for i in range(len(rects) - 1):
        r1 = rects[i]
        r2 = rects[i + 1]
        arrow(draw, (900, r1[3]), (900, r2[1]))

    note = (520, 1210, 1280, 1280)
    draw.rounded_rectangle(note, radius=20, fill="#f6f8fb", outline=LINE, width=2)
    center_text(draw, (900, 1245), "说明：当前系统强调“本地优先保存”，自动补传机制仍待进一步完善。", FONT_S, MUTED)

    out = BASE / "图3-训练主链路流程图.png"
    img.save(out, quality=95)


if __name__ == "__main__":
    generate_fig2()
    generate_fig3()
