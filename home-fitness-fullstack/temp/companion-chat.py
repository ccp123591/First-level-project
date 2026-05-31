"""
Companion chat end-to-end:
- 登录 demo
- 打开浮窗，默认进入「闲聊」tab
- 连续 3 轮对话（最后一轮探测记忆唤起）
- 截图归档 + 校验 reply 出现 + 没有 console error
"""
import sys
from pathlib import Path
from playwright.sync_api import sync_playwright

BASE = "http://localhost:5173"
SHOT = Path("C:/Users/c/Desktop/一级项目/home-fitness-fullstack/temp/companion-chat")
SHOT.mkdir(parents=True, exist_ok=True)

TURNS = [
    "嗨，我今天工作压力好大",
    "那我先深呼吸一下",
    "你还记得我刚才说什么吗"
]

def main():
    errors, console = [], []
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        ctx = browser.new_context(viewport={"width": 1280, "height": 900}, locale="zh-CN")
        page = ctx.new_page()
        page.on("console", lambda m: console.append(f"[{m.type}] {m.text}"))
        page.on("pageerror", lambda e: errors.append(f"[pageerror] {e}"))

        # Login
        page.goto(f"{BASE}/login", wait_until="networkidle")
        page.locator('input[type="email"]').first.fill("demo@fitcoach.com")
        page.locator('input[type="password"]').first.fill("admin123")
        page.locator('label.agree input[type="checkbox"]').check()
        page.locator('button.submit-btn:has-text("登录")').first.click()
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(1200)
        assert "/train" in page.url, f"login failed, url={page.url}"

        # Open companion
        page.locator('.companion-fab .fab-btn').first.click(force=True)
        page.wait_for_timeout(600)

        # 默认应在 chat tab
        chat_tab_active = page.locator('.p-tabs .tab.active').first.inner_text()
        print(f"[default tab] {chat_tab_active}")
        assert "闲聊" in chat_tab_active, "default tab not 闲聊"

        # 默认应有一条 assistant 开场白
        first_msgs = page.locator('.msg-list .msg').count()
        print(f"[opener] msg count = {first_msgs}")

        page.screenshot(path=str(SHOT / "00-open.png"), full_page=True)

        # 3 轮对话
        for i, txt in enumerate(TURNS, 1):
            print(f"\n=== Turn {i}: {txt} ===")
            old_assist = page.locator('.msg-list .msg.assistant:not(.typing)').count()
            ta = page.locator('.composer textarea').first
            ta.click()
            ta.fill("")
            ta.type(txt, delay=15)
            page.wait_for_timeout(100)
            # Enter 触发 onEnter
            ta.press('Enter')
            # 等待新的 assistant (非 typing) 出现
            try:
                page.wait_for_function(
                    f"() => document.querySelectorAll('.msg-list .msg.assistant:not(.typing)').length > {old_assist}",
                    timeout=45000
                )
            except Exception as e:
                page.screenshot(path=str(SHOT / f"0{i}-FAIL.png"), full_page=True)
                errors.append(f"turn{i} no reply: {e}")
                break
            page.wait_for_timeout(500)
            last = page.locator('.msg-list .msg.assistant:not(.typing)').last
            reply = last.locator('.text').inner_text()
            recalled_count = last.locator('.recalled li').count()
            print(f"[reply] {reply[:200]}")
            print(f"[recalled count] {recalled_count}")
            page.screenshot(path=str(SHOT / f"0{i}-turn{i}.png"), full_page=True)

        # 检查清空
        page.locator('button:has-text("清空这次对话")').first.click(force=True)
        page.wait_for_timeout(300)
        cleared = page.locator('.msg-list .msg').count()
        print(f"\n[after clear] msg count = {cleared}")

        page.screenshot(path=str(SHOT / "99-cleared.png"), full_page=True)
        browser.close()

    bad = [l for l in console if l.startswith('[error]')]
    print(f"\n[console-errors] {len(bad)}")
    for l in bad[:8]: print(' ', l[:200])
    print(f"[pageerrors] {len(errors)}")
    for e in errors: print(' ', e[:200])
    return 0 if not bad and not errors else 1

if __name__ == "__main__":
    sys.exit(main())
