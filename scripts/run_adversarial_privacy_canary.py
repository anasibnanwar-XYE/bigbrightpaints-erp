#!/usr/bin/env python3
"""Bounded local IAM adversarial privacy-canary validation harness.

This harness is intentionally local-only and evidence-safe:
- only localhost/127.0.0.1 targets are allowed;
- request budgets are enforced before any network call;
- bearer/refresh tokens stay in process memory and are never written to URLs or temp files;
- saved evidence is sanitized, scanned, and deleted before exit.
"""

from __future__ import annotations

import argparse
import concurrent.futures
import contextlib
import dataclasses
import json
import os
import re
import shutil
import socket
import subprocess
import sys
import tempfile
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_BASE_URL = "http://localhost:18081"
DEFAULT_MGMT_URL = "http://localhost:19090"
DEFAULT_MAILHOG_URL = "http://localhost:18025"
ALLOWED_HOSTS = {"localhost", "127.0.0.1", "::1"}
MISSION_APPROVED_PORTS = {"base-url": 18081, "mgmt-url": 19090, "mailhog-url": 18025}
SECRET_KEY_RE = re.compile(
    r"(password|passtoken|token|secret|qruri|recovery|authorization|cookie|digest|hash|mfa)",
    re.IGNORECASE,
)
JWT_RE = re.compile(r"\beyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\b")
BEARER_RE = re.compile(r"Bearer\s+[A-Za-z0-9._~+/=-]{20,}", re.IGNORECASE)
REFRESH_JSON_RE = re.compile(
    r'("(?:accessToken|refreshToken|token|secret|qrUri|password|recoveryCode|recoveryCodes)"\s*:\s*)"([^"]*)"',
    re.IGNORECASE,
)
EMAIL_RE = re.compile(r"\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b")
PHONE_RE = re.compile(r"\b(?:\+?\d[\d .()-]{7,}\d)\b")
QUERY_TOKEN_RE = re.compile(r"(?i)(access[_-]?token|refresh[_-]?token|token|jwt|secret)=")
AUTH_PAYLOAD_KEYS = {
    "accessToken",
    "authScopeCode",
    "companyCode",
    "displayName",
    "email",
    "expiresIn",
    "mfaEnabled",
    "mustChangePassword",
    "permissions",
    "qrUri",
    "recoveryCode",
    "recoveryCodes",
    "refreshToken",
    "roles",
    "sessionId",
    "sessions",
    "token",
    "tokenType",
    "user",
}


@dataclasses.dataclass
class HttpCapture:
    method: str
    path: str
    status: int
    elapsed_ms: int
    sanitized_body: Any
    body_text: str


@dataclasses.dataclass(frozen=True)
class ProbeExpectation:
    name: str
    expected_statuses: tuple[int, ...]
    required_fragments: tuple[str, ...] = ()
    allow_200: bool = False
    forbid_auth_payload: bool = True


class Budget:
    def __init__(self, total_limit: int, per_assertion_limit: int, concurrency_limit: int) -> None:
        self.total_limit = total_limit
        self.per_assertion_limit = per_assertion_limit
        self.concurrency_limit = concurrency_limit
        self.total = 0
        self.by_assertion: dict[str, int] = {}
        self.current_concurrency = 0
        self.max_seen_concurrency = 0
        self._lock = threading.Lock()

    @contextlib.contextmanager
    def request(self, assertion_id: str):
        with self._lock:
            next_total = self.total + 1
            next_assertion = self.by_assertion.get(assertion_id, 0) + 1
            next_concurrency = self.current_concurrency + 1
            if next_total > self.total_limit:
                raise RuntimeError(
                    f"request budget exceeded: total would be {next_total}/{self.total_limit}"
                )
            if next_assertion > self.per_assertion_limit:
                raise RuntimeError(
                    f"{assertion_id} budget exceeded: "
                    f"{next_assertion}/{self.per_assertion_limit}"
                )
            if next_concurrency > self.concurrency_limit:
                raise RuntimeError(
                    f"concurrency budget exceeded: "
                    f"{next_concurrency}/{self.concurrency_limit}"
                )
            self.total = next_total
            self.by_assertion[assertion_id] = next_assertion
            self.current_concurrency = next_concurrency
            self.max_seen_concurrency = max(self.max_seen_concurrency, next_concurrency)
        try:
            yield
        finally:
            with self._lock:
                self.current_concurrency -= 1


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Run local bounded IAM adversarial privacy-canary validation."
    )
    parser.add_argument("--base-url", default=os.environ.get("BASE_URL", DEFAULT_BASE_URL))
    parser.add_argument("--mgmt-url", default=os.environ.get("MGMT_URL", DEFAULT_MGMT_URL))
    parser.add_argument("--mailhog-url", default=os.environ.get("MAILHOG_URL", DEFAULT_MAILHOG_URL))
    parser.add_argument("--seed-password-env", default="ERP_VALIDATION_SEED_PASSWORD")
    parser.add_argument("--backend-log", default=os.environ.get("BACKEND_LOG_PATH", ""))
    parser.add_argument("--max-requests", type=int, default=100)
    parser.add_argument("--per-assertion-max", type=int, default=30)
    parser.add_argument("--max-concurrency", type=int, default=2)
    parser.add_argument("--timeout", type=float, default=5.0)
    parser.add_argument("--connect-timeout", type=float, default=1.0)
    parser.add_argument("--verbose", action="store_true")
    return parser.parse_args()


def ensure_local_http_url(url: str, label: str) -> str:
    parsed = urllib.parse.urlparse(url)
    if parsed.scheme != "http":
        raise SystemExit(f"{label} must use http:// on mission localhost ports: {url}")
    host = parsed.hostname
    if host not in ALLOWED_HOSTS:
        raise SystemExit(f"{label} must target localhost only, got host={host!r}")
    expected_port = MISSION_APPROVED_PORTS.get(label)
    actual_port = parsed.port
    if expected_port is not None and actual_port != expected_port:
        raise SystemExit(
            f"{label} must use mission-approved localhost port {expected_port}, "
            f"got {actual_port or 80}: {url}"
        )
    return url.rstrip("/")


def require_port_open(url: str, timeout: float) -> None:
    parsed = urllib.parse.urlparse(url)
    host = parsed.hostname or "localhost"
    port = parsed.port or 80
    with socket.create_connection((host, port), timeout=timeout):
        return


def redact_value(value: Any) -> Any:
    if isinstance(value, dict):
        return {
            str(key): ("<redacted>" if SECRET_KEY_RE.search(str(key)) else redact_value(val))
            for key, val in value.items()
        }
    if isinstance(value, list):
        return [redact_value(item) for item in value]
    if isinstance(value, str):
        return sanitize_text(value)
    return value


def sanitize_text(text: str) -> str:
    sanitized = REFRESH_JSON_RE.sub(r'\1"<redacted>"', text)
    sanitized = JWT_RE.sub("<jwt-redacted>", sanitized)
    sanitized = BEARER_RE.sub("Bearer <redacted>", sanitized)
    sanitized = EMAIL_RE.sub("<email-redacted>", sanitized)
    sanitized = PHONE_RE.sub("<phone-redacted>", sanitized)
    return sanitized


def decode_json_or_text(body: bytes) -> tuple[Any, str]:
    text = body.decode("utf-8", errors="replace")
    if not text:
        return "", ""
    try:
        parsed = json.loads(text)
    except json.JSONDecodeError:
        return sanitize_text(text), sanitize_text(text)
    sanitized = redact_value(parsed)
    return sanitized, json.dumps(sanitized, sort_keys=True)


def raw_text(body: bytes) -> str:
    return body.decode("utf-8", errors="replace")


def response_has_success_true(value: Any) -> bool:
    if isinstance(value, dict):
        if value.get("success") is True:
            return True
        return any(response_has_success_true(item) for item in value.values())
    if isinstance(value, list):
        return any(response_has_success_true(item) for item in value)
    return False


def response_contains_auth_payload(value: Any, *, inside_error_metadata: bool = False) -> bool:
    if isinstance(value, dict):
        for key, val in value.items():
            key_text = str(key)
            if not inside_error_metadata and key_text in AUTH_PAYLOAD_KEYS:
                return True
            next_inside_error_metadata = inside_error_metadata or key_text in {"details", "errors"}
            if response_contains_auth_payload(
                val, inside_error_metadata=next_inside_error_metadata
            ):
                return True
    elif isinstance(value, list):
        return any(
            response_contains_auth_payload(item, inside_error_metadata=inside_error_metadata)
            for item in value
        )
    return False


def contains_unredacted_secret_json_field(text: str) -> bool:
    for match in REFRESH_JSON_RE.finditer(text):
        value = match.group(2).strip()
        normalized = value.lower()
        if not value:
            continue
        if "redacted" in normalized or "masked" in normalized:
            continue
        if value.startswith("***") or set(value) == {"*"}:
            continue
        if value.endswith("..."):
            continue
        if value.startswith("<") and value.endswith(">"):
            continue
        if value.startswith("${") and value.endswith("}"):
            continue
        return True
    return False


class Harness:
    def __init__(self, args: argparse.Namespace, seed_password: str, evidence_dir: Path) -> None:
        self.base_url = ensure_local_http_url(args.base_url, "base-url")
        self.mgmt_url = ensure_local_http_url(args.mgmt_url, "mgmt-url")
        self.mailhog_url = ensure_local_http_url(args.mailhog_url, "mailhog-url")
        self.timeout = args.timeout
        self.connect_timeout = args.connect_timeout
        self.verbose = args.verbose
        self.backend_log_paths = self.resolve_backend_log_paths(args.backend_log)
        self.budget = Budget(args.max_requests, args.per_assertion_max, args.max_concurrency)
        self.seed_password = seed_password
        self.evidence_dir = evidence_dir
        self.canaries = {
            "password": f"ADV_CANARY_PASSWORD_{uuid.uuid4().hex}",
            "token": f"ADV_CANARY_TOKEN_{uuid.uuid4().hex}.eyJnotatoken",
            "reset": f"ADV_CANARY_RESET_LINK_{uuid.uuid4().hex}",
            "mfa": f"ADV_CANARY_MFA_SECRET_{uuid.uuid4().hex}",
            "recovery": f"ADV_CANARY_RECOVERY_CODE_{uuid.uuid4().hex}",
            "email": f"adv-canary-{uuid.uuid4().hex}@example.invalid",
            "phone": f"+1555{uuid.uuid4().hex[:8]}",
            "user_agent": f"AdvCanaryUA/{uuid.uuid4().hex}",
            "tenant": f"ADV_CANARY_TENANT_{uuid.uuid4().hex}",
        }
        self.captures: list[HttpCapture] = []
    def resolve_backend_log_paths(self, explicit_backend_log: str) -> list[Path]:
        candidates = [
            explicit_backend_log,
            os.environ.get("BACKEND_LOG_PATH", ""),
            os.environ.get("LOG_FILE", ""),
            os.environ.get("SPRING_BOOT_LOG_FILE", ""),
            os.environ.get("SPRING_LOG_FILE", ""),
        ]
        paths: list[Path] = []
        seen: set[str] = set()
        for candidate in candidates:
            if not candidate:
                continue
            path = Path(candidate).expanduser()
            key = str(path)
            if key in seen:
                continue
            seen.add(key)
            paths.append(path)
        return paths

    def request(
        self,
        assertion_id: str,
        method: str,
        path: str,
        *,
        body: dict[str, Any] | None = None,
        headers: dict[str, str] | None = None,
        capture: bool = True,
    ) -> HttpCapture:
        if not path.startswith("/"):
            raise RuntimeError(f"refusing non-path request target: {path}")
        if QUERY_TOKEN_RE.search(path):
            raise RuntimeError(f"refusing to place token-like material in URL: {path}")
        url = self.base_url + path
        parsed = urllib.parse.urlparse(url)
        if parsed.hostname not in ALLOWED_HOSTS:
            raise RuntimeError(f"refusing non-local URL: {url}")
        payload = None
        request_headers = {"Accept": "application/json"}
        if headers:
            request_headers.update(headers)
        if body is not None:
            payload = json.dumps(body, separators=(",", ":")).encode("utf-8")
            request_headers["Content-Type"] = "application/json"
        req = urllib.request.Request(url, data=payload, method=method, headers=request_headers)
        start = time.monotonic()
        with self.budget.request(assertion_id):
            try:
                with urllib.request.urlopen(req, timeout=self.timeout) as response:
                    raw = response.read(65536)
                    status = response.getcode()
            except urllib.error.HTTPError as exc:
                raw = exc.read(65536)
                status = exc.code
        elapsed_ms = round((time.monotonic() - start) * 1000)
        self.assert_raw_surface_clean(
            f"raw-http {method} {path}",
            raw_text(raw),
            allow_token_material=False,
            strict_email=False,
        )
        sanitized_body, body_text = decode_json_or_text(raw)
        capture_obj = HttpCapture(method, path, status, elapsed_ms, sanitized_body, body_text)
        if capture:
            self.captures.append(capture_obj)
            self.write_capture(assertion_id, capture_obj)
        if self.verbose:
            print(f"[adv-canary] {assertion_id} {method} {path} -> {status} ({elapsed_ms}ms)")
        return capture_obj

    def write_capture(self, assertion_id: str, capture: HttpCapture) -> None:
        safe_name = re.sub(r"[^A-Za-z0-9_.-]+", "_", f"{assertion_id}_{capture.method}_{capture.path}")
        path = self.evidence_dir / f"{len(self.captures):03d}_{safe_name}.json"
        path.write_text(
            json.dumps(
                {
                    "assertion": assertion_id,
                    "method": capture.method,
                    "path": capture.path,
                    "status": capture.status,
                    "elapsedMs": capture.elapsed_ms,
                    "body": capture.sanitized_body,
                },
                indent=2,
                sort_keys=True,
            ),
            encoding="utf-8",
        )

    def health(self) -> None:
        require_port_open(self.base_url, self.connect_timeout)
        require_port_open(self.mgmt_url, self.connect_timeout)
        url = self.mgmt_url + "/actuator/health"
        with urllib.request.urlopen(url, timeout=self.timeout) as response:
            if response.getcode() != 200:
                raise RuntimeError(f"actuator health failed with {response.getcode()}")

    def login(self, email: str, company_code: str, user_agent: str) -> dict[str, str]:
        raw, capture = self.raw_json_request(
            "VAL-CROSS-010",
            "POST",
            "/api/v1/auth/login",
            headers={"User-Agent": user_agent},
            body={"email": email, "password": self.seed_password, "companyCode": company_code},
            capture=True,
            allow_token_material=True,
        )
        if capture.status != 200:
            raise RuntimeError(f"expected login success for {email}/{company_code}, got {capture.status}")
        access = raw.get("accessToken")
        refresh = raw.get("refreshToken")
        if not isinstance(access, str) or not access:
            raise RuntimeError("login did not return an in-memory access token")
        if not isinstance(refresh, str) or not refresh:
            raise RuntimeError("login did not return an in-memory refresh token")
        return {"access": access, "refresh": refresh}

    def raw_json_request(
        self,
        assertion_id: str,
        method: str,
        path: str,
        *,
        body: dict[str, Any] | None = None,
        headers: dict[str, str] | None = None,
        capture: bool = False,
        allow_token_material: bool = False,
    ) -> tuple[dict[str, Any], HttpCapture]:
        """Return raw JSON only in memory; do not persist this response."""
        if QUERY_TOKEN_RE.search(path):
            raise RuntimeError(f"refusing token-like URL: {path}")
        url = self.base_url + path
        payload = json.dumps(body or {}, separators=(",", ":")).encode("utf-8")
        request_headers = {"Accept": "application/json", "Content-Type": "application/json"}
        if headers:
            request_headers.update(headers)
        req = urllib.request.Request(url, data=payload, method=method, headers=request_headers)
        start = time.monotonic()
        with self.budget.request(assertion_id):
            with urllib.request.urlopen(req, timeout=self.timeout) as response:
                raw = response.read(65536)
                status = response.getcode()
        elapsed_ms = round((time.monotonic() - start) * 1000)
        self.assert_raw_surface_clean(
            f"raw-http {method} {path}",
            raw_text(raw),
            allow_token_material=allow_token_material,
            strict_email=False,
        )
        sanitized_body, body_text = decode_json_or_text(raw)
        capture_obj = HttpCapture(method, path, status, elapsed_ms, sanitized_body, body_text)
        if capture:
            self.captures.append(capture_obj)
            self.write_capture(assertion_id, capture_obj)
        parsed = json.loads(raw.decode("utf-8"))
        if not isinstance(parsed, dict):
            raise RuntimeError("expected object JSON response")
        return parsed, capture_obj

    def scan_text_blob(
        self,
        label: str,
        text: str,
        *,
        strict_email: bool = False,
        allow_token_material: bool = False,
        detect_secret_json_fields: bool = True,
    ) -> list[str]:
        findings: list[str] = []
        for name, marker in self.canaries.items():
            if marker and marker in text:
                findings.append(f"{label}: canary {name} marker leaked")
        if not allow_token_material and JWT_RE.search(text):
            findings.append(f"{label}: JWT-shaped token leaked")
        if not allow_token_material and BEARER_RE.search(text):
            findings.append(f"{label}: bearer token leaked")
        if (
            detect_secret_json_fields
            and not allow_token_material
            and contains_unredacted_secret_json_field(text)
        ):
            findings.append(f"{label}: secret-bearing JSON field leaked")
        if not allow_token_material and QUERY_TOKEN_RE.search(text):
            findings.append(f"{label}: token-like URL query material leaked")
        if strict_email and EMAIL_RE.search(text):
            findings.append(f"{label}: email-shaped PII remained in sanitized evidence")
        return findings

    def assert_raw_surface_clean(
        self,
        label: str,
        text: str,
        *,
        allow_token_material: bool,
        strict_email: bool,
    ) -> None:
        findings = self.scan_text_blob(
            label,
            text,
            strict_email=strict_email,
            allow_token_material=allow_token_material,
        )
        if findings:
            raise RuntimeError("raw pre-redaction scan failed:\n  - " + "\n  - ".join(findings))

    def scan_path(
        self,
        path: Path,
        strict_email: bool = False,
        detect_secret_json_fields: bool = True,
    ) -> list[str]:
        findings: list[str] = []
        if not path.exists():
            return findings
        if path.is_file():
            candidates = [path]
        else:
            candidates = [p for p in path.rglob("*") if p.is_file() and p.stat().st_size <= 2_000_000]
        for candidate in candidates:
            try:
                text = candidate.read_text(encoding="utf-8", errors="ignore")
            except OSError as exc:
                findings.append(f"{candidate}: could not read for scan: {exc}")
                continue
            findings.extend(
                self.scan_text_blob(
                    str(candidate),
                    text,
                    strict_email=strict_email,
                    detect_secret_json_fields=detect_secret_json_fields,
                )
            )
        return findings

    def fetch_mailhog(self) -> str:
        url = self.mailhog_url + "/api/v2/messages"
        try:
            with urllib.request.urlopen(url, timeout=self.timeout) as response:
                raw = response.read(500_000)
        except Exception as exc:  # MailHog may be unavailable in non-mail flows.
            return json.dumps({"mailhogUnavailable": str(exc)})
        decoded_raw = raw_text(raw)
        self.assert_raw_surface_clean(
            "raw-mailhog",
            decoded_raw,
            allow_token_material=False,
            strict_email=False,
        )
        sanitized = sanitize_text(decoded_raw)
        (self.evidence_dir / "mailhog_sanitized.json").write_text(sanitized, encoding="utf-8")
        return sanitized

    def query_audit_metadata(self) -> str:
        docker = shutil.which("docker")
        if not docker:
            return json.dumps({"auditScan": "skipped", "reason": "docker unavailable"})
        cmd = [
            docker,
            "exec",
            "iam-hardcut-postgres",
            "psql",
            "-U",
            "erp",
            "-d",
            "erp_domain",
            "-At",
            "-c",
            (
                "select coalesce(jsonb_agg(row_to_json(t)), '[]'::jsonb) "
                "from ("
                "select event_type, outcome, auth_scope_code, metadata::text as metadata "
                "from iam_security_events order by occurred_at desc limit 50"
                ") t"
            ),
        ]
        try:
            result = subprocess.run(cmd, check=False, text=True, capture_output=True, timeout=10)
        except Exception as exc:
            return json.dumps({"auditScan": "skipped", "reason": str(exc)})
        output = result.stdout if result.returncode == 0 else result.stderr
        self.assert_raw_surface_clean(
            "raw-audit-metadata",
            output,
            allow_token_material=False,
            strict_email=False,
        )
        sanitized = sanitize_text(output)
        (self.evidence_dir / "audit_metadata_sanitized.json").write_text(sanitized, encoding="utf-8")
        return sanitized

    def scan_backend_logs(self) -> list[str]:
        findings: list[str] = []
        for path in self.backend_log_paths:
            if path.is_file():
                findings.extend(self.scan_path(path))
            elif self.verbose:
                print(f"[adv-canary] backend log path not found/skipped: {path}", file=sys.stderr)
        return findings

    def expect_probe(self, capture: HttpCapture, expectation: ProbeExpectation) -> None:
        if capture.status not in expectation.expected_statuses:
            raise RuntimeError(
                f"{expectation.name} expected status "
                f"{'/'.join(map(str, expectation.expected_statuses))}, got {capture.status}"
            )
        if capture.status == 200 and not expectation.allow_200:
            raise RuntimeError(f"{expectation.name} unexpectedly returned HTTP 200")
        for fragment in expectation.required_fragments:
            if fragment.lower() not in capture.body_text.lower():
                raise RuntimeError(
                    f"{expectation.name} response missing required body invariant "
                    f"{fragment!r}: {capture.body_text}"
                )
        if response_has_success_true(capture.sanitized_body):
            raise RuntimeError(f"{expectation.name} returned success=true on denial/absence probe")
        if expectation.forbid_auth_payload and response_contains_auth_payload(capture.sanitized_body):
            raise RuntimeError(
                f"{expectation.name} returned usable auth/profile/session payload fields"
            )

    def run(self) -> dict[str, Any]:
        self.health()
        admin = self.login(
            "validation.admin@example.com",
            "MOCK",
            self.canaries["user_agent"],
        )
        rival = self.login(
            "validation.rival.admin@example.com",
            "RIVAL",
            "AdvCanaryRival/" + uuid.uuid4().hex,
        )
        auth = {"Authorization": f"Bearer {admin['access']}"}
        denial_expectations = {
            "tenant-mismatch": ProbeExpectation(
                "tenant mismatch X-Company-Code",
                (403,),
                ("success", "false", "COMPANY_CONTEXT_MISMATCH"),
            ),
            "legacy-company-id": ProbeExpectation(
                "legacy X-Company-Id",
                (403,),
                ("success", "false", "COMPANY_CONTEXT_LEGACY_HEADER_UNSUPPORTED"),
            ),
            "retired-profile": ProbeExpectation(
                "retired auth profile route",
                (404, 405),
            ),
            "wrong-scope-refresh": ProbeExpectation(
                "wrong-scope refresh token",
                (400,),
                ("success", "false", "Invalid refresh token"),
            ),
            "malformed-login": ProbeExpectation(
                "malformed login payload",
                (400,),
                ("success", "false"),
            ),
            "malformed-refresh": ProbeExpectation(
                "malformed refresh payload",
                (400,),
                ("success", "false", "Invalid refresh token"),
            ),
            "malformed-mfa-activate": ProbeExpectation(
                "malformed MFA activate payload",
                (400,),
                ("success", "false"),
            ),
            "malformed-mfa-disable": ProbeExpectation(
                "malformed MFA disable payload",
                (400,),
                ("success", "false"),
            ),
        }
        self.request("VAL-ADV-013", "GET", "/api/v1/auth/me", headers=auth)
        self.request("VAL-ADV-013", "GET", "/api/v1/auth/sessions", headers=auth)
        self.expect_probe(
            self.request(
                "VAL-CROSS-010",
                "GET",
                "/api/v1/auth/me",
                headers={**auth, "X-Company-Code": "RIVAL"},
            ),
            denial_expectations["tenant-mismatch"],
        )
        self.expect_probe(
            self.request(
                "VAL-CROSS-010",
                "GET",
                "/api/v1/auth/me",
                headers={**auth, "X-Company-Id": self.canaries["tenant"]},
            ),
            denial_expectations["legacy-company-id"],
        )
        self.expect_probe(
            self.request("VAL-CROSS-010", "GET", "/api/v1/auth/profile", headers=auth),
            denial_expectations["retired-profile"],
        )
        self.request(
            "VAL-ADV-013",
            "POST",
            "/api/v1/auth/login",
            headers={"User-Agent": self.canaries["user_agent"] + "-bad-login"},
            body={
                "email": self.canaries["email"],
                "password": self.canaries["password"],
                "companyCode": self.canaries["tenant"],
            },
        )
        self.request(
            "VAL-ADV-013",
            "POST",
            "/api/v1/auth/password/forgot",
            body={
                "email": self.canaries["email"],
                "companyCode": "MOCK",
                "phone": self.canaries["phone"],
                "resetHint": self.canaries["reset"],
            },
        )
        self.expect_probe(
            self.request(
                "VAL-CROSS-011",
                "POST",
                "/api/v1/auth/refresh-token",
                body={"refreshToken": admin["refresh"], "companyCode": "RIVAL"},
            ),
            denial_expectations["wrong-scope-refresh"],
        )
        self.request(
            "VAL-CROSS-011",
            "GET",
            "/api/v1/auth/me/security-events?type=SESSION&page=0&size=5",
            headers=auth,
        )

        malformed_payloads = [
            (
                "malformed-login",
                "/api/v1/auth/login",
                {
                    "email": "not-an-email",
                    "password": self.canaries["password"],
                    "companyCode": "MOCK",
                },
            ),
            (
                "malformed-refresh",
                "/api/v1/auth/refresh-token",
                {"refreshToken": self.canaries["token"], "companyCode": "MOCK"},
            ),
            (
                "malformed-mfa-activate",
                "/api/v1/auth/mfa/activate",
                {"code": self.canaries["mfa"], "factorType": "sms"},
            ),
            (
                "malformed-mfa-disable",
                "/api/v1/auth/mfa/disable",
                {"recoveryCode": self.canaries["recovery"], "factorType": "sms"},
            ),
        ]
        with concurrent.futures.ThreadPoolExecutor(max_workers=2) as executor:
            futures = [
                executor.submit(
                    self.request,
                    "VAL-CROSS-010",
                    "POST",
                    path,
                    body=payload,
                    headers=auth if "mfa" in path else None,
                )
                for _, path, payload in malformed_payloads
            ]
            expectation_by_future = {
                future: denial_expectations[name]
                for future, (name, _, _) in zip(futures, malformed_payloads)
            }
            for future in concurrent.futures.as_completed(futures):
                capture = future.result()
                self.expect_probe(capture, expectation_by_future[future])

        self.request(
            "VAL-CROSS-011",
            "POST",
            "/api/v1/auth/logout",
            headers=auth,
            body={"refreshToken": admin["refresh"]},
        )
        self.request(
            "VAL-CROSS-010",
            "GET",
            "/api/v1/auth/me",
            headers={"Authorization": f"Bearer {rival['access']}"},
        )
        self.request(
            "VAL-CROSS-011",
            "POST",
            "/api/v1/auth/logout",
            headers={"Authorization": f"Bearer {rival['access']}"},
            body={"refreshToken": rival["refresh"]},
        )

        findings: list[str] = []
        findings.extend(self.scan_path(self.evidence_dir, strict_email=True))
        findings.extend(self.scan_path(ROOT / "openapi.json"))
        findings.extend(
            self.scan_path(ROOT / "docs" / "frontend-api", detect_secret_json_fields=False)
        )
        findings.extend(
            self.scan_path(ROOT / "docs" / "frontend-portals", detect_secret_json_fields=False)
        )
        mailhog_text = self.fetch_mailhog()
        findings.extend(self.scan_text_blob("mailhog", mailhog_text))
        audit_text = self.query_audit_metadata()
        findings.extend(self.scan_text_blob("audit-metadata", audit_text))
        findings.extend(self.scan_backend_logs())

        if findings:
            raise RuntimeError("canary/secret scan failed:\n  - " + "\n  - ".join(findings))

        statuses: dict[str, int] = {}
        for capture in self.captures:
            key = f"{capture.method} {capture.path} -> {capture.status}"
            statuses[key] = statuses.get(key, 0) + 1
        return {
            "assertions": ["VAL-ADV-013", "VAL-CROSS-010", "VAL-CROSS-011"],
            "requestCount": self.budget.total,
            "requestCountsByAssertion": dict(sorted(self.budget.by_assertion.items())),
            "maxConcurrencyObserved": self.budget.max_seen_concurrency,
            "perAssertionCap": self.budget.per_assertion_limit,
            "wholeSuiteCap": self.budget.total_limit,
            "fixtureProtocol": {
                "runtime": "validation-seed profile on mission localhost ports",
                "users": [
                    "validation.admin@example.com / MOCK",
                    "validation.rival.admin@example.com / RIVAL",
                ],
                "resetProtocol": "fresh mission runtime is recommended before each run; harness revokes only sessions it creates",
            },
            "stopConditions": [
                "non-local URL",
                "request/concurrency budget breach",
                "unexpected broad status outside allowed auth/error set",
                "actuator health unavailable",
                "canary/token/PII scan finding",
            ],
            "scanSurfaces": [
                "sanitized temp evidence",
                "openapi.json",
                "docs/frontend-api",
                "docs/frontend-portals",
                "MailHog API",
                "iam_security_events metadata via mission Postgres container when available",
                "backend log when --backend-log or supported environment defaults are supplied",
            ],
            "statusSummary": dict(sorted(statuses.items())),
            "result": "PASS",
        }


def main() -> int:
    args = parse_args()
    if args.max_concurrency > 2:
        print("max concurrency must be <= 2", file=sys.stderr)
        return 2
    if args.max_requests > 100:
        print("max requests must be <= 100", file=sys.stderr)
        return 2
    seed_password = os.environ.get(args.seed_password_env, "")
    if not seed_password:
        print(
            f"{args.seed_password_env} must be set for validation-seed runtime; "
            "the value is consumed in memory and never printed.",
            file=sys.stderr,
        )
        return 2
    evidence_dir = Path(tempfile.mkdtemp(prefix="iam-adv-canary-", dir=tempfile.gettempdir()))
    summary: dict[str, Any] | None = None
    try:
        harness = Harness(args, seed_password, evidence_dir)
        summary = harness.run()
        summary["tempEvidenceCleanup"] = {
            "path": str(evidence_dir),
            "scannedBeforeDeletion": True,
            "deleted": False,
        }
        print(json.dumps(summary, indent=2, sort_keys=True))
        return 0
    except Exception as exc:
        print(f"[adv-canary] ERROR: {exc}", file=sys.stderr)
        if summary:
            print(json.dumps(summary, indent=2, sort_keys=True), file=sys.stderr)
        return 1
    finally:
        shutil.rmtree(evidence_dir, ignore_errors=True)
        if summary is not None:
            print(
                json.dumps(
                    {
                        "tempEvidenceCleanup": {
                            "path": str(evidence_dir),
                            "existsAfterCleanup": evidence_dir.exists(),
                        }
                    },
                    sort_keys=True,
                )
            )


if __name__ == "__main__":
    raise SystemExit(main())
