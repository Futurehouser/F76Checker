import json, re, sys, os, urllib.request, collections
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from es_names import ES

D, OUT = sys.argv[1], sys.argv[2]
items = json.load(open(os.path.join(D, 'items1.json'), encoding='utf-8'))
miss = [x['en'] for x in items if x['en'] not in ES]
assert not miss, miss
os.makedirs(OUT, exist_ok=True)
UA = {'User-Agent': 'Mozilla/5.0 (compatible; F76Checker/1.0)'}

def slug(s):
    return re.sub(r'[^a-z0-9]+', '_', s.lower()).strip('_')

SUB = {'Pistols/Rifles': 'pistols_rifles', 'Pistols': 'pistols', 'Rifles': 'rifles', 'Shotguns': 'shotguns',
       'Bows': 'bows', 'Heavy guns': 'heavy', 'Explosive heavy guns': 'explosive', 'One-handed': 'one_handed',
       'Two-handed': 'two_handed', 'Unarmed': 'unarmed', 'Grenades': 'grenades', 'Mines': 'mines',
       'Throwing knives': 'throwing', 'Other': 'other'}
out = []
ids = collections.Counter()
for x in items:
    if x['sub'] == 'Unique':
        sub = 'unique_ranged' if x['group'] == 'Ranged' else 'unique_melee'
    else:
        sub = SUB[x['sub']]
    i = slug(x['en'])
    ids[i] += 1
    if ids[i] > 1:
        i = f"{i}_{sub}"
    es, src = ES[x['en']]
    out.append(dict(id=i, section=sub, en=x['en'], es=es, esSource=src, image=x['img'],
                    wiki='https://fallout.fandom.com/wiki/' + x['link'].replace(' ', '_')))
json.dump({'category': 'weapons', 'items': out},
          open(os.path.join(OUT, 'weapons.json'), 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
print(len(out), collections.Counter(o['esSource'] for o in out))
