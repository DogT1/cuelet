"""Tiny UI driver for on-device checks over adb.

    python tools/ui.py tap "New Automation"      tap the first node whose text or description matches
    python tools/ui.py type "Hello"              type into the focused field
    python tools/ui.py dump                      list visible texts with their centres
Uses ADB_SERIAL and ADB (path to adb.exe) from the environment.
"""
import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

ADB = os.environ.get("ADB", "adb")
SERIAL = os.environ.get("ADB_SERIAL")


def adb(*args, text=True):
    cmd = [ADB] + (["-s", SERIAL] if SERIAL else []) + list(args)
    return subprocess.run(cmd, capture_output=True, text=text, encoding="utf-8" if text else None,
                          errors="replace" if text else None, check=True).stdout


def nodes():
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    root = ET.fromstring(adb("shell", "cat", "/sdcard/ui.xml"))
    for n in root.iter("node"):
        label = n.get("text") or n.get("content-desc") or ""
        m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", n.get("bounds", ""))
        if label and m:
            x1, y1, x2, y2 = map(int, m.groups())
            yield label, (x1 + x2) // 2, (y1 + y2) // 2


def tap(label, index=0):
    hits = [(x, y) for text, x, y in nodes() if text == label] or \
           [(x, y) for text, x, y in nodes() if label.lower() in text.lower()]
    if len(hits) <= index:
        sys.exit(f"not found: {label}")
    x, y = hits[index]
    adb("shell", "input", "tap", str(x), str(y))
    print(f"tapped {label} at {x},{y}")


if __name__ == "__main__":
    cmd = sys.argv[1]
    if cmd == "tap":
        tap(sys.argv[2], int(sys.argv[3]) if len(sys.argv) > 3 else 0)
    elif cmd == "type":
        adb("shell", "input", "text", sys.argv[2].replace(" ", "%s"))
    elif cmd == "dump":
        sys.stdout.reconfigure(encoding="utf-8")
        for text, x, y in nodes():
            print(f"{x:5} {y:5}  {text}")
