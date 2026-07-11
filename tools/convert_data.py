#!/usr/bin/env python3
"""Convert DrugReference.kt, TemplateManager.kt, DeptTemplates.kt to JSON."""
import re, json, os

base = r"F:\NOTE\mynoteANDROID\MyNote-Android"
srcdir = os.path.join(base, "app", "src", "main", "java", "com", "mynote", "android", "util")
outdir = os.path.join(base, "app", "src", "main", "assets")
os.makedirs(outdir, exist_ok=True)

# ── 1. DrugReference.kt → drugs.json ──
with open(os.path.join(srcdir, "DrugReference.kt"), "r", encoding="utf-8") as f:
    content = f.read()

# Drug("name", "category", "dosage", "indications", "contraindications", "caution", group="")
# Some use named param: group = "..."
pattern = r'Drug\("([^"]*?)",\s*"([^"]*?)",\s*\n?\s*"([^"]*?)",\s*\n?\s*"([^"]*?)",\s*\n?\s*"([^"]*?)",\s*\n?\s*"([^"]*?)"(?:,\s*group\s*=\s*"([^"]*)")?\)'
matches = re.findall(pattern, content, re.DOTALL)
drugs = []
for m in matches:
    drugs.append({
        "name": m[0], "category": m[1], "dosage": m[2],
        "indications": m[3], "contraindications": m[4],
        "caution": m[5], "group": m[6] if m[6] else m[1]
    })
with open(os.path.join(outdir, "drugs.json"), "w", encoding="utf-8") as f:
    json.dump(drugs, f, ensure_ascii=False, indent=2)
print(f"DrugReference: {len(drugs)} drugs → drugs.json ({os.path.getsize(os.path.join(outdir, 'drugs.json'))} bytes)")

# ── 2. TemplateManager.kt → templates.json ──
with open(os.path.join(srcdir, "TemplateManager.kt"), "r", encoding="utf-8") as f:
    content = f.read()

# Template("title", """...""")
pattern2 = r'Template\("([^"]*)",\s*"""([\s\S]*?)"""\)'
matches2 = re.findall(pattern2, content)
templates = []
for m in matches2:
    templates.append({"title": m[0], "content": m[1].strip()})
with open(os.path.join(outdir, "templates.json"), "w", encoding="utf-8") as f:
    json.dump(templates, f, ensure_ascii=False, indent=2)
print(f"TemplateManager: {len(templates)} templates → templates.json")

# ── 3. DeptTemplates.kt → dept_templates.json ──
with open(os.path.join(srcdir, "DeptTemplates.kt"), "r", encoding="utf-8") as f:
    content = f.read()

# Check for Template("title", """...""") or other format
matches3 = re.findall(pattern2, content)
if matches3:
    dtemplates = [{"title": m[0], "content": m[1].strip()} for m in matches3]
    with open(os.path.join(outdir, "dept_templates.json"), "w", encoding="utf-8") as f:
        json.dump(dtemplates, f, ensure_ascii=False, indent=2)
    print(f"DeptTemplates: {len(dtemplates)} dept templates → dept_templates.json")
else:
    print("DeptTemplates: different format, checking...")
    for line in content.split('\n')[:50]:
        if 'Template(' in line or 'DeptTemplate(' in line or 'class' in line:
            print(f"  {line[:100]}")
