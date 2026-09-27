#!/usr/bin/env python3
"""Packs a directory (manifest.json + scripts) into a .jdr package.

Usage:
    python scripts/pack_jdr.py samples/itingshu itingshu.jdr
"""
import sys
import zipfile
from pathlib import Path


def main() -> None:
    if len(sys.argv) != 3:
        sys.exit("用法: python pack_jdr.py <目录> <输出.jdr>")
    src, out = Path(sys.argv[1]), Path(sys.argv[2])
    if not src.is_dir():
        sys.exit(f"不是目录: {src}")
    files = sorted(p for p in src.rglob("*") if p.is_file())
    if not files:
        sys.exit(f"目录为空: {src}")
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        for path in files:
            name = path.relative_to(src).as_posix()
            if name.startswith("/") or ".." in name.split("/"):
                sys.exit(f"非法文件名: {name}")
            z.write(path, name)
            print(f"  + {name} ({path.stat().st_size} bytes)")
    print(f"已生成 {out.resolve()} ({out.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
