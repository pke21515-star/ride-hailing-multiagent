# -*- coding: utf-8 -*-
"""真实 UI 登录验证：admin/admin123 走表单 → 应成功跳转看板"""
from playwright.sync_api import sync_playwright

OUT = "C:/Users/33653/Desktop/秋招/网约车多Agent调度系统/"
BASE = "http://127.0.0.1:8871"

with sync_playwright() as p:
    b = p.chromium.launch(channel="msedge", headless=True)
    ctx = b.new_context(viewport={"width": 1680, "height": 1050},
                        device_scale_factor=1.25, locale="zh-CN")
    pg = ctx.new_page()
    logs = []
    pg.on("console", lambda m: logs.append(f"[{m.type}] {m.text[:150]}"))
    pg.on("response", lambda r: logs.append(f"[http] {r.status} {r.url[-40:]}") if "/api/" in r.url else None)

    pg.goto(f"{BASE}/admin/index.html", wait_until="networkidle", timeout=60000)
    pg.wait_for_timeout(2000)
    pg.screenshot(path=OUT + "shot_admin_login.png")
    print("login page url:", pg.url)

    # 表单填写（可见输入框）
    filled = {"user": False, "pwd": False}
    for el in pg.query_selector_all("input"):
        try:
            if not el.is_visible():
                continue
        except Exception:
            continue
        t = (el.get_attribute("type") or "text").lower()
        if t == "password" and not filled["pwd"]:
            el.fill("admin123"); filled["pwd"] = True
        elif t != "password" and not filled["user"]:
            el.fill("admin"); filled["user"] = True
    print("filled:", filled)

    # 点登录按钮
    clicked = False
    for btn in pg.query_selector_all("button"):
        try:
            if "登录" in (btn.inner_text() or "") and btn.is_visible():
                btn.click(); clicked = True; break
        except Exception:
            pass
    print("clicked login button:", clicked)

    pg.wait_for_timeout(5000)
    print("url after login:", pg.url)
    ok = "/login" not in pg.url
    print("UI LOGIN:", "SUCCESS ✅" if ok else "FAILED ❌")
    pg.screenshot(path=OUT + "shot_admin_login_result.png")
    print("--- 相关日志 ---")
    for l in logs[-15:]:
        print(l)
    b.close()
