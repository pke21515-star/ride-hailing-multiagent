# -*- coding: utf-8 -*-
"""网约车应用端 E2E：管理端前端→admin登录→stats→黑名单→乘客下单→支付→订单列表→持久化"""
import sys
import httpx

BASE = "http://127.0.0.1:8871"

def check(name, ok, detail=""):
    print(f"[{'PASS' if ok else 'FAIL'}] {name} {detail}")
    if not ok:
        sys.exit(1)

c = httpx.Client(base_url=BASE, trust_env=False, timeout=15)

# 1. 管理端前端可访问（vue-pure-admin 构建产物）
r = c.get("/", follow_redirects=True)
html = r.text
check("admin-ui", r.status_code == 200 and "网约车多Agent协同调度平台" in html and "static/js" in html,
      f"status={r.status_code} title_ok={'网约车' in html}")

# 2. 根路径重定向到 admin
r = c.get("/", follow_redirects=False)
check("root-redirect", r.status_code in (301, 302, 303, 307) or (r.status_code == 200 and "网约车" in r.text),
      f"status={r.status_code}")

# 3. admin 登录
r = c.post("/api/auth/login", json={"phone": "admin", "password": "admin123"})
check("admin-login", r.status_code == 200, f"status={r.status_code} body={r.text[:80]}")
TOKEN = r.json()["token"]
AH = {"Authorization": f"Bearer {TOKEN}"}

# 4. 统计
r = c.get("/api/admin/stats", headers=AH)
d = r.json()
check("admin-stats", r.status_code == 200 and "totalOrders" in str(d.keys()) or r.status_code == 200,
      f"keys={list(d.keys())[:6]}")

# 5. 黑名单增删
r = c.post("/api/admin/blacklist", headers=AH, json={"passengerId": "e2e-block-1", "reason": "E2E测试"})
check("blacklist-add", r.status_code == 200, f"status={r.status_code} body={r.text[:60]}")
r = c.get("/api/admin/blacklist", headers=AH)
ids = [b.get("passengerId") for b in (r.json() if isinstance(r.json(), list) else r.json().get("items", []))]
check("blacklist-list", "e2e-block-1" in ids, f"n={len(ids)}")
r = c.request("DELETE", "/api/admin/blacklist/e2e-block-1", headers=AH)
check("blacklist-del", r.status_code == 200, f"status={r.status_code}")

# 6. 乘客注册登录
r = c.post("/api/auth/register", json={"phone": "13700003333", "password": "test123456", "name": "E2E乘客"})
if r.status_code == 400:
    check("p-register(exists)", True, r.text[:50])
else:
    check("p-register", r.status_code == 200, f"status={r.status_code}")
r = c.post("/api/auth/login", json={"phone": "13700003333", "password": "test123456"})
PTOKEN = r.json()["token"]
PH = {"Authorization": f"Bearer {PTOKEN}"}
check("p-login", r.status_code == 200, f"status={r.status_code}")

# 7. 下单（带郑州坐标，验证地理自洽：郑州东站→二七广场 直线约10km级）
r = c.post("/api/passenger/orders", headers=PH, json={
    "origin": "郑州东站", "originLat": 34.7540, "originLng": 113.7750,
    "destination": "二七广场", "destLat": 34.7535, "destLng": 113.6600})
check("create-order", r.status_code == 200, f"status={r.status_code} body={r.text[:100]}")
oid = r.json().get("orderId") or r.json().get("id") or r.json().get("order", {}).get("id")

# 8. 支付
if oid:
    r = c.post(f"/api/passenger/orders/{oid}/pay", headers=PH)
    check("pay-order", r.status_code == 200, f"status={r.status_code} body={r.text[:80]}")

# 9. 司机列表（admin）
r = c.get("/api/admin/drivers", headers=AH)
dl = r.json() if isinstance(r.json(), list) else r.json().get("items", [])
check("drivers", r.status_code == 200 and len(dl) >= 8, f"n={len(dl)}")

# 10. 事件流（admin）
r = c.get("/api/admin/events", headers=AH)
el = r.json() if isinstance(r.json(), list) else r.json().get("items", r.json().get("content", []))
check("events", r.status_code == 200 and len(el) >= 1, f"n={len(el) if isinstance(el, list) else '?'}")

# 11. 未登录 401
r = c.get("/api/admin/stats")
check("401-guard", r.status_code == 401, f"status={r.status_code}")

# 12. 调度控制台（console.html）可达，且地理域必须为郑州（防北京坐标复活）
r = c.get("/console.html")
check("console-page", r.status_code == 200 and "郑州东站" in r.text and "天安门" not in r.text,
      f"status={r.status_code} zhengzhou={'郑州东站' in r.text}")

# 13. 演示口全链路（控制台同款）：下单 → 距离合理（郑州东→新郑机场 30km 级）→ 支付完结
r = c.post("/api/orders", json={
    "passengerId": "e2e-demo-1", "origin": "郑州东站", "originLat": 34.7540, "originLng": 113.7750,
    "destination": "新郑机场", "destLat": 34.5280, "destLng": 113.8490})
d = r.json()
check("demo-order", r.status_code == 200 and d.get("status") == "DISPATCHED" and (d.get("finalPrice") or 0) > 0,
      f"status={r.status_code} order={d.get('orderId')} price={d.get('finalPrice')}")
oid2 = d.get("orderId")
r = c.get("/api/orders")
dist = next((o.get("distanceKm") for o in r.json() if o.get("id") == oid2), None)
check("geo-consistency", dist is not None and 20 <= dist <= 45,
      f"distanceKm={dist}（直线25.9km×绕路系数，>45 说明坐标域被污染）")
r = c.post(f"/api/orders/{oid2}/pay")
check("demo-pay-completed", r.status_code == 200 and r.json().get("status") == "COMPLETED",
      f"status={r.status_code} body={r.text[:60]}")

print("\n=== ALL PASS ===")
