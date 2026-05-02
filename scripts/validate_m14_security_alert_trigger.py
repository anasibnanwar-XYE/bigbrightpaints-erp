#!/usr/bin/env python3
"""M14 validation-harness SECURITY_ALERT trigger proof.

This harness uses only local runtime HTTP APIs. It never prints bearer tokens,
passwords, .env values, or tenant-private payloads; evidence is limited to HTTP
statuses, trace IDs, event IDs, and safe metadata keys.
"""

from __future__ import annotations

import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass
from typing import Any


APP_BASE = "http://localhost:8081"
SUPERADMIN_EMAIL = "validation.superadmin@example.com"
PLATFORM_CODE = "PLATFORM"
DEFAULT_PASSWORD = "ValidationSeed!2026"


@dataclass
class HttpResult:
  status: int
  body: Any
  headers: dict[str, str]


def note(message: str) -> None:
  print(f"[m14-security-alert-trigger] {message}", flush=True)


def fail(message: str) -> None:
  print(f"[m14-security-alert-trigger] ERROR: {message}", file=sys.stderr, flush=True)
  raise SystemExit(1)


def dry_run() -> None:
  note("dry_run=true")
  note("runtime_profile=prod,flyway-v2,mock,validation-seed,validation-harness")
  note("trigger=POST /api/v1/validation/harness/security-alert")
  note("readback=GET /api/v1/superadmin/audit/suspicious-events?reference=<runMarker>")
  note("evidence=eventId,traceId,runMarker,HTTP statuses; credentials and tokens redacted")


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
    payload: Any | None = None,
    headers: dict[str, str] | None = None,
    expected: set[int] | None = None,
    timeout: int = 20,
) -> HttpResult:
  headers = dict(headers or {})
  if token:
    headers["Authorization"] = f"Bearer {token}"
  data = None
  if payload is not None:
    headers["Content-Type"] = "application/json"
    data = json.dumps(payload, separators=(",", ":")).encode("utf-8")
  req = urllib.request.Request(url, data=data, headers=headers, method=method)
  try:
    with urllib.request.urlopen(req, timeout=timeout) as response:
      result = HttpResult(
          response.status,
          parse_body(response.read(), response.headers.get("Content-Type", "")),
          {key.lower(): value for key, value in response.headers.items()},
      )
  except urllib.error.HTTPError as exc:
    result = HttpResult(
        exc.code,
        parse_body(exc.read(), exc.headers.get("Content-Type", "")),
        {key.lower(): value for key, value in exc.headers.items()},
    )
  if expected is not None and result.status not in expected:
    fail(f"{method} {safe_url(url)} expected {sorted(expected)} got {result.status}; body={safe_body(result.body)}")
  return result


def safe_url(url: str) -> str:
  parsed = urllib.parse.urlsplit(url)
  query = urllib.parse.parse_qsl(parsed.query, keep_blank_values=True)
  redacted = [(key, "<redacted>" if "token" in key.lower() else value) for key, value in query]
  return urllib.parse.urlunsplit(
      (parsed.scheme, parsed.netloc, parsed.path, urllib.parse.urlencode(redacted), ""))


def safe_body(body: Any) -> Any:
  if not isinstance(body, dict):
    return body
  safe: dict[str, Any] = {}
  for key in ("success", "message", "errorCode", "traceId"):
    if key in body:
      safe[key] = body[key]
  data = body.get("data")
  if isinstance(data, dict):
    safe["dataKeys"] = sorted(data.keys())
    for key in ("eventId", "eventType", "auditStatus", "alertType", "runMarker", "traceId"):
      if key in data:
        safe[key] = data[key]
  return safe or {"keys": sorted(body.keys())}


def data(result: HttpResult) -> dict[str, Any]:
  if not isinstance(result.body, dict) or not isinstance(result.body.get("data"), dict):
    fail(f"response missing data envelope: {safe_body(result.body)}")
  return result.body["data"]


def wait_for_runtime() -> None:
  deadline = time.time() + 180
  last_status = "000"
  while time.time() < deadline:
    try:
      result = request("GET", f"{APP_BASE}/api/v1/auth/me", expected={200, 401, 403}, timeout=5)
      last_status = str(result.status)
      note(f"api_reachable auth_me_anon_status={result.status}")
      return
    except Exception as exc:  # noqa: BLE001 - bounded readiness polling
      last_status = exc.__class__.__name__
      time.sleep(2)
  fail(f"backend did not become API-reachable; last={last_status}")


def login(password: str) -> str:
  response = request(
      "POST",
      f"{APP_BASE}/api/v1/auth/login",
      payload={"email": SUPERADMIN_EMAIL, "password": password, "companyCode": PLATFORM_CODE},
      expected={200},
  )
  token = response.body.get("accessToken") if isinstance(response.body, dict) else None
  if not token:
    fail("superadmin login did not return token marker")
  note("login actor=validation.superadmin@example.com company=PLATFORM token=present(redacted)")
  return token


def trigger_alert(run_marker: str) -> dict[str, Any]:
  result = request(
      "POST",
      f"{APP_BASE}/api/v1/validation/harness/security-alert",
      payload={
          "runMarker": run_marker,
          "alertType": "M14_VALIDATION_SECURITY_ALERT",
          "reasonCode": "M14_RUNTIME_TRIGGER",
      },
      headers={"X-Trace-Id": run_marker},
      expected={200},
  )
  payload = data(result)
  if payload.get("eventType") != "SECURITY_ALERT" or not payload.get("eventId"):
    fail(f"trigger did not return SECURITY_ALERT event evidence: {safe_body(result.body)}")
  note(
      "trigger_http=200 "
      f"eventId={payload.get('eventId')} "
      f"traceId={payload.get('traceId')} "
      f"runMarker={payload.get('runMarker')}")
  return payload


def suspicious_readback(token: str, run_marker: str, expected_event_id: int) -> None:
  encoded_marker = urllib.parse.quote(run_marker)
  result = request(
      "GET",
      f"{APP_BASE}/api/v1/superadmin/audit/suspicious-events?reference={encoded_marker}",
      token=token,
      expected={200},
  )
  payload = data(result)
  content = payload.get("content") if isinstance(payload, dict) else None
  if not isinstance(content, list):
    fail(f"suspicious feed missing content list: {safe_body(result.body)}")
  ids = [item.get("eventId") for item in content if isinstance(item, dict)]
  if expected_event_id not in ids:
    fail(f"suspicious feed did not include eventId={expected_event_id}; ids={ids}")
  sample = next(
      item for item in content if isinstance(item, dict) and item.get("eventId") == expected_event_id)
  metadata = sample.get("metadata") if isinstance(sample.get("metadata"), dict) else {}
  forbidden = ("password", "token", "secret", "bearer", "jwt", "payload")
  joined = json.dumps(sample, sort_keys=True).lower()
  leaked = [word for word in forbidden if word in joined]
  if leaked:
    fail(f"suspicious feed evidence contained forbidden markers: {leaked}")
  note(
      "suspicious_readback_http=200 "
      f"matchedEventId={expected_event_id} "
      f"traceId={sample.get('traceId')} "
      f"metadataKeys={','.join(sorted(metadata.keys()))}")


def main() -> None:
  if "--dry-run" in sys.argv:
    dry_run()
    return
  run_marker = os.environ.get("M14_SECURITY_ALERT_RUN_MARKER")
  if not run_marker:
    run_marker = time.strftime("M14TRIGGER%Y%m%d%H%M%S", time.gmtime())
  password = os.environ.get("ERP_VALIDATION_SEED_PASSWORD", DEFAULT_PASSWORD)
  wait_for_runtime()
  trigger = trigger_alert(run_marker)
  token = login(password)
  suspicious_readback(token, run_marker, int(trigger["eventId"]))
  note("status=pass secrets=redacted db_fixture=not_used")


if __name__ == "__main__":
  main()
