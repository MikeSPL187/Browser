# Состояние проекта Vola

Файл для передачи работы между сессиями Claude Code. Новая сессия читает его первым и
обновляет в конце работы.

_Обновлено: 2026-09-30._

## PR и сборка

- **PR #1–#17, #19, #20 слиты в `main`** (merge commit, CI зелёный). #20 — поддержка Android 12+
  (`minSdk` 31, обёртки в `PlatformCompat.kt`, `IoCompat.kt`). #19 — Dependabot, обновление actions.
- **Ветки слитых PR не удалены:** облачной среде запрещено удалять ветки в GitHub. Владельцу:
  удалить их на странице *Branches* и включить *Settings → General → Automatically delete head
  branches*.
- **Dependabot, открыты:**
  - **#23** (Gradle 9.8, OkHttp 5, Guava, org.json) — CI зелёный, ждёт решения владельца.
  - **#21** (Kotlin 2.4.20, Compose Multiplatform 1.12, kotlinx) и **#22** (Compose BOM 2026.09,
    AndroidX) — CI красный. Новая версия Compose добавила проверку lint
    `LocalContextGetResourceValueCall`: 18 ошибок, первая — `ui/BrowserScreen.kt:423`
    (`context.getString` внутри Compose). Нужен отдельный PR: заменить на `stringResource` /
    `LocalResources`, потом перезапустить эти PR. Effort: High.
- **Текущий PR** — ветка `ccr-86902d83-x44u8j`: холст дизайна v3 и документы (без кода).

## Подпись

- Ключ Preview сгенерирован в сессии 2026-09-30 и передан владельцу файлами (в git не попал).
  Владелец добавляет секрет `VOLA_PREVIEW_KEYSTORE_BASE64` сам. Проверка: в запуске
  *Preview APKs* нет уведомления «VOLA_PREVIEW_KEYSTORE_BASE64 is not set».
- `docs/vola/signing.md` переписан без Termux: ключ делает Claude, владелец только вставляет секрет.
- Релизный ключ ещё не нужен — сделать перед первым релизом.

## Дизайн

Направление: Zen Browser + Material 3 Expressive. Холст (63 экрана, 14 страниц):
https://claude.ai/artifact/CEgKgiZHg4zuju2u7ALZ8C, версия 21 — исходники в
`docs/vola/design/canvas/`.

- Ревизии: `docs/vola/design/review-v2.md`, `docs/vola/design/review-v3.md` (полировка всех
  экранов, общие правила: строка состояния и полоска жестов, зона большого пальца, миниатюры-страницы,
  группы кнопок M3 Expressive).
- После замечаний владельца: «Ввод адреса» — ссылка из буфера и «Перейти» у строки ввода;
  «Видео» — кнопки Vola поверх плеера сайта (новый экран) и лист только с тем, чего у сайта нет.
- Ждём, примет ли владелец v3.

## Решения владельца

- Сторонняя открытая библиотека для KDBX допустима (`features-roadmap.md`).
- План переноса дизайна в код одобрен (`tech-plan.md`, раздел 4). Первый PR — токены темы и шрифты
  Manrope/Literata (Effort: High), начать после приёмки макетов.

## Следующие шаги

1. Владелец: секрет Preview; удалить ветки слитых PR; решить про #23; посмотреть холст v3.
2. PR с исправлением lint `LocalContextGetResourceValueCall`, затем перезапуск #21 и #22.
3. После приёмки макетов — PR 1 переноса дизайна (токены и шрифты). Перед каждым PR рекомендовать
   Effort.
4. По готовности: `geckoview-update.yml`, храповик размера `BrowserController`, скриншоты Compose
   Preview в CI.

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
- Строка состояния и полоска жестов вставляются в каждый экран одним скриптом: разметка —
  `.stat` и `.navh` в `vola.css`, в конце корневого `div` доски.
- Публикация холста: копии файлов в папке `<scratch>/pub/project/…`, вызов Artifact с `url`,
  `root` = эта папка, `file_path` = `project/canvas.json`, `files` = изменённые файлы.
- Firefox, по сообщениям СМИ, с версии 155 выходит раз в две недели. Учитывать в процессе
  обновления GeckoView (`tech-plan.md`).
- Иконки пространств генерирует `scripts/workspace-icons/generate.py`
  (нужен доступ к raw.githubusercontent.com).
