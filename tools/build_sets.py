"""Builds power_armor.json and armor.json (6 pieces per set) for the app.

Usage: python tools/build_sets.py app/src/main/assets

English set names and piece images come from the English Fallout Wiki
(Power_armor_(Fallout_76), Fallout_76_armor_and_apparel -> "Armor Sets", and each set's own page).
Spanish names come from the Spanish wiki (Planos de Fallout 76/Armadura and the power armor paint
pages); `es_src` marks the ones the Spanish wiki does not confirm.
"""
import json, os, re, sys, urllib.parse, urllib.request

UA = {'User-Agent': 'Mozilla/5.0 (compatible; F76Checker/1.0)'}
API = 'https://fallout.fandom.com/api.php'

PARTS = ['helmet', 'torso', 'left_arm', 'right_arm', 'left_leg', 'right_leg']

# Spanish piece words
ES_PART = {'helmet': 'Casco', 'mask': 'Máscara', 'torso': 'Torso', 'coraza': 'Coraza',
           'left_arm': 'Brazo izquierdo', 'right_arm': 'Brazo derecho',
           'left_leg': 'Pierna izquierda', 'right_leg': 'Pierna derecha'}

# id, wiki page, EN set name, ES set name, es source, set image,
# EN piece names (6), ES "de ..." suffix, ES head word, ES torso word,
# images: (helmet, torso, left arm, right arm, left leg, right leg)  None -> set image
PA = [
    dict(id='excavator', page='Excavator power armor', en='Excavator power armor', es='Servoarmadura excavadora', src='wiki',
         img='FO76 Excavator power armor.png',
         pieces=['Excavator helmet', 'Excavator torso', 'Excavator left arm', 'Excavator right arm', 'Excavator left leg', 'Excavator right leg'],
         es_of='de excavadora',
         imgs=['FO76 Excavator helmet.png', 'FO76 Excavator torso.png', 'FO76 Excavator left arm.png', 'FO76 Excavator right arm.png',
               'FO76 Excavator left and right leg.png', 'FO76 Excavator left and right leg.png']),
    dict(id='hellcat', page='Hellcat power armor', en='Hellcat power armor', es='Servoarmadura Arpía', src='wiki',
         img='FO76 pa hellcat.png',
         pieces=['Hellcat helm', 'Hellcat torso', 'Hellcat left arm', 'Hellcat right arm', 'Hellcat left leg', 'Hellcat right leg'],
         es_of='de Arpía',
         imgs=['FO76 PA Hellcat Helmet.png', 'FO76 PA Hellcat Torso.png', 'FO76 PA Hellcat Arms.png', 'FO76 PA Hellcat Arms.png',
               'FO76 PA Hellcat Legs.png', 'FO76 PA Hellcat Legs.png']),
    dict(id='raider_pa', page='Raider power armor (Fallout 76)', en='Raider power armor', es='Servoarmadura de saqueador', src='wiki',
         img='FO76 Raider power armor.png',
         pieces=['Raider power helmet', 'Raider power chest piece', 'Raider power left arm', 'Raider power right arm', 'Raider power left leg', 'Raider power right leg'],
         es_of='de servoarmadura de saqueador',
         imgs=['FO76 Raider power armor helmet.png', 'FO76 Raider power armor torso.png', 'FO76 Raider power armor left arm.png',
               'FO76 Raider power armor right arm.png', 'FO76 Raider power armor leg.png', 'FO76 Raider power armor leg.png']),
    dict(id='strangler_heart', page='Strangler heart power armor', en='Strangler heart power armor', es='Servoarmadura de corazón estrangulador', src='translated',
         img='FO76NW Strangler Heart PA.png',
         pieces=['Strangler heart helmet', 'Strangler heart torso', 'Strangler heart left arm', 'Strangler heart right arm', 'Strangler heart left leg', 'Strangler heart right leg'],
         es_of='de corazón estrangulador', imgs=[None] * 6),
    dict(id='t45', page='T-45 power armor (Fallout 76)', en='T-45 power armor', es='Servoarmadura T-45', src='wiki',
         img='FO76 T-45 power armor.png',
         pieces=['T-45 helmet', 'T-45 chest piece', 'T-45 left arm', 'T-45 right arm', 'T-45 left leg', 'T-45 right leg'],
         es_of='de T-45',
         imgs=['FO76 T-45 helmet.png', 'FO76 T-45 torso.png', 'FO76 T-45 arm.png', 'FO76 T-45 arm.png', 'FO76 T-45 leg.png', 'FO76 T-45 leg.png']),
    dict(id='t51b', page='T-51b power armor (Fallout 76)', en='T-51b power armor', es='Servoarmadura T-51b', src='wiki',
         img='FO76 T-51 power armor.png',
         pieces=['T-51b helmet', 'T-51b chest piece', 'T-51b left arm', 'T-51b right arm', 'T-51b left leg', 'T-51b right leg'],
         es_of='de T-51b',
         imgs=['FO76 T-51 helmet.png', 'FO76 T-51 torso.png', 'FO76 T-51 arm.png', 'FO76 T-51 arm.png', 'FO76 T-51 leg.png', 'FO76 T-51 leg.png']),
    dict(id='t60', page='T-60 power armor (Fallout 76)', en='T-60 power armor', es='Servoarmadura T-60', src='wiki',
         img='FO76 T-60 power armor.png',
         pieces=['T-60 helmet', 'T-60 chest piece', 'T-60 left arm', 'T-60 right arm', 'T-60 left leg', 'T-60 right leg'],
         es_of='de T-60',
         imgs=['FO76 T-60 helmet.png', 'FO76 T-60 torso.png', 'FO76 T-60 arm.png', 'FO76 T-60 arm.png', 'FO76 T-60 leg.png', 'FO76 T-60 leg.png']),
    dict(id='t65', page='T-65 power armor', en='T-65 power armor', es='Servoarmadura T-65', src='wiki',
         img='F76WL T65 Power Armor.png',
         pieces=['T-65 helmet', 'T-65 chest piece', 'T-65 left arm', 'T-65 right arm', 'T-65 left leg', 'T-65 right leg'],
         es_of='de T-65', imgs=[None] * 6),
    dict(id='ultracite', page='Ultracite power armor', en='Ultracite power armor', es='Servoarmadura de ultracita', src='wiki',
         img='FO76 Ultracite power armor.png',
         pieces=['Ultracite helm', 'Ultracite torso', 'Ultracite left arm', 'Ultracite right arm', 'Ultracite left leg', 'Ultracite right leg'],
         es_of='de ultracita',
         imgs=['FO76 Ultracite helmet.png', 'FO76 Ultracite torso.png', 'FO76 Ultracite arm.png', 'FO76 Ultracite arm.png',
               'FO76 Ultracite leg.png', 'FO76 Ultracite leg.png']),
    dict(id='union', page='Union power armor', en='Union power armor', es='Servoarmadura del Sindicato', src='wiki',
         img='F076TP Union Power Armor.png',
         pieces=['Union helm', 'Union torso', 'Union left arm', 'Union right arm', 'Union left leg', 'Union right leg'],
         es_of='del Sindicato',
         imgs=['Score s10 powerarmor union helmet l.png', 'Score s10 powerarmor union torso l.png', 'Score s10 powerarmor union armright l.png',
               'Score s10 powerarmor union armright l.png', 'Score s10 powerarmor union legleft l.png', 'Score s10 powerarmor union legleft l.png']),
    dict(id='vulcan', page='Vulcan power armor', en='Vulcan power armor', es='Servoarmadura Vulcano', src='translated',
         img='Fo76 vulcan power armor.png',
         pieces=['Vulcan helmet', 'Vulcan torso', 'Vulcan left arm', 'Vulcan right arm', 'Vulcan left leg', 'Vulcan right leg'],
         es_of='de Vulcano', imgs=[None, 'Atx vulcan pa torso d.png', None, None, None, None]),
    dict(id='x01', page='X-01 power armor (Fallout 76)', en='X-01 power armor', es='Servoarmadura X-01', src='wiki',
         img='FO76 X-01 power armor.png',
         pieces=['X-01 helmet', 'X-01 torso', 'X-01 left arm', 'X-01 right arm', 'X-01 left leg', 'X-01 right leg'],
         es_of='de X-01',
         imgs=['FO76 X-01 helmet.png', 'FO76 X-01 torso.png', 'FO76 X-01 arm.png', 'FO76 X-01 arm.png', 'FO76 X-01 leg.png', 'FO76 X-01 leg.png']),
]


def armor(id, page, en, es, src, img, prefix, es_of, imgs, head='helmet', torso_es='torso', head_en=None, head_es_of=None, head_src=None):
    head_en = head_en or f'{prefix} {"mask" if head == "mask" else "helmet"}'
    return dict(id=id, page=page, en=en, es=es, src=src, img=img,
                pieces=[head_en, f'{prefix} chest piece', f'{prefix} left arm', f'{prefix} right arm', f'{prefix} left leg', f'{prefix} right leg'],
                es_of=es_of, head=head, torso_es=torso_es, head_es_of=head_es_of, imgs=imgs, head_src=head_src)


def arms_legs(h, t, a, l):
    return [h, t, a, a, l, l]


ARMOR = [
    armor('arctic_marine', 'Arctic marine armor', 'Arctic marine armor', 'Armadura de marine polar', 'wiki', 'FO76FW armor arctic marine set.png',
          'Arctic marine armor', 'de marine polar', head_es_of='de armadura de marine polar',
          imgs=arms_legs('FO76FW armor arctic helmet.png', 'FO76FW armor arctic torso.png', 'FO76FW armor arctic arms.png', 'FO76FW armor arctic legs.png')),
    armor('botsmith', 'Botsmith armor', 'Botsmith armor', 'Armadura de artesana de robots', 'wiki', 'FO76TYM armor botsmith set.png',
          'Botsmith', 'de artesana de robots',
          imgs=arms_legs('FO76TYM armor botsmith sentrybot helmet.png', 'FO76TYM armor botsmith torso.png', 'FO76TYM armor botsmith arms.png', 'FO76TYM armor botsmith legs.png')),
    armor('bos_recon', 'Brotherhood recon armor', 'Brotherhood recon armor', 'Armadura de reconocimiento de la Hermandad', 'wiki', 'FO76SD BoS recon armor set.png',
          'Brotherhood recon', 'de reconocimiento de la Hermandad',
          imgs=arms_legs('FO76SD armor BOSrecon helm.png', None, 'Fo76SD armor BoS Recon arms.png', 'Fo76SD armor BoS Recon legs.png')),
    armor('civil_engineer', 'Civil Engineer armor', 'Civil Engineer armor', 'Armadura de ingeniero civil', 'wiki', 'FO76AC2 Muni Armor.png',
          'Civil Engineer', 'de ingeniero civil', imgs=[None] * 6),
    armor('combat', 'Combat armor (Fallout 76)', 'Combat armor', 'Armadura de combate', 'wiki', 'Fo76 armor heavy combat set.png',
          'Combat armor', 'de combate', head_es_of='de armadura de combate',
          imgs=arms_legs('FO76 armor combat helmet.png', 'FO76 armor combat heavy torso.png', 'FO76 armor combat heavy arms.png', 'FO76 armor combat heavy legs.png')),
    armor('covert_scout', 'Covert scout armor', 'Covert scout armor', 'Armadura de scout sigilosa', 'wiki', 'FO76LL armor covert set.png',
          'Covert scout armor', 'de armadura de scout sigilosa', head='mask', torso_es='coraza',
          imgs=arms_legs('FO76LL armor covert mask.png', 'FO76LL armor covert torso.png', 'FO76LL armor covert arms.png', 'FO76LL armor covert legs.png')),
    armor('forest_scout', 'Forest scout armor', 'Forest scout armor', 'Armadura de scout del bosque', 'wiki', 'FO76 armor forest scout set.png',
          'Forest scout armor', 'de armadura de scout del bosque', head='mask', torso_es='coraza',
          imgs=arms_legs('FO76 armor forest mask.png', 'FO76 armor forest torso.png', 'FO76 armor forest arms.png', 'FO76 armor forest legs.png')),
    armor('leather', 'Leather armor (Fallout 76)', 'Leather armor', 'Armadura de cuero', 'wiki', 'Fo76 armor heavy leather set.png',
          'Leather', 'de cuero', head_src='no-ingame-helmet',
          imgs=arms_legs(None, 'FO76 armor heavy leather torso.png', 'FO76 armor heavy leather arms.png', 'FO76 armor heavy leather legs.png')),
    armor('marine', 'Marine armor (Fallout 76)', 'Marine armor', 'Armadura de marine', 'wiki', 'Fo76 armor marine set.png',
          'Marine armor', 'de marine', head_es_of='de armadura de marine',
          imgs=arms_legs('FO76 armor marine helmet.png', 'FO76 armor marine torso.png', 'FO76 armor marine arms.png', 'FO76 armor marine legs.png')),
    armor('metal', 'Metal armor (Fallout 76)', 'Metal armor', 'Armadura de metal', 'wiki', 'Fo76 armor heavy metal set.png',
          'Metal', 'de metal',
          imgs=['FO76 armor metal helmet.png', 'FO76 armor metal torso.png', 'FO76 armor metal left arm.png', 'FO76 armor metal right arm.png',
                'Fo76 armor metal left leg.png', 'Fo76 armor metal right leg.png']),
    armor('raider', 'Raider armor (Fallout 76)', 'Raider armor', 'Armadura de saqueador', 'wiki', 'FO76 armor heavy raider set.png',
          'Raider', 'de saqueador', head_src='no-ingame-helmet',
          imgs=arms_legs(None, 'FO76 armor sturdy raider torso 1.png', 'FO76 armor heavy raider arms.png', 'FO76 armor heavy raider legs.png')),
    armor('robot', 'Robot armor (Fallout 76)', 'Robot armor', 'Armadura robótica', 'wiki', 'Fo76 armor heavy robot set.png',
          'Robot', 'de robot',
          imgs=arms_legs('Fo76 armor sentry helmet.png', 'Fo76 armor heavy robot torso.png', 'Fo76 armor heavy robot arms.png', 'FO76 armor robot heavy legs.png')),
    armor('secret_service', 'Secret Service armor', 'Secret Service armor', 'Armadura del servicio secreto', 'wiki', 'Fo76WL armor Secret Service set.png',
          'Secret Service', 'del servicio secreto',
          imgs=arms_legs('FO76WL armor Secret Service helmet.png', 'Fo76WL armor Secret Service torso.png', 'Fo76WL armor Secret Service arms.png', 'Fo76WL armor Secret Service legs.png')),
    armor('solar', 'Solar armor', 'Solar armor', 'Armadura solar', 'wiki', 'FO76NW vault 94 scout set.png',
          'Solar armor', 'de armadura solar', head='mask', torso_es='coraza',
          head_en='Vault 94 scout armor mask', head_es_of='de armadura de scout del Refugio 94',
          imgs=arms_legs('FO76NW vault 94 scout mask.png', 'FO76NW vault 94 scout torso.png', 'FO76NW vault 94 scout arms.png', 'FO76NW vault 94 scout legs.png')),
    armor('thorn', 'Thorn armor', 'Thorn armor', 'Armadura con púas', 'wiki', 'FO76NW vault 94 scout set.png',
          'Thorn armor', 'de armadura con púas', head='mask', torso_es='coraza',
          head_en='Vault 94 scout armor mask', head_es_of='de armadura de scout del Refugio 94',
          imgs=arms_legs('FO76NW vault 94 scout mask.png', 'FO76NW vault 94 scout torso.png', 'FO76NW vault 94 scout arms.png', 'FO76NW vault 94 scout legs.png')),
    armor('trapper', 'Trapper armor (Fallout 76)', 'Trapper armor', 'Armadura de trampero', 'wiki', 'FO76 armor trapper set.png',
          'Trapper', 'de trampero',
          imgs=arms_legs('FO76 headgear trapperhelm.webp', 'FO76 armor trapper torso.png', 'FO76 armor trapper arms.png', 'FO76 armor trapper legs.png')),
    armor('urban_scout', 'Urban scout armor', 'Urban scout armor', 'Armadura de scout urbano', 'wiki', 'FO76 armor urban scout set.png',
          'Urban scout armor', 'de armadura de scout urbano', head='mask', torso_es='coraza',
          imgs=arms_legs('Fallout 76 Scout Armor Mask.png', 'FO76 armor urban torso.png', 'FO76 armor urban arms.png', 'FO76 armor urban legs.png')),
    armor('wood', 'Wood armor', 'Wood armor', 'Armadura de madera', 'wiki', 'FO76 armor wood set.png',
          'Wood', 'de madera', head_es_of='de armadura de madera',
          imgs=arms_legs('FO76 armor wood helmet.png', 'FO76 armor wood torso.png', 'FO76 armor wood arms.png', 'FO76 armor wood legs.png')),
]


def api(params):
    params = dict(params, format='json', formatversion='2')
    url = API + '?' + urllib.parse.urlencode(params)
    return json.load(urllib.request.urlopen(urllib.request.Request(url, headers=UA)))


def resolve_images(files):
    files = sorted(set(f for f in files if f))
    urls = {}
    for k in range(0, len(files), 50):
        chunk = files[k:k + 50]
        r = api(dict(action='query', titles='|'.join('File:' + f for f in chunk), prop='imageinfo', iiprop='url', iiurlwidth=256, redirects=1))
        q = r['query']
        back = {}
        for n in q.get('normalized', []):
            back[n['to']] = n['from']
        for rd in q.get('redirects', []):
            back[rd['to']] = back.get(rd['from'], rd['from'])
        for p in q['pages']:
            if p.get('imageinfo'):
                t = back.get(p['title'], p['title'])
                ii = p['imageinfo'][0]
                urls[t[5:]] = ii.get('thumburl') or ii['url']
    # map back with normalization (underscores/case of first letter)
    def norm(s): return s.replace('_', ' ')
    out = {}
    for f in files:
        out[f] = urls.get(f) or urls.get(norm(f)) or urls.get(norm(f)[0].upper() + norm(f)[1:])
    return out


def build(sets, category, power):
    files = [s['img'] for s in sets] + [i for s in sets for i in s['imgs']]
    urls = resolve_images(files)
    missing = [f for f in files if f and not urls.get(f)]
    if missing:
        print('WARNING missing images:', missing, file=sys.stderr)
    sections, items = [], []
    for s in sets:
        set_url = urls[s['img']]
        wiki = 'https://fallout.fandom.com/wiki/' + s['page'].replace(' ', '_')
        sections.append(dict(id=s['id'], en=s['en'], es=s['es'], esSource=s['src'], image=set_url, wiki=wiki))
        for part, en_name, img in zip(PARTS, s['pieces'], s['imgs']):
            if power:
                es_word = ES_PART[part]
                of = s['es_of']
            else:
                if part == 'helmet':
                    es_word = ES_PART[s['head']]
                    of = s['head_es_of'] or s['es_of']
                elif part == 'torso':
                    es_word = ES_PART[s['torso_es']]
                    of = s['es_of']
                else:
                    es_word = ES_PART[part]
                    of = s['es_of']
            item = dict(id=f"{s['id']}_{part}", section=s['id'], en=en_name, es=f'{es_word} {of}',
                        esSource=s['src'], image=urls.get(img) or set_url, wiki=wiki)
            if part == 'helmet' and not power and s.get('head_src'):
                item['note'] = s['head_src']
            items.append(item)
    return {'category': category, 'sections': sections, 'items': items}


if __name__ == '__main__':
    out = sys.argv[1]
    for name, sets, power in [('power_armor', PA, True), ('armor', ARMOR, False)]:
        data = build(sets, name, power)
        with open(os.path.join(out, name + '.json'), 'w', encoding='utf-8') as f:
            json.dump(data, f, ensure_ascii=False, indent=1)
        print(name, len(data['sections']), 'sets', len(data['items']), 'pieces')
