---
name: crash
description: "Read a Vola crash report - from the in-app crash journal («Vola закрылась с ошибкой», #126), a logcat crash buffer or a tombstone - symbolicate its Gecko frames and find the cause in the code. Use when the user pastes or attaches a crash report, a backtrace or a nightly smoke failure, and when the user types /crash."
---

# Разбор отчёта о сбое

Отчёт из журнала сбоев Vola начинается строкой `Process … stopped: …`, затем `Vola <версия> (…, full localRelease)`.
Нативный сбой содержит кадры `#00 pc 0495dd78  libxul.so (BuildId: …)` — их расшифровывает
`scripts/ci/symbolicate.py` через сервер символов Mozilla (хост открыт в настройках среды).

## Порядок
1. Сохранить текст отчёта в файл в scratchpad (не в репозиторий: там могут быть адреса страниц).
2. **Тип сбоя** по первой строке:
   - `uncaught exception` — Java/Kotlin: стек уже читаемый, искать верхний кадр `dev.sk2andy.…`;
   - `native crash (signal …)` — `python3 scripts/ci/symbolicate.py <файл>`;
   - `was not responding` — смотреть поток `main` в отчёте: что блокирует главный поток;
   - `closed by Android to free memory` — не ошибка кода; смотреть, что держит память.
3. **Расшифрованный стек** читать сверху: первая функция Gecko называет подсистему
   (`WebAuthn…` → #123 H1, `GeckoView…Display`/`Compositor` → отрисовка, `mozilla::jni` → вызов
   Java из Gecko). Исходники GeckoView: `raw.githubusercontent.com/mozilla-firefox/firefox/main/<путь>`.
4. **Воспроизвести.** Ночной smoke (`.github/workflows/nightly-smoke.yml`, итоги в #134) открывает
   популярные сайты в release-сборке; для своего сайта — Actions → Nightly smoke → Run workflow
   после правки списка `SITES` в `scripts/ci/release_smoke.py`.
5. **Доложить** владельцу: где упало (функция и файл), почему, что предлагается; исправление —
   отдельным PR по обычным правилам (`/claim`, `/preflight`).
