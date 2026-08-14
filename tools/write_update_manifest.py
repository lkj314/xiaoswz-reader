#!/usr/bin/env python3
"""构建后生成局域网更新清单 version.json（与 APK 同目录）

用法：
    python tools/write_update_manifest.py [更新说明...]

示例：
    python tools/write_update_manifest.py "M2 阅读器打磨：分页翻页/沉浸/目录/预读"
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
GRADLE_FILE = ROOT / "app" / "build.gradle.kts"
OUT_DIR = ROOT / "app" / "build" / "outputs" / "apk" / "debug"


def main() -> None:
    notes = " ".join(sys.argv[1:]).strip()

    text = GRADLE_FILE.read_text(encoding="utf-8")
    code_match = re.search(r"versionCode\s*=\s*(\d+)", text)
    name_match = re.search(r'versionName\s*=\s*"([^"]+)"', text)
    if not code_match or not name_match:
        raise SystemExit("未能在 app/build.gradle.kts 中找到 versionCode/versionName")

    code = int(code_match.group(1))
    name = name_match.group(1)

    manifest = {
        "versionCode": code,
        "versionName": name,
        "apkUrl": "app-debug.apk",
        "notes": notes or f"v{name}",
    }

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    target = OUT_DIR / "version.json"
    target.write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"已生成 {target}")
    print(f"  versionCode={code}  versionName={name}  notes={manifest['notes']}")


if __name__ == "__main__":
    main()
