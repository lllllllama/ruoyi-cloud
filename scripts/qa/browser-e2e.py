import argparse
import json
import socket
import sys
import time
from pathlib import Path

from playwright.sync_api import Error as PlaywrightError, Page, sync_playwright


BROWSERS = {
    "chrome": r"C:\Program Files\Google\Chrome\Application\chrome.exe",
    "edge": r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
}
VIEWPORTS = {
    "desktop": {"width": 1366, "height": 768},
    "wide": {"width": 1920, "height": 1080},
}
BASE_URL = "http://127.0.0.1:81"
PASSWORD = "admin123"


class QaRun:
    def __init__(self) -> None:
        self.results = []

    def check(self, name: str, condition: bool, actual: str = "") -> None:
        self.results.append({
            "case": name,
            "status": "PASS" if condition else "FAIL",
            "actual": actual,
        })
        print(f"[{self.results[-1]['status']}] {name}: {actual}")


def captcha_answer(uuid: str) -> str:
    key = f"captcha_codes:{uuid}".encode("utf-8")
    command = b"*2\r\n$3\r\nGET\r\n$" + str(len(key)).encode("ascii") + b"\r\n" + key + b"\r\n"
    with socket.create_connection(("127.0.0.1", 6379), timeout=5) as client:
        client.sendall(command)
        response = client.recv(2048)
    first_line, payload = response.split(b"\r\n", 1)
    if not first_line.startswith(b"$"):
        raise RuntimeError(f"Unexpected Redis response: {response!r}")
    return str(json.loads(payload.split(b"\r\n", 1)[0].decode("utf-8")))


def login(page: Page, username: str, base_url: str) -> None:
    captcha = {}

    def capture(response) -> None:
        if response.url.rstrip("/").endswith("/code") and response.ok:
            captcha.update(response.json())

    page.on("response", capture)
    page.goto(base_url, wait_until="networkidle")
    page.locator("input").nth(0).fill(username)
    page.locator("input").nth(1).fill(PASSWORD)
    page.locator("input").nth(2).fill(captcha_answer(captcha["uuid"]))
    page.locator(".login-form button").click()
    page.wait_for_url(lambda url: "/login" not in url, timeout=15_000)
    page.wait_for_load_state("networkidle")


def navigate(page: Page, base_url: str, path: str) -> None:
    page.goto(base_url.rstrip("/") + path, wait_until="networkidle")
    page.locator(".app-container").wait_for(state="visible", timeout=15_000)
    page.locator(".el-loading-mask").wait_for(state="hidden", timeout=15_000)


def visible_button(page_or_locator, text: str):
    return page_or_locator.locator("button:visible").filter(has_text=text).first


def form_item(container, label: str):
    return container.locator(".el-form-item").filter(has_text=label).first


def select_option(page: Page, form_container, label: str, option_text: str) -> None:
    item = form_item(form_container, label)
    item.locator(".el-select").click()
    option = page.locator(".el-select-dropdown:visible .el-select-dropdown__item").filter(
        has_text=option_text
    ).first
    option.wait_for(state="visible")
    option.click()


def wait_success(page: Page, expected: str = "成功") -> None:
    message = page.locator(".el-message--success:visible").filter(has_text=expected).last
    message.wait_for(state="visible", timeout=15_000)


def fill_task_dialog(page: Page, task_name: str) -> None:
    dialog = page.locator(".el-dialog:visible").filter(has_text="新增任务").last
    dialog.wait_for(state="visible")
    form_item(dialog, "任务名称").locator("input").fill(task_name)
    form_item(dialog, "任务类型").locator("input").fill("E2E")
    visible_button(dialog, "确定").click()
    wait_success(page, "保存成功")
    page.locator(".el-loading-mask").wait_for(state="hidden", timeout=15_000)


def table_row(page: Page, text: str):
    row = page.locator(".task-tree-page > .el-table > .el-table__body-wrapper .el-table__row").filter(has_text=text).first
    row.wait_for(state="visible", timeout=15_000)
    return row


def click_task_row_action(page: Page, row_text: str, action: str) -> None:
    rows = page.locator(".task-tree-page > .el-table > .el-table__body-wrapper .el-table__row")
    row_index = next((index for index in range(rows.count()) if row_text in rows.nth(index).inner_text()), None)
    if row_index is None:
        raise RuntimeError(f"Task row not found: {row_text}")
    action_buttons = page.locator("button:visible").filter(has_text=action)
    action_index = row_index if action != "子任务" else min(row_index, action_buttons.count() - 1)
    action_buttons.nth(action_index).click()


def create_research_workflow(page: Page, qa: QaRun, base_url: str, suffix: str) -> None:
    framework_name = f"E2E年度任务-{suffix}"
    root_name = f"E2E一级-{suffix}"
    second_name = f"E2E二级-{suffix}"
    leaf_name = f"E2E三级-{suffix}"
    deliverable_name = f"E2E成果-{suffix}"

    navigate(page, base_url, "/research/framework")
    visible_button(page, "新增年度任务").click()
    dialog = page.locator(".el-dialog:visible").filter(has_text="新增年度任务").last
    dialog.wait_for(state="visible")
    select_option(page, dialog, "课题", "QA课题A")
    form_item(dialog, "框架名称").locator("input").fill(framework_name)
    form_item(dialog, "总体目标").locator("textarea").fill("Chrome/Edge 真实浏览器端到端验证")
    visible_button(dialog, "确定").click()
    wait_success(page, "保存成功")
    page.get_by_text(framework_name, exact=True).first.wait_for(state="visible", timeout=15_000)
    qa.check(f"{suffix}-FRAMEWORK-CREATE", True, framework_name)

    page.reload(wait_until="networkidle")
    page.get_by_text(framework_name, exact=True).first.wait_for(state="visible", timeout=15_000)
    qa.check(f"{suffix}-FRAMEWORK-PERSIST", True, framework_name)

    navigate(page, base_url, "/research/task")
    toolbar_select = page.locator(".task-toolbar .el-select").first
    toolbar_select.click()
    page.locator(".el-select-dropdown:visible .el-select-dropdown__item").filter(
        has_text=framework_name
    ).first.click()
    page.locator(".el-loading-mask").wait_for(state="hidden", timeout=15_000)

    visible_button(page, "添加一级任务").click()
    fill_task_dialog(page, root_name)
    table_row(page, root_name)
    click_task_row_action(page, root_name, "子任务")
    fill_task_dialog(page, second_name)
    table_row(page, second_name)
    click_task_row_action(page, second_name, "子任务")
    fill_task_dialog(page, leaf_name)
    leaf_row = table_row(page, leaf_name)
    qa.check(f"{suffix}-THREE-LEVEL-TREE", leaf_row.count() == 1, leaf_name)

    click_task_row_action(page, leaf_name, "成果")
    drawer = page.locator(".el-drawer:visible").last
    drawer.wait_for(state="visible", timeout=15_000)
    visible_button(drawer, "添加成果").click()
    deliverable_dialog = page.locator(".el-dialog:visible").filter(has_text="添加交付成果").last
    deliverable_dialog.wait_for(state="visible")
    form_item(deliverable_dialog, "成果名称").locator("input").fill(deliverable_name)
    form_item(deliverable_dialog, "成果要求").locator("textarea").fill("完成一份可归档的端到端测试成果")
    select_option(page, deliverable_dialog, "责任人", "A课题成员")
    page.keyboard.press("Escape")
    visible_button(deliverable_dialog, "确定").click()
    wait_success(page, "保存成功")
    drawer.get_by_text(deliverable_name, exact=True).wait_for(state="visible", timeout=15_000)
    qa.check(f"{suffix}-DELIVERABLE-ASSIGNEE", True, deliverable_name)

    page.keyboard.press("Escape")
    page.wait_for_timeout(300)
    visible_button(page, "校验任务结构").click()
    wait_success(page, "任务结构校验通过")
    qa.check(f"{suffix}-TREE-VALIDATE", True, "任务结构校验通过")


def record_runtime_events(page: Page, runtime: dict) -> None:
    page.on("console", lambda message: runtime["console_errors"].append({
        "text": message.text,
        "url": message.location.get("url", ""),
    }) if message.type == "error" else None)
    page.on("requestfailed", lambda request: runtime["request_failures"].append({
        "url": request.url,
        "error": request.failure,
    }))
    page.on("pageerror", lambda error: runtime["page_errors"].append(str(error)))


def check_admin_pages(page: Page, qa: QaRun, base_url: str, prefix: str) -> None:
    pages = {
        "/research-group/group": "课题编码",
        "/research/framework": "框架名称",
        "/research/task": "添加一级任务",
        "/research/my-task": "交付成果",
        "/research/audit": "成果名称",
        "/research/archive": "归档时间",
        "/fund/budget": "项目总资金",
        "/fund/allocation": "拨付",
        "/fund/use": "使用",
    }
    for path, marker in pages.items():
        navigate(page, base_url, path)
        visible = page.get_by_text(marker, exact=False).first.is_visible()
        qa.check(f"{prefix}-PAGE-{path}", visible, marker)
        if path == "/research/audit":
            check_fixed_action_layout(
                page,
                qa,
                prefix,
                "AUDIT",
                ["查看", "通过", "退回"],
                {
                    "submissionId": -1,
                    "groupName": "布局回归课题",
                    "taskName": "布局回归任务",
                    "deliverableName": "布局回归成果",
                    "submissionName": "布局回归提交",
                    "submitTime": "2026-08-28T00:00:00.000+08:00",
                },
            )
        if path == "/research/archive":
            check_fixed_action_layout(
                page,
                qa,
                prefix,
                "ARCHIVE",
                ["资料", "取消审核"],
                {
                    "submissionId": -1,
                    "groupName": "布局回归课题",
                    "taskName": "布局回归任务",
                    "deliverableName": "布局回归成果",
                    "submissionName": "布局回归提交",
                    "submitUserName": "提交人",
                    "archiveUserName": "归档人",
                    "archiveTime": "2026-08-28T00:00:00.000+08:00",
                },
            )
        check_business_table_layout(page, qa, prefix, path)


def check_business_table_layout(page: Page, qa: QaRun, prefix: str, path: str) -> None:
    result = page.evaluate("""
        () => {
          const table = document.querySelector('.app-container > .el-table')
          if (!table) return { tableFound: false, actionRows: [], rawIsoDateTimes: [] }
          const fixedBody = table.querySelector('.el-table__fixed-right .el-table__fixed-body-wrapper')
          const body = fixedBody || table.querySelector(':scope > .el-table__body-wrapper')
          const rows = body ? Array.from(body.querySelectorAll('tbody > tr')).filter(row => {
            const box = row.getBoundingClientRect()
            return box.height > 0 && box.width > 0
          }) : []
          const actionRows = rows.map(row => {
            const rowBox = row.getBoundingClientRect()
            const buttons = Array.from(row.querySelectorAll('button')).filter(button => {
              const style = window.getComputedStyle(button)
              const box = button.getBoundingClientRect()
              return style.visibility !== 'hidden' && style.display !== 'none' && box.height > 0
            })
            const boxes = buttons.map(button => button.getBoundingClientRect())
            const centers = boxes.map(box => box.top + box.height / 2)
            return {
              labels: buttons.map(button => button.innerText.trim()),
              singleLine: centers.length < 2 || Math.max(...centers) - Math.min(...centers) <= 2,
              contained: boxes.every(box => box.top >= rowBox.top - 1 && box.bottom <= rowBox.bottom + 1)
            }
          }).filter(row => row.labels.length > 0)
          return {
            tableFound: true,
            actionRows,
            rawIsoDateTimes: (table.innerText.match(/\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:\\d{2})?/g) || [])
          }
        }
    """)
    safe_name = path.strip('/').replace('/', '-').upper()
    layout_valid = result["tableFound"] and all(
        row["singleLine"] and row["contained"] for row in result["actionRows"]
    )
    qa.check(
        f"{prefix}-{safe_name}-TABLE-ACTIONS-LAYOUT",
        layout_valid,
        json.dumps(result["actionRows"], ensure_ascii=False),
    )
    qa.check(
        f"{prefix}-{safe_name}-NO-RAW-ISO-DATETIME",
        not result["rawIsoDateTimes"],
        json.dumps(result["rawIsoDateTimes"], ensure_ascii=False),
    )


def check_fixed_action_layout(
    page: Page,
    qa: QaRun,
    prefix: str,
    page_name: str,
    expected_labels: list,
    fixture_row: dict,
) -> None:
    fixture_injected = False
    if page.locator(".el-table__fixed-right .el-table__fixed-body-wrapper tbody tr").count() == 0:
        fixture_injected = page.evaluate("""
            row => {
              const root = document.querySelector('.app-container')
              const view = root && root.__vue__
              if (!view) return false
              view.rows = [row]
              view.total = 1
              return true
            }
        """, fixture_row)
        if fixture_injected:
            page.locator(".el-table__fixed-right .el-table__fixed-body-wrapper tbody tr").wait_for(
                state="visible", timeout=5_000
            )
    result = page.evaluate("""
        () => {
          const rows = Array.from(document.querySelectorAll(
            '.el-table__fixed-right .el-table__fixed-body-wrapper tbody tr'
          )).filter(row => row.getBoundingClientRect().height > 0)
          return {
            rowCount: rows.length,
            rows: rows.map(row => {
              const rowBox = row.getBoundingClientRect()
              const buttons = Array.from(row.querySelectorAll('button')).filter(button => {
                const style = window.getComputedStyle(button)
                const box = button.getBoundingClientRect()
                return style.visibility !== 'hidden' && style.display !== 'none' && box.height > 0
              })
              const boxes = buttons.map(button => button.getBoundingClientRect())
              const centers = boxes.map(box => box.top + box.height / 2)
              return {
                buttonCount: buttons.length,
                labels: buttons.map(button => button.innerText.trim()),
                singleLine: centers.length > 0 && Math.max(...centers) - Math.min(...centers) <= 2,
                contained: boxes.every(box => box.top >= rowBox.top - 1 && box.bottom <= rowBox.bottom + 1)
              }
            })
          }
        }
    """)
    result["fixtureInjected"] = fixture_injected
    valid = result["rowCount"] > 0 and all(
        row["buttonCount"] == len(expected_labels)
        and row["labels"] == expected_labels
        and row["singleLine"]
        and row["contained"]
        for row in result["rows"]
    )
    qa.check(f"{prefix}-{page_name}-ACTIONS-VISIBLE-SINGLE-LINE", valid, json.dumps(result, ensure_ascii=False))


def check_fund_record_button(
    page: Page,
    qa: QaRun,
    base_url: str,
    prefix: str,
    path: str,
    plan_field: str,
    records_field: str,
    open_field: str,
    plan: dict,
    dialog_marker: str,
    button_text: str,
) -> None:
    navigate(page, base_url, path)
    injected = page.evaluate("""
        data => {
          const root = document.querySelector('.app-container')
          const view = root && root.__vue__
          if (!view) return false
          view[data.planField] = data.plan
          view[data.recordsField] = []
          view[data.openField] = true
          return true
        }
    """, {
        "planField": plan_field,
        "recordsField": records_field,
        "openField": open_field,
        "plan": plan,
    })
    dialog = page.locator(".el-dialog:visible").filter(has_text=dialog_marker).last
    if injected:
        dialog.wait_for(state="visible", timeout=5_000)
    allowed_visible = injected and visible_button(dialog, button_text).is_visible()
    qa.check(f"{prefix}-{button_text}-CAPABILITY-ALLOWED", allowed_visible, f"injected={injected}")

    page.evaluate("""
        data => {
          const root = document.querySelector('.app-container')
          const view = root && root.__vue__
          view[data.planField].canSubmitRecord = false
        }
    """, {"planField": plan_field})
    page.wait_for_timeout(100)
    denied_hidden = dialog.locator("button:visible").filter(has_text=button_text).count() == 0
    qa.check(f"{prefix}-{button_text}-CAPABILITY-DENIED", denied_hidden, f"hidden={denied_hidden}")


def check_role_navigation(browser, qa: QaRun, base_url: str, browser_name: str) -> None:
    cases = [
        ("a_leader", ["年度任务", "任务清单", "未审资料", "使用管理"], ["课题管理"]),
        ("a_member", ["我的任务", "拨付管理", "使用管理"], ["课题管理"]),
        ("alloc_user", ["拨付管理"], ["使用管理"]),
        ("outsider", ["拨付管理"], ["使用管理", "任务调度"]),
    ]
    for username, expected, forbidden in cases:
        context = browser.new_context(viewport=VIEWPORTS["desktop"])
        page = context.new_page()
        try:
            login(page, username, base_url)
            menu_text = "\n".join(page.locator(".el-menu-item, .el-submenu__title").all_inner_texts())
            for item in expected:
                qa.check(f"{browser_name}-{username}-MENU-{item}", item in menu_text, menu_text)
            for item in forbidden:
                qa.check(f"{browser_name}-{username}-NO-MENU-{item}", item not in menu_text, menu_text)
            if username == "a_member":
                check_fund_record_button(
                    page, qa, base_url, f"{browser_name}-{username}", "/fund/use",
                    "recordPlan", "records", "recordsOpen",
                    {"usePlanId": -1, "useName": "权限渲染测试", "status": "0",
                     "forceFinish": "0", "canSubmitRecord": True},
                    "使用记录", "提交使用",
                )
            if username == "alloc_user":
                check_fund_record_button(
                    page, qa, base_url, f"{browser_name}-{username}", "/fund/allocation",
                    "recordPlan", "records", "recordsOpen",
                    {"planId": -1, "allocationName": "权限渲染测试", "status": "0",
                     "canSubmitRecord": True},
                    "拨付记录", "提交拨付",
                )
            if username == "outsider":
                navigate(page, base_url, "/fund/allocation")
                qa.check(f"{browser_name}-OUTSIDER-ALLOCATION-PAGE", page.get_by_text("拨付", exact=False).first.is_visible(), page.url)
                page.goto(base_url.rstrip("/") + "/fund/use", wait_until="networkidle")
                qa.check(f"{browser_name}-OUTSIDER-USE-ROUTE-DENIED", "/fund/use" not in page.url, page.url)
        except (AssertionError, PlaywrightError, RuntimeError) as error:
            qa.check(f"{browser_name}-{username}-UNHANDLED", False, repr(error))
        finally:
            context.close()


def main() -> None:
    sys.stdout.reconfigure(encoding="utf-8")
    parser = argparse.ArgumentParser(description="RuoYi Chrome/Edge end-to-end regression")
    parser.add_argument("--browser", choices=["all", *BROWSERS], default="all")
    parser.add_argument("--base-url", default=BASE_URL)
    parser.add_argument("--skip-mutations", action="store_true")
    args = parser.parse_args()

    qa = QaRun()
    artifact_dir = Path(r"D:\ruoyi\dev-tools\runtime\e2e-artifacts")
    artifact_dir.mkdir(parents=True, exist_ok=True)
    browser_names = list(BROWSERS) if args.browser == "all" else [args.browser]
    run_stamp = time.strftime("%Y%m%d%H%M%S")
    runtime_report = {}

    with sync_playwright() as playwright:
        for browser_name in browser_names:
            browser = playwright.chromium.launch(headless=True, executable_path=BROWSERS[browser_name])
            runtime_report[browser_name] = {}
            try:
                for viewport_name, viewport in VIEWPORTS.items():
                    runtime = {"console_errors": [], "request_failures": [], "page_errors": []}
                    runtime_report[browser_name][viewport_name] = runtime
                    context = browser.new_context(viewport=viewport, accept_downloads=True)
                    page = context.new_page()
                    record_runtime_events(page, runtime)
                    prefix = f"{browser_name}-{viewport_name}"
                    try:
                        login(page, "admin", args.base_url)
                        qa.check(f"{prefix}-LOGIN", "/index" in page.url, page.url)
                        menu_text = "\n".join(page.locator(".el-menu-item, .el-submenu__title").all_inner_texts())
                        qa.check(f"{prefix}-ADMIN-MENUS", all(item in menu_text for item in ["课题管理", "任务调度", "资金管理"]), menu_text)
                        check_admin_pages(page, qa, args.base_url, prefix)
                        layout = page.evaluate("({width: document.documentElement.scrollWidth, viewport: window.innerWidth})")
                        qa.check(f"{prefix}-NO-PAGE-HORIZONTAL-OVERFLOW", layout["width"] <= layout["viewport"] + 1, str(layout))
                        if viewport_name == "desktop" and not args.skip_mutations:
                            create_research_workflow(page, qa, args.base_url, f"{browser_name}-{run_stamp}")
                        page.screenshot(path=str(artifact_dir / f"e2e-{prefix}-{run_stamp}.png"), full_page=True)
                    except (AssertionError, PlaywrightError, RuntimeError) as error:
                        qa.check(f"{prefix}-UNHANDLED", False, repr(error))
                        page.screenshot(path=str(artifact_dir / f"e2e-failure-{prefix}-{run_stamp}.png"), full_page=True)
                    finally:
                        context.close()
                    qa.check(f"{prefix}-NO-PAGE-ERROR", not runtime["page_errors"], json.dumps(runtime["page_errors"], ensure_ascii=False))
                check_role_navigation(browser, qa, args.base_url, browser_name)
            finally:
                browser.close()

    report = {
        "status": "PASS" if all(item["status"] == "PASS" for item in qa.results) else "FAIL",
        "total": len(qa.results),
        "passed": sum(item["status"] == "PASS" for item in qa.results),
        "failed": [item for item in qa.results if item["status"] == "FAIL"],
        "runtime": runtime_report,
    }
    report_path = artifact_dir / f"browser-e2e-{run_stamp}.json"
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))
    print(f"report={report_path}")
    if report["status"] != "PASS":
        raise SystemExit(1)


if __name__ == "__main__":
    main()
