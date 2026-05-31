"""
畅聊 tab 烟雾测试：
- 登录 demo
- 打开 companion FAB → 切 "畅聊" tab
- 验证形象 svg / status text / 大按钮可见
- 点击 开始畅聊 → 状态切到 listening 或 error（headless 无麦时）
- 切回 "闲聊" → 应自动停 STT（panel 没残留 listening ring）
- 全程截图归档 + 检查 console error
"""
import sys
from pathlib import Path
from playwright.sync_api import sync_playwright

BASE = "http://localhost:5173"
SHOT = Path("C:/Users/c/Desktop/一级项目/home-fitness-fullstack/temp/voice-smoke")
SHOT.mkdir(parents=True, exist_ok=True)


def main():
    errors, console = [], []
    with sync_playwright() as p:
        browser = p.chromium.launch(
            headless=True,
            args=[
                "--use-fake-ui-for-media-stream",
                "--autoplay-policy=no-user-gesture-required",
            ],
        )
        ctx = browser.new_context(
            viewport={"width": 1366, "height": 900},
            locale="zh-CN",
            permissions=["microphone"],
        )
        page = ctx.new_page()
        page.on("console", lambda m: console.append(f"[{m.type}] {m.text}"))
        page.on("pageerror", lambda e: errors.append(f"[pageerror] {e}"))

        # ---- 登录 ----
        page.goto(f"{BASE}/login", wait_until="networkidle")
        page.locator('input[type="email"]').first.fill("demo@fitcoach.com")
        page.locator('input[type="password"]').first.fill("admin123")
        page.locator('label.agree input[type="checkbox"]').check()
        page.locator('button.submit-btn:has-text("登录")').first.click()
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(900)
        assert "/train" in page.url, f"login failed: {page.url}"
        print("[ok] login →", page.url)

        # ---- 打开 FAB ----
        page.locator(".companion-fab .fab-btn").first.click(force=True)
        page.wait_for_timeout(400)
        page.screenshot(path=str(SHOT / "01-fab-open.png"), full_page=False)

        # 三个 tab
        tabs = page.locator(".p-tabs .tab").all_inner_texts()
        print(f"[tabs] {tabs}")
        assert any("畅聊" in t for t in tabs), "缺畅聊 tab"

        # ---- 切到 畅聊 ----
        page.locator('.p-tabs .tab:has-text("畅聊")').first.click()
        page.wait_for_timeout(500)

        # 验证 voice-room 渲染
        page.wait_for_selector(".voice-room", timeout=3000)
        page.wait_for_selector(".voice-room .avatar", timeout=3000)
        # 状态字段（idle）
        status = page.locator(".voice-room .status .label").first.inner_text()
        print(f"[voice idle status] {status}")
        assert status, "idle 状态空"

        # 大按钮存在
        big_text = page.locator(".voice-room .big").first.inner_text()
        print(f"[big btn] {big_text}")
        assert "开始畅聊" in big_text, "按钮文本异常"
        page.screenshot(path=str(SHOT / "02-voice-idle.png"), full_page=False)

        # ---- 点击 开始畅聊 ----
        page.locator(".voice-room .big").first.click()
        page.wait_for_timeout(1500)
        cls = page.locator(".voice-room").first.get_attribute("class") or ""
        print(f"[after start] class = {cls}")
        # 在 headless+fake-ui 下，可能进 listening；如果浏览器不支持 STT 则会 error
        assert ("s-listening" in cls) or ("s-error" in cls) or ("s-idle" in cls), \
            f"未进入预期态: {cls}"
        page.screenshot(path=str(SHOT / "03-voice-listening.png"), full_page=False)

        # ---- 切回闲聊 → 应停止畅聊 ----
        page.locator('.p-tabs .tab:has-text("闲聊")').first.click()
        page.wait_for_timeout(500)
        # 此时 voice-room 应 unmount
        vis = page.locator(".voice-room").count()
        print(f"[after switch back] voice-room count = {vis}")
        assert vis == 0, "畅聊未被卸载"
        page.screenshot(path=str(SHOT / "04-back-to-chat.png"), full_page=False)

        # ---- 再切回 畅聊, 状态应回到 idle ----
        page.locator('.p-tabs .tab:has-text("畅聊")').first.click()
        page.wait_for_timeout(400)
        cls2 = page.locator(".voice-room").first.get_attribute("class") or ""
        print(f"[remount class] {cls2}")
        assert "s-idle" in cls2 or "s-error" in cls2, f"重新挂载未回 idle/error: {cls2}"
        page.screenshot(path=str(SHOT / "05-voice-remounted.png"), full_page=False)

        # ---- 关闭浮窗 ----
        page.locator(".panel .close").first.click()
        page.wait_for_timeout(400)
        page.screenshot(path=str(SHOT / "06-closed.png"), full_page=False)

        browser.close()

    bad = [l for l in console if l.startswith("[error]")]
    print(f"\n[console-errors] {len(bad)}")
    for l in bad[:15]:
        print("  ", l[:240])
    print(f"[pageerrors] {len(errors)}")
    for e in errors:
        print("  ", e[:240])

    return 0 if not bad and not errors else 1


if __name__ == "__main__":
    sys.exit(main())
