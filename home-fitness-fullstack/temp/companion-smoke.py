"""
Companion UI smoke test:
- 登录 demo 用户
- /train 页面应出现陪伴浮窗 fab
- 点 fab 展开面板，看到问候 + 建议内容
- /records 页面浮窗仍在
- 跳转 /settings 看到「陪伴 Agent」配置卡
- 关掉「右下角浮动陪伴」开关后浮窗消失
"""
import sys
import json
from pathlib import Path
from playwright.sync_api import sync_playwright

BASE = "http://localhost:5173"
SHOT_DIR = Path("C:/Users/c/Desktop/一级项目/home-fitness-fullstack/temp/companion-smoke")
SHOT_DIR.mkdir(parents=True, exist_ok=True)

def main():
    errors = []
    console_logs = []

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        ctx = browser.new_context(viewport={"width": 1280, "height": 800}, locale="zh-CN")
        page = ctx.new_page()

        page.on("console", lambda msg: console_logs.append(f"[{msg.type}] {msg.text}"))
        page.on("pageerror", lambda exc: errors.append(f"[pageerror] {exc}"))

        # === 登录 ===
        page.goto(f"{BASE}/login", wait_until="networkidle")
        page.screenshot(path=str(SHOT_DIR / "01-login.png"), full_page=True)

        # 填邮箱/密码
        # 先快照一份内容看选择器
        Path(SHOT_DIR / "_login_html.txt").write_text(page.content()[:3000], encoding="utf-8")

        # 尝试常见选择器
        email_inp = page.locator('input[type="email"], input[placeholder*="邮箱"]').first
        if email_inp.count() == 0:
            email_inp = page.locator('input').nth(0)
        email_inp.fill("demo@fitcoach.com")

        pw_inp = page.locator('input[type="password"]').first
        pw_inp.fill("admin123")

        # 必须勾上「同意协议」否则按钮 disabled
        agree_label = page.locator('label.agree').first
        if agree_label.count():
            agree_label.locator('input[type="checkbox"]').check()

        # 找登录按钮
        login_btn = page.locator('button.submit-btn:has-text("登录")').first
        if login_btn.count() == 0:
            login_btn = page.locator('button[type="submit"]').first
        login_btn.click()

        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(1200)
        url_after_login = page.url
        print(f"[after login] url={url_after_login}")

        # === Train 页 浮窗存在 ===
        if "/train" not in url_after_login:
            page.goto(f"{BASE}/train", wait_until="networkidle")
            page.wait_for_timeout(500)
        page.screenshot(path=str(SHOT_DIR / "02-train.png"), full_page=True)

        fab = page.locator('.companion-fab .fab-btn').first
        fab_visible = fab.count() > 0 and fab.is_visible()
        print(f"[train] companion fab visible={fab_visible}")
        if not fab_visible:
            errors.append("companion fab NOT visible on /train after login")

        # === 点击展开 ===
        if fab_visible:
            fab.click(force=True)
            page.wait_for_timeout(800)
            page.screenshot(path=str(SHOT_DIR / "03-companion-open.png"), full_page=True)

            panel = page.locator('.companion-fab .panel').first
            panel_visible = panel.count() > 0 and panel.is_visible()
            print(f"[train] panel visible after click={panel_visible}")
            if not panel_visible:
                errors.append("panel did not appear after fab click")
            else:
                # 等加载完成
                page.wait_for_timeout(2500)
                page.screenshot(path=str(SHOT_DIR / "04-companion-loaded.png"), full_page=True)

                greet_text = panel.locator('.greet').first.inner_text() if panel.locator('.greet').count() else ""
                body_text = panel.locator('.body').first.inner_text() if panel.locator('.body').count() else ""
                loading_text = panel.locator('.loading').first.inner_text() if panel.locator('.loading').count() else ""
                print(f"[panel] greet={greet_text!r}")
                print(f"[panel] body={body_text[:140]!r}")
                print(f"[panel] loading={loading_text!r}")
                if not greet_text and not body_text:
                    errors.append("panel rendered but greet/body both empty")

                # 收起
                page.locator('.companion-fab .close').first.click(force=True)
                page.wait_for_timeout(400)

        # === 切到 /records，浮窗仍在 ===
        page.goto(f"{BASE}/records", wait_until="networkidle")
        page.wait_for_timeout(500)
        page.screenshot(path=str(SHOT_DIR / "05-records.png"), full_page=True)
        fab2 = page.locator('.companion-fab .fab-btn').first
        print(f"[records] fab visible={fab2.count() > 0 and fab2.is_visible()}")

        # === 设置页：找到「陪伴 Agent」卡 ===
        page.goto(f"{BASE}/settings", wait_until="networkidle")
        page.wait_for_timeout(500)
        page.screenshot(path=str(SHOT_DIR / "06-settings.png"), full_page=True)
        has_companion_card = page.locator('text="陪伴 Agent"').count() > 0
        has_auto_speak = page.locator('text="反馈到达后自动播报"').count() > 0
        print(f"[settings] 陪伴Agent卡={has_companion_card}  自动播报开关={has_auto_speak}")
        if not (has_companion_card and has_auto_speak):
            errors.append("settings page missing companion card/toggles")

        # === 关闭浮窗开关 ===
        cb_label = page.locator('label:has-text("右下角浮动陪伴")').first
        if cb_label.count():
            cb_label.locator('input[type="checkbox"]').click()
            page.locator('button:has-text("保存设置")').click()
            page.wait_for_timeout(500)
            page.goto(f"{BASE}/train", wait_until="networkidle")
            page.wait_for_timeout(500)
            page.screenshot(path=str(SHOT_DIR / "07-after-disable.png"), full_page=True)
            fab3 = page.locator('.companion-fab .fab-btn').first
            fab_off = fab3.count() == 0 or not fab3.is_visible()
            print(f"[disabled] fab hidden={fab_off}")
            if not fab_off:
                errors.append("companion fab still visible after disable")

        browser.close()

    # console errors
    bad = [l for l in console_logs if l.startswith("[error]")]
    print(f"\n[console-errors] count={len(bad)}")
    for l in bad[:10]:
        print(" ", l[:200])
    print(f"\n[pageerrors] count={len(errors)}")
    for e in errors:
        print(" ", e[:200])

    return 0 if not errors and not bad else 1

if __name__ == "__main__":
    sys.exit(main())
