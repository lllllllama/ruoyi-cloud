import argparse
import importlib.util
import json
import sys
from pathlib import Path

from playwright.sync_api import sync_playwright


SCRIPT_DIR = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("browser_e2e", SCRIPT_DIR / "browser-e2e.py")
browser_e2e = importlib.util.module_from_spec(spec)
spec.loader.exec_module(browser_e2e)

FRAMEWORK_NAME = "SEC-XSS-FRAMEWORK"
TASK_PAYLOAD = '<img src=x onerror="window.__qaXss=1">SEC-XSS-TASK'
SUBMISSION_PAYLOAD = '<svg onload="window.__qaXss=2">SEC-XSS-SUBMISSION'
DESCRIPTION_PAYLOAD = "<script>window.__qaXss=3</script>SEC-XSS-DESCRIPTION"


def main() -> None:
    sys.stdout.reconfigure(encoding="utf-8")
    parser = argparse.ArgumentParser(description="Verify stored XSS payloads render as text")
    parser.add_argument("--browser", choices=browser_e2e.BROWSERS, default="chrome")
    parser.add_argument("--base-url", default=browser_e2e.BASE_URL)
    args = parser.parse_args()

    dialogs = []
    page_errors = []
    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(
            headless=True,
            executable_path=browser_e2e.BROWSERS[args.browser],
        )
        context = browser.new_context(viewport=browser_e2e.VIEWPORTS["desktop"])
        page = context.new_page()
        page.on("dialog", lambda dialog: (dialogs.append(dialog.message), dialog.dismiss()))
        page.on("pageerror", lambda error: page_errors.append(str(error)))
        try:
            browser_e2e.login(page, "admin", args.base_url)
            browser_e2e.navigate(page, args.base_url, "/research/task")
            page.locator(".task-toolbar .el-select").click()
            page.locator(".el-select-dropdown:visible .el-select-dropdown__item").filter(
                has_text=FRAMEWORK_NAME
            ).first.click()
            page.locator(".el-loading-mask").wait_for(state="hidden", timeout=15_000)
            task_row = page.locator(".el-table__row").filter(has_text=TASK_PAYLOAD).first
            task_row.wait_for(state="visible", timeout=15_000)
            task_safe = (
                page.locator("img[src='x']").count() == 0
                and page.evaluate("window.__qaXss === undefined")
            )

            browser_e2e.navigate(page, args.base_url, "/research/audit")
            submission_row = page.locator(".el-table__row").filter(has_text=SUBMISSION_PAYLOAD).first
            submission_row.wait_for(state="visible", timeout=15_000)
            submission_safe = (
                page.locator("svg[onload]").count() == 0
                and page.evaluate("window.__qaXss === undefined")
            )
            browser_e2e.visible_button(page, "查看").click()
            page.locator(".el-dialog:visible").filter(has_text=DESCRIPTION_PAYLOAD).wait_for(state="visible", timeout=15_000)
            detail_safe = (
                page.locator(".el-dialog:visible script").count() == 0
                and page.evaluate("window.__qaXss === undefined")
            )
            report = {
                "status": "PASS" if task_safe and submission_safe and detail_safe and not dialogs and not page_errors else "FAIL",
                "task_rendered_as_text": task_safe,
                "submission_rendered_as_text": submission_safe,
                "description_rendered_as_text": detail_safe,
                "dialogs": dialogs,
                "page_errors": page_errors,
            }
            print(json.dumps(report, ensure_ascii=False, indent=2))
            if report["status"] != "PASS":
                raise SystemExit(1)
        finally:
            context.close()
            browser.close()


if __name__ == "__main__":
    main()
