# Заметки о среде Claude Code

Приёмы и ограничения облачной среды, найденные в прошлых сессиях. Читать при упоре в среду
(сеть, сборка, CI, рендер досок), а не на старте каждой сессии.


- **Material 3 1.5.0-alpha29:** `Slider(value = …)` устарел; новая перегрузка —
  `Slider(state, onValueChange, …, onValueChangeFinished)`, `SliderState(value, steps, trackRange)`;
  `onValueChange` сам обновляет `state.value`. Исходники JetBrains на Maven Central отстают
  (1.13.0-alpha01 = AndroidX alpha27, старый API) — сверять с `androidx-main` на
  raw.githubusercontent.com и с текстом предупреждения в логе CI.
- **Тур стал длиннее:** четыре прохода с шагами Q16c/Q18 — около часа; лимит задачи 75 мин (#83).
  Ветка, в которой код другой ветки уже содержится и чей тур чист, проверяет и её (так слиты #85
  через #86 и #79 через #84).

- **Слияние серии PR (сессия 2026-10-04):** пробная интеграция в своей ветке + `workflow_dispatch`
  для `build.yml` и `screenshots.yml` (тур публикуется в `screenshots/<ветка>/<sha7>/`); затем
  конвейер «голова предыдущего PR → следующий PR». После каждого слияния веток проверять, что ветка
  запушена (`git fetch` и сверка SHA), иначе следующая вольёт старую голову. Кэш иконок после
  объединения — `compile_material_symbols.py fetch` (удаляет лишние записи; иначе падает
  `test_compile_material_symbols.py`). Локально перед пушем гонять все `scripts/test_*.py`, а не только
  гейты. GitHub после слияния базы сам переводит зависимые PR на `main`.

- **Продолжение из другого аккаунта Claude:** история сессий не переносится — контекст только в
  `docs/vola/`. Нужен GitHub с правами записи в репозиторий (подготовка — в конце
  `NEXT_SESSION.md`); подписка на PR, открытые другим аккаунтом, может не работать — CI проверять
  вручную.
- **Compose UI не собрать в контейнере:** compose-desktop 1.9 тянет `androidx.*` с Google Maven
  (403). Shared-UI проверяет только CI; чистые правила — в JVM-проекте (Kotlin 2.2.20, junit).
- **Gecko в дампе UI:** uiautomator видит текст страницы GeckoView (узлы доступности) — тур может
  искать ссылки по тексту, но только в видимой части (прокручивать).
- **Ru-проход тура** иногда падает на `adb shell input tap` (код 1) после смены локали — шаги
  пропускаются, снимков `*-ru` нет. Не баг кода.

- **JVM-проект (сессия 2026-10-04):** `/opt/gradle` 8.14 есть, но тулчейна JDK 17 нет — не писать
  `jvmToolchain(17)`; Maven Central иногда отвечает 429 — повторить через 10 с. Файлы с Android
  (`Context`, `R`) — копией без этих классов, а не симлинком; `@Immutable` убрать.
- **Подписка на PR другой сессии** не работает («Could not subscribe») — проверять их вручную.
- **Доски** рендерит `<scratch>/render/render.js` (Playwright из `/opt/node-tools`, Chromium
  `/opt/pw-browsers/chromium-1194/chrome-linux/chrome`, `support.js` подменён пустым, шрифты Google —
  через `curl`).

- **Экран, до которого тур не дойдёт** (нужна биометрия, нет страницы с запросом разрешения):
  отладочная активити в `app/src/debug/.../ui/*PreviewActivity.kt` (exported, только debug) +
  `adb shell am start -n $PACKAGE/<класс>` в туре. Так сняты экран замка (#54) и лист разрешения (#57).
- **JVM-проект в этой сессии:** Gradle + Kotlin 2.2.20, `org.jetbrains.compose.runtime:runtime-desktop:1.6.11`
  (без `androidx.collection`), исходники и тесты — симлинками; для `BrowserTab` нужны заглушки
  `DEFAULT_PROFILE_ID` и `BrowserEngineFailureKind`. Кешируется в `~/.gradle` — второй прогон ≈1 с.
- **Аудит доступности:** находка «unlabeled» на полоске у нижнего края экрана — смотри
  `ui-<экран>.xml` из ветки `screenshots` и прогоняй `a11y_audit.audit()` локально на выгрузке.

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
- **Итог тура без лога CI:** в ветке `screenshots` рядом с кадрами лежат `crash-log.txt`,
  `tour-log.txt`, `a11y-report.txt`, `startup.txt` — `git fetch --depth 1 origin screenshots`, затем
  `git show FETCH_HEAD:<ветка>/<sha7>/crash-log.txt`. Обрезанный PNG = эмулятор отвалился на снимке.
- **Серия слияний:** `git config rerere.enabled true` — разрешение конфликта, найденное пробным
  слиянием с будущим `main`, применится само при настоящем слиянии.
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
- **JVM-проект для чистых частей (сессия Q5):** `<scratch>/jvm` — Gradle 8.14 + Kotlin 2.4.20,
  `org.jetbrains.compose.runtime:runtime-desktop:1.6.11` (новые версии тянут `androidx.*` с Google
  Maven), `exclude(group = "androidx.collection")`, без `jvmToolchain`; заглушки `android.graphics.Bitmap`,
  `android.content.Context/SharedPreferences`; исходники и тесты — симлинками из `app/src`. Так
  ловятся и конфликты JVM-сигнатур (`isX` + `setX`).
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
