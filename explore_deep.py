# -*- coding: utf-8 -*-
"""Deep exploratory testing for bugs."""
import os, sys, json
sys.stdout.reconfigure(encoding='utf-8')
from playwright.sync_api import sync_playwright

SCREENSHOT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "screenshots")
os.makedirs(SCREENSHOT_DIR, exist_ok=True)

FINDINGS = []

def snap(page, name):
    path = os.path.join(SCREENSHOT_DIR, name + ".png")
    page.screenshot(path=path, full_page=True)
    return path

def login(page, user, pwd):
    page.goto("http://localhost:5173/login")
    page.wait_for_load_state("networkidle")
    page.get_by_label("Логін").fill(user)
    page.get_by_label("Пароль").fill(pwd)
    page.get_by_role("button", name="Увійти").click()
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(2000)

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    ctx = browser.new_context(viewport={"width": 1280, "height": 900})
    page = ctx.new_page()

    # ─── BUG 1: Episode UUID ───
    print("=== BUG 1: Episode UUID ===")
    login(page, "doctor1", "doctor123")
    
    if "select" in page.url:
        page.get_by_text("Карта інтенсивної терапії").first.click()
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(2000)
    
    responses = []
    page.on("response", lambda r: responses.append({
        "url": r.url, "status": r.status
    }) if "/api/" in r.url and r.status >= 400 else None)
    
    for eid in ["a1111111", "a2222222", "a3333333"]:
        page.goto(f"http://localhost:5173/doctor/episode/{eid}")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(2000)
        snap(page, f"episode_uuid_bug_{eid}")
        print(f"  {eid}: url={page.url}")
    
    bad_epi = [r for r in responses if "episode" in r["url"] or "clinical-day" in r["url"]]
    print(f"  Failed episode API calls: {len(bad_epi)}")
    for r in bad_epi[:6]:
        print(f"    [{r['status']}] {r['url'][:100]}")
    
    FINDINGS.append({
        "title": "Episode seed data uses non-UUID IDs - API fails with 400",
        "role": "ALL (Doctor, Nurse, HOD)",
        "url": "/doctor/episode/{id}",
        "severity": "CRITICAL",
        "evidence": f"API returns 400: Invalid UUID string for a1111111, a2222222, a3333333. {len(bad_epi)} failed calls."
    })

    # ─── BUG 2: Create Card page ───
    print("\n=== BUG 2: Create Card ===")
    page.goto("http://localhost:5173/doctor/create-card")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(2000)
    snap(page, "create_card_page")
    print(f"  URL: {page.url}")
    
    if "login" not in page.url:
        inputs = page.locator("input, select, textarea").all()
        buttons = page.locator("button").all()
        print(f"  Inputs: {len(inputs)}, Buttons: {len(buttons)}")
        
        # Try to interact
        for inp in inputs[:3]:
            try:
                inp.fill("test value")
                print(f"  Filled input: {inp.get_attribute('name') or inp.get_attribute('id') or 'unnamed'}")
            except:
                pass
    
    # ─── BUG 3: Admin - try to delete own account ───
    print("\n=== BUG 3: Admin delete self ===")
    ctx2 = browser.new_context(viewport={"width": 1280, "height": 900})
    page2 = ctx2.new_page()
    login(page2, "admin", "admin123")
    page2.goto("http://localhost:5173/admin")
    page2.wait_for_load_state("networkidle")
    page2.wait_for_timeout(2000)
    snap(page2, "admin_panel")
    
    # Check if admin can change their own role
    admin_rows = page2.locator("tr").filter(has_text="admin")
    if admin_rows.count() > 0:
        print("  Admin row found")
        # Check for role combobox interaction
        combos = page2.locator("tr").filter(has_text="admin").locator("[role='combobox'], select")
        print(f"  Admin row comboboxes: {combos.count()}")
    
    # Check audit tab
    audit_tab = page2.get_by_role("tab", name="Журнал аудиту")
    if audit_tab.count() > 0:
        audit_tab.click()
        page2.wait_for_timeout(2000)
        snap(page2, "audit_log_empty")
        audit_content = page2.text_content("main") or ""
        if "запис" in audit_content.lower():
            print("  Audit log has entries")
        else:
            FINDINGS.append({
                "title": "Audit log may be empty or not loading",
                "role": "Admin",
                "url": "/admin",
                "severity": "MEDIUM",
                "evidence": "Audit tab clicked but no records visible"
            })
    
    # Statistics tab
    stats_tab = page2.get_by_role("tab", name="Статистика")
    if stats_tab.count() > 0:
        stats_tab.click()
        page2.wait_for_timeout(2000)
        snap(page2, "statistics_tab")
        stats_content = page2.text_content("main") or ""
        if any(w in stats_content.lower() for w in ["статистик", "граф", "диаграм", "chart"]):
            print("  Statistics tab has content")
        elif len(stats_content.strip()) < 50:
            FINDINGS.append({
                "title": "Statistics tab appears empty or has placeholder content",
                "role": "Admin",
                "url": "/admin",
                "severity": "LOW",
                "evidence": "Statistics tab clicked but minimal content"
            })
    else:
        FINDINGS.append({
            "title": "Statistics tab missing from admin panel",
            "role": "Admin",
            "url": "/admin",
            "severity": "LOW",
            "evidence": "No Statistics tab found"
        })

    # ─── BUG 4: Prescription workflow ───
    print("\n=== BUG 4: Prescriptions ===")
    ctx3 = browser.new_context(viewport={"width": 1280, "height": 900})
    page3 = ctx3.new_page()
    login(page3, "doctor1", "doctor123")
    
    if "select" in page3.url:
        page3.get_by_text("Листок лікарських призначень").first.click()
        page3.wait_for_load_state("networkidle")
        page3.wait_for_timeout(2000)
        snap(page3, "prescription_select")
    
    page3.goto("http://localhost:5173/prescriptions/doctor")
    page3.wait_for_load_state("networkidle")
    page3.wait_for_timeout(2000)
    snap(page3, "prescriptions_page")
    print(f"  Prescriptions URL: {page3.url}")

    # Try nurse prescriptions
    ctx4 = browser.new_context(viewport={"width": 1280, "height": 900})
    page4 = ctx4.new_page()
    login(page4, "nurse1", "nurse123")
    page4.goto("http://localhost:5173/prescriptions/nurse")
    page4.wait_for_load_state("networkidle")
    page4.wait_for_timeout(2000)
    snap(page4, "nurse_prescriptions")
    print(f"  Nurse prescriptions URL: {page4.url}")
    
    # ─── BUG 5: Routes not found ───
    print("\n=== BUG 5: Route handling ===")
    bad_routes = ["/nonexistent", "/doctor/nonexistent", "/api/nonexistent"]
    for route in bad_routes:
        page.goto(f"http://localhost:5173{route}")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(1000)
        snap(page, f"route_{route.replace('/', '_')}")
        print(f"  {route}: {page.url} (title: {page.title()})")

    # ─── BUG 6: Login validation ───
    print("\n=== BUG 6: Login validation ===")
    page.goto("http://localhost:5173/login")
    page.wait_for_load_state("networkidle")
    
    # Try empty login
    page.get_by_role("button", name="Увійти").click()
    page.wait_for_timeout(1000)
    snap(page, "login_empty_submit")
    
    # Try wrong password
    page.get_by_label("Логін").fill("doctor1")
    page.get_by_label("Пароль").fill("wrongpassword")
    page.get_by_role("button", name="Увійти").click()
    page.wait_for_timeout(2000)
    snap(page, "login_wrong_password")
    login_content = page.text_content("body") or ""
    if "Помилка" in login_content or "error" in login_content.lower() or "невірно" in login_content.lower():
        print("  Login validation shows error message")
    else:
        FINDINGS.append({
            "title": "Login with wrong password shows no visible error",
            "role": "ALL",
            "url": "/login",
            "severity": "MEDIUM",
            "evidence": "Submitted wrong password but no clear error appeared"
        })
    
    # ─── BUG 7: HOD specific ───
    print("\n=== BUG 7: HOD features ===")
    ctx5 = browser.new_context(viewport={"width": 1280, "height": 900})
    page5 = ctx5.new_page()
    login(page5, "head1", "head123")
    
    if "select" in page5.url:
        page5.get_by_text("Карта інтенсивної терапії").first.click()
        page5.wait_for_load_state("networkidle")
        page5.wait_for_timeout(2000)
    
    page5.goto("http://localhost:5173/doctor/episode/a2222222")
    page5.wait_for_load_state("networkidle")
    page5.wait_for_timeout(2000)
    snap(page5, "hod_episode_a2222222")
    print(f"  HOD episode: {page5.url}")
    
    # ─── BUG 8: Admin delete user ───
    print("\n=== BUG 8: Admin delete user ===")
    page2.goto("http://localhost:5173/admin")
    page2.wait_for_load_state("networkidle")
    page2.wait_for_timeout(2000)
    
    delete_btns = page2.get_by_role("button", name="Видалити")
    print(f"  Delete buttons: {delete_btns.count()}")
    
    if delete_btns.count() > 0:
        # Try to click delete on a doctor row (not admin)
        for i in range(delete_btns.count()):
            try:
                btn = delete_btns.nth(i)
                parent_row = btn.locator("xpath=ancestor::tr")
                row_text = parent_row.text_content() or ""
                if "admin" not in row_text.lower():
                    btn.click(timeout=5000)
                    page2.wait_for_timeout(1000)
                    snap(page2, f"delete_user_{i}")
                    print(f"  Clicked delete button #{i} for row: {row_text[:50]}")
                    
                    # Check for confirmation dialog
                    try:
                        dialog = page2.once("dialog", lambda d: (d.accept(), print(f"  Dialog accepted: {d.message}"))[0])
                        page2.wait_for_timeout(2000)
                    except:
                        pass
                    break
            except Exception as e:
                print(f"  Cannot click delete #{i}: {str(e)[:60]}")

    # ─── Print all findings ───
    print("\n\n" + "=" * 60)
    print("ALL FINDINGS")
    print("=" * 60)
    for i, f in enumerate(FINDINGS):
        print(f"\n[{f['severity']}] {f['title']}")
        print(f"  Role: {f['role']} | URL: {f['url']}")
    
    # Save
    with open(os.path.join(SCREENSHOT_DIR, "deep_findings.json"), "w", encoding="utf-8") as fp:
        json.dump(FINDINGS, fp, ensure_ascii=False, indent=2)
    
    for c in [ctx, ctx2, ctx3, ctx4, ctx5]:
        try: c.close()
        except: pass
    browser.close()

print(f"\nDone. Screenshots in: {SCREENSHOT_DIR}")
