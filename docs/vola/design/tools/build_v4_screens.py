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


def group(rows, bg='var(--sf-lowest)'):
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
<div style="position: absolute; left: 8px; right: 8px; bottom: 22px; height: 56px; border-radius: 28px; background: var(--sf-lowest); box-shadow: 0 0 0 2px var(--pri), var(--e2); display: flex; align-items: center; gap: 2px; padding: 0 4px 0 16px; z-index: 4">
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
    cards = ''.join('<i style="height: 248px; border-radius: 22px; background: var(--sf-lowest)"></i>' for _ in range(4))
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
    menu = ('<div style="position: absolute; left: 20px; right: 20px; top: 452px; border-radius: 28px; background: var(--sf-lowest); box-shadow: var(--e3); padding: 6px; z-index: 5">'
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
            + f'<span style="height: 48px; border-radius: 24px; background: var(--sf-lowest); box-shadow: var(--e1); display: flex; align-items: center; gap: 8px; padding: 0 14px 0 4px">{gem("movie", 40, 14, "s")}<span style="font-size: 15px; font-weight: 600">Аниме</span></span>'
            + small('w-personal t-light', 'home') + '</div>'
            + f'<span style="width: 56px; height: 56px; border-radius: 20px; background: var(--pri); color: var(--on-pri); display: flex; align-items: center; justify-content: center">{ms("add", "l")}</span></div>'
            + handle())
    W('WorkspaceSwipe', 'смена пространства свайпом', 'w-work t-light aura', body)

    icons = ['work', 'home', 'movie', 'menu_book', 'star', 'palette', 'public', 'bolt', 'photo_camera', 'person', 'bookmark', 'travel_explore']
    ic = ''.join(f'<button role="radio" aria-checked="{"true" if k == 0 else "false"}" style="height: 48px; border-radius: {"16px" if k == 0 else "24px"}; background: {"var(--pri)" if k == 0 else "var(--sf-high)"}; color: {"var(--on-pri)" if k == 0 else "var(--on-sf-v)"}; display: flex; align-items: center; justify-content: center">{ms(n, "f s" if k == 0 else "s")}</button>' for k, n in enumerate(icons))
    head = lambda title, sub, trail='': f'<div style="display: flex; align-items: center; gap: 14px; padding: 4px 4px 0">{gem("work")}<span style="flex-grow: 1; display: flex; flex-direction: column; gap: 2px"><span class="ty-title-l">{title}</span><span class="ty-cap">{sub}</span></span>{trail}</div>'
    new_ws = sheet(head('Новое пространство', 'Свои вкладки, Essentials и цвет')
                   + '<label style="display: flex; flex-direction: column; gap: 6px"><span class="ty-label" style="padding: 0 4px">Название</span><span style="height: 56px; border-radius: 16px; background: var(--sf-lowest); box-shadow: inset 0 0 0 2px var(--pri); display: flex; align-items: center; padding: 0 16px; font-size: 17px; font-weight: 600">Работа<i style="width: 2px; height: 22px; margin-left: 1px; background: var(--pri)"></i></span></label>'
                   + '<span class="ty-label" style="padding: 0 4px">Цвет</span>' + color_dots()
                   + f'<span class="ty-label" style="padding: 0 4px">Значок</span><div style="display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 8px">{ic}</div>'
                   + group([row('shield_lock', 'Отдельное хранилище', 'Свои cookie, входы и данные сайтов', switch(True), h=60),
                            row('fingerprint', 'Вход по биометрии', 'Вкладки скрыты, пока вы не войдёте', switch(False), h=60)])
                   + btn('Создать пространство', 'fill', '', h=56), gap=12)
    W('WorkspaceSheet', 'новое пространство', 'w-work t-light aura', tabs_backdrop() + status() + scrim() + new_ws + handle())

    edit = f'<button class="ib4" aria-label="Переименовать" style="background: var(--sf-lowest); color: var(--on-sf)">{ms("edit")}</button>'
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
    add_sheet = sheet(f'<span class="ty-title" style="padding: 0 4px">Добавить из открытых вкладок</span><div style="border-radius: 24px; background: var(--sf-lowest); overflow: hidden">{add_rows}</div>', gap=12)
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
