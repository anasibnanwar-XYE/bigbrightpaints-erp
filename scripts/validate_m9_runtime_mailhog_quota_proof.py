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
      "coverage=PDF_EXPORTS,EMAILS(BUSINESS/MailHog-safe),JOBS,STORAGE,"
      "API_CALLS(monthly),BURST_REQUESTS_PER_MINUTE,MAX_CONCURRENT_REQUESTS")
  note("auth_evidence=token-present markers only; passwords and bearer tokens are never printed")
  note("mailhog_evidence=HTTP status, message count, IDs, subjects, recipients only")
  note("rate_limit_evidence=HTTP 429 plus Retry-After/X-RateLimit-* headers and safe error code")


def json_bytes(payload: Any) -> bytes:
  return json.dumps(payload, separators=(",", ":")).encode("utf-8")


def parse_body(raw: bytes, content_type: str) -> Any:
  if not raw:
    return None
  if "application/json" in content_type:
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
    for key in ("decision", "accepted", "dimension", "reasonCode", "message", "limitType"):
      if key in data:
        safe[key] = data[key]
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
        expected={200},
    )
  else:
    put_limit_overrides(
        super_token,
        company_id,
        {key: original_value},
        "m9-runtime-quota-proof-restore",
    )


def quota_check(
    super_token: str,
    company_id: int,
    dimension: str,
    *,
    units: int | None = None,
    bytes_: int | None = None,
    email_category: str | None = None,
) -> dict[str, Any]:
  payload = {"dimension": dimension, "units": units, "bytes": bytes_, "emailCategory": email_category, "dryRun": True}
  result = request(
      "POST",
      f"{APP_BASE}/api/v1/superadmin/tenants/{company_id}/quota-check",
      token=super_token,
      company_code=PLATFORM_CODE,
      payload=payload,
      expected={200},
  )
  data = get_data(result)
  if data.get("accepted") is not False:
    fail(f"{dimension} quota-check expected deterministic block, got {safe_body(result.body)}")
  note(
      "quota_dimension="
      f"{dimension} decision={data.get('decision')} reasonCode={data.get('reasonCode')} "
      f"usedBefore={data.get('usedBefore')} limit={data.get('limit')} "
      f"mailhogEvidenceSafe={data.get('mailhogEvidenceSafe')} "
      f"tokenRedactionRequired={data.get('tokenRedactionRequired')} trace={response_trace(result)}")
  return data


def tenant_get(path: str, tenant_token: str, expected: set[int]) -> HttpResult:
  return request(
      "GET",
      f"{APP_BASE}{path}",
      token=tenant_token,
      company_code=TENANT_CODE,
      expected=expected,
      timeout=60,
  )


def find_invoice_id(tenant_token: str) -> int | None:
  result = tenant_get("/api/v1/invoices?page=0&size=100", tenant_token, {200})
  data = get_data(result)
  invoices = data if isinstance(data, list) else data.get("content", [])
  for invoice in invoices:
    if invoice.get("invoiceNumber") == TENANT_INVOICE_NUMBER:
      return int(invoice["id"])
  return None


def main() -> None:
  if os.environ.get("M9_QUOTA_PROOF_DRY_RUN", "").lower() in {"1", "true", "yes"}:
    dry_run()
    return

  password = os.environ.get("ERP_VALIDATION_SEED_PASSWORD")
  if not password:
    fail("ERP_VALIDATION_SEED_PASSWORD must be set for full runtime proof; value is never printed")

  note("run_marker_start=m9-runtime-mailhog-quota-proof")
  wait_for_runtime()
  health = request("GET", f"{MGMT_BASE}/actuator/health", expected={200, 503})
  readiness = request("GET", f"{MGMT_BASE}/actuator/health/readiness", expected={200, 503})
  mailhog_count, mailhog_items = mailhog_summary()
  note(f"health_status={health.status} readiness_status={readiness.status}")
  note(f"mailhog_http=200 message_count={mailhog_count} message_summary={json.dumps(mailhog_items, sort_keys=True)}")

  super_token = login(SUPERADMIN_EMAIL, PLATFORM_CODE, password)
  tenant_token = login(TENANT_EMAIL, TENANT_CODE, password)
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
  usage = get_data(
      request(
          "GET",
          f"{APP_BASE}/api/v1/superadmin/tenants/{company_id}/usage",
          token=super_token,
          company_code=PLATFORM_CODE,
          expected={200},
      ))
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

  try:
    override_limits = {
        OVERRIDE_KEYS[dimension]: dimension_used(usage, dimension) for dimension in REQUIRED_DIMENSIONS
    }
    put_limit_overrides(
        super_token,
        company_id,
        override_limits,
        "m9-runtime-mailhog-quota-proof-tighten",
    )
    for dimension in REQUIRED_DIMENSIONS:
      if dimension == "STORAGE":
        quota_check(super_token, company_id, dimension, bytes_=1)
      elif dimension == "EMAILS":
        quota_check(super_token, company_id, dimension, units=1, email_category="BUSINESS")
      else:
        quota_check(super_token, company_id, dimension, units=1)

    api_used = dimension_used(usage, "API_CALLS")
    tight_limits = dict(original_limits)
    tight_limits.update(
        {
            "quotaMaxApiRequests": api_used,
            "quotaMaxStorageBytes": max(int(original_limits.get("quotaMaxStorageBytes", 0)), 1024),
            "quotaMaxConcurrentRequests": 10,
            "burstRequestsPerMinute": 100,
            "quotaSoftLimitEnabled": True,
            "quotaHardLimitEnabled": True,
        })
    put_limits(super_token, company_id, tight_limits)
    monthly = tenant_get("/api/v1/auth/me", tenant_token, {200, 429})
    if monthly.status != 429:
      fail(f"monthly API quota expected 429, got {monthly.status}")
    note(
        f"api_monthly_status=429 headers={json.dumps(rate_limit_headers(monthly), sort_keys=True)} "
        f"body={safe_body(monthly.body)}")

    burst_limits = dict(original_limits)
    burst_limits.update(
        {
            "quotaMaxApiRequests": api_used + 1000,
            "quotaMaxStorageBytes": max(int(original_limits.get("quotaMaxStorageBytes", 0)), 1024),
            "quotaMaxConcurrentRequests": 10,
            "burstRequestsPerMinute": 1,
            "quotaSoftLimitEnabled": True,
            "quotaHardLimitEnabled": True,
        })
    put_limits(super_token, company_id, burst_limits)
    burst_results = [tenant_get("/api/v1/auth/me", tenant_token, {200, 429}) for _ in range(3)]
    burst_429 = next((result for result in burst_results if result.status == 429), None)
    if burst_429 is None:
      fail("burst quota expected at least one 429")
    note(
        f"burst_status=429 headers={json.dumps(rate_limit_headers(burst_429), sort_keys=True)} "
        f"body={safe_body(burst_429.body)}")

    invoice_id = find_invoice_id(tenant_token)
    if invoice_id is None:
      note("concurrency_pdf_probe=skipped reason=seeded_invoice_not_found")
    else:
      concurrency_limits = dict(original_limits)
      concurrency_limits.update(
          {
              "quotaMaxApiRequests": api_used + 1000,
              "quotaMaxStorageBytes": max(int(original_limits.get("quotaMaxStorageBytes", 0)), 1024),
              "quotaMaxConcurrentRequests": 1,
              "burstRequestsPerMinute": 100,
              "quotaSoftLimitEnabled": True,
              "quotaHardLimitEnabled": True,
          })
      put_limits(super_token, company_id, concurrency_limits)
      results: list[HttpResult] = []
      lock = threading.Lock()

      def fetch_pdf() -> None:
        result = tenant_get(f"/api/v1/invoices/{invoice_id}/pdf", tenant_token, {200, 429})
        with lock:
          results.append(result)

      threads = [threading.Thread(target=fetch_pdf) for _ in range(4)]
      for thread in threads:
        thread.start()
      for thread in threads:
        thread.join()
      concurrency_429 = next((result for result in results if result.status == 429), None)
      if concurrency_429 is None:
        fail(f"concurrency quota expected 429, got statuses={[result.status for result in results]}")
      note(
          f"concurrency_status=429 headers={json.dumps(rate_limit_headers(concurrency_429), sort_keys=True)} "
          f"body={safe_body(concurrency_429.body)}")
  finally:
    for key, original_value in original_overrides.items():
      remove_or_restore_override(super_token, company_id, key, original_value)
    put_limits(super_token, company_id, original_limits)

  note("run_marker_end=m9-runtime-mailhog-quota-proof")


if __name__ == "__main__":
  main()
