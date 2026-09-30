# Состояние проекта Vola

Файл для передачи работы между сессиями Claude Code. Новая сессия читает его первым и
обновляет в конце работы.

_Обновлено: 2026-09-30 (сессия PR 1 — токены темы)._

## PR и сборка

- **PR #25 — PR 1 переноса дизайна: токены темы v4** (ветка `ccr-d3f28808-uk5drv`, метка
  `screenshots`).
  - Схемы 8 пространств и приватного режима генерируются `gencss.mjs --kotlin` в
    `ui/theme/VolaSchemes.kt`.
  - `VolaTheme` / `VolaExtendedColors`, шрифты Manrope и Literata, `VolaShapes`, `VolaSpacing`,
    `VolaElevation`, `VolaMotion`, настройка `BrowserChromeStyle` (Frame/Air, пока без UI).
  - AMOLED убран: тёмная всегда чёрная, `amoled` мигрирует в `dark`.
  - Статус CI — в самом PR. Слить после зелёного CI и проверки владельцем.
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

1. Довести #25 до зелёного CI; владелец проверяет по чек-листу в описании и сливает.
2. Отдельный PR: исправить lint `LocalContextGetResourceValueCall` (18 мест, `context.getString`
   в Compose → `stringResource` / `LocalResources`), затем перезапустить Dependabot #21 и #22
   (Effort: High). Рекомендуется до PR 2: #21/#22 обновляют Compose, на котором строится весь
   дальнейший UI.
3. PR 2 плана — значки Material Symbols Rounded (`VolaIcons`, Effort: High), дальше по таблице
   `tech-plan.md`.
4. Хвосты PR 1 для следующих PR:
   - PR 5: включать `privateMode = true` в `CandyTheme` на приватной вкладке;
   - PR 7: страница HTTPS-only (`GeckoHttpsOnlyErrorPage.kt`) пока в старых фиолетовых цветах;
   - PR 3/8: показать выбор «Рама»/«Воздух» (`AppearanceSettings.chromeStyle` уже хранится).
5. Владелец:
   - секрет Preview;
   - удалить ветки слитых PR.
6. По готовности:
   - `geckoview-update.yml`;
   - храповик размера `BrowserController`;
   - скриншоты Compose Preview в CI.

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
- Иконки пространств генерирует `scripts/workspace-icons/generate.py`
  (нужен доступ к raw.githubusercontent.com).
