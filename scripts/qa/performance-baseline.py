import argparse
import concurrent.futures
import json
import queue
import random
import socket
import sys
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path


GROUP_A = 991000001
PASSWORD = "admin123"


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


def request_json(base_url: str, method: str, path: str, token: str = None, body=None, timeout: float = 10.0):
    data = None if body is None else json.dumps(body, ensure_ascii=False).encode("utf-8")
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    request = urllib.request.Request(base_url.rstrip("/") + path, data=data, headers=headers, method=method)
    started = time.perf_counter()
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            raw = response.read().decode("utf-8")
            parsed = json.loads(raw) if raw else None
            return response.status, parsed, (time.perf_counter() - started) * 1000
    except urllib.error.HTTPError as error:
        raw = error.read().decode("utf-8", errors="replace")
        try:
            parsed = json.loads(raw)
        except json.JSONDecodeError:
            parsed = {"raw": raw}
        return error.code, parsed, (time.perf_counter() - started) * 1000


def is_success(status: int, body) -> bool:
    return 200 <= status < 300 and (not isinstance(body, dict) or int(body.get("code", 200)) == 200)


def expect_success(base_url: str, method: str, path: str, token: str = None, body=None):
    status, response, elapsed = request_json(base_url, method, path, token, body)
    if not is_success(status, response):
        raise RuntimeError(f"{method} {path} failed: HTTP={status}, body={response}")
    return response, elapsed


def login(base_url: str, username: str) -> str:
    captcha, _ = expect_success(base_url, "GET", "/code")
    response, _ = expect_success(base_url, "POST", "/auth/login", body={
        "username": username,
        "password": PASSWORD,
        "code": captcha_answer(captcha["uuid"]),
        "uuid": captcha["uuid"],
    })
    return response["data"]["access_token"]


def list_item(base_url: str, path: str, token: str, field: str, expected: str, collection: str):
    response, _ = expect_success(base_url, "GET", path, token)
    for item in response.get(collection, []):
        if item.get(field) == expected:
            return item
    raise RuntimeError(f"Could not find {field}={expected} from {path}")


def setup_scenario(base_url: str, stamp: str):
    admin = login(base_url, "admin")
    leader = login(base_url, "a_leader")
    core = login(base_url, "a_core")
    expect_success(base_url, "POST", "/ruoyi-fund/budget", admin, {
        "topicId": GROUP_A,
        "totalAmount": "100000000.00",
        "planEndTime": "2026-12-31 23:59:59",
        "fundDesc": "50-user performance baseline",
    })
    framework_name = f"PERF年度任务-{stamp}"
    expect_success(base_url, "POST", "/ruoyi-research/framework", leader, {
        "groupId": GROUP_A,
        "frameworkName": framework_name,
        "year": 2026,
        "leadDeptId": 103,
        "overallGoal": "50-user performance baseline",
        "status": "0",
        "sort": 999,
        "units": [],
    })
    framework = list_item(
        base_url,
        "/ruoyi-research/framework/list?" + urllib.parse.urlencode({
            "frameworkName": framework_name,
            "pageNum": 1,
            "pageSize": 100,
        }),
        leader,
        "frameworkName",
        framework_name,
        "rows",
    )
    task_name = f"PERF任务-{stamp}"
    expect_success(base_url, "POST", "/ruoyi-research/task", leader, {
        "frameworkId": framework["frameworkId"],
        "groupId": GROUP_A,
        "parentId": 0,
        "level": 1,
        "taskName": task_name,
        "taskType": "PERF",
        "description": "Performance write target",
        "deadline": "2026-12-31",
        "sort": 1,
    })
    task = list_item(
        base_url,
        "/ruoyi-research/task/list?" + urllib.parse.urlencode({
            "frameworkId": framework["frameworkId"],
            "taskName": task_name,
        }),
        leader,
        "taskName",
        task_name,
        "data",
    )
    deliverable, _ = expect_success(base_url, "POST", "/ruoyi-research/deliverable", leader, {
        "groupId": GROUP_A,
        "taskId": task["taskId"],
        "deliverableName": f"PERF成果-{stamp}",
        "requirement": "Performance submissions",
        "requiredNum": 1000000,
        "deadline": "2026-12-31",
        "isRequired": "1",
        "sort": 1,
    })
    return {
        "leader": leader,
        "core": core,
        "framework_id": framework["frameworkId"],
        "deliverable_id": deliverable["data"]["deliverableId"],
    }


def percentile(values, ratio: float):
    if not values:
        return None
    ordered = sorted(values)
    index = min(len(ordered) - 1, max(0, int(len(ordered) * ratio + 0.999999) - 1))
    return round(ordered[index], 2)


def main() -> None:
    sys.stdout.reconfigure(encoding="utf-8")
    parser = argparse.ArgumentParser(description="50-user Gateway performance baseline")
    parser.add_argument("--base-url", default="http://127.0.0.1:8080")
    parser.add_argument("--users", type=int, default=50)
    parser.add_argument("--duration-seconds", type=int, default=900)
    parser.add_argument("--interval-seconds", type=float, default=1.0)
    args = parser.parse_args()

    stamp = time.strftime("%Y%m%d%H%M%S")
    scenario = setup_scenario(args.base_url, stamp)
    submission_ids = queue.Queue()
    results = []
    result_lock = threading.Lock()
    sequence = 0
    sequence_lock = threading.Lock()
    deadline = time.monotonic() + args.duration_seconds
    query_paths = [
        f"/ruoyi-research/task/list?frameworkId={scenario['framework_id']}",
        "/ruoyi-research/task/my",
        f"/ruoyi-research/submission/list?groupId={GROUP_A}&pageNum=1&pageSize=10",
        f"/ruoyi-fund/allocation/plan/list?topicId={GROUP_A}&pageNum=1&pageSize=10",
        f"/ruoyi-fund/allocation/overview/{GROUP_A}",
        f"/ruoyi-fund/use/plan/list?topicId={GROUP_A}&pageNum=1&pageSize=10",
    ]

    def next_sequence() -> int:
        nonlocal sequence
        with sequence_lock:
            sequence += 1
            return sequence

    def run_user(user_index: int) -> None:
        rng = random.Random(20260828 + user_index)
        while time.monotonic() < deadline:
            cycle_started = time.monotonic()
            roll = rng.random()
            category = "query"
            method = "GET"
            path = rng.choice(query_paths)
            body = None
            token = scenario["core"]
            submission_id = None
            if roll >= 0.8:
                category = "write"
                method = "POST"
                path = "/ruoyi-research/submission"
                item_sequence = next_sequence()
                body = {
                    "deliverableId": scenario["deliverable_id"],
                    "submissionName": f"PERF-SUB-{stamp}-{item_sequence}",
                    "submissionDesc": "50-user performance baseline draft",
                }
            elif roll >= 0.7:
                try:
                    submission_id = submission_ids.get_nowait()
                    category = "critical"
                    method = "PUT"
                    path = f"/ruoyi-research/submission/{submission_id}/submit"
                    body = {}
                except queue.Empty:
                    category = "query"

            started = time.perf_counter()
            error = ""
            status = 0
            response = None
            try:
                status, response, elapsed = request_json(args.base_url, method, path, token, body)
                success = is_success(status, response)
                if not success:
                    error = json.dumps(response, ensure_ascii=False)[:500]
                elif category == "write":
                    created = response.get("data") or {}
                    created_id = created.get("submissionId")
                    if created_id is None:
                        success = False
                        error = "Successful draft response did not contain submissionId"
                    else:
                        submission_ids.put(created_id)
            except Exception as exception:
                elapsed = (time.perf_counter() - started) * 1000
                success = False
                error = repr(exception)
            with result_lock:
                results.append({
                    "category": category,
                    "method": method,
                    "path": path,
                    "success": success,
                    "elapsed_ms": elapsed,
                    "status": status,
                    "error": error,
                })
            remaining = args.interval_seconds - (time.monotonic() - cycle_started)
            if remaining > 0:
                time.sleep(remaining)

    print(json.dumps({
        "event": "START",
        "users": args.users,
        "duration_seconds": args.duration_seconds,
        "interval_seconds": args.interval_seconds,
        "framework_id": scenario["framework_id"],
        "deliverable_id": scenario["deliverable_id"],
    }, ensure_ascii=False))
    started_at = time.monotonic()
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.users) as executor:
        futures = [executor.submit(run_user, index) for index in range(args.users)]
        for future in futures:
            future.result()
    duration = time.monotonic() - started_at

    categories = {}
    for category in ["query", "write", "critical"]:
        selected = [item for item in results if item["category"] == category]
        categories[category] = {
            "requests": len(selected),
            "failures": sum(not item["success"] for item in selected),
            "p95_ms": percentile([item["elapsed_ms"] for item in selected], 0.95),
            "max_ms": round(max((item["elapsed_ms"] for item in selected), default=0), 2),
        }
    failures = [item for item in results if not item["success"]]
    report = {
        "status": "PASS",
        "users": args.users,
        "duration_seconds": round(duration, 2),
        "requests": len(results),
        "throughput_rps": round(len(results) / duration, 2),
        "failures": len(failures),
        "error_rate_percent": round(len(failures) * 100 / len(results), 4) if results else 100,
        "categories": categories,
        "sample_errors": failures[:10],
        "thresholds": {
            "error_rate_percent_lt": 0.5,
            "query_p95_ms_lt": 1000,
            "write_p95_ms_lt": 1500,
            "critical_p95_ms_lt": 2000,
        },
    }
    report["status"] = "PASS" if (
        report["error_rate_percent"] < 0.5
        and categories["query"]["p95_ms"] is not None and categories["query"]["p95_ms"] < 1000
        and categories["write"]["p95_ms"] is not None and categories["write"]["p95_ms"] < 1500
        and categories["critical"]["p95_ms"] is not None and categories["critical"]["p95_ms"] < 2000
    ) else "FAIL"
    artifact_dir = Path(r"D:\ruoyi\dev-tools\runtime\performance-artifacts")
    artifact_dir.mkdir(parents=True, exist_ok=True)
    report_path = artifact_dir / f"performance-baseline-{stamp}.json"
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))
    print(f"report={report_path}")
    if report["status"] != "PASS":
        raise SystemExit(1)


if __name__ == "__main__":
    main()
