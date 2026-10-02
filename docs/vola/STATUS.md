# Состояние проекта Vola

Файл для передачи работы между сессиями Claude Code. Новая сессия читает его первым и
обновляет в конце работы.

_Обновлено: 2026-10-02 (сессия Q4: #35 ввод адреса и #36 счётчик поиска; #34 Q3b готов — всё ждёт слияния)._

## PR и сборка

- **PR #36 — счётчик поиска «0/0»** (ветка `claude/find-counter` поверх #34, метка `screenshots`).
  GeckoView может ответить `found=true` с `total` 0/−1 до подсчёта; адаптер принимал это за итог.
  `GeckoFindResult.found`; правило держит «найдено, не посчитано» в подсчёте (индикатор, стрелки
  работают); `FindInPageController` через 400 мс делает шаг назад и вперёд (до 2 раз), иначе «3/…».
  Поиск вынесен из `BrowserController` (−95 строк) за узкий порт `BrowserEngineFindPort`. Лог тура
  пишет показание счётчика (`find counter …`). Исходники Gecko из среды недоступны (прокси).
- **PR #35 — Q4: ввод адреса и подсказки** (ветка `ccr-d5100543-bbvsh1`, метка `screenshots`, поверх #34 — владелец просил
  #34 пока не сливать; после его слияния дифф сократится сам).
  - Состояние ввода (открыт, текст, подсветка, фокус) — `browser/AddressEditorState.kt` в
    `AddressBarController.editor`; `BrowserScreen` делегирует свои переменные (`by editor::…`).
    Туда же переехали подсказки из библиотеки и автодополнение домена (`navigationSuggestions`,
    `domainCompletion`, `Host.suggestionSources()`): `BrowserController` −11 строк, `BrowserScreen`
    −7, храповик обновлён.
  - Группы `AddressSuggestionGroupRules`: сверху библиотека (история, избранное — новый источник),
    поиск, у поля — открытые вкладки; волосяные линии между группами. Порядок на экране = порядок
    для клавиатуры.
  - **П1 принято владельцем (2026-10-01):** вкладки других пространств — строка «Открыта в «X»» с
    самоцветом; касание → `requestProfileSelection`, затем `switchToOpenTab`. Заблокированные
    пространства и приватные вкладки не показываются; в приватной вкладке — только приватные.
  - Чип буфера: до касания читается только `ClipDescription` (тип, классификация URL Android 12+,
    возраст ≤ 5 мин) — системного уведомления нет; по касанию ссылка открывается, текст вставляется.
    Отклонение от доски: в чипе нет адреса (его нельзя показать без чтения буфера).
  - Морф: поле 56 dp с акцентным кольцом, пружина `VolaMotion.standard`; боковые кнопки уходят на
    пружинах. Шапка «Новая вкладка в «Работе»» с самоцветом (`WorkspaceGem`, цвета — `VolaGemColors`).
  - Токены `VolaAddressEditor`, `VolaGem`; значки ContentPaste, History, NorthWest, Star
    (`ic_north_east` удалён); 13 строк EN/RU; `@VolaPreviews` в `AddressEditorPreviews.kt`; тесты
    правил (группы, буфер, подсветка, П1, избранное, строки); шаг тура `address-*`.
  - CI зелёный на `faa2946`: 46 кадров, без падений, аудит — 3 известных находки; снимки
    `address-*` просмотрены.
  - Первый прогон (`cc76b77`): lint `RememberReturnType`, аудит нашёл подложку редактора без
    подписи, шапка не читалась поверх страницы — исправлено в `0fcba2c`.
  - Не сделано (по доске): микрофон (Q25), чип выбора поисковика, QR (нужны Play Services),
    значки сайтов в плитках (пока монограммы).
- **#34 — Q3b: CI зелёный на `460ec2e`, ждёт слияния владельцем.** Найдено при проверке: в трёх
  прошлых прогонах тура почти все кадры закрывал системный диалог «System UI isn't responding»
  (медленный эмулятор; `hide_error_dialogs` не помогает); пока он открыт, uiautomator видит только
  его — меню, поиск и настройки «not found», аудит пустой. Тур теперь нажимает «Подождать» перед
  каждым дампом (`dump_ui()`). Прогон `460ec2e`: 43 кадра, все шаги, аудит — 3 известных находки.
- **Dependabot #21 и #22 закрыты** (2026-10-01).
- **Q3b — проверки в туре и `@VolaPreviews`** (ветка `claude/q3b-tour-checks`, метка `screenshots`).
  Тур сохраняет дамп UI рядом с каждым снимком (`ui-dumps/`, не публикуется);
  `scripts/ci/a11y_audit.py` (+4 теста) ищет кликабельные элементы Vola меньше 48 dp и без подписи
  (страницы GeckoView/WebView пропускаются); известные — в `scripts/ci/a11y-baseline.txt`, падают
  только новые (без файла — только сбор). Проход со шрифтом 200 % (`*-a11y`), холодный старт —
  `startup.txt` и сводка задачи. `@VolaPreviews` в `ui/theme`, первым применён в `FindInPageBar`.
  Первый прогон (`6fa43dc`): 10 находок, часть — обрезанные краем элементы; аудит теперь не меряет
  их размер, а «без подписи» ключует по экрану. Контрольный прогон (`ee69e56`) зелёный: 3 известных
  находки (карточка вкладки, переключатели пространства и «Внешнего вида») — это вся базовая
  линия; малые размеры оказались обрезанными краем и удалены. Холодный старт на эмуляторе:
  в среднем 3,1–3,3 с — точка отсчёта.
- **Q3a — проверки качества в CI — слит (#33).** В CI: 0 находок по всем четырём проверкам.
  `scripts/ci/quality_gates.py` + `scripts/test_quality_gates.py`, шаг *Quality gates* в *Build*:
  `size` (храповик, `size-baseline.json`), `tokens` (новые строки PR, `// token-exempt: причина`),
  `engine` (типы движков только в адаптерах), `brand`. На `main` — 0 находок; на прошлых UI-PR
  правило токенов нашло бы 12 литералов. Версия GeckoView для архива данных перенесена в
  `browser/gecko/GeckoRuntimeIdentity.kt`. Q3b (тур: 48 dp, TalkBack, 200 %, старт; `@VolaPreviews`)
  — следующим PR.
- **PR #32 — Q2: GeckoView 157.0 и ежедневный `geckoview-update.yml` — слит 2026-10-01.**
  - `scripts/ci/geckoview_latest.py` + `scripts/test_geckoview_latest.py` (13 тестов, в шаге
    *Script tests*): последняя стабильная версия из `maven-metadata.xml`, запись в каталог.
  - Воркфлоу: ежедневно в 05:17 UTC; ветка `geckoview/<версия>`, PR с меткой `screenshots`,
    закрытие устаревших PR GeckoView, запуск *Build* и *Screenshots* через `workflow_dispatch`.
    В PR, меняющих воркфлоу или скрипт, — холостой прогон (печатает версии).
  - Холостой прогон нашёл `157.0.20260924084938`; версия поднята тем же скриптом.
  - Dependabot GeckoView больше не трогает (`ignore`). Метки `dependencies` в репозитории нет.
  - **Владельцу:** *Settings → Actions → General → Allow GitHub Actions to create and approve pull
    requests* — без этого воркфлоу пушит ветку, но не может открыть PR.
- **2026-10-01: слит #31 (Q1).** #21 и #22 Dependabot закроет при следующей проверке.
- **2026-10-01: план работ — `docs/vola/ROADMAP.md`** (очередь Q1–Q31 с Effort, порядок работы
  над PR, критерии качества, Vola vs лидеры, техдолг). Владелец подтвердил очередь, начата Q1.
- **PR #31 — Q1: зависимости вместо Dependabot #21 и #22** (ветка `claude/new-session-sdccw8`,
  метка `screenshots`). Compose BOM 2026.09.00, Material 3 1.5.0-alpha29, AndroidX, AGP 9.4.1,
  Kotlin 2.4.20, Compose Multiplatform 1.12.1, kotlinx-serialization 1.11.0. kotlinx-datetime
  удалён: `Instant.parse` → `kotlin.time.Instant` (сверено на 14 входах). `currentResources()` →
  `LocalResources.current`. Аксессоры `compose.*` в `shared` заменены записями каталога
  `jetbrains-compose-*` (те же координаты: 1.12.1 и material3 1.9.0).
  - CI на `bbeb28f` зелёный, снимки эмулятора сверены с #30: раскладка та же, отличия — живые
    сайты и часы; падений нет.
  - Новые предупреждения Material 3 1.5 (`Slider`, `ListItem`, `SearchBar`, 13 мест) не
    исправлены намеренно: экраны переделываются в Q16/Q18, там их проверят снимки (ROADMAP).
  - Наблюдение: на снимке `find-dark` счётчик «0/0» при подсвеченных совпадениях (код поиска не
    менялся) — отдельный PR, см. ROADMAP, раздел 5.
  - После слияния Dependabot должен сам закрыть #21 и #22.
- **2026-09-30: слиты #30** (тур снимков: капсула по координатам).
- **2026-09-30: слиты #25, #26, #27, #28 и #29** (токены, lint, значки, `AddressBarController`,
  «Рама», «Остров», «Поиск», новая вкладка). Dependabot `rebase` запрошен в #21 и #22; после
  слияния #22 заменить `currentResources()` на `LocalResources.current`.

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
- **PR #1–#17, #19, #20, #23, #24 слиты в `main`** (merge commit, CI зелёный).
  #20 — поддержка Android 12+
  (`minSdk` 31, обёртки в `PlatformCompat.kt`, `IoCompat.kt`). #19 — Dependabot, обновление actions.
- **Ветки слитых PR не удалены:** облачной среде запрещено удалять ветки в GitHub. Владельцу:
  удалить их на странице *Branches* и включить *Settings → General → Automatically delete head
  branches*.
- **Dependabot, открыты:** #21 и #22 (CI красный с 29.09, `rebase` не сработал) — их версии
  взяты в #31.

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

- 2026-10-02: на новой вкладке сетка закладок Candy заменяется на Essentials (закладки — в
  «Избранном»); Q5 разбит на Q5a (Essentials) и Q5b (защита, П7).
- 2026-10-02: Essentials — своя сетка у каждого пространства (новое получает копию «Личного»);
  П7 принято в Q5 (недельный отчёт защиты по сайтам, только данные устройства, карточку можно
  скрыть); П8 принято (свайп по панели — вкладки, пространства — в обзоре и по самоцвету; CLAUDE.md
  обновлён).
- 2026-10-01: П1 принято (Q4, #35).
- «Рама» по умолчанию, «Воздух» — выбор в Настройки → Внешний вид.
- Тёмная тема всегда на чистом чёрном, переключателя OLED нет (записано в `CLAUDE.md`).
- Сторонняя открытая библиотека для KDBX допустима (`features-roadmap.md`).
- План переноса дизайна в код одобрен (`tech-plan.md`, раздел 4, обновлён под v4: 19 PR с
  досками и Effort). PR 1 (токены темы) — #25.
- Палитры Dynamic и Neutral оставлены; в тёмной теме у них тоже чёрный фон.

## Следующие шаги

Готовый промпт для новой сессии — `docs/vola/NEXT_SESSION.md`.

Очередь и порядок — `docs/vola/ROADMAP.md`, раздел 2.

1. Владелец сливает #34 (Q3b), затем #35 (Q4) и #36 (счётчик поиска); после каждого слияния
   оставшиеся ветки — влить `main` (конфликт ожидается только в `scripts/ci/size-baseline.json`:
   взять `quality_gates.py size --update`). Включить разрешение Actions открывать PR.
2. **Q5a — Essentials** (слово владельца дано 2026-10-02; план — `NEXT_SESSION.md`): заменяет сетку
   закладок на новой вкладке; затем **Q5b** — карточка защиты и недельный отчёт (П7).
3. Ответы владельца на остальные вопросы ROADMAP, раздел 6 (Safe Browsing, П2–П6, релизный ключ).
4. Владелец: секрет Preview; удалить ветки слитых PR.

## Заметки о среде

- **Доски без холста:** `W-*.dc.html` рендерятся и без `support.js` (подменить пустым скриптом через
  `page.route`), шрифты Google — через `curl` в `page.route`. Скрипт —
  `<scratch>/render/render.js` (пересоздать по образцу из сессии Q4: Playwright, Chromium из
  `/opt/pw-browsers`). Кадр 390×844 вырезать Pillow.
- **Системный диалог на эмуляторе:** проверять снимки глазами (контактный лист Pillow), а не только
  статус задачи — зелёный тур может снять диалог вместо экрана.

- **Исходники Material 3 без Google Maven:** `dl.google.com` закрыт, но JetBrains публирует
  копию на Maven Central: `org.jetbrains.compose.material3:material3:<v>:sources` (общий код:
  `Slider.kt`, `ListItem.kt`, `SearchBar.kt`). Соответствие версий — в
  `material3-android-<v>.module` (1.13.0-alpha01 → AndroidX 1.5.0-alpha27). Константы плагина
  CMP (`composeMaterial3Version`) — `javap -constants` на `compose-gradle-plugin-<v>.jar`.
- **Сравнение снимков с прошлым PR:** `git archive origin/screenshots <папка>` для обеих веток,
  Pillow (`pip install pillow`) — разница по пикселям и листы «было → стало».
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
