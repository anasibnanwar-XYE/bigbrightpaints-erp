#!/usr/bin/env python3
"""M9 runtime/MailHog quota proof harness.

This harness is intentionally evidence-oriented: it prints statuses, trace IDs,
safe quota metadata, MailHog message IDs/subjects, and rate-limit headers only.
It never prints bearer tokens, passwords, activation links, reset links, or
environment-file values.
"""

from __future__ import annotations

import json
import os
import sys
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from dataclasses import dataclass
from typing import Any


APP_BASE = "http://localhost:8081"
MGMT_BASE = "http://localhost:9090"
MAILHOG_BASE = "http://localhost:8025"
SUPERADMIN_EMAIL = "validation.superadmin@example.com"
TENANT_EMAIL = "validation.admin@example.com"
PLATFORM_CODE = "PLATFORM"
TENANT_CODE = "MOCK"
TENANT_INVOICE_NUMBER = "VAL-MOCK-INV-001"
REQUIRED_DIMENSIONS = ("PDF_EXPORTS", "EMAILS", "JOBS", "STORAGE")
OVERRIDE_KEYS = {
    "PDF_EXPORTS": "maxPdfExports",
    "EMAILS": "maxEmails",
    "JOBS": "maxJobs",
    "STORAGE": "maxStorageBytes",
}
BLOCK_STATUSES = {400, 403, 409, 422, 429}


@dataclass
class HttpResult:
  status: int
  body: Any
  headers: dict[str, str]


def note(message: str) -> None:
  print(f"[m9-runtime-mailhog-quota-proof] {message}", flush=True)


def fail(message: str) -> None:
  print(f"[m9-runtime-mailhog-quota-proof] ERROR: {message}", file=sys.stderr, flush=True)
  raise SystemExit(1)


def dry_run() -> None:
  note("dry_run=true")
  note(f"approved_ports app=8081 management=9090 mailhog=8025")
  note("runtime_profile=prod,flyway-v2,mock,validation-seed")
  note(
      "coverage=real-actions:PDF_EXPORTS(invoice-pdf),EMAILS(invoice-business-email/MailHog),"
      "JOBS(orchestrator-command),STORAGE(catalog-import),"
      "API_CALLS(monthly),BURST_REQUESTS_PER_MINUTE,MAX_CONCURRENT_REQUESTS")
  note("dry_run_real_action_plan=success-delta,exhausted-block,blocked-usage-unchanged")
  note("auth_evidence=token-present markers only; passwords and bearer tokens are never printed")
  note("mailhog_evidence=HTTP status, message count, IDs, subjects, recipients only")
  note("rate_limit_evidence=HTTP 429 plus Retry-After/X-RateLimit-* headers and safe error code")


def json_bytes(payload: Any) -> bytes:
  return json.dumps(payload, separators=(",", ":")).encode("utf-8")


def parse_body(raw: bytes, content_type: str) -> Any:
  if not raw:
    return None
  if "json" in content_type.lower():
    return json.loads(raw.decode("utf-8"))
  return {"bytes": len(raw)}


def request(
    method: str,
    url: str,
    *,
    token: str | None = None,
    company_code: str | None = None,
    payload: Any | None = None,
    headers: dict[str, str] | None = None,
    expected: set[int] | None = None,
    timeout: int = 20,
) -> HttpResult:
  req_headers = dict(headers or {})
  if token:
    req_headers["Authorization"] = f"Bearer {token}"
  if company_code:
    req_headers["X-Company-Code"] = company_code
  data = None
  if payload is not None:
    req_headers["Content-Type"] = "application/json"
    data = json_bytes(payload)
  req = urllib.request.Request(url, data=data, headers=req_headers, method=method)
  try:
    with urllib.request.urlopen(req, timeout=timeout) as response:
      raw = response.read()
      result = HttpResult(
          response.status,
          parse_body(raw, response.headers.get("Content-Type", "")),
          {key.lower(): value for key, value in response.headers.items()},
      )
  except urllib.error.HTTPError as exc:
    raw = exc.read()
    result = HttpResult(
        exc.code,
        parse_body(raw, exc.headers.get("Content-Type", "")),
        {key.lower(): value for key, value in exc.headers.items()},
    )
  if expected is not None and result.status not in expected:
    fail(f"{method} {safe_url(url)} expected {sorted(expected)} got {result.status}; body={safe_body(result.body)}")
  return result


def multipart_request(
    method: str,
    url: str,
    *,
    token: str,
    company_code: str,
    field_name: str,
    file_name: str,
    content_type: str,
    content: bytes,
    headers: dict[str, str] | None = None,
    expected: set[int] | None = None,
    timeout: int = 60,
) -> HttpResult:
  boundary = "----m9quota" + uuid.uuid4().hex
  body = (
      f"--{boundary}\r\n"
      f'Content-Disposition: form-data; name="{field_name}"; filename="{file_name}"\r\n'
      f"Content-Type: {content_type}\r\n\r\n").encode("utf-8")
  body += content
  body += f"\r\n--{boundary}--\r\n".encode("utf-8")
  req_headers = dict(headers or {})
  req_headers["Authorization"] = f"Bearer {token}"
  req_headers["X-Company-Code"] = company_code
  req_headers["Content-Type"] = f"multipart/form-data; boundary={boundary}"
  req_headers["Content-Length"] = str(len(body))
  req = urllib.request.Request(url, data=body, headers=req_headers, method=method)
  try:
    with urllib.request.urlopen(req, timeout=timeout) as response:
      raw = response.read()
      result = HttpResult(
          response.status,
          parse_body(raw, response.headers.get("Content-Type", "")),
          {key.lower(): value for key, value in response.headers.items()},
      )
  except urllib.error.HTTPError as exc:
    raw = exc.read()
    result = HttpResult(
        exc.code,
        parse_body(raw, exc.headers.get("Content-Type", "")),
        {key.lower(): value for key, value in exc.headers.items()},
    )
  if expected is not None and result.status not in expected:
    fail(f"{method} {safe_url(url)} expected {sorted(expected)} got {result.status}; body={safe_body(result.body)}")
  return result


def safe_url(url: str) -> str:
  parsed = urllib.parse.urlsplit(url)
  query = urllib.parse.parse_qsl(parsed.query, keep_blank_values=True)
  redacted = [(k, "<redacted>" if "token" in k.lower() else v) for k, v in query]
  return urllib.parse.urlunsplit(
      (parsed.scheme, parsed.netloc, parsed.path, urllib.parse.urlencode(redacted), ""))


def safe_body(body: Any) -> Any:
  if not isinstance(body, dict):
    return body
  safe: dict[str, Any] = {}
  for key in ("success", "message", "errorCode", "reason", "reasonCode", "traceId"):
    if key in body:
      safe[key] = body[key]
  data = body.get("data")
  if isinstance(data, dict):
    safe["dataKeys"] = sorted(data.keys())
    for key in ("code", "decision", "accepted", "dimension", "reasonCode", "message", "limitType"):
      if key in data:
        safe[key] = data[key]
    details = data.get("details")
    if isinstance(details, dict):
      for key in ("dimension", "reasonCode", "usedBefore", "requestedUnits", "limit", "stateBefore"):
        if key in details:
          safe[key] = details[key]
  return safe or {"keys": sorted(body.keys())}


def wait_for_runtime() -> None:
  deadline = time.time() + 180
  last_status = "000"
  while time.time() < deadline:
    try:
      auth = request("GET", f"{APP_BASE}/api/v1/auth/me", expected={200, 401, 403}, timeout=5)
      last_status = str(auth.status)
      note(f"api_reachable auth_me_anon_status={auth.status}")
      return
    except Exception as exc:  # noqa: BLE001 - bounded readiness polling
      last_status = exc.__class__.__name__
      time.sleep(2)
  fail(f"backend did not become API-reachable; last={last_status}")


def login(email: str, company_code: str, password: str) -> str:
  response = request(
      "POST",
      f"{APP_BASE}/api/v1/auth/login",
      payload={"email": email, "password": password, "companyCode": company_code},
      expected={200},
  )
  token = response.body.get("accessToken") if isinstance(response.body, dict) else None
  if not token:
    fail(f"login failed to return token marker for actor={email}")
  note(f"login actor={email} company={company_code} token=present(redacted)")
  return token


def response_trace(result: HttpResult) -> str:
  if isinstance(result.body, dict):
    metadata = result.body.get("metadata")
    if isinstance(metadata, dict):
      for key in ("traceId", "correlationId", "requestId"):
        if metadata.get(key):
          return str(metadata[key])
    for key in ("traceId", "correlationId", "requestId"):
      if result.body.get(key):
        return str(result.body[key])
  return result.headers.get("x-request-id") or result.headers.get("x-correlation-id") or "n/a"


def get_data(result: HttpResult) -> Any:
  if not isinstance(result.body, dict) or "data" not in result.body:
    fail(f"expected ApiResponse data, got {safe_body(result.body)}")
  return result.body["data"]


def tenant_id(super_token: str) -> int:
  query = urllib.parse.urlencode({"q": TENANT_CODE, "page": 0, "size": 10, "sort": "companyCode,asc"})
  result = request(
      "GET",
      f"{APP_BASE}/api/v1/superadmin/tenants?{query}",
      token=super_token,
      company_code=PLATFORM_CODE,
      expected={200},
  )
  content = get_data(result).get("content", [])
  for tenant in content:
    if tenant.get("companyCode") == TENANT_CODE:
      company_id = int(tenant["companyId"])
      note(f"tenant_fixture companyCode={TENANT_CODE} companyId={company_id}")
      return company_id
  fail(f"seeded tenant {TENANT_CODE} not found")


def dimension_used(usage: dict[str, Any], dimension: str) -> int:
  for item in usage.get("dimensions", []):
    if item.get("dimension") == dimension:
      return int(item.get("used", 0))
  return 0


def rate_limit_headers(result: HttpResult) -> dict[str, str]:
  wanted = ("retry-after", "x-ratelimit-limit", "x-ratelimit-remaining", "x-ratelimit-reset")
  return {key: result.headers[key] for key in wanted if key in result.headers}


def mailhog_summary() -> tuple[int, list[dict[str, Any]]]:
  result = request("GET", f"{MAILHOG_BASE}/api/v2/messages", expected={200})
  body = result.body if isinstance(result.body, dict) else {}
  items = body.get("items", [])
  summary = []
  for item in items[:5]:
    content = item.get("Content", {}) if isinstance(item, dict) else {}
    headers = content.get("Headers", {}) if isinstance(content, dict) else {}
    summary.append(
        {
            "id": item.get("ID"),
            "subject": (headers.get("Subject") or [""])[0],
            "to": (headers.get("To") or [""])[0],
        })
  return int(body.get("total", len(items))), summary


def mailhog_snapshot() -> tuple[int, set[str], list[dict[str, Any]]]:
  result = request("GET", f"{MAILHOG_BASE}/api/v2/messages", expected={200})
  body = result.body if isinstance(result.body, dict) else {}
  items = body.get("items", [])
  ids: set[str] = set()
  summary = []
  for item in items:
    item_id = item.get("ID")
    if item_id:
      ids.add(str(item_id))
    content = item.get("Content", {}) if isinstance(item, dict) else {}
    headers = content.get("Headers", {}) if isinstance(content, dict) else {}
    summary.append(
        {
            "id": item_id,
            "subject": (headers.get("Subject") or [""])[0],
            "to": (headers.get("To") or [""])[0],
        })
  return int(body.get("total", len(items))), ids, summary


def wait_for_mailhog_delta(
    before_count: int, before_ids: set[str], *, expected_delta: int = 1
) -> tuple[int, set[str], list[dict[str, Any]]]:
  deadline = time.time() + 60
  latest = mailhog_snapshot()
  while time.time() < deadline:
    count, ids, _ = latest
    if count >= before_count + expected_delta and len(ids - before_ids) >= expected_delta:
      return latest
    time.sleep(0.5)
    latest = mailhog_snapshot()
  return latest


def put_limits(super_token: str, company_id: int, limits: dict[str, Any]) -> None:
  request(
      "PUT",
      f"{APP_BASE}/api/v1/superadmin/tenants/{company_id}/limits",
      token=super_token,
      company_code=PLATFORM_CODE,
      payload=limits,
      expected={200},
  )


def put_limit_overrides(super_token: str, company_id: int, limits: dict[str, int], reason: str) -> None:
  request(
      "PUT",
      f"{APP_BASE}/api/v1/superadmin/tenants/{company_id}/entitlements/overrides",
      token=super_token,
      company_code=PLATFORM_CODE,
      payload={"limits": limits, "features": {}, "reason": reason},
      expected={200},
  )


def remove_or_restore_override(
    super_token: str, company_id: int, key: str, original_value: int | None) -> None:
  if original_value is None:
    request(
        "DELETE",
        f"{APP_BASE}/api/v1/superadmin/tenants/{company_id}/entitlements/overrides/{key}",
        token=super_token,
        company_code=PLATFORM_CODE,
        payload={"reason": "m9-runtime-quota-proof-restore"},
        expected={200, 400},
    )
  else:
    put_limit_overrides(
        super_token,
        company_id,
        {key: original_value},
        "m9-runtime-quota-proof-restore",
    )


def tenant_get(path: str, tenant_token: str, expected: set[int]) -> HttpResult:
  return request(
      "GET",
      f"{APP_BASE}{path}",
      token=tenant_token,
      company_code=TENANT_CODE,
      expected=expected,
      timeout=60,
  )


def tenant_post(
    path: str,
    tenant_token: str,
    *,
    payload: Any | None = None,
    headers: dict[str, str] | None = None,
    expected: set[int],
    timeout: int = 60,
) -> HttpResult:
  return request(
      "POST",
      f"{APP_BASE}{path}",
      token=tenant_token,
      company_code=TENANT_CODE,
      payload=payload,
      headers=headers,
      expected=expected,
      timeout=timeout,
  )


def usage_snapshot(super_token: str, company_id: int) -> dict[str, Any]:
  return get_data(
      request(
          "GET",
          f"{APP_BASE}/api/v1/superadmin/tenants/{company_id}/usage",
          token=super_token,
          company_code=PLATFORM_CODE,
          expected={200},
      ))


def dimension_value(super_token: str, company_id: int, dimension: str) -> int:
  return dimension_used(usage_snapshot(super_token, company_id), dimension)


def set_real_action_limit(super_token: str, company_id: int, dimension: str, value: int) -> None:
  put_limit_overrides(
      super_token,
      company_id,
      {OVERRIDE_KEYS[dimension]: max(1, int(value))},
      f"m9-runtime-quota-proof-{dimension.lower()}",
  )


def set_runtime_limits(
    super_token: str,
    company_id: int,
    original_limits: dict[str, Any],
    *,
    api_limit: int,
    burst_limit: int = 100,
    concurrency_limit: int = 10,
) -> None:
  limits = dict(original_limits)
  limits.update(
      {
          "quotaMaxActiveUsers": max(int(original_limits.get("quotaMaxActiveUsers", 0)), 500),
          "quotaMaxApiRequests": max(1, int(api_limit)),
          "quotaMaxStorageBytes": max(int(original_limits.get("quotaMaxStorageBytes", 0)), 1024),
          "quotaMaxConcurrentRequests": max(1, int(concurrency_limit)),
          "burstRequestsPerMinute": max(1, int(burst_limit)),
          "quotaSoftLimitEnabled": True,
          "quotaHardLimitEnabled": True,
      })
  put_limits(super_token, company_id, limits)


def assert_real_action_increment(
    super_token: str,
    company_id: int,
    dimension: str,
    action_name: str,
    action,
    *,
    minimum_delta: int = 1,
) -> HttpResult:
  before = dimension_value(super_token, company_id, dimension)
  set_real_action_limit(super_token, company_id, dimension, before + max(minimum_delta, 1) + 5)
  result = action()
  after = dimension_value(super_token, company_id, dimension)
  delta = after - before
  if delta < minimum_delta:
    fail(
        f"{action_name} expected {dimension} delta >= {minimum_delta}, "
        f"usedBefore={before} usedAfter={after} status={result.status}")
  note(
      f"real_action={action_name} dimension={dimension} status={result.status} "
      f"usedBefore={before} usedAfter={after} delta={delta} trace={response_trace(result)}")
  return result


def assert_exhausted_blocks(
    super_token: str,
    company_id: int,
    dimension: str,
    action_name: str,
    action,
    *,
    before_mailhog_ids: set[str] | None = None,
) -> HttpResult:
  set_real_action_limit(super_token, company_id, dimension, 1)
  previous_used = dimension_value(super_token, company_id, dimension)
  previous_mailhog_count, previous_mailhog_ids, _ = mailhog_snapshot()
  if before_mailhog_ids is not None:
    previous_mailhog_ids = set(previous_mailhog_ids)
  for attempt in range(1, 8):
    result = action()
    current_used = dimension_value(super_token, company_id, dimension)
    current_mailhog_count, current_mailhog_ids, _ = mailhog_snapshot()
    if result.status in BLOCK_STATUSES:
      if current_used != previous_used:
        fail(
            f"{action_name} blocked {dimension} changed usage: "
            f"usedBefore={previous_used} usedAfter={current_used}")
      mailhog_delta = current_mailhog_count - previous_mailhog_count
      if before_mailhog_ids is not None and current_mailhog_ids != previous_mailhog_ids:
        fail(f"{action_name} blocked attempt changed MailHog IDs")
      note(
          f"quota_block={action_name} dimension={dimension} status={result.status} "
          f"attempt={attempt} usedBefore={previous_used} usedAfter={current_used} "
          f"mailhogDelta={mailhog_delta} body={safe_body(result.body)} trace={response_trace(result)}")
      return result
    previous_used = current_used
    previous_mailhog_count = current_mailhog_count
    previous_mailhog_ids = current_mailhog_ids
  fail(f"{action_name} expected exhausted {dimension} block within bounded real-action attempts")


def find_invoice_id(tenant_token: str) -> int:
  result = tenant_get("/api/v1/invoices?page=0&size=100", tenant_token, {200})
  data = get_data(result)
  invoices = data if isinstance(data, list) else data.get("content", [])
  for invoice in invoices:
    if invoice.get("invoiceNumber") == TENANT_INVOICE_NUMBER:
      return int(invoice["id"])
  fail(f"seeded invoice {TENANT_INVOICE_NUMBER} not found; real PDF/email/concurrency proof cannot proceed")


def find_order_id(tenant_token: str) -> int:
  result = tenant_get("/api/v1/sales/orders?page=0&size=100", tenant_token, {200})
  data = get_data(result)
  orders = data if isinstance(data, list) else data.get("content", [])
  if not orders:
    fail("seeded sales order not found; real orchestrator job proof cannot proceed")
  order_id = int(orders[0]["id"])
  note(f"job_fixture salesOrderId={order_id} orderNumber={orders[0].get('orderNumber')}")
  return order_id


def storage_csv(run_marker: str) -> bytes:
  sku = "RM-M9-" + uuid.uuid4().hex[:10].upper()
  rows = [
      "brand,product_name,sku_code,category,unit_of_measure,base_price,gst_rate",
      f"M9 Proof Brand,{run_marker} storage import,{sku},RAW_MATERIAL,KG,10.00,18.00",
  ]
  return ("\n".join(rows) + "\n").encode("utf-8")


def main() -> None:
  if os.environ.get("M9_QUOTA_PROOF_DRY_RUN", "").lower() in {"1", "true", "yes"}:
    dry_run()
    return

  password = os.environ.get("ERP_VALIDATION_SEED_PASSWORD")
  if not password:
    fail("ERP_VALIDATION_SEED_PASSWORD must be set for full runtime proof; value is never printed")

  run_marker = "m9-runtime-mailhog-quota-proof-" + uuid.uuid4().hex[:10]
  note(f"run_marker_start={run_marker}")
  wait_for_runtime()
  health = request("GET", f"{MGMT_BASE}/actuator/health", expected={200, 503})
  readiness = request("GET", f"{MGMT_BASE}/actuator/health/readiness", expected={200, 503})
  mailhog_count, mailhog_items = mailhog_summary()
  note(f"health_status={health.status} readiness_status={readiness.status}")
  note(f"mailhog_http=200 message_count={mailhog_count} message_summary={json.dumps(mailhog_items, sort_keys=True)}")

  super_token = login(SUPERADMIN_EMAIL, PLATFORM_CODE, password)
  company_id = tenant_id(super_token)

  detail = get_data(
      request(
          "GET",
          f"{APP_BASE}/api/v1/superadmin/tenants/{company_id}",
          token=super_token,
          company_code=PLATFORM_CODE,
          expected={200},
      ))
  original_limits = dict(detail["limits"])
  usage = usage_snapshot(super_token, company_id)
  entitlements = get_data(
      request(
          "GET",
          f"{APP_BASE}/api/v1/superadmin/tenants/{company_id}/entitlements",
          token=super_token,
          company_code=PLATFORM_CODE,
          expected={200},
      ))
  original_overrides = {
      key: (
          int(value["tenantOverride"])
          if isinstance(value, dict) and value.get("tenantOverride") is not None
          else None
      )
      for key, value in entitlements.get("limits", {}).items()
      if key in OVERRIDE_KEYS.values()
  }
  api_used = dimension_used(usage, "API_CALLS")
  set_runtime_limits(
      super_token,
      company_id,
      original_limits,
      api_limit=api_used + 10000,
      burst_limit=1000,
      concurrency_limit=20,
  )
  tenant_token = login(TENANT_EMAIL, TENANT_CODE, password)
  invoice_id = find_invoice_id(tenant_token)
  order_id = find_order_id(tenant_token)
  note(f"pdf_email_fixture invoiceId={invoice_id} invoiceNumber={TENANT_INVOICE_NUMBER}")

  try:
    api_used = dimension_used(usage, "API_CALLS")
    set_runtime_limits(
        super_token,
        company_id,
        original_limits,
        api_limit=api_used + 10000,
        burst_limit=1000,
        concurrency_limit=20,
    )

    def invoice_pdf_action(expected: set[int] = {200, *BLOCK_STATUSES}) -> HttpResult:
      return tenant_get(f"/api/v1/invoices/{invoice_id}/pdf", tenant_token, expected)

    assert_real_action_increment(
        super_token,
        company_id,
        "PDF_EXPORTS",
        "invoice_pdf",
        lambda: invoice_pdf_action({200}),
    )
    assert_exhausted_blocks(
        super_token,
        company_id,
        "PDF_EXPORTS",
        "invoice_pdf",
        lambda: invoice_pdf_action({200, *BLOCK_STATUSES}),
    )

    # Email proof must isolate EMAILS from the PDF side-effect of rendering the attachment.
    set_real_action_limit(
        super_token,
        company_id,
        "PDF_EXPORTS",
        dimension_value(super_token, company_id, "PDF_EXPORTS") + 50,
    )

    def invoice_email_action(expected: set[int] = {200, *BLOCK_STATUSES}) -> HttpResult:
      return tenant_post(
          f"/api/v1/invoices/{invoice_id}/email",
          tenant_token,
          expected=expected,
          timeout=90,
      )

    mailhog_before_count, mailhog_before_ids, _ = mailhog_snapshot()
    email_result = assert_real_action_increment(
        super_token,
        company_id,
        "EMAILS",
        "invoice_business_email",
        lambda: invoice_email_action({200}),
    )
    mailhog_after_count, mailhog_after_ids, mailhog_after_summary = wait_for_mailhog_delta(
        mailhog_before_count, mailhog_before_ids)
    new_mail_ids = sorted(mailhog_after_ids - mailhog_before_ids)
    if mailhog_after_count != mailhog_before_count + 1 or len(new_mail_ids) != 1:
      fail(
          "invoice business email expected exactly one MailHog delivery: "
          f"before={mailhog_before_count} after={mailhog_after_count} newIds={new_mail_ids}")
    new_mail = next((item for item in mailhog_after_summary if str(item.get("id")) == new_mail_ids[0]), {})
    note(
        "mailhog_delivery=invoice_business_email "
        f"delta=1 messageId={new_mail.get('id')} subject={json.dumps(new_mail.get('subject'))} "
        f"to={json.dumps(new_mail.get('to'))} trace={response_trace(email_result)}")
    assert_exhausted_blocks(
        super_token,
        company_id,
        "EMAILS",
        "invoice_business_email",
        lambda: invoice_email_action({200, *BLOCK_STATUSES}),
        before_mailhog_ids=mailhog_after_ids,
    )

    def job_action(idempotency_key: str, note_suffix: str, expected: set[int]) -> HttpResult:
      headers = {
          "Idempotency-Key": idempotency_key,
          "X-Request-Id": "m9-job-" + uuid.uuid4().hex[:12],
      }
      return tenant_post(
          f"/api/v1/orchestrator/orders/{order_id}/fulfillment",
          tenant_token,
          payload={"status": "PROCESSING", "notes": f"{run_marker}-{note_suffix}"},
          headers=headers,
          expected=expected,
      )

    job_before = dimension_value(super_token, company_id, "JOBS")
    set_real_action_limit(super_token, company_id, "JOBS", job_before + 10)
    replay_key = "m9-job-" + uuid.uuid4().hex
    first_job = job_action(replay_key, "idempotent-replay", {202})
    job_after_first = dimension_value(super_token, company_id, "JOBS")
    if job_after_first - job_before < 1:
      fail(f"orchestrator job expected JOBS increment, before={job_before} after={job_after_first}")
    second_job = job_action(replay_key, "idempotent-replay", {202})
    job_after_replay = dimension_value(super_token, company_id, "JOBS")
    if job_after_replay != job_after_first:
      fail(
          f"orchestrator idempotent replay double-counted JOBS: "
          f"afterFirst={job_after_first} afterReplay={job_after_replay}")
    note(
        f"real_action=orchestrator_job dimension=JOBS status={first_job.status} "
        f"usedBefore={job_before} usedAfter={job_after_first} delta={job_after_first - job_before} "
        f"replayStatus={second_job.status} replayUsedAfter={job_after_replay} "
        f"trace={response_trace(first_job)}")
    assert_exhausted_blocks(
        super_token,
        company_id,
        "JOBS",
        "orchestrator_job",
        lambda: job_action("m9-job-" + uuid.uuid4().hex, "blocked-probe", {202, *BLOCK_STATUSES}),
    )

    def storage_import_action(expected: set[int]) -> tuple[HttpResult, int]:
      csv = storage_csv(run_marker)
      result = multipart_request(
          "POST",
          f"{APP_BASE}/api/v1/catalog/import",
          token=tenant_token,
          company_code=TENANT_CODE,
          field_name="file",
          file_name="m9-storage-proof.csv",
          content_type="text/csv",
          content=csv,
          headers={"Idempotency-Key": "m9-storage-" + uuid.uuid4().hex},
          expected=expected,
      )
      return result, len(csv)

    storage_before = dimension_value(super_token, company_id, "STORAGE")
    storage_probe_csv = storage_csv(run_marker)
    set_real_action_limit(super_token, company_id, "STORAGE", storage_before + len(storage_probe_csv) + 1024)
    storage_result = multipart_request(
        "POST",
        f"{APP_BASE}/api/v1/catalog/import",
        token=tenant_token,
        company_code=TENANT_CODE,
        field_name="file",
        file_name="m9-storage-proof.csv",
        content_type="text/csv",
        content=storage_probe_csv,
        headers={"Idempotency-Key": "m9-storage-" + uuid.uuid4().hex},
        expected={200},
    )
    storage_after = dimension_value(super_token, company_id, "STORAGE")
    storage_delta = storage_after - storage_before
    if storage_delta < len(storage_probe_csv):
      fail(
          f"catalog import expected STORAGE byte delta >= {len(storage_probe_csv)}, "
          f"usedBefore={storage_before} usedAfter={storage_after}")
    note(
        f"real_action=catalog_import_storage dimension=STORAGE status={storage_result.status} "
        f"usedBefore={storage_before} usedAfter={storage_after} delta={storage_delta} "
        f"bytes={len(storage_probe_csv)} trace={response_trace(storage_result)}")
    set_real_action_limit(super_token, company_id, "STORAGE", 1)
    storage_block_before = dimension_value(super_token, company_id, "STORAGE")
    blocked_storage, blocked_storage_bytes = storage_import_action({200, *BLOCK_STATUSES})
    storage_block_after = dimension_value(super_token, company_id, "STORAGE")
    if blocked_storage.status not in BLOCK_STATUSES:
      fail(f"catalog import storage expected exhausted quota block, got {blocked_storage.status}")
    if storage_block_after != storage_block_before:
      fail(
          f"catalog import blocked STORAGE changed usage: "
          f"before={storage_block_before} after={storage_block_after}")
    note(
        f"quota_block=catalog_import_storage dimension=STORAGE status={blocked_storage.status} "
        f"bytes={blocked_storage_bytes} usedBefore={storage_block_before} "
        f"usedAfter={storage_block_after} body={safe_body(blocked_storage.body)} "
        f"trace={response_trace(blocked_storage)}")

    api_used = dimension_value(super_token, company_id, "API_CALLS")
    set_runtime_limits(
        super_token,
        company_id,
        original_limits,
        api_limit=1,
        burst_limit=1000,
        concurrency_limit=20,
    )
    monthly = None
    for _ in range(8):
      candidate = tenant_get("/api/v1/auth/me", tenant_token, {200, 429})
      if candidate.status == 429:
        monthly = candidate
        break
    if monthly is None:
      fail("monthly API quota expected deterministic 429 within bounded tenant requests")
    note(
        f"api_monthly_status=429 headers={json.dumps(rate_limit_headers(monthly), sort_keys=True)} "
        f"body={safe_body(monthly.body)}")

    api_used = dimension_value(super_token, company_id, "API_CALLS")
    set_runtime_limits(
        super_token,
        company_id,
        original_limits,
        api_limit=api_used + 10000,
        burst_limit=1,
        concurrency_limit=20,
    )
    burst_results = [tenant_get("/api/v1/auth/me", tenant_token, {200, 429}) for _ in range(6)]
    burst_429 = next((result for result in burst_results if result.status == 429), None)
    if burst_429 is None:
      fail("burst quota expected at least one 429")
    note(
        f"burst_status=429 headers={json.dumps(rate_limit_headers(burst_429), sort_keys=True)} "
        f"body={safe_body(burst_429.body)}")

    api_used = dimension_value(super_token, company_id, "API_CALLS")
    set_runtime_limits(
        super_token,
        company_id,
        original_limits,
        api_limit=api_used + 10000,
        burst_limit=1000,
        concurrency_limit=1,
    )
    set_real_action_limit(
        super_token,
        company_id,
        "PDF_EXPORTS",
        dimension_value(super_token, company_id, "PDF_EXPORTS") + 100,
    )
    results: list[HttpResult] = []
    lock = threading.Lock()

    def fetch_pdf() -> None:
      result = tenant_get(f"/api/v1/invoices/{invoice_id}/pdf", tenant_token, {200, 429})
      with lock:
        results.append(result)

    threads = [threading.Thread(target=fetch_pdf) for _ in range(16)]
    for thread in threads:
      thread.start()
    for thread in threads:
      thread.join()
    concurrency_429 = next((result for result in results if result.status == 429), None)
    if concurrency_429 is None:
      fail(f"concurrency quota expected 429, got statuses={[result.status for result in results]}")
    note(
        f"concurrency_pdf_probe=real fixture=invoice_pdf invoiceId={invoice_id} "
        f"statuses={[result.status for result in results]} "
        f"headers={json.dumps(rate_limit_headers(concurrency_429), sort_keys=True)} "
        f"body={safe_body(concurrency_429.body)}")
  finally:
    for key, original_value in original_overrides.items():
      remove_or_restore_override(super_token, company_id, key, original_value)
    put_limits(super_token, company_id, original_limits)

  note(f"run_marker_end={run_marker}")


if __name__ == "__main__":
  main()
