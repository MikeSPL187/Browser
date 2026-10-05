# Инструменты холста дизайна

Инструменты и их библиотеки в приложение не попадают. Исключение — сгенерированный файл
`VolaSchemes.kt` (см. «Цвета»).

## Цвета

- `gencss.mjs` генерирует цвета из исходных цветов пространств (Material Color Utilities,
  схема M3 2025): светлая, тёмная (фон всегда `#000000`) и повышенный контраст. Серые исходники
  (Graphite) остаются серыми: яркий основной цвет и цветной ореол — только у цветных.
  - `node gencss.mjs > ../canvas/vola4-colors.css` — CSS холста (три примера пространств и
    приватная тёмная).
  - `node gencss.mjs --kotlin > <корень репозитория>/app/src/main/java/dev/sk2andy/materialbrowser/ui/theme/VolaSchemes.kt`
    — схемы приложения: все восемь `WorkspaceAccent` и приватный режим, по четыре варианта
    (светлая, тёмная, повышенный контраст светлая и тёмная), плюс ореол, ok/warn и карточка.
    Исходные цвета — тона 40 из `ui/theme/WorkspaceAccents.kt` (`appSeeds`); юнит-тест
    `VolaSchemesTest` сверяет их. Если контраст любой пары ниже 4,5:1, генератор падает с ошибкой.
    Файл не править руками.
  - `node gencss.mjs --kotlin-themes > <корень репозитория>/app/src/main/java/dev/sk2andy/materialbrowser/ui/theme/VolaThemeSchemes.kt`
    — нейтральные роли тем доски W-Themes («Лёд», «Сумерки», «Бумага», «Моно»): поверхности,
    текст на них, контуры, ореол и карточка, по четыре варианта. Основной, вторичный, третичный
    цвета, ошибка, ok и warn остаются у акцента пространства. В тёмных вариантах фон чистый
    чёрный. Генератор проверяет контраст 4,5:1 для каждой темы с каждым акцентом и падает при
    нарушении. Файл не править руками.
  - Запуск во временной папке, в приложение библиотека не попадает:
    `npm install @material/material-color-utilities@0.4.0`. Если Node не находит модули
    библиотеки, допишите `.js` к относительным импортам (команда безопасна при повторном запуске):
    `find node_modules/@material/material-color-utilities -name '*.js' -exec sed -i -E "s#(from '\.\.?/[^']+)';#\1.js';#; s#\.js\.js'#.js'#" {} +`.
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
