# Zen Browser: визуальный язык и взаимодействие (состояние на 2025–2026, Zen 1.19–1.23b)

Метод: основной источник — исходники `zen-browser/desktop` (ветка `dev`, последний коммит 10.10.2026), значения по умолчанию из `prefs/zen/*.yaml`, документация `zen-browser/docs` (обновлена 24.04.2026) и заметки о выпусках `zen-browser/www` (`src/release-notes/stable.json`, выпуски по 1.23.2b от 09.10.2026). Сайты docs.zen-browser.app, omgubuntu, slashgear через WebFetch не открылись (DNS), поэтому документацию читал из репозитория, а отзывы — только по выдаче поиска. Код в Vola не копируем: ниже только числа и идеи.

Сокращения ссылок:
- D = https://github.com/zen-browser/desktop/blob/dev/
- DOCS = https://github.com/zen-browser/docs/blob/main/content/docs/
- RN = https://github.com/zen-browser/www/blob/main/src/release-notes/stable.json (то же на https://zen-browser.app/release-notes)

Терминология: в 2026 году в интерфейсе и заметках о выпусках «Workspaces» называются **Spaces** (код — `src/zen/spaces/`), а настройки ещё называются `zen.workspaces.*`. Документация от апреля 2026 пишет «workspaces» — это устаревшее название. — [RN](https://github.com/zen-browser/www/blob/main/src/release-notes/stable.json); [D src/zen/spaces](https://github.com/zen-browser/desktop/tree/dev/src/zen/spaces)

## Пространства (Spaces): показ, переключение, градиент

### Вывод
Пространство — это свой набор вкладок, иконка (эмодзи или символ) и градиентная тема окна. Переключатель — ряд иконок внизу боковой панели: неактивные серые и полупрозрачные, при переполнении сворачиваются в точки. Переключение — горизонтальный сдвиг содержимого панели (пружина без отскока, 250 мс), который ведётся за пальцем на тачпаде, а фон перетекает в градиент нового пространства. Градиент задаётся на цветовом круге из 1–3 точек, которые связаны правилом цветовой гармонии; есть ползунки прозрачности и зернистости.

### Найденные факты
**Показ**
- Документация: иконки пространств «at the bottom of the sidebar». Первое пространство создают кликом по «Default» → «+». У каждого пространства своя иконка, имя и контейнер по умолчанию (отдельные cookie). Ограничение: контейнер делит cookie, но не историю и не расширения. — [DOCS user-manual/workspaces.mdx](https://github.com/zen-browser/docs/blob/main/content/docs/user-manual/workspaces.mdx)
- Кнопки переключателя: 30×30 px (в одном из режимов 32×32), промежуток 3 px, радиус как у кнопок панели (`--toolbarbutton-border-radius: 6px`). Неактивная иконка: `filter: grayscale(1); opacity: 0.7`, активная — в цвете и с `opacity: 1`. Переходы: filter и opacity 0.2 s, width 0.1 s. Когда иконок слишком много, неактивные сворачиваются в точки 8×8 px (радиус 50%). В режиме перестановки остальные иконки гаснут до opacity 0.2. Размер шрифта иконок 12–16 px. — [D src/zen/spaces/zen-workspaces.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/spaces/zen-workspaces.css); [D zen-theme.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/common/styles/zen-theme.css)
- Индикатор текущего пространства (имя и иконка) вверху списка вкладок: высота 44 px при развёрнутой панели и 38 px при свёрнутой, font-weight 500, размер шрифта small, фон при наведении скруглён на `--border-radius-medium` (14 px), иконка 16×16. — [D zen-theme.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/common/styles/zen-theme.css); [D zen-workspaces.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/spaces/zen-workspaces.css)
- 1.21b: улучшен доступ к переполненным иконкам пространств и отклик при наведении. 1.23b: на macOS кнопки внизу панели крупнее, а при панели справа их порядок зеркальный. — [RN](https://github.com/zen-browser/www/blob/main/src/release-notes/stable.json)

**Переключение**
- Настройки по умолчанию: `zen.workspaces.swipe-actions = true`, `swipe-actions.delta-multiplier = 100`, `swipe-actions.edge-actions = true`, `switch-animation-duration = 250` (мс), `wrap-around-navigation = true` (по кругу), `scroll-modifier-key = ctrl` (Ctrl + колесо), `natural-scroll = false`. При перетаскивании вкладки к краю пространство переключается: `zen.workspaces.dnd-switch-padding = 20` px, `zen.tabs.dnd-switch-space-delay = 500` мс. — [D prefs/zen/workspaces.yaml](https://github.com/zen-browser/desktop/blob/dev/prefs/zen/workspaces.yaml); [D prefs/zen/zen.yaml](https://github.com/zen-browser/desktop/blob/dev/prefs/zen/zen.yaml)
- Анимация: контейнеры вкладок сдвигаются по `translateX(...%)`; во время жеста смещение равно половине пройденного расстояния (`offsetPixels / 2`). Завершение — `motion.animate(..., { type: "spring", bounce: 0, duration: 0.25 s })`. Переменная прозрачности фона `--zen-background-opacity` анимируется той же пружиной: градиент старого пространства перетекает в новый. Используется библиотека Motion (motion.dev), вшитая как `zen-vendor/motion.min.mjs`. — [D src/zen/spaces/ZenSpaceManager.mjs](https://github.com/zen-browser/desktop/blob/dev/src/zen/spaces/ZenSpaceManager.mjs); [D ZenUIManager.mjs](https://github.com/zen-browser/desktop/blob/dev/src/zen/common/modules/ZenUIManager.mjs)
- 1.19.6b: переключение больше не блокируется, следующее пространство можно выбрать, пока идёт анимация. 1.19.12b: улучшены ощущение и скорость свайпа между пространствами. 1.23b: свайп вправо на последнем пространстве создаёт новое; для этого есть отдельная анимация «прыжка» иконки «+» (`zen-swipe-add-icon-jump`, модуль `ZenSpaceAddSwipe.mjs`). — [RN](https://github.com/zen-browser/www/blob/main/src/release-notes/stable.json); [D zen-animations.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/common/styles/zen-animations.css)
- Тактильная отдача включена по умолчанию: `zen.haptic-feedback.enabled = true` (macOS, трекпад). — [D prefs/zen/zen.yaml](https://github.com/zen-browser/desktop/blob/dev/prefs/zen/zen.yaml)
- 1.19.6b и 1.19.8b: Zen учитывает системную настройку «уменьшить движение» и отключает или упрощает второстепенные анимации. — [RN](https://github.com/zen-browser/www/blob/main/src/release-notes/stable.json)
- Новое в 2026: Space Routing (1.21b) — правила, по которым ссылки домена открываются в нужном пространстве. 1.22b — синхронизация пространств через Mozilla-аккаунт. 1.22.1b — пространством, папкой или сплитом можно поделиться ссылкой. — [RN](https://github.com/zen-browser/www/blob/main/src/release-notes/stable.json)

**Генератор градиента**
- Цветовой круг с перетаскиваемыми точками. Правила гармонии (смещение по тону, в градусах): complementary [180], singleAnalogous [310], splitComplementary [150, 210], analogous [50, 310], triadic [120, 240]. Можно задать от 1 до 3 точек: правило выбирается по их числу, а при добавлении или удалении точки переключается на соседнее. — [D src/zen/spaces/ZenGradientGenerator.mjs](https://github.com/zen-browser/desktop/blob/dev/src/zen/spaces/ZenGradientGenerator.mjs)
- Светлота зависит от расстояния точки до центра круга (режимы `explicit-lightness` и `explicit-black-white`). Ползунок прозрачности 0.25–0.8 (на macOS 0.30–0.8), по умолчанию 0.4. Ползунок «текстуры» (зерна) — круговой регулятор 0…1, который задаёт `--zen-grainy-background-opacity`. — [D ZenGradientGenerator.mjs](https://github.com/zen-browser/desktop/blob/dev/src/zen/spaces/ZenGradientGenerator.mjs); [D theme-picker.inc](https://github.com/zen-browser/desktop/blob/dev/src/browser/base/content/zen-panels/theme-picker.inc)
- Как строится фон. 2 цвета — два `linear-gradient` под углами `rotation` и `rotation+180°`, у каждого цвет держится до 30% и уходит в прозрачность к 120%. 3 цвета — `linear-gradient(-5deg, c1 10%, transparent 80%)` плюс `radial-gradient(circle at 95% 0%, c3 0%, transparent 75%)` плюс `radial-gradient(circle at 0% 0%, c2 10%, transparent 70%)`. Пользовательские цвета — линейный градиент с равномерными остановками. У панели инструментов цвета всегда непрозрачные. Базовый цвет панели: тёмная тема [23, 23, 26], светлая [240, 240, 244]; прозрачность 0.6, если боковой панели разрешена прозрачность, иначе 1. — [D ZenGradientGenerator.mjs](https://github.com/zen-browser/desktop/blob/dev/src/zen/spaces/ZenGradientGenerator.mjs)
- Цвет текста на градиенте выбирается автоматически: сравниваются коэффициенты контраста тёмного и светлого текста. Сдвиг в пользу тёмной темы `zen.theme.dark-mode-bias = 0.3` уменьшает альфу светлого текста. — [D ZenGradientGenerator.mjs](https://github.com/zen-browser/desktop/blob/dev/src/zen/spaces/ZenGradientGenerator.mjs); [D prefs/zen/theme.yaml](https://github.com/zen-browser/desktop/blob/dev/prefs/zen/theme.yaml)
- Акцент по умолчанию — системный (`zen.theme.accent-color = "AccentColor"`). Производные цвета получаются через `color-mix`. Светлая тема: primary = акцент 50% + чёрный 50%, secondary = primary 20% + белый, tertiary = акцент 2% + белый. Тёмная тема: primary = акцент 20% + фон 80%, secondary = акцент 30% + фон. — [D zen-theme.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/common/styles/zen-theme.css)
- 1.19.9b: известная проблема — Essentials и папки выглядят перенасыщенными в некоторых темах. — [RN](https://github.com/zen-browser/www/blob/main/src/release-notes/stable.json)

### Выводы
- Для Vola (профили, как в Safari): переключение профиля горизонтальным свайпом по нижней панели или обзору вкладок с пружиной без отскока около 250 мс; фон, окрашенный цветом профиля, перетекает одновременно со сдвигом. Это совпадает с решением «акцент профиля — в обзоре и на новой вкладке».
- Приём «неактивные иконки серые и полупрозрачные (0.7), активная в цвете» подходит для полоски профилей. Свёртка в точки 8 px при переполнении — для узкого телефона.
- Генератор гармоний из 1–3 точек и автоматический выбор цвета текста по контрасту — готовая модель для «цвета профиля». На мобильном её стоит упростить до одного цвета: дальше гармония и градиент строятся автоматически.

### Пробелы
- Набор иконок или эмодзи для пространств (`ZenSpaceIcons.mjs`) подробно не изучал.
- Скриншотов текущей версии не смотрел: сайты из этой среды не открывались. Описание визуала восстановлено по CSS.

## Essentials и закреплённые вкладки

### Вывод
Essentials — сетка плиток с фавиконками над списком вкладок: до 12 плиток, обычно 4 колонки, высота плитки 46 px, радиус 14 px, промежуток 4 px. Подложкой плитки служит размытая (20 px) фавиконка самого сайта. По умолчанию у каждого пространства свои Essentials. Закреплённые вкладки помнят исходный URL и умеют к нему возвращаться.

### Найденные факты
- `zen.tabs.essentials.max = 12`; `zen.workspaces.separate-essentials = true` (свои Essentials у каждого пространства); `zen.theme.essentials-favicon-bg = true`. — [D prefs/zen/zen.yaml](https://github.com/zen-browser/desktop/blob/dev/prefs/zen/zen.yaml); [D prefs/zen/workspaces.yaml](https://github.com/zen-browser/desktop/blob/dev/prefs/zen/workspaces.yaml); [D prefs/zen/theme.yaml](https://github.com/zen-browser/desktop/blob/dev/prefs/zen/theme.yaml)
- Сетка: `grid-template-columns: repeat(auto-fit, minmax(max(23.7%, tab-min-height+4px), 1fr))`, то есть 4 колонки; есть варианты с 30% (3 колонки) и с 25%/48 px. Промежуток 4 px. `max-height` и `grid-template-columns` меняются за 0.3 s ease-out. Высота плитки `--tab-min-height: 46px`, радиус `--border-radius-medium: 14px`. — [D src/zen/tabs/zen-tabs/vertical-tabs.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/tabs/zen-tabs/vertical-tabs.css)
- Подложка: фавиконка растянута за пределы плитки (`inset: -50%`) и размыта `filter: blur(20px)`. Выбранная плитка: светлая тема `rgba(255,255,255,0.85)`; тёмная — акцент, смешанный с `rgba(132,132,132,0.85)` (40%). Внутренний отступ подложки 2 px, радиус 14−2 px; фон при наведении меняется за 0.1 s. — [D vertical-tabs.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/tabs/zen-tabs/vertical-tabs.css)
- Обычная вкладка: радиус 8 px (`--tab-border-radius`), вертикальный отступ `--tab-margin-block: 2px`, отступ панели `--zen-min-toolbox-padding: 5px` (macOS 6 px). Нажатая вкладка сжимается до 0.985 (`--zen-active-tab-scale`). — [D vertical-tabs.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/tabs/zen-tabs/vertical-tabs.css); [D zen-theme.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/common/styles/zen-theme.css)
- Закреплённые вкладки: если URL ушёл от закреплённого, появляется кнопка «вернуть к исходному URL» (`zen-pinned-changed`). Сочетание закрытия делает «reset-unload-switch» (сброс, выгрузка, переход). По умолчанию `zen.tabs.open-pinned-in-new-tab = true`. Ссылки на внешние сайты из Essentials и закреплённых вкладок открываются в Glance без клавиши-модификатора (`zen.glance.open-essential-external-links = true`). — [D prefs/zen/workspaces.yaml](https://github.com/zen-browser/desktop/blob/dev/prefs/zen/workspaces.yaml); [D prefs/zen/glance.yaml](https://github.com/zen-browser/desktop/blob/dev/prefs/zen/glance.yaml); [DOCS glance.mdx](https://github.com/zen-browser/docs/blob/main/content/docs/user-manual/glance.mdx)
- Пользователи, перешедшие с Arc, советуют включать отдельные Essentials для каждого пространства, «как в Arc». — [Reddit через redlib (выдача поиска)](https://redlib.hbubli.cc/r/zen_browser/comments/1kyxrqa/im_switching_from_arc)
- Есть промо-блок Essentials (`zen-essentials-promo.css`); в 1.21.7b и 1.21.8b исправлен пустой промежуток, который он оставлял после Esc. Пользователи жаловались на пустое место над Essentials в компактном режиме. — [RN](https://github.com/zen-browser/www/blob/main/src/release-notes/stable.json); [выдача поиска по r/zen_browser](https://reddit.sudovanilla.org/r/zen_browser/top)

### Выводы
- Для «Закреплённых сайтов» Vola на новой вкладке и в обзоре: сетка 4 колонки, плитка около 46–56 dp, радиус около 14 dp, промежуток 4–8 dp. Приём «размытая фавиконка как подложка» даёт цвет без ручной настройки и хорошо смотрится на OLED.
- «Вернуть закреплённую вкладку к исходному URL» — сильная идея для закреплённых сайтов в профиле.
- Открытие внешних ссылок из закреплённого сайта в предпросмотре удерживает «приложение-сайт» на месте.

### Пробелы
- Поведение Essentials в свёрнутой (узкой) панели и их анимацию при перестановке подробно не изучал.

## Компактный режим

### Вывод
Компактный режим прячет боковую панель (по умолчанию) и, если включить, верхнюю панель. Они всплывают при наведении на край окна как плавающая карточка с отступом 8 px. Показ — 0.25 s по кривой `linear()` с перелётом около 1%, скрытие — 0.15 s ease; у всплывшей панели фон «акрил» с размытием 42 px.

### Найденные факты
- Значения по умолчанию: `zen.view.compact.hide-tabbar = true`, `hide-toolbar = false`, `show-sidebar-and-toolbar-on-hover = true`, `animate-sidebar = true`, `sidebar-keep-hover.duration = 150` мс, `toolbar-hide-after-hover.duration = 1000` мс, `toolbar-flash-popup = false` (длительность вспышки 800 мс), `outside-window-edge-offset.horizontal = 200` px и `.vertical = 100` px, `show-background-tab-toast = true` (тост, когда вкладка открылась в фоне). — [D prefs/zen/compact-mode.yaml](https://github.com/zen-browser/desktop/blob/dev/prefs/zen/compact-mode.yaml)
- Документация: в режиме одной панели компактный режим прячет боковую панель, она появляется при наведении на боковой край. В режиме нескольких панелей можно спрятать боковую, верхнюю или обе; верхняя появляется у верхнего края. Сочетания «Toggle Floating Sidebar/Toolbar» закрепляют панель до повторного нажатия. — [DOCS compact-mode.mdx](https://github.com/zen-browser/docs/blob/main/content/docs/user-manual/compact-mode.mdx)
- Анимация показа: `transition: translate 0.25s linear(...)`. Кривая — табличная пружина: 0.5 к 20% времени, 0.94 к 45%, 1.0 к 59%, максимум 1.0109 около 72–73%, к концу 1.0034. Скрытие: `translate 0.15s ease, visibility 0.15s ease`. Спрятанная панель сдвинута за край на `100% − float − 1px`. — [D src/zen/compact-mode/sidebar.inc.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/compact-mode/sidebar.inc.css)
- Плавающая панель: отступ `--zen-compact-float` равен `--zen-element-separation` (8 px), без отступов вокруг страницы — 10 px. Акриловый фон: `backdrop-filter: blur(42px) saturate(110%) brightness(0.5)`; выключается при `prefers-reduced-transparency`. — [D sidebar.inc.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/compact-mode/sidebar.inc.css)
- Верхняя панель: высота и прозрачность меняются за `--zen-hidden-toolbar-transition: 0.15s ease-in-out`; у сдвига страницы задержка 0.2 s. — [D compact-mode/toolbar.inc.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/compact-mode/toolbar.inc.css); [D zen-browser-container.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/common/styles/zen-browser-container.css)
- Полноэкранный режим без рамки: при полном экране и компактном режиме отступ вокруг страницы становится 0 (`zen.view.borderless-fullscreen = true`). — [D zenThemeModifier.js](https://github.com/zen-browser/desktop/blob/dev/src/zen/common/zenThemeModifier.js); [D prefs/zen/view.yaml](https://github.com/zen-browser/desktop/blob/dev/prefs/zen/view.yaml)
- 1.21.11b: на Windows и Linux компактный режим следит за мышью и вне окна — панель не прячется, пока курсор рядом с краем. 1.17b: добавлена кнопка переключения компактного режима. 1.11.1b: исправлен сдвиг сайта при панели справа. — [RN](https://github.com/zen-browser/www/blob/main/src/release-notes/stable.json); [Neowin, выдача поиска](https://www.neowin.net/software/zen-browser-117b/)
- Компактный режим без отвлекающих элементов отмечали как сильную сторону (обзор Slashdot, 2024). — [Slashdot (выдача поиска)](https://news.slashdot.org/story/24/09/23/0353206/zen-browser-a-new-firefox-based-alternative-to-chromium-browsers)

### Выводы
- Мобильный аналог — панели, которые прячутся при прокрутке, как в Safari: появление пружиной с небольшим перелётом (около 1%, около 250 мс), исчезновение быстрее и без перелёта (около 150 мс). Асимметрия «показ медленнее и живее, скрытие короче» — главный урок.
- Тост «вкладка открыта в фоне» полезен, когда панели спрятаны.

### Пробелы
- Точных параметров пружины, из которой получена `linear()`-таблица, нет: в коде только сэмплы.

## Glance (предпросмотр ссылки)

### Вывод
Alt+клик по ссылке (в Essentials и закреплённых вкладках — любой клик по внешней ссылке) открывает сайт в плавающей карточке шириной 80% поверх страницы. Страница под ней уменьшается до 0.97 и тускнеет до 30% непрозрачности. Карточка вылетает из точки клика по дуге, пружина с bounce 0.2 за 350 мс. Слева сверху колонка круглых кнопок: закрыть, развернуть во вкладку, открыть в сплите.

### Найденные факты
- Значения по умолчанию: `zen.glance.enabled = true`, `activation-method = "alt"` (можно ctrl или shift), `animation-duration = 350` мс, `enable-contextmenu-search = true` (поиск выделенного текста в Glance), `open-essential-external-links = true`. — [D prefs/zen/glance.yaml](https://github.com/zen-browser/desktop/blob/dev/prefs/zen/glance.yaml)
- Документация: три кнопки слева сверху — закрыть (или клик вне Glance), развернуть в новую вкладку, добавить сайт в сплит. — [DOCS glance.mdx](https://github.com/zen-browser/docs/blob/main/content/docs/user-manual/glance.mdx)
- Анимация открытия: фоновая страница `scale: [1, 0.97], opacity: [1, 0.3]`, `type: "spring", bounce: 0.2, duration: 0.35 s`. Карточка летит по дуге от места клика: 80 ключевых кадров, высота дуги = расстояние × 0.2, но не больше 20 px; при этом растут scaleX и scaleY. Содержимое проявляется (opacity 0→1) за последние 0.2 s пружиной без отскока. Закрытие: фон возвращается `scale: [0.97, 1], opacity: [0.3, 1]` пружиной bounce 0 за 350/1.5 ≈ 233 мс. При повторной попытке открыть есть «встряска» `scale [1, 1.005, 1]` за 250 мс. — [D src/zen/glance/ZenGlanceManager.mjs](https://github.com/zen-browser/desktop/blob/dev/src/zen/glance/ZenGlanceManager.mjs)
- Геометрия: ширина карточки 80%, высота 100% области; радиус как у страницы; тень `--zen-big-shadow`; фон карточки до загрузки белый или rgb(17,17,17). Колонка кнопок: `top: 15px`, `gap: 12px`, `max-width: 56px`, кнопки круглые (`border-radius: 999px`), тень `0 0 12px 1px rgba(0,0,0,0.07)`, при наведении `scale 1.02`, при нажатии 0.98, переходы 0.05 s. Кнопка закрытия раскрывается красной (rgb(220,53,69)) подписью-подтверждением: `max-width 0 → 4rem` за 0.2 s. Без отступов вокруг страницы радиус 6 px и отступ 6 px. — [D src/zen/glance/zen-glance.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/glance/zen-glance.css); [D zen-glance.inc.xhtml](https://github.com/zen-browser/desktop/blob/dev/src/zen/glance/zen-glance.inc.xhtml)
- 1.19.4b: ускорены Glance и его анимации. 1.21.4b: переход в полный экран из Glance превращает его в обычную вкладку. 1.23b: исправлены цвета кнопок Glance. — [RN](https://github.com/zen-browser/www/blob/main/src/release-notes/stable.json)
- Обзоры сравнивают Glance с «Little Arc / Peek» в Arc. — [efficient.app (выдача поиска)](https://efficient.app/compare/arc-browser-vs-zen); [supasidebar (выдача поиска)](https://supasidebar.com/blog/zen-vs-arc)

### Выводы
- На Android естественный триггер — долгое нажатие на ссылку → «Предпросмотр» (как Peek в Safari), а для закреплённых сайтов — автоматически для внешних ссылок. Карточка-лист поверх страницы, страница под ней 0.97 и затемнена, пружина около 350 мс с bounce около 0.2. Действия «Открыть во вкладке» и «Закрыть» (свайп вниз); сплита на телефоне нет.
- Вылет из точки касания по дуге — сильный фирменный момент для `plan-v5.md` (раздел 5), при отключённых анимациях — только смена прозрачности.

### Пробелы
- Есть ли в Glance жест закрытия (свайп), кроме клика вне карточки, — в коде не проверял.

## Разделённый экран (Split View)

### Вывод
До 4 вкладок рядом: горизонтально, вертикально или сеткой. Сплит создают перетаскиванием вкладки к краю страницы, Alt+кликом по вкладке (1.19.9b) или через «Split link». Промежуток между панелями равен отступу вокруг страницы (8 px). Ручка изменения размера невидима, только у горизонтального разделителя при наведении появляется полоска 50×2 px. Активная панель обведена акцентной рамкой 2 px.

### Найденные факты
- Документация: до 4 вкладок; у активной панели сверху накладка с ручкой «:::» (перенести влево, вправо, вверх или вниз) и кнопкой «‒» (вынуть из сплита); сочетания для раскладок horizontal, vertical, grid. — [DOCS split-view.mdx](https://github.com/zen-browser/docs/blob/main/content/docs/user-manual/split-view.mdx)
- Значения по умолчанию: `zen.splitView.min-resize-width = 7` (минимальная доля панели при изменении размера, судя по коду — в процентах), `rearrange-hover-size = 24`, `drag-over-split-threshold = 40`, `drag-over-split-delayMC = 500` мс, `enable-tab-drop` и `enable-tab-click-split` включены. — [D prefs/zen/split-view.yaml](https://github.com/zen-browser/desktop/blob/dev/prefs/zen/split-view.yaml); [D ZenViewSplitter.mjs](https://github.com/zen-browser/desktop/blob/dev/src/zen/split-view/ZenViewSplitter.mjs)
- CSS: промежуток между строками равен `--zen-element-separation`, между колонками — на 1 px больше. Обводка активной панели 2 px: светлая тема — акцент с светлотой −20, тёмная — цвет основной кнопки. `inset` меняется за 0.08–0.09 s ease-out, opacity — за 0.2 s. Разделитель-полоска 50×2 px, радиус 2 px, появляется за 0.1 s. Накладка-заголовок скруглена на 6 px и имеет тень `0 0 5px 1px rgba(0,0,0,.1)`. — [D src/zen/split-view/zen-split-view.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/split-view/zen-split-view.css); [D zen-browser-ui.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/common/styles/zen-browser-ui.css)
- 1.17b: исправлено медленное переключение сплитов. 1.19.6b: пункт меню переименован в «Join tabs». 1.19.3b: сплит прячется в полном экране. — [RN](https://github.com/zen-browser/www/blob/main/src/release-notes/stable.json); [Neowin (выдача поиска)](https://www.neowin.net/software/zen-browser-117b/)

### Выводы
- На телефоне сплит не нужен. На планшете (WindowSizeClass Expanded) — 2 панели с промежутком в токен отступа, невидимой ручкой и короткой подсветкой полоски при касании, рамка 2 dp на активной панели.

### Пробелы
- Сколько времени идёт анимация перестановки панелей, точно не установлено.

## Общий стиль: рамка, отступы, тени, прозрачность, шрифт, размеры, движение, темы

### Вывод
Главный визуальный приём Zen — страница как «плавающая карточка». Вокруг неё отступ 8 px (со стороны боковой панели отступа нет, есть промежуток), лёгкая тень `0 3px 8px rgba(0,0,0,.24)` и небольшой радиус, согласованный с радиусом окна ОС. Оболочка окрашена градиентом пространства и может быть полупрозрачной (Mica, vibrancy, акрил). С 1.22 (сентябрь 2026) оболочка перешла на «сквиркл»-углы и крупнее шрифт в панели, с 1.23 — размытие у боковой панели и командной строки. Движение построено на пружинах Motion (bounce 0–0.2, 250–350 мс) и кривых с перелётом.

### Найденные факты
**Радиусы и отступы**
- `zen.theme.content-element-separation = 8` (px) задаёт `--zen-element-separation`, то есть отступы вокруг страницы и промежутки между панелями. `zen.theme.border-radius = -1` означает радиус ОС: macOS 10 px (11 px в теме Tahoe), Linux — радиус заголовка GTK CSD или 8 px, Windows 9 px. Запасное значение в CSS — 7 px. — [D prefs/zen/theme.yaml](https://github.com/zen-browser/desktop/blob/dev/prefs/zen/theme.yaml); [D zenThemeModifier.js](https://github.com/zen-browser/desktop/blob/dev/src/zen/common/zenThemeModifier.js); [D zen-theme.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/common/styles/zen-theme.css)
- Внутренний радиус страницы: `--zen-native-inner-radius = max(5px, (radius − separation/2) × squircle)`, на карточке он делится обратно на squircle и рисуется с `corner-shape: round`. Коэффициент `--zen-squircle-value` 1.3 (Windows 2.3). Комментарий в коде: «inner radius = outer radius − separation», то есть концентрические углы. — [D zen-theme.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/common/styles/zen-theme.css); [D zen-browser-container.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/common/styles/zen-browser-container.css)
- Обёртка вкладок `#zen-tabbox-wrapper { margin: separation; margin-top: 0 }`, со стороны панели margin 0; между панелью и страницей `gap: separation`. — [D zen-browser-ui.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/common/styles/zen-browser-ui.css)
- Прочие радиусы: вкладка 8 px, кнопки панели 6 px, `--border-radius-medium` 14 px (Essentials, индикатор пространства, всплывающие панели), раскрытая адресная строка 15 px, иконки и внутренняя часть адресной строки 8 px. — [D vertical-tabs.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/tabs/zen-tabs/vertical-tabs.css); [D zen-omnibox.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/common/styles/zen-omnibox.css)
- 1.22b (03.09.2026): «The chrome UI now has a better look and feel, implementing squircles…»; крупнее шрифт текста в боковой панели. — [RN](https://github.com/zen-browser/www/blob/main/src/release-notes/stable.json)

**Тени, прозрачность, размытие**
- Карточка страницы: `box-shadow: rgba(0,0,0,0.24) 0px 3px 8px` (`--zen-big-shadow`), `overflow: clip`. Фон страницы до отрисовки: белый или rgb(32,32,32); у прозрачных страниц — `rgba(255,255,255,.6)` или `.1`. — [D zen-browser-container.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/common/styles/zen-browser-container.css); [D zen-theme.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/common/styles/zen-theme.css)
- Плавающая адресная строка (`zen.urlbar.behavior = "floating-on-type"`: всплывает по центру, как командная строка, когда начинаешь печатать): радиус 15 px, тень `0 30px 140px -15px rgba(0,0,0,.8)` в светлой теме и `.6` в тёмной, `backdrop-filter: blur(40px)`, высота строки подсказки не меньше 45 px, контейнер 52 px. В режиме поиска — свечение акцентом, которое гаснет за 1 s. — [D zen-omnibox.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/common/styles/zen-omnibox.css); [D zen-animations.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/common/styles/zen-animations.css); [D prefs/zen/zen-urlbar.yaml](https://github.com/zen-browser/desktop/blob/dev/prefs/zen/zen-urlbar.yaml)
- Прочие размытия: акрил панели в компактном режиме 42 px, панель Boosts `saturate(2) blur(15px)`, мелкие элементы 10 px. 1.23b: боковая панель и командная строка получили размытие. Полупрозрачное окно — на Windows Mica, macOS и Linux (по настройке). — [D sidebar.inc.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/compact-mode/sidebar.inc.css); [D zen-theme.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/common/styles/zen-theme.css); [RN](https://github.com/zen-browser/www/blob/main/src/release-notes/stable.json)

**Цвета оболочки**
- Фон оболочки без темы: светлая rgb(235,235,235), тёмная #1b1b1b. Фирменные: тёмный #101010, «бумага» #e2e2e2. Диалоги #fafbff / #1c1c1c. Всплывающие панели rgb(244,244,244) / rgb(31,31,31). Фон кнопки при наведении: 7% контрастного цвета, при нажатии 10%. Приватное окно — почти чёрное rgb(11,10,11). Неактивное окно сереет (`zen.view.grey-out-inactive-windows = true`). — [D zen-theme.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/common/styles/zen-theme.css); [D prefs/zen/view.yaml](https://github.com/zen-browser/desktop/blob/dev/prefs/zen/view.yaml)

**Размеры и шрифт**
- Высота панели инструментов 38 px (macOS 42 px), панель закладок +30 px. Иконка кнопки 16 px (macOS 17 px), внутренний отступ 6 px, внешний 4 px. — [D zen-theme.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/common/styles/zen-theme.css)
- Ширина боковой панели по умолчанию 186 px (macOS 230 px), максимум развёрнутой 500 px. С 1.23b панель меняет ширину и в скрытом состоянии; если сделать её слишком узкой, она прячется. — [D ZenCustomizableUI.sys.mjs](https://github.com/zen-browser/desktop/blob/dev/src/zen/common/sys/ZenCustomizableUI.sys.mjs); [D prefs/zen/view.yaml](https://github.com/zen-browser/desktop/blob/dev/prefs/zen/view.yaml); [RN](https://github.com/zen-browser/www/blob/main/src/release-notes/stable.json)
- Шрифт — системный: macOS SF Pro (системный), Windows Segoe UI. Текст в боковой панели: Windows 14 px, macOS 1.25rem. Своего фирменного шрифта нет. — [D zen-theme.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/common/styles/zen-theme.css); [D vertical-tabs.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/tabs/zen-tabs/vertical-tabs.css)
- По умолчанию одна панель (`zen.view.use-single-toolbar = true`): адрес и кнопки в боковой панели, только домен (`show-domain-only-in-sidebar`); вкладки вертикальные, слева. Кнопки окна на macOS заменены тремя одноцветными точками, цветные «светофоры» появляются при наведении. Окно можно двигать за верхние 5% страницы. — [D prefs/zen/view.yaml](https://github.com/zen-browser/desktop/blob/dev/prefs/zen/view.yaml); [D zen-urlbar.yaml](https://github.com/zen-browser/desktop/blob/dev/prefs/zen/zen-urlbar.yaml); [D zen-browser-ui.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/common/styles/zen-browser-ui.css)

**Движение**
- Пружины Motion: пространства — bounce 0, 250 мс; Glance — bounce 0.2, 350 мс. CSS-кривые с перелётом: `cubic-bezier(0.34, 1.56, 0.64, 1)` 0.25 s (масштаб в приветствии), `cubic-bezier(0.14, 1.43, 0.56, 1.01)` 0.4 s и `cubic-bezier(0.11, 1.6, 0.63, 1)` 0.35 s (Boosts). Плавное торможение: `cubic-bezier(0.22, 1, 0.36, 1)` (открытие библиотеки), `cubic-bezier(0.2, 0.8, 0.2, 1)` 0.35 s (оверлей «Поделиться»), `cubic-bezier(0.25, 1, 0.5, 1)` (медиа), `cubic-bezier(0.075, 0.82, 0.165, 1)` 0.25–0.4 s. Пружина-таблица `linear(0, 0.34 12%, 0.7 24%, 0.93 38%, 1.03 55%, 1)` для появления элементов библиотеки. Микропереходы 0.05–0.2 s; появление диалога — opacity 0→1 со сдвигом translateY −10 px → 0. — [D zen-library-widget.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/common/styles/zen-library-widget.css); [D zen-welcome.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/welcome/zen-welcome.css); [D zen-share.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/share/zen-share.css); [D zen-animations.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/common/styles/zen-animations.css)
- 1.19.9b: новая анимация прогресса загрузки страницы (`zen-progress-bar-pulse / long-load / settle`). — [RN](https://github.com/zen-browser/www/blob/main/src/release-notes/stable.json); [D zen-animations.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/common/styles/zen-animations.css)

**Темы и Mods**
- Mods — CSS-моды из реестра zen-browser.app/mods, ставятся одной кнопкой; их настройки — about:config. Есть руководство по живой правке userChrome. Темизация опирается на CSS-переменные `--zen-*`: `--zen-primary-color`, `--zen-border-radius`, `--zen-element-separation`, `--zen-colors-*`, `--zen-big-shadow` и другие. — [DOCS themes-marketplace.mdx](https://github.com/zen-browser/docs/blob/main/content/docs/themes-store/themes-marketplace.mdx); [DOCS guides/live-editing.mdx](https://github.com/zen-browser/docs/blob/main/content/docs/guides/live-editing.mdx); [D zen-theme.css](https://github.com/zen-browser/desktop/blob/dev/src/zen/common/styles/zen-theme.css)
- 1.20b (май 2026): Boosts — правка вида любого сайта (оттенок, шрифты, «zap» элементов, автотёмная тема). — [RN](https://github.com/zen-browser/www/blob/main/src/release-notes/stable.json)

### Выводы
- Для Vola (Safari — главный ориентир): плавающую карточку страницы на телефоне лучше не делать, она съедает ширину. Её место — обзор вкладок и планшет, где страница — карточка с отступом 8 dp, концентрическим радиусом (внешний минус отступ) и мягкой тенью около `0 3 8 / 24%`. В полноэкранном и иммерсивном режиме отступ 0, как «borderless fullscreen» в Zen.
- Токены для `ui/theme` по образцу Zen: spacing.element = 8 dp; radius: small 6, tab 8, medium 14, sheet 15–16; motion: spring без отскока около 250 мс (переключения), spring bounce около 0.2 и 350 мс (выразительные появления), микропереходы 50–200 мс, скрытие быстрее показа.
- Тёмная оболочка Zen (#1b1b1b, #1c1c1c у диалогов) почти совпадает с решением Vola о фоне около #1C1C1E.

### Пробелы
- Высоту обычной вкладки в боковой панели (кроме Essentials, 46 px) точно не определил: значение берётся из Firefox и в изученных файлах не переопределено.
- Точный вид сквиркл-углов 1.22 (`corner-shape` и класс `no-squircles`) по CSS проверен лишь частично.

## Отзывы: что хвалят и что критикуют

### Вывод
Хвалят: спокойный минималистичный вид, вертикальные вкладки, компактный режим, пространства, сплит и Glance. Zen считают «живой» заменой Arc на Firefox. Критикуют: шероховатости и баги (сдвиги, пустые промежутки, отзывчивость на Windows), отсутствие расширений Chrome, привыкание к вкладкам слева; часть рецензентов считает Arc более отполированным. Крупного критического обзора от The Verge, Ars Technica, XDA или How-To Geek не нашёл; оценки ниже — из выдачи поиска (полные страницы не открылись).

### Найденные факты
- SlashGear: эстетика сильно вдохновлена Arc; по функциям Zen — почти копия Arc один к одному (пространства, кастомизация, сплит). — [SlashGear (выдача поиска)](https://www.slashgear.com/1957695/why-people-are-switching-to-zen-web-browser)
- OMG! Ubuntu (август 2025): Zen — «то, чем должен быть Firefox»; новые функции ощущаются органично, а не прикрученными. — [OMG! Ubuntu (выдача поиска)](https://www.omgubuntu.co.uk/2025/08/zen-browser-is-what-mozilla-firefox-should-be/amp)
- Linux Pro Magazine (2025): хвалит малый расход ресурсов и режимы показа (компактный, сплит); минус — нельзя показать несколько вкладок в одной строке. — [Linux Pro Magazine (выдача поиска)](https://www.linuxpromagazine.com/Issues/2025/294/Zen-Browser)
- Пользователь Wilders Security: к вкладкам слева сначала было непривычно, но это быстро прошло. — [Wilders Security (выдача поиска)](https://www.wilderssecurity.com/goto/post?id=3223436)
- Сравнения с Arc: Arc ощущается более отполированным; главная жалоба мигрантов — «нужны расширения Chrome». Часть авторов считает Zen менее надёжным и более глючным, другие — что активная разработка это исправит. — [efficient.app (выдача поиска)](https://efficient.app/compare/arc-browser-vs-zen); [supasidebar (выдача поиска)](https://supasidebar.com/blog/zen-vs-arc); [remotesynthesis (выдача поиска)](https://remotesynthesis.com/posts/migrating-from-arc-to-zen)
- Reddit r/zen_browser (зеркала, даты неточны): сдвиг страницы при компактной панели справа, нет «точки уведомления» на вкладках в компактном режиме, просят расширения по пространствам, пустое место над Essentials, вялая отзывчивость на Windows 11, панель закладок выскакивает при быстром движении мыши вверх. — [Reddit через зеркало (выдача поиска)](https://reddit.sudovanilla.org/r/zen_browser/top)
- Заметки о выпусках 2026 подтверждают работу над этими болями: скорость переключения пространств (1.19.2b, 1.21.1b — зависания), плавность Glance (1.19.4b), «reduce motion» (1.19.6b, 1.19.8b), упрощённое контекстное меню панели (1.19.8b), отслеживание мыши компактным режимом (1.21.11b). — [RN](https://github.com/zen-browser/www/blob/main/src/release-notes/stable.json)

### Выводы
- Сильные стороны Zen для Vola: минимум интерфейса вокруг страницы, цвет пространства или профиля как эмоциональная идентичность, предпросмотр ссылок. Слабые места, которых стоит избегать: резкие и неотзывчивые переходы, «дыры» в раскладке при скрытии панелей, анимации без учёта «уменьшить движение». Это совпадает с бюджетами качества `plan-v5.md` (тормозящие кадры, доступность).

### Пробелы
- Материалов The Verge, Ars Technica, XDA и How-To Geek о Zen 1.x не нашёл. Полные тексты SlashGear и OMG! Ubuntu не открылись (DNS), поэтому их оценки взяты из выдачи поиска и требуют проверки.
- Отзывов о самых новых изменениях (сквирклы 1.22, Library 1.23 в октябре 2026) пока нет.
