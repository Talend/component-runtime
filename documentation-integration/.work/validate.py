#!/usr/bin/env python3
"""Consistency validation for documentation-integration/. Exit code 1 when a hard check fails."""
import os, re, json, glob, sys
HERE = os.path.dirname(os.path.abspath(__file__)); DOC = os.path.abspath(os.path.join(HERE, '..'))
CAT = os.path.join(DOC, '02-feature-catalog')
PFX = ['DSG','CFG','UI','VAL','ACT','DAT','RUN','LCM','INT','SVC','HTTP','SRV','TST']
LATER = {os.path.join(DOC, n) for n in ('07-designer-blueprint.md', '08-runtime-blueprint.md', '09-integration-checklist.md', 'README.md')}
errors = 0
def err(msg):
    global errors; errors += 1; print('FAIL', msg)

def slug(h):
    h = h.strip().lower().replace('`', '')
    h = re.sub(r'[^\w\- ]', '', h)
    return h.replace(' ', '-')

mdfiles = sorted(f for f in glob.glob(DOC + '/**/*.md', recursive=True) if '\\.work\\' not in f and '/.work/' not in f)
allmd = mdfiles + [f for f in glob.glob(DOC + '/.work/*.md')]

# 1. json parse
for f in glob.glob(DOC + '/**/*.json', recursive=True):
    try: json.load(open(f, encoding='utf-8'))
    except Exception as e: err(f'JSON parse {f}: {e}')
print('json files parsed')

# 2. entries: md vs json
heads = {}      # id -> (kind, file)
for p in PFX:
    f = glob.glob(os.path.join(CAT, p + '-*.md'))[0]
    prim, stub = [], []
    for m in re.finditer(r'(?m)^### (' + p + r'-\d{3})(.*)$', open(f, encoding='utf-8').read()):
        (stub if '(moved) -> see' in m.group(2) else prim).append(m.group(1))
        heads[m.group(1)] = ('stub' if '(moved) -> see' in m.group(2) else 'primary', f)
    frag = [e['id'] for e in json.load(open(os.path.join(CAT, f'index.{p}.json'), encoding='utf-8'))]
    if sorted(prim) != sorted(frag): err(f'{p}: md primary entries != json fragment ({len(prim)} vs {len(frag)}) diff {set(prim) ^ set(frag)}')
    if len(set(prim)) != len(prim): err(f'{p}: duplicate heading')
    for s in stub:
        tgt = re.search(r'### ' + s + r' \(moved\) -> see (\S+)', open(f, encoding='utf-8').read()).group(1)
        if tgt not in heads and True: pass
    print(f'{p}: md primary={len(prim)} stubs={len(stub)} json={len(frag)}')
full = json.load(open(os.path.join(CAT, 'index.json'), encoding='utf-8'))
ids = [e['id'] for e in full]
if len(ids) != len(set(ids)): err('index.json duplicate ids')
frag_all = [e['id'] for p in PFX for e in json.load(open(os.path.join(CAT, f'index.{p}.json'), encoding='utf-8'))]
if sorted(ids) != sorted(frag_all): err('index.json != concatenation of fragments')
order = {p: i for i, p in enumerate(PFX)}
if ids != sorted(ids, key=lambda i: (order[i.split('-')[0]], int(i.split('-')[1]))): err('index.json not sorted')
for e in full:
    for k in ('id', 'name', 'category', 'level', 'designer', 'runtime', 'source', 'tag'):
        if k not in e: err(f'{e["id"]} missing {k}')
    if e['level'] not in (0, 1, 2): err(f'{e["id"]} bad level')
    if e['tag'] not in ('Designer', 'Runtime', 'Both'): err(f'{e["id"]} bad tag')
# stubs must not be in json; stub target must be primary
for i, (kind, f) in heads.items():
    if kind == 'stub':
        if i in ids: err(f'stub {i} present in index')
        t = re.search(r'### ' + i + r' \(moved\) -> see (\S+)', open(f, encoding='utf-8').read()).group(1)
        if t not in ids: err(f'stub {i} points to non-primary {t}')

# 3. referenced IDs exist
idre = re.compile(r'(?<![\w-])((?:' + '|'.join(PFX) + r')-\d{3})(?![\w-])')
unknown = {}
for f in allmd + glob.glob(CAT + '/*.json'):
    for n, ln in enumerate(open(f, encoding='utf-8').read().split('\n'), 1):
        for m in idre.finditer(ln):
            if m.group(1) not in heads: unknown.setdefault(m.group(1), []).append(f'{os.path.relpath(f, DOC)}:{n}')
for i, w in unknown.items(): err(f'unknown id {i}: {w[:3]}')
# references to stubs outside stub lines / merge notes (should have been rewritten)
stubref = {}
for f in allmd + glob.glob(CAT + '/*.json'):
    for n, ln in enumerate(open(f, encoding='utf-8').read().split('\n'), 1):
        if '(moved) -> see' in ln or re.search(r'(?i)merged from', ln) or f.endswith('inventory.md') or f.endswith('README.md') and CAT in f: continue
        for m in idre.finditer(ln):
            if heads.get(m.group(1), ('',))[0] == 'stub': stubref.setdefault(m.group(1), []).append(f'{os.path.relpath(f, DOC)}:{n}')
for i, w in stubref.items(): err(f'reference to stub {i}: {w[:3]}')

# 4. links
anchors = {}
def anchors_of(f):
    if f in anchors: return anchors[f]
    seen, out = {}, set()
    fence = False
    for ln in open(f, encoding='utf-8').read().split('\n'):
        if ln.startswith('```'): fence = not fence; continue
        if fence: continue
        m = re.match(r'#{1,6} (.*)', ln)
        if m:
            s = slug(m.group(1)); n = seen.get(s, 0); seen[s] = n + 1
            out.add(s if n == 0 else f'{s}-{n}')
    anchors[f] = out; return out
dangling_later, broken = {}, []
linkre = re.compile(r'(?<!\!)\[[^\]]*\]\(([^)\s]+)\)')
for f in allmd:
    fence = False
    for n, ln in enumerate(open(f, encoding='utf-8').read().split('\n'), 1):
        if ln.startswith('```'): fence = not fence; continue
        if fence: continue
        ln2 = re.sub(r'`[^`]*`', '', ln)
        for m in linkre.finditer(ln2):
            tgt = m.group(1)
            if re.match(r'(https?:|mailto:|#$)', tgt): continue
            path, _, anc = tgt.partition('#')
            full = os.path.normpath(os.path.join(os.path.dirname(f), path)) if path else f
            rel = os.path.relpath(f, DOC) + f':{n}'
            if full in LATER and not os.path.exists(full):
                dangling_later.setdefault(os.path.relpath(full, DOC), []).append(rel); continue
            if not os.path.exists(full): broken.append(f'{rel} -> {tgt} (missing file)'); continue
            if anc and full.endswith('.md') and anc.lower() not in anchors_of(full): broken.append(f'{rel} -> {tgt} (missing anchor)')
for b in broken: err('link ' + b)
print('links checked; pending links to files written later:')
for k, v in dangling_later.items(): print(f'  {k}: {len(v)} link(s), e.g. {v[:2]}')

# 5. etag
for f in allmd:
    for n, ln in enumerate(open(f, encoding='utf-8').read().split('\n'), 1):
        if re.search(r'(?i)etag|if-none-match', ln):
            print(f'  etag mention {os.path.relpath(f, DOC)}:{n}: {ln.strip()[:140]}')

# 6. json fences
bad = 0
for f in allmd:
    t = open(f, encoding='utf-8').read()
    for m in re.finditer(r'```json\n(.*?)```', t, flags=re.S):
        try: json.loads(m.group(1))
        except Exception as e:
            bad += 1; print(f'  json fence not strict JSON in {os.path.relpath(f, DOC)} near line {t[:m.start()].count(chr(10)) + 2}: {str(e)[:60]}')
print('json fences failing strict parse:', bad)
print('ERRORS:', errors)
sys.exit(1 if errors else 0)
