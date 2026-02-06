#!/usr/bin/env python3
import argparse
import json
import os
import sys
import xml.etree.ElementTree as ET


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Enforce module/package coverage from JaCoCo XML")
    p.add_argument("--jacoco", required=True, help="Path to jacoco.xml")
    p.add_argument("--packages", required=True, help="Comma-separated package prefixes (dot notation)")
    p.add_argument("--classes", default="", help="Optional comma-separated class names (dot notation)")
    p.add_argument("--line-threshold", type=float, default=0.92)
    p.add_argument("--branch-threshold", type=float, default=0.85)
    p.add_argument("--active-classes-only", action="store_true",
                   help="Evaluate only classes that have runtime-covered lines/branches in this run")
    p.add_argument("--min-active-classes", type=int, default=0,
                   help="Minimum number of active classes required to pass")
    p.add_argument("--min-active-packages", type=int, default=0,
                   help="Minimum number of packages with active class coverage required to pass")
    p.add_argument("--output", default="")
    return p.parse_args()


def main() -> int:
    args = parse_args()
    package_prefixes = [p.strip().replace(".", "/") for p in args.packages.split(",") if p.strip()]
    class_filters = [c.strip().replace(".", "/") for c in args.classes.split(",") if c.strip()]
    tree = ET.parse(args.jacoco)
    root = tree.getroot()

    covered_line = 0
    missed_line = 0
    covered_branch = 0
    missed_branch = 0
    matched_packages: list[str] = []
    matched_classes = 0
    active_classes = 0
    active_packages: set[str] = set()
    class_summaries: list[dict[str, object]] = []

    for pkg in root.findall("./package"):
        name = pkg.get("name", "")
        if not any(name.startswith(prefix) for prefix in package_prefixes):
            continue
        for clazz in pkg.findall("class"):
            class_name = clazz.get("name", "")
            if class_filters and not any(class_name == prefix or class_name.startswith(prefix + "$")
                                         for prefix in class_filters):
                continue

            matched_classes += 1
            matched_packages.append(name.replace("/", "."))
            class_line_covered = 0
            class_line_missed = 0
            class_branch_covered = 0
            class_branch_missed = 0
            for counter in clazz.findall("counter"):
                ctype = counter.get("type")
                covered = int(counter.get("covered", "0"))
                missed = int(counter.get("missed", "0"))
                if ctype == "LINE":
                    class_line_covered = covered
                    class_line_missed = missed
                elif ctype == "BRANCH":
                    class_branch_covered = covered
                    class_branch_missed = missed

            is_active = (class_line_covered > 0) or (class_branch_covered > 0)
            if args.active_classes_only and not is_active:
                class_summaries.append({
                    "class": class_name.replace("/", "."),
                    "package": name.replace("/", "."),
                    "active": False,
                    "line_covered": class_line_covered,
                    "line_total": class_line_covered + class_line_missed,
                    "line_ratio": (class_line_covered / (class_line_covered + class_line_missed))
                    if (class_line_covered + class_line_missed) else 1.0,
                    "branch_covered": class_branch_covered,
                    "branch_total": class_branch_covered + class_branch_missed,
                    "branch_ratio": (class_branch_covered / (class_branch_covered + class_branch_missed))
                    if (class_branch_covered + class_branch_missed) else 1.0,
                })
                continue

            if is_active:
                active_classes += 1
                active_packages.add(name.replace("/", "."))

            covered_line += class_line_covered
            missed_line += class_line_missed
            covered_branch += class_branch_covered
            missed_branch += class_branch_missed
            class_summaries.append({
                "class": class_name.replace("/", "."),
                "package": name.replace("/", "."),
                "active": is_active,
                "line_covered": class_line_covered,
                "line_total": class_line_covered + class_line_missed,
                "line_ratio": (class_line_covered / (class_line_covered + class_line_missed))
                if (class_line_covered + class_line_missed) else 1.0,
                "branch_covered": class_branch_covered,
                "branch_total": class_branch_covered + class_branch_missed,
                "branch_ratio": (class_branch_covered / (class_branch_covered + class_branch_missed))
                if (class_branch_covered + class_branch_missed) else 1.0,
            })

    line_total = covered_line + missed_line
    branch_total = covered_branch + missed_branch
    line_ratio = (covered_line / line_total) if line_total else 1.0
    branch_ratio = (covered_branch / branch_total) if branch_total else 1.0
    active_class_pass = active_classes >= args.min_active_classes
    active_package_pass = len(active_packages) >= args.min_active_packages
    passes = (
            line_ratio >= args.line_threshold
            and branch_ratio >= args.branch_threshold
            and active_class_pass
            and active_package_pass
    )

    summary = {
        "matched_packages": sorted(set(matched_packages)),
        "matched_classes": matched_classes,
        "class_filters": [c.replace("/", ".") for c in class_filters],
        "active_classes": active_classes,
        "active_classes_only": args.active_classes_only,
        "min_active_classes": args.min_active_classes,
        "active_packages": sorted(active_packages),
        "min_active_packages": args.min_active_packages,
        "line_covered": covered_line,
        "line_total": line_total,
        "line_ratio": line_ratio,
        "line_threshold": args.line_threshold,
        "branch_covered": covered_branch,
        "branch_total": branch_total,
        "branch_ratio": branch_ratio,
        "branch_threshold": args.branch_threshold,
        "class_summaries": class_summaries,
        "passes": passes,
    }

    print("[module_coverage_gate] summary:")
    print(json.dumps(summary, indent=2))

    if args.output:
        os.makedirs(os.path.dirname(args.output), exist_ok=True)
        with open(args.output, "w", encoding="utf-8") as fh:
            json.dump(summary, fh, indent=2)

    return 0 if passes else 1


if __name__ == "__main__":
    sys.exit(main())
