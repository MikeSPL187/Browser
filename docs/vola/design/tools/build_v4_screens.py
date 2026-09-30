"""Vola v4 screens: every canvas board rebuilt on the v4 language (frame chrome by default).

Boards are written as canvas/W-<Name>.dc.html. Run with run_screens.py [group ...].
"""
import sys

from build_v4 import (BAIKAL, BAIKAL_T, ESS, TABS, address_body, address_bar, article, board, doc_page,
                      forecast_page, handle, keyboard, kino, mini, status, tab_card, watchlist_page)

# ---------------------------------------------------------------- components

def ms(name, cls='', style=''):
    return f'<span class="ms {cls}" style="{style}">{name}</span>'


def frame_screen(content, bar_html, collapsed=False, card_bottom=None):
    """Page card inside the workspace frame plus the bottom bar area."""
    bottom = card_bottom if card_bottom is not None else (58 if collapsed else 92)
    return f'''{status()}
<div class="page-card" style="top: 40px; bottom: {bottom}px">{content}</div>
{bar_html}
{handle()}'''


def page_bar(domain='north-guide.ru', ws_icon='work', tabs=4, action='menu_book'):
    inner = (address_bar(domain, ws_icon=ws_icon).replace('>menu_book<', f'>{action}<')
             .replace('tabcount">4', f'tabcount">{tabs}'))
    return f'<div style="position: absolute; left: 8px; right: 8px; bottom: 28px; height: 56px; display: flex; align-items: center; gap: 4px">{inner}</div>'


def capsule(domain='north-guide.ru'):
    return f'''<button aria-label="Показать панель" style="position: absolute; left: 50%; bottom: 20px; transform: translateX(-50%); height: 32px; border-radius: 16px; padding: 0 14px; background: color-mix(in srgb, var(--sf-lowest) 82%, transparent); box-shadow: var(--e1); display: flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 600; white-space: nowrap">{ms('lock', 'xs', 'font-size: 15px; color: var(--on-sf-v)')}{domain}</button>'''


def scrim():
    return '<div aria-hidden="true" style="position: absolute; inset: 0; background: rgba(8, 14, 16, 0.42)"></div>'


def sheet(inner, pad='0 16px 30px', gap=14, top=None):
    pos = f'top: {top}px;' if top is not None else ''
    return f'''<div style="position: absolute; left: 0; right: 0; bottom: 0; {pos} border-radius: 32px 32px 0 0; background: var(--sf-low); box-shadow: 0 -10px 40px rgba(8, 14, 16, 0.18); padding: {pad}; display: flex; flex-direction: column; gap: {gap}px; z-index: 5">
<i style="width: 36px; height: 4px; border-radius: 2px; background: var(--ol-v); margin: 10px auto 2px"></i>
{inner}
</div>'''


def topbar(title='', actions=(), back=True, top=44):
    acts = ''.join(f'<button class="ib4" aria-label="{a[1]}" style="color: var(--on-sf)">{ms(a[0])}</button>' for a in actions)
    b = f'<button class="ib4" aria-label="Назад" style="color: var(--on-sf)">{ms("arrow_back")}</button>' if back else '<span style="width: 12px"></span>'
    t = f'<span class="ty-title-l" style="flex-grow: 1; min-width: 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis">{title}</span>' if title else '<span style="flex-grow: 1"></span>'
    return f'<div style="position: absolute; left: 8px; right: 8px; top: {top}px; height: 56px; display: flex; align-items: center; gap: 4px; z-index: 3">{b}{t}{acts}</div>'


def switch(on):
    return (f'<span aria-hidden="true" style="width: 52px; height: 32px; border-radius: 16px; background: {"var(--pri)" if on else "var(--sf-highest)"}; box-shadow: {"none" if on else "inset 0 0 0 2px var(--ol)"}; display: flex; align-items: center; justify-content: {"flex-end" if on else "flex-start"}; padding: 0 {"4px" if on else "7px"}; flex-shrink: 0">'
            f'<i style="width: {24 if on else 16}px; height: {24 if on else 16}px; border-radius: 12px; background: {"var(--on-pri)" if on else "var(--ol)"}; display: flex; align-items: center; justify-content: center">{ms("check", "", "font-size: 16px; color: var(--pri)") if on else ""}</i></span>')


CHEV = '<span class="ms s" style="color: var(--on-sf-v)">chevron_right</span>'


def value(text):
    return f'<span class="ty-cap" style="font-size: 13.5px; font-weight: 600; white-space: nowrap">{text}</span>'


def lead(icon, tone=None, ink=None, fill=False):
    if tone:
        return f'<span style="width: 40px; height: 40px; border-radius: 14px; background: {tone}; color: {ink}; display: flex; align-items: center; justify-content: center; flex-shrink: 0">{ms(icon, "s f" if fill else "s")}</span>'
    return ms(icon, '', 'color: var(--on-sf-v); width: 24px')


def row(icon, title, sub='', trail='', tone=None, ink=None, danger=False, h=64, fill=False):
    color = 'color: var(--err);' if danger else ''
    icon_html = lead(icon, tone, ink, fill) if icon else ''
    sub_html = f'<span class="ty-cap">{sub}</span>' if sub else ''
    return f'''<div style="min-height: {h}px; display: flex; align-items: center; gap: 16px; padding: 10px 16px; {color}">
{icon_html}
<span style="flex-grow: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px"><span class="ty-label" style="font-size: 15px; {color}">{title}</span>{sub_html}</span>
{trail}
</div>'''


def group(rows, bg='var(--card)'):
    sep = '<i style="height: 1px; background: var(--sf-high); margin: 0 16px 0 56px"></i>'
    return f'<div style="border-radius: 24px; background: {bg}; overflow: hidden">{sep.join(rows)}</div>'


def section(title):
    return f'<span class="ty-over" style="padding: 8px 4px 0">{title}</span>'


def seg(options, selected):
    return '<div class="bgroup4" role="radiogroup">' + ''.join(
        f'<button role="radio" aria-checked="{"true" if k == selected else "false"}" class="{"sel" if k == selected else ""}">{o}</button>' for k, o in enumerate(options)) + '</div>'


def chip(label, on=False, icon=''):
    ic = ms(icon, 'xs') if icon else ''
    if on:
        return f'<button class="chip" style="background: var(--sec-c); color: var(--on-sec-c); box-shadow: none">{ms("check", "xs")}{label}</button>'
    return f'<button class="chip">{ic}{label}</button>'


def searchfield(ph, icon='search'):
    return f'<label style="height: 52px; border-radius: 26px; background: var(--sf-high); display: flex; align-items: center; gap: 12px; padding: 0 18px; color: var(--on-sf-v); font-size: 16px; font-weight: 500">{ms(icon, "s")}{ph}</label>'


def fav(letter, bg, ink, size=40, radius=13, fs=16):
    return f'<span class="fav" style="width: {size}px; height: {size}px; border-radius: {radius}px; background: {bg}; color: {ink}; font-size: {fs}px">{letter}</span>'


def list_item(letter, bg, ink, title, sub, trail=''):
    return f'''<div style="min-height: 64px; display: flex; align-items: center; gap: 14px; padding: 10px 8px 10px 14px">
{fav(letter, bg, ink)}
<span style="flex-grow: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px"><span class="ty-label" style="font-size: 15px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis">{title}</span><span class="ty-cap" style="white-space: nowrap; overflow: hidden; text-overflow: ellipsis">{sub}</span></span>
{trail}
</div>'''


def fab(icon, label='', bottom=28):
    lab = f'<span style="font-size: 15px; font-weight: 600">{label}</span>' if label else ''
    pad = '0 22px 0 18px' if label else '0'
    w = '' if label else 'width: 60px;'
    return f'<button style="position: absolute; right: 16px; bottom: {bottom}px; height: 60px; {w} border-radius: 20px; padding: {pad}; background: var(--pri-c); color: var(--on-pri-c); display: flex; align-items: center; justify-content: center; gap: 10px; box-shadow: var(--e2); z-index: 4">{ms(icon)}{lab}</button>'


def fade(h=140):
    return f'<div aria-hidden="true" style="position: absolute; left: 0; right: 0; bottom: 0; height: {h}px; background: linear-gradient(180deg, transparent 0%, var(--sf) 70%); z-index: 3"></div>'


def screen(content_top=108, inner='', gap=12, pad=16):
    return f'<div style="position: absolute; left: {pad}px; right: {pad}px; top: {content_top}px; display: flex; flex-direction: column; gap: {gap}px">{inner}</div>'


def big_title(title, sub='', top=100):
    s = f'<span class="ty-cap" style="font-size: 14px; line-height: 20px">{sub}</span>' if sub else ''
    return f'<div style="position: absolute; left: 20px; right: 20px; top: {top}px; display: flex; flex-direction: column; gap: 6px"><span class="ty-display">{title}</span>{s}</div>'


def btn(label, kind='fill', icon='', grow=False, h=52):
    ic = ms(icon, 's') if icon else ''
    g = 'flex: 1 1 0;' if grow else ''
    return f'<button class="btn {kind}" style="height: {h}px; border-radius: {h // 2}px; {g}">{ic}{label}</button>'


def W(name, title, root, body, **kw):
    board(f'W-{name}.dc.html', f'Vola v4 — {title}', root, body, **kw)


# ---------------------------------------------------------------- browse

def article_long(hl=None):
    """The north-guide article scrolled to the routes section; hl(word, current) marks find hits."""
    h = hl or (lambda w, cur=False: w)
    p = lambda t: f'<p style="font-family: Literata, Georgia, serif; font-size: 17px; line-height: 28px; color: #2B3432">{t}</p>'
    h2 = lambda t: f'<h2 style="font-size: 20px; line-height: 26px; font-weight: 700; color: #15201D; letter-spacing: -0.01em; margin-top: 4px">{t}</h2>'
    return f'''<div style="position: absolute; inset: 0; background: #FFFFFF; padding: 18px 20px 0; display: flex; flex-direction: column; gap: 14px">
{p('Ниже — три маршрута разной длины: от короткой прогулки к гроту до двухдневного перехода вдоль берега.')}
{h2('Маршрут 1. Листвянка — Большие Коты')}
{p(f'Около 20 км вдоль берега. Большую часть пути видно посёлок и дорогу, поэтому маршрут подходит для первого выхода на {h("лёд")}.')}
<div style="border-radius: 16px; background: #EEF5F3; box-shadow: inset 3px 0 0 #2F6B5F; padding: 12px 14px; font-size: 14px; line-height: 21px; color: #2B4A43">Перед выходом проверьте прогноз толщины {h("льда")} и сообщите маршрут знакомым.</div>
{h2('Маршрут 2. Ольхон — мыс Хобой')}
{p(f'Ледяные гроты, сокуи и прозрачный {h("лёд", True)} у северной оконечности острова. Лучшее время — утро.')}
{p(f'Дорога от Хужира занимает около двух часов. На мысе ветер сильнее, чем в посёлке, поэтому возьмите маску и запасные перчатки.')}
{h2('Что взять с собой')}
{p(f'Ледоступы, термос и пауэрбанк: на морозе телефон разряжается быстрее. Солнечные очки обязательны — отражённый от {h("льда")} свет слепит.')}
</div>'''


def g_browse():
    W('Main', 'страница', 'w-work t-light aura', frame_screen(article(), page_bar()))
    # Scrolled: content moved up, bar collapsed to a capsule.
    W('Scrolled', 'прокрутка', 'w-work t-light aura', frame_screen(article_long() + '<div aria-hidden="true" style="position: absolute; right: 3px; top: 120px; width: 4px; height: 90px; border-radius: 2px; background: rgba(0,0,0,0.22)"></div>', capsule(), collapsed=True))
    W('Editing', 'ввод адреса', 'w-work t-light aura', address_body('A'))
    # Find in page: highlights on the article, find bar replaces the address bar.
    hl = lambda w, cur=False: f'<mark style="background: {"var(--pri)" if cur else "color-mix(in srgb, var(--pri-c) 80%, transparent)"}; color: {"var(--on-pri)" if cur else "inherit"}; border-radius: 4px; padding: 0 2px">{w}</mark>'
    find_page = article_long(hl)
    find_bar = f'''<div style="position: absolute; left: 8px; right: 8px; bottom: 76px; display: flex; gap: 8px; z-index: 4">{chip('Регистр')}{chip('Слово целиком')}{chip('ё = е', True)}</div>
<div style="position: absolute; left: 8px; right: 8px; bottom: 22px; height: 56px; border-radius: 28px; background: var(--card); box-shadow: 0 0 0 2px var(--pri), var(--e2); display: flex; align-items: center; gap: 2px; padding: 0 4px 0 16px; z-index: 4">
{ms('search', 's', 'color: var(--on-sf-v)')}
<span style="flex-grow: 1; min-width: 0; display: flex; align-items: center; padding-left: 10px; font-size: 17px; font-weight: 600">лёд<i style="width: 2px; height: 22px; margin-left: 1px; border-radius: 1px; background: var(--pri)"></i></span>
<span class="ty-cap" style="font-size: 14px; font-weight: 600; padding: 0 6px; white-space: nowrap">2 из 7</span>
<button class="ib4" aria-label="Предыдущее" style="width: 44px; color: var(--on-sf)">{ms('keyboard_arrow_down', '', 'transform: rotate(180deg)')}</button>
<button class="ib4" aria-label="Следующее" style="width: 44px; color: var(--on-sf)">{ms('keyboard_arrow_down')}</button>
<button class="ib4" aria-label="Закрыть поиск" style="width: 44px; color: var(--on-sf)">{ms('close')}</button>
</div>'''
    marks = ''.join(f'<i style="position: absolute; right: 0; top: {t}px; width: 8px; height: 3px; border-radius: 2px; background: {"var(--pri)" if k == 1 else "color-mix(in srgb, var(--pri) 45%, transparent)"}"></i>' for k, t in enumerate((120, 250, 300, 410, 520, 560, 620)))
    W('Find', 'поиск на странице', 'w-work t-light aura', frame_screen(find_page + f'<div aria-hidden="true" style="position: absolute; right: 2px; top: 60px; bottom: 10px; width: 8px">{marks}</div>', find_bar, card_bottom=128))
    # Reader view with the reading sheet.
    paper = '#F7F1E6'
    reader_page = f'''<div style="position: absolute; inset: 0; background: {paper}; padding: 16px 24px; display: flex; flex-direction: column; gap: 14px">
<span style="display: flex; align-items: center; gap: 8px; font-size: 13px; color: #6C5F4B"><span class="fav" style="width: 22px; height: 22px; border-radius: 7px; background: #2F6B5F; color: #FFFFFF; font-size: 11px">С</span><span style="flex-grow: 1">north-guide.ru · 12 минут</span><button aria-label="Выйти из режима чтения" style="width: 40px; height: 40px; border-radius: 20px; background: rgba(60, 50, 30, 0.08); color: #3B3226; display: flex; align-items: center; justify-content: center; margin-right: -8px"><span class="ms s">close</span></button></span>
<h1 style="font-family: Literata, Georgia, serif; font-size: 30px; line-height: 37px; font-weight: 600; color: #2A2217">Байкал зимой: как выбрать маршрут по льду</h1>
<p style="font-family: Literata, Georgia, serif; font-size: 18px; line-height: 30px; color: #3B3226">Лёд на Байкале становится крепким к середине февраля. До этого лучше выбирать прогулки у берега и маршруты с проводником.</p>
<p style="font-family: Literata, Georgia, serif; font-size: 18px; line-height: 30px; color: #3B3226">Ниже — три маршрута разной длины: от короткой прогулки к гроту до двухдневного перехода вдоль берега.</p>
</div>'''
    swatch = lambda bg, ink, name, on: f'<button role="radio" aria-checked="{"true" if on else "false"}" style="flex: 1 1 0; height: 64px; border-radius: 18px; background: {bg}; color: {ink}; box-shadow: {"inset 0 0 0 2.5px var(--pri)" if on else "inset 0 0 0 1px rgba(0,0,0,0.08)"}; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 2px"><span style="font-family: Literata, Georgia, serif; font-size: 20px; font-weight: 600">Аа</span><span style="font-size: 11.5px; font-weight: 600">{name}</span></button>'
    reader_sheet = sheet(f'''<div style="display: flex; align-items: center; justify-content: space-between; padding: 0 4px"><span class="ty-title">Вид для чтения</span>{btn('Слушать', 'tonal', 'headphones', h=40)}</div>
<div style="display: flex; gap: 8px">{swatch('#FFFFFF', '#1C1B1F', 'Светлый', False)}{swatch('#F7F1E6', '#3B3226', 'Бумага', True)}{swatch('#2A2723', '#E9E1D3', 'Тёмный', False)}{swatch('#000000', '#E6E1E5', 'Чёрный', False)}</div>
<div style="display: flex; align-items: center; gap: 14px; padding: 0 6px"><span style="font-family: Literata, Georgia, serif; font-size: 14px">А</span><span style="position: relative; flex-grow: 1; height: 16px; display: flex; align-items: center"><i style="flex-grow: 1; height: 16px; border-radius: 8px; background: linear-gradient(90deg, var(--pri) 0 55%, var(--sf-highest) 55% 100%)"></i><i style="position: absolute; left: 55%; width: 4px; height: 28px; margin-left: 2px; border-radius: 2px; background: var(--pri)"></i></span><span style="font-family: Literata, Georgia, serif; font-size: 22px">А</span></div>
{seg(['С засечками', 'Без засечек'], 0)}
{group([row('format_size', 'Широкие поля', '', switch(True), h=56)])}''')
    W('Reader', 'режим чтения', 'w-work t-light aura', f'{status()}<div class="page-card" style="top: 40px; bottom: 0; border-radius: 26px 26px 0 0">{reader_page}</div>{reader_sheet}{handle()}')



# ---------------------------------------------------------------- tabs & workspaces

def tabs_backdrop():
    """Blurred tab overview used behind sheets and menus."""
    cards = ''.join('<i style="height: 248px; border-radius: 22px; background: var(--card)"></i>' for _ in range(4))
    return ('<div aria-hidden="true" style="position: absolute; inset: 0; filter: blur(8px); opacity: 0.8">'
            '<div style="position: absolute; left: 20px; top: 52px; width: 150px; height: 32px; border-radius: 10px; background: color-mix(in srgb, var(--on-sf) 70%, transparent)"></div>'
            f'<div style="position: absolute; left: 16px; right: 16px; top: 120px; display: grid; grid-template-columns: 1fr 1fr; gap: 12px">{cards}</div></div>')


WS_COLORS = [('#5E4EB7', False), ('#2F5BD3', False), ('#006877', True), ('#2B6C3F', False), ('#7C5800', False), ('#A23F2B', False), ('#A0305B', False), ('#55595F', False)]


def color_dots():
    out = []
    for c, on in WS_COLORS:
        ring = f'inset 0 0 0 2.5px {c}' if on else 'none'
        size = 28 if on else 32
        out.append(f'<button role="radio" aria-checked="{"true" if on else "false"}" style="width: 40px; height: 40px; border-radius: 20px; display: flex; align-items: center; justify-content: center; box-shadow: {ring}"><i style="width: {size}px; height: {size}px; border-radius: 16px; background: {c}"></i></button>')
    return '<div style="display: flex; justify-content: space-between">' + ''.join(out) + '</div>'


def gem(icon, size=56, radius=20, cls='l'):
    return f'<span class="gem" style="width: {size}px; height: {size}px; border-radius: {radius}px">{ms(icon, "f " + cls)}</span>'


def g_tabs():
    from build_v4 import tabs_body
    W('Tabs', 'обзор вкладок', 'w-work t-light aura', tabs_body('A'))
    card = tab_card(TABS[3], current=True).replace('height: 248px', 'height: 300px')
    quick = ''.join(f'<button style="height: 72px; border-radius: 20px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6px; color: var(--on-sf); font-size: 12.5px; font-weight: 600">{ms(i)}{t}</button>'
                    for i, t in (('star', 'В Essentials'), ('content_copy', 'Дублировать'), ('splitscreen', 'Рядом'), ('share', 'Отправить')))
    menu = ('<div style="position: absolute; left: 20px; right: 20px; top: 452px; border-radius: 28px; background: var(--card); box-shadow: var(--e3); padding: 6px; z-index: 5">'
            f'<div style="display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 2px; padding: 2px 0 6px; border-bottom: 1px solid var(--sf-high)">{quick}</div>'
            + row('workspaces', 'Переместить', '', value('Работа') + CHEV, h=52)
            + row('tab_group', 'Добавить в группу', '', CHEV, h=52)
            + row('snooze', 'Отложить', '', value('до завтра') + CHEV, h=52)
            + row('close', 'Закрыть вкладку', '', '', danger=True, h=52) + '</div>')
    W('TabActions', 'действия с вкладкой', 'w-work t-light aura',
      tabs_backdrop() + '<div aria-hidden="true" style="position: absolute; inset: 0; background: color-mix(in srgb, var(--sf-c) 45%, transparent)"></div>' + status()
      + f'<div style="position: absolute; left: 50%; top: 64px; width: 240px; margin-left: -120px; z-index: 4">{card}</div>' + menu + handle())

    work = ''.join(tab_card(t) for t in (TABS[2], TABS[3]))
    anime_tabs = [('Обзор первой серии', 'К', '#FFE1D6', '#9A3A1E', 'video'), ('Список к просмотру', 'С', '#F1DEFA', '#6B2C8C', 'list'),
                  ('Расписание сезона', 'Р', '#D8EEFF', '#1E5E8C', 'cal'), ('Форум: третья серия', 'Ф', '#FFE9B8', '#7C5800', 'doc')]
    anime = ''.join(tab_card(t) for t in anime_tabs)

    def col(cls, title, icon, grid, op):
        return (f'<div class="{cls}" style="width: 358px; flex-shrink: 0; display: flex; flex-direction: column; gap: 14px; opacity: {op}">'
                f'<div style="display: flex; align-items: center; gap: 10px; height: 44px">{gem(icon, 40, 14, "s")}<span class="ty-head">{title}</span></div>'
                f'<div style="display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px">{grid}</div></div>')
    small = lambda cls, icon: f'<span class="{cls}" style="width: 48px; height: 48px; display: flex; align-items: center; justify-content: center">{gem(icon, 32, 11, "xs")}</span>'
    body = ('<div aria-hidden="true" class="w-anime t-light aura" style="position: absolute; inset: 0; -webkit-mask-image: linear-gradient(90deg, transparent 8%, #000 58%); mask-image: linear-gradient(90deg, transparent 8%, #000 58%)"></div>' + status()
            + f'<div style="position: absolute; left: -238px; top: 64px; display: flex; gap: 24px">{col("w-work t-light", "Работа", "work", work, 0.55)}{col("w-anime t-light", "Аниме", "movie", anime, 1)}</div>'
            + '<div style="position: absolute; left: 16px; right: 16px; top: 690px; display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 10px">' + ''.join(f'<span style="height: 56px; border-radius: 18px; background: color-mix(in srgb, #FFFFFF 82%, transparent); box-shadow: var(--e1); display: flex; align-items: center; justify-content: center">{fav(l, b, i, 30, 10, 14)}</span>' for l, b, i in (('К', '#FFE1D6', '#9A3A1E'), ('С', '#F1DEFA', '#6B2C8C'), ('Р', '#D8EEFF', '#1E5E8C'), ('Ф', '#FFE9B8', '#7C5800'), ('Т', '#D4F1EC', '#00665A'))) + '</div>'
            + '<div class="w-anime t-light" style="position: absolute; left: 12px; right: 12px; bottom: 26px; display: flex; align-items: center; gap: 10px">'
            + '<div style="flex-grow: 1; height: 56px; border-radius: 28px; background: color-mix(in srgb, var(--sf-lowest) 70%, transparent); box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--on-sf) 6%, transparent); display: flex; align-items: center; gap: 2px; padding: 0 4px">'
            + small('w-work t-light', 'work')
            + f'<span style="height: 48px; border-radius: 24px; background: var(--card); box-shadow: var(--e1); display: flex; align-items: center; gap: 8px; padding: 0 14px 0 4px">{gem("movie", 40, 14, "s")}<span style="font-size: 15px; font-weight: 600">Аниме</span></span>'
            + small('w-personal t-light', 'home') + '</div>'
            + f'<span style="width: 56px; height: 56px; border-radius: 20px; background: var(--pri); color: var(--on-pri); display: flex; align-items: center; justify-content: center">{ms("add", "l")}</span></div>'
            + handle())
    W('WorkspaceSwipe', 'смена пространства свайпом', 'w-work t-light aura', body)

    icons = ['work', 'home', 'movie', 'menu_book', 'star', 'palette', 'public', 'bolt', 'photo_camera', 'person', 'bookmark', 'travel_explore']
    ic = ''.join(f'<button role="radio" aria-checked="{"true" if k == 0 else "false"}" style="height: 48px; border-radius: {"16px" if k == 0 else "24px"}; background: {"var(--pri)" if k == 0 else "var(--sf-high)"}; color: {"var(--on-pri)" if k == 0 else "var(--on-sf-v)"}; display: flex; align-items: center; justify-content: center">{ms(n, "f s" if k == 0 else "s")}</button>' for k, n in enumerate(icons))
    head = lambda title, sub, trail='': f'<div style="display: flex; align-items: center; gap: 14px; padding: 4px 4px 0">{gem("work")}<span style="flex-grow: 1; display: flex; flex-direction: column; gap: 2px"><span class="ty-title-l">{title}</span><span class="ty-cap">{sub}</span></span>{trail}</div>'
    new_ws = sheet(head('Новое пространство', 'Свои вкладки, Essentials и цвет')
                   + '<label style="display: flex; flex-direction: column; gap: 6px"><span class="ty-label" style="padding: 0 4px">Название</span><span style="height: 56px; border-radius: 16px; background: var(--card); box-shadow: inset 0 0 0 2px var(--pri); display: flex; align-items: center; padding: 0 16px; font-size: 17px; font-weight: 600">Работа<i style="width: 2px; height: 22px; margin-left: 1px; background: var(--pri)"></i></span></label>'
                   + '<span class="ty-label" style="padding: 0 4px">Цвет</span>' + color_dots()
                   + f'<span class="ty-label" style="padding: 0 4px">Значок</span><div style="display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 8px">{ic}</div>'
                   + group([row('shield_lock', 'Отдельное хранилище', 'Свои cookie, входы и данные сайтов', switch(True), h=60),
                            row('fingerprint', 'Вход по биометрии', 'Вкладки скрыты, пока вы не войдёте', switch(False), h=60)])
                   + btn('Создать пространство', 'fill', '', h=56), gap=12)
    W('WorkspaceSheet', 'новое пространство', 'w-work t-light aura', tabs_backdrop() + status() + scrim() + new_ws + handle())

    edit = f'<button class="ib4" aria-label="Переименовать" style="background: var(--card); color: var(--on-sf)">{ms("edit")}</button>'
    ws_set = sheet(head('Работа', '6 вкладок · 5 в Essentials', edit) + color_dots()
                   + group([row('star', 'Essentials', '5 сайтов · свои для пространства', CHEV, h=60), row('palette', 'Фон и значок', 'Градиент из цвета пространства', CHEV, h=60),
                            row('search', 'Поиск по умолчанию', 'Как в браузере · DuckDuckGo', CHEV, h=60), row('shield', 'Защита', 'Строгая блокировка трекеров', CHEV, h=60)])
                   + group([row('shield_lock', 'Отдельное хранилище', 'Свои cookie и входы', switch(True), h=60), row('fingerprint', 'Вход по биометрии', 'После 5 минут в фоне', switch(True), h=60)])
                   + f'<button class="btn out" style="height: 52px; border-radius: 26px; color: var(--err)">{ms("delete", "s")}Удалить пространство</button>', gap=12)
    W('WorkspaceSettings', 'настройки пространства', 'w-work t-light aura', tabs_backdrop() + status() + scrim() + ws_set + handle())

# ---------------------------------------------------------------- new tab

ESS8 = [('Почта', 'П', '#DDE3FF', '#2F4AA8'), ('Календарь', 'К', '#D4F1EC', '#00665A'), ('Задачи', 'З', '#FFE1D6', '#9A3A1E'), ('Документы', 'Д', '#E6DEFF', '#4A3A9E'),
        ('Код', 'К', '#E2E2E6', '#303036'), ('Чат', 'Ч', '#D8EEFF', '#1E5E8C'), ('Дизайн', 'Д', '#FFDDEA', '#962F59'), ('Отчёты', 'О', '#FFEBC2', '#6E4F00')]


def ess_tile(name, l, bg, ink, badge=False):
    b = f'<span aria-label="Убрать" style="position: absolute; left: -4px; top: -4px; width: 24px; height: 24px; border-radius: 12px; background: var(--inv-sf); color: var(--inv-on-sf); display: flex; align-items: center; justify-content: center; box-shadow: 0 0 0 2px var(--sf-c)">{ms("close", "", "font-size: 15px")}</span>' if badge else ''
    return (f'<a href="#" style="display: flex; flex-direction: column; align-items: center; gap: 8px">'
            f'<span style="position: relative; width: 68px; height: 68px; border-radius: 24px; background: color-mix(in srgb, var(--sf-lowest) 86%, transparent); box-shadow: var(--e1); display: flex; align-items: center; justify-content: center">{fav(l, bg, ink, 36, 12, 16)}{b}</span>'
            f'<span style="font-size: 12.5px; font-weight: 600; color: var(--on-sf)">{name}</span></a>')


def newtab_bar(placeholder='Поиск или адрес', ws_icon='work'):
    return f'''<div style="position: absolute; left: 8px; right: 8px; bottom: 28px; height: 56px; display: flex; align-items: center; gap: 4px">
<button aria-label="Пространство" style="width: 48px; height: 48px; display: flex; align-items: center; justify-content: center">{gem(ws_icon, 40, 14, "s")}</button>
<button style="flex-grow: 1; min-width: 0; height: 48px; border-radius: 24px; background: color-mix(in srgb, var(--sf-lowest) 86%, transparent); box-shadow: var(--e1); display: flex; align-items: center; gap: 10px; padding: 0 6px 0 16px; color: var(--on-sf-v); font-size: 16px; font-weight: 500; white-space: nowrap">{ms("search", "s")}<span style="flex-grow: 1; text-align: left">{placeholder}</span><span class="ib4" style="width: 40px; height: 40px; color: var(--on-sf)">{ms("mic", "s")}</span></button>
<button class="ib4" aria-label="Вкладки" style="width: 44px"><span class="tabcount">4</span></button>
<button class="ib4" aria-label="Меню" style="width: 40px; color: var(--on-sf)">{ms("more_vert")}</button>
</div>'''


def g_newtab():
    ess = ''.join(ess_tile(*e) for e in ESS8)
    def thumb(kind, brand, tone):
        return (f'<span style="position: relative; width: 56px; height: 64px; border-radius: 12px; overflow: hidden; flex-shrink: 0; box-shadow: 0 0 0 1px var(--sf-high)">'
                f'<span style="position: absolute; left: 0; top: 0; width: 160px; height: 184px; transform: scale(0.35); transform-origin: 0 0">{mini(kind, brand, tone)}</span></span>')

    def cont_item(kind, brand, tone, title, sub, trail):
        return (f'<div style="min-height: 72px; display: flex; align-items: center; gap: 14px; padding: 8px 8px 8px 12px">{thumb(kind, brand, tone)}'
                f'<span style="flex-grow: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px"><span class="ty-label" style="font-size: 15px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis">{title}</span><span class="ty-cap">{sub}</span></span>{trail}</div>')
    sep = '<i style="height: 1px; background: var(--sf-high); margin: 0 14px 0 82px"></i>'
    cont = sep.join([
        cont_item('doc', '#4A3A9E', '#E6DEFF', 'План запуска на октябрь', 'Закрыта 20 минут назад', f'<button class="ib4" aria-label="Вернуть" style="width: 40px; height: 40px">{ms("undo", "s")}</button>'),
        cont_item('chart', '#6E4F00', '#FFEBC2', 'Отчёт по метрикам', 'С планшета · 2 часа назад', f'<span class="ib4" style="width: 40px; height: 40px">{ms("devices", "s")}</span>'),
        cont_item('board', '#9A3A1E', '#FFE1D6', 'Спринт 42 · доска задач', 'Отложена до 18:00', f'<span class="ib4" style="width: 40px; height: 40px">{ms("snooze", "s")}</span>')])
    body = f'''{status()}
<div style="position: absolute; left: 16px; right: 16px; top: 52px; height: 48px; display: flex; align-items: center; gap: 10px"><span class="ty-head" style="flex-grow: 1">Работа</span><span class="ty-cap" style="font-size: 13.5px">Вторник, 30 сентября</span></div>
<div style="position: absolute; left: 16px; right: 16px; top: 112px; display: flex; flex-direction: column; gap: 20px">
<div style="display: flex; flex-direction: column; gap: 12px"><div style="display: flex; align-items: center; justify-content: space-between; padding: 0 4px"><span class="ty-over">Essentials</span><button style="font-size: 13.5px; font-weight: 600; color: var(--pri)">Изменить</button></div>
<div style="display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 18px 8px">{ess}</div></div>
<div style="display: flex; flex-direction: column; gap: 10px"><span class="ty-over" style="padding: 0 4px">Продолжить</span><div style="border-radius: 24px; background: color-mix(in srgb, var(--sf-lowest) 86%, transparent); box-shadow: var(--e1); overflow: hidden">{cont}</div></div>
<a href="#" style="border-radius: 24px; background: color-mix(in srgb, var(--pri-c) 55%, transparent); padding: 14px 12px 14px 14px; display: flex; align-items: center; gap: 14px"><span style="width: 44px; height: 44px; border-radius: 15px; background: var(--pri); color: var(--on-pri); display: flex; align-items: center; justify-content: center">{ms("gpp_good", "f")}</span><span style="flex-grow: 1; display: flex; flex-direction: column; gap: 2px"><span class="ty-label" style="font-size: 15px">1 284 трекера за неделю</span><span class="ty-cap" style="color: var(--on-pri-c)">Заблокированы на 96 сайтах</span></span>{CHEV}</a>
</div>
{newtab_bar()}
{handle()}'''
    W('NewTab', 'новая вкладка', 'w-work t-light aura', body)

    edit = ''.join(ess_tile(*e, badge=True) for e in ESS8[:7]) + (
        '<a href="#" style="display: flex; flex-direction: column; align-items: center; gap: 8px"><span style="width: 68px; height: 68px; border-radius: 24px; box-shadow: inset 0 0 0 2px var(--pri); color: var(--pri); display: flex; align-items: center; justify-content: center">'
        + ms('add', 'l') + '</span><span style="font-size: 12.5px; font-weight: 600; color: var(--pri)">Добавить</span></a>')
    add_rows = (list_item('О', '#FFEBC2', '#6E4F00', 'Отчёт по метрикам', 'metrics.example.com', f'<button class="ib4" aria-label="Добавить" style="background: var(--sec-c); color: var(--on-sec-c)">{ms("add")}</button>')
                + list_item('З', '#FFE1D6', '#9A3A1E', 'Спринт 42 · доска задач', 'tasks.example.com', f'<button class="ib4" aria-label="Добавить" style="background: var(--sec-c); color: var(--on-sec-c)">{ms("add")}</button>'))
    add_sheet = sheet(f'<span class="ty-title" style="padding: 0 4px">Добавить из открытых вкладок</span><div style="border-radius: 24px; background: var(--card); overflow: hidden">{add_rows}</div>', gap=12)
    body = f'''{status()}
<div style="position: absolute; left: 20px; right: 16px; top: 52px; display: flex; align-items: center; gap: 12px"><span style="flex-grow: 1; display: flex; flex-direction: column; gap: 2px"><span class="ty-head">Essentials</span><span class="ty-cap" style="font-size: 13.5px">Перетащите, чтобы поменять порядок</span></span>{btn("Готово", "fill", "", h=44)}</div>
<div style="position: absolute; left: 16px; right: 16px; top: 140px; display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 18px 8px">{edit}</div>
<div style="position: absolute; left: 16px; right: 16px; top: 352px">{group([row("layers", "Общие для всех пространств", "Иначе у каждого пространства свои", switch(False)), row("grid_view", "Показывать в обзоре вкладок", "Ряд значков над панелью", switch(True))])}</div>
{add_sheet}{handle()}'''
    W('EssentialsEdit', 'правка Essentials', 'w-work t-light aura', body)

    facts = [('history', 'Без истории и cookie', 'Адреса, формы и входы не сохраняются'), ('description', 'Ничего на диске', 'Кэш и данные сайтов живут только в памяти'),
             ('gpp_good', 'Строгая защита', 'Трекеры и отпечатки блокируются всегда'), ('download', 'Загрузки остаются', 'Скачанные файлы и Essentials сохраняются')]
    fl = ''.join(f'<div style="display: flex; gap: 14px; align-items: flex-start"><span style="width: 40px; height: 40px; border-radius: 14px; background: var(--sf-high); color: var(--pri); display: flex; align-items: center; justify-content: center; flex-shrink: 0">{ms(i, "s")}</span><span style="display: flex; flex-direction: column; gap: 2px; padding-top: 2px"><span class="ty-label" style="font-size: 15px">{t}</span><span class="ty-cap">{d}</span></span></div>' for i, t, d in facts)
    body = f'''{status()}
<div style="position: absolute; left: 24px; right: 24px; top: 96px; display: flex; flex-direction: column; gap: 20px">
<span class="gem priv" style="width: 72px; height: 72px; border-radius: 26px; box-shadow: 0 12px 32px rgba(106, 75, 216, 0.45)">{ms("domino_mask", "f l")}</span>
<span class="ty-display">Приватная вкладка</span>
<span class="ty-body" style="color: var(--on-sf-v)">Vola не сохранит, что вы делали здесь. Закроете вкладку — всё сотрётся.</span>
<div style="display: flex; flex-direction: column; gap: 14px; padding-top: 4px">{fl}</div>
</div>
<div style="position: absolute; left: 12px; right: 12px; bottom: 100px">{group([row("fingerprint", "Замок при выходе", "Вернуться к вкладкам — только по отпечатку", switch(True))], "var(--sf-c)")}</div>
{newtab_bar("Поиск без следов", "domino_mask").replace('class="gem"', 'class="gem priv"').replace('tabcount">4', 'tabcount">1')}
{handle()}'''
    W('PrivateTab', 'приватная вкладка', 'w-private t-dark', body, style='background: radial-gradient(120% 60% at 0% 0%, rgba(106, 75, 216, 0.35) 0%, transparent 70%), #000000')

# ---------------------------------------------------------------- menu & site

MAP = (__import__('pathlib').Path(__file__).resolve().parent / 'map.svg').read_text(encoding='utf-8')


def page_under(content=None):
    """The current page behind a sheet (frame chrome)."""
    return f'{status()}<div class="page-card" style="top: 40px; bottom: 92px">{content or article()}</div>{page_bar()}'


def conn_group(items):
    """M3 Expressive connected icon button group (first/last rounded)."""
    out = []
    n = len(items)
    for k, (icon, label, state) in enumerate(items):
        r = '26px 10px 10px 26px' if k == 0 else ('10px 26px 26px 10px' if k == n - 1 else '10px')
        bg = 'var(--sec-c)' if state == 'on' else 'var(--card)'
        fg = 'var(--on-sec-c)' if state == 'on' else ('var(--ol-v)' if state == 'off' else 'var(--on-sf)')
        fill = ' f' if state == 'on' else ''
        out.append(f'<button aria-label="{label}" style="flex: 1 1 0; height: 56px; border-radius: {r}; background: {bg}; color: {fg}; display: flex; align-items: center; justify-content: center">{ms(icon, fill.strip())}</button>')
    return '<div style="display: flex; gap: 3px">' + ''.join(out) + '</div>'


def tile(icon, label, on=False):
    bg = 'var(--sec-c)' if on else 'var(--card)'
    fg = 'var(--on-sec-c)' if on else 'var(--on-sf)'
    return (f'<button style="height: 84px; border-radius: 22px; background: {bg}; color: {fg}; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; padding: 0 4px">'
            f'{ms(icon, "f" if on else "")}<span style="font-size: 12px; line-height: 14px; font-weight: 600; text-align: center; height: 28px; display: flex; align-items: flex-start">{label}</span></button>')


def g_menu():
    nav = conn_group([('arrow_back', 'Назад', ''), ('arrow_forward', 'Вперёд', 'off'), ('refresh', 'Обновить', ''), ('star', 'В избранном', 'on'), ('share', 'Поделиться', '')])
    tiles = ''.join(tile(*t) for t in [('add', 'Новая вкладка'), ('domino_mask', 'Приватная'), ('search', 'Найти'), ('menu_book', 'Чтение'),
                                        ('translate', 'Перевести'), ('crop', 'Скриншот'), ('splitscreen', 'Split View'), ('fullscreen_exit', 'Компактно', True),
                                        ('desktop_windows', 'Версия для ПК'), ('picture_as_pdf', 'Сохранить PDF'), ('add_to_home_screen', 'На главный экран'), ('dark_mode', 'Тёмный сайт')])
    lst = group([row('download', 'Загрузки', '', value('1 идёт') + CHEV, h=52), row('history', 'История', '', CHEV, h=52), row('bookmarks', 'Избранное', '', CHEV, h=52),
                 row('key', 'Пароли', '', CHEV, h=52), row('extension', 'Расширения', '', value('3') + CHEV, h=52), row('settings', 'Настройки', '', CHEV, h=52)])
    menu = sheet(f'{nav}<div style="display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 6px">{tiles}</div>{lst}', gap=10, pad='0 12px 28px')
    W('Menu', 'главное меню', 'w-work t-light aura', page_under() + scrim() + menu + handle())
    W('MenuDark', 'главное меню (тёмная)', 'w-work t-dark aura', page_under(article(dark=True)) + scrim() + menu + handle())

    perm = lambda icon, label, state, bg, fg: row(icon, label, '', f'<span style="height: 34px; border-radius: 17px; padding: 0 10px 0 12px; background: {bg}; color: {fg}; display: flex; align-items: center; gap: 4px; font-size: 13px; font-weight: 600">{state}{ms("expand_more", "xs")}</span>', h=56)
    stat = lambda icon, n, label, tone, ink: f'<div style="flex: 1 1 0; border-radius: 22px; background: {tone}; color: {ink}; padding: 14px; display: flex; flex-direction: column; gap: 6px">{ms(icon, "f")}<span style="font-size: 28px; line-height: 30px; font-weight: 700">{n}</span><span style="font-size: 13px; font-weight: 600; opacity: 0.85">{label}</span></div>'
    site = sheet(f'''<div style="display: flex; align-items: center; gap: 14px; padding: 4px 4px 0">{fav('С', '#2F6B5F', '#FFFFFF', 52, 18, 22)}<span style="flex-grow: 1; display: flex; flex-direction: column; gap: 4px"><span class="ty-title-l">north-guide.ru</span><span style="display: flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 600; color: var(--ok)">{ms("lock", "xs f")}Соединение защищено</span></span></div>
<div style="display: flex; gap: 10px">{stat('gpp_good', '14', 'трекеров заблокировано', 'var(--pri-c)', 'var(--on-pri-c)')}{stat('cookie', '1', 'cookie-баннер скрыт', 'var(--sf-lowest)', 'var(--on-sf)')}</div>
{section('Разрешения сайта')}
{group([perm('location_on', 'Геолокация', 'Разрешено', 'var(--ok-c)', 'var(--on-ok-c)'), perm('photo_camera', 'Камера', 'Спрашивать', 'var(--sf-high)', 'var(--on-sf-v)'), perm('notifications', 'Уведомления', 'Запрещено', 'var(--err-c)', 'var(--on-err-c)')])}
{group([row('shield', 'Защита на этом сайте', 'Выключите, если сайт работает неправильно', switch(True), h=64), row('block', 'Блокировать всплывающие окна', '', switch(True), h=56)])}
{group([row('dns', 'Данные сайта', '2,4 МБ · 3 cookie', f'<button class="btn" style="height: 36px; border-radius: 18px; padding: 0 14px; background: var(--err-c); color: var(--on-err-c); font-size: 13px">{ms("delete", "xs")}Удалить</button>', h=64), row('verified', 'Сертификат', 'Let’s Encrypt · до 12 января', CHEV, h=64)])}''', gap=10, pad='0 14px 28px')
    W('SiteInfo', 'сведения о сайте', 'w-work t-light aura', page_under() + scrim() + site + handle())

    cats = [('Реклама', 6, '#1E6875'), ('Аналитика', 5, '#7D5700'), ('Соцсети', 2, '#A0305B'), ('Отпечатки', 1, '#2F5BD3')]
    total = sum(c[1] for c in cats)
    stops, acc = [], 0
    for _, n, color in cats:
        stops.append(f'{color} {acc / total * 100:.1f}% {(acc + n) / total * 100 - 0.8:.1f}%, transparent {(acc + n) / total * 100 - 0.8:.1f}% {(acc + n) / total * 100:.1f}%')
        acc += n
    donut = f'<span style="position: relative; width: 112px; height: 112px; border-radius: 56px; background: conic-gradient({", ".join(stops)}); flex-shrink: 0; display: flex; align-items: center; justify-content: center"><span style="width: 84px; height: 84px; border-radius: 42px; background: var(--card); display: flex; flex-direction: column; align-items: center; justify-content: center"><span style="font-size: 28px; font-weight: 700; line-height: 30px">14</span><span class="ty-cap" style="font-size: 11.5px">запросов</span></span></span>'
    bars = ''.join(f'<div style="display: flex; align-items: center; gap: 12px; height: 40px; padding: 0 16px"><i style="width: 10px; height: 10px; border-radius: 5px; background: {c}"></i><span class="ty-label" style="width: 96px">{name}</span><span style="flex-grow: 1; height: 8px; border-radius: 4px; background: var(--sf-high)"><i style="width: {n / 6 * 100:.0f}%; height: 8px; border-radius: 4px; background: {c}"></i></span><span class="ty-label" style="width: 18px; text-align: right">{n}</span></div>' for name, n, c in cats)
    hosts = [('ads.example-network.com', 'Реклама', 4), ('metrics.example.net', 'Аналитика', 3), ('pixel.example-social.com', 'Соцсети', 2), ('fp.example-cdn.com', 'Снятие отпечатка', 1)]
    hl = group([row('block', h, k, f'<span class="ty-cap" style="font-weight: 700">×{n}</span>', tone='var(--sf-high)', ink='var(--on-sf-v)', h=60) for h, k, n in hosts])
    body = f'''{status()}{topbar('Privacy X-Ray')}
{screen(108, f"""<div style="border-radius: 28px; background: var(--card); padding: 18px; display: flex; gap: 18px; align-items: center; box-shadow: var(--e1)">{donut}<span style="display: flex; flex-direction: column; gap: 6px"><span class="ty-title">Заблокировано на north-guide.ru</span><span class="ty-cap" style="font-size: 13px; line-height: 18px">Страница грузится быстрее и не передаёт сведения о вас</span></span></div>
<div style="border-radius: 24px; background: var(--card); padding: 8px 0">{bars}</div>
{section('Кому страница пыталась отправить данные')}{hl}
<a href="#" style="border-radius: 22px; background: color-mix(in srgb, var(--pri-c) 55%, transparent); padding: 12px 12px 12px 16px; display: flex; align-items: center; gap: 12px">{ms('insights' if False else 'shield', 'f', 'color: var(--pri)')}<span style="flex-grow: 1; display: flex; flex-direction: column; gap: 2px"><span class="ty-label">За неделю: 1 284 трекера на 96 сайтах</span><span class="ty-cap" style="color: var(--on-pri-c)">Настройки защиты</span></span>{CHEV}</a>""", gap=12)}
{handle()}'''
    W('PrivacyXRay', 'Privacy X-Ray', 'w-work t-light aura', body)

    perm_sheet = sheet(f'''<span style="width: 60px; height: 60px; border-radius: 22px; background: var(--pri-c); color: var(--on-pri-c); display: flex; align-items: center; justify-content: center; margin: 6px 4px 0">{ms("location_on", "f l")}</span>
<span style="display: flex; flex-direction: column; gap: 6px; padding: 0 4px"><span class="ty-title-l">Показать сайту, где вы?</span><span class="ty-body" style="color: var(--on-sf-v)"><b style="color: var(--on-sf)">maps.example.com</b> просит доступ к вашему местоположению.</span></span>
{group([row('my_location' if False else 'location_on', 'Точное местоположение', 'Иначе — только примерный район', switch(True), h=64)])}
<span class="ty-cap" style="padding: 0 6px">Решение можно изменить в сведениях о сайте</span>
{btn('Разрешить на этом сайте', 'fill', '', h=52)}
<div style="display: flex; gap: 8px">{btn('Только сейчас', 'tonal', '', True)}{btn('Запретить', 'out', '', True)}</div>''', gap=12, pad='0 16px 32px')
    body = f'''<div aria-hidden="true" style="position: absolute; inset: 0">{MAP}</div>
<div aria-hidden="true" style="position: absolute; left: 16px; right: 16px; top: 52px; height: 52px; border-radius: 26px; background: #FFFFFF; box-shadow: 0 4px 14px rgba(0,0,0,0.12); display: flex; align-items: center; gap: 12px; padding: 0 16px; color: #5F6368; font-size: 15px; font-weight: 500">{ms("search", "s")}Поиск на карте</div>
{status()}{scrim()}{perm_sheet}{handle()}'''
    W('Permission', 'запрос разрешения', 'w-work t-light', body)

    act = lambda icon, label, on=False: f'<button style="height: 76px; border-radius: 20px; background: {"var(--sec-c)" if on else "transparent"}; color: {"var(--on-sec-c)" if on else "var(--on-sf)"}; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6px; font-size: 12px; line-height: 14px; font-weight: 600; text-align: center">{ms(icon, "f" if on else "")}{label}</button>'
    acts = ''.join(act(*a) for a in [('open_in_new', 'Glance', True), ('add', 'Новая вкладка'), ('tab', 'В фоне'), ('splitscreen', 'Рядом'),
                                     ('domino_mask', 'Приватно'), ('tab_group', 'В группу…'), ('content_copy', 'Копировать'), ('share', 'Отправить')])
    bars = ''.join(f'<i style="flex: 1; height: {h}%; border-radius: 4px 4px 1px 1px; background: {"#2F6B5F" if k == 5 else "#BFE0D8"}"></i>' for k, h in enumerate((35, 46, 56, 64, 78, 100)))
    peek = f'''<div style="position: absolute; left: 14px; right: 14px; top: 212px; border-radius: 30px; background: var(--sf-low); box-shadow: var(--e3); overflow: hidden; z-index: 5">
<div style="position: relative; height: 150px; background: linear-gradient(160deg, #EEF6F4, #D4F1EC); padding: 16px; display: flex; flex-direction: column; justify-content: flex-end; gap: 4px"><span aria-hidden="true" style="position: absolute; right: 18px; top: 18px; width: 112px; height: 56px; display: flex; align-items: flex-end; gap: 5px">{bars}</span><span class="ty-over" style="color: #2F6B5F">Прогноз льда</span><span style="font-size: 19px; line-height: 24px; font-weight: 700; color: #16211E">Толщина льда: южная часть озера</span></div>
<div style="height: 44px; display: flex; align-items: center; gap: 8px; padding: 0 16px; border-bottom: 1px solid var(--sf-high)">{ms("lock", "xs", "color: var(--ok)")}<span class="ty-cap" style="font-size: 13px; font-weight: 600">ice-forecast.example.ru/south</span></div>
<div style="display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 4px; padding: 8px">{acts}</div></div>'''
    link = '<div style="position: absolute; left: 20px; top: 150px; height: 36px; border-radius: 10px; background: #FFFFFF; box-shadow: 0 0 0 3px var(--pri); padding: 0 10px; display: flex; align-items: center; font-family: Literata, Georgia, serif; font-size: 17px; color: #1E5E8C; text-decoration: underline; z-index: 5">прогноз толщины льда</div>'
    body = f'''{status()}<div class="page-card" style="top: 40px; bottom: 92px; filter: blur(3px)">{article_long()}</div>{page_bar()}
<div aria-hidden="true" style="position: absolute; inset: 0; background: rgba(8, 14, 16, 0.3)"></div>{link}{peek}{handle()}'''
    W('LinkPeek', 'долгое нажатие на ссылку', 'w-work t-light aura', body)

# ---------------------------------------------------------------- passwords

def badge(label, kind):
    tones = {'leak': ('var(--err-c)', 'var(--on-err-c)', 'warning'), 'reuse': ('var(--warn-c)', 'var(--on-warn-c)', 'content_copy'),
             'weak': ('var(--sf-high)', 'var(--on-sf-v)', 'key'), '2fa': ('var(--sf-high)', 'var(--on-sf-v)', 'pin'), 'passkey': ('var(--sec-c)', 'var(--on-sec-c)', 'passkey')}
    bg, fg, icon = tones[kind]
    lab = f'<span>{label}</span>' if label else ''
    ic = '' if kind == '2fa' else ms(icon, "", "font-size: 15px")
    return f'<span style="height: 26px; border-radius: 13px; padding: 0 {8 if label else 5}px; background: {bg}; color: {fg}; display: flex; align-items: center; gap: 4px; font-size: 12px; font-weight: 700; white-space: nowrap; flex-shrink: 0">{ic}{lab}</span>'


PW_ROWS = [('П', '#DDE3FF', '#2F4AA8', 'mail.example.com', 'anna@example.com', [('', 'passkey'), ('2FA', '2fa')]),
           ('Б', '#D3F0D9', '#2B6C3F', 'bank.example.ru', 'anna.k', [('', 'passkey')]),
           ('З', '#FFE1D6', '#9A3A1E', 'tasks.example.com', 'anna@work.example', [('Повтор', 'reuse')]),
           ('Ф', '#FFDDEA', '#962F59', 'forum.example.org', 'snowfox', [('Утечка', 'leak')]),
           ('О', '#FFEBC2', '#6E4F00', 'cloud.example.net', 'anna@example.com', [('2FA', '2fa')]),
           ('Р', '#E6DEFF', '#4A3A9E', 'recipes.example.com', 'anna@example.com', [('Утечка', 'leak')])]


def passwords_body():
    rows = '<i style="height: 1px; background: var(--sf-high); margin: 0 16px 0 68px"></i>'.join(
        list_item(l, bg, ink, host, user, '<span style="display: flex; gap: 4px; padding-right: 6px">' + ''.join(badge(t, k) for t, k in bs) + '</span>') for l, bg, ink, host, user, bs in PW_ROWS)
    health = f'''<a href="#" style="border-radius: 24px; background: var(--card); box-shadow: var(--e1); padding: 14px 12px 14px 16px; display: flex; align-items: center; gap: 14px">
<span style="width: 44px; height: 44px; border-radius: 15px; background: var(--warn-c); color: var(--on-warn-c); display: flex; align-items: center; justify-content: center">{ms('health_and_safety', 'f')}</span>
<span style="flex-grow: 1; min-width: 0; display: flex; flex-direction: column; gap: 6px"><span class="ty-label" style="font-size: 15px">Проверка паролей</span><span style="display: flex; gap: 5px; flex-wrap: wrap">{badge('3 утечки', 'leak')}{badge('5 повторов', 'reuse')}{badge('2 слабых', 'weak')}</span></span>{CHEV}</a>'''
    chips = f'<div style="display: flex; gap: 8px">{chip("Все 128", True)}{chip("Ключи 6", False, "passkey")}{chip("2FA 9", False, "pin")}</div>'
    return f'''{status()}{topbar('Пароли', (('casino', 'Генератор'), ('lock', 'Заблокировать'), ('more_vert', 'Ещё')))}
{screen(108, searchfield('Сайт или логин') + health + chips + section('Недавние') + f'<div style="border-radius: 24px; background: var(--card); overflow: hidden">{rows}</div>', gap=12)}
{fade(120)}{fab('add', '', 28)}{handle()}'''


def g_passwords():
    W('Passwords', 'пароли', 'w-personal t-light aura', passwords_body())
    W('PasswordsDark', 'пароли (тёмная)', 'w-personal t-dark aura', passwords_body())

    field = lambda label, val, trail, mono=False: f'''<div style="min-height: 68px; display: flex; align-items: center; gap: 8px; padding: 10px 8px 10px 16px"><span style="flex-grow: 1; display: flex; flex-direction: column; gap: 4px"><span class="ty-cap">{label}</span><span style="font-size: {19 if mono else 16}px; font-weight: 600; letter-spacing: {"0.12em" if mono else "0"}; {"font-family: JetBrains Mono, monospace; color: var(--pri);" if mono else ""}">{val}</span></span>{trail}</div>'''
    ib = lambda icon, label: f'<button class="ib4" aria-label="{label}" style="color: var(--on-sf)">{ms(icon)}</button>'
    ring = '<span style="width: 34px; height: 34px; border-radius: 17px; background: conic-gradient(var(--pri) 0 60%, var(--sf-high) 60% 100%); display: flex; align-items: center; justify-content: center"><span style="width: 26px; height: 26px; border-radius: 13px; background: var(--card); display: flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 700">18</span></span>'
    sep = '<i style="height: 1px; background: var(--sf-high); margin: 0 16px"></i>'
    creds = f'<div style="border-radius: 24px; background: var(--card); overflow: hidden">{field("Логин", "anna@example.com", ib("content_copy", "Копировать"))}{sep}{field("Пароль", "•••• •••• ••••", ib("visibility", "Показать") + ib("content_copy", "Копировать"))}{sep}{field("Код 2FA", "482 913", ring + ib("content_copy", "Копировать"), True)}</div>'
    warn = f'<div style="border-radius: 22px; background: var(--warn-c); color: var(--on-warn-c); padding: 12px 12px 12px 16px; display: flex; align-items: center; gap: 12px">{ms("warning", "f")}<span style="flex-grow: 1; font-size: 14px; font-weight: 600; line-height: 19px">Этот пароль есть ещё на 2 сайтах</span><button class="btn" style="height: 40px; border-radius: 20px; padding: 0 16px; background: var(--on-warn-c); color: var(--warn-c); font-size: 14px">Сменить</button></div>'
    more = group([row('passkey', 'Ключ доступа', 'Вход без пароля · на этом телефоне', '', 'var(--sec-c)', 'var(--on-sec-c)'), row('language', 'Сайты', 'mail.example.com, accounts.example.com', ''), row('history', 'История пароля', 'Изменён 12 марта · 2 прошлые версии', CHEV)])
    body = f'''{status()}{topbar('', (('edit', 'Изменить'), ('more_vert', 'Ещё')))}
{screen(104, f"""<div style="display: flex; align-items: center; gap: 16px; padding: 0 4px 4px">{fav('П', '#DDE3FF', '#2F4AA8', 60, 20, 24)}<span style="display: flex; flex-direction: column; gap: 2px"><span class="ty-head">Почта</span><span class="ty-cap" style="font-size: 14px">mail.example.com</span></span></div>{warn}{creds}{more}<button class="btn" style="height: 48px; color: var(--err); align-self: center">{ms('delete', 's')}Удалить запись</button>""", gap=12)}
{handle()}'''
    W('PasswordDetail', 'запись пароля', 'w-personal t-light aura', body)

    def issue(title, count, tone, ink, icon, items):
        head = f'<div style="display: flex; align-items: center; gap: 10px; padding: 6px 4px 0"><span style="width: 28px; height: 28px; border-radius: 9px; background: {tone}; color: {ink}; display: flex; align-items: center; justify-content: center">{ms(icon, "f", "font-size: 17px")}</span><span class="ty-title" style="flex-grow: 1">{title}</span><span class="ty-label" style="color: var(--on-sf-v)">{count}</span></div>'
        rows = '<i style="height: 1px; background: var(--sf-high); margin: 0 16px 0 68px"></i>'.join(
            list_item(l, bg, ik, h, sub, '<button class="btn tonal" style="height: 36px; border-radius: 18px; padding: 0 14px; font-size: 13.5px; margin-right: 6px">Сменить</button>') for l, bg, ik, h, sub in items)
        return head + f'<div style="border-radius: 24px; background: var(--card); overflow: hidden">{rows}</div>'
    score = f'''<div style="border-radius: 28px; background: var(--card); box-shadow: var(--e1); padding: 18px; display: flex; align-items: center; gap: 18px">
<span style="width: 92px; height: 92px; border-radius: 46px; background: conic-gradient(var(--err) 0 8%, var(--warn) 8% 30%, var(--ol) 30% 38%, var(--ok) 38% 100%); display: flex; align-items: center; justify-content: center; flex-shrink: 0"><span style="width: 72px; height: 72px; border-radius: 36px; background: var(--card); display: flex; flex-direction: column; align-items: center; justify-content: center"><span style="font-size: 26px; font-weight: 700; line-height: 28px">92%</span><span class="ty-cap" style="font-size: 11px">в порядке</span></span></span>
<span style="display: flex; flex-direction: column; gap: 6px"><span class="ty-title">Начните с утечек</span><span class="ty-cap" style="font-size: 13px; line-height: 18px">Их пароли уже есть у злоумышленников. Остальные 118 записей в порядке.</span></span></div>'''
    body = f'''{status()}{topbar('Проверка паролей')}
{screen(108, score + issue('Найдены в утечках', 3, 'var(--err-c)', 'var(--on-err-c)', 'warning', [('Ф', '#FFDDEA', '#962F59', 'forum.example.org', 'snowfox · утечка 2024 года'), ('Р', '#E6DEFF', '#4A3A9E', 'recipes.example.com', 'anna@example.com')])
         + issue('Повторяются', 5, 'var(--warn-c)', 'var(--on-warn-c)', 'content_copy', [('З', '#FFE1D6', '#9A3A1E', 'tasks.example.com', 'Такой же, как на 2 сайтах')])
         + issue('Слабые', 2, 'var(--sf-high)', 'var(--on-sf-v)', 'key', [('К', '#E6DEFF', '#4A3A9E', 'cinema.example.ru', '8 символов, только буквы')])
         + f'<div style="border-radius: 20px; background: var(--sf-high); padding: 12px 14px; display: flex; gap: 10px; align-items: flex-start">{ms("info", "s", "color: var(--on-sf-v)")}<span class="ty-cap" style="font-size: 13px; line-height: 18px">Проверка утечек анонимна: на сервер уходят только первые 5 символов хеша пароля. Её можно выключить.</span></div>', gap=10)}
{handle()}'''
    W('PasswordHealth', 'проверка паролей', 'w-personal t-light aura', body)

    site_form = lambda heading, email_val, focus_pw: f'''<div style="position: absolute; left: 0; right: 0; top: 40px; bottom: 0; background: #FFFFFF; padding: 16px 24px; display: flex; flex-direction: column; gap: 14px">
<span style="display: flex; align-items: center; gap: 10px; font-size: 16px; font-weight: 700; color: #1B2A55">{fav('П' if 'Вход' in heading else 'О', '#2F4AA8' if 'Вход' in heading else '#3B5BDB', '#FFFFFF', 30, 9, 14)}{'Почта' if 'Вход' in heading else 'Облако'}</span>
<span style="font-size: 26px; font-weight: 700; color: #151B2E; margin-top: 8px; letter-spacing: -0.01em">{heading}</span>
<span style="display: flex; flex-direction: column; gap: 6px"><span style="font-size: 13px; font-weight: 600; color: #4A5270">Эл. почта</span><span style="height: 52px; border-radius: 12px; border: {'1px solid #C8CCDA' if email_val else '2px solid #3B5BDB'}; display: flex; align-items: center; padding: 0 14px; font-size: 16px; color: {'#151B2E' if email_val else '#8A90A6'}">{email_val or 'name@example.com'}</span></span>
<span style="display: flex; flex-direction: column; gap: 6px"><span style="font-size: 13px; font-weight: 600; color: #4A5270">{'Придумайте пароль' if focus_pw else 'Пароль'}</span><span style="height: 52px; border-radius: 12px; border: {'2px solid #3B5BDB' if focus_pw else '1px solid #C8CCDA'}; display: flex; align-items: center; padding: 0 13px">{'<i style="width: 2px; height: 22px; background: #3B5BDB"></i>' if focus_pw else ''}</span></span>
</div>'''
    acct = lambda l, bg, ink, name, sub: list_item(l, bg, ink, name, sub, f'<span class="ib4" style="width: 40px; height: 40px">{ms("fingerprint", "s")}</span>')
    fill = sheet(f'''<div style="display: flex; align-items: center; gap: 12px; padding: 0 4px">{lead('key', 'var(--sec-c)', 'var(--on-sec-c)')}<span style="display: flex; flex-direction: column; gap: 2px"><span class="ty-title">Войти на mail.example.com</span><span style="display: flex; align-items: center; gap: 4px; font-size: 12.5px; font-weight: 600; color: var(--ok)">{ms('verified', 'f', 'font-size: 15px')}Адрес проверен: это тот же сайт</span></span></div>
{btn('Войти с ключом доступа', 'fill', 'passkey', h=56)}
<div style="border-radius: 24px; background: var(--card); overflow: hidden">{acct('А', '#DDE3FF', '#2F4AA8', 'anna@example.com', 'Последний вход вчера')}<i style="height: 1px; background: var(--sf-high); margin: 0 16px 0 68px"></i>{acct('Р', '#D4F1EC', '#00665A', 'anna.work@example.com', 'Пространство «Работа»')}</div>
<div style="display: flex; gap: 8px">{btn('Другой пароль', 'out', 'search', True, 48)}{btn('Не заполнять', 'out', 'close', True, 48)}</div>''', gap=12)
    W('Autofill', 'автозаполнение входа', 'w-personal t-light', status() + site_form('Вход в аккаунт', '', False) + '<div aria-hidden="true" style="position: absolute; inset: 0; background: linear-gradient(180deg, transparent 30%, rgba(8, 14, 16, 0.3) 100%)"></div>' + fill + handle())

    pw = '<span style="font-family: JetBrains Mono, monospace; font-size: 22px; font-weight: 600; letter-spacing: 0.02em">vK7<b style="color: var(--pri)">#</b>qe2Lm<b style="color: var(--pri)">!</b>Tz9pWf</span>'
    strength = '<div style="display: flex; gap: 4px">' + ''.join('<i style="flex: 1; height: 6px; border-radius: 3px; background: var(--ok)"></i>' for _ in range(4)) + '</div>'
    slider = '<span style="position: relative; flex-grow: 1; height: 16px; display: flex; align-items: center; margin: 0 8px"><i style="flex-grow: 1; height: 16px; border-radius: 8px; background: linear-gradient(90deg, var(--pri) 0 62%, var(--sf-highest) 62% 100%)"></i><i style="position: absolute; left: 62%; width: 4px; height: 28px; margin-left: 2px; border-radius: 2px; background: var(--pri)"></i></span>'
    gen = sheet(f'''<div style="display: flex; align-items: center; justify-content: space-between; padding: 0 4px"><span class="ty-title">Надёжный пароль</span><div style="width: 170px">{seg(['Пароль', 'Фраза'], 0)}</div></div>
<div style="border-radius: 24px; background: var(--card); padding: 16px 8px 16px 18px; display: flex; align-items: center; gap: 4px"><span style="flex-grow: 1">{pw}</span><button class="ib4" aria-label="Другой">{ms('refresh')}</button><button class="ib4" aria-label="Копировать">{ms('content_copy')}</button></div>
{strength}<span style="font-size: 13px; font-weight: 600; color: var(--ok); padding: 0 4px; margin-top: -6px">Очень надёжный · подбор займёт века</span>
{group([row('', 'Длина', '', slider + '<span class="ty-label" style="width: 22px; text-align: right">20</span>', h=56), row('', 'Цифры и символы', '', switch(True), h=56), row('', 'Без похожих знаков (l, 1, O, 0)', '', switch(False), h=56)])}
{btn('Использовать и сохранить', 'fill', 'check', h=56)}''', gap=12)
    W('Generator', 'генератор пароля', 'w-personal t-light', status() + site_form('Регистрация', 'anna@example.com', True) + '<div aria-hidden="true" style="position: absolute; inset: 0; background: linear-gradient(180deg, transparent 30%, rgba(8, 14, 16, 0.3) 100%)"></div>' + gen + handle())

# ---------------------------------------------------------------- transfer

def qr_grid(n=25, seed=1847):
    rnd_state = [seed]

    def rnd():
        rnd_state[0] = rnd_state[0] * 16807 % 2147483647
        return rnd_state[0] / 2147483647

    def in_finder(x, y, ox, oy):
        return ox - 1 <= x <= ox + 7 and oy - 1 <= y <= oy + 7

    def finder_on(x, y, ox, oy):
        dx, dy = x - ox, y - oy
        if not (0 <= dx <= 6 and 0 <= dy <= 6):
            return False
        return dx in (0, 6) or dy in (0, 6) or (2 <= dx <= 4 and 2 <= dy <= 4)
    cells = []
    for y in range(n):
        for x in range(n):
            if in_finder(x, y, 0, 0) or in_finder(x, y, n - 7, 0) or in_finder(x, y, 0, n - 7):
                on = finder_on(x, y, 0, 0) or finder_on(x, y, n - 7, 0) or finder_on(x, y, 0, n - 7)
            elif 9 <= x <= 15 and 9 <= y <= 15:
                on = False
            elif y == 6:
                on = x % 2 == 0
            elif x == 6:
                on = y % 2 == 0
            else:
                on = rnd() > 0.52
            cells.append(f'<i style="background: {"#1B1F20" if on else "transparent"}"></i>')
    return ''.join(cells)


LOGO = ('<svg viewBox="0 0 108 108" style="width: 32px; height: 32px"><defs><linearGradient id="ql" x1="29.5" y1="30" x2="51.5" y2="80" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="#A78BFA"></stop><stop offset="1" stop-color="#6D4CFF"></stop></linearGradient>'
        '<linearGradient id="qr" x1="77.5" y1="28" x2="51.5" y2="80" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="#22D3EE"></stop><stop offset="1" stop-color="#3B6BFF"></stop></linearGradient></defs>'
        '<path d="M31,34 C35.5,52.5 42.5,67.5 51.5,78.5" fill="none" stroke="url(#ql)" stroke-width="13" stroke-linecap="round"></path><path d="M51.5,78.5 C58.5,62.5 66.5,44.5 77,30" fill="none" stroke="url(#qr)" stroke-width="13" stroke-linecap="round"></path></svg>')


def g_transfer():
    src = [('language', 'Chrome', 'Пароли (CSV) и закладки (HTML)', '#DDE3FF', '#2F4AA8'), ('public', 'Firefox', 'Пароли и закладки с компьютера', '#FFE1D6', '#9A3A1E'),
           ('travel_explore', 'Samsung Internet', 'Закладки (HTML)', '#E6DEFF', '#4A3A9E'), ('shield_lock', 'Bitwarden, Proton Pass, 1Password', 'Пароли, коды 2FA, карты (CSV, JSON)', '#D4F1EC', '#00665A'),
           ('key', 'KeePass', 'Файл .kdbx целиком, с группами', '#FFEBC2', '#6E4F00'), ('devices', 'Другая копия Vola', 'Архив данных или связка устройств', 'var(--sec-c)', 'var(--on-sec-c)')]
    rows = group([row(i, t, d, CHEV, bg, ink, fill=True) for i, t, d, bg, ink in src])
    body = f'''{status()}{topbar('Перенести в Vola')}
{screen(108, f'<span class="ty-body" style="color: var(--on-sf-v); padding: 0 4px">Закладки, пароли и история переносятся на этом телефоне. Файл экспорта Vola предложит удалить сразу после импорта.</span>{section("Откуда")}{rows}{btn("У меня уже есть файл", "tonal", "upload_file", h=52)}<span style="display: flex; align-items: center; justify-content: center; gap: 6px" class="ty-cap">{ms("shield_lock", "xs", "font-size: 16px")}Файлы разбираются на телефоне и никуда не уходят</span>', gap=12)}
{handle()}'''
    W('Import', 'перенос данных', 'w-work t-light aura', body)

    steps = [('check', 'Откройте Chrome → Менеджер паролей', 'Настройки → Экспортировать пароли', True), ('check', 'Закладки — с компьютера', 'chrome://bookmarks → ⋮ → Экспорт закладок (HTML)', True),
             ('3', 'Вернитесь в Vola и выберите файлы', 'Дубликаты Vola пропустит сама', False)]
    st = ''.join(f'<div style="display: flex; gap: 14px; padding: 12px 16px"><span style="width: 32px; height: 32px; border-radius: 16px; flex-shrink: 0; background: {"var(--ok-c)" if done else "var(--pri)"}; color: {"var(--on-ok-c)" if done else "var(--on-pri)"}; display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 700">{ms("check", "s") if done else n}</span><span style="display: flex; flex-direction: column; gap: 2px"><span class="ty-label" style="font-size: 15px">{t}</span><span class="ty-cap">{d}</span></span></div>' for n, t, d, done in steps)
    stat = lambda n, label: f'<div style="border-radius: 18px; background: var(--sf-low); padding: 12px; display: flex; flex-direction: column; gap: 2px"><span style="font-size: 24px; font-weight: 700">{n}</span><span class="ty-cap">{label}</span></div>'
    done = f'''<div style="border-radius: 28px; background: var(--card); padding: 18px; display: flex; flex-direction: column; gap: 14px; box-shadow: var(--e1)">
<div style="display: flex; align-items: center; gap: 12px">{lead('check_circle', 'var(--ok-c)', 'var(--on-ok-c)', True)}<span style="display: flex; flex-direction: column; gap: 2px"><span class="ty-title">Готово</span><span class="ty-cap">Chrome Passwords.csv · 38 КБ</span></span></div>
<div style="display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px">{stat('126', 'паролей')}{stat('214', 'закладок')}{stat('3', 'дубликата')}</div>
<div style="border-radius: 18px; background: var(--warn-c); color: var(--on-warn-c); padding: 12px 14px; display: flex; gap: 10px; align-items: flex-start">{ms('warning', 's f')}<span style="font-size: 13.5px; font-weight: 600; line-height: 19px">В файле пароли лежат открытым текстом. Удалите его из «Загрузок».</span></div>
<div style="display: flex; gap: 8px">{btn('Удалить файл', 'fill', 'delete', True).replace('class="btn fill"', 'class="btn fill" style="background: var(--err); color: #FFFFFF"').replace('" style="height', '; height', 1) if False else '<button class="btn" style="flex: 1 1 0; height: 52px; border-radius: 26px; background: var(--err); color: #FFFFFF">' + ms('delete', 's') + 'Удалить файл</button>'}{btn('К паролям', 'tonal', '', True)}</div></div>'''
    nxt = f'<a href="#" style="border-radius: 24px; background: var(--card); padding: 14px 12px 14px 16px; display: flex; align-items: center; gap: 14px">{lead("health_and_safety", "var(--warn-c)", "var(--on-warn-c)", True)}<span style="flex-grow: 1; display: flex; flex-direction: column; gap: 2px"><span class="ty-label" style="font-size: 15px">Проверить перенесённые</span><span class="ty-cap">3 пароля в утечках, 5 повторяются</span></span>{CHEV}</a>'
    body = f'''{status()}{topbar('Перенос из Chrome')}
{screen(108, f'<div style="border-radius: 24px; background: var(--card); padding: 4px 0">{st}</div>{done}{nxt}', gap=12)}
{handle()}'''
    W('ImportChrome', 'перенос из Chrome', 'w-work t-light aura', body)

    qr = f'''<div role="img" aria-label="QR-код связки" style="position: relative; width: 208px; height: 208px; border-radius: 28px; background: #FFFFFF; box-shadow: var(--e2); padding: 20px; display: grid; grid-template-columns: repeat(25, minmax(0, 1fr)); grid-auto-rows: 1fr; align-self: center">{qr_grid()}
<span style="position: absolute; left: 50%; top: 50%; width: 44px; height: 44px; margin: -22px 0 0 -22px; border-radius: 13px; background: #FFFFFF; display: flex; align-items: center; justify-content: center">{LOGO}</span></div>'''
    ways = group([row('wifi', 'Напрямую по Wi-Fi', 'Когда устройства в одной сети', switch(True), '#DDE3FF', '#2F4AA8'),
                  row('folder', 'Через вашу папку', 'Syncthing, Nextcloud, Диск — любые файлы', switch(True), '#D4F1EC', '#00665A'),
                  row('dns', 'Свой сервер', 'Если он у вас уже настроен', switch(False), 'var(--sf-high)', 'var(--on-sf-v)')])
    body = f'''{status()}{topbar('Связать устройства')}
{screen(104, f"""{qr}<span style="display: flex; flex-direction: column; align-items: center; gap: 6px; text-align: center; padding: 4px 12px 0"><span class="ty-title">Отсканируйте на втором устройстве</span><span class="ty-cap" style="font-size: 13px; line-height: 18px">Vola → Настройки → Синхронизация → «Присоединиться». Ключ передаётся только через этот код.</span></span>
<div style="display: flex; gap: 8px; justify-content: center"><span class="chip" style="box-shadow: none; background: var(--sf-high)">{ms('schedule', 'xs')}Код действует ещё 4:52</span><button class="chip" style="color: var(--pri)">{ms('key', 'xs')}Фраза из 12 слов</button></div>
{section('Как передавать данные')}{ways}""", gap=12)}
{handle()}'''
    W('SyncPair', 'связать устройства', 'w-work t-light aura', body)

# ---------------------------------------------------------------- tools

def g_tools():
    from build_v4 import tab_card as tc
    # Tab groups inside the workspace.
    def group_card(name, color, tint, thumbs, count):
        mini_t = ''.join(f'<span style="border-radius: 12px; background: #FFFFFF; overflow: hidden; position: relative; box-shadow: 0 1px 3px rgba(0,0,0,0.08)"><span style="position: absolute; left: 0; top: 0; width: 160px; height: 184px; transform: scale(0.44); transform-origin: 0 0">{mini(k, b, t)}</span></span>' for k, b, t in thumbs)
        return (f'<div style="display: flex; flex-direction: column; gap: 8px"><div style="height: 262px; border-radius: 22px; background: {tint}; box-shadow: inset 0 0 0 2px {color}; padding: 8px; display: grid; grid-template-columns: 1fr 1fr; grid-template-rows: 1fr 1fr; gap: 6px">{mini_t}</div>'
                f'<span style="display: flex; align-items: center; gap: 8px; padding: 0 4px"><i style="width: 10px; height: 10px; border-radius: 5px; background: {color}"></i><span style="flex-grow: 1; font-size: 13.5px; font-weight: 600">{name}</span><span class="ty-cap" style="font-weight: 700">{count}</span></span></div>')
    g1 = group_card('Поездка на Байкал', '#2F5BD3', '#E4EAFF', [('article', '#2F6B5F', '#D4F1EC'), ('chart', '#00665A', '#D4F1EC'), ('list', '#1E5E8C', '#D8EEFF'), ('doc', '#9A3A1E', '#FFE1D6')], 4)
    g2 = group_card('Спринт 42', '#A23F2B', '#FCE6DF', [('board', '#9A3A1E', '#FFE1D6'), ('doc', '#4A3A9E', '#E6DEFF'), ('chart', '#6E4F00', '#FFEBC2'), ('list', '#00665A', '#D4F1EC')], 3)
    tabs = ''.join(tc(t, current=(k == 0)) for k, t in enumerate([TABS[2], ('Макеты главного экрана', 'М', '#FFDDEA', '#962F59', 'board')]))
    toast = f'<div style="position: absolute; left: 12px; right: 12px; bottom: 96px; border-radius: 20px; background: var(--inv-sf); color: var(--inv-on-sf); padding: 8px 8px 8px 16px; display: flex; align-items: center; gap: 12px; box-shadow: var(--e2); z-index: 4">{ms("tab_group", "s", "color: var(--pri-c)")}<span style="flex-grow: 1; font-size: 14px; font-weight: 600; line-height: 19px">3 вкладки про ремонт — собрать в группу?</span><button class="btn" style="height: 40px; border-radius: 20px; padding: 0 12px; color: var(--pri-c); font-size: 14px">Собрать</button></div>'
    from build_v4 import tabs_body
    base = tabs_body('A')
    a = base.index('<div style="position: absolute; left: 10px; right: 10px; top: 104px;')
    b = base.index('<div style="position: absolute; left: 16px; right: 16px; top: 690px;')
    grid = f'<div style="position: absolute; left: 16px; right: 16px; top: 108px; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px 12px">{g1}{g2}{tabs}</div>'
    body = base[:a] + grid + base[b:].replace('top: 690px', 'top: 1690px') + toast
    body = body.replace('6 вкладок', '2 группы · 9 вкладок')
    W('TabGroups', 'группы вкладок', 'w-work t-light aura', body)

    # Translate: English page with the translation sheet.
    en = f'''<div style="position: absolute; inset: 0; background: #FFFFFF">
<div style="height: 56px; padding: 0 18px; display: flex; align-items: center; gap: 10px; border-bottom: 1px solid #EEF0F2"><span style="font-family: Literata, Georgia, serif; font-size: 18px; font-weight: 600; color: #11233A; flex-grow: 1">Lake Journal</span><span style="height: 32px; border-radius: 16px; padding: 0 14px; background: #11233A; color: #FFFFFF; font-size: 13px; font-weight: 600; display: flex; align-items: center">Subscribe</span></div>
<div style="padding: 20px; display: flex; flex-direction: column; gap: 14px"><span style="font-size: 12px; font-weight: 700; letter-spacing: 0.08em; text-transform: uppercase; color: #2F5BD3">Science · 6 min read</span>
<h1 style="font-family: Literata, Georgia, serif; font-size: 30px; line-height: 36px; font-weight: 600; color: #11233A">Why Baikal ice turns turquoise in March</h1>
<div style="height: 180px; border-radius: 18px; overflow: hidden">{BAIKAL}</div>
<p style="font-family: Literata, Georgia, serif; font-size: 17px; line-height: 28px; color: #2A3442">Clear, cold nights and almost no snow let the ice grow without bubbles.</p></div></div>'''
    lang = lambda l, on: f'<span style="flex: 1 1 0; height: 48px; border-radius: 16px; background: {"var(--sec-c)" if on else "var(--card)"}; color: {"var(--on-sec-c)" if on else "var(--on-sf)"}; display: flex; align-items: center; justify-content: center; gap: 6px; font-size: 15px; font-weight: 600">{l}</span>'
    tr = sheet(f'''<div style="display: flex; align-items: center; gap: 14px; padding: 0 4px">{lead('translate', 'var(--pri-c)', 'var(--on-pri-c)')}<span style="display: flex; flex-direction: column; gap: 2px"><span class="ty-title">Перевести на русский?</span><span class="ty-cap">Страница на английском</span></span></div>
<div style="display: flex; align-items: center; gap: 8px">{lang('Английский', False)}{ms('arrow_forward', 's', 'color: var(--on-sf-v)')}{lang('Русский', True)}</div>
<div style="border-radius: 20px; background: var(--card); padding: 12px 14px; display: flex; flex-direction: column; gap: 10px"><span style="display: flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 600; color: var(--ok)">{ms('shield_lock', 'xs f')}Переводит сам телефон: текст никуда не уходит</span><span style="display: flex; justify-content: space-between" class="ty-cap"><span>Загрузка языка · 17 из 24 МБ</span><span>один раз</span></span><span style="height: 6px; border-radius: 3px; background: var(--sf-high)"><i style="width: 71%; height: 6px; border-radius: 3px; background: var(--pri)"></i></span></div>
{group([row('', 'Всегда переводить английский', '', switch(False), h=56)])}
<div style="display: flex; gap: 8px">{btn('Не для этого сайта', 'out', '', True)}{btn('Перевести', 'fill', 'translate', True)}</div>''', gap=12)
    W('Translate', 'перевод на устройстве', 'w-work t-light aura', f'{status()}<div class="page-card" style="top: 40px; bottom: 92px">{en}</div>{page_bar("lakejournal.example", action="translate")}{scrim()}{tr}{handle()}')

    # Full-page screenshot editor (dark UI around the shot).
    lines = ''.join(f'<i style="height: 5px; border-radius: 3px; background: #E3E6EA; width: {w}%"></i>' for w in (96, 88, 92, 70, 94, 84, 90, 60))
    shot = f'''<div style="position: absolute; left: 72px; right: 72px; top: 112px; height: 560px; border-radius: 16px; background: #FFFFFF; overflow: hidden; box-shadow: 0 20px 50px rgba(0,0,0,0.6)">
<div style="padding: 14px 12px 0; display: flex; flex-direction: column; gap: 8px"><span style="display: flex; align-items: center; gap: 6px"><i style="width: 14px; height: 14px; border-radius: 4px; background: #2F6B5F"></i><span style="font-size: 9px; font-weight: 700; color: #1F3A34">Северный путеводитель</span></span>
<span style="font-family: Literata, Georgia, serif; font-size: 16px; line-height: 19px; font-weight: 600; color: #16211E">Байкал зимой: как выбрать маршрут по льду</span>
<span style="height: 96px; border-radius: 9px; overflow: hidden">{BAIKAL_T}</span>
<span style="position: relative; display: flex; flex-direction: column; gap: 5px">{lines}<span style="position: absolute; left: -2px; right: -2px; top: 14px; height: 40px; border-radius: 8px; background: rgba(120,120,130,0.62); backdrop-filter: blur(6px); display: flex; align-items: center; justify-content: center; gap: 4px; font-size: 10px; font-weight: 700; color: #FFFFFF"><span class="ms" style="font-size: 14px">blur_on</span>Скрыто: имя и почта</span></span>
<span style="display: flex; flex-direction: column; gap: 5px; margin-top: 6px">{lines}</span><span style="display: flex; flex-direction: column; gap: 5px">{lines}</span></div></div>
<div aria-hidden="true" style="position: absolute; left: 64px; right: 64px; top: 104px; height: 576px; border-radius: 22px; box-shadow: inset 0 0 0 2px var(--pri)"></div>
<span aria-hidden="true" style="position: absolute; left: 165px; top: 97px; width: 60px; height: 14px; border-radius: 7px; background: var(--pri)"></span><span aria-hidden="true" style="position: absolute; left: 165px; top: 673px; width: 60px; height: 14px; border-radius: 7px; background: var(--pri)"></span>'''
    tools = f'''<div style="position: absolute; left: 0; right: 0; top: 696px; text-align: center" class="ty-cap">390 × 4 180 · 1,3 МБ · PNG</div>
<div style="position: absolute; left: 12px; right: 12px; bottom: 26px; display: flex; align-items: center; gap: 8px">
<div style="flex-grow: 1; height: 60px; border-radius: 30px; background: var(--sf-high); display: flex; align-items: center; justify-content: space-around; padding: 0 4px">
<button class="ib4" aria-label="Обрезать" style="color: var(--on-sf)">{ms('crop')}</button><button class="ib4" aria-label="Рисовать" style="color: var(--on-sf)">{ms('draw')}</button><button class="ib4" aria-label="Скрыть личное" style="background: var(--pri-c); color: var(--on-pri-c)">{ms('blur_on')}</button><button class="ib4" aria-label="Поделиться" style="color: var(--on-sf)">{ms('share')}</button></div>
<button class="btn fill" style="height: 60px; border-radius: 22px; padding: 0 20px">{ms('download', 's')}Сохранить</button></div>'''
    head = f'<div style="position: absolute; left: 8px; right: 8px; top: 44px; height: 56px; display: flex; align-items: center; justify-content: space-between"><button class="ib4" aria-label="Закрыть" style="color: var(--on-sf)">{ms("close")}</button><div style="width: 240px">{seg(["Экран", "Вся страница"], 1)}</div><span style="width: 48px"></span></div>'
    W('Screenshot', 'скриншот страницы', 'w-work t-dark', status() + head + shot + tools + handle())

    # Video: Vola pill on the player and the video sheet.
    pill_note = f'''<div style="position: absolute; right: 16px; top: 150px; width: 272px; border-radius: 20px; background: var(--inv-sf); color: var(--inv-on-sf); padding: 14px 16px 8px; box-shadow: var(--e3); display: flex; flex-direction: column; gap: 8px; z-index: 6">
<span aria-hidden="true" style="position: absolute; right: 30px; top: -6px; width: 14px; height: 14px; border-radius: 3px; background: var(--inv-sf); transform: rotate(45deg)"></span>
<span style="font-size: 15px; font-weight: 700">Это кнопки Vola, не сайта</span>
<span style="display: flex; gap: 10px; font-size: 13px; line-height: 18px; opacity: 0.85">{ms('picture_in_picture_alt', 'xs')}Картинка в картинке одним касанием</span>
<span style="display: flex; gap: 10px; font-size: 13px; line-height: 18px; opacity: 0.85">{ms('more_horiz', 'xs')}Звук в фоне, скорость, окно при выходе</span>
<span style="display: flex; justify-content: flex-end"><button class="btn" style="height: 36px; padding: 0 12px; color: var(--pri-c); font-size: 14px">Понятно</button></span></div>'''
    kino_page = kino().replace('box-shadow: none', '')
    kino_page = kino_page.replace('<span class="glass4 dk" style="position: absolute; right: 10px; top: 10px;', '<span class="glass4 dk" style="position: absolute; right: 10px; top: 10px; outline: 3px solid color-mix(in srgb, var(--pri) 70%, transparent); outline-offset: 1px;')
    W('VideoPill', 'кнопки Vola на видео', 'w-anime t-light aura', f'{status()}<div class="page-card" style="top: 40px; bottom: 92px">{kino_page}</div>{page_bar("kinoteka.example", "movie", 3, "picture_in_picture_alt")}{pill_note}{handle()}')

    big = lambda icon, label, on=False: f'<button style="height: 84px; border-radius: 24px; background: {"var(--pri)" if on else "var(--card)"}; color: {"var(--on-pri)" if on else "var(--on-sf)"}; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; font-size: 13px; line-height: 16px; font-weight: 600; text-align: center">{ms(icon, "f" if on else "")}{label}</button>'
    video_sheet = sheet(f'''<div style="display: flex; align-items: center; gap: 14px; padding: 0 4px"><span style="width: 72px; height: 44px; border-radius: 12px; background: linear-gradient(160deg, #1F1838 0%, #4A2A55 42%, #9A4E55 72%, #D9905C 100%); flex-shrink: 0"></span><span style="display: flex; flex-direction: column; gap: 2px"><span class="ty-title">Обзор первой серии</span><span class="ty-cap">Кинотека · 8:12 из 24:00</span></span></div>
<div style="display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px">{big('picture_in_picture_alt', 'Картинка<br>в картинке', True)}{big('headphones', 'Звук<br>в фоне')}{big('fullscreen', 'Во весь<br>экран')}</div>
<div style="border-radius: 24px; background: var(--card); padding: 12px 16px 14px; display: flex; flex-direction: column; gap: 10px"><div style="display: flex; align-items: center; gap: 16px">{ms('speed', '', 'color: var(--on-sf-v)')}<span style="flex-grow: 1; display: flex; flex-direction: column; gap: 2px"><span class="ty-label" style="font-size: 15px">Скорость</span><span class="ty-cap">Работает и там, где у плеера её нет</span></span><span class="ty-label" style="color: var(--pri)">1,25×</span></div>{seg(['0,75', '1', '1,25', '1,5', '2'], 2)}</div>
{group([row('open_in_new', 'Окно при выходе из Vola', 'Видео продолжится поверх других приложений', switch(True)), row('volume_off', 'Без звука на этом сайте', '', switch(False), h=56)])}''', gap=12)
    W('Video', 'лист «Видео»', 'w-anime t-light aura', f'{status()}<div class="page-card" style="top: 40px; bottom: 92px">{kino()}</div>{page_bar("kinoteka.example", "movie", 3, "picture_in_picture_alt")}{scrim()}{video_sheet}{handle()}')

    # Voice search.
    rings = ''.join(f'<span style="position: absolute; width: {d}px; height: {d}px; border-radius: 50%; background: color-mix(in srgb, var(--pri) {a}%, transparent)"></span>' for d, a in ((248, 6), (186, 10), (132, 18)))
    body = f'''{status()}
<div style="position: absolute; left: 8px; right: 16px; top: 44px; display: flex; align-items: center; justify-content: space-between"><button class="ib4" aria-label="Отменить" style="color: var(--on-sf)">{ms('close')}</button><button class="chip">{ms('language', 'xs')}Русский{ms('expand_more', 'xs')}</button></div>
<div style="position: absolute; left: 28px; right: 28px; top: 176px; display: flex; flex-direction: column; align-items: center; gap: 16px; text-align: center"><span class="chip" style="box-shadow: none; background: var(--pri-c); color: var(--on-pri-c)"><i style="width: 8px; height: 8px; border-radius: 4px; background: var(--pri)"></i>Слушаю</span><p style="font-size: 30px; line-height: 37px; font-weight: 700; letter-spacing: -0.015em">погода на Байкале в&nbsp;феврале <span style="color: var(--on-sf-v)">и толщина льда</span></p></div>
<div style="position: absolute; left: 0; right: 0; top: 400px; height: 248px; display: flex; align-items: center; justify-content: center">{rings}<button aria-label="Остановить запись" class="gem" style="position: relative; width: 96px; height: 96px; border-radius: 48px; box-shadow: 0 14px 32px color-mix(in srgb, var(--pri) 45%, transparent)">{ms('mic', 'f', 'font-size: 44px')}</button></div>
<p class="ty-cap" style="position: absolute; left: 0; right: 0; top: 664px; text-align: center; font-size: 13.5px">Коснитесь, чтобы закончить</p>
<div style="position: absolute; left: 20px; right: 20px; top: 704px; display: flex; gap: 10px">{btn('Текстом', 'out', 'keyboard', True)}{btn('Искать сейчас', 'tonal', 'search', True)}</div>
<span class="ty-cap" style="position: absolute; left: 0; right: 0; top: 784px; display: flex; align-items: center; justify-content: center; gap: 6px">{ms('shield_lock', 'xs', 'font-size: 16px')}Распознаётся на телефоне, запись не сохраняется</span>
{handle()}'''
    W('Voice', 'голосовой поиск', 'w-work t-light aura', body)

# ---------------------------------------------------------------- library

def ws_dot(ws):
    return f'<span class="w-{ws} t-light" style="display: inline-flex"><span class="gem" style="width: 20px; height: 20px; border-radius: 7px">{ms({"work": "work", "anime": "movie", "personal": "home"}[ws], "f", "font-size: 13px")}</span></span>'


def filter_chips(labels, on=0):
    return '<div style="display: flex; gap: 8px">' + ''.join(chip(l, k == on) for k, l in enumerate(labels)) + '</div>'


def g_library():
    days = [('Сегодня', [('С', '#2F6B5F', '#FFFFFF', 'Байкал зимой: как выбрать маршрут', 'north-guide.ru', '14:32', 'work'), ('П', '#D4F1EC', '#00665A', 'Прогноз толщины льда', 'ice-forecast.example.ru', '14:05', 'work'), ('Д', '#E6DEFF', '#4A3A9E', 'План запуска на октябрь', 'docs.example.com', '11:40', 'work')]),
            ('Вчера', [('Р', '#D8EEFF', '#1E5E8C', 'Расписание сезона: осень', 'calendar.example.com', '22:18', 'anime'), ('З', '#FFE1D6', '#9A3A1E', 'Спринт 42 · доска задач', 'tasks.example.com', '18:02', 'work'), ('К', '#FFDDEA', '#962F59', 'Каталог шрифтов с кириллицей', 'fonts.example.org', '09:47', 'personal')]),
            ('27 сентября', [('О', '#FFEBC2', '#6E4F00', 'Отчёт по метрикам', 'metrics.example.com', '16:20', 'work'), ('К', '#FFE1D6', '#9A3A1E', 'Обзор первой серии', 'kinoteka.example', '21:05', 'anime')])]
    sep = '<i style="height: 1px; background: var(--sf-high); margin: 0 16px 0 68px"></i>'
    blocks = ''.join(section(d) + '<div style="border-radius: 24px; background: var(--card); overflow: hidden">' + sep.join(
        list_item(l, bg, ink, t, h, f'<span style="display: flex; align-items: center; gap: 8px; padding-right: 8px"><span class="ty-cap" style="font-weight: 600">{tm}</span>{ws_dot(ws)}</span>') for l, bg, ink, t, h, tm, ws in items) + '</div>' for d, items in days)
    body = f'''{status()}{topbar('История', (('delete_sweep', 'Очистить'), ('more_vert', 'Ещё')))}
{screen(108, searchfield('Поиск по истории') + filter_chips(['Все', 'Работа', 'Аниме', 'Личное']) + blocks, gap=10)}
{fade(110)}{handle()}'''
    W('History', 'история', 'w-work t-light aura', body)

    folders = [('Путешествия', '12 сайтов', 'travel_explore', '#D4F1EC', '#00665A'), ('Чтение', '8 статей', 'menu_book', '#E6DEFF', '#4A3A9E'), ('Работа', '15 сайтов', 'work', '#D8EEFF', '#1E5E8C'), ('Идеи', '5 сайтов', 'auto_awesome', '#FFEBC2', '#6E4F00')]
    fg = ''.join(f'<a href="#" style="border-radius: 22px; background: var(--card); padding: 14px; display: flex; flex-direction: column; gap: 10px">{lead(i, bg, ink, True)}<span style="display: flex; flex-direction: column; gap: 2px"><span class="ty-label" style="font-size: 15px">{n}</span><span class="ty-cap">{c}</span></span></a>' for n, c, i, bg, ink in folders)
    marks = [('П', '#D4F1EC', '#00665A', 'Прогноз толщины льда', 'ice-forecast.example.ru'), ('С', '#2F6B5F', '#FFFFFF', 'Байкал зимой: как выбрать маршрут', 'north-guide.ru'),
             ('К', '#FFDDEA', '#962F59', 'Каталог шрифтов с кириллицей', 'fonts.example.org'), ('Л', '#FFE9B8', '#7C5800', 'Ледовые маршруты Байкала', 'north-guide.ru/routes')]
    ml = '<div style="border-radius: 24px; background: var(--card); overflow: hidden">' + sep.join(list_item(*m, f'<button class="ib4" aria-label="Ещё" style="width: 40px; height: 40px">{ms("more_vert", "s")}</button>') for m in marks) + '</div>'
    body = f'''{status()}{topbar('Избранное', (('search', 'Поиск'), ('sort', 'Сортировка')))}
{screen(108, f'<div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px">{fg}</div>' + section('Без папки') + ml, gap=10)}
{fade(120)}{fab('folder', 'Папка')}{handle()}'''
    W('Favorites', 'избранное', 'w-work t-light aura', body)

    active = f'''<div style="border-radius: 24px; background: var(--card); padding: 14px 8px 14px 14px; display: flex; flex-direction: column; gap: 10px; box-shadow: var(--e1)">
<div style="display: flex; align-items: center; gap: 14px">{lead('picture_as_pdf', '#FFE1D6', '#9A3A1E', True)}<span style="flex-grow: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px"><span class="ty-label" style="font-size: 15px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis">Карта маршрутов Байкала.pdf</span><span class="ty-cap">18,6 из 30 МБ · 20 с</span></span><button class="ib4" aria-label="Пауза">{ms('pause')}</button><button class="ib4" aria-label="Отменить">{ms('close')}</button></div>
<span style="height: 6px; border-radius: 3px; background: var(--sf-high); margin: 0 6px 4px 0"><i style="width: 62%; height: 6px; border-radius: 3px; background: var(--pri)"></i></span></div>'''
    files = [('description', 'Снаряжение для льда.pdf', 'PDF · 2,1 МБ · north-guide.ru', '#FFE1D6', '#9A3A1E', 'ok'), ('image', 'olkhon-grotto.jpg', 'Фото · 3,4 МБ · 14:10', '#D4F1EC', '#00665A', 'ok'),
             ('table_chart', 'Отчёт по метрикам.xlsx', 'Таблица · 86 КБ · 11:52', '#D3F0D9', '#2B6C3F', 'ok'), ('map', 'olkhon-route.gpx', 'Трек маршрута · 40 КБ · 09:12', '#FFE9B8', '#7C5800', 'ok'),
             ('gpp_maybe', 'ice-report.exe', 'Заблокирован: файл опасен', 'var(--err-c)', 'var(--on-err-c)', 'bad')]
    fl = '<div style="border-radius: 24px; background: var(--card); overflow: hidden">' + sep.join(row(i, t, d, (f'<button class="ib4" aria-label="Ещё" style="width: 40px">{ms("more_vert", "s")}</button>' if st == 'ok' else '<span class="chip" style="box-shadow: none; background: var(--err-c); color: var(--on-err-c)">Подробнее</span>'), bg, ink, fill=True) for i, t, d, bg, ink, st in files) + '</div>'
    body = f'''{status()}{topbar('Загрузки', (('search', 'Поиск'), ('folder', 'Папка загрузок')))}
{screen(108, active + filter_chips(['Все', 'Документы', 'Фото', 'Видео']) + section('Сегодня') + fl, gap=10)}
{handle()}'''
    W('Downloads', 'загрузки', 'w-work t-light aura', body)

    def snoozed(l, bg, ink, t, h, ws, wsname, when):
        return f'''<div style="border-radius: 24px; background: var(--card); padding: 14px; display: flex; flex-direction: column; gap: 12px">
<div style="display: flex; align-items: center; gap: 12px">{fav(l, bg, ink)}<span style="flex-grow: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px"><span class="ty-label" style="font-size: 15px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis">{t}</span><span class="ty-cap">{h}</span></span></div>
<div style="display: flex; align-items: center; gap: 8px"><span class="chip" style="box-shadow: none; background: var(--sf-high)">{ws_dot(ws)}{wsname}</span><span class="chip" style="box-shadow: none; background: var(--sf-high)">{ms('snooze', 'xs')}{when}</span><span style="flex-grow: 1"></span><button class="ib4" aria-label="Открыть сейчас" style="width: 40px; height: 40px; background: var(--sec-c); color: var(--on-sec-c)">{ms('open_in_new', 's')}</button></div></div>'''
    items = snoozed('О', '#FFEBC2', '#6E4F00', 'Отчёт по метрикам', 'metrics.example.com', 'work', 'Работа', 'Сегодня, 18:00') + snoozed('Р', '#D8EEFF', '#1E5E8C', 'Расписание сезона: осень', 'calendar.example.com', 'anime', 'Аниме', 'В субботу') + snoozed('К', '#FFDDEA', '#962F59', 'Каталог шрифтов с кириллицей', 'fonts.example.org', 'personal', 'Личное', '6 октября')
    hint = f'<div style="border-radius: 20px; box-shadow: inset 0 0 0 1px var(--ol-v); padding: 12px 14px; display: flex; gap: 10px; align-items: flex-start">{ms("info", "s", "color: var(--on-sf-v)")}<span class="ty-cap" style="font-size: 13.5px; line-height: 19px">Чтобы отложить вкладку, нажмите на неё в обзоре вкладок и держите, затем выберите «Отложить».</span></div>'
    body = f'''{status()}{topbar('Отложенные')}
{screen(108, '<span class="ty-body" style="color: var(--on-sf-v); padding: 0 4px">Вкладки вернутся в своё пространство в выбранное время</span>' + items + hint, gap=10)}
{handle()}'''
    W('Snoozed', 'отложенные вкладки', 'w-work t-light aura', body)


# ---------------------------------------------------------------- settings

def g_settings():
    def cat(i, t, d, bg, ink):
        return row(i, t, d, CHEV, bg, ink, fill=True, h=64)
    default = f'''<div style="border-radius: 24px; background: var(--pri-c); color: var(--on-pri-c); padding: 14px 12px 14px 16px; display: flex; align-items: center; gap: 14px"><span style="width: 44px; height: 44px; border-radius: 14px; background: #FFFFFF; display: flex; align-items: center; justify-content: center">{LOGO}</span><span style="flex-grow: 1; display: flex; flex-direction: column; gap: 2px"><span class="ty-label" style="font-size: 15px; color: var(--on-pri-c)">Сделать Vola основным</span><span style="font-size: 13px; opacity: 0.85">Открывать ссылки в Vola</span></span>{btn('Сделать', 'fill', '', h=40)}</div>'''
    g1 = group([cat('key', 'Пароли и автозаполнение', '128 паролей · 6 ключей доступа', '#E6DEFF', '#4A3A9E'), cat('shield', 'Приватность и защита', 'Строгая · только HTTPS', '#D3F0D9', '#2B6C3F'),
                cat('sync', 'Синхронизация', 'Без сервера · сквозное шифрование', '#D8EEFF', '#1E5E8C'), cat('upload_file', 'Перенести в Vola', 'Из Chrome, Firefox, Bitwarden', '#FFEBC2', '#6E4F00')])
    g2 = group([cat('palette', 'Внешний вид', 'Рама · тема авто', '#FFDDEA', '#962F59'), cat('workspaces', 'Пространства', '3 пространства', 'var(--pri-c)', 'var(--on-pri-c)'),
                cat('swipe', 'Вкладки и жесты', 'Группы, свайпы, автоархив', '#FFE1D6', '#9A3A1E'), cat('search', 'Поиск', 'DuckDuckGo · голос · подсказки', '#D4F1EC', '#00665A')])
    g3 = group([cat('extension', 'Расширения', 'uBlock Origin и ещё 3', '#E2E2E6', '#303036'), cat('translate', 'Перевод', 'На устройстве · 2 языка', '#DDE3FF', '#2F4AA8'), cat('download', 'Загрузки', 'Проверка файлов · папка', '#D3F0D9', '#2B6C3F')])
    body = f'''{status()}{topbar('Настройки', (('search', 'Поиск настроек'),))}
{screen(108, default + g1 + g2 + g3, gap=12)}
{fade(90)}{handle()}'''
    W('Settings', 'настройки', 'w-work t-light aura', body)

    from build_v4 import appearance
    appearance()
    import shutil
    here = __import__('pathlib').Path(__file__).resolve().parent.parent / 'canvas'
    shutil.copy(here / 'V4-Appearance.dc.html', here / 'W-SetAppearance.dc.html')
    s = (here / 'W-SetAppearance.dc.html').read_text(encoding='utf-8').replace('<div class="v4 w-work t-light"', '<div class="v4 w-work t-light aura"').replace('background: var(--sf-c)"', '"')
    (here / 'W-SetAppearance.dc.html').write_text(s, encoding='utf-8')

    level = lambda name, sub, on: f'<button role="radio" aria-checked="{"true" if on else "false"}" style="flex: 1 1 0; border-radius: 20px; padding: 12px 10px; background: {"var(--pri)" if on else "var(--card)"}; color: {"var(--on-pri)" if on else "var(--on-sf)"}; display: flex; flex-direction: column; gap: 2px; text-align: center"><span style="font-size: 14.5px; font-weight: 700">{name}</span><span style="font-size: 12px; opacity: 0.85">{sub}</span></button>'
    hero = f'''<div style="border-radius: 28px; background: var(--ok-c); color: var(--on-ok-c); padding: 18px; display: flex; gap: 14px; align-items: center"><span style="width: 52px; height: 52px; border-radius: 18px; background: var(--ok); color: #FFFFFF; display: flex; align-items: center; justify-content: center">{ms('gpp_good', 'f l')}</span><span style="display: flex; flex-direction: column; gap: 2px"><span class="ty-title" style="color: var(--on-ok-c)">Вы под защитой</span><span style="font-size: 13px; line-height: 18px">За неделю заблокировано 1 284 трекера на 96 сайтах</span></span></div>'''
    prot = f'{section("Защита от трекеров")}<div style="display: flex; gap: 6px">{level("Обычная", "меньше поломок", False)}{level("Строгая", "по умолчанию", True)}{level("Своя", "Filter Studio", False)}</div>'
    rows = group([row('lock', 'Только HTTPS', 'Всегда · предупреждать перед http', switch(True)), row('cookie', 'Скрывать cookie-баннеры', 'Отказываться от необязательных', switch(True)),
                  row('dns', 'Защищённый DNS', 'Выключен · выбрать сервер', CHEV), row('fingerprint', 'Защита от отпечатков', 'Сайтам сложнее вас узнать', switch(True)),
                  row('block', 'Просить не отслеживать (GPC)', 'Сигнал сайтам в каждом запросе', switch(True)), row('delete_sweep', 'Очищать при выходе', 'История и cookie · выбрать', CHEV)])
    W('SetPrivacy', 'приватность и защита', 'w-work t-light aura', f'{status()}{topbar("Приватность и защита")}{screen(108, hero + prot + rows, gap=12)}{fade(80)}{handle()}')

    eng = lambda l, bg, ink, n, sub, on: f'''<div role="radio" aria-checked="{"true" if on else "false"}" style="min-height: 60px; display: flex; align-items: center; gap: 14px; padding: 8px 16px">{fav(l, bg, ink, 36, 12, 15)}<span style="flex-grow: 1; display: flex; flex-direction: column; gap: 2px"><span class="ty-label" style="font-size: 15px">{n}</span><span class="ty-cap">{sub}</span></span><span style="width: 22px; height: 22px; border-radius: 11px; box-shadow: inset 0 0 0 2px {"var(--pri)" if on else "var(--ol)"}; display: flex; align-items: center; justify-content: center">{'<i style="width: 12px; height: 12px; border-radius: 6px; background: var(--pri)"></i>' if on else ''}</span></div>'''
    engines = '<div style="border-radius: 24px; background: var(--card); overflow: hidden">' + '<i style="height: 1px; background: var(--sf-high); margin: 0 16px 0 66px"></i>'.join([
        eng('D', '#FDE3D9', '#C2431E', 'DuckDuckGo', 'Не сохраняет запросы', True), eng('S', '#DDE3FF', '#2F4AA8', 'Startpage', 'Результаты Google без слежки', False),
        eng('B', '#FFEBC2', '#6E4F00', 'Brave Search', 'Собственный индекс', False), eng('Я', '#FFDDEA', '#962F59', 'Яндекс', 'Лучше для русскоязычных запросов', False),
        eng('X', '#D4F1EC', '#00665A', 'SearXNG', 'Свой сервер · search.example.org', False),
        f'<div style="min-height: 56px; display: flex; align-items: center; gap: 14px; padding: 8px 16px; color: var(--pri)"><span style="width: 36px; height: 36px; border-radius: 12px; background: var(--pri-c); color: var(--on-pri-c); display: flex; align-items: center; justify-content: center">{ms("add", "s")}</span><span class="ty-label" style="font-size: 15px; color: var(--pri)">Добавить поисковую систему</span></div>']) + '</div>'
    opts = group([row('', 'Подсказки поиска', 'Запрос уходит в выбранный поиск', switch(True)), row('', 'Искать во вкладках и истории', 'Только на устройстве', switch(True)), row('', 'Быстрые команды', 'Начните ввод с «>» — вкладки, пространства, настройки', CHEV)])
    W('SetSearch', 'поиск', 'w-work t-light aura', f'{status()}{topbar("Поиск")}{screen(108, section("Поисковая система") + engines + opts, gap=10)}{handle()}')

    view = lambda icon, name, on: f'<button role="radio" aria-checked="{"true" if on else "false"}" style="flex: 1 1 0; height: 84px; border-radius: 20px; background: {"var(--sec-c)" if on else "var(--card)"}; color: {"var(--on-sec-c)" if on else "var(--on-sf)"}; box-shadow: {"inset 0 0 0 2px var(--pri)" if on else "none"}; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6px; font-size: 13.5px; font-weight: 600">{ms(icon, "f" if on else "")}{name}</button>'
    tabs_rows = group([row('grid_view', 'Вид обзора', 'Сетка, список или стопка', value('Сетка') + CHEV), row('archive', 'Автоархив неактивных', 'Через 14 дней без открытия', value('14 дней') + CHEV), row('tab_group', 'Вкладки со страницы — в группу', 'Как Tab Islands в Opera', switch(True))])
    gest = group([row('swipe', 'Свайп по панели — соседняя вкладка', 'Влево и вправо по адресной панели', switch(True)), row('arrow_upward', 'Свайп вверх — обзор вкладок', 'Вверх по адресной панели', switch(True)),
                  row('dock_to_bottom', 'Прятать панель при прокрутке', 'Остаётся капсула с адресом', switch(True)), row('bolt', 'Тактильная отдача', 'Лёгкий отклик на жестах', switch(True))])
    illus = f'''<div class="aura" style="position: relative; height: 140px; border-radius: 28px; overflow: hidden">
<div style="position: absolute; left: 50%; top: 18px; width: 150px; height: 200px; margin-left: -75px; border-radius: 22px; background: #FFFFFF; box-shadow: var(--e2); padding: 12px; display: flex; flex-direction: column; gap: 6px"><i style="height: 8px; width: 60%; border-radius: 4px; background: #D9DEE0"></i><i style="height: 44px; border-radius: 10px; background: #E8EFF1"></i><i style="height: 6px; width: 90%; border-radius: 3px; background: #E3E8E6"></i><i style="height: 6px; width: 70%; border-radius: 3px; background: #E3E8E6"></i></div>
<div style="position: absolute; left: 50%; top: 96px; width: 132px; height: 30px; margin-left: -66px; border-radius: 15px; background: color-mix(in srgb, var(--sf-lowest) 90%, transparent); box-shadow: var(--e2); display: flex; align-items: center; gap: 6px; padding: 0 5px"><i class="gem" style="width: 20px; height: 20px; border-radius: 7px"></i><i style="flex-grow: 1; height: 6px; border-radius: 3px; background: #D9DEE0"></i></div>
<span style="position: absolute; left: 44px; top: 96px; color: var(--pri)">{ms('arrow_back', 'l')}</span><span style="position: absolute; right: 44px; top: 96px; color: var(--pri)">{ms('arrow_forward', 'l')}</span>
<span style="position: absolute; left: 50%; top: 58px; margin-left: -14px; color: var(--pri)">{ms('arrow_upward', 'l')}</span></div>'''
    views = '<div style="display: flex; gap: 8px">' + view('grid_view', 'Сетка', True) + view('layers', 'Список', False) + view('tab', 'Стопка', False) + '</div>'
    W('SetTabs', 'вкладки и жесты', 'w-work t-light aura', f'{status()}{topbar("Вкладки и жесты")}{screen(108, illus + section("Жесты") + gest + section("Обзор вкладок") + tabs_rows, gap=10)}{handle()}')

    ubo = f'''<div style="border-radius: 28px; background: var(--card); padding: 16px; display: flex; flex-direction: column; gap: 14px; box-shadow: var(--e1)"><div style="display: flex; align-items: center; gap: 14px"><span style="width: 52px; height: 52px; border-radius: 18px; background: #7A1F1F; color: #FFFFFF; display: flex; align-items: center; justify-content: center">{ms('shield', 'f')}</span><span style="flex-grow: 1; display: flex; flex-direction: column; gap: 2px"><span class="ty-title">uBlock Origin</span><span class="ty-cap">Блокировщик рекламы · встроен</span></span>{switch(True)}</div><div style="display: flex; gap: 8px">{btn('Настроить', 'tonal', 'tune', h=40)}{btn('Разрешения', 'out', '', h=40)}</div></div>'''
    exts = group([row('dark_mode', 'Dark Reader', 'Работает в пространстве «Аниме»', switch(True), '#E6DEFF', '#4A3A9E', fill=True), row('play_arrow', 'SponsorBlock', 'Пропускает рекламные вставки в видео', switch(True), '#D4F1EC', '#00665A', fill=True),
                  row('schedule', 'LeechBlock NG', 'Ограничивает отвлекающие сайты в «Работе»', switch(True), '#FFE9B8', '#7C5800', fill=True)])
    W('SetExtensions', 'расширения', 'w-work t-light aura', f'''{status()}{topbar("Расширения")}{screen(108, ubo + section("Установлены") + exts + group([row('code', 'Пользовательские скрипты', '2 скрипта', CHEV, 'var(--sf-high)', 'var(--on-sf-v)')]) + btn('Найти расширения Firefox', 'fill', 'search', h=52) + '<span class="ty-cap" style="text-align: center; padding: 0 16px">Каталог addons.mozilla.org. В сборке на System WebView расширений нет.</span>', gap=10)}{handle()}''')

    sync_hero = f'''<div style="border-radius: 28px; background: var(--card); padding: 18px; display: flex; flex-direction: column; gap: 12px; box-shadow: var(--e1)"><div style="display: flex; align-items: center; gap: 14px">{lead('sync', 'var(--ok-c)', 'var(--on-ok-c)')}<span style="display: flex; flex-direction: column; gap: 2px"><span class="ty-title">Связка из 2 устройств</span><span class="ty-cap">5 минут назад · напрямую по Wi-Fi</span></span></div><span style="display: flex; gap: 8px; align-items: flex-start" class="ty-cap">{ms('shield_lock', 'xs', 'font-size: 16px; color: var(--ok)')}<span style="font-size: 13px; line-height: 18px">Сквозное шифрование. Своего сервера нет: данные идут напрямую или через вашу папку.</span></span></div>'''
    devs = group([row('smartphone' if False else 'devices', 'Этот телефон', 'Vola · пространство «Работа»', '', 'var(--pri-c)', 'var(--on-pri-c)'), row('desktop_windows', 'Планшет', 'Vola · через папку Syncthing', CHEV, 'var(--sf-high)', 'var(--on-sf-v)'),
                  f'<div style="min-height: 56px; display: flex; align-items: center; gap: 16px; padding: 8px 16px; color: var(--pri)">{ms("add", "", "width: 40px; text-align: center")}<span class="ty-label" style="font-size: 15px; color: var(--pri)">Добавить устройство</span></div>'])
    what = group([row('tab', 'Открытые вкладки', '', switch(True), h=52), row('workspaces', 'Пространства и Essentials', '', switch(True), h=52), row('bookmarks', 'Избранное', '', switch(True), h=52), row('key', 'Пароли и ключи доступа', '', switch(True), h=52), row('history', 'История', '', switch(False), h=52)])
    W('SetSync', 'синхронизация', 'w-work t-light aura', f'{status()}{topbar("Синхронизация")}{screen(108, sync_hero + section("Устройства") + devs + section("Что синхронизировать") + what, gap=10)}{handle()}')

# ---------------------------------------------------------------- zen features

def g_zen():
    # Compact mode: page fills the screen; a thin workspace strip reveals the bar.
    page = article_long().replace('padding: 18px 20px 0;', 'padding: 52px 20px 0;')
    hint = f'''<div class="glass4" style="position: absolute; left: 50%; bottom: 64px; width: 312px; margin-left: -156px; border-radius: 22px; padding: 12px 14px; display: flex; align-items: center; gap: 12px; z-index: 4">{lead('fullscreen_exit', 'var(--pri-c)', 'var(--on-pri-c)')}<span style="display: flex; flex-direction: column; gap: 2px"><span class="ty-label" style="font-size: 15px">Компактный режим</span><span class="ty-cap">Прокрутите вверх или коснитесь полоски</span></span></div>
<button aria-label="Показать панель" style="position: absolute; left: 50%; bottom: 22px; width: 72px; height: 24px; margin-left: -36px; display: flex; align-items: center; justify-content: center; z-index: 4"><i style="width: 56px; height: 6px; border-radius: 3px; background: var(--pri); box-shadow: 0 0 0 4px color-mix(in srgb, var(--pri) 18%, transparent)"></i></button>'''
    W('Compact', 'компактный режим', 'w-work t-light', f'<div style="position: absolute; inset: 0">{page}</div><div aria-hidden="true" style="position: absolute; left: 0; right: 0; bottom: 0; height: 200px; background: linear-gradient(180deg, rgba(255,255,255,0) 0%, #FFFFFF 62%)"></div>{status()}{hint}{handle()}')

    # Split view: two page cards in the frame with a draggable divider.
    top = doc_page()
    board_page = f'''<div style="position: absolute; inset: 0; background: #FFFFFF">
<div style="height: 48px; padding: 0 16px; display: flex; align-items: center; gap: 10px; border-bottom: 1px solid #F0EDF3"><span class="fav" style="width: 24px; height: 24px; border-radius: 8px; background: #FFE1D6; color: #9A3A1E; font-size: 12px">З</span><span style="font-size: 14px; font-weight: 700; color: #1C1B1F">Спринт 42</span></div>
<div style="padding: 14px; display: grid; grid-template-columns: 1fr 1fr; gap: 10px">
<div style="display: flex; flex-direction: column; gap: 8px"><span class="ty-over">В работе</span>{"".join(f'<div style="border-radius: 14px; background: {c}; padding: 10px; display: flex; flex-direction: column; gap: 8px"><span style="font-size: 13px; font-weight: 600; color: #1C1B1F">{t}</span><span style="font-size: 11px; font-weight: 600; color: rgba(28,27,31,0.6)">{d}</span></div>' for t, d, c in (('Экран оплаты', 'до 3 окт.', '#FFE1D6'), ('Онбординг', 'до 5 окт.', '#E6DEFF')))}</div>
<div style="display: flex; flex-direction: column; gap: 8px"><span class="ty-over">Готово</span>{"".join(f'<div style="border-radius: 14px; background: {c}; padding: 10px; display: flex; flex-direction: column; gap: 8px"><span style="font-size: 13px; font-weight: 600; color: #1C1B1F">{t}</span><span style="font-size: 11px; font-weight: 600; color: rgba(28,27,31,0.6)">{d}</span></div>' for t, d, c in (('Поиск', '29 сент.', '#D4F1EC'), ('Профиль', '27 сент.', '#D8EEFF')))}</div></div></div>'''
    divider = f'<div style="position: absolute; left: 0; right: 0; top: 426px; height: 16px; display: flex; align-items: center; justify-content: center; z-index: 3"><i style="width: 48px; height: 5px; border-radius: 3px; background: var(--on-sf); opacity: 0.45"></i></div>'
    bar = f'''<div style="position: absolute; left: 8px; right: 8px; bottom: 28px; height: 56px; display: flex; align-items: center; gap: 4px">
<button aria-label="Выйти из Split View" style="width: 48px; height: 48px; display: flex; align-items: center; justify-content: center">{gem('splitscreen', 40, 14, 's')}</button>
<span style="flex-grow: 1; height: 48px; border-radius: 24px; background: color-mix(in srgb, var(--sf-lowest) 80%, transparent); display: flex; flex-direction: column; align-items: center; justify-content: center"><span style="font-size: 15px; font-weight: 600">docs.example.com</span><span class="ty-cap" style="font-size: 11.5px">Верхняя панель активна</span></span>
<button class="ib4" aria-label="Поменять местами" style="color: var(--on-sf)">{ms('swap_vert')}</button><button class="ib4" aria-label="Меню" style="width: 40px; color: var(--on-sf)">{ms('more_vert')}</button></div>'''
    body = f'''{status()}<div class="page-card" style="top: 40px; height: 384px; box-shadow: 0 0 0 2.5px var(--pri), var(--e1)">{top}</div>{divider}<div class="page-card" style="top: 444px; bottom: 92px">{board_page}</div>{bar}{handle()}'''
    W('Split', 'Split View', 'w-work t-light aura', body)

    # Glance: floating preview over the dimmed page.
    from build_v4 import forecast_page as fp
    glance = f'''<div aria-hidden="true" style="position: absolute; left: 50%; top: 50px; width: 40px; height: 5px; margin-left: -20px; border-radius: 3px; background: rgba(255,255,255,0.9); z-index: 5"></div>
<div style="position: absolute; left: 12px; right: 12px; top: 62px; bottom: 104px; border-radius: 30px; overflow: hidden; background: #FFFFFF; box-shadow: var(--e3); z-index: 4">
<div style="height: 52px; display: flex; align-items: center; gap: 8px; padding: 0 6px 0 16px; background: var(--sf-low); border-bottom: 1px solid var(--sf-high)">{ms('lock', 'xs f', 'color: var(--ok)')}<span style="flex-grow: 1; font-size: 14px; font-weight: 600">ice-forecast.example.ru</span><button class="ib4" aria-label="Закрыть" style="width: 40px; height: 40px">{ms('close', 's')}</button></div>
<div style="position: absolute; left: 0; right: 0; top: 52px; bottom: 52px">{fp().replace('<div style="height: 56px; padding: 0 18px;', '<div style="display: none; height: 56px; padding: 0 18px;')}</div>
<div style="position: absolute; left: 12px; right: 12px; bottom: 10px; height: 36px; border-radius: 14px; background: var(--sf-low); display: flex; align-items: center; gap: 8px; padding: 0 12px; font-size: 12.5px; font-weight: 600; color: var(--on-sf-v)">{ms('visibility_off', 'xs', 'font-size: 16px')}Не попадёт в историю, пока не откроете</div></div>
<div style="position: absolute; left: 12px; right: 12px; bottom: 30px; display: flex; gap: 10px; z-index: 4">{btn('Открыть во вкладке', 'fill', 'open_in_new', True, 56)}<button class="ib4" aria-label="Открыть рядом" style="width: 56px; height: 56px; border-radius: 28px; background: var(--sf-lowest); color: var(--on-sf); box-shadow: var(--e2)">{ms('splitscreen')}</button></div>'''
    body = f'{status()}<div class="page-card" style="top: 40px; bottom: 92px; filter: blur(3px)">{article()}</div>{page_bar()}<div aria-hidden="true" style="position: absolute; inset: 0; background: rgba(8, 14, 16, 0.4); z-index: 3"></div>{glance}{handle()}'
    W('Glance', 'Glance', 'w-work t-light aura', body)

    # Themes: live preview, presets, accent, radius, density.
    presets = [('Vola', 'linear-gradient(135deg, #8DEBFF, #D0E4FF)', True), ('Лёд', 'linear-gradient(135deg, #DCEFF5, #A9D8E6)', False), ('Сумерки', 'linear-gradient(135deg, #2B2140, #7A4B8C)', False), ('Бумага', 'linear-gradient(135deg, #F7F1E6, #E6D8BE)', False), ('Моно', 'linear-gradient(135deg, #F2F3F4, #9AA0A3)', False)]
    pr = ''.join(f'<button style="display: flex; flex-direction: column; align-items: center; gap: 6px"><span style="width: 58px; height: 58px; border-radius: 20px; background: {g}; box-shadow: {"0 0 0 3px var(--sf), 0 0 0 5.5px var(--pri)" if on else "var(--e1)"}"></span><span class="ty-cap" style="font-weight: 600">{n}</span></button>' for n, g, on in presets)
    preview = f'''<div class="aura" style="height: 230px; border-radius: 28px; display: flex; align-items: flex-end; justify-content: center; overflow: hidden">
<div style="width: 180px; height: 210px; border-radius: 26px 26px 0 0; background: #FFFFFF; box-shadow: var(--e2); padding: 14px 12px; display: flex; flex-direction: column; gap: 8px; position: relative">
<i style="height: 9px; width: 70%; border-radius: 5px; background: #2B3432; opacity: 0.8"></i><span style="height: 60px; border-radius: 12px; overflow: hidden">{BAIKAL_T.replace('id="t', 'id="p').replace('url(#t', 'url(#p')}</span><i style="height: 6px; width: 90%; border-radius: 3px; background: #E3E8E6"></i><i style="height: 6px; width: 76%; border-radius: 3px; background: #E3E8E6"></i>
<span style="position: absolute; left: 8px; right: 8px; bottom: 10px; height: 34px; border-radius: 17px; background: var(--sf-c); display: flex; align-items: center; gap: 6px; padding: 0 5px"><i class="gem" style="width: 24px; height: 24px; border-radius: 9px"></i><i style="flex-grow: 1; height: 24px; border-radius: 12px; background: #FFFFFF"></i></span></div></div>'''
    body = f'''{status()}{topbar('Темы')}
{screen(108, preview + f'<div style="display: flex; justify-content: space-between; padding: 4px 2px">{pr}</div>' + '<div style="border-radius: 24px; background: var(--card); padding: 14px 16px 16px; display: flex; flex-direction: column; gap: 10px"><span style="display: flex; justify-content: space-between"><span class="ty-label" style="font-size: 15px">Акцент</span><span class="ty-cap" style="font-weight: 600">Как у пространства</span></span>' + color_dots() + '<span class="ty-label" style="font-size: 15px; padding-top: 4px">Скругления</span>' + seg(['Строгие', 'Мягкие', 'Круглые'], 2) + '<span class="ty-label" style="font-size: 15px; padding-top: 4px">Плотность</span>' + seg(['Плотно', 'Обычно', 'Просторно'], 1) + '</div>', gap=12)}
{handle()}'''
    W('Themes', 'темы', 'w-work t-light aura', body)


# ---------------------------------------------------------------- system

def g_system():
    W('Splash', 'заставка', 'w-work t-dark', f'<div style="position: absolute; inset: 0; background: #000000"></div>{status(light=True)}<div style="position: absolute; left: 50%; top: 50%; width: 160px; height: 160px; margin: -80px 0 0 -80px; display: flex; align-items: center; justify-content: center">{LOGO.replace("width: 32px; height: 32px", "width: 132px; height: 132px")}</div><span style="position: absolute; left: 0; right: 0; bottom: 64px; text-align: center; font-size: 22px; font-weight: 700; letter-spacing: -0.01em; color: #E8EEF0">Vola</span>{handle(light=True)}')

    # Welcome: three mini phones showing the workspaces.
    phone = lambda cls, rot, x, y, z, content: f'<div class="{cls} aura" style="position: absolute; left: {x}px; top: {y}px; width: 150px; height: 300px; border-radius: 30px; transform: rotate({rot}deg); box-shadow: var(--e3); overflow: hidden; z-index: {z}"><div style="position: absolute; left: 4px; right: 4px; top: 14px; bottom: 40px; border-radius: 20px; background: #FFFFFF; overflow: hidden">{content}</div><div style="position: absolute; left: 8px; right: 8px; bottom: 8px; height: 26px; display: flex; gap: 4px; align-items: center"><i class="gem" style="width: 22px; height: 22px; border-radius: 8px"></i><i style="flex-grow: 1; height: 22px; border-radius: 11px; background: rgba(255,255,255,0.85)"></i></div></div>'
    scaled = lambda html, sc: f'<div style="width: {int(142 / sc)}px; height: {int(246 / sc)}px; transform: scale({sc}); transform-origin: 0 0; position: relative">{html}</div>'
    hero = (phone('w-anime t-light', -9, 30, 92, 1, scaled(kino(), 0.37)) + phone('w-personal t-light', 9, 210, 100, 1, scaled(watchlist_page(), 0.37)) + phone('w-work t-light', 0, 120, 70, 2, scaled(article(), 0.37)))
    body = f'''<div class="aura" style="position: absolute; inset: 0"></div>{status()}{hero}
<div style="position: absolute; left: 24px; right: 24px; top: 430px; display: flex; flex-direction: column; gap: 12px"><span class="ty-display">Браузер, который не&nbsp;мешает</span><span class="ty-body" style="color: var(--on-sf-v)">Страница на весь экран, пространства для работы и отдыха, защита от слежки без настроек.</span>
<div style="display: flex; gap: 6px; padding-top: 6px"><i style="width: 22px; height: 6px; border-radius: 3px; background: var(--pri)"></i><i style="width: 6px; height: 6px; border-radius: 3px; background: var(--ol-v)"></i><i style="width: 6px; height: 6px; border-radius: 3px; background: var(--ol-v)"></i></div></div>
<div style="position: absolute; left: 20px; right: 20px; bottom: 36px; display: flex; flex-direction: column; gap: 10px">{btn('Начать', 'fill', '', h=56)}{btn('Перенести из другого браузера', 'out', 'upload_file', h=52)}</div>{handle()}'''
    W('Welcome', 'приветствие', 'w-work t-light', body)

    AUTO_HALF = ('<span style="position: absolute; inset: 0; clip-path: polygon(58% 0, 100% 0, 100% 100%, 42% 100%); background: #000000; padding: 12px 10px; display: flex; flex-direction: column; gap: 6px">'
                 '<i style="height: 7px; width: 70%; border-radius: 4px; background: #2A2F31"></i><i style="height: 36px; border-radius: 8px; background: #1B2A30"></i></span>')

    def mini_theme(name, bg, line, block, bar, on, auto=False):
        ring = '0 0 0 3px var(--sf-c), 0 0 0 5.5px var(--pri)' if on else 'var(--e1)'
        half = AUTO_HALF if auto else ''
        return (f'<button role="radio" aria-checked="{"true" if on else "false"}" style="display: flex; flex-direction: column; align-items: center; gap: 8px">'
                f'<span style="position: relative; width: 100%; height: 132px; border-radius: 22px; background: {bg}; box-shadow: {ring}; padding: 12px 10px; display: flex; flex-direction: column; gap: 6px; overflow: hidden">'
                f'<i style="height: 7px; width: 70%; border-radius: 4px; background: {line}"></i><i style="height: 36px; border-radius: 8px; background: {block}"></i><i style="height: 6px; width: 86%; border-radius: 3px; background: {line}"></i>'
                f'<i style="position: absolute; left: 8px; right: 8px; bottom: 8px; height: 20px; border-radius: 10px; background: {bar}"></i>{half}</span>'
                f'<span style="font-size: 14px; font-weight: 600">{name}</span></button>')
    themes = '<div style="display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px">' + mini_theme('Светлая', '#FFFFFF', '#E1E6E7', '#DCE9EF', '#EAEFF0', False) + mini_theme('Тёмная', '#000000', '#2A2F31', '#1B2A30', '#141B1C', False) + mini_theme('Авто', '#FFFFFF', '#E1E6E7', '#DCE9EF', '#EAEFF0', True, True) + '</div>'
    opts = group([row('gpp_good', 'Строгая защита от трекеров', 'Включена', switch(True)), row('lock', 'Только HTTPS', 'Включено', switch(True)), row('swipe', 'Показать жесты', 'Короткое обучение, 30 секунд', switch(True))])
    body = f'''{status()}
<div style="position: absolute; left: 20px; right: 20px; top: 56px; display: flex; flex-direction: column; gap: 16px"><div style="display: flex; gap: 6px">{"".join(f'<i style="flex: 1; height: 4px; border-radius: 2px; background: {c}"></i>' for c in ("var(--pri)", "var(--pri)", "var(--ol-v)"))}</div><span class="ty-head">Настройте под себя</span>{themes}{opts}
<div style="border-radius: 24px; background: var(--card); padding: 14px 12px 14px 16px; display: flex; align-items: center; gap: 12px"><span style="flex-grow: 1; display: flex; flex-direction: column; gap: 2px"><span class="ty-label" style="font-size: 15px">Сделать Vola основным браузером</span><span class="ty-cap">Можно позже в настройках</span></span>{btn('Сделать', 'tonal', '', h=40)}</div></div>
<div style="position: absolute; left: 20px; right: 20px; bottom: 36px">{btn('Далее', 'fill', '', h=56).replace('class="btn fill" style="', 'class="btn fill" style="width: 100%; ')}</div>{handle()}'''
    W('Setup', 'первая настройка', 'w-work t-light aura', body)

    def state_screen(icon, tone, ink, title, text, primary, secondary, extra=''):
        return f'''<div style="position: absolute; left: 28px; right: 28px; top: 170px; display: flex; flex-direction: column; align-items: center; gap: 14px; text-align: center">
<span style="width: 84px; height: 84px; border-radius: 30px; background: {tone}; color: {ink}; display: flex; align-items: center; justify-content: center; margin-bottom: 6px">{ms(icon, 'f', 'font-size: 44px')}</span>
<span class="ty-head">{title}</span><span class="ty-body" style="color: var(--on-sf-v)">{text}</span>{extra}</div>
<div style="position: absolute; left: 20px; right: 20px; bottom: 36px; display: flex; flex-direction: column; gap: 10px">{primary}{secondary}</div>'''
    card = lambda content: f'{status()}<div class="page-card" style="top: 40px; bottom: 92px; background: var(--sf)">{content}</div>'
    off = state_screen('wifi_off', 'var(--sf-high)', 'var(--on-sf-v)', 'Нет подключения', 'Страница откроется сама, как только сеть вернётся. Проверьте Wi-Fi или мобильный интернет.', btn('Повторить', 'fill', 'refresh', h=52), btn('Настройки сети', 'out', '', h=52)).replace('bottom: 36px', 'bottom: 24px')
    W('Offline', 'нет сети', 'w-work t-light aura', card(off) + page_bar() + handle())

    http = state_screen('no_encryption', 'var(--warn-c)', 'var(--on-warn-c)', 'У сайта нет защищённой версии', 'Vola открывает сайты только по HTTPS. <b style="color: var(--on-sf)">neverssl.com</b> не поддерживает защищённое соединение: всё, что вы увидите или введёте, могут прочитать или подменить по пути.',
                        btn('Вернуться назад', 'fill', '', h=52), '<button class="btn" style="height: 48px; color: var(--on-sf-v)">Всё равно открыть по HTTP</button>',
                        f'<div style="border-radius: 20px; background: var(--warn-c); color: var(--on-warn-c); padding: 12px 14px; display: flex; gap: 10px; align-items: flex-start">{ms("warning", "s f")}<span style="font-size: 14px; font-weight: 600; line-height: 19px">Не вводите на таком сайте пароли и данные карт.</span></div>').replace('bottom: 36px', 'bottom: 24px')
    W('HttpsOnly', 'только HTTPS', 'w-work t-light aura', card(http) + page_bar('neverssl.com') + handle())

    lock = f'''<div class="aura" style="position: absolute; inset: 0; filter: saturate(1.1)"></div>{status()}
<div style="position: absolute; left: 24px; right: 24px; top: 190px; display: flex; flex-direction: column; align-items: center; gap: 16px; text-align: center"><span style="position: relative">{gem('work', 84, 30, 'l')}<span style="position: absolute; right: -8px; bottom: -8px; width: 36px; height: 36px; border-radius: 18px; background: var(--card); color: var(--on-sf); box-shadow: var(--e2); display: flex; align-items: center; justify-content: center">{ms('lock', 'xs f')}</span></span><span class="ty-head">Пространство «Работа» заблокировано</span><span class="ty-body" style="color: var(--on-sf-v)">Вкладки этого пространства скрыты. Подтвердите, что это вы.</span></div>
<div style="position: absolute; left: 50%; top: 520px; width: 96px; height: 96px; margin-left: -48px; border-radius: 48px; background: color-mix(in srgb, var(--pri) 14%, transparent); display: flex; align-items: center; justify-content: center"><span style="width: 72px; height: 72px; border-radius: 36px; background: var(--card); color: var(--pri); box-shadow: var(--e2); display: flex; align-items: center; justify-content: center">{ms('fingerprint', '', 'font-size: 40px')}</span></div>
<div style="position: absolute; left: 20px; right: 20px; bottom: 36px; display: flex; flex-direction: column; gap: 10px">{btn('Разблокировать', 'fill', 'fingerprint', h=56)}<button class="btn" style="height: 48px; color: var(--on-sf-v)">Перейти в другое пространство</button></div>{handle()}'''
    W('Locked', 'пространство заблокировано', 'w-work t-light', lock)


# ---------------------------------------------------------------- security (new)

def g_security():
    danger = f'''<div style="position: absolute; left: 24px; right: 24px; top: 130px; display: flex; flex-direction: column; gap: 16px">
<span style="width: 76px; height: 76px; border-radius: 26px; background: #FFFFFF; color: #B3261E; display: flex; align-items: center; justify-content: center">{ms('dangerous', 'f', 'font-size: 42px')}</span>
<span class="ty-head" style="color: #FFFFFF">Сайт выдаёт себя за другой</span>
<span class="ty-body" style="color: rgba(255,255,255,0.88)"><b style="color: #FFFFFF">bank-exarnple.ru</b> похож на ваш банк, но это другой адрес. Такие страницы крадут пароли и данные карт.</span>
<div style="border-radius: 20px; background: rgba(255,255,255,0.12); padding: 12px 14px; display: flex; flex-direction: column; gap: 8px; color: #FFFFFF"><span style="display: flex; gap: 10px; font-size: 14px; line-height: 19px">{ms('key', 's')}Vola не будет заполнять здесь пароли</span><span style="display: flex; gap: 10px; font-size: 14px; line-height: 19px">{ms('verified', 's')}Настоящий сайт: bank.example.ru</span></div></div>
<div style="position: absolute; left: 20px; right: 20px; bottom: 36px; display: flex; flex-direction: column; gap: 10px"><button class="btn" style="height: 56px; border-radius: 28px; background: #FFFFFF; color: #8C1D18">Вернуться в безопасное место</button><button class="btn" style="height: 48px; color: rgba(255,255,255,0.9)">Подробнее и всё равно открыть</button></div>'''
    W('DangerousSite', 'опасный сайт', 'w-work t-dark', f'<div style="position: absolute; inset: 0; background: linear-gradient(180deg, #8C1D18 0%, #5C0F0C 100%)"></div>{status(light=True)}{danger}{handle(light=True)}')

    check = sheet(f'''<div style="display: flex; align-items: center; gap: 14px; padding: 0 4px">{lead('gpp_maybe', 'var(--warn-c)', 'var(--on-warn-c)', True)}<span style="display: flex; flex-direction: column; gap: 2px"><span class="ty-title">Проверьте файл перед открытием</span><span class="ty-cap">ice-report.apk · 14,2 МБ · с files.example.net</span></span></div>
<div style="border-radius: 24px; background: var(--card); overflow: hidden">{row('apps', 'Это приложение, а не документ', 'Установить его может только Android — не Vola', '', 'var(--warn-c)', 'var(--on-warn-c)')}{row('public', 'Сайт впервые открыт сегодня', 'Вы пришли по ссылке из письма', '', 'var(--sf-high)', 'var(--on-sf-v)')}{row('verified', 'Подпись разработчика не найдена', '', '', 'var(--sf-high)', 'var(--on-sf-v)')}</div>
<span class="ty-cap" style="padding: 0 6px; font-size: 13px; line-height: 18px">Проверка идёт на телефоне по базе опасных файлов: файл никуда не отправляется.</span>
<div style="display: flex; gap: 8px">{btn('Удалить', 'fill', 'delete', True)}{btn('Сохранить', 'out', '', True)}</div>''', gap=12)
    W('DownloadCheck', 'проверка загрузки', 'w-work t-light aura', f'{status()}<div class="page-card" style="top: 40px; bottom: 92px">{article_long()}</div>{page_bar()}{scrim()}{check}{handle()}')

# ---------------------------------------------------------------- dark

def g_dark():
    from build_v4 import tabs_body
    body = tabs_body('A')
    # Sites follow the dark theme, as on the dark page screen: the article thumbnail darkens too.
    for a, b in (('min-height: 0; background: #FFFFFF', 'min-height: 0; background: var(--sf-lowest)'),
                 ('background: #FFFFFF; padding: 10px', 'background: var(--sf-lowest); padding: 10px'),
                 ('background: #E3E8E6', 'background: var(--sf-high)'), ('background: #15201D', 'background: var(--on-sf)'),
                 ('border-radius: 22px; background: var(--sf-lowest)', 'border-radius: 22px; background: var(--card)')):
        body = body.replace(a, b)
    W('TabsDark', 'обзор вкладок (тёмная)', 'w-work t-dark aura', body)
    W('PageDark', 'страница (тёмная)', 'w-work t-dark aura', frame_screen(article(dark=True), page_bar()))


# ---------------------------------------------------------------- tablet

def tab_row(l, bg, ink, title, cur=False, indent=12):
    style = 'background: var(--card); box-shadow: var(--e1);' if cur else ''
    weight = 600 if cur else 500
    return (f'<a href="#" style="height: 44px; border-radius: 14px; {style} display: flex; align-items: center; gap: 10px; padding: 0 8px 0 {indent}px">{fav(l, bg, ink, 22, 7, 11)}'
            f'<span style="flex-grow: 1; min-width: 0; font-size: 14px; font-weight: {weight}; white-space: nowrap; overflow: hidden; text-overflow: ellipsis">{title}</span></a>')


def rail_gem(cls, icon, label):
    return f'<button aria-label="{label}" class="{cls}" style="width: 44px; height: 44px; display: flex; align-items: center; justify-content: center; background: none">{gem(icon, 30, 10, "xs")}</button>'


def sidebar(width=300):
    def ess_tile_s(n, l, b, i):
        ring = 'box-shadow: 0 0 0 2px var(--pri);' if n == 'Документы' else ''
        return f'<a href="#" aria-label="{n}" style="height: 52px; border-radius: 16px; background: color-mix(in srgb, var(--card) 80%, transparent); {ring} display: flex; align-items: center; justify-content: center">{fav(l, b, i, 28, 9, 13)}</a>'
    ess = ''.join(ess_tile_s(*e) for e in ESS + [('Код', 'К', '#E2E2E6', '#303036')])
    tabs = ''.join([tab_row('Д', '#E6DEFF', '#4A3A9E', 'План запуска на октябрь', True), tab_row('З', '#FFE1D6', '#9A3A1E', 'Спринт 42 · доска задач'),
                    tab_row('О', '#FFEBC2', '#6E4F00', 'Отчёт по метрикам'), tab_row('М', '#FFDDEA', '#962F59', 'Макеты главного экрана')])
    grp = (f'<button style="height: 40px; display: flex; align-items: center; gap: 10px; padding: 0 12px; font-size: 13px; font-weight: 600; color: var(--on-sf-v)">{ms("expand_more", "xs")}'
           f'<i style="width: 10px; height: 10px; border-radius: 5px; background: #2F5BD3"></i><span style="flex-grow: 1; text-align: left">Поездка на Байкал</span><span>2</span></button>'
           + tab_row('С', '#2F6B5F', '#FFFFFF', 'Байкал зимой: маршруты', indent=36) + tab_row('П', '#D4F1EC', '#00665A', 'Прогноз толщины льда', indent=36))
    top = (f'<div style="display: flex; align-items: center"><button class="ib4" aria-label="Назад" style="color: var(--on-sf)">{ms("arrow_back")}</button>'
           f'<button class="ib4" aria-label="Вперёд" style="color: var(--ol-v)">{ms("arrow_forward")}</button><button class="ib4" aria-label="Обновить" style="color: var(--on-sf)">{ms("refresh")}</button>'
           f'<span style="flex-grow: 1"></span><button class="ib4" aria-label="Скрыть панель" style="color: var(--on-sf)">{ms("left_panel_close")}</button></div>')
    addr = (f'<button style="height: 48px; border-radius: 16px; background: color-mix(in srgb, var(--card) 85%, transparent); display: flex; align-items: center; gap: 8px; padding: 0 6px 0 14px; font-size: 14.5px; font-weight: 600">'
            f'{ms("lock", "xs", "color: var(--on-sf-v)")}<span style="flex-grow: 1; text-align: left">docs.example.com</span>'
            f'<span style="width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; color: var(--on-sf-v)">{ms("shield", "xs")}</span></button>')
    ws = f'<div style="display: flex; align-items: center; gap: 10px; padding: 6px 8px 0">{gem("work", 28, 10, "xs")}<span class="ty-label" style="flex-grow: 1">Работа</span><span class="ty-cap">6</span></div>'
    new = f'<button style="height: 44px; border-radius: 14px; display: flex; align-items: center; gap: 10px; padding: 0 12px; color: var(--on-sf-v); font-size: 14px; font-weight: 600">{ms("add", "s")}Новая вкладка</button>'
    foot = (f'<div style="margin-top: auto; display: flex; align-items: center; gap: 2px; padding-bottom: 4px">'
            f'<span style="width: 44px; height: 44px; display: flex; align-items: center; justify-content: center">{gem("work", 36, 12, "s")}</span>'
            f'{rail_gem("w-anime t-light", "movie", "Аниме")}{rail_gem("w-personal t-light", "home", "Личное")}'
            f'<button class="ib4" aria-label="Новое пространство" style="width: 44px; height: 44px">{ms("add", "s")}</button><span style="flex-grow: 1"></span>'
            f'<button class="ib4" aria-label="Загрузки" style="width: 44px; height: 44px">{ms("download", "s")}</button>'
            f'<button class="ib4" aria-label="Меню" style="width: 44px; height: 44px">{ms("more_vert", "s")}</button></div>')
    sep = '<i style="height: 1px; background: var(--ol-v); opacity: 0.5; margin: 4px 12px"></i>'
    return (f'<nav aria-label="Боковая панель" style="position: relative; width: {width}px; flex-shrink: 0; display: flex; flex-direction: column; gap: 10px; padding: 0 4px">'
            f'{top}{addr}<div style="display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px">{ess}</div>{ws}'
            f'<div style="display: flex; flex-direction: column; gap: 2px">{tabs}{sep}{grp}</div>{new}{foot}</nav>')


def tstatus():
    return status().replace('class="sb4"', 'class="sb4" style="height: 32px; padding: 0 28px; font-size: 13px"')


def thandle():
    return handle().replace('width: 108px', 'width: 160px').replace('margin-left: -54px', 'margin-left: -80px')


READ = 'font-family: Literata, Georgia, serif; font-size: 18px; line-height: 30px; color: #2E2B33'
DOC_CARD = 'border-radius: 20px; background: #F4F1F7; padding: 16px 18px; display: flex; flex-direction: column; gap: 10px'


def check_item(done, text):
    box = 'background: #4A3A9E; color: #FFFFFF' if done else 'box-shadow: inset 0 0 0 2px #AFA8B8'
    mark = ms('check', '', 'font-size: 16px') if done else ''
    label = f'<span style="color: #6E6873">{text}</span>' if done else text
    return (f'<span style="display: flex; gap: 12px; align-items: center; font-size: 16px; color: #1C1B1F">'
            f'<span style="width: 20px; height: 20px; border-radius: 6px; {box}; display: flex; align-items: center; justify-content: center">{mark}</span>{label}</span>')


def doc_card(title, inner):
    return f'<div style="{DOC_CARD}"><span style="font-size: 13px; font-weight: 700; color: #49454F">{title}</span>{inner}</div>'


def kv(rows):
    return ''.join(f'<span style="display: flex; justify-content: space-between; font-size: 14px; color: #1C1B1F"><span>{a}</span><b style="font-weight: 700">{b}</b></span>' for a, b in rows)


PROGRESS = ('<div style="border-radius: 20px; background: #F4F1F7; padding: 16px 18px; display: flex; flex-direction: column; gap: 10px">'
            '<span style="display: flex; justify-content: space-between; font-size: 13px; font-weight: 700; color: #49454F"><span>Готовность</span><span style="color: #4A3A9E">3 из 8</span></span>'
            '<span style="height: 8px; border-radius: 4px; background: #E6E1EB"><i style="width: 38%; height: 8px; border-radius: 4px; background: #4A3A9E"></i></span></div>')


def people():
    items = (('АК', '#E6DEFF', '#4A3A9E', 'Анна К.', 'бета'), ('МС', '#FFE1D6', '#9A3A1E', 'Максим С.', 'релиз'), ('ЛП', '#D4F1EC', '#00665A', 'Лена П.', 'отзывы'))
    return ''.join(f'<span style="display: flex; align-items: center; gap: 10px; font-size: 14px; color: #1C1B1F">{fav(a, bg, ink, 28, 14, 11)}'
                   f'<span style="flex-grow: 1">{n}</span><span style="color: #6E6873; font-size: 13px">{r}</span></span>' for a, bg, ink, n, r in items)


def launch_doc(narrow=False):
    checks = ''.join(check_item(d, t) for d, t in ((True, 'Сборка беты для тестировщиков'), (False, 'Анкета отзывов и канал поддержки'), (False, 'Список известных проблем')))
    size, lh = (36, 42) if narrow else (44, 50)
    head = (f'<span class="ty-over" style="color: #4A3A9E">Запуск · октябрь</span>'
            f'<h1 style="font-family: Literata, Georgia, serif; font-size: {size}px; line-height: {lh}px; font-weight: 600; color: #1C1B1F">План запуска на октябрь</h1>')
    intro = f'<p style="{READ}">Цели релиза, сроки по неделям и ответственные. Документ лежит в Essentials пространства «Работа».</p>'
    h2 = '<h2 style="font-size: 22px; font-weight: 700; color: #1C1B1F; padding-top: 4px">{}</h2>'
    w1 = h2.format('Неделя 1') + f'<p style="{READ}">Закрываем задачи беты и собираем отзывы тестировщиков. В пятницу — созвон по итогам.</p>'
    w2 = h2.format('Неделя 2') + f'<p style="{READ}">Исправляем найденное, готовим страницы в магазинах и скриншоты для планшетов. К четвергу — кандидат в релиз.</p>'
    if narrow:
        return f'<div style="position: absolute; inset: 0; background: #FFFFFF; padding: 24px 36px; display: flex; flex-direction: column; gap: 14px">{head}{intro}{PROGRESS}{w1}{checks}</div>'
    dates = doc_card('Сроки', kv((('Бета', '6 октября'), ('Кандидат', '16 октября'), ('Релиз', '20 октября'))))
    aside = f'<aside style="padding-top: 36px; display: flex; flex-direction: column; gap: 12px">{dates}{PROGRESS}{doc_card("Ответственные", people())}</aside>'
    return (f'<div style="position: absolute; inset: 0; background: #FFFFFF; padding: 44px 60px; display: grid; grid-template-columns: 2fr 1fr; gap: 44px">'
            f'<div style="display: flex; flex-direction: column; gap: 16px">{head}{intro}{w1}{checks}{w2}</div>{aside}</div>')


def sprint_board():
    def card(t, d, c, who):
        return (f'<div style="border-radius: 16px; background: {c}; padding: 12px; display: flex; flex-direction: column; gap: 10px">'
                f'<span style="font-size: 14px; font-weight: 600; color: #1C1B1F">{t}</span>'
                f'<span style="display: flex; align-items: center; justify-content: space-between"><span style="font-size: 12px; font-weight: 600; color: rgba(28,27,31,0.6)">{d}</span>{fav(who[0], "#FFFFFF", who[1], 24, 12, 9)}</span></div>')
    A, M, L = ('АК', '#4A3A9E'), ('МС', '#9A3A1E'), ('ЛП', '#00665A')
    cols = (('Очередь', (('Тёмная тема графиков', '8 окт.', '#F1EEF4', L), ('Экспорт в CSV', '9 окт.', '#F1EEF4', M), ('Подсказки в пустых списках', '10 окт.', '#F1EEF4', A))),
            ('В работе', (('Экран оплаты', '3 окт.', '#FFE1D6', M), ('Онбординг', '5 окт.', '#E6DEFF', A), ('Уведомления', '8 окт.', '#FFE9B8', L))),
            ('Готово', (('Поиск', '29 сент.', '#D4F1EC', A), ('Профиль', '27 сент.', '#D8EEFF', M), ('Вход по ключу доступа', '25 сент.', '#D4F1EC', L))))
    grid = ''.join(f'<div style="display: flex; flex-direction: column; gap: 10px"><span class="ty-over" style="display: flex; justify-content: space-between; color: #49454F"><span>{c}</span><span>{len(items)}</span></span>'
                   + ''.join(card(*i) for i in items) + '</div>' for c, items in cols)
    pill = '<span style="height: 32px; border-radius: 16px; background: #F4F1F7; padding: 0 12px; display: flex; align-items: center; font-size: 13px; font-weight: 600; color: #49454F">до 10 октября</span>'
    return (f'<div style="position: absolute; inset: 0; background: #FFFFFF; padding: 20px 24px; display: flex; flex-direction: column; gap: 16px">'
            f'<div style="display: flex; align-items: center; justify-content: space-between"><span style="font-size: 22px; font-weight: 700; color: #1C1B1F">Спринт 42</span>{pill}</div>'
            f'<div style="display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px">{grid}</div></div>')


def pane(content, domain, letter, bg, ink, active):
    ring = 'box-shadow: 0 0 0 2.5px var(--pri), var(--e1);' if active else ''
    weight, color = (700, 'var(--on-sf)') if active else (600, 'var(--on-sf-v)')
    head = (f'<div style="position: absolute; left: 0; right: 0; top: 0; height: 44px; display: flex; align-items: center; gap: 8px; padding: 0 4px 0 14px; '
            f'background: var(--card); border-bottom: 1px solid var(--sf-high); z-index: 2">{fav(letter, bg, ink, 20, 6, 11)}'
            f'<span style="flex-grow: 1; font-size: 13.5px; font-weight: {weight}; color: {color}">{domain}</span>'
            f'<button class="ib4" aria-label="Поменять местами" style="width: 36px; height: 36px">{ms("swap_horiz", "xs")}</button>'
            f'<button class="ib4" aria-label="Закрыть панель" style="width: 36px; height: 36px">{ms("close", "xs")}</button></div>')
    return (f'<main class="page-card" style="position: relative; left: 0; right: 0; flex: 1 1 0; min-width: 0; border-radius: 24px; {ring}">'
            f'{head}<div style="position: absolute; left: 0; right: 0; top: 44px; bottom: 0">{content}</div></main>')


def tablet_settings_body():
    nav = [('palette', 'Внешний вид', True), ('workspaces', 'Пространства', False), ('swipe', 'Вкладки и жесты', False), ('search', 'Поиск', False),
           ('key', 'Пароли и автозаполнение', False), ('shield', 'Приватность и защита', False), ('extension', 'Расширения', False), ('sync', 'Синхронизация', False), ('info', 'О Vola', False)]

    def nav_item(i, t, on):
        bg, fg, w = ('var(--sec-c)', 'var(--on-sec-c)', 700) if on else ('transparent', 'var(--on-sf)', 500)
        return f'<a href="#" style="height: 52px; border-radius: 26px; background: {bg}; color: {fg}; display: flex; align-items: center; gap: 14px; padding: 0 18px; font-size: 15px; font-weight: {w}">{ms(i, "f" if on else "")}{t}</a>'
    navh = ''.join(nav_item(*n) for n in nav)
    from build_v4 import mini_phone

    def choice(style, name, sub, on):
        mark = ms('check_circle', 'f s', 'color: var(--pri)') if on else ''
        bg, sh = ('var(--sec-c)', 'inset 0 0 0 2px var(--pri)') if on else ('var(--card)', 'var(--e1)')
        return (f'<button role="radio" aria-checked="{"true" if on else "false"}" style="flex: 1 1 0; border-radius: 24px; padding: 14px 16px; display: flex; gap: 16px; align-items: center; background: {bg}; box-shadow: {sh}">'
                f'{mini_phone(style)}<span style="display: flex; flex-direction: column; gap: 4px; text-align: left"><span style="display: flex; align-items: center; gap: 6px; font-size: 17px; font-weight: 700">{mark}{name}</span>'
                f'<span class="ty-cap" style="font-size: 13.5px">{sub}</span></span></button>')
    grads = ('conic-gradient(#006978 0 33%, #A23F2B 0 66%, #5F4FB8 0)', 'conic-gradient(#6D8F5A 0 50%, #D8C9A7 0)', 'linear-gradient(135deg, #0B6E99, #3FC1C9)',
             'linear-gradient(135deg, #C2410C, #F59E0B)', 'conic-gradient(#1F2426 0 50%, #D9DEE0 0)')
    pal = ''.join(f'<span style="width: 44px; height: 44px; border-radius: 22px; background: {g}; box-shadow: {"0 0 0 3px var(--card), 0 0 0 5px var(--pri)" if k == 0 else "none"}"></span>' for k, g in enumerate(grads))
    slider = ('<span aria-label="Размер текста 100 %" style="display: flex; align-items: center; gap: 12px; width: 220px"><span style="font-size: 12px; font-weight: 700; color: var(--on-sf-v)">А</span>'
              '<span style="position: relative; flex-grow: 1; height: 16px; display: flex; align-items: center"><i style="position: absolute; left: 0; right: 0; height: 4px; border-radius: 2px; background: var(--sec-c)"></i>'
              '<i style="position: absolute; left: 0; width: 50%; height: 4px; border-radius: 2px; background: var(--pri)"></i><i style="position: absolute; left: calc(50% - 2px); width: 4px; height: 16px; border-radius: 2px; background: var(--pri)"></i></span>'
              '<span style="font-size: 17px; font-weight: 700; color: var(--on-sf-v)">А</span></span>')
    rows = group([row('left_panel_open', 'Боковая панель', 'Слева на планшете, прячется жестом от края', switch(True), h=60),
                  row('text_fields', 'Размер текста', 'Интерфейс и страницы · 100 %', slider, h=60),
                  row('animation', 'Пружинные анимации', 'Переходы между вкладками и пространствами', switch(True), h=60)])
    card = 'border-radius: 24px; background: var(--card); padding: 16px 18px; display: flex; flex-direction: column; gap: 12px'
    lab = '<span class="ty-label" style="font-size: 15px">{}</span>'
    left = f'<div style="{card}">{lab.format("Тема")}{seg(["Светлая", "Тёмная", "Авто"], 2)}{lab.format("Плотность")}{seg(["Компактно", "Обычно", "Просторно"], 1)}</div>'
    right = (f'<div style="{card}"><span style="display: flex; justify-content: space-between">{lab.format("Цвета")}<span class="ty-cap" style="font-weight: 600">Свои у каждого пространства</span></span>'
             f'<div style="display: flex; gap: 14px; height: 44px">{pal}</div>{lab.format("Скругления")}{seg(["Строгие", "Мягкие", "Круглые"], 2)}</div>')
    content = (f'<div style="display: flex; flex-direction: column; gap: 14px"><span class="ty-head">Внешний вид</span>'
               f'<div role="radiogroup" aria-label="Оформление" style="display: flex; gap: 14px">{choice("frame", "Рама", "Страница в цвете пространства", True)}{choice("air", "Воздух", "Страница на весь экран", False)}</div>'
               f'<div style="display: grid; grid-template-columns: 1fr 1fr; gap: 14px">{left}{right}</div>{rows}</div>')
    side = (f'<nav aria-label="Разделы настроек" style="width: 320px; flex-shrink: 0; display: flex; flex-direction: column; gap: 10px">'
            f'<div style="display: flex; align-items: center; gap: 4px"><button class="ib4" aria-label="Назад" style="color: var(--on-sf)">{ms("arrow_back")}</button><span class="ty-title-l">Настройки</span></div>'
            f'{searchfield("Поиск настроек")}<div style="display: flex; flex-direction: column; gap: 2px">{navh}</div></nav>')
    return (f'{tstatus()}<div style="position: absolute; left: 20px; right: 20px; top: 40px; bottom: 24px; display: flex; gap: 20px">{side}'
            f'<section style="flex-grow: 1; border-radius: 32px; background: var(--sf-low); padding: 24px 28px">{content}</section></div>{thandle()}')


def g_tablet():
    frame = 'position: absolute; left: 12px; right: 12px; top: 36px; bottom: 20px; display: flex'
    body = f'{tstatus()}<div style="{frame}; gap: 12px">{sidebar()}<main class="page-card" style="position: relative; left: 0; right: 0; flex-grow: 1; border-radius: 24px">{launch_doc()}</main></div>{thandle()}'
    W('Tablet', 'планшет', 'w-work t-light aura', body, w=1280, h=800)

    def rail_tile(n, l, b, i):
        ring = 'box-shadow: 0 0 0 2px var(--pri);' if n in ('Задачи', 'Документы') else ''
        return f'<a href="#" aria-label="{n}" style="width: 48px; height: 48px; border-radius: 16px; background: color-mix(in srgb, var(--card) 80%, transparent); {ring} display: flex; align-items: center; justify-content: center">{fav(l, b, i, 26, 8, 12)}</a>'
    rail = ''.join(rail_tile(*e) for e in ESS)
    nav = (f'<nav aria-label="Панель" style="width: 56px; flex-shrink: 0; display: flex; flex-direction: column; align-items: center; gap: 8px">'
           f'<button class="ib4" aria-label="Развернуть панель" style="color: var(--on-sf)">{ms("left_panel_open")}</button>{rail}'
           f'<button class="ib4" aria-label="Новая вкладка">{ms("add")}</button><span style="flex-grow: 1"></span>{gem("work", 40, 14, "s")}</nav>')
    divider = '<div role="separator" aria-label="Граница панелей" style="width: 12px; flex-shrink: 0; display: flex; align-items: center; justify-content: center"><i style="width: 5px; height: 56px; border-radius: 3px; background: var(--on-sf); opacity: 0.4"></i></div>'
    panes = pane(launch_doc(True), 'docs.example.com', 'Д', '#E6DEFF', '#4A3A9E', True) + divider + pane(sprint_board(), 'tasks.example.com', 'З', '#FFE1D6', '#9A3A1E', False)
    W('TabletSplit', 'планшет: Split View', 'w-work t-light aura', f'{tstatus()}<div style="{frame}; gap: 10px">{nav}{panes}</div>{thandle()}', w=1280, h=800)

    W('TabletSettings', 'планшет: настройки', 'w-work t-light aura', tablet_settings_body(), w=1280, h=800)


# ---------------------------------------------------------------- base (tokens, components, states)

def scheme_hex():
    """Parse canvas/vola4-colors.css into {'.w-work.t-light': {'--pri': '#006978', ...}}."""
    import pathlib
    import re
    css = (pathlib.Path(__file__).resolve().parent.parent / 'canvas' / 'vola4-colors.css').read_text()
    out = {}
    for sel, body in re.findall(r'([^{}]+)\{([^}]*)\}', css):
        sel = sel.strip().split('\n')[-1].strip()
        out[sel] = dict(re.findall(r'(--[\w-]+):\s*(#[0-9A-Fa-f]{6})', body))
    return out


def base_head(over, title, lead_text):
    return (f'<header style="display: flex; align-items: flex-end; justify-content: space-between; gap: 40px">'
            f'<div style="display: flex; flex-direction: column; gap: 8px"><span class="ty-over" style="color: var(--pri)">{over}</span><h1 class="ty-display" style="font-size: 44px; line-height: 50px">{title}</h1></div>'
            f'<p class="ty-body" style="max-width: 560px; color: var(--on-sf-v); text-align: right">{lead_text}</p></header>')


def panel(title, inner, sub='', style=''):
    s = f'<span class="ty-cap">{sub}</span>' if sub else ''
    return (f'<section style="border-radius: 28px; background: var(--card); box-shadow: var(--e1); padding: 22px 24px; display: flex; flex-direction: column; gap: 16px; {style}">'
            f'<div style="display: flex; align-items: baseline; justify-content: space-between; gap: 16px"><h2 class="ty-title">{title}</h2>{s}</div>{inner}</section>')


def spring_path(zeta, k, w=220, h=86, t_max=0.6):
    import math
    wn = math.sqrt(k)
    pts = []
    for i in range(61):
        t = t_max * i / 60
        if zeta < 1:
            wd = wn * math.sqrt(1 - zeta * zeta)
            x = 1 - math.exp(-zeta * wn * t) * (math.cos(wd * t) + zeta / math.sqrt(1 - zeta * zeta) * math.sin(wd * t))
        else:
            x = 1 - math.exp(-wn * t) * (1 + wn * t)
        pts.append((w * i / 60, h - 8 - (h - 22) * x))
    return 'M' + ' L'.join(f'{a:.1f} {b:.1f}' for a, b in pts)


SPACER20 = '<span style="width: 20px"></span>'


def g_base():
    H = scheme_hex()
    wss = (('work', 'Работа', 'work', '#006877'), ('anime', 'Аниме', 'movie', '#A23F2B'), ('personal', 'Личное', 'home', '#5E4EB7'))
    roles = (('--pri', 'Основной'), ('--pri-c', 'Контейнер'), ('--sec-c', 'Тон'), ('--sf', 'Фон'), ('--sf-c', 'Поверхность'), ('--on-sf', 'Текст'))

    def sw_row(sel, dark):
        vals = H[sel]
        cells = ''.join(
            f'<span style="display: flex; flex-direction: column; gap: 6px; min-width: 0"><i style="height: 44px; border-radius: 14px; background: {vals[r]}; box-shadow: inset 0 0 0 1px {"rgba(255,255,255,0.10)" if dark else "rgba(0,0,0,0.06)"}"></i>'
            f'<span style="font-size: 11px; font-weight: 500; font-family: ui-monospace, monospace; color: {"#A5ACAE" if dark else "var(--on-sf-v)"}">{vals[r]}</span></span>'
            for r, n in roles)
        aura = f'linear-gradient(90deg, {vals["--aura-1"]}, {vals["--aura-2"]} 55%, {vals["--aura-3"]})'
        bg = '#000000' if dark else 'var(--sf-low)'
        label = 'Тёмная · фон всегда #000000' if dark else 'Светлая'
        return (f'<div style="border-radius: 20px; background: {bg}; padding: 14px; display: flex; flex-direction: column; gap: 10px">'
                f'<span style="display: flex; align-items: center; gap: 10px"><i style="flex-grow: 1; height: 10px; border-radius: 5px; background: {aura}"></i><span style="font-size: 11.5px; font-weight: 700; color: {"#A5ACAE" if dark else "var(--on-sf-v)"}">{label}</span></span>'
                f'<div style="display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 8px">{cells}</div></div>')

    def ws_card(key, name, icon, seed):
        return (f'<div class="w-{key} t-light" style="border-radius: 24px; background: var(--sf-lowest); box-shadow: inset 0 0 0 1px var(--sf-high); padding: 16px; display: flex; flex-direction: column; gap: 12px; color: var(--on-sf)">'
                f'<span style="display: flex; align-items: center; gap: 12px">{gem(icon, 40, 14, "s")}<span style="display: flex; flex-direction: column"><span class="ty-title">{name}</span>'
                f'<span class="ty-cap" style="font-family: ui-monospace, monospace">исходный {seed} · TonalSpot 2025</span></span></span>{sw_row(f".w-{key}.t-light", False)}{sw_row(f".w-{key}.t-dark", True)}</div>')

    colors = panel('Цвет пространства → вся оболочка', f'<div style="display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px">{"".join(ws_card(*w) for w in wss)}</div>',
                   'Слева направо: основной · контейнер · тон · фон · поверхность · текст')

    def status_sw(var, name, text):
        return (f'<span style="display: flex; align-items: center; gap: 10px"><span style="height: 32px; border-radius: 16px; padding: 0 12px; background: var({var}-c); color: var(--on{var}-c); display: flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 700">{text}</span>'
                f'<span class="ty-cap">{name}</span></span>')
    statuses = panel('Статусы и приватность', (
        '<div style="display: flex; flex-direction: column; gap: 10px">'
        + status_sw('--ok', 'Успех — согласован с цветом пространства', ms('check_circle', 'f xs') + 'Защищено')
        + status_sw('--warn', 'Предупреждение', ms('warning', 'f xs') + 'Повтор пароля')
        + status_sw('--err', 'Ошибка и опасность', ms('dangerous', 'f xs') + 'Утечка')
        + f'<span style="display: flex; align-items: center; gap: 10px"><span class="gem priv" style="width: 32px; height: 32px; border-radius: 11px">{ms("domino_mask", "f xs")}</span><span class="ty-cap">Приватные вкладки — свой фиолетовый, всегда тёмные</span></span></div>'))

    type_rows = (('ty-display', 'Display · 36/40 · 700', 'Пространства'), ('ty-head', 'Headline · 28/34 · 700', 'Обзор вкладок'), ('ty-title-l', 'Title L · 22/28 · 700', 'Настройки пространства'),
                 ('ty-title', 'Title · 17/22 · 600', 'north-guide.ru'), ('ty-body', 'Body · 15/22 · 500', 'Свои cookie, входы и данные сайтов'), ('ty-label', 'Label · 14/18 · 600', 'Открыть в группе'),
                 ('ty-cap', 'Caption · 12.5/16 · 500', 'Обновлено 5 минут назад'), ('ty-over', 'Overline · 12/16 · 700', 'Essentials'), ('ty-read', 'Чтение · Literata 17/28', 'Лёд на Байкале становится крепким к середине февраля.'))
    typ = panel('Шрифт', '<div style="display: flex; flex-direction: column; gap: 8px">' + ''.join(
        f'<div style="display: grid; grid-template-columns: 180px 1fr; align-items: baseline; gap: 16px"><span class="ty-cap" style="font-weight: 600">{m}</span><span class="{c}" style="white-space: nowrap; overflow: hidden; text-overflow: ellipsis">{s}</span></div>'
        for c, m, s in type_rows) + '</div>', 'Manrope — интерфейс, Literata — чтение · вес 500–700')

    radii = (('xs', 8), ('s', 12), ('m', 16), ('l', 20), ('card', 24), ('xl', 28), ('sheet', 32), ('full', 26))
    shapes = panel('Форма', '<div style="display: grid; grid-template-columns: repeat(8, minmax(0, 1fr)); gap: 10px">' + ''.join(
        f'<span style="display: flex; flex-direction: column; align-items: center; gap: 6px"><i style="width: 52px; height: 52px; border-radius: {r}px; background: var(--pri-c); box-shadow: inset 0 0 0 1.5px var(--pri)"></i><span class="ty-label" style="font-size: 12px">{n}</span><span class="ty-cap" style="font-size: 11px">{"50 %" if n == "full" else f"{r} dp"}</span></span>'
        for n, r in radii) + '</div>', 'r-xs … r-sheet')

    depth_items = (('Уровень 1', 'box-shadow: var(--e1); background: var(--sf-lowest)', 'карточки, плитки'), ('Уровень 2', 'box-shadow: var(--e2); background: var(--sf-lowest)', 'панель, меню'),
                   ('Уровень 3', 'box-shadow: var(--e3); background: var(--sf-lowest)', 'листы, диалоги'), ('Стекло', 'background: color-mix(in srgb, var(--sf-lowest) 86%, transparent); backdrop-filter: blur(28px); box-shadow: var(--e2)', 'фон ≥ 84 %'))
    depth = panel('Глубина', '<div class="aura" style="border-radius: 20px; padding: 22px 18px; display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px">' + ''.join(
        f'<span style="height: 76px; border-radius: 20px; {st}; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 2px"><span class="ty-label">{n}</span><span class="ty-cap" style="font-size: 11.5px">{d}</span></span>'
        for n, st, d in depth_items) + '</div>')

    springs = (('Быстрая', 0.9, 1400, 'кнопки, переключатели, чипы'), ('Обычная', 0.9, 700, 'панель адреса, листы, карточки'), ('Медленная', 0.9, 300, 'смена пространства, Split View'), ('Эффекты', 1.0, 1600, 'цвет, прозрачность, размытие'))
    motion = panel('Движение · пружины M3 Expressive', '<div style="display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px">' + ''.join(
        f'<div style="border-radius: 20px; background: var(--sf-low); padding: 14px; display: flex; flex-direction: column; gap: 8px"><svg viewBox="0 0 220 86" style="width: 100%; height: 76px"><path d="M0 14 H220" stroke="var(--ol-v)" stroke-dasharray="3 4" fill="none"></path>'
        f'<path d="{spring_path(z, k)}" stroke="var(--pri)" stroke-width="3" fill="none" stroke-linecap="round"></path></svg><span class="ty-label">{n}</span><span class="ty-cap" style="font-family: ui-monospace, monospace">затухание {z} · жёсткость {k}</span><span class="ty-cap">{u}</span></div>'
        for n, z, k, u in springs) + '</div>', 'Тактильная отдача: смена пространства, закрытие вкладки свайпом, долгое нажатие')

    sp = ((4, 'внутри значка'), (8, 'между чипами'), (12, 'в строке списка'), (16, 'поля экрана'), (20, 'внутри карточки'), (24, 'между группами'), (32, 'секции'), (48, 'над заголовком'))
    spacing = panel('Отступы · шаг 4', '<div style="display: grid; grid-template-columns: repeat(8, minmax(0, 1fr)); align-items: end; gap: 8px">' + ''.join(
        f'<span style="display: flex; flex-direction: column; align-items: center; gap: 6px; text-align: center"><i style="width: {s}px; height: {s}px; border-radius: {min(s // 3, 8)}px; background: var(--pri)"></i><span class="ty-label" style="font-size: 12.5px">{s}</span><span class="ty-cap" style="font-size: 11px; line-height: 13px; height: 26px">{u}</span></span>' for s, u in sp) + '</div>')

    icons = ('home', 'work', 'movie', 'search', 'bookmark', 'download', 'shield', 'key', 'history', 'tab_group', 'translate', 'share')
    ic = panel('Значки · Material Symbols Rounded', '<div style="display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 8px">' + ''.join(
        f'<span style="height: 64px; border-radius: 18px; background: var(--sf-low); display: flex; align-items: center; justify-content: center; gap: 10px">{ms(i, "", "color: var(--on-sf-v)")}{ms(i, "f", "color: var(--pri)")}</span>' for i in icons) + '</div>',
        '24 dp · вес 400 · контур → заливка у выбранного')

    touch = panel('Касание', '<div style="display: flex; align-items: center; gap: 18px; flex-wrap: wrap">'
                  f'<span style="width: 48px; height: 48px; border-radius: 24px; box-shadow: inset 0 0 0 1.5px var(--pri); display: flex; align-items: center; justify-content: center; color: var(--pri)">{ms("close", "s")}</span><span class="ty-cap" style="width: 90px">Цель касания 48 dp</span>'
                  '<button class="btn tonal" style="background: color-mix(in srgb, var(--on-sec-c) 10%, var(--sec-c))">Нажатие 10 %</button>'
                  '<button class="btn tonal" style="box-shadow: 0 0 0 3px var(--sf-lowest), 0 0 0 6px var(--pri)">Фокус · кольцо 3</button></div>')

    body = (f'<div style="position: absolute; inset: 0; padding: 48px 56px; display: flex; flex-direction: column; gap: 20px">'
            + base_head('Vola v4 · основа', 'Токены', 'Все значения — в ui/theme. Цвет пространства задаёт всю схему; тёмная тема всегда на чистом чёрном. Готовые значения: canvas/vola4-colors.css и vola4.css.')
            + colors
            + f'<div style="display: grid; grid-template-columns: 1.25fr 1fr; gap: 20px">{typ}<div style="display: flex; flex-direction: column; gap: 20px">{shapes}{statuses}</div></div>'
            + f'<div style="display: grid; grid-template-columns: 1fr 1.1fr; gap: 20px">{depth}{spacing}</div>'
            + motion
            + f'<div style="display: grid; grid-template-columns: 1.25fr 1fr; gap: 20px">{ic}{touch}</div></div>')
    W('Tokens', 'токены', 'w-work t-light', body, w=1440, h=1800)

    # ------------------------------------------------ components
    from build_v4 import address_bar

    def bar_demo(label, inner, cls=''):
        return (f'<div style="display: flex; flex-direction: column; gap: 8px"><div class="aura {cls}" style="color: var(--on-sf); position: relative; height: 76px; border-radius: 22px; padding: 10px 8px; display: flex; align-items: center; gap: 4px; overflow: hidden">{inner}</div>'
                f'<span class="ty-cap">{label}</span></div>')
    loading = address_bar('ice-forecast.example.com').replace('<span class="ms xs" style="color: var(--on-sf-v)">lock</span>', '<span class="ms xs" style="color: var(--on-sf-v)">lock</span>', 1)
    loading = loading.replace('class="fieldpill" aria-label="Адрес и сведения о сайте" style="position: relative;', 'class="fieldpill" aria-label="Адрес и сведения о сайте" style="position: relative; overflow: hidden;', 1)
    wave = '<svg aria-hidden="true" viewBox="0 0 120 6" preserveAspectRatio="none" style="position: absolute; left: 62px; bottom: 5px; width: 42%; height: 5px"><path d="M0 3 Q5 0 10 3 T20 3 T30 3 T40 3 T50 3 T60 3 T70 3 T80 3 T90 3 T100 3 T110 3 T120 3" stroke="var(--pri)" stroke-width="2" fill="none" stroke-linecap="round"></path></svg>'
    loading = loading.replace('>menu_book<', '>close<', 1) + wave
    private = address_bar('Приватная вкладка').replace('class="gem"', 'class="gem priv"').replace('>work<', '>domino_mask<')
    capsule_demo = f'<span style="flex-grow: 1; display: flex; justify-content: center">{capsule().replace("position: absolute; left: 50%; bottom: 20px; transform: translateX(-50%);", "")}</span>'
    bars = panel('Адресная панель', '<div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px">'
                 + bar_demo('Обычная · «Рама»', address_bar()) + bar_demo('Загрузка · волнистый индикатор, «Стоп»', loading)
                 + bar_demo('При прокрутке · капсула с адресом', capsule_demo) + bar_demo('Приватная · всегда тёмная, ничего не пишет на диск', private, 'w-private t-dark') + '</div>')

    buttons = panel('Кнопки', '<div style="display: flex; flex-direction: column; gap: 14px">'
                    f'<div style="display: flex; flex-wrap: wrap; gap: 10px; align-items: center">{btn("Основная", "fill", h=48)}{btn("Тональная", "tonal", h=48)}{btn("Контурная", "out", h=48)}'
                    '<button class="btn" style="color: var(--pri); padding: 0 12px">Текст</button>'
                    '<button class="btn" style="background: var(--err-c); color: var(--on-err-c)">Удалить</button></div>'
                    f'<div style="display: flex; flex-wrap: wrap; gap: 12px; align-items: center"><button class="ib4" aria-label="Назад" style="color: var(--on-sf)">{ms("arrow_back")}</button>'
                    f'<button class="ib4" aria-label="Обновить" style="background: var(--sec-c); color: var(--on-sec-c)">{ms("refresh")}</button>'
                    f'<button class="ib4" aria-label="Закладка" style="background: var(--pri); color: var(--on-pri)">{ms("bookmark", "f")}</button>'
                    f'<button aria-label="Новая вкладка" style="width: 56px; height: 56px; border-radius: 20px; background: var(--pri); color: var(--on-pri); display: flex; align-items: center; justify-content: center; box-shadow: 0 8px 20px color-mix(in srgb, var(--pri) 35%, transparent)">{ms("add", "l")}</button>'
                    f'<button style="height: 56px; border-radius: 20px; padding: 0 20px 0 16px; background: var(--pri-c); color: var(--on-pri-c); display: flex; align-items: center; gap: 10px; box-shadow: var(--e2); font-size: 15px; font-weight: 600">{ms("add")}Новая группа</button></div>'
                    f'{seg(["День", "Неделя", "Месяц"], 1)}</div>')

    sel = panel('Выбор и ввод', '<div style="display: flex; flex-direction: column; gap: 14px">'
                f'<div style="display: flex; flex-wrap: wrap; gap: 8px">{chip("Сегодня", True)}{chip("Работа", icon="work")}{chip("Видео", icon="movie")}{chip("До завтра", icon="schedule")}</div>'
                f'<div style="display: flex; align-items: center; gap: 18px">{switch(True)}{switch(False)}'
                '<span style="width: 24px; height: 24px; border-radius: 12px; box-shadow: inset 0 0 0 2px var(--pri); display: flex; align-items: center; justify-content: center"><i style="width: 12px; height: 12px; border-radius: 6px; background: var(--pri)"></i></span>'
                f'<span style="width: 22px; height: 22px; border-radius: 7px; background: var(--pri); color: var(--on-pri); display: flex; align-items: center; justify-content: center">{ms("check", "", "font-size: 18px")}</span>'
                '<span style="position: relative; flex-grow: 1; height: 20px; display: flex; align-items: center"><i style="position: absolute; left: 0; right: 0; height: 6px; border-radius: 3px; background: var(--sec-c)"></i><i style="position: absolute; left: 0; width: 62%; height: 6px; border-radius: 3px; background: var(--pri)"></i><i style="position: absolute; left: calc(62% - 2px); width: 4px; height: 20px; border-radius: 2px; background: var(--pri)"></i></span></div>'
                f'{searchfield("Поиск по истории")}</div>')

    ws = panel('Пространства', '<div style="display: flex; flex-direction: column; gap: 14px">'
               '<div style="height: 56px; border-radius: 28px; background: var(--sf-low); display: flex; align-items: center; gap: 2px; padding: 0 4px">'
               f'<span style="height: 48px; border-radius: 24px; background: var(--sf-lowest); box-shadow: var(--e1); display: flex; align-items: center; gap: 8px; padding: 0 14px 0 4px">{gem("work", 40, 14, "s")}<span style="font-size: 15px; font-weight: 600">Работа</span></span>'
               f'<span class="w-anime t-light" style="width: 48px; height: 48px; display: flex; align-items: center; justify-content: center">{gem("movie", 32, 11, "xs")}</span>'
               f'<span class="w-personal t-light" style="width: 48px; height: 48px; display: flex; align-items: center; justify-content: center">{gem("home", 32, 11, "xs")}</span>'
               f'<span style="width: 48px; height: 48px; display: flex; align-items: center; justify-content: center"><span class="gem priv" style="width: 32px; height: 32px; border-radius: 11px">{ms("domino_mask", "f xs")}</span></span></div>'
               f'<div style="display: flex; align-items: center; gap: 14px">{gem("work", 56, 20)}{gem("work", 40, 14, "s")}{gem("work", 28, 10, "xs")}<span class="ty-cap" style="max-width: 200px">Самоцвет пространства: 56 · 40 · 28 dp, градиент из основного цвета</span></div>'
               + '<div style="border-radius: 20px; background: var(--sf-low); padding: 4px 0">' + ''.join(
                   f'<div class="w-{k} t-light" style="height: 52px; display: flex; align-items: center; gap: 12px; padding: 0 14px; background: none; color: var(--on-sf)">{gem(i, 32, 11, "xs")}<span class="ty-label" style="flex-grow: 1; font-size: 15px">{n}</span><span class="ty-cap">{c}</span>{ms("check", "s", "color: var(--pri)") if k == "work" else SPACER20}</div>'
                   for k, i, n, c in (('work', 'work', 'Работа', '6 вкладок'), ('anime', 'movie', 'Аниме', '4 вкладки'), ('personal', 'home', 'Личное', '9 вкладок'))) + '</div></div>')

    tiles = panel('Вкладки и Essentials', f'<div style="display: grid; grid-template-columns: 180px 1fr; gap: 18px; align-items: start"><div style="width: 180px">{tab_card(TABS[5], current=True)}</div>'
                  f'<div style="display: flex; flex-direction: column; gap: 14px"><div class="aura" style="border-radius: 20px; padding: 14px; display: flex; gap: 14px">{ess_tile("Почта", "П", "#DDE3FF", "#2F4AA8")}{ess_tile("Задачи", "З", "#FFE1D6", "#9A3A1E", True)}</div>'
                  f'<div style="display: flex; gap: 10px; align-items: center">{fav("С", "#2F6B5F", "#FFFFFF")}{fav("Д", "#E6DEFF", "#4A3A9E")}{fav("К", "#D4F1EC", "#00665A")}<span class="ty-cap">Монограмма сайта, пока нет значка</span></div></div></div>')

    lists = panel('Списки и листы', '<div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px; align-items: start">'
                  + group([row('shield', 'Приватность и защита', 'Строгая · только HTTPS', CHEV, 'var(--sec-c)', 'var(--on-sec-c)'),
                           row('cookie', 'Cookie-баннеры', 'Отказ от необязательных', switch(True), 'var(--sec-c)', 'var(--on-sec-c)'),
                           row('delete', 'Удалить данные сайта', '', '', danger=True)], bg='var(--sf-low)')
                  + '<div style="position: relative; height: 200px; border-radius: 24px; background: var(--sf-c); overflow: hidden">'
                  + sheet('<span class="ty-title">Лист снизу</span><span class="ty-cap">Скругление 32, ручка, затемнение фона. Закрывается свайпом вниз.</span>' + f'<div style="display: flex; gap: 8px">{btn("Готово", "fill", grow=True, h=44)}</div>', pad='0 18px 18px', gap=8) + '</div></div>')

    snack = panel('Уведомления', '<div style="display: flex; flex-direction: column; gap: 10px">'
                  f'<div style="height: 52px; border-radius: 18px; background: var(--inv-sf); color: #FFFFFF; display: flex; align-items: center; gap: 12px; padding: 0 8px 0 16px"><span style="flex-grow: 1; font-size: 14px; font-weight: 600">Вкладка закрыта</span><button class="btn" style="height: 40px; color: var(--pri-c); padding: 0 12px">Вернуть</button></div>'
                  f'<div style="height: 52px; border-radius: 18px; background: var(--inv-sf); color: #FFFFFF; display: flex; align-items: center; gap: 12px; padding: 0 8px 0 16px">{ms("download_done", "s", "color: var(--pri-c)")}<span style="flex-grow: 1; font-size: 14px; font-weight: 600">Файл загружен и проверен</span><button class="btn" style="height: 40px; color: var(--pri-c); padding: 0 12px">Открыть</button></div></div>')

    ind = panel('Индикаторы', '<div style="display: flex; align-items: center; gap: 24px; flex-wrap: wrap">'
                '<svg viewBox="0 0 160 12" style="width: 160px; height: 12px"><path d="M2 6 Q7 1 12 6 T22 6 T32 6 T42 6 T52 6 T62 6 T72 6 T82 6 T92 6" stroke="var(--pri)" stroke-width="4" fill="none" stroke-linecap="round"></path><path d="M102 6 H158" stroke="var(--sec-c)" stroke-width="4" stroke-linecap="round"></path></svg>'
                '<svg viewBox="0 0 48 48" style="width: 44px; height: 44px"><circle cx="24" cy="24" r="19" stroke="var(--sec-c)" stroke-width="5" fill="none"></circle><circle cx="24" cy="24" r="19" stroke="var(--pri)" stroke-width="5" fill="none" stroke-dasharray="80 200" stroke-linecap="round" transform="rotate(-90 24 24)"></circle></svg>'
                '<span style="width: 44px; height: 44px; border-radius: 14px 22px 14px 22px; background: var(--pri); transform: rotate(12deg)"></span>'
                f'<span style="display: flex; gap: 6px">{badge("Утечка", "leak")}{badge("Повтор", "reuse")}{badge("", "passkey")}</span>'
                f'<span style="height: 32px; border-radius: 16px; background: var(--ok-c); color: var(--on-ok-c); padding: 0 12px; display: flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 700">{ms("lock", "f xs")}На устройстве</span></div>', 'волна — страница · кольцо — файл · форма — ожидание')

    body = (f'<div style="position: absolute; inset: 0; padding: 48px 56px; display: flex; flex-direction: column; gap: 20px">'
            + base_head('Vola v4 · основа', 'Компоненты', 'Один набор деталей на всех экранах. Цвет каждой детали берётся из пространства, поэтому одна и та же кнопка в «Работе» бирюзовая, в «Аниме» — терракотовая.')
            + bars
            + f'<div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px">{buttons}{sel}</div>'
            + f'<div style="display: grid; grid-template-columns: 1fr 1.3fr; gap: 20px">{ws}{tiles}</div>'
            + f'<div style="display: grid; grid-template-columns: 1.3fr 1fr; gap: 20px">{lists}<div style="display: flex; flex-direction: column; gap: 20px">{snack}{ind}</div></div></div>')
    W('Components', 'компоненты', 'w-work t-light', body, w=1440, h=1580)

    # ------------------------------------------------ states
    def state(icon, tone, ink, title, text, action, kind='tonal', extra=''):
        return (f'<div style="border-radius: 28px; background: var(--card); box-shadow: var(--e1); padding: 28px 22px 24px; display: flex; flex-direction: column; align-items: center; text-align: center; gap: 12px; min-height: 300px">'
                f'{extra}<span style="width: 72px; height: 72px; border-radius: 24px; background: {tone}; color: {ink}; display: flex; align-items: center; justify-content: center">{ms(icon, "f", "font-size: 36px")}</span>'
                f'<span class="ty-title" style="font-size: 18px">{title}</span><span class="ty-cap" style="font-size: 13.5px; line-height: 19px; max-width: 250px">{text}</span>'
                f'<span style="flex-grow: 1"></span>{btn(action, kind, h=44)}</div>')
    sk = lambda w, h=12: f'<i style="height: {h}px; width: {w}%; border-radius: {h // 2}px; background: var(--sf-high)"></i>'
    skeleton = (f'<div style="border-radius: 28px; background: var(--card); box-shadow: var(--e1); padding: 22px; display: flex; flex-direction: column; gap: 12px; min-height: 300px">'
                f'<i style="height: 120px; border-radius: 18px; background: linear-gradient(100deg, var(--sf-high) 30%, var(--sf-c) 50%, var(--sf-high) 70%)"></i>{sk(80, 16)}{sk(94)}{sk(88)}{sk(60)}'
                f'<span style="flex-grow: 1"></span><span style="height: 44px; border-radius: 22px; background: var(--sf-low); display: flex; align-items: center; gap: 8px; padding: 0 14px; font-size: 13px; font-weight: 600; position: relative; overflow: hidden">'
                f'{ms("progress_activity", "xs", "color: var(--pri)")}<span style="flex-grow: 1">Загрузка · north-guide.ru</span>{ms("close", "xs")}<i style="position: absolute; left: 0; bottom: 0; width: 45%; height: 3px; background: var(--pri)"></i></span></div>')
    pri = ('var(--pri-c)', 'var(--on-pri-c)')
    empty = ''.join([state('history', *pri, 'История пуста', 'Здесь появятся сайты, которые вы откроете. Приватные вкладки сюда не попадают.', 'Открыть новую вкладку'),
                     state('download', *pri, 'Загрузок пока нет', 'Файлы сохраняются в «Загрузки» телефона. Каждый файл Vola проверяет перед открытием.', 'Выбрать папку'),
                     state('bookmark', *pri, 'Избранное пусто', 'Нажмите звёздочку в меню страницы или перенесите закладки из другого браузера.', 'Перенести закладки'),
                     state('key', *pri, 'Паролей пока нет', 'Vola предложит сохранить пароль при входе. Можно перенести из Chrome или Bitwarden.', 'Перенести пароли')])
    errors = ''.join([skeleton,
                      state('wifi_off', 'var(--sf-high)', 'var(--on-sf-v)', 'Сайт не отвечает', 'north-guide.ru долго не отвечает. Возможно, он перегружен или недоступен в вашей сети.', 'Повторить', 'fill'),
                      state('gpp_bad', 'var(--err-c)', 'var(--on-err-c)', 'Небезопасное соединение', 'Сертификат сайта не подходит к адресу. Данные могут прочитать по пути.', 'Вернуться назад', 'fill'),
                      state('fingerprint', *pri, 'Пароли заблокированы', 'Подтвердите, что это вы, чтобы увидеть или заполнить пароль.', 'Разблокировать', 'fill')])
    body = (f'<div style="position: absolute; inset: 0; padding: 48px 56px; display: flex; flex-direction: column; gap: 20px">'
            + base_head('Vola v4 · основа', 'Состояния', 'Каждый список знает, что показать, когда в нём ничего нет, пока данные грузятся и когда что-то пошло не так. Одна схема: значок, заголовок, одно предложение, одно действие.')
            + '<h2 class="ty-title" style="padding-top: 4px">Пусто</h2>'
            + f'<div style="display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 20px">{empty}</div>'
            + '<h2 class="ty-title" style="padding-top: 4px">Загрузка и ошибки</h2>'
            + f'<div style="display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 20px">{errors}</div></div>')
    W('States', 'состояния', 'w-work t-light', body, w=1440, h=940)
