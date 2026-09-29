# Состояние проекта Vola

Файл для передачи работы между сессиями Claude Code. Новая сессия читает его первым и
обновляет в конце работы.

_Обновлено: 2026-09-29 (вторая сессия дня)._

## PR и сборка

- **PR #1–#17 слиты в `main`** по порядку, merge commit, CI каждого был зелёным. Цепочка
  перенацеливалась на `main` перед слиянием.
- **Ветки слитых PR не удалены:** облачной среде запрещено удалять ветки в GitHub. Владельцу:
  удалить их на странице *Branches* и включить *Settings → General → Automatically delete head
  branches*, тогда GitHub будет удалять ветку сам после каждого слияния.
- **PR #20** `build: поддержка Android 12+ (minSdk 31)`, ветка `ccr-86902d83-x44u8j`.
  `minSdk` 33 → 31, обёртки над API Android 13 в `PlatformCompat.kt`, «Назад» в
  `AppDataTransferActivity` через `OnBackPressedDispatcher`, README и строки «Android 12+».
  В `build.yml` добавлен шаг «Show lint errors»: при падении lint в логе видны все ошибки,
  а не только первая. В том же PR — документы и холст дизайна v2 (только docs, без кода).
  CI зелёный, конфликтов нет, ждёт решения владельца.
- **PR #19** — Dependabot (обновление actions). Не трогали, ждёт решения владельца.
- Подписка на события PR #1–#17 и #20 оформлена в сессии 2026-09-29.

## Дизайн

Направление: Zen Browser + Material 3 Expressive. Холст (62 экрана, 14 страниц):
https://claude.ai/artifact/CEgKgiZHg4zuju2u7ALZ8C, версия 18 — исходники в
`docs/vola/design/canvas/` (`vola.css` — токены, `vola-icons.js` — иконки, `*.dc.html` — экраны).

- Ревизия v2 и список исправлений: `docs/vola/design/review-v2.md`.
- Новые страницы холста: «Пароли и автозаполнение», «Перенос и синхронизация», «Инструменты
  страницы», доска «Состояния», OLED-вариант паролей.
- Ключевые решения прежние: адресная панель — плавающий стеклянный остров внизу, капсула при
  прокрутке; цвет пространства тонирует фон; Manrope для интерфейса, Literata для чтения;
  никаких эмодзи, только иконки.
- Владелец ещё не видел v2 — ждём замечаний.

## Документы этой сессии

| Файл | О чём |
| --- | --- |
| `docs/vola/signing.md` | Пошагово с телефона: ключ Preview в Termux → секрет GitHub → резервная копия |
| `docs/vola/ci-instrumented-tests.md` | Скриншоты Compose Preview на JVM + androidTest на эмуляторе (Gradle Managed Devices, шарды) + дымовой прогон на Android 12 |
| `docs/vola/features-roadmap.md` | Сравнение с Chrome, Firefox, Safari, Samsung, Vivaldi, Brave, Opera, Arc, Zen; менеджер паролей и синхронизация без сервера |
| `docs/vola/tech-plan.md` | Разгрузка BrowserController и настроек по правилу бойскаута, процесс обновления GeckoView, план PR переноса дизайна с уровнями Effort |

## Следующие шаги

1. Владелец: удалить ветки слитых PR; добавить секрет `VOLA_PREVIEW_KEYSTORE_BASE64`
   (`signing.md`); посмотреть холст v2; решить про PR #19 и про стороннюю библиотеку для KDBX.
2. PR #20 зелёный и без конфликтов: слить после согласия владельца (merge commit).
3. Перенос дизайна в код — по таблице в `tech-plan.md`, начиная с токенов и шрифтов
   (Effort: High). Перед каждым PR рекомендовать Effort и ждать подтверждения.
4. Параллельно по готовности: `geckoview-update.yml`, храповик размера `BrowserController`,
   скриншоты Compose Preview в CI.

## Заметки о среде

- Android SDK в облачной среде нет (`dl.google.com`, `maven.mozilla.org` закрыты; `maven.google.com`
  открыт). Сборку и тесты проверяет CI. `python3 scripts/test_translations.py` проверяет строки
  EN/RU локально.
- Логи CI из blob-хранилища скачать нельзя: смотреть хвост лога через GitHub-инструменты.
  Для lint теперь есть шаг «Show lint errors».
- **Удалять ветки GitHub из среды нельзя** (прокси git не пускает, классификатор запрещает).
- **Рендер холста для проверки глазами:** `support.js` = `artifact-type/dc-runtime.js` из
  артефакта (action `read` с `path`), локальный `python3 -m http.server`, Playwright из
  `/opt/node22/lib/node_modules/playwright` с `executablePath: /opt/pw-browsers/chromium`.
  Шрифты Google подгружать через `curl` и отдавать через `page.route` (сертификат прокси
  Chromium не принимает; TLS-проверку не отключать).
- Публикация холста: копии файлов в папке `<scratch>/pub/project/…`, вызов Artifact с `url`,
  `root` = эта папка, `file_path` = `project/canvas.json`, `files` = изменённые файлы.
- Firefox, по сообщениям СМИ, с версии 155 выходит раз в две недели. Учитывать в процессе
  обновления GeckoView (`tech-plan.md`).
- Иконки пространств генерирует `scripts/workspace-icons/generate.py`
  (нужен доступ к raw.githubusercontent.com).
