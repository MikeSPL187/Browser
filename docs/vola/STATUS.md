# Состояние проекта Vola

Файл для передачи работы между сессиями Claude Code. Новая сессия читает его первым и
обновляет в конце работы.

_Обновлено: 2026-09-30 (сессия PR 3: `AddressBarController` #28 и «Рама» 3a)._

## PR и сборка

- **PR #25 — PR 1 переноса дизайна: токены темы v4** (ветка `ccr-d3f28808-uk5drv`, метка
  `screenshots`).
  - Схемы 8 пространств и приватного режима генерируются `gencss.mjs --kotlin` в
    `ui/theme/VolaSchemes.kt`.
  - `VolaTheme` / `VolaExtendedColors`, шрифты Manrope и Literata, `VolaShapes`, `VolaSpacing`,
    `VolaElevation`, `VolaMotion`, настройка `BrowserChromeStyle` (Frame/Air, пока без UI).
  - AMOLED убран: тёмная всегда чёрная, `amoled` мигрирует в `dark`.
  - CI зелёный, снимки эмулятора проверены (исправлен тонкий шрифт: у вариативных TTF ось
    `wght` задаётся явно через `variationSettings`). Ждёт проверки владельцем.
- **PR #26 — исправление lint `LocalContextGetResourceValueCall`** (ветка
  `ccr-d3f28808-uk5drv-lint`). 18 мест: тосты по id строки, `stringResource()`, хелпер
  `ui/ComposeResources.kt` `currentResources()` (замена на `LocalResources` после обновления
  Compose). Проверено временным поднятием Compose до версии из #22: lint зелёный. CI зелёный.
  После слияния: Dependabot `rebase` в #21 и #22.
- **PR #27 — PR 2 плана: значки Material Symbols Rounded** (ветка `ccr-d3f28808-uk5drv-icons`).
  Генератор `scripts/compile_material_symbols.py` (кэш путей `material_symbols.json`, тест
  `test_compile_material_symbols.py`): `VolaIcons` (`ImageVector` в `shared`) вместо
  `Icons.Default` и 64 перегенерированных drawable с прежними именами. Удалены
  `material-icons-core` и `materialIconsExtended`.
- **PR #28 — PR 3c плана: вынос `AddressBarController`** (ветка
  `ccr-d3f28808-uk5drv-addressbar`, от `main`). Состояние адресной панели (пристыковка, долгое
  нажатие, раскладка действий, фокус при запуске, проба автопристыковки) — в
  `browser/AddressBarController.kt` с узким `Host` и `AddressBarPreferenceStore`; UI обращается к
  `controller.addressBar.*`. Поведение не меняется, 9 юнит-тестов. CI зелёный.
- **PR 3a «Рама» — ветка `ccr-d3f28808-uk5drv-frame`, PR ещё не открыт.** Собрана от `main` с
  вливанием #25, #27 и #28 (merge-коммиты); PR открыть после их слияния, дифф сократится сам.
  CI проверяется ручным запуском *Build* и *Screenshots* на ветке (`workflow_dispatch`): на
  `7880b95` оба зелёные, снимки эмулятора проверены (шапка сайта прилегает к верху карточки;
  исправлен двойной отступ под статус-бар из политики страницы — `tabSafeAreaTopInsetPx()`).
  - Порт `BrowserContentFrame`: `BrowserController.updateContentFrame()` → общий для обоих
    движков `GeckoViewInsetRules.resolve(hostFrame = …)` вычитает карточку из safe-area и
    клавиатуры (страница не получает двойных отступов).
  - `ui/BrowserContentFrameRules.kt` (геометрия, тесты), `BrowserContentFrameUi.kt`: отступы в
    пикселях и `BrowserContentFrameMask` — ореол поверх углов карточки (SurfaceView не
    обрезается скруглением), контур и тень. Токены `VolaFrame`, градиент `VolaAura`.
  - Без рамки: «Воздух», полноэкранный режим и видео, страница настроек расширения, новая вкладка
    (её дизайн — PR 5). В «Раме» нет подложки статус-бара и размытия под панелью.
  - Настройки → Внешний вид → «Оформление»: «Рама»/«Воздух». Превью
    `BrowserContentFramePreviews.kt`. Инструментальные edge-to-edge тесты закреплены на «Воздухе».
  - Осталось на 3b «Остров»: капсула при прокрутке (карточка растёт вниз, доска Scrolled), ободок
    ореола у острова в «Воздухе», стиль панели по доскам Main/Editing.
- **PR 3b «Остров» — ветка `ccr-d3f28808-uk5drv-island` поверх «Рамы», PR ещё не открыт.**
  - Порт `BrowserDynamicToolbarHost`: Gecko — `setDynamicToolbarMaxHeight`/`setVerticalClipping`
    (страница не перекладывается, закреплённые внизу элементы поднимаются над панелью); WebView
    такого API не имеет — карточка там не растёт (`supportsDynamicContentFrame`).
  - В «Раме» движок раскладывает страницу под сжатую капсулу; пока панель развёрнута, низ
    карточки закрыт маской ореола, пружина `VolaMotion.standard` двигает только маску.
  - Сжатая капсула 40 dp (`VolaIsland.compactHeight`) с замком для HTTPS; в «Воздухе» — ободок
    ореола и свечение (`Modifier.islandAuraRim`).
  - CI (ручной запуск) зелёный на `bcb8cdd`, снимки прокрутки проверены: капсула и выросшая
    карточка в светлой и тёмной теме. Тур скриншотов: две прокрутки, до 7 попыток найти пункт меню.
- **PR 3d «Поиск по странице» — ветка `ccr-d3f28808-uk5drv-find` поверх острова, PR не открыт.**
  Строка поиска по доске Find: внизу на месте острова, над клавиатурой, карточка с акцентным
  контуром, счётчик, ↑ ↓ ✕. Чипы «Регистр» и «Слово целиком» — новый порт
  `BrowserEngineViewPort.setFindInPageOptions` (Gecko: `FINDER_FIND_MATCH_CASE`,
  `FINDER_FIND_WHOLE_WORD`; в WebView опций нет — чипы скрыты). «ё = е» не нужен: Gecko ищет
  без учёта диакритики. Остров скрыт, пока открыт поиск. Шаг `find-*` в туре скриншотов.
- **PR #29 — новая вкладка (часть PR 5), ветка `ccr-d3f28808-uk5drv-newtab` поверх «Поиска»:** фон —
  ореол пространства, сверху название пространства (если задано) и дата. Не сделано: «Продолжить»,
  карточка защиты, новая сетка Essentials. CI зелёный. Шаг тура `find-*` не находит сжатую капсулу —
  поправить тур (нажимать по координатам капсулы).
- **Стек веток «Рама» → «Остров» → «Поиск»:** после слияния #25/#27/#28 открывать PR по очереди,
  каждый — от предыдущей ветки после её слияния (дифф каждого PR — только его фича).
- **Порядок слияния:** #26, #27, #28 независимы от #25 и друг от друга; сливать в любом порядке.
  После слияния #25, #27 и #28 — открыть PR «Рамы». Значки превью темы — перевести на `VolaIcons`.
- **PR #1–#17, #19, #20, #23, #24 слиты в `main`** (merge commit, CI зелёный).
  #20 — поддержка Android 12+
  (`minSdk` 31, обёртки в `PlatformCompat.kt`, `IoCompat.kt`). #19 — Dependabot, обновление actions.
- **Ветки слитых PR не удалены:** облачной среде запрещено удалять ветки в GitHub. Владельцу:
  удалить их на странице *Branches* и включить *Settings → General → Automatically delete head
  branches*.
- **Dependabot, открыты:**
  - **#21** (Kotlin 2.4.20, Compose Multiplatform 1.12, kotlinx) и **#22** (Compose BOM 2026.09,
    AndroidX) — CI красный. Новая версия Compose добавила проверку lint
    `LocalContextGetResourceValueCall`: 18 ошибок, первая — `ui/BrowserScreen.kt:423`
    (`context.getString` внутри Compose). Нужен отдельный PR: заменить на `stringResource` /
    `LocalResources`, потом перезапустить эти PR. Effort: High.

## Подпись

- Ключ Preview сгенерирован в сессии 2026-09-30 и передан владельцу файлами (в git не попал).
  Владелец добавляет секрет `VOLA_PREVIEW_KEYSTORE_BASE64` сам. Проверка: в запуске
  *Preview APKs* нет уведомления «VOLA_PREVIEW_KEYSTORE_BASE64 is not set».
- `docs/vola/signing.md` переписан без Termux: ключ делает Claude, владелец только вставляет секрет.
- Релизный ключ ещё не нужен — сделать перед первым релизом.

## Дизайн

**Макеты v4 готовы** (2026-09-30).
- Холст: https://claude.ai/artifact/CEgKgiZHg4zuju2u7ALZ8C, версия 25.
- Первая страница — живой прототип и оформления «Рама»/«Воздух». Дальше 15 страниц с 65 экранами
  v4 по разделам, последняя — «v3 · архив».
- Исходники: `docs/vola/design/canvas/` (`W-*.dc.html`, `V4*.dc.html`, `vola4.css`,
  `vola4-colors.css`). Сборщики и генератор цветов — `docs/vola/design/tools/`.
- Ревизии: `design/review-v4.md` (язык v4, что лучше v3 по разделам), `design/review-v3.md`.
  Обзор браузеров — `design/benchmark-2026.md`.
- Каждый экран v4 сравнён с v3 бок о бок; всё, что проигрывало, переделано. Правило владельца:
  v4 не хуже v3 ни на одном экране.
- Новое в v4: цвет всей оболочки из цвета пространства (Material Color Utilities, контраст ≥ 4,5:1),
  Material Symbols Rounded, Manrope/Literata, пружины M3 Expressive, экраны защиты (опасный сайт,
  проверка загрузки).

## Решения владельца

- «Рама» по умолчанию, «Воздух» — выбор в Настройки → Внешний вид.
- Тёмная тема всегда на чистом чёрном, переключателя OLED нет (записано в `CLAUDE.md`).
- Сторонняя открытая библиотека для KDBX допустима (`features-roadmap.md`).
- План переноса дизайна в код одобрен (`tech-plan.md`, раздел 4, обновлён под v4: 19 PR с
  досками и Effort). PR 1 (токены темы) — #25.
- Палитры Dynamic и Neutral оставлены; в тёмной теме у них тоже чёрный фон.

## Следующие шаги

Готовый промпт для новой сессии — `docs/vola/NEXT_SESSION.md`.

1. Владелец проверяет и сливает #25, #26, #27, #28 (чек-листы в описаниях).
2. После #26: команда Dependabot `rebase` в #21 и #22, затем перевести `currentResources()` на
   `LocalResources.current`.
3. После слияния #25, #27, #28: влить `main` в ветку «Рамы», открыть PR (метка `screenshots`),
   подписаться на события. Затем 3b «Остров» (Effort: **Extra**): капсула при прокрутке с ростом
   карточки (для Gecko — `setVerticalClipping`/динамическая панель, без перекладки страницы),
   ободок ореола в «Воздухе», пружины `VolaMotion`.
4. Хвосты PR 1: PR 5 — `privateMode = true` на приватной вкладке и новая вкладка на ореоле; PR 7 —
   страница HTTPS-only в цветах v4; PR 8 — визуальный выбор «Рама»/«Воздух» по доске SetAppearance.
5. Владелец: секрет Preview; удалить ветки слитых PR.
6. По готовности: `geckoview-update.yml`, храповик размера `BrowserController`, скриншоты
   Compose Preview в CI.

## Заметки о среде

- Android SDK в облачной среде нет (`dl.google.com`, `maven.mozilla.org` закрыты; `maven.google.com`
  открыт). Сборку и тесты проверяет CI. `python3 scripts/test_translations.py` проверяет строки
  EN/RU локально.
- Логи CI из blob-хранилища скачать нельзя: смотреть хвост лога через GitHub-инструменты
  (`get_job_logs` с `tail_lines`). Для lint есть шаг «Show lint errors».
- **Удалять ветки GitHub из среды нельзя** (прокси git не пускает, классификатор запрещает).
- **Рендер холста для проверки глазами:** `support.js` = `artifact-type/dc-runtime.js` из
  артефакта (action `read` с `path`), локальный `python3 -m http.server` (запускать в фоне с
  большим `timeout`), Playwright из `/opt/node22/lib/node_modules/playwright` с
  `executablePath: /opt/pw-browsers/chromium`. Шрифты Google подгружать через `curl` и отдавать
  через `page.route` (сертификат прокси Chromium не принимает; TLS-проверку не отключать).
- Экраны v4 собираются скриптами: `python3 docs/vola/design/tools/run_screens.py [группа …]`.
  Строка состояния и полоска жестов — `status()` и `handle()` в `build_v4.py`.
- Значки v4 — шрифт Material Symbols Rounded с подмножеством из `tools/icon_font_url.txt`. Для
  локального рендера файл шрифта по ссылке `fonts.gstatic.com/l/font?kit=…` сохраняется как
  `k_<md5 ссылки>`.
- Публикация холста: копии файлов в папке `<scratch>/pub/project/…`, вызов Artifact с `url`,
  `root` = `<scratch>/pub`, `file_path` = абсолютный путь к `<scratch>/pub/project/canvas.json`,
  `files` = изменённые файлы (`project/<имя>` → `project/<имя>`).
- Firefox, по сообщениям СМИ, с версии 155 выходит раз в две недели. Учитывать в процессе
  обновления GeckoView (`tech-plan.md`).
- **Проверка Kotlin без Android SDK:** Gradle есть (`/opt/gradle`), Maven Central открыт, а
  Google Maven (`dl.google.com`, `maven.google.com`) закрыт. Файлы без Android-зависимостей
  (цвета, токены, модели `shared`) собираются в отдельном JVM-проекте в scratchpad на
  `org.jetbrains.compose.material3:material3-desktop` с исключёнными группами `androidx.lifecycle`,
  `androidx.annotation`, `androidx.collection`, `androidx.arch.core`. Так гоняются и JVM-тесты.
  Maven Central иногда отвечает 429 — повторить через минуту.
- **Библиотека цвета 0.4.0:** часть импортов уже с `.js`; правка из README инструментов
  идемпотентна.
- **Визуальная проверка цветов:** HTML-копия сетки превью из `VolaSchemes.kt` рендерится в
  Chromium (Playwright) с локальным `res/font/manrope.ttf`, так проверены схемы в PR #25.
- **Отдельные PR при неслитом #25:** владелец разрешил новые ветки
  `ccr-d3f28808-uk5drv-<тема>` от свежего `main` (сессия 2026-09-30).
- **Проверка новой проверки lint без SDK:** временный коммит с поднятием версий в своей ветке →
  CI → убрать коммит (`git reset` + `push --force-with-lease` в своей ветке).
- **Значки:** правка — только через манифест `scripts/compile_material_symbols.py`, затем
  `fetch`. Проверка глазами — лист «было → стало» (SVG из путей, рендер в Chromium).
- Иконки пространств генерирует `scripts/workspace-icons/generate.py`
  (нужен доступ к raw.githubusercontent.com).
