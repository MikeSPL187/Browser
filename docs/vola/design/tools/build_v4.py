"""Build Vola v4 direction boards into the canvas folder."""
import pathlib

SP = pathlib.Path(__file__).resolve().parent
OUT = pathlib.Path(__file__).resolve().parent.parent / 'canvas'
BAIKAL = (SP / 'baikal.svg').read_text(encoding='utf-8')
BAIKAL_T = BAIKAL.replace('id="', 'id="t').replace('url(#', 'url(#t')
FONTS = ('<link href="https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&amp;'
         'family=Literata:opsz,wght@7..72,400;7..72,600&amp;display=swap" rel="stylesheet">')


def status(light=False):
    cls = 'sb4 lt' if light else 'sb4'
    return (f'<div class="{cls}" aria-hidden="true"><span>12:30</span><span class="ic">'
            '<svg viewBox="0 0 16 16"><path d="M15 1.5v13H1.5z"></path></svg>'
            '<svg viewBox="0 0 16 16"><path d="M8 14.2L.6 5.3a11.6 11.6 0 0 1 14.8 0z"></path></svg>'
            '<svg class="bat" viewBox="0 0 26 16"><rect x="1" y="3.5" width="20" height="9" rx="3" fill="none" stroke="currentColor" stroke-width="1.3"></rect>'
            '<rect x="2.9" y="5.4" width="13" height="5.2" rx="1.6"></rect><rect x="22.3" y="6.3" width="2" height="3.4" rx="1"></rect></svg>'
            '</span></div>')


def handle(light=False):
    return f'<div class="gh4{" lt" if light else ""}" aria-hidden="true"></div>'


def board(name, title, root_cls, body, w=390, h=844, style='', extra_css='', logic=''):
    logic = logic or 'class Component extends DCLogic {\nrenderVals() {\nreturn {};\n}\n}'
    html = f'''<!doctype html>
<html lang="ru">
<head>
<meta charset="utf-8">
<title>{title}</title>
<script src="./support.js"></script>
<link rel="stylesheet" href="./vola4-colors.css">
<link rel="stylesheet" href="./vola4.css">
</head>
<body>
<x-dc>
<helmet>
{FONTS}
<style>body{{margin:0;background:#E7EBEC}}{extra_css}</style>
</helmet>
<div class="v4 {root_cls}" style="width: {w}px; height: {h}px; {style}">
{body}
</div>
</x-dc>
<script type="text/x-dc" data-dc-script data-props='{{"$preview":{{"width":{w},"height":{h}}}}}'>
{logic}
</script>
</body>
</html>
'''
    (OUT / name).write_text(html, encoding='utf-8')


# ---------- shared page content: the article on north-guide.ru ----------

def article(dark=False, top_pad=0):
    ink = '#E6EEEC' if dark else '#15201D'
    text = '#C3CFCC' if dark else '#2B3432'
    muted = '#8FA19C' if dark else '#6A7672'
    kicker = '#7CCBB8' if dark else '#2F6B5F'
    bg = '#111716' if dark else '#FFFFFF'
    head_bd = 'rgba(255,255,255,0.08)' if dark else '#EDF0EF'
    return f'''<div style="position: absolute; inset: 0; background: {bg}">
<div style="height: {56 + top_pad}px; padding: {top_pad}px 8px 0 18px; display: flex; align-items: center; gap: 10px; border-bottom: 1px solid {head_bd}">
<span style="width: 28px; height: 28px; border-radius: 9px; background: #2F6B5F; display: flex; align-items: center; justify-content: center"><svg viewBox="0 0 24 24" style="width: 18px; height: 18px"><path d="M3 18 L9 9 L13 14 L16 10 L21 18 Z" fill="#DDF3EC"></path></svg></span>
<span style="flex-grow: 1; font-size: 15px; font-weight: 700; color: {ink}; letter-spacing: -0.01em">Северный путеводитель</span>
<span style="font-size: 13px; font-weight: 600; color: {kicker}; padding: 0 10px">Маршруты</span>
</div>
<div style="padding: 20px 20px 0; display: flex; flex-direction: column; gap: 14px">
<span style="font-size: 12px; font-weight: 700; letter-spacing: 0.08em; text-transform: uppercase; color: {kicker}">Зима · 12 минут чтения</span>
<h1 style="font-family: Literata, Georgia, serif; font-size: 30px; line-height: 36px; font-weight: 600; letter-spacing: -0.01em; color: {ink}">Байкал зимой: как выбрать маршрут по льду</h1>
<span style="display: flex; align-items: center; gap: 10px; font-size: 13px; color: {muted}"><span class="fav" style="width: 28px; height: 28px; border-radius: 14px; background: #F1DED2; color: #8A4A2B; font-size: 11px">АО</span><span><b style="font-weight: 600; color: {text}">Анна Орлова</b> · 12 февраля</span></span>
<figure style="margin: 4px 0 0; display: flex; flex-direction: column; gap: 8px">
<div style="height: 196px; border-radius: 18px; overflow: hidden">{BAIKAL}</div>
<figcaption style="font-size: 12px; color: {muted}">Прозрачный лёд у Ольхона в конце февраля</figcaption>
</figure>
<p style="font-family: Literata, Georgia, serif; font-size: 17px; line-height: 28px; color: {text}">Лёд на Байкале становится крепким к середине февраля. До этого лучше выбирать прогулки у берега и маршруты с проводником.</p>
<p style="font-family: Literata, Georgia, serif; font-size: 17px; line-height: 28px; color: {text}">Ниже — три маршрута разной длины: от короткой прогулки к гроту до двухдневного перехода вдоль берега.</p>
</div>
</div>'''


def address_bar(domain='north-guide.ru', glass=False, ws_icon='work'):
    field_bg = 'transparent' if glass else 'color-mix(in srgb, var(--sf-lowest) 80%, transparent)'
    field_ring = 'none' if glass else 'inset 0 0 0 1px color-mix(in srgb, var(--on-sf) 7%, transparent)'
    return f'''<button aria-label="Пространство «Работа»" style="width: 48px; height: 48px; border-radius: 24px; display: flex; align-items: center; justify-content: center; flex-shrink: 0"><span class="gem" style="width: 40px; height: 40px; border-radius: 14px"><span class="ms f s">{ws_icon}</span></span></button>
<button class="fieldpill" aria-label="Адрес и сведения о сайте" style="position: relative; flex-grow: 1; min-width: 0; height: 48px; border-radius: 24px; background: {field_bg}; box-shadow: {field_ring}; display: flex; align-items: center; justify-content: center; gap: 6px; padding: 0 44px 0 14px">
<span class="ms xs" style="color: var(--on-sf-v)">lock</span><span style="font-size: 16px; font-weight: 600; letter-spacing: -0.01em; white-space: nowrap; overflow: hidden; text-overflow: ellipsis">{domain}</span>
<span aria-label="Режим чтения" style="position: absolute; right: 4px; top: 4px; width: 40px; height: 40px; border-radius: 20px; display: flex; align-items: center; justify-content: center; color: var(--pri)"><span class="ms s">menu_book</span></span>
</button>
<button class="ib4" aria-label="Вкладки: 4" style="width: 44px"><span class="tabcount">4</span></button>
<button class="ib4" aria-label="Меню" style="width: 40px; color: var(--on-sf)"><span class="ms">more_vert</span></button>'''


def page_a(dark=False):
    theme = 't-dark' if dark else 't-light'
    body = f'''{status()}
<div class="page-card" style="top: 40px; bottom: 92px; background: {'#111716' if dark else '#FFFFFF'}">
{article(dark)}
</div>
<div style="position: absolute; left: 8px; right: 8px; bottom: 28px; height: 56px; display: flex; align-items: center; gap: 4px">
{address_bar()}
</div>
{handle()}'''
    name = 'V4A-PageDark.dc.html' if dark else 'V4A-Page.dc.html'
    board(name, 'Vola v4 А — страница' + (' (тёмная)' if dark else ''), f'w-work {theme} aura', body)


def page_b(oled=False):
    theme = 't-oled' if oled else 't-light'
    glow_a = 30 if not oled else 22
    body = f'''<div style="position: absolute; inset: 0">{article(oled, top_pad=40)}</div>
{status(light=False) if not oled else status(light=True)}
<div aria-hidden="true" style="position: absolute; left: -40px; right: -40px; bottom: -60px; height: 220px; background: radial-gradient(60% 70% at 30% 100%, color-mix(in srgb, var(--aura-1) {glow_a}%, transparent) 0%, transparent 70%), radial-gradient(60% 70% at 75% 100%, color-mix(in srgb, var(--aura-2) {glow_a}%, transparent) 0%, transparent 70%); filter: blur(6px)"></div>
<div class="glass4 halo" style="position: absolute; left: 12px; right: 12px; bottom: 22px; height: 60px; border-radius: 30px; display: flex; align-items: center; gap: 2px; padding: 0 4px 0 6px">
{address_bar(glass=True)}
</div>
{handle(light=oled)}'''
    name = 'V4B-PageOled.dc.html' if oled else 'V4B-Page.dc.html'
    board(name, 'Vola v4 Б — страница' + (' (OLED)' if oled else ''), f'w-work {theme}', body)


if __name__ == '__main__':
    import sys
    targets = sys.argv[1:] or ['page']
    if 'page' in targets:
        page_a(); page_a(dark=True); page_b(); page_b(oled=True)
    print('built', targets)


# ---------- tab overview ----------

def mini(kind, brand, tone):
    """Miniature page used as a tab thumbnail (161 x 184)."""
    L = 'var(--sf-high)'
    head = f'<span style="display: flex; align-items: center; gap: 5px"><i style="width: 12px; height: 12px; border-radius: 4px; background: {brand}"></i><i style="height: 5px; width: 40%; border-radius: 3px; background: {L}"></i></span>'
    line = lambda w, h=5: f'<i style="height: {h}px; width: {w}%; border-radius: 3px; background: {L}"></i>'
    ink = lambda w, h=8: f'<i style="height: {h}px; width: {w}%; border-radius: 4px; background: var(--on-sf); opacity: 0.7"></i>'
    if kind == 'doc':
        inner = head + ink(84) + ink(56) + line(94) + line(88) + line(70) + ''.join(
            f'<span style="display: flex; gap: 5px; align-items: center"><i style="width: 9px; height: 9px; border-radius: 3px; {s}"></i>{line(w)}</span>'
            for s, w in [(f'background: {brand}', 60), ('box-shadow: inset 0 0 0 1.5px var(--ol-v)', 52), ('box-shadow: inset 0 0 0 1.5px var(--ol-v)', 58)]) + \
            f'<span style="margin-top: 2px; border-radius: 8px; background: {tone}; padding: 7px; display: flex; flex-direction: column; gap: 4px">{line(78)}{line(52)}</span>'
    elif kind == 'board':
        cols = [('#FFE1D6', [36, 24, 30]), ('#E6DEFF', [28, 44]), ('#D4F1EC', [22, 30, 18])]
        inner = head + ink(52) + '<span style="display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 5px; margin-top: 2px">' + ''.join(
            '<span style="display: flex; flex-direction: column; gap: 5px">' + line(70, 4) + ''.join(f'<i style="height: {h}px; border-radius: 7px; background: {c}"></i>' for h in hs) + '</span>'
            for c, hs in cols) + '</span>'
    elif kind == 'chart':
        bars = [34, 48, 40, 62, 55, 84, 72]
        inner = head + f'<span style="display: flex; gap: 5px"><span style="flex: 1; border-radius: 8px; background: {tone}; padding: 6px; display: flex; flex-direction: column; gap: 4px"><i style="height: 4px; width: 60%; border-radius: 2px; background: rgba(0,0,0,0.1)"></i><i style="height: 10px; width: 70%; border-radius: 3px; background: {brand}"></i></span><span style="flex: 1; border-radius: 8px; background: var(--sf-low); padding: 6px; display: flex; flex-direction: column; gap: 4px">{line(56, 4)}<i style="height: 10px; width: 56%; border-radius: 3px; background: var(--on-sf); opacity: 0.55"></i></span></span>' + \
            '<span style="height: 72px; display: flex; align-items: flex-end; gap: 5px; border-bottom: 1.5px solid var(--sf-high); margin-top: 4px">' + ''.join(
                f'<i style="flex: 1; height: {b}%; border-radius: 3px 3px 1px 1px; background: {brand if k == 5 else tone}"></i>' for k, b in enumerate(bars)) + '</span>' + line(80)
    elif kind == 'article':
        inner = ('<span style="display: flex; align-items: center; gap: 5px"><i style="width: 12px; height: 12px; border-radius: 4px; background: #2F6B5F"></i><i style="height: 5px; width: 50%; border-radius: 3px; background: #E3E8E6"></i></span>'
                 '<i style="height: 8px; width: 88%; border-radius: 4px; background: #15201D; opacity: 0.8"></i><i style="height: 8px; width: 62%; border-radius: 4px; background: #15201D; opacity: 0.8"></i>'
                 f'<span style="height: 66px; border-radius: 9px; overflow: hidden; margin-top: 2px">{BAIKAL_T}</span>'
                 + ''.join(f'<i style="height: 5px; width: {w}%; border-radius: 3px; background: #E3E8E6"></i>' for w in (96, 90, 74)))
    elif kind == 'list':
        inner = head + ink(60) + ''.join(f'<span style="display: flex; gap: 6px; align-items: center"><i style="width: 22px; height: 16px; border-radius: 4px; background: {tone}"></i>{line(w)}</span>' for w in (70, 58, 76, 64))
    else:
        inner = head
    inner += ''.join(line(w) for w in (92, 84, 70, 88, 60, 76))
    bg = '#FFFFFF' if kind == 'article' else 'var(--sf-lowest)'
    return f'<span style="position: relative; display: flex; flex-direction: column; gap: 6px; height: 100%; border-radius: 16px; background: {bg}; padding: 10px; overflow: hidden; box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--on-sf) 6%, transparent)">{inner}</span>'


TABS = [
    ('Прогноз толщины льда', 'П', '#D4F1EC', '#00665A', 'chart'),
    ('Расписание электричек до Листвянки', 'Р', '#D8EEFF', '#1E5E8C', 'list'),
    ('План запуска на октябрь', 'Д', '#E6DEFF', '#4A3A9E', 'doc'),
    ('Спринт 42 · доска задач', 'З', '#FFE1D6', '#9A3A1E', 'board'),
    ('Отчёт по метрикам', 'О', '#FFEBC2', '#6E4F00', 'chart'),
    ('Байкал зимой: как выбрать маршрут', 'С', '#2F6B5F', '#FFFFFF', 'article'),
]


def tab_card(t, current=False, card_bg='var(--sf-lowest)'):
    title, l, tone, ink, kind = t
    brand = ink if kind != 'article' else '#2F6B5F'
    ring = 'box-shadow: 0 0 0 2.5px var(--pri), 0 10px 28px color-mix(in srgb, var(--pri) 28%, transparent);' if current else 'box-shadow: var(--e1);'
    fav_bg, fav_ink = (tone, ink) if kind != 'article' else ('#2F6B5F', '#FFFFFF')
    return f'''<div style="height: 248px; border-radius: 22px; background: {card_bg}; {ring} padding: 6px; display: flex; flex-direction: column; gap: 4px">
<div style="height: 34px; display: flex; align-items: center; gap: 8px; padding: 0 0 0 6px">
<span class="fav" style="width: 20px; height: 20px; border-radius: 6px; background: {fav_bg}; color: {fav_ink}; font-size: 11px">{l}</span>
<span style="flex-grow: 1; min-width: 0; font-size: 13px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis">{title}</span>
<button aria-label="Закрыть вкладку" style="width: 32px; height: 32px; border-radius: 16px; display: flex; align-items: center; justify-content: center; color: var(--on-sf-v)"><span class="ms xs">close</span></button>
</div>
<div style="flex-grow: 1; min-height: 0">{mini(kind, brand, tone if kind != 'article' else '#D4F1EC')}</div>
</div>'''


ESS = [('Почта', 'П', '#DDE3FF', '#2F4AA8'), ('Календарь', 'К', '#D4F1EC', '#00665A'), ('Задачи', 'З', '#FFE1D6', '#9A3A1E'),
       ('Документы', 'Д', '#E6DEFF', '#4A3A9E'), ('Чат', 'Ч', '#D8EEFF', '#1E5E8C')]


def tabs_body(direction):
    frame = direction == 'A'
    card_bg = 'var(--sf-lowest)' if frame else 'var(--sf-low)'
    fade_to = 'var(--sf-c)' if frame else 'var(--sf)'
    grid = ''.join(tab_card(t, current=(k == 5), card_bg=card_bg) for k, t in enumerate(TABS))
    ess = ''.join(f'<a href="#" aria-label="{n}" style="height: 56px; border-radius: 18px; background: {"color-mix(in srgb, var(--sf-lowest) 82%, transparent)" if frame else "var(--sf-lowest)"}; box-shadow: var(--e1); display: flex; align-items: center; justify-content: center"><span class="fav" style="width: 30px; height: 30px; border-radius: 10px; background: {bg}; color: {ink}; font-size: 14px">{l}</span></a>' for n, l, bg, ink in ESS)
    pills = f'''<button role="tab" aria-selected="true" style="height: 48px; border-radius: 24px; background: var(--sf-lowest); box-shadow: var(--e1); display: flex; align-items: center; gap: 8px; padding: 0 14px 0 4px"><span class="gem" style="width: 40px; height: 40px; border-radius: 14px"><span class="ms f s">work</span></span><span style="font-size: 15px; font-weight: 600">Работа</span></button>
<button role="tab" aria-selected="false" aria-label="Аниме" style="width: 48px; height: 48px; display: flex; align-items: center; justify-content: center"><span class="w-anime t-light gem" style="width: 32px; height: 32px; border-radius: 11px"><span class="ms f xs">movie</span></span></button>
<button role="tab" aria-selected="false" aria-label="Личное" style="width: 48px; height: 48px; display: flex; align-items: center; justify-content: center"><span class="w-personal t-light gem" style="width: 32px; height: 32px; border-radius: 11px"><span class="ms f xs">home</span></span></button>
<button role="tab" aria-selected="false" aria-label="Приватные вкладки" style="width: 48px; height: 48px; display: flex; align-items: center; justify-content: center"><span style="width: 32px; height: 32px; border-radius: 11px; background: #1E1B2E; color: #C9BFFF; display: flex; align-items: center; justify-content: center"><span class="ms f xs">domino_mask</span></span></button>'''
    plus = '<button aria-label="Новая вкладка" style="width: 56px; height: 56px; border-radius: 20px; background: var(--pri); color: var(--on-pri); display: flex; align-items: center; justify-content: center; box-shadow: 0 8px 20px color-mix(in srgb, var(--pri) 35%, transparent); flex-shrink: 0"><span class="ms l">add</span></button>'
    if frame:
        bar = f'<div role="tablist" aria-label="Пространства" style="flex-grow: 1; min-width: 0; height: 56px; border-radius: 28px; background: color-mix(in srgb, var(--sf-lowest) 70%, transparent); box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--on-sf) 6%, transparent); display: flex; align-items: center; gap: 2px; padding: 0 4px">{pills}</div>'
        bottom = f'<div style="position: absolute; left: 12px; right: 12px; bottom: 26px; display: flex; align-items: center; gap: 10px">{bar}{plus}</div>'
    else:
        bar = f'<div role="tablist" aria-label="Пространства" class="glass4 halo" style="flex-grow: 1; min-width: 0; height: 60px; border-radius: 30px; display: flex; align-items: center; gap: 2px; padding: 0 4px">{pills}</div>'
        bottom = f'<div style="position: absolute; left: 12px; right: 12px; bottom: 22px; display: flex; align-items: center; gap: 10px">{bar}{plus}</div>'
    glow = '' if frame else '<div aria-hidden="true" style="position: absolute; left: -60px; right: -60px; bottom: -80px; height: 260px; background: radial-gradient(50% 60% at 30% 100%, color-mix(in srgb, var(--aura-1) 40%, transparent) 0%, transparent 70%), radial-gradient(50% 60% at 75% 100%, color-mix(in srgb, var(--aura-2) 36%, transparent) 0%, transparent 70%)"></div>'
    return f'''{glow}{status()}
<div style="position: absolute; left: 20px; right: 8px; top: 44px; height: 56px; display: flex; align-items: center; gap: 4px">
<span style="flex-grow: 1; display: flex; align-items: baseline; gap: 10px"><span class="ty-head">Работа</span><span class="ty-cap" style="font-size: 14px">6 вкладок</span></span>
<button class="ib4" aria-label="Найти вкладку" style="color: var(--on-sf)"><span class="ms">search</span></button>
<button class="ib4" aria-label="Ещё" style="color: var(--on-sf)"><span class="ms">more_vert</span></button>
</div>
<div style="position: absolute; left: 10px; right: 10px; top: 104px; height: 578px; overflow: hidden; -webkit-mask-image: linear-gradient(180deg, transparent 0, #000 72px); mask-image: linear-gradient(180deg, transparent 0, #000 72px)">
<div style="position: absolute; left: 6px; right: 6px; bottom: 14px; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px">{grid}</div>
</div>
<div style="position: absolute; left: 16px; right: 16px; top: 690px; display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 10px">{ess}</div>
{bottom}
{handle()}'''


def tabs():
    board('V4A-Tabs.dc.html', 'Vola v4 А — обзор вкладок', 'w-work t-light aura', tabs_body('A'))
    board('V4B-Tabs.dc.html', 'Vola v4 Б — обзор вкладок', 'w-work t-light', tabs_body('B'))


if __name__ == '__main__':
    import sys
    if 'tabs' in sys.argv[1:]:
        tabs()


# ---------- address entry ----------

def keyboard(dark=False):
    kb_bg = '#1E2426' if dark else '#E3E8EA'
    key = '#3A4245' if dark else '#FFFFFF'
    fn = '#2C3336' if dark else '#C9D1D4'
    ink = '#E8EEF0' if dark else '#1B2224'
    k = lambda ch, grow=1, bg=None, extra='': f'<span style="flex: {grow} 1 0; height: 44px; border-radius: 9px; background: {bg or key}; box-shadow: 0 1px 0 rgba(0,0,0,{0.3 if dark else 0.12}); display: flex; align-items: center; justify-content: center; font-size: 20px; font-weight: 400; color: {ink}; {extra}">{ch}</span>'
    row = lambda chars: '<div style="display: flex; gap: 5px">' + ''.join(k(c) for c in chars) + '</div>'
    return f'''<div aria-label="Клавиатура системы" style="position: absolute; left: 0; right: 0; bottom: 0; height: 290px; background: {kb_bg}; padding: 0 4px 30px">
<div style="height: 46px; display: flex; align-items: center; color: {ink}; font-size: 16px">
<span style="width: 48px; display: flex; justify-content: center; opacity: 0.7"><span class="ms s">apps</span></span>
<span style="flex: 1; text-align: center; opacity: 0.75">лёд</span><i style="width: 1px; height: 20px; background: {fn}"></i>
<span style="flex: 1; text-align: center; font-weight: 600">льда</span><i style="width: 1px; height: 20px; background: {fn}"></i>
<span style="flex: 1; text-align: center; opacity: 0.75">лёдом</span>
<span style="width: 48px; display: flex; justify-content: center; opacity: 0.7"><span class="ms s">mic</span></span>
</div>
<div style="display: flex; flex-direction: column; gap: 9px">
{row('йцукенгшщзх')}
{row('фывапролджэ')}
<div style="display: flex; gap: 5px">{k('<span class="ms s">keyboard_capslock</span>', 1.4, fn)}{''.join(k(c) for c in 'ячсмитьбю')}{k('<span class="ms s">backspace</span>', 1.4, fn)}</div>
<div style="display: flex; gap: 5px">{k('?123', 1.5, fn, 'font-size: 14px; font-weight: 600')}{k(',', 1, fn)}{k('<span class="ms s">language</span>', 1, fn)}{k('Русский', 4.4, None, 'font-size: 13px; font-weight: 500; opacity: 0.7')}{k('.', 1, fn)}{k('<span class="ms s">arrow_forward</span>', 1.5, 'var(--pri)', 'color: var(--on-pri)')}</div>
</div>
</div>'''


def suggest_row(fav, fav_bg, fav_ink, title, sub, trail, strong=False):
    return f'''<a href="#" style="min-height: 56px; display: flex; align-items: center; gap: 14px; padding: 6px 8px 6px 12px; border-radius: 18px{'; background: color-mix(in srgb, var(--pri-c) 45%, transparent)' if strong else ''}">
<span class="fav" style="width: 36px; height: 36px; border-radius: 12px; background: {fav_bg}; color: {fav_ink}; font-size: 15px">{fav}</span>
<span style="flex-grow: 1; min-width: 0; display: flex; flex-direction: column; gap: 1px"><span style="font-size: 15px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis">{title}</span><span class="ty-cap" style="white-space: nowrap; overflow: hidden; text-overflow: ellipsis">{sub}</span></span>
{trail}
</a>'''


def query_row(rest):
    return f'''<a href="#" style="height: 48px; display: flex; align-items: center; gap: 14px; padding: 0 4px 0 20px; border-radius: 16px">
<span class="ms s" style="color: var(--on-sf-v)">search</span>
<span style="flex-grow: 1; font-size: 16px; font-weight: 500; padding-left: 4px">байкал лёд <b style="font-weight: 700">{rest}</b></span>
<span class="ib4" aria-label="Подставить в строку" style="width: 40px; height: 40px"><span class="ms s">north_west</span></span>
</a>'''


def address_body(direction, dark=False):
    frame = direction == 'A'
    ic = lambda n: f'<span class="ms s" style="color: var(--on-sf-v); margin-right: 8px">{n}</span>'
    rows = (suggest_row('П', '#D8EEFF', '#1E5E8C', 'Погода на Байкале в феврале', 'pogoda.example.ru · вчера', ic('history'))
            + suggest_row('Л', '#FFE9B8', '#7C5800', 'Ледовые маршруты Байкала', 'Избранное · Путешествия', ic('star'))
            + '<i style="height: 1px; margin: 4px 14px; background: var(--ol-v); opacity: 0.5"></i>'
            + query_row('где кататься') + query_row('на неделю') + query_row('толщина')
            + '<i style="height: 1px; margin: 4px 14px; background: var(--ol-v); opacity: 0.5"></i>'
            + suggest_row('С', '#2F6B5F', '#FFFFFF', 'Байкал зимой: как выбрать маршрут', 'Открыта во вкладке', '<span class="btn fill" style="height: 36px; padding: 0 14px; font-size: 13.5px">Перейти</span>', strong=True))
    card_bg = 'var(--sf-lowest)'
    list_card = f'<div style="position: absolute; left: 10px; right: 10px; bottom: 412px; border-radius: 26px; background: {card_bg}; box-shadow: var(--e2); padding: 6px">{rows}</div>'
    helper = f'''<div style="position: absolute; left: 10px; right: 10px; bottom: 362px; height: 40px; display: flex; gap: 8px">
<a href="#" style="flex-grow: 1; min-width: 0; height: 40px; border-radius: 20px; background: var(--sf-lowest); box-shadow: var(--e1); display: flex; align-items: center; gap: 8px; padding: 0 4px 0 12px">
<span class="ms xs" style="color: var(--pri)">content_paste</span>
<span style="flex-grow: 1; min-width: 0; font-size: 13.5px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis"><span style="color: var(--on-sf-v); font-weight: 500">Из буфера · </span>north-guide.ru/routes/olkhon</span>
<span style="height: 32px; border-radius: 16px; background: var(--sec-c); color: var(--on-sec-c); padding: 0 12px; display: flex; align-items: center; font-size: 13px; font-weight: 600">Открыть</span>
</a>
<button aria-label="Искать в DuckDuckGo" style="height: 40px; border-radius: 20px; background: var(--sf-lowest); box-shadow: var(--e1); display: flex; align-items: center; gap: 2px; padding: 0 6px 0 5px"><span class="fav" style="width: 30px; height: 30px; border-radius: 15px; background: #FDE3D9; color: #C2431E; font-size: 13px">D</span><span class="ms xs" style="color: var(--on-sf-v)">expand_more</span></button>
</div>'''
    field_cls = 'glass4 halo' if not frame else ''
    field_style = '' if not frame else 'background: var(--sf-lowest); box-shadow: 0 0 0 2px var(--pri), var(--e2);'
    field = f'''<div class="{field_cls}" style="position: absolute; left: 10px; right: 10px; bottom: 298px; height: 56px; border-radius: 28px; {field_style} display: flex; align-items: center; gap: 4px; padding: 0 4px 0 18px">
<span class="ms s" style="color: var(--on-sf-v)">search</span>
<span style="flex-grow: 1; min-width: 0; display: flex; align-items: center; padding-left: 8px; font-size: 17px; font-weight: 600">байкал лёд<i style="width: 2px; height: 22px; margin-left: 1px; border-radius: 1px; background: var(--pri)"></i></span>
<button class="ib4" aria-label="Очистить" style="width: 40px; height: 40px"><span class="ms s">close</span></button>
<button class="ib4" aria-label="Голосовой ввод" style="color: var(--on-sf)"><span class="ms">mic</span></button>
</div>'''
    bg_glow = '' if frame else '<div aria-hidden="true" style="position: absolute; left: -80px; right: -80px; top: -120px; height: 520px; background: radial-gradient(50% 50% at 30% 40%, color-mix(in srgb, var(--aura-1) 30%, transparent) 0%, transparent 70%), radial-gradient(45% 45% at 80% 55%, color-mix(in srgb, var(--aura-2) 26%, transparent) 0%, transparent 70%)"></div>'
    return f'''{bg_glow}{status()}
<div style="position: absolute; left: 22px; right: 10px; top: 48px; height: 48px; display: flex; align-items: center; gap: 10px"><span class="gem" style="width: 32px; height: 32px; border-radius: 11px"><span class="ms f xs">work</span></span><span class="ty-label" style="flex-grow: 1">Новая вкладка в «Работе»</span><button class="ib4" aria-label="Сканировать QR-код" style="color: var(--on-sf)"><span class="ms">qr_code_scanner</span></button></div>
{list_card}
{helper}
{field}
{keyboard(dark)}
{handle(light=dark)}'''


def address():
    board('V4A-Address.dc.html', 'Vola v4 А — ввод адреса', 'w-work t-light aura', address_body('A'))
    board('V4B-Address.dc.html', 'Vola v4 Б — ввод адреса', 'w-work t-light', address_body('B'))


if __name__ == '__main__':
    import sys
    if 'address' in sys.argv[1:]:
        address()


# ---------- second workspace: the shell takes the workspace color ----------

def kino(top_pad=0):
    eps = [('Серия 2. Лёд и огонь', '23:10', '#16384A', '#3E6E86'), ('Серия 3. Под водой', '24:48', '#2A2216', '#8A6A3A'), ('Серия 4. Большие Коты', '22:05', '#231A2E', '#6B3E7A'), ('Серия 5. Шаманка', '23:40', '#2B1A16', '#9A4E55')]
    ep = ''.join(f'<span style="display: flex; gap: 12px; align-items: center"><span style="position: relative; width: 120px; height: 68px; border-radius: 12px; background: linear-gradient(160deg, {a} 0%, {b} 100%); flex-shrink: 0"><span style="position: absolute; right: 6px; bottom: 6px; height: 18px; border-radius: 6px; padding: 0 6px; background: rgba(0,0,0,0.55); color: #FFFFFF; font-size: 11px; font-weight: 600; display: flex; align-items: center">{d}</span></span><span style="display: flex; flex-direction: column; gap: 3px"><span style="font-size: 14.5px; font-weight: 600; color: #1C1B1F">{t}</span><span style="font-size: 12.5px; color: #6B6470">Кинотека · 23 мин</span></span></span>' for t, d, a, b in eps)
    return f'''<div style="position: absolute; inset: 0; background: #FFFFFF">
<div style="height: {56 + top_pad}px; padding: {top_pad}px 8px 0 18px; display: flex; align-items: center; gap: 10px">
<span class="fav" style="width: 28px; height: 28px; border-radius: 9px; background: #A23F2B; color: #FFFFFF; font-size: 14px">К</span>
<span style="flex-grow: 1; font-size: 16px; font-weight: 700; color: #3A1A12; letter-spacing: -0.01em">Кинотека</span>
<span class="ms" style="color: #5B4A45; padding: 0 12px">search</span>
</div>
<div style="position: relative; height: 216px; overflow: hidden; background: linear-gradient(160deg, #1F1838 0%, #4A2A55 42%, #9A4E55 72%, #D9905C 100%)">
<span style="position: absolute; right: 64px; top: 36px; width: 84px; height: 84px; border-radius: 50%; background: radial-gradient(closest-side, #FFE2B8 0%, rgba(255,226,184,0.35) 55%, rgba(255,226,184,0) 100%)"></span>
<span style="position: absolute; left: -40px; right: -40px; bottom: 34px; height: 70px; border-radius: 50% 50% 0 0; background: #3A2448; opacity: 0.85"></span>
<span style="position: absolute; left: 0; right: 0; bottom: 0; height: 96px; background: linear-gradient(180deg, rgba(16,10,24,0) 0%, rgba(16,10,24,0.82) 100%)"></span>
<span style="position: absolute; left: 50%; top: 78px; margin-left: -30px; width: 60px; height: 60px; border-radius: 30px; background: rgba(0,0,0,0.38); color: #FFFFFF; display: flex; align-items: center; justify-content: center"><span class="ms f l">play_arrow</span></span>
<span style="position: absolute; left: 16px; right: 16px; bottom: 14px; height: 4px; border-radius: 2px; background: rgba(255,255,255,0.3)"><i style="width: 34%; height: 4px; border-radius: 2px; background: #FFFFFF"></i></span>
<span class="glass4 dk" style="position: absolute; right: 10px; top: 10px; height: 40px; border-radius: 20px; display: flex; align-items: center; padding: 0 4px; color: #FFFFFF; background: rgba(28,24,36,0.62)"><span style="width: 36px; height: 36px; display: flex; align-items: center; justify-content: center"><span class="ms s">picture_in_picture_alt</span></span><i style="width: 1px; height: 18px; background: rgba(255,255,255,0.25)"></i><span style="width: 36px; height: 36px; display: flex; align-items: center; justify-content: center"><span class="ms s">more_horiz</span></span></span>
</div>
<div style="padding: 16px 18px; display: flex; flex-direction: column; gap: 14px">
<span style="display: flex; flex-direction: column; gap: 4px"><span style="font-size: 19px; font-weight: 700; color: #1C1B1F; letter-spacing: -0.01em">Обзор первой серии</span><span style="font-size: 13px; color: #6B6470">24 мин · субтитры · 12 тыс. просмотров</span></span>
<span style="font-size: 14px; font-weight: 700; color: #1C1B1F; padding-top: 4px">Следующие серии</span>
{ep}
</div>
</div>'''


def anime_a():
    body = f'''{status()}
<div class="page-card" style="top: 40px; bottom: 92px">{kino()}</div>
<div style="position: absolute; left: 8px; right: 8px; bottom: 28px; height: 56px; display: flex; align-items: center; gap: 4px">
{address_bar('kinoteka.example', ws_icon='movie').replace('Пространство «Работа»', 'Пространство «Аниме»').replace('>menu_book<', '>picture_in_picture_alt<').replace('Режим чтения', 'Картинка в картинке').replace('Вкладки: 4', 'Вкладки: 3').replace('tabcount">4', 'tabcount">3')}
</div>
{handle()}'''
    board('V4A-Anime.dc.html', 'Vola v4 А — пространство «Аниме»', 'w-anime t-light aura', body)


def anime_b():
    body = f'''<div style="position: absolute; inset: 0">{kino(top_pad=40)}</div>
{status()}
<div aria-hidden="true" style="position: absolute; left: -40px; right: -40px; bottom: -60px; height: 220px; background: radial-gradient(60% 70% at 30% 100%, color-mix(in srgb, var(--aura-1) 34%, transparent) 0%, transparent 70%), radial-gradient(60% 70% at 75% 100%, color-mix(in srgb, var(--aura-2) 34%, transparent) 0%, transparent 70%); filter: blur(6px)"></div>
<div class="glass4 halo" style="position: absolute; left: 12px; right: 12px; bottom: 22px; height: 60px; border-radius: 30px; display: flex; align-items: center; gap: 2px; padding: 0 4px 0 6px">
{address_bar('kinoteka.example', glass=True, ws_icon='movie').replace('Пространство «Работа»', 'Пространство «Аниме»').replace('>menu_book<', '>picture_in_picture_alt<').replace('Режим чтения', 'Картинка в картинке').replace('Вкладки: 4', 'Вкладки: 3').replace('tabcount">4', 'tabcount">3')}
</div>
{handle()}'''
    board('V4B-Anime.dc.html', 'Vola v4 Б — пространство «Аниме»', 'w-anime t-light', body)


if __name__ == '__main__':
    import sys
    if 'anime' in sys.argv[1:]:
        anime_a(); anime_b()


# ---------- settings: appearance with the A/B chooser ----------

def mini_phone(style):
    """Tiny phone preview of a chrome style: 'frame' or 'air'."""
    lines = ''.join(f'<i style="height: 4px; width: {w}%; border-radius: 2px; background: #DCE3E2"></i>' for w in (92, 84, 88, 70))
    pic = BAIKAL_T.replace('id="t', 'id="m' + style).replace('url(#t', 'url(#m' + style)
    page = (f'<span style="display: flex; align-items: center; gap: 4px"><i style="width: 9px; height: 9px; border-radius: 3px; background: #2F6B5F"></i><i style="height: 4px; width: 46%; border-radius: 2px; background: #DCE3E2"></i></span>'
            f'<i style="height: 6px; width: 86%; border-radius: 3px; background: #15201D; opacity: 0.8; margin-top: 3px"></i><i style="height: 6px; width: 58%; border-radius: 3px; background: #15201D; opacity: 0.8"></i>'
            f'<span style="height: 44px; border-radius: 7px; overflow: hidden; margin-top: 3px">{pic}</span>{lines}')
    if style == 'frame':
        return f'''<span class="aura" style="position: relative; width: 104px; height: 188px; border-radius: 20px; overflow: hidden; box-shadow: 0 0 0 1px color-mix(in srgb, var(--on-sf) 10%, transparent)">
<span style="position: absolute; left: 3px; right: 3px; top: 10px; bottom: 26px; border-radius: 13px; background: #FFFFFF; padding: 8px 7px; display: flex; flex-direction: column; gap: 4px; overflow: hidden">{page}</span>
<span style="position: absolute; left: 6px; right: 6px; bottom: 6px; height: 14px; display: flex; align-items: center; gap: 3px"><i class="gem" style="width: 13px; height: 13px; border-radius: 5px"></i><i style="flex-grow: 1; height: 13px; border-radius: 7px; background: color-mix(in srgb, #FFFFFF 85%, transparent)"></i></span>
</span>'''
    return f'''<span style="position: relative; width: 104px; height: 188px; border-radius: 20px; overflow: hidden; background: #FFFFFF; box-shadow: 0 0 0 1px color-mix(in srgb, var(--on-sf) 10%, transparent)">
<span style="position: absolute; left: 0; right: 0; top: 10px; bottom: 0; padding: 8px 7px; display: flex; flex-direction: column; gap: 4px">{page}</span>
<span style="position: absolute; left: -20px; right: -20px; bottom: -20px; height: 60px; background: radial-gradient(50% 60% at 30% 100%, color-mix(in srgb, var(--aura-1) 55%, transparent) 0%, transparent 70%), radial-gradient(50% 60% at 75% 100%, color-mix(in srgb, var(--aura-2) 50%, transparent) 0%, transparent 70%)"></span>
<span class="halo" style="position: absolute; left: 6px; right: 6px; bottom: 6px; height: 17px; border-radius: 9px; display: flex; align-items: center; padding: 0 2px; box-shadow: 0 3px 8px color-mix(in srgb, var(--aura-1) 40%, transparent)"><i class="gem" style="width: 11px; height: 11px; border-radius: 4px"></i></span>
</span>'''


def seg(options, selected):
    return '<div class="bgroup4" role="radiogroup">' + ''.join(
        f'<button role="radio" aria-checked="{"true" if k == selected else "false"}" class="{"sel" if k == selected else ""}">{o}</button>' for k, o in enumerate(options)) + '</div>'


def row(icon, title, sub, trail):
    return f'''<div style="min-height: 64px; display: flex; align-items: center; gap: 16px; padding: 10px 16px">
<span class="ms" style="color: var(--on-sf-v)">{icon}</span>
<span style="flex-grow: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px"><span class="ty-label" style="font-size: 15px">{title}</span>{f'<span class="ty-cap">{sub}</span>' if sub else ''}</span>
{trail}
</div>'''


def switch(on):
    return (f'<span style="width: 52px; height: 32px; border-radius: 16px; background: {"var(--pri)" if on else "var(--sf-highest)"}; box-shadow: {"none" if on else "inset 0 0 0 2px var(--ol)"}; display: flex; align-items: center; justify-content: {"flex-end" if on else "flex-start"}; padding: 0 {"4px" if on else "7px"}; flex-shrink: 0">'
            f'<i style="width: {24 if on else 16}px; height: {24 if on else 16}px; border-radius: 12px; background: {"var(--on-pri)" if on else "var(--ol)"}"></i></span>')


def appearance():
    choice = lambda style, name, sub, on: f'''<button role="radio" aria-checked="{'true' if on else 'false'}" style="flex: 1 1 0; border-radius: 24px; padding: 14px 10px 12px; display: flex; flex-direction: column; align-items: center; gap: 10px; background: {'var(--sec-c)' if on else 'var(--sf-lowest)'}; box-shadow: {'inset 0 0 0 2px var(--pri)' if on else 'var(--e1)'}">
{mini_phone(style)}
<span style="display: flex; flex-direction: column; align-items: center; gap: 2px; text-align: center"><span style="display: flex; align-items: center; gap: 6px; font-size: 15px; font-weight: 700">{'<span class="ms f xs" style="color: var(--pri)">check_circle</span>' if on else ''}{name}</span><span class="ty-cap">{sub}</span></span>
</button>'''
    body = f'''{status()}
<div style="position: absolute; left: 8px; right: 8px; top: 44px; height: 56px; display: flex; align-items: center; gap: 4px"><button class="ib4" aria-label="Назад" style="color: var(--on-sf)"><span class="ms">arrow_back</span></button><span class="ty-title-l">Внешний вид</span></div>
<div style="position: absolute; left: 16px; right: 16px; top: 108px; display: flex; flex-direction: column; gap: 12px">
<span class="ty-over" style="padding: 0 4px">Оформление</span>
<div role="radiogroup" aria-label="Оформление" style="display: flex; gap: 10px">
{choice('frame', 'Рама', 'Страница в цвете пространства', True)}
{choice('air', 'Воздух', 'Страница на весь экран', False)}
</div>
<div style="border-radius: 24px; background: var(--sf-lowest); overflow: hidden; margin-top: 4px">
<div style="padding: 14px 16px 16px; display: flex; flex-direction: column; gap: 12px"><span class="ty-label" style="font-size: 15px">Тема</span>{seg(['<span class="ms xs">light_mode</span>Светлая', '<span class="ms xs">dark_mode</span>Тёмная', '<span class="ms xs">contrast</span>Авто'], 2)}</div>
<i style="height: 1px; background: var(--sf-high); margin: 0 16px"></i>
{row('bolt', 'Чистый чёрный (OLED)', 'В тёмной теме, экономит заряд', switch(False))}
<i style="height: 1px; background: var(--sf-high); margin: 0 16px"></i>
<div style="padding: 14px 16px 16px; display: flex; flex-direction: column; gap: 12px"><span class="ty-label" style="font-size: 15px">Плотность</span>{seg(['Компактно', 'Обычно', 'Просторно'], 1)}</div>
</div>
<div style="border-radius: 24px; background: var(--sf-lowest); overflow: hidden">
{row('palette', 'Цвета', 'Свои у каждого пространства', '<span class="ms s" style="color: var(--on-sf-v)">chevron_right</span>')}
</div>
</div>
{handle()}'''
    board('V4-Appearance.dc.html', 'Vola v4 — внешний вид', 'w-work t-light', body, style='background: var(--sf-c)')


if __name__ == '__main__':
    import sys
    if 'appearance' in sys.argv[1:]:
        appearance()
