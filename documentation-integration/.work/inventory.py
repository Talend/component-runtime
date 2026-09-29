#!/usr/bin/env python3
"""Coverage inventory: public types of component-api / component-spi, REST operations of
component-server-api, payload types of component-server-model -> catalog IDs.
Writes inventory.md (same folder). Re-run after any catalog change."""
import os, re, json, glob, sys
HERE = os.path.dirname(os.path.abspath(__file__))
DOC = os.path.abspath(os.path.join(HERE, '..'))
REPO = os.path.abspath(os.path.join(DOC, '..'))
CAT = os.path.join(DOC, '02-feature-catalog')
PFX = ['DSG','CFG','UI','VAL','ACT','DAT','RUN','LCM','INT','SVC','HTTP','SRV','TST']

# ---- intentionally excluded types (type -> reason) ----
EXCLUDED = json.load(open(os.path.join(HERE, 'inventory-exclusions.json'), encoding='utf-8')) \
    if os.path.exists(os.path.join(HERE, 'inventory-exclusions.json')) else {}

def strip_java(src, keep_strings=False):
    src = re.sub(r'"""(.*?)"""', lambda m: '""' + '\n' * m.group(1).count('\n'), src, flags=re.S)
    src = re.sub(r"'(\\.|[^'\\])'", "''", src)
    src = re.sub(r'/\*.*?\*/', lambda m: '\n' * m.group(0).count('\n'), src, flags=re.S)
    src = re.sub(r'//[^\n]*', '', src)
    if not keep_strings:
        src = re.sub(r'"(\\.|[^"\\\n])*"', '""', src)
    return src

DECL = re.compile(r'(?P<mods>(?:(?:public|protected|private|static|final|abstract|sealed|strictfp)\s+)*)(?P<kind>@interface|interface|enum|class|record)\s+(?P<name>\w+)')

def fix_stack_parse(path):
    # Simpler and more robust: handle nesting by tracking brace depth explicitly
    raw = open(path, encoding='utf-8').read()
    src = strip_java(raw)
    pkg = re.search(r'package\s+([\w.]+);', src)
    pkg = pkg.group(1) if pkg else ''
    res = []
    stack = []  # entries: dict(name, kind, depth, public)
    depth = 0
    pending = None
    pat = re.compile(r'\{|\}|(?<![\w.])((?:(?:public|protected|private|static|final|abstract|sealed|strictfp)\s+)*)(@interface|interface|enum|class|record)\s+(\w+)')
    for m in pat.finditer(src):
        t = m.group(0)
        if t == '{':
            depth += 1
            if pending is not None:
                pending['depth'] = depth; stack.append(pending); pending = None
            continue
        if t == '}':
            if stack and stack[-1]['depth'] == depth: stack.pop()
            depth -= 1
            continue
        mods, kind, name = m.group(1) or '', m.group(2), m.group(3)
        # a type declaration must be followed (before ';') by '{'
        tail = src[m.end():m.end()+600]
        if not re.match(r'[^;{}]*\{', tail): continue
        parent = stack[-1] if stack else None
        if parent is None:
            public = 'public' in mods
        else:
            public = parent['public'] and (('public' in mods) or parent['kind'] in ('interface', '@interface'))
            if 'private' in mods or 'protected' in mods: public = False
        qn = '.'.join([s['name'] for s in stack] + [name])
        res.append((qn, kind, public, path, pkg))
        pending = dict(name=name, kind=kind, public=public)
    return res

def collect(root):
    out = []
    for f in sorted(glob.glob(root + '/**/*.java', recursive=True)):
        if os.path.basename(f) in ('package-info.java', 'module-info.java'): continue
        out.extend(fix_stack_parse(f))
    return out

# ---- catalog text ----
def catalog():
    entries = []
    for p in PFX:
        md = open(glob.glob(os.path.join(CAT, p + '-*.md'))[0], encoding='utf-8').read()
        parts = re.split(r'(?m)^(?=### )', md)
        for x in parts[1:]:
            h = re.match(r'### (\w+-\d+)(.*)', x)
            if '(moved) -> see' in x.split('\n')[0]: continue
            src = re.search(r'\*\*Source\*\*:(.*)', x)
            entries.append(dict(id=h.group(1), heading=h.group(2), source=(src.group(1) if src else ''), body=x))
    return entries

def word(name):
    return re.compile(r'(?<![\w])' + re.escape(name) + r'(?![\w])')

def main():
    cat = catalog()
    api_root = os.path.join(REPO, 'component-api', 'src', 'main', 'java')
    spi_root = os.path.join(REPO, 'component-spi', 'src', 'main', 'java')
    doc03 = open(os.path.join(DOC, '03-component-server-api.md'), encoding='utf-8').read()
    doc04 = open(os.path.join(DOC, '04-data-model.md'), encoding='utf-8').read()
    lines = []
    gaps = []
    stats = dict(total=0, covered=0, excluded=0, nonpublic=0)

    def bt(name):
        return re.compile(r'`[^`]*(?<![\w.])' + re.escape(name) + r'(?![\w])[^`]*`')

    def cover(qn, path):
        simple = qn.split('.')[-1]
        base = os.path.splitext(os.path.basename(path))[0]
        nested = '.' in qn
        strong, weak = [], []
        for e in cat:
            file_match = ('/' + base + '.java') in e['source'] or (base + '.java') in e['source']
            if nested:
                if word(qn).search(e['heading']) or (file_match and word(simple).search(e['heading'])):
                    strong.append(e['id'])
                elif word(qn).search(e['body']) or (file_match and bt(simple).search(e['body'])):
                    weak.append(e['id'])
            else:
                if word(simple).search(e['heading']) or file_match:
                    strong.append(e['id'])
                elif bt(simple).search(e['body']):
                    weak.append(e['id'])
        return strong, weak

    def section(title, types, root):
        lines.append(f'\n## {title}\n')
        lines.append('| Type | Kind | Source (relative) | Covered by | Match | Note |')
        lines.append('|---|---|---|---|---|---|')
        for qn, kind, public, path, pkg in types:
            rel = os.path.relpath(path, root).replace('\\', '/')
            full = pkg + '.' + qn
            stats['total'] += 1
            if not public:
                stats['nonpublic'] += 1
                lines.append(f'| `{qn}` | {kind} | `{rel}` | - | excluded | not public (package-private/private/protected): internal, not part of the API |')
                continue
            if full in EXCLUDED or qn in EXCLUDED:
                stats['excluded'] += 1
                lines.append(f'| `{qn}` | {kind} | `{rel}` | - | excluded | {EXCLUDED.get(full, EXCLUDED.get(qn))} |')
                continue
            strong, weak = cover(qn, path)
            if strong:
                stats['covered'] += 1
                lines.append(f'| `{qn}` | {kind} | `{rel}` | {", ".join(strong)} | primary | |')
            elif weak:
                stats['covered'] += 1
                lines.append(f'| `{qn}` | {kind} | `{rel}` | {", ".join(weak)} | mention | documented inside the listed entry |')
            else:
                gaps.append(full)
                lines.append(f'| `{qn}` | {kind} | `{rel}` | **GAP** | none | |')

    api = collect(os.path.join(api_root))
    spi = collect(os.path.join(spi_root))
    section('component-api (`org.talend.sdk.component.api`)', api, api_root)
    section('component-spi (`org.talend.sdk.component.spi`)', spi, spi_root)

    # REST operations
    lines.append('\n## component-server-api: REST operations\n')
    lines.append('| Operation | Java method | Covered by (catalog) | Documented in 03 |')
    lines.append('|---|---|---|---|')
    srv = [e for e in cat if e['id'].startswith('SRV-')]
    ops = []
    for f in sorted(glob.glob(REPO + '/component-server-parent/component-server-api/src/main/java/**/*.java', recursive=True)):
        src = strip_java(open(f, encoding='utf-8').read(), keep_strings=True)
        cm = re.search(r'@Path\("([^"]*)"\)[^{]*?public interface (\w+)[^{]*\{', src, flags=re.S)
        if not cm: continue
        base, iface = cm.group(1), cm.group(2)
        body, depth, cur = src[cm.end():], 0, ''
        stmts = []
        for ch in body:
            if ch == '(': depth += 1
            elif ch == ')': depth -= 1
            if ch == ';' and depth == 0:
                stmts.append(cur); cur = ''
            else:
                cur += ch
        for st in stmts:
            verb = re.search(r'@(GET|POST|PUT|DELETE|PATCH|HEAD)\b', st)
            if not verb: continue
            p = re.search(r'@Path\("([^"]*)"\)', st)
            bare = re.sub(r'@\w+(\((?:[^()]|\((?:[^()]|\([^()]*\))*\))*\))?', ' ', st)
            mm = re.search(r'(\w+)\s*\(', bare)
            path = '/api/v1/' + base.strip('/') + ('/' + p.group(1).strip('/') if p else '')
            ops.append((verb.group(1), path, iface + '#' + (mm.group(1) if mm else '?')))
    for verb, path, jm in ops:
        stats['total'] += 1
        short = path[len('/api/v1'):]
        ids = [e['id'] for e in srv if (verb + ' ' + path) in e['heading'] or (verb + ' ' + short) in e['body'] or (short in e['heading'] and verb in e['heading'])]
        if not ids:
            ids = [e['id'] for e in srv if short.split('{')[0].rstrip('/') in e['heading'] or short in e['source']]
        ok03 = 'yes' if short.replace('{id}', '{id}') in doc03 or short.split('{')[0] in doc03 else 'NO'
        if ids: stats['covered'] += 1
        else: gaps.append(f'{verb} {path}')
        lines.append(f'| `{verb} {path}` | `{jm}` | {", ".join(ids) if ids else "**GAP**"} | {ok03} |')

    # model payload types
    lines.append('\n## component-server-model: payload types\n')
    lines.append('| Type | Catalog entries mentioning it | Documented in 03/04 |')
    lines.append('|---|---|---|')
    for f in sorted(glob.glob(REPO + '/component-server-parent/component-server-model/src/main/java/**/*.java', recursive=True)):
        for qn, kind, public, path, pkg in fix_stack_parse(f):
            if not public: continue
            simple = qn.split('.')[-1]
            ids = [e['id'] for e in cat if word(simple).search(e['heading'] + e['source'] + e['body'])]
            d = 'yes' if word(simple).search(doc03) or word(simple).search(doc04) else 'NO'
            stats['total'] += 1
            if ids or d == 'yes': stats['covered'] += 1
            else: gaps.append('model ' + qn)
            lines.append(f'| `{qn}` | {", ".join(ids[:8]) if ids else "-"}{" ..." if len(ids) > 8 else ""} | {d} |')

    head = ['# Coverage inventory (generated)', '',
            'Generated by `.work/inventory.py` from the sources under `component-api`, `component-spi`, `component-server-parent/component-server-api` and `component-server-model`, compared with the fragments in `02-feature-catalog/`.', '',
            'Match kinds: `primary` = an entry heading or its `Source` names the type or its file; `mention` = the type is documented inside another entry (nested types, companions); `excluded` = intentionally not catalogued (reason given).', '',
            f'Totals: {stats["total"]} items; covered {stats["covered"]}; intentionally excluded (non-public) {stats["nonpublic"]}; intentionally excluded (public, listed with reason) {stats["excluded"]}; **unexplained gaps: {len(gaps)}**.', '']
    if gaps: head += ['Gaps:', ''] + [f'- {g}' for g in gaps] + ['']
    open(os.path.join(HERE, 'inventory.md'), 'w', encoding='utf-8', newline='\n').write('\n'.join(head + lines) + '\n')
    print(stats, 'gaps:', len(gaps))
    for g in gaps: print('  GAP', g)

if __name__ == '__main__':
    main()
