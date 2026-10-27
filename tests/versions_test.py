#!/usr/bin/env python3
"""The screensaver app's version comparison (android/app .../Versions.java) on a plain JVM: major first, then minor, as numbers.
Needs javac; without it, skipped with a note.   Run: python3 tests/versions_test.py"""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if not shutil.which("javac"):
    print("SKIP versions: no javac here")
    sys.exit(0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / "android/app/src/main/java/sa/mytv/screensaver/Versions.java"),
                    str(ROOT / "tests/VersionsRun.java")], check=True)
    run = subprocess.run(["java", "-cp", tmp, "sa.mytv.screensaver.VersionsRun"], capture_output=True)
out = run.stdout.decode("utf-8")

# the apps read the newest version from /releases/latest: that must be the numbered release v3.N, not apk-latest (no number)
wf = (ROOT / ".github/workflows/android.yml").read_text(encoding="utf-8")
latest_tag = wf.split("make_latest: true")[0].rsplit("tag_name:", 1)[-1].split("\n")[0].strip()
out += ("PASS" if latest_tag.startswith("v3.") else "FAIL") + f" the release marked latest is the numbered one ({latest_tag})\n"
# «تحديث التطبيق» downloads from that same release, and install.sh lets the app install (REQUEST_INSTALL_PACKAGES)
upd = (ROOT / "android/app/src/main/java/sa/mytv/screensaver/AppUpdate.java").read_text(encoding="utf-8")
inst = (ROOT / "optimize/install.sh").read_text(encoding="utf-8")
ok = "releases/download/v" in upd and "appops set sa.mytv.screensaver REQUEST_INSTALL_PACKAGES allow" in inst
out += ("PASS" if ok else "FAIL") + " «تحديث التطبيق»: the numbered release, and install.sh grants the install permission\n"
print(out, end="")
sys.exit(1 if run.returncode != 0 or "FAIL" in out else 0)
