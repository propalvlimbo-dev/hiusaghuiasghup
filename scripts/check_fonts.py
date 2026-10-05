#!/usr/bin/env python3
"""Harness: баланс скобок во всех наших Java-файлах + валидность всех JSON
(миксины, шейдеры, конфиги). Полную компиляцию делает пользователь у себя."""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
fail = 0

def strip(src: str) -> str:
    # Сначала char-литералы (могут содержать '"'), потом строки.
    src = re.sub(r"'(\\.|[^'\\])'", "''", src)
    src = re.sub(r'"(\\.|[^"\\])*"', '""', src)
    src = re.sub(r"//.*", "", src)
    src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)
    return src

java_files = []
for base in ("src/client/java/ru/rooyzee", "src/client/java/platform",
             "src/client/java/org/xrose"):
    java_files += sorted((ROOT / base).rglob("*.java"))
for f in java_files:
    s = strip(f.read_text(encoding="utf-8"))
    b = s.count("{") - s.count("}")
    p = s.count("(") - s.count(")")
    if b or p:
        print(f"[FAIL] {f.relative_to(ROOT)}: braces {b:+d} parens {p:+d}")
        fail += 1
print(f"java files checked: {len(java_files)}")

json_files = sorted(ROOT.glob("src/client/resources/**/*.json")) + \
             sorted(ROOT.glob("*.mixins.json")) + sorted(ROOT.glob("**/*.mixins.json"))
seen = set()
for f in json_files:
    if f in seen or f.name in ("fabric.mod.json",):
        continue
    seen.add(f)
    try:
        json.loads(f.read_text(encoding="utf-8"))
    except Exception as e:
        print(f"[FAIL] {f.relative_to(ROOT)}: {e}")
        fail += 1
print(f"json files checked: {len(seen)}")

sys.exit(1 if fail else 0)
