#!/usr/bin/env python3
"""Convert DiseaseReference.kt getAll() data to JSON."""
import re, json, os

src = r"F:\NOTE\mynoteANDROID\MyNote-Android\app\src\main\java\com\mynote\android\util\DiseaseReference.kt"
with open(src, "r", encoding="utf-8") as f:
    content = f.read()

# Disease("name", "dept", "symptoms", "differential", "drugs", "treatment")
# Multiline: use re.DOTALL and non-greedy match
pattern = r'Disease\("([^"]*)", "([^"]*)",\s*"([^"]*)", "([^"]*)",\s*"""([^"]*)""",\s*"""([^"]*)"""\)'
matches = re.findall(pattern, content, re.DOTALL)

print(f"Triple-quote pattern: {len(matches)} matches")

# If triple-quote didn't work, try single-line format
if len(matches) == 0:
    pattern2 = r'Disease\("([^"]*)", "([^"]*)",\s*\n?\s*"([^"]*)", "([^"]*)",\s*\n?\s*"([^"]*)",\s*\n?\s*"([^"]*)"\)'
    matches = re.findall(pattern2, content, re.DOTALL)
    print(f"Single-line pattern: {len(matches)} matches")

diseases = []
for m in matches:
    diseases.append({
        "name": m[0],
        "department": m[1],
        "symptoms": m[2],
        "differential": m[3],
        "drugs": m[4],
        "treatment": m[5]
    })

if len(diseases) < 900:
    print(f"WARNING: only {len(diseases)} found (expected ~960), sampling raw Disease lines...")
    for line in content.split('\n')[:200]:
        if 'Disease(' in line:
            # Show first 100 chars
            idx = line.index('Disease(')
            print(f"  SAMPLE: {line[idx:idx+120]}")
    print("...")

outdir = r"F:\NOTE\mynoteANDROID\MyNote-Android\app\src\main\assets"
os.makedirs(outdir, exist_ok=True)
outfile = os.path.join(outdir, "diseases.json")
with open(outfile, "w", encoding="utf-8") as f:
    json.dump(diseases, f, ensure_ascii=False, indent=2)

print(f"Converted {len(diseases)} diseases → {outfile}")
print(f"File size: {os.path.getsize(outfile)} bytes")
