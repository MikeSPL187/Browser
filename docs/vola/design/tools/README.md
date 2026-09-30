# Инструменты холста дизайна

Всё здесь нужно только для макетов и в приложение не попадает.

## Цвета

- `gencss.mjs` генерирует `canvas/vola4-colors.css` из цветов пространств (Material Color
  Utilities, схема M3 2025): светлая, тёмная (фон всегда `#000000`) и повышенный контраст, плюс
  приватная тёмная. Запуск во временной папке:
  `npm install @material/material-color-utilities@0.4.0`, затем
  `node gencss.mjs > ../canvas/vola4-colors.css`. Если Node не находит модули библиотеки,
  допишите `.js` к относительным импортам в её файлах:
  `find node_modules/@material/material-color-utilities -name '*.js' -exec sed -i -E "s#(from '\./[^']+|from '\.\./[^']+)';#\1.js';#" {} +`.
- `contrast.py` проверяет контраст пар «текст — фон» в сгенерированном CSS (порог 4,5:1):
  `python3 contrast.py ../canvas/vola4-colors.css`. Запускать после каждой генерации.

## Экраны

- `build_v4.py` — доски выбора оформления и прототип (`canvas/V4*.dc.html`):
  `python3 build_v4.py page tabs address anime appearance proto`.
- `build_v4_screens.py` — все экраны v4 (`canvas/W-*.dc.html`), по группам `g_<имя>`:
  `python3 run_screens.py` собирает все группы, `python3 run_screens.py browse tabs` — выбранные.
- Картинки: `baikal.svg` (иллюстрация статьи), `map.svg` (карта для экрана разрешений).
- `icon_font_url.txt` — ссылка на Material Symbols Rounded с нужным набором значков. Новый значок
  нужно добавить в параметр `icon_names` (по алфавиту), иначе вместо него появится текст.
- В приложении значки те же, Material Symbols Rounded, но генерируются скриптом
  `scripts/compile_material_symbols.py`: `VolaIcons` для Compose (модуль `shared`) и vector
  drawables для меню, уведомлений и виджетов. Новый значок: строка в `COMPOSE_ICONS` или
  `DRAWABLES`, затем `python3 scripts/compile_material_symbols.py fetch`.

## Проверка глазами

Доски рендерятся локально: `python3 -m http.server` в папке с копиями досок, `support.js` и
CSS, затем Playwright (Chromium из `/opt/pw-browsers`). Шрифты Google скачиваются `curl` и
отдаются через `page.route`. Подробности — в `docs/vola/STATUS.md`, раздел «Заметки о среде».
