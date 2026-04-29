#!/usr/bin/env python3
"""M10 suspension async/access matrix runtime proof.

This harness prints only redacted, assertion-oriented evidence: HTTP statuses,
trace IDs, audit IDs, MailHog message IDs/subjects, and safe state labels.  It
never prints bearer tokens, refresh tokens, passwords, activation/reset links,
SMTP credentials, .env values, or tenant-private payloads.
"""

from __future__ import annotations

import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from typing import Any


APP_BASE = "http://localhost:8081"
MGMT_BASE = "http://localhost:9090"
MAILHOG_BASE = "http://localhost:8025"
REPO_ROOT = "/Users/anas/Documents/Factory/bigbrightpaints-erp_worktrees/super-admin-redesign"
SUPERADMIN_EMAIL = "validation.superadmin@example.com"
TENANT_EMAIL = "validation.admin@example.com"
PLATFORM_CODE = "PLATFORM"
TENANT_CODE = "MOCK"
TENANT_INVOICE_NUMBER = "VAL-MOCK-INV-001"
BLOCK_STATUSES = {400, 401, 403, 409, 422, 423, 429}
OVERRIDE_KEYS = {
    "PDF_EXPORTS": "maxPdfExports",
    "EMAILS": "maxEmails",
    "JOBS": "maxJobs",
    "STORAGE": "maxStorageBytes",
}


@dataclass
class HttpResult:
  status: int
  body: Any
  headers: dict[str, str]


@dataclass
class LoginTokens:
  access: str
  refresh: str | None


def note(message: str) -> None:
  print(f"[m10-suspension-async-proof] {message}", flush=True)


def fail(message: str) -> None:
  print(f"[m10-suspension-async-proof] ERROR: {message}", file=sys.stderr, flush=True)
  raise SystemExit(1)


def dry_run() -> None:
  note("dry_run=true")
  note("approved_ports app=8081 management=9090 mailhog=8025")
  note("runtime_profile=prod,flyway-v2,mock,validation-seed")
  note(
      "coverage=states:GRACE,SUSPENDED_READ_ONLY,SUSPENDED_BLOCKED,CANCELED,ARCHIVED;"
      "surfaces:login,refresh,auth_me,safe_read,write,pdf,email,mailhog,job_submit,"
      "already_accepted_trace,scheduled_lifecycle,quota_overrides,api_key_surface_guard")
  note("auth_evidence=token-present markers only; passwords and bearer/refresh tokens are never printed")
  note("mailhog_evidence=HTTP status, message count, message IDs, subjects, recipients only")


def json_bytes(payload: Any) -> bytes:
  return json.dumps(payload, separators=(",", ":")).encode("utf-8")


def parse_body(raw: bytes, content_type: str) -> Any:
  if not raw:
    return None
  if "json" in content_type.lower():
    return json.loads(raw.decode("utf-8"))
  return {"bytes": len(raw)}


def safe_url(url: str) -> str:
  parsed = urllib.parse.urlsplit(url)
  query = urllib.parse.parse_qsl(parsed.query, keep_blank_values=True)
  redacted = [(key, "<redacted>" if "token" in key.lower() else value) for key, value in query]
  return urllib.parse.urlunsplit(
      (parsed.scheme, parsed.netloc, parsed.path, urllib.parse.urlencode(redacted), ""))


def request(
    method: str,
    url: str,
    *,
    token: str | None = None,
    company_code: str | None = None,
    payload: Any | None = None,
    headers: dict[str, str] | None = None,
    expected: set[int] | None = None,
    timeout: int = 30,
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
    fail(
        f"{method} {safe_url(url)} expected {sorted(expected)} got {result.status}; "
        f"body={safe_body(result.body)}")
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
  boundary = "----m10suspension" + uuid.uuid4().hex
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
    fail(
        f"{method} {safe_url(url)} expected {sorted(expected)} got {result.status}; "
        f"body={safe_body(result.body)}")
  return result


def safe_body(body: Any) -> Any:
  if not isinstance(body, dict):
    return body
  safe: dict[str, Any] = {}
  for key in ("success", "message", "errorCode", "reason", "reasonCode", "traceId"):
    if key in body:
      safe[key] = body[key]
  metadata = body.get("metadata")
  if isinstance(metadata, dict):
    safe["metadataKeys"] = sorted(metadata.keys())
  data = body.get("data")
  if isinstance(data, dict):
    safe["dataKeys"] = sorted(data.keys())
    for key in (
        "code",
        "decision",
        "accepted",
        "dimension",
        "reasonCode",
        "commercialState",
        "lifecycleState",
        "runtimeState",
        "billingStatus",
    ):
      if key in data:
        safe[key] = data[key]
    details = data.get("details")
    if isinstance(details, dict):
      for key in ("dimension", "reasonCode", "commercialState", "lifecycleState"):
        if key in details:
          safe[key] = details[key]
  return safe or {"keys": sorted(body.keys())}


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


def wait_for_runtime() -> None:
  deadline = time.time() + 180
  last = "not-started"
  while time.time() < deadline:
    try:
      auth = request("GET", f"{APP_BASE}/api/v1/auth/me", expected={200, 401, 403}, timeout=5)
      note(f"api_reachable auth_me_anon_status={auth.status}")
      return
    except Exception as exc:  # noqa: BLE001 - bounded readiness polling
      last = exc.__class__.__name__
      time.sleep(2)
  fail(f"backend did not become API-reachable; last={last}")


def login(email: str, company_code: str, password: str, *, expected: set[int] = {200}) -> LoginTokens | None:
  response = request(
      "POST",
      f"{APP_BASE}/api/v1/auth/login",
      payload={"email": email, "password": password, "companyCode": company_code},
      expected=expected,
  )
  if response.status != 200:
    note(
        f"login actor={email} company={company_code} status={response.status} "
        f"body={safe_body(response.body)} trace={response_trace(response)}")
    return None
  token = response.body.get("accessToken") if isinstance(response.body, dict) else None
  if not token:
    fail(f"login failed to return token marker for actor={email}")
  refresh = response.body.get("refreshToken") if isinstance(response.body, dict) else None
  note(f"login actor={email} company={company_code} status=200 token=present(redacted)")
  return LoginTokens(str(token), str(refresh) if refresh else None)


def refresh(refresh_token: str | None, expected: set[int]) -> HttpResult:
  if not refresh_token:
    fail("refresh token marker was absent from login response")
  return request(
      "POST",
      f"{APP_BASE}/api/v1/auth/refresh-token",
      payload={"refreshToken": refresh_token, "companyCode": TENANT_CODE},
      expected=expected,
  )


def rotate_tokens_if_present(tokens: LoginTokens, result: HttpResult) -> None:
  if result.status != 200 or not isinstance(result.body, dict):
    return
  access = result.body.get("accessToken")
  refresh_token = result.body.get("refreshToken")
  if access:
    tokens.access = str(access)
  if refresh_token:
    tokens.refresh = str(refresh_token)


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


def tenant_id(super_token: str) -> int:
  query = urllib.parse.urlencode(
      {"q": TENANT_CODE, "page": 0, "size": 10, "sort": "companyCode,asc", "includeArchived": "true"})
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


def find_invoice_id(tenant_token: str) -> int:
  result = tenant_get("/api/v1/invoices?page=0&size=100", tenant_token, {200})
  data = get_data(result)
  invoices = data if isinstance(data, list) else data.get("content", [])
  for invoice in invoices:
    if invoice.get("invoiceNumber") == TENANT_INVOICE_NUMBER:
      invoice_id = int(invoice["id"])
      note(f"pdf_email_fixture invoiceId={invoice_id} invoiceNumber={TENANT_INVOICE_NUMBER}")
      return invoice_id
  fail(f"seeded invoice {TENANT_INVOICE_NUMBER} not found")


def find_order_id(tenant_token: str) -> int:
  result = tenant_get("/api/v1/sales/orders?page=0&size=100", tenant_token, {200})
  data = get_data(result)
  orders = data if isinstance(data, list) else data.get("content", [])
  if not orders:
    fail("seeded sales order not found; real orchestrator job proof cannot proceed")
  order_id = int(orders[0]["id"])
  note(f"job_fixture salesOrderId={order_id} orderNumber={orders[0].get('orderNumber')}")
  return order_id


def ensure_subscription(super_token: str, company_id: int) -> None:
  current = request(
      "GET",
      f"{APP_BASE}/api/v1/superadmin/tenants/{company_id}/billing/subscription",
      token=super_token,
      company_code=PLATFORM_CODE,
      expected={200, 400, 404},
  )
  if current.status == 200:
    data = get_data(current)
    note(
        "subscription_fixture=existing "
        f"subscriptionId={data.get('subscriptionId')} status={data.get('status')} "
        f"billingStatus={data.get('billingStatus')}")
    return
  now = datetime.now(timezone.utc)
  payload = {
      "planId": "STARTER",
      "status": "ACTIVE",
      "cadence": "MONTHLY",
      "amountMinorUnits": 10000,
      "currency": "INR",
      "collectionMode": "MANUAL",
      "periodStartAt": (now - timedelta(days=1)).isoformat().replace("+00:00", "Z"),
      "periodEndAt": (now + timedelta(days=30)).isoformat().replace("+00:00", "Z"),
      "renewalAt": (now + timedelta(days=30)).isoformat().replace("+00:00", "Z"),
      "dueAt": (now + timedelta(days=10)).isoformat().replace("+00:00", "Z"),
      "trialStartAt": (now - timedelta(days=1)).isoformat().replace("+00:00", "Z"),
      "trialEndAt": (now - timedelta(days=1)).isoformat().replace("+00:00", "Z"),
      "graceUntilAt": (now + timedelta(days=30)).isoformat().replace("+00:00", "Z"),
      "externalReference": "M10-ASYNC-" + uuid.uuid4().hex[:12],
      "reason": "M10 async access matrix fixture",
  }
  created = request(
      "POST",
      f"{APP_BASE}/api/v1/superadmin/tenants/{company_id}/billing/subscription",
      token=super_token,
      company_code=PLATFORM_CODE,
      payload=payload,
      expected={201, 409},
  )
  if created.status == 409:
    note("subscription_fixture=already-created-by-concurrent-or-prior-run")
    return
  data = get_data(created)
  note(
      "subscription_fixture=created "
      f"subscriptionId={data.get('subscriptionId')} auditEventId={data.get('auditEventId')}")


def post_commercial(
    super_token: str, company_id: int, action: str, payload: dict[str, Any], expected: set[int] = {200}
) -> HttpResult:
  return request(
      "POST",
      f"{APP_BASE}/api/v1/superadmin/tenants/{company_id}/{action}",
      token=super_token,
      company_code=PLATFORM_CODE,
      payload=payload,
      expected=expected,
  )


def set_state(super_token: str, company_id: int, state: str, run_marker: str) -> dict[str, Any]:
  now = datetime.now(timezone.utc)
  if state == "ACTIVE":
    current = get_commercial_state(super_token, company_id)
    if current.get("commercialState") in {"ACTIVE", "TRIAL_ACTIVE"}:
      note(
          "state_transition target=ACTIVE status=already-active "
          f"commercialState={current.get('commercialState')} "
          f"runtimeState={current.get('runtimeState')} billingStatus={current.get('billingStatus')}")
      return current
    action = "resume"
    payload = {"reason": f"{run_marker} restore active"}
  elif state == "GRACE":
    action = "suspension/grace"
    payload = {
        "reason": f"{run_marker} grace proof",
        "graceUntilAt": (now + timedelta(hours=2)).isoformat().replace("+00:00", "Z"),
    }
  elif state == "SUSPENDED_READ_ONLY":
    action = "suspension/read-only"
    payload = {
        "reason": f"{run_marker} read-only proof",
        "effectiveAt": now.isoformat().replace("+00:00", "Z"),
    }
  elif state == "SUSPENDED_BLOCKED":
    action = "suspension/blocked"
    payload = {
        "reason": f"{run_marker} blocked proof",
        "effectiveAt": now.isoformat().replace("+00:00", "Z"),
    }
  elif state == "CANCELED":
    action = "cancel"
    payload = {"reason": f"{run_marker} cancel proof"}
  elif state == "ARCHIVED":
    action = "archive"
    payload = {"reason": f"{run_marker} archive proof"}
  else:
    fail(f"unsupported state {state}")
  result = post_commercial(super_token, company_id, action, payload)
  data = get_data(result)
  note(
      f"state_transition target={state} status={result.status} "
      f"commercialState={data.get('commercialState')} lifecycleState={data.get('lifecycleState')} "
      f"runtimeState={data.get('runtimeState')} billingStatus={data.get('billingStatus')} "
      f"auditEventId={data.get('auditEventId')} trace={response_trace(result)}")
  return data


def get_commercial_state(super_token: str, company_id: int) -> dict[str, Any]:
  result = request(
      "GET",
      f"{APP_BASE}/api/v1/superadmin/tenants/{company_id}/commercial-state",
      token=super_token,
      company_code=PLATFORM_CODE,
      expected={200},
  )
  return get_data(result)


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


def csv_fixture(run_marker: str) -> bytes:
  sku = "RM-M10-" + uuid.uuid4().hex[:10].upper()
  rows = [
      "brand,product_name,sku_code,category,unit_of_measure,base_price,gst_rate",
      f"M10 Proof Brand,{run_marker} storage write,{sku},RAW_MATERIAL,KG,10.00,18.00",
  ]
  return ("\n".join(rows) + "\n").encode("utf-8")


def catalog_write_action(tenant_token: str, run_marker: str, expected: set[int]) -> HttpResult:
  return multipart_request(
      "POST",
      f"{APP_BASE}/api/v1/catalog/import",
      token=tenant_token,
      company_code=TENANT_CODE,
      field_name="file",
      file_name="m10-suspension-proof.csv",
      content_type="text/csv",
      content=csv_fixture(run_marker),
      headers={idempotency_header_name(): validation_idempotency_marker("storage")},
      expected=expected,
  )


def idempotency_header_name() -> str:
  return "Idempotency-" + "Key"


def validation_idempotency_marker(kind: str) -> str:
  return "validation-" + kind + "-" + uuid.uuid4().hex


def invoice_pdf_action(tenant_token: str, invoice_id: int, expected: set[int]) -> HttpResult:
  return tenant_get(f"/api/v1/invoices/{invoice_id}/pdf", tenant_token, expected)


def invoice_email_action(tenant_token: str, invoice_id: int, expected: set[int]) -> HttpResult:
  return tenant_post(f"/api/v1/invoices/{invoice_id}/email", tenant_token, expected=expected, timeout=90)


def job_action(
    tenant_token: str,
    order_id: int,
    idempotency_key: str,
    run_marker: str,
    expected: set[int],
) -> HttpResult:
  headers = {
      "Idempotency-Key": idempotency_key,
      "X-Request-Id": "m10-job-" + uuid.uuid4().hex[:12],
  }
  return tenant_post(
      f"/api/v1/orchestrator/orders/{order_id}/fulfillment",
      tenant_token,
      payload={"status": "PROCESSING", "notes": f"{run_marker}-job"},
      headers=headers,
      expected=expected,
  )


def trace_read(tenant_token: str, trace_id: str, expected: set[int]) -> HttpResult:
  return tenant_get(f"/api/v1/orchestrator/traces/{urllib.parse.quote(trace_id)}", tenant_token, expected)


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
  result = request(
      "PUT",
      f"{APP_BASE}/api/v1/superadmin/tenants/{company_id}/entitlements/overrides",
      token=super_token,
      company_code=PLATFORM_CODE,
      payload={"limits": limits, "features": {}, "reason": reason},
      expected={200},
  )
  data = get_data(result)
  note(
      f"quota_override_update keys={sorted(limits.keys())} "
      f"auditEventId={data.get('auditEventId')} trace={response_trace(result)}")


def remove_or_restore_override(
    super_token: str, company_id: int, key: str, original_value: int | None) -> None:
  if original_value is None:
    request(
        "DELETE",
        f"{APP_BASE}/api/v1/superadmin/tenants/{company_id}/entitlements/overrides/{key}",
        token=super_token,
        company_code=PLATFORM_CODE,
        payload={"reason": "m10-suspension-async-proof-restore"},
        expected={200, 400},
    )
  else:
    put_limit_overrides(
        super_token,
        company_id,
        {key: original_value},
        "m10-suspension-async-proof-restore",
    )


def high_runtime_limits(original_limits: dict[str, Any]) -> dict[str, Any]:
  limits = dict(original_limits)
  limits.update(
      {
          "quotaMaxActiveUsers": max(int(original_limits.get("quotaMaxActiveUsers", 0)), 500),
          "quotaMaxApiRequests": max(int(original_limits.get("quotaMaxApiRequests", 0)), 100000),
          "quotaMaxStorageBytes": max(int(original_limits.get("quotaMaxStorageBytes", 0)), 10485760),
          "quotaMaxConcurrentRequests": max(int(original_limits.get("quotaMaxConcurrentRequests", 0)), 20),
          "burstRequestsPerMinute": max(int(original_limits.get("burstRequestsPerMinute", 0)), 1000),
          "quotaSoftLimitEnabled": True,
          "quotaHardLimitEnabled": True,
      })
  return limits


def api_key_surface_guard() -> None:
  openapi_path = os.path.join(REPO_ROOT, "openapi.json")
  try:
    with open(openapi_path, "r", encoding="utf-8") as handle:
      openapi = json.load(handle)
  except FileNotFoundError:
    fail("openapi.json not found for API-key surface guard")
  schemes = openapi.get("components", {}).get("securitySchemes", {})
  api_key_schemes = [
      name for name, scheme in schemes.items()
      if isinstance(scheme, dict) and str(scheme.get("type", "")).lower() == "apikey"
  ]
  api_key_paths = [
      path for path in openapi.get("paths", {})
      if "api-key" in path.lower() or "apikey" in path.lower()
  ]
  if api_key_schemes or api_key_paths:
    fail(
        "tenant API-key surface exists but no M10 suspension enforcement proof is implemented; "
        f"schemes={api_key_schemes} paths={api_key_paths}")
  note("api_key_surface=current_contract_absent apiKeySecuritySchemes=0 apiKeyPaths=0")


def assert_allowed(label: str, result: HttpResult, expected_status: int = 200) -> None:
  if result.status != expected_status:
    fail(f"{label} expected {expected_status}, got {result.status}; body={safe_body(result.body)}")
  note(f"{label} status={result.status} trace={response_trace(result)}")


def assert_blocked(label: str, result: HttpResult) -> None:
  if result.status not in BLOCK_STATUSES:
    fail(f"{label} expected blocked status, got {result.status}; body={safe_body(result.body)}")
  note(f"{label} blocked_status={result.status} body={safe_body(result.body)} trace={response_trace(result)}")


def assert_mailhog_unchanged(label: str, before_count: int, before_ids: set[str]) -> None:
  after_count, after_ids, _ = mailhog_snapshot()
  if after_count != before_count or after_ids != before_ids:
    fail(f"{label} changed MailHog unexpectedly before={before_count} after={after_count}")
  note(f"{label} mailhog_unchanged=true messageCount={after_count}")


def prove_allowed_grace(
    tenant_tokens: LoginTokens,
    invoice_id: int,
    order_id: int,
    run_marker: str,
) -> None:
  auth_me = tenant_get("/api/v1/auth/me", tenant_tokens.access, {200})
  assert_allowed("matrix state=GRACE surface=stale_auth_me", auth_me)
  refreshed = refresh(tenant_tokens.refresh, {200})
  assert_allowed("matrix state=GRACE surface=refresh", refreshed)
  rotate_tokens_if_present(tenant_tokens, refreshed)
  pdf = invoice_pdf_action(tenant_tokens.access, invoice_id, {200})
  assert_allowed("matrix state=GRACE surface=pdf", pdf)
  before_count, before_ids, _ = mailhog_snapshot()
  email = invoice_email_action(tenant_tokens.access, invoice_id, {200})
  assert_allowed("matrix state=GRACE surface=email", email)
  after_count, after_ids, after_summary = wait_for_mailhog_delta(before_count, before_ids)
  new_ids = sorted(after_ids - before_ids)
  if after_count != before_count + 1 or len(new_ids) != 1:
    fail(f"GRACE email expected exactly one MailHog delivery; before={before_count} after={after_count}")
  new_mail = next((item for item in after_summary if str(item.get("id")) == new_ids[0]), {})
  note(
      "matrix state=GRACE surface=mailhog delta=1 "
      f"messageId={new_mail.get('id')} subject={json.dumps(new_mail.get('subject'))} "
      f"to={json.dumps(new_mail.get('to'))}")
  job = job_action(tenant_tokens.access, order_id, "m10-grace-job-" + uuid.uuid4().hex, run_marker, {202})
  trace_id = job.body.get("traceId") if isinstance(job.body, dict) else None
  if not trace_id:
    fail("GRACE job accepted without traceId marker")
  note(f"matrix state=GRACE surface=job_submit status=202 traceId={trace_id}")


def prove_blocked_state(
    state: str,
    tenant_tokens: LoginTokens,
    invoice_id: int,
    order_id: int,
    run_marker: str,
    *,
    login_expected: set[int],
    refresh_expected: set[int],
) -> None:
  auth_me = tenant_get("/api/v1/auth/me", tenant_tokens.access, {200, *BLOCK_STATUSES})
  if state == "SUSPENDED_READ_ONLY":
    assert_allowed(f"matrix state={state} surface=stale_auth_me", auth_me)
  else:
    assert_blocked(f"matrix state={state} surface=stale_auth_me", auth_me)
  login(TENANT_EMAIL, TENANT_CODE, os.environ["ERP_VALIDATION_SEED_PASSWORD"], expected=login_expected)
  refreshed = refresh(tenant_tokens.refresh, refresh_expected)
  if 200 in refresh_expected and refreshed.status == 200:
    note(f"matrix state={state} surface=refresh status=200 token=present(redacted)")
    rotate_tokens_if_present(tenant_tokens, refreshed)
  else:
    assert_blocked(f"matrix state={state} surface=refresh", refreshed)
  safe_read = tenant_get("/api/v1/catalog/items?page=0&size=1", tenant_tokens.access, {200, *BLOCK_STATUSES})
  if state == "SUSPENDED_READ_ONLY":
    assert_allowed(f"matrix state={state} surface=safe_read", safe_read)
  else:
    assert_blocked(f"matrix state={state} surface=safe_read", safe_read)
  before_mailhog_count, before_mailhog_ids, _ = mailhog_snapshot()
  for surface, action in (
      ("write", lambda: catalog_write_action(tenant_tokens.access, run_marker, {200, *BLOCK_STATUSES})),
      ("pdf", lambda: invoice_pdf_action(tenant_tokens.access, invoice_id, {200, *BLOCK_STATUSES})),
      ("email", lambda: invoice_email_action(tenant_tokens.access, invoice_id, {200, *BLOCK_STATUSES})),
      (
          "job_submit",
          lambda: job_action(
              tenant_tokens.access,
              order_id,
              "m10-blocked-job-" + uuid.uuid4().hex,
              run_marker,
              {202, *BLOCK_STATUSES},
          ),
      ),
  ):
    assert_blocked(f"matrix state={state} surface={surface}", action())
  assert_mailhog_unchanged(
      f"matrix state={state} surface=blocked_email", before_mailhog_count, before_mailhog_ids)


def main() -> None:
  if os.environ.get("M10_SUSPENSION_ASYNC_PROOF_DRY_RUN", "").lower() in {"1", "true", "yes"}:
    dry_run()
    return
  password = os.environ.get("ERP_VALIDATION_SEED_PASSWORD")
  if not password:
    fail("ERP_VALIDATION_SEED_PASSWORD must be set for full runtime proof; value is never printed")

  run_marker = "m10-suspension-async-proof-" + uuid.uuid4().hex[:10]
  note(f"run_marker_start={run_marker}")
  api_key_surface_guard()
  wait_for_runtime()
  health = request("GET", f"{MGMT_BASE}/actuator/health", expected={200, 503})
  readiness = request("GET", f"{MGMT_BASE}/actuator/health/readiness", expected={200, 503})
  mailhog_count, _, mailhog_items = mailhog_snapshot()
  note(f"health_status={health.status} readiness_status={readiness.status}")
  note(f"mailhog_http=200 message_count={mailhog_count} sample={json.dumps(mailhog_items[:5], sort_keys=True)}")

  super_tokens = login(SUPERADMIN_EMAIL, PLATFORM_CODE, password)
  if super_tokens is None:
    fail("Super Admin login did not succeed")
  company_id = tenant_id(super_tokens.access)
  ensure_subscription(super_tokens.access, company_id)
  set_state(super_tokens.access, company_id, "ACTIVE", run_marker)

  detail = get_data(
      request(
          "GET",
          f"{APP_BASE}/api/v1/superadmin/tenants/{company_id}",
          token=super_tokens.access,
          company_code=PLATFORM_CODE,
          expected={200},
      ))
  original_limits = dict(detail["limits"])
  entitlements = get_data(
      request(
          "GET",
          f"{APP_BASE}/api/v1/superadmin/tenants/{company_id}/entitlements",
          token=super_tokens.access,
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
  put_limits(super_tokens.access, company_id, high_runtime_limits(original_limits))
  put_limit_overrides(
      super_tokens.access,
      company_id,
      {"maxPdfExports": 100000, "maxEmails": 100000, "maxJobs": 100000, "maxStorageBytes": 10485760},
      "m10-suspension-async-proof-high-limits",
  )

  tenant_tokens = login(TENANT_EMAIL, TENANT_CODE, password)
  if tenant_tokens is None:
    fail("Tenant login did not succeed in ACTIVE state")
  invoice_id = find_invoice_id(tenant_tokens.access)
  order_id = find_order_id(tenant_tokens.access)

  try:
    accepted_replay_marker = validation_idempotency_marker("accepted-job")
    accepted = job_action(tenant_tokens.access, order_id, accepted_replay_marker, run_marker, {202})
    accepted_trace_id = accepted.body.get("traceId") if isinstance(accepted.body, dict) else None
    if not accepted_trace_id:
      fail("active accepted job did not return traceId")
    note(f"already_accepted_job initial_status=202 traceId={accepted_trace_id}")

    set_state(super_tokens.access, company_id, "GRACE", run_marker)
    prove_allowed_grace(tenant_tokens, invoice_id, order_id, run_marker)

    set_state(super_tokens.access, company_id, "SUSPENDED_READ_ONLY", run_marker)
    accepted_replay = job_action(
        tenant_tokens.access,
        order_id,
        accepted_replay_marker,
        run_marker,
        {202, *BLOCK_STATUSES},
    )
    if accepted_replay.status == 202:
      replay_trace_id = accepted_replay.body.get("traceId") if isinstance(accepted_replay.body, dict) else None
      if replay_trace_id != accepted_trace_id:
        fail(
            "already accepted job replay returned a different trace marker: "
            f"initial={accepted_trace_id} replay={replay_trace_id}")
      note(
          "already_accepted_job state=SUSPENDED_READ_ONLY replay_status=202 "
          f"traceId={accepted_trace_id}")
    else:
      assert_blocked("already_accepted_job state=SUSPENDED_READ_ONLY replay", accepted_replay)
    prove_blocked_state(
        "SUSPENDED_READ_ONLY",
        tenant_tokens,
        invoice_id,
        order_id,
        run_marker,
        login_expected={200},
        refresh_expected={200},
    )
    put_limit_overrides(
        super_tokens.access,
        company_id,
        {"maxPdfExports": 100000, "maxEmails": 100000, "maxJobs": 100000},
        "m10-suspension-async-proof-read-only-bypass-probe",
    )
    blocked_after_override = job_action(
        tenant_tokens.access,
        order_id,
        "m10-read-only-override-job-" + uuid.uuid4().hex,
        run_marker,
        {202, *BLOCK_STATUSES},
    )
    assert_blocked("quota_override_bypass state=SUSPENDED_READ_ONLY surface=job_submit", blocked_after_override)

    set_state(super_tokens.access, company_id, "SUSPENDED_BLOCKED", run_marker)
    prove_blocked_state(
        "SUSPENDED_BLOCKED",
        tenant_tokens,
        invoice_id,
        order_id,
        run_marker,
        login_expected=BLOCK_STATUSES,
        refresh_expected=BLOCK_STATUSES,
    )

    set_state(super_tokens.access, company_id, "ACTIVE", run_marker)
    tenant_tokens = login(TENANT_EMAIL, TENANT_CODE, password)
    if tenant_tokens is None:
      fail("Tenant login did not recover after resume")
    set_state(super_tokens.access, company_id, "CANCELED", run_marker)
    prove_blocked_state(
        "CANCELED",
        tenant_tokens,
        invoice_id,
        order_id,
        run_marker,
        login_expected=BLOCK_STATUSES,
        refresh_expected=BLOCK_STATUSES,
    )
    put_limit_overrides(
        super_tokens.access,
        company_id,
        {"maxPdfExports": 100000, "maxEmails": 100000, "maxJobs": 100000},
        "m10-suspension-async-proof-canceled-bypass-probe",
    )
    canceled_pdf = invoice_pdf_action(tenant_tokens.access, invoice_id, {200, *BLOCK_STATUSES})
    assert_blocked("quota_override_bypass state=CANCELED surface=pdf", canceled_pdf)

    set_state(super_tokens.access, company_id, "ACTIVE", run_marker)
    tenant_tokens = login(TENANT_EMAIL, TENANT_CODE, password)
    if tenant_tokens is None:
      fail("Tenant login did not recover after canceled resume")
    set_state(super_tokens.access, company_id, "ARCHIVED", run_marker)
    prove_blocked_state(
        "ARCHIVED",
        tenant_tokens,
        invoice_id,
        order_id,
        run_marker,
        login_expected=BLOCK_STATUSES,
        refresh_expected=BLOCK_STATUSES,
    )

    set_state(super_tokens.access, company_id, "ACTIVE", run_marker)
    tenant_tokens = login(TENANT_EMAIL, TENANT_CODE, password)
    if tenant_tokens is None:
      fail("Tenant login did not recover before scheduled lifecycle proof")
    future_at = (datetime.now(timezone.utc) + timedelta(seconds=2)).isoformat().replace("+00:00", "Z")
    scheduled = post_commercial(
        super_tokens.access,
        company_id,
        "cancel",
        {"reason": f"{run_marker} scheduled cancel proof", "effectiveAt": future_at},
    )
    scheduled_data = get_data(scheduled)
    note(
        "scheduled_background action=cancel scheduled_status=200 "
        f"commercialState={scheduled_data.get('commercialState')} effectiveAt={scheduled_data.get('effectiveAt')} "
        f"auditEventId={scheduled_data.get('auditEventId')}")
    before_due_job = job_action(
        tenant_tokens.access,
        order_id,
        "m10-before-scheduled-cancel-" + uuid.uuid4().hex,
        run_marker,
        {202},
    )
    note(f"scheduled_background before_effective_job_status={before_due_job.status} trace={response_trace(before_due_job)}")
    time.sleep(3)
    after_due = get_commercial_state(super_tokens.access, company_id)
    if after_due.get("commercialState") != "CANCELED":
      fail(f"scheduled cancel did not apply after effectiveAt; state={after_due}")
    after_due_job = job_action(
        tenant_tokens.access,
        order_id,
        "m10-after-scheduled-cancel-" + uuid.uuid4().hex,
        run_marker,
        {202, *BLOCK_STATUSES},
    )
    assert_blocked("scheduled_background after_effective_surface=job_submit", after_due_job)
    note(
        "scheduled_background applied=true "
        f"commercialState={after_due.get('commercialState')} auditEventId={after_due.get('auditEventId')}")
  finally:
    set_state(super_tokens.access, company_id, "ACTIVE", run_marker)
    for key, original_value in original_overrides.items():
      remove_or_restore_override(super_tokens.access, company_id, key, original_value)
    put_limits(super_tokens.access, company_id, original_limits)
    note("cleanup=restored-active-and-original-limits")

  note(f"run_marker_end={run_marker}")


if __name__ == "__main__":
  main()
