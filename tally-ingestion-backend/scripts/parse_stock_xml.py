import argparse
import csv
from decimal import Decimal, InvalidOperation
from pathlib import Path
import xml.etree.ElementTree as ET


def parse_qty(raw: str):
    """
    Parse quantities like '6212 PCS' or '5 DRM'. Returns (qty Decimal, unit str).
    """
    if not raw:
        return None, None
    parts = raw.strip().split()
    if not parts:
        return None, None
    if len(parts) == 1:
        # No unit provided
        try:
            return Decimal(parts[0]), None
        except InvalidOperation:
            return None, None
    unit = parts[-1]
    qty_part = " ".join(parts[:-1])
    try:
        qty = Decimal(qty_part)
    except InvalidOperation:
        return None, unit
    return qty, unit


def parse_decimal(raw: str):
    if raw is None:
        return None
    raw = raw.strip()
    if raw == "":
        return None
    try:
        return Decimal(raw)
    except InvalidOperation:
        return None


def extract_items(xml_path: Path):
    tree = ET.parse(xml_path)
    root = tree.getroot()
    children = list(root)
    items = []

    for idx, node in enumerate(children):
        if node.tag != "DSPACCNAME":
            continue

        name = (node.findtext("DSPDISPNAME") or "").strip()
        qty = rate = amount = unit = None

        # Look ahead for the stock info block
        if idx + 1 < len(children) and children[idx + 1].tag == "DSPSTKINFO":
            stk_info = children[idx + 1]
            qty_raw = stk_info.findtext(".//DSPCLQTY")
            rate_raw = stk_info.findtext(".//DSPCLRATE")
            amt_raw = stk_info.findtext(".//DSPCLAMTA")

            qty, unit = parse_qty(qty_raw or "")
            rate = parse_decimal(rate_raw)
            amount = parse_decimal(amt_raw)

        items.append(
            {
                "stock_item_name": name,
                "closing_qty": qty,
                "unit": unit,
                "closing_rate": rate,
                "closing_amount": amount,
            }
        )

    return items


def write_products_csv(items, out_path: Path):
    """
    Emit a products CSV compatible with the existing CSV ingestor.
    Extra fields are retained in raw_data; the ingestor will ignore unknown columns.
    """
    fieldnames = [
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
        "closing_qty",
        "closing_rate",
        "closing_amount",
        "closing_amount_abs",
    ]
    with out_path.open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        for item in items:
            row = {key: "" for key in fieldnames}
            row["stock_item_name"] = item["stock_item_name"]
            row["base_unit"] = item.get("unit") or ""
            row["closing_qty"] = item.get("closing_qty") or ""
            row["closing_rate"] = item.get("closing_rate") or ""
            amount = item.get("closing_amount")
            row["closing_amount"] = amount or ""
            row["closing_amount_abs"] = abs(amount) if amount is not None else ""
            writer.writerow(row)


def write_opening_stock_csv(items, out_path: Path):
    """
    Emit a simple opening stock file for downstream inventory adjustments.
    """
    fieldnames = ["stock_item_name", "qty", "unit", "rate", "amount", "amount_abs"]
    with out_path.open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        for item in items:
            amount = item.get("closing_amount")
            amount_abs = abs(amount) if amount is not None else ""
            writer.writerow(
                {
                    "stock_item_name": item["stock_item_name"],
                    "qty": item.get("closing_qty") or "",
                    "unit": item.get("unit") or "",
                    "rate": item.get("closing_rate") or "",
                    "amount": amount or "",
                    "amount_abs": amount_abs,
                }
            )


def main():
    parser = argparse.ArgumentParser(description="Parse Tally stock summary XML into CSVs.")
    parser.add_argument("--xml", required=True, type=Path, help="Path to StkSum.xml")
    parser.add_argument(
        "--outdir",
        type=Path,
        default=Path("tally-ingestion-backend") / "output",
        help="Output directory (default: tally-ingestion-backend/output)",
    )
    args = parser.parse_args()

    args.outdir.mkdir(parents=True, exist_ok=True)

    items = extract_items(args.xml)
    products_path = args.outdir / "products_from_tally.csv"
    opening_stock_path = args.outdir / "opening_stock.csv"

    write_products_csv(items, products_path)
    write_opening_stock_csv(items, opening_stock_path)

    print(f"Wrote {len(items)} items")
    print(f"Products CSV: {products_path}")
    print(f"Opening stock CSV: {opening_stock_path}")


if __name__ == "__main__":
    main()
