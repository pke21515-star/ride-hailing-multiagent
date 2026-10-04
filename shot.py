# -*- coding: utf-8 -*-
"""作品集视觉验收 v2：console 大屏（含 KPI 实测）+ admin 四页（cookie/localStorage 注入登录）"""
import json
import urllib.request
from playwright.sync_api import sync_playwright

OUT = "C:/Users/33653/Desktop/秋招/网约车多Agent调度系统/"
BASE = "http://127.0.0.1:8871"

# 先在脚本外拿 admin token（与 E2E 同源方式）
req = urllib.request.Request(
    f"{BASE}/api/auth/login",
    data=json.dumps({"phone": "admin", "password": "admin123"}).encode(),
    headers={"Content-Type": "application/json"})
with urllib.request.urlopen(req, timeout=10) as r:
    TOKEN = json.load(r)["token"]
print("admin token OK:", TOKEN[:20], "...")

with sync_playwright() as p:
    b = p.chromium.launch(channel="msedge", headless=True)
    ctx = b.new_context(viewport={"width": 1680, "height": 1050},
                        device_scale_factor=1.25, locale="zh-CN")
    pg = ctx.new_page()

    # 1) 实时调度控制台 + KPI 实测
    pg.goto(f"{BASE}/console.html", wait_until="networkidle", timeout=30000)
    pg.wait_for_timeout(3000)
    idle_in_page = pg.evaluate(
        "fetch('/api/drivers').then(r=>r.json()).then(d=>d.filter(x=>x.idle).length)")
    k3 = pg.inner_text("#k3")
    print(f"console KPI k3 = '{k3}'；页面内 fetch 空闲司机数 = {idle_in_page}")
    pg.screenshot(path=OUT + "shot_console.png", full_page=True)
    print("shot_console.png OK")

    # 2) admin：注入认证态（Cookie authorized-token + multiple-tabs，localStorage user-info）
    pg.goto(f"{BASE}/admin/index.html", wait_until="domcontentloaded", timeout=60000)
    exp = __import__("time").time() * 1000 + 7 * 24 * 3600 * 1000
    pg.evaluate(f"""() => {{
        const t = {json.dumps(TOKEN)};
        const exp = {exp};
        document.cookie = 'authorized-token=' + encodeURIComponent(
            JSON.stringify({{accessToken: t, expires: exp, refreshToken: t}})) + '; path=/';
        document.cookie = 'multiple-tabs=true; path=/';
        localStorage.setItem('user-info', JSON.stringify({{
            refreshToken: t, expires: exp, avatar: '', username: 'admin',
            nickname: '管理员', roles: ['admin'], permissions: ['*:*:*']}}));
    }}""")
    print("auth injected")

    for route, name in [("/ride/dashboard", "shot_admin_dashboard"),
                        ("/ride/orders", "shot_admin_orders"),
                        ("/ride/drivers", "shot_admin_drivers"),
                        ("/ride/blacklist", "shot_admin_blacklist")]:
        pg.goto(f"{BASE}/admin/index.html#{route}", wait_until="networkidle", timeout=30000)
        pg.wait_for_timeout(2800)
        pg.screenshot(path=OUT + name + ".png")
        print(name + ".png OK  url=" + pg.url)

    b.close()
print("ALL SHOTS DONE")
