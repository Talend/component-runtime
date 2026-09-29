#!/usr/bin/env python3
"""Builds 02-feature-catalog/index.json from the per-category fragments index.<PREFIX>.json.
Usage: python build_index.py   (run from anywhere)"""
import json, os, re
HERE = os.path.dirname(os.path.abspath(__file__))
CAT = os.path.join(HERE, '..', '02-feature-catalog')
PFX = ['DSG','CFG','UI','VAL','ACT','DAT','RUN','LCM','INT','SVC','HTTP','SRV','TST']
DEFAULT_TAG = {'DSG':'Designer','CFG':'Designer','UI':'Designer','VAL':'Designer','ACT':'Designer',
               'RUN':'Runtime','LCM':'Runtime','INT':'Runtime','SVC':'Runtime','HTTP':'Runtime',
               'DAT':'Both','SRV':'Both','TST':'Both'}
FIELDS = ['id','name','category','level','designer','runtime','source']
def is_none(s):
    s = s.strip()
    return s.lower().rstrip('.') == 'none' or bool(re.match(r'(?i)^none\b[^.;]{0,120}$', s))
def tag(e):
    d, r = not is_none(e['designer']), not is_none(e['runtime'])
    if d and r: return 'Both'
    if d: return 'Designer'
    if r: return 'Runtime'
    return DEFAULT_TAG[e['category']]
out, seen = [], set()
for p in PFX:
    frag = json.load(open(os.path.join(CAT, f'index.{p}.json'), encoding='utf-8'))
    frag.sort(key=lambda e: int(e['id'].split('-')[1]))
    for e in frag:
        assert e['id'] not in seen, e['id']; seen.add(e['id'])
        assert e['id'].startswith(p + '-') and e['category'] == p, e['id']
        assert e['level'] in (0, 1, 2), e['id']
        o = {k: e[k] for k in FIELDS}; o['tag'] = tag(e); out.append(o)
with open(os.path.join(CAT, 'index.json'), 'w', encoding='utf-8', newline='\n') as f:
    json.dump(out, f, indent=2, ensure_ascii=False); f.write('\n')
print(len(out), 'entries')
