import json, re, sys, urllib.request, urllib.parse, os
D = sys.argv[1]
UA = {'User-Agent': 'Mozilla/5.0 (compatible; F76Checker/1.0)'}
def api(base, params):
    params = dict(params, format='json', formatversion='2')
    url = base + '?' + urllib.parse.urlencode(params)
    return json.load(urllib.request.urlopen(urllib.request.Request(url, headers=UA)))
EN='https://fallout.fandom.com/api.php'; ES='https://fallout.fandom.com/es/api.php'
en = open(os.path.join(D,'en.wiki'),encoding='utf-8').read()
# parse sections
items=[]; group=None; sub=None
lines=en.split('\n'); i=0
cur_link=None; cur_disp=None
for ln in lines:
    m=re.match(r'^==([^=].*?)==\s*$',ln)
    if m: group=m.group(1).strip(); continue
    m=re.match(r'^\|title\s*=(.*)$',ln)
    if m: sub=m.group(1).strip(); continue
    m=re.match(r'^\|\s*\[\[File:([^|\]]+)\|',ln)
    if m and cur_link:
        items.append(dict(group=group,sub=sub,link=cur_link,en=cur_disp,file=m.group(1).strip()))
        cur_link=None; continue
    if ln.startswith('|') and not ln.startswith('|title') and not ln.startswith('|colnumber'):
        links=[l for l in re.findall(r'\[\[([^\]]+)\]\]',ln) if not l.startswith('File:')]
        if links:
            parts=links[-1].split('|'); cur_link=parts[0].strip(); cur_disp=parts[-1].strip(); continue
        if ln.strip()=='|' and cur_link:
            items.append(dict(group=group,sub=sub,link=cur_link,en=cur_disp,file=None)); cur_link=None
items=[x for x in items if x['group']!='Cut weapons']
print(len(items), file=sys.stderr)
# langlinks en->es
links=sorted(set(x['link'] for x in items))
ll={}; redirects={}
for k in range(0,len(links),50):
    r=api(EN,dict(action='query',titles='|'.join(links[k:k+50]),prop='langlinks',lllang='es',redirects=1))
    q=r['query']
    for rd in q.get('redirects',[]): redirects[rd['from']]=rd['to']
    for n in q.get('normalized',[]): redirects.setdefault(n['from'],n['to'])
    for p in q['pages']:
        for l in p.get('langlinks',[]): ll[p['title']]=l['title']
def resolve(t):
    seen=0
    while t in redirects and seen<5: t=redirects[t]; seen+=1
    return t
for x in items:
    x['es_article']=ll.get(resolve(x['link']))
# image urls
files=sorted(set(x['file'] for x in items if x['file'])); urls={}
for k in range(0,len(files),50):
    r=api(EN,dict(action='query',titles='|'.join('File:'+f for f in files[k:k+50]),prop='imageinfo',iiprop='url',iiurlwidth=256,redirects=1))
    q=r['query']; norm={n['to']:n['from'] for n in q.get('normalized',[])}
    rmap={rd['to']:rd['from'] for rd in q.get('redirects',[])}
    for p in q['pages']:
        if p.get('imageinfo'):
            t=p['title']; t=rmap.get(t,t); t=norm.get(t,t)
            urls[t[5:]]=p['imageinfo'][0].get('thumburl') or p['imageinfo'][0]['url']
for x in items: x['img']=urls.get(x['file'])
json.dump(items,open(os.path.join(D,'items1.json'),'w',encoding='utf-8'),ensure_ascii=False,indent=1)
print('noimg',[x['file'] for x in items if not x['img']])
print('noes',len([x for x in items if not x['es_article']]))
