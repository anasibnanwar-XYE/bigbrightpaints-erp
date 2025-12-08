"""
Tally Master → ERP staging converter.

Generates editable CSVs (products, dealers, accounts, trial balance) from Tally exports.
Fields align with staging tables used by tally-ingestion-backend (stg_tally_*).
Required-but-unknown values are left blank; rows needing operator attention are flagged.

Outputs (to --outdir, default: tally-ingestion-backend/output):
  - products_master.csv
  - dealers_master.csv
  - accounts_master.csv
  - trial_balance.csv
  - issues.json (duplicates / needs_review summary for your GUI)

Usage:
  python tally_master_convert.py --xml path/to/export.xml --outdir output/
"""

import argparse
import csv
import hashlib
import json
from collections import defaultdict
from decimal import Decimal, InvalidOperation
from pathlib import Path
from typing import Dict, List, Optional, Tuple
import xml.etree.ElementTree as ET


# ---------- Basic parsing helpers ----------
def parse_decimal(raw: Optional[str]) -> Optional[Decimal]:
    if raw is None:
        return None
    txt = raw.strip().replace(",", "")
    if txt == "":
        return None
    try:
        return Decimal(txt)
    except InvalidOperation:
        return None


def parse_qty_with_unit(raw: Optional[str]) -> Tuple[Optional[Decimal], Optional[str]]:
    if not raw:
        return None, None
    parts = raw.strip().split()
    if not parts:
        return None, None
    if len(parts) == 1:
        return parse_decimal(parts[0]), None
    qty = parse_decimal(" ".join(parts[:-1]))
    unit = parts[-1].upper()
    return qty, unit


def normalize_uom(uom: Optional[str]) -> Optional[str]:
    if not uom:
        return None
    u = uom.strip().upper()
    mapping = {
        "PCS": "PCS",
        "PC": "PCS",
        "NOS": "PCS",
        "KG": "KG",
        "KGS": "KG",
        "GM": "GM",
        "GMS": "GM",
        "G": "GM",
        "LTR": "LTR",
        "L": "LTR",
        "LT": "LTR",
        "LITRE": "LTR",
        "LTRS": "LTR",
        "ML": "ML",
        "DRM": "DRM",
        "DRUM": "DRUM",
        "BAG": "BAG",
        "BAGS": "BAG",
        "BOX": "BOX",
        "CTN": "CTN",
        "CARTON": "CTN",
        "SET": "SET",
    }
    return mapping.get(u, u)


def norm_name(name: Optional[str]) -> str:
    if not name:
        return ""
    return " ".join(name.lower().strip().split())


def source_hash(*values: str) -> str:
    h = hashlib.sha256()
    for v in values:
        h.update((v or "").encode("utf-8"))
    return h.hexdigest()[:16]


# ---------- XML parsing ----------
def parse_ledgers(doc: ET.ElementTree) -> List[Dict]:
    ledgers = []

    def split_dc(val: Optional[str]) -> Tuple[Optional[Decimal], Optional[str]]:
        if not val:
            return None, None
        v = val.strip()
        dc = None
        if v.lower().endswith("dr") or v.lower().endswith("cr"):
            dc = v[-2:]
            v = v[:-2]
        return parse_decimal(v), dc

    for led in doc.iterfind(".//LEDGER"):
        name = (led.get("NAME") or led.findtext("NAME") or "").strip()
        if not name:
            continue
        ledger_group = (led.findtext("PARENT") or "").strip()
        opening = led.findtext("OPENINGBALANCE")
        closing = led.findtext("CLOSINGBALANCE")
        opening_amt, opening_dc = split_dc(opening)
        closing_amt, closing_dc = split_dc(closing)

        address_parts = []
        for tag in ("ADDRESS", "MAILINGNAME", "LEDSTATENAME", "COUNTRYNAME"):
            txt = led.findtext(tag)
            if txt:
                address_parts.append(txt.strip())

        ledger = {
            "ledger_name": name,
            "ledger_group": ledger_group,
            "opening_balance": opening_amt,
            "opening_type": opening_dc,
            "closing_balance": closing_amt,
            "closing_type": closing_dc,
            "address": ", ".join(address_parts),
            "state": led.findtext("STATE"),
            "pincode": led.findtext("PINCODE"),
            "gstin": led.findtext("PARTYGSTIN") or led.findtext("GSTIN"),
            "pan": led.findtext("INCOMETAXNUMBER") or led.findtext("PAN"),
            "phone": led.findtext("PHONE"),
            "email": led.findtext("EMAIL"),
            "bank_name": led.findtext("BANKNAME"),
            "account_number": led.findtext("BANKACCOUNTNUMBER"),
            "ifsc_code": led.findtext("IFSCODE"),
        }
        ledgers.append(ledger)
    return ledgers


def parse_stock_items(doc: ET.ElementTree) -> List[Dict]:
    items: List[Dict] = []
    root = doc.getroot()

    # Pass 1: Stock Summary style (DSPACCNAME followed by DSPSTKINFO)
    for parent in root.iter():
        children = list(parent)
        for idx, child in enumerate(children):
            if child.tag != "DSPACCNAME":
                continue
            name = (child.findtext("DSPDISPNAME") or child.text or "").strip()
            if not name:
                continue
            qty = rate = amount = None
            unit = None
            if idx + 1 < len(children) and children[idx + 1].tag == "DSPSTKINFO":
                stk = children[idx + 1]
                qty_raw = stk.findtext(".//DSPCLQTY") or stk.findtext(".//DSPSTKC")
                rate_raw = stk.findtext(".//DSPCLRATE")
                amt_raw = stk.findtext(".//DSPCLAMTA") or stk.findtext(".//DSPCLAMT")
                qty, unit_raw = parse_qty_with_unit(qty_raw)
                unit = normalize_uom(unit_raw)
                rate = parse_decimal(rate_raw)
                amount = parse_decimal(amt_raw)

            items.append(
                {
                    "stock_item_name": name,
                    "stock_group": None,
                    "stock_category": None,
                    "base_unit": unit,
                    "alternate_unit": None,
                    "conversion_factor": None,
                    "gst_rate": None,
                    "hsn_code": None,
                    "item_code": None,
                    "barcode": None,
                    "brand": None,
                    "base_product_name": name,
                    "color": None,
                    "size": None,
                    "pack_size": None,
                    "opening_qty": qty,
                    "opening_rate": rate,
                    "opening_amount": amount,
                }
            )

    # Pass 2: STOCKITEM masters (metadata like HSN/UOM)
    for si in doc.iterfind(".//STOCKITEM"):
        name = (si.get("NAME") or si.findtext("NAME") or "").strip()
        if not name:
            continue
        base_unit = normalize_uom(si.findtext("BASEUNITS"))
        alt_unit = normalize_uom(si.findtext("ADDITIONALUNITS"))
        hsn = (si.findtext("GSTAPPLICABLE") or si.findtext("HSNCODE") or "").strip() or None
        gst_rate = parse_decimal(si.findtext("RATEOFTAXCALCULATION") or si.findtext("GSTOVRDN"))
        barcode = si.findtext("BARCODE")
        item_code = si.findtext("PARTNUMBER") or si.findtext("ITEMCODE")
        stock_group = si.findtext("PARENT")

        items.append(
            {
                "stock_item_name": name,
                "stock_group": stock_group,
                "stock_category": None,
                "base_unit": base_unit,
                "alternate_unit": alt_unit,
                "conversion_factor": None,
                "gst_rate": gst_rate,
                "hsn_code": hsn,
                "item_code": item_code,
                "barcode": barcode,
                "brand": None,
                "base_product_name": name,
                "color": None,
                "size": None,
                "pack_size": None,
                "opening_qty": None,
                "opening_rate": None,
                "opening_amount": None,
            }
        )

    return items


# ---------- Issues / dedupe ----------
def flag_duplicates(records: List[Dict], keys: List[str]) -> List[Dict]:
    issues = []
    buckets: Dict[str, List[int]] = defaultdict(list)
    for idx, rec in enumerate(records):
        sig_parts = [norm_name(rec.get(k)) for k in keys]
        if all(part == "" for part in sig_parts):
            continue
        sig = "|".join(sig_parts)
        buckets[sig].append(idx)
    for sig, idxs in buckets.items():
        if len(idxs) > 1:
            issues.append({"type": "duplicate", "key": sig, "rows": idxs})
    return issues


# ---------- CSV writers ----------
def write_csv(path: Path, rows: List[Dict], fieldnames: List[str]):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=fieldnames)
        w.writeheader()
        for row in rows:
            safe = {fn: row.get(fn, "") for fn in fieldnames}
            w.writerow(safe)


# ---------- Main conversion ----------
def main():
    parser = argparse.ArgumentParser(description="Convert Tally XML masters to editable CSVs.")
    parser.add_argument("--xml", required=True, type=Path, help="Path to Tally XML export")
    parser.add_argument("--outdir", type=Path, default=Path("tally-ingestion-backend") / "output")
    args = parser.parse_args()

    doc = ET.parse(args.xml)

    products_raw = parse_stock_items(doc)
    ledgers_raw = parse_ledgers(doc)

    # Build products CSV
    product_rows = []
    product_issues = []
    seen_prod = set()
    for p in products_raw:
        key = (norm_name(p["stock_item_name"]), p.get("base_unit"))
        needs_review = False
        if not p.get("base_unit"):
            needs_review = True
        if not p.get("stock_item_name"):
            needs_review = True
        if key in seen_prod:
            needs_review = True
        seen_prod.add(key)
        row = {
            **p,
            "source_hash": source_hash(p.get("stock_item_name", ""), str(p.get("opening_qty", "")), str(p.get("opening_amount", ""))),
            "needs_review": "TRUE" if needs_review else "",
            "notes": "",
        }
        product_rows.append(row)
    product_issues.extend(flag_duplicates(product_rows, ["stock_item_name", "base_unit"]))

    # Dealers (subset of ledgers that look like customers)
    dealer_rows = []
    dealer_issues = []
    for led in ledgers_raw:
        # heuristics: Sundry Debtors or has GSTIN (customer-like)
        group = (led.get("ledger_group") or "").lower()
        is_debtor = "debtor" in group or "sundry" in group or led.get("gstin")
        if not is_debtor:
            continue
        needs_review = False
        if not led.get("gstin") and not led.get("phone") and not led.get("email"):
            needs_review = True
        row = {
            "party_name": led["ledger_name"],
            "ledger_name": led["ledger_name"],
            "address": led.get("address"),
            "city": None,
            "state": led.get("state"),
            "pincode": led.get("pincode"),
            "country": None,
            "email": led.get("email"),
            "phone": led.get("phone"),
            "mobile": led.get("phone"),
            "pan": led.get("pan"),
            "gstin": led.get("gstin"),
            "credit_period_days": None,
            "credit_limit": None,
            "opening_balance": led.get("opening_balance"),
            "dr_cr": led.get("opening_type"),
            "mapped_user_email": "",
            "mapped_user_id": "",
            "source_hash": source_hash(led["ledger_name"], led.get("gstin") or ""),
            "needs_review": "TRUE" if needs_review else "",
            "notes": "",
        }
        dealer_rows.append(row)
    dealer_issues.extend(flag_duplicates(dealer_rows, ["gstin"]))
    dealer_issues.extend(flag_duplicates(dealer_rows, ["ledger_name"]))

    # Accounts (all ledgers)
    account_rows = []
    account_issues = []
    for led in ledgers_raw:
        needs_review = False
        if not led.get("ledger_group"):
            needs_review = True
        row = {
            "ledger_name": led["ledger_name"],
            "ledger_group": led.get("ledger_group"),
            "opening_balance": led.get("opening_balance"),
            "dr_cr": led.get("opening_type"),
            "address": led.get("address"),
            "city": None,
            "state": led.get("state"),
            "pincode": led.get("pincode"),
            "pan": led.get("pan"),
            "gstin": led.get("gstin"),
            "bank_name": led.get("bank_name"),
            "account_number": led.get("account_number"),
            "ifsc_code": led.get("ifsc_code"),
            "source_hash": source_hash(led["ledger_name"], led.get("ledger_group") or ""),
            "needs_review": "TRUE" if needs_review else "",
            "notes": "",
        }
        account_rows.append(row)
    account_issues.extend(flag_duplicates(account_rows, ["ledger_name"]))
    account_issues.extend(flag_duplicates(account_rows, ["gstin"]))

    # Trial balance helper
    trial_rows = []
    for led in ledgers_raw:
        trial_rows.append(
            {
                "ledger_name": led["ledger_name"],
                "ledger_group": led.get("ledger_group"),
                "opening_balance": led.get("opening_balance"),
                "opening_type": led.get("opening_type"),
                "closing_balance": led.get("closing_balance"),
                "closing_type": led.get("closing_type"),
                "source_hash": source_hash(led["ledger_name"], led.get("closing_balance") or ""),
            }
        )

    # Write outputs
    outdir: Path = args.outdir
    write_csv(
        outdir / "products_master.csv",
        product_rows,
        [
            "stock_item_name",
            "stock_group",
            "stock_category",
            "base_unit",
            "alternate_unit",
            "conversion_factor",
            "gst_rate",
            "hsn_code",
            "item_code",
            "barcode",
            "brand",
            "base_product_name",
            "color",
            "size",
            "pack_size",
            "opening_qty",
            "opening_rate",
            "opening_amount",
            "source_hash",
            "needs_review",
            "notes",
        ],
    )

    write_csv(
        outdir / "dealers_master.csv",
        dealer_rows,
        [
            "party_name",
            "ledger_name",
            "address",
            "city",
            "state",
            "pincode",
            "country",
            "email",
            "phone",
            "mobile",
            "pan",
            "gstin",
            "credit_period_days",
            "credit_limit",
            "opening_balance",
            "dr_cr",
            "mapped_user_email",
            "mapped_user_id",
            "source_hash",
            "needs_review",
            "notes",
        ],
    )

    write_csv(
        outdir / "accounts_master.csv",
        account_rows,
        [
            "ledger_name",
            "ledger_group",
            "opening_balance",
            "dr_cr",
            "address",
            "city",
            "state",
            "pincode",
            "pan",
            "gstin",
            "bank_name",
            "account_number",
            "ifsc_code",
            "source_hash",
            "needs_review",
            "notes",
        ],
    )

    write_csv(
        outdir / "trial_balance.csv",
        trial_rows,
        [
            "ledger_name",
            "ledger_group",
            "opening_balance",
            "opening_type",
            "closing_balance",
            "closing_type",
            "source_hash",
        ],
    )

    issues = {
        "products": product_issues,
        "dealers": dealer_issues,
        "accounts": account_issues,
    }
    (outdir / "issues.json").write_text(json.dumps(issues, indent=2), encoding="utf-8")

    print(f"Products: {len(product_rows)} | Dealers: {len(dealer_rows)} | Accounts: {len(account_rows)}")
    print(f"Issues file: {(outdir / 'issues.json').resolve()}")


if __name__ == "__main__":
    main()
