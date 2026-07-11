#!/usr/bin/env python3
"""
DDInter 2.0 数据下载与处理脚本
来源: https://ddinter2.scbdd.com (中南大学, CC BY-NC-SA 4.0)
输出: ddinter_interactions.json → app/src/main/assets/
用法: python tools/download_ddinter.py
"""

import csv
import json
import ssl
import sys
from pathlib import Path
from urllib.request import urlretrieve, urlopen

# DDInter 服务器证书过期，跳过验证
ssl._create_default_https_context = ssl._create_unverified_context

ATC_CODES = ['A', 'B', 'D', 'H', 'L', 'P', 'R', 'V']
BASE_URL = "https://ddinter2.scbdd.com/static/media/download/ddinter_downloads_code_{}.csv"
BASE_URL_HTTP = "http://ddinter2.scbdd.com/static/media/download/ddinter_downloads_code_{}.csv"
OUTPUT_FILE = Path("app/src/main/assets/ddinter_interactions.json")

# DDInter CSV 列名 (按实际 CSV 头)
# Drug1, Drug2, Level, Severity, Mechanism, Management_strategy, Drug1_ATC, Drug2_ATC ...

def download_all():
    OUTPUT_FILE.parent.mkdir(parents=True, exist_ok=True)
    interactions = []
    drug_index = {}  # drug name → [interaction id list]
    skipped = 0

    for code in ATC_CODES:
        tmp = Path(f"/tmp/ddinter_{code}.csv")
        ok = False
        for base in (BASE_URL, BASE_URL_HTTP):
            url = base.format(code)
            print(f"下载: {code} ({url[:5]}) ... ", end="", flush=True)
            try:
                urlretrieve(url, tmp)
                ok = True
                break
            except Exception as e:
                print(f"失败: {type(e).__name__}")
        if not ok:
            print(f"  -> 跳过 {code}，请手动下载后放到 {tmp}")
            continue
        print(f"OK ({tmp.stat().st_size // 1024}KB)")

        with open(tmp, 'r', encoding='utf-8') as f:
            reader = csv.DictReader(f)
            for row in reader:
                try:
                    a = row.get('Drug_A', '').strip()
                    b = row.get('Drug_B', '').strip()
                    if not a or not b:
                        skipped += 1
                        continue

                    level_str = row.get('Level', '').strip()
                    desc = row.get('Description', row.get('Mechanism', '')).strip()
                    mng = row.get('Management_strategy', row.get('Management', '')).strip()
                    level_map = {'Major': 1, 'Moderate': 2, 'Minor': 3,
                                 'Contraindicated': 1, 'Cautious': 2, 'Attention': 3,
                                 '禁忌': 1, '谨慎': 2, '关注': 3, '不推荐': 1,
                                 '1': 1, '2': 2, '3': 3, 'major': 1, 'moderate': 2, 'minor': 3}
                    level = 2
                    for k, v in level_map.items():
                        if k in str(level_str):
                            level = v
                            break

                    idx = len(interactions)
                    interactions.append({
                        "a": a,
                        "b": b,
                        "lvl": level,
                        "desc": desc or mng,
                        "mng": ""
                    })
                    drug_index.setdefault(a.lower(), []).append(idx)
                    drug_index.setdefault(b.lower(), []).append(idx)
                except Exception as e:
                    skipped += 1

        tmp.unlink()  # clean up

    # 去重 + 压缩
    seen = {}
    deduped = []
    for item in interactions:
        key = (item['a'].lower(), item['b'].lower())
        if key in seen or (key[1], key[0]) in seen:
            skipped += 1
            continue
        seen[key] = True
        deduped.append(item)

    # 去重后重建索引（关键！去重会改变数组位置）
    drug_index = {}
    for idx, item in enumerate(deduped):
        drug_index.setdefault(item['a'].lower(), []).append(idx)
        drug_index.setdefault(item['b'].lower(), []).append(idx)

    output = {
        "_meta": {
            "source": "DDInter 2.0 (中南大学, CC BY-NC-SA 4.0)",
            "url": "https://ddinter2.scbdd.com",
            "count": len(deduped),
            "date": "2026-07"
        },
        "ix": drug_index,
        "ddi": deduped
    }

    with open(OUTPUT_FILE, 'w', encoding='utf-8') as f:
        json.dump(output, f, ensure_ascii=False, separators=(',', ':'))

    size = OUTPUT_FILE.stat().st_size
    print(f"\n完成: {len(deduped)} 对相互作用, 导出 {size // 1024}KB 到 {OUTPUT_FILE}")
    print(f"跳过: {skipped} 条")

if __name__ == '__main__':
    download_all()
