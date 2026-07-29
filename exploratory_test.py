# -*- coding: utf-8 -*-
"""
Exploratory Testing Script for ICU Patient Chart
Tests all 4 roles: Admin, Doctor, Nurse, Head of Department
"""
import os, sys, json, time, datetime, traceback

# Force UTF-8 for stdout
sys.stdout.reconfigure(encoding='utf-8')

from playwright.sync_api import sync_playwright

SCREENSHOT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "screenshots")
os.makedirs(SCREENSHOT_DIR, exist_ok=True)

BUGS = []
CONSOLE_ERRORS = []
NETWORK_LOGS = []

def slug(text):
    return text.lower().replace(" ", "_").replace("/", "_")[:60]

def screenshot(page, name):
    path = os.path.join(SCREENSHOT_DIR, f"{slug(name)}.png")
    page.screenshot(path=path, full_page=True)
    return path

def record_bug(page, title, role, url, steps, expected, actual, severity="MEDIUM"):
    bug = {
        "title": title,
        "role": role,
        "url": url,
        "steps": steps,
        "expected": expected,
        "actual": actual,
        "severity": severity,
        "screenshot": screenshot(page, f"{role}_{title}"),
    }
    BUGS.append(bug)
    print(f"\n[BUG {severity}] {title}")
    print(f"  Role: {role} | URL: {url}")
    print(f"  Expected: {expected}")
    print(f"  Actual: {actual}")

def login_as(page, username, password):
    page.goto("http://localhost:5173/login")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(500)
    page.get_by_label("Логін").fill(username)
    page.get_by_label("Пароль").fill(password)
    page.get_by_role("button", name="Увійти").click()
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(2000)
    return page.url

def safe_click(page, locator, timeout=3000):
    """Click with timeout handling."""
    try:
        locator.first.click(timeout=timeout)
        page.wait_for_timeout(500)
        return True
    except:
        return False

def safe_text(page, locator):
    try:
        return locator.text_content()
    except:
        return ""

def test_admin(context, results):
    """Test Admin role."""
    page = context.new_page()
    page.on("console", lambda msg: CONSOLE_ERRORS.append({"type": msg.type, "text": msg.text, "context": "admin"}) if msg.type in ("error", "warning") else None)
    page.on("response", lambda resp: NETWORK_LOGS.append({"url": resp.url, "status": resp.status, "context": "admin"}) if resp.status >= 400 else None)
    
    print("=== ADMIN TESTING ===")
    
    # Login
    url_after_login = login_as(page, "admin", "admin123")
    results["admin"]["login_url"] = url_after_login
    results["admin"]["pages_visited"].append(url_after_login)
    print(f"Login redirected to: {url_after_login}")
    
    if "select" not in url_after_login:
        record_bug(page, "Admin redirected to wrong page after login", "admin", url_after_login,
                   ["Login as admin"], "Redirect to /select", f"Redirected to {url_after_login}", "MEDIUM")
    
    # Navigate directly to admin
    page.goto("http://localhost:5173/admin")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(2000)
    results["admin"]["pages_visited"].append("/admin")
    sp = screenshot(page, "admin_main_page")
    results["admin"]["screenshots"].append(sp)
    
    # --- Users Tab ---
    print("  Testing Users tab...")
    
    # Check user table structure
    try:
        rows = page.locator("table tbody tr, [role='rowgroup'] tr").all()
        print(f"  Found {len(rows)} user rows")
    except:
        rows = []
    
    # Try to change a role via combobox
    try:
        comboboxes = page.locator("[role='combobox'], select, [data-slot='select-trigger']").all()
        print(f"  Found {len(comboboxes)} role selectors")
        for i, cb in enumerate(comboboxes[:2]):
            try:
                cb.click(timeout=2000)
                page.wait_for_timeout(500)
                # Try to select an option
                option = page.locator("[role='option'], option, [data-slot='select-item']").first
                if option.count() > 0:
                    option.click()
                    page.wait_for_timeout(500)
                    print(f"  Changed role via combobox #{i}")
            except Exception as e:
                print(f"  Could not interact with combobox #{i}: {str(e)[:50]}")
    except Exception as e:
        print(f"  Error with comboboxes: {str(e)[:100]}")
    
    # Test Prescriber toggle
    try:
        prescriber_cells = page.locator("td, [role='cell']").filter(has_text="НІ")
        if prescriber_cells.count() > 0:
            print(f"  Found {prescriber_cells.count()} 'НІ' prescriber cells")
    except:
        pass
    
    # Test Delete button
    try:
        delete_btns = page.get_by_role("button", name="Видалити")
        print(f"  Found {delete_btns.count()} delete buttons")
        if delete_btns.count() > 0:
            # Try to click delete on first non-admin user
            for i in range(delete_btns.count()):
                try:
                    btn = delete_btns.nth(i)
                    row_text = btn.locator("xpath=ancestor::tr").text_content()
                    if "admin" not in row_text.lower():
                        # Try clicking
                        btn.click(timeout=3000)
                        page.wait_for_timeout(1000)
                        print(f"  Clicked delete button #{i}")
                        # Check for dialog
                        break
                except Exception as e:
                    print(f"  Could not click delete #{i}: {str(e)[:50]}")
    except Exception as e:
        print(f"  Error with delete buttons: {str(e)[:100]}")
    
    # --- Audit Tab ---
    print("  Testing Audit tab...")
    try:
        audit_tab = page.get_by_role("tab", name="Журнал аудиту")
        if audit_tab.count() > 0:
            safe_click(page, audit_tab)
            page.wait_for_timeout(1500)
            sp2 = screenshot(page, "admin_audit_tab")
            results["admin"]["screenshots"].append(sp2)
            audit_content = page.text_content("main") or ""
            if "запис" in audit_content.lower() or "аудит" in audit_content.lower():
                print("  Audit tab has content related to audit records")
            else:
                print("  Audit tab content present but may be empty")
        else:
            record_bug(page, "Audit log tab not found in admin panel", "admin", page.url,
                       ["Navigate to Admin panel"], "Audit log tab should be visible",
                       "Audit log tab is missing", "MEDIUM")
    except Exception as e:
        print(f"  Error testing audit: {str(e)[:100]}")
    
    # --- Statistics Tab ---
    print("  Testing Statistics tab...")
    try:
        stats_tab = page.get_by_role("tab", name="Статистика")
        if stats_tab.count() > 0:
            safe_click(page, stats_tab)
            page.wait_for_timeout(1500)
            sp3 = screenshot(page, "admin_statistics")
            results["admin"]["screenshots"].append(sp3)
            print("  Statistics tab accessible")
        else:
            print("  Statistics tab not found (may not exist)")
    except Exception as e:
        print(f"  Error testing stats: {str(e)[:100]}")
    
    # --- Navigation & Theme ---
    print("  Testing navigation controls...")
    try:
        # Theme toggle
        theme_btn = page.locator("button").filter(has_text="тему").or_(
            page.get_by_role("button", name="тему")
        )
        if theme_btn.count() > 0:
            theme_btn.first.click(timeout=2000)
            page.wait_for_timeout(500)
            print("  Theme toggled")
            sp4 = screenshot(page, "admin_theme_dark")
            results["admin"]["screenshots"].append(sp4)
        
        # User menu
        user_menu_btn = page.locator("button").filter(has_text="Меню").or_(
            page.get_by_role("button", name="Меню")
        )
        if user_menu_btn.count() > 0:
            user_menu_btn.first.click(timeout=2000)
            page.wait_for_timeout(500)
            print("  User menu opened")
            
            # Check for logout
            if "Вийти" in (page.text_content("body") or ""):
                print("  Logout option found in user menu")
            else:
                record_bug(page, "User menu missing logout option", "admin", page.url,
                           ["Click user menu button"], "Logout option should be available",
                           "No logout option found in user menu", "LOW")
            
            # Close menu
            page.locator("body").click(position={"x": 10, "y": 10})
            page.wait_for_timeout(300)
    except Exception as e:
        print(f"  Error with navigation controls: {str(e)[:100]}")
    
    # --- Permission Testing ---
    print("  Testing permission restrictions...")
    for path, desc in [("/doctor", "Doctor dashboard"), ("/nurse", "Nurse dashboard"),
                       ("/doctor/episode/a1111111", "Episode page")]:
        page.goto(f"http://localhost:5173{path}")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(500)
        current = page.url
        print(f"  Admin -> {desc}: {current}")
    
    page.close()
    return results

def test_doctor(context, results):
    """Test Doctor role."""
    page = context.new_page()
    page.on("console", lambda msg: CONSOLE_ERRORS.append({"type": msg.type, "text": msg.text, "context": "doctor"}) if msg.type in ("error", "warning") else None)
    page.on("response", lambda resp: NETWORK_LOGS.append({"url": resp.url, "status": resp.status, "context": "doctor"}) if resp.status >= 400 else None)
    
    print("=== DOCTOR TESTING ===")
    
    # Login
    url_after_login = login_as(page, "doctor1", "doctor123")
    results["doctor"]["login_url"] = url_after_login
    results["doctor"]["pages_visited"].append(url_after_login)
    print(f"Login redirected to: {url_after_login}")
    
    if "doctor" not in url_after_login and "select" in url_after_login:
        print("  Doctor redirected to app selector (normal - needs to pick app)")
        # Pick ICU chart
        icu_card = page.get_by_text("Карта інтенсивної терапії").first
        if icu_card.count() > 0:
            icu_card.click()
            page.wait_for_load_state("networkidle")
            page.wait_for_timeout(2000)
            results["doctor"]["pages_visited"].append(page.url)
            print(f"  After selecting ICU chart: {page.url}")
    
    # Doctor dashboard
    page.goto("http://localhost:5173/doctor")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(2000)
    sp = screenshot(page, "doctor_dashboard")
    results["doctor"]["screenshots"].append(sp)
    results["doctor"]["pages_visited"].append("/doctor")
    
    print("  Exploring doctor dashboard...")
    doc_content = page.text_content("body") or ""
    
    # Check for patient data
    for patient in ["Петренко", "Коваленко", "Сидоренко"]:
        if patient in doc_content:
            print(f"  Patient '{patient}' found on dashboard")
    
    # Check for search
    search_inputs = page.locator("input").all()
    print(f"  Found {len(search_inputs)} input fields")
    
    # Check for department toggle
    dept_buttons = page.locator("button").filter(has_text="Хірургія").or_(
        page.locator("button").filter(has_text="Реабілітація")
    )
    print(f"  Department toggle buttons: {dept_buttons.count()}")
    if dept_buttons.count() > 0:
        try:
            dept_buttons.first.click(timeout=2000)
            page.wait_for_timeout(1000)
            print("  Toggled department")
        except:
            pass
    
    # Look for create card button
    create_btn = page.locator("button, a").filter(has_text="Створити")
    if create_btn.count() > 0:
        print(f"  Found {create_btn.count()} 'Створити' buttons")
        try:
            create_btn.first.click(timeout=3000)
            page.wait_for_timeout(2000)
            sp2 = screenshot(page, "doctor_create_card")
            results["doctor"]["screenshots"].append(sp2)
            results["doctor"]["pages_visited"].append(page.url)
            print(f"  Create card page: {page.url}")
        except:
            print("  Could not click create card button")
    
    # Try to navigate to episode
    page.goto("http://localhost:5173/doctor/episode/a1111111")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(2000)
    results["doctor"]["pages_visited"].append("/doctor/episode/a1111111")
    sp3 = screenshot(page, "doctor_episode")
    results["doctor"]["screenshots"].append(sp3)
    print(f"  Episode page access: {page.url}")
    
    if "login" in page.url:
        record_bug(page, "Doctor cannot access episode page", "doctor", page.url,
                   ["Login as doctor", "Navigate to /doctor/episode/a1111111"],
                   "Doctor should access episode page directly",
                   f"Redirected to login: {page.url}", "HIGH")
    
    # Try prescriptions page
    page.goto("http://localhost:5173/prescriptions/doctor")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(2000)
    results["doctor"]["pages_visited"].append("/prescriptions/doctor")
    sp4 = screenshot(page, "doctor_prescriptions")
    results["doctor"]["screenshots"].append(sp4)
    
    # Try opening a prescription
    open_btns = page.locator("button, a").filter(has_text="Відкрити")
    if open_btns.count() > 0:
        print(f"  Found {open_btns.count()} 'Відкрити' buttons")
        try:
            open_btns.first.click(timeout=3000)
            page.wait_for_timeout(2000)
            sp5 = screenshot(page, "doctor_prescription_detail")
            results["doctor"]["screenshots"].append(sp5)
            print("  Opened prescription detail")
        except:
            print("  Could not open prescription")
    
    # Permission testing
    print("  Testing permission restrictions...")
    for path, desc in [("/admin", "Admin panel"), ("/nurse", "Nurse dashboard")]:
        page.goto(f"http://localhost:5173{path}")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(500)
        print(f"  Doctor -> {desc}: {page.url}")
    
    # Theme toggle
    try:
        theme_btn = page.locator("button").filter(has_text="тему")
        if theme_btn.count() > 0:
            theme_btn.first.click(timeout=2000)
            page.wait_for_timeout(500)
            print("  Theme toggled")
    except:
        pass
    
    page.close()
    return results

def test_nurse(context, results):
    """Test Nurse role."""
    page = context.new_page()
    page.on("console", lambda msg: CONSOLE_ERRORS.append({"type": msg.type, "text": msg.text, "context": "nurse"}) if msg.type in ("error", "warning") else None)
    page.on("response", lambda resp: NETWORK_LOGS.append({"url": resp.url, "status": resp.status, "context": "nurse"}) if resp.status >= 400 else None)
    
    print("=== NURSE TESTING ===")
    
    # Login
    url_after_login = login_as(page, "nurse1", "nurse123")
    results["nurse"]["login_url"] = url_after_login
    results["nurse"]["pages_visited"].append(url_after_login)
    print(f"Login redirected to: {url_after_login}")
    
    if "select" in url_after_login:
        # Pick ICU chart
        icu_card = page.get_by_text("Карта інтенсивної терапії").first
        if icu_card.count() > 0:
            icu_card.click()
            page.wait_for_load_state("networkidle")
            page.wait_for_timeout(2000)
            results["nurse"]["pages_visited"].append(page.url)
            print(f"  After selecting ICU chart: {page.url}")
    
    # Nurse dashboard
    page.goto("http://localhost:5173/nurse")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(2000)
    sp = screenshot(page, "nurse_dashboard")
    results["nurse"]["screenshots"].append(sp)
    results["nurse"]["pages_visited"].append("/nurse")
    
    print("  Exploring nurse dashboard...")
    nurse_content = page.text_content("body") or ""
    
    for patient in ["Петренко", "Коваленко", "Сидоренко"]:
        if patient in nurse_content:
            print(f"  Patient '{patient}' found on nurse dashboard")
    
    # Check for vital signs / hourly records
    # Try to open episode
    page.goto("http://localhost:5173/nurse/episode/a1111111")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(2000)
    results["nurse"]["pages_visited"].append("/nurse/episode/a1111111")
    sp2 = screenshot(page, "nurse_episode")
    results["nurse"]["screenshots"].append(sp2)
    print(f"  Nurse episode access: {page.url}")
    
    if "login" in page.url:
        record_bug(page, "Nurse cannot access episode page", "nurse", page.url,
                   ["Login as nurse", "Navigate to /nurse/episode/a1111111"],
                   "Nurse should access episode page",
                   f"Redirected to login: {page.url}", "HIGH")
    
    # Try prescriptions page
    page.goto("http://localhost:5173/prescriptions/nurse")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(2000)
    results["nurse"]["pages_visited"].append("/prescriptions/nurse")
    sp3 = screenshot(page, "nurse_prescriptions")
    results["nurse"]["screenshots"].append(sp3)
    print(f"  Nurse prescriptions page: {page.url}")
    
    # Try to open a prescription
    open_btns = page.locator("button, a").filter(has_text="Відкрити")
    if open_btns.count() > 0:
        try:
            open_btns.first.click(timeout=3000)
            page.wait_for_timeout(2000)
            sp4 = screenshot(page, "nurse_prescription_detail")
            results["nurse"]["screenshots"].append(sp4)
            print("  Opened nurse prescription detail")
        except:
            print("  Could not open prescription")
    
    # Permission testing
    print("  Testing permission restrictions...")
    for path, desc in [("/admin", "Admin panel"), ("/doctor/create-card", "Create card page")]:
        page.goto(f"http://localhost:5173{path}")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(500)
        print(f"  Nurse -> {desc}: {page.url}")
    
    page.close()
    return results

def test_hod(context, results):
    """Test Head of Department role."""
    page = context.new_page()
    page.on("console", lambda msg: CONSOLE_ERRORS.append({"type": msg.type, "text": msg.text, "context": "hod"}) if msg.type in ("error", "warning") else None)
    page.on("response", lambda resp: NETWORK_LOGS.append({"url": resp.url, "status": resp.status, "context": "hod"}) if resp.status >= 400 else None)
    
    print("=== HOD TESTING ===")
    
    # Login
    url_after_login = login_as(page, "head1", "head123")
    results["hod"]["login_url"] = url_after_login
    results["hod"]["pages_visited"].append(url_after_login)
    print(f"Login redirected to: {url_after_login}")
    
    if "select" in url_after_login:
        icu_card = page.get_by_text("Карта інтенсивної терапії").first
        if icu_card.count() > 0:
            icu_card.click()
            page.wait_for_load_state("networkidle")
            page.wait_for_timeout(2000)
            results["hod"]["pages_visited"].append(page.url)
            print(f"  After selecting ICU chart: {page.url}")
    
    # Doctor dashboard (HOD has DOCTOR-level access)
    page.goto("http://localhost:5173/doctor")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(2000)
    sp = screenshot(page, "hod_dashboard")
    results["hod"]["screenshots"].append(sp)
    results["hod"]["pages_visited"].append("/doctor")
    
    print("  Exploring HOD dashboard...")
    hod_content = page.text_content("body") or ""
    
    # Check for department-specific UI
    if "Відділення" in hod_content:
        print("  Department info visible")
    
    # Check for department toggle
    dept_btns = page.locator("button").filter(has_text="Хірургія").or_(
        page.locator("button").filter(has_text="Реабілітація")
    )
    print(f"  Department buttons: {dept_btns.count()}")
    
    # Episode access
    page.goto("http://localhost:5173/doctor/episode/a1111111")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(2000)
    results["hod"]["pages_visited"].append("/doctor/episode/a1111111")
    sp2 = screenshot(page, "hod_episode")
    results["hod"]["screenshots"].append(sp2)
    print(f"  HOD episode access: {page.url}")
    
    if "login" in page.url:
        record_bug(page, "HOD cannot access episode page", "hod", page.url,
                   ["Login as head1", "Navigate to /doctor/episode/a1111111"],
                   "HOD should access episode page",
                   f"Redirected to login: {page.url}", "HIGH")
    
    # Prescriptions
    page.goto("http://localhost:5173/prescriptions/doctor")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(2000)
    results["hod"]["pages_visited"].append("/prescriptions/doctor")
    sp3 = screenshot(page, "hod_prescriptions")
    results["hod"]["screenshots"].append(sp3)
    
    # Clinical day reopen feature
    print("  Testing clinical day reopen...")
    try:
        page.goto("http://localhost:5173/doctor/episode/a2222222")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(2000)
        
        reopen_btn = page.locator("button, a").filter(has_text="Відкрити").or_(
            page.locator("button").filter(has_text="Перевідкрити").or_(
                page.locator("button").filter(has_text="Reopen")
            )
        )
        print(f"  Reopen-related buttons: {reopen_btn.count()}")
    except:
        pass
    
    # Permission testing
    print("  Testing permission restrictions...")
    for path, desc in [("/admin", "Admin panel"), ("/nurse", "Nurse dashboard")]:
        page.goto(f"http://localhost:5173{path}")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(500)
        print(f"  HOD -> {desc}: {page.url}")
    
    page.close()
    return results

def run_all_tests():
    print("=" * 60)
    print("ICU PATIENT CHART - EXPLORATORY TESTING")
    print("=" * 60)
    print(f"Started at: {datetime.datetime.now()}")
    
    results = {
        "admin": {"pages_visited": [], "screenshots": [], "login_url": ""},
        "doctor": {"pages_visited": [], "screenshots": [], "login_url": ""},
        "nurse": {"pages_visited": [], "screenshots": [], "login_url": ""},
        "hod": {"pages_visited": [], "screenshots": [], "login_url": ""},
    }
    
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        
        ctx_admin = browser.new_context(viewport={"width": 1280, "height": 900})
        ctx_doctor = browser.new_context(viewport={"width": 1280, "height": 900})
        ctx_nurse = browser.new_context(viewport={"width": 1280, "height": 900})
        ctx_hod = browser.new_context(viewport={"width": 1280, "height": 900})
        
        try:
            results = test_admin(ctx_admin, results)
            results = test_doctor(ctx_doctor, results)
            results = test_nurse(ctx_nurse, results)
            results = test_hod(ctx_hod, results)
        except Exception as e:
            print(f"\nFATAL ERROR: {e}")
            traceback.print_exc()
        finally:
            for ctx in [ctx_admin, ctx_doctor, ctx_nurse, ctx_hod]:
                try: ctx.close()
                except: pass
            browser.close()
    
    return results

# ──────────────────────────────────────────────────────────────
if __name__ == "__main__":
    start = time.time()
    results = run_all_tests()
    elapsed = time.time() - start
    
    print("\n\n" + "=" * 60)
    print("FINAL REPORT")
    print("=" * 60)
    print(f"Duration: {elapsed:.1f}s")
    
    for role, data in results.items():
        print(f"\n{role.upper()}: {len(data['pages_visited'])} pages, {len(data['screenshots'])} screenshots")
        for p in data['pages_visited']:
            print(f"  - {p}")
    
    print(f"\nCONSOLE ERRORS/WARNINGS: {len(CONSOLE_ERRORS)}")
    for ce in CONSOLE_ERRORS[:15]:
        print(f"  [{ce['type']}] {ce['text'][:200]}")
    if len(CONSOLE_ERRORS) > 15:
        print(f"  ... and {len(CONSOLE_ERRORS) - 15} more")
    
    print(f"\nNETWORK ERRORS (>=400): {len(NETWORK_LOGS)}")
    for ne in NETWORK_LOGS[:15]:
        print(f"  [{ne['status']}] {ne['url'][:120]}")
    if len(NETWORK_LOGS) > 15:
        print(f"  ... and {len(NETWORK_LOGS) - 15} more")
    
    print(f"\nBUGS FOUND: {len(BUGS)}")
    for i, bug in enumerate(BUGS):
        print(f"\n  Bug #{i+1}: [{bug['severity']}] {bug['title']}")
        print(f"    Role: {bug['role']} | URL: {bug['url']}")
        print(f"    Screenshot: {bug['screenshot']}")
    
    print(f"\nScreenshots: {SCREENSHOT_DIR}")
    
    report = {
        "timestamp": datetime.datetime.now().isoformat(),
        "duration_seconds": elapsed,
        "results": {k: {
            "pages_visited": v["pages_visited"],
            "screenshots": [os.path.basename(s) for s in v["screenshots"]],
        } for k, v in results.items()},
        "console_errors": [{"type": e["type"], "text": e["text"][:200]} for e in CONSOLE_ERRORS[:50]],
        "network_errors": [{"status": n["status"], "url": n["url"][:150]} for n in NETWORK_LOGS[:50]],
        "bugs": BUGS,
    }
    report_path = os.path.join(SCREENSHOT_DIR, "exploratory_test_report.json")
    with open(report_path, "w", encoding="utf-8") as f:
        json.dump(report, f, ensure_ascii=False, indent=2)
    print(f"\nReport saved: {report_path}")
